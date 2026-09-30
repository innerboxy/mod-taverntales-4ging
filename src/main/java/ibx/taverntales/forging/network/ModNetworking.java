package ibx.taverntales.forging.network;

import ibx.taverntales.forging.client.ClientLootBags;
import ibx.taverntales.forging.compat.beyonddimensions.BeyondDimensionsCompat;
import ibx.taverntales.forging.lootbag.LootBagTables;
import ibx.taverntales.forging.menu.EquipmentForgeMenu;
import ibx.taverntales.forging.recipe.EquipmentForgeRecipe;
import ibx.taverntales.forging.registry.ModRecipes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetworking {

    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(CraftEquipmentPayload.TYPE, CraftEquipmentPayload.STREAM_CODEC, ModNetworking::handleCraft);
        registrar.playToClient(NetItemsPayload.TYPE, NetItemsPayload.STREAM_CODEC, ModNetworking::handleNetItems);
        registrar.playToClient(LootBagSyncPayload.TYPE, LootBagSyncPayload.STREAM_CODEC, ModNetworking::handleLootBagSync);
    }

    /**
     * 服务端:登录与 /reload 时把锻造配方和袋子表发给玩家(数据包驱动,故不能在启动时算一次了事)。
     * 挂 OnDatapackSyncEvent 而非登录事件,是为了让 /reload 后界面和 JEI 里的展示也跟着更新。
     * 客户端在 RecipesReceivedEvent 里接收(见 ClientForgeRecipes)。
     */
    public static void onDatapackSync(final OnDatapackSyncEvent event) {
        event.sendRecipes(ModRecipes.EQUIPMENT_FORGE_TYPE.get());
        var payload = new LootBagSyncPayload(LootBagTables.collect(event.getPlayerList().getServer()));
        // 登录时只含刚登录的那位,/reload 时是所有人
        event.getRelevantPlayers().forEach(p -> PacketDistributor.sendToPlayer(p, payload));
    }

    /** 客户端:缓存袋子表供 JEI 展示 */
    private static void handleLootBagSync(LootBagSyncPayload payload, IPayloadContext context) {
        ClientLootBags.accept(payload.tables());
    }

    /** 服务端:向玩家同步其超越维度网络的物品快照(未装 BD 时不发送) */
    public static void syncNetItems(ServerPlayer player) {
        if (BeyondDimensionsCompat.isLoaded()) {
            PacketDistributor.sendToPlayer(player, new NetItemsPayload(BeyondDimensionsCompat.getStoredItems(player)));
        }
    }

    /** 客户端:缓存超越维度物品快照到当前锻造菜单 */
    private static void handleNetItems(NetItemsPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof EquipmentForgeMenu menu) {
            menu.setNetItems(payload.items());
        }
    }

    /** 服务端处理锻造请求:校验界面仍打开、配方存在、材料足够,然后消耗并发放 */
    private static void handleCraft(CraftEquipmentPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!(player.containerMenu instanceof EquipmentForgeMenu menu) || !menu.stillValid(player)) return;

        player.level().getServer().getRecipeManager().byKey(payload.recipeId()).ifPresent(holder -> {
            if (!(holder.value() instanceof EquipmentForgeRecipe recipe)) return;
            if (!recipe.consume(player)) return;

            // 背包能放多少放多少,放不下的掉落到地上(避免背包满时物品丢失)
            ItemStack result = recipe.result().create();
            player.getInventory().placeItemBackInInventory(result);
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
            // 锻造可能消耗了网络中的物品,同步最新快照
            syncNetItems(player);
        });
    }
}

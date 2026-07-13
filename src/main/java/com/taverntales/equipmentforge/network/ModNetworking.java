package com.taverntales.equipmentforge.network;

import com.taverntales.equipmentforge.compat.beyonddimensions.BeyondDimensionsCompat;
import com.taverntales.equipmentforge.menu.EquipmentForgeMenu;
import com.taverntales.equipmentforge.recipe.EquipmentForgeRecipe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetworking {

    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(CraftEquipmentPayload.TYPE, CraftEquipmentPayload.STREAM_CODEC, ModNetworking::handleCraft);
        registrar.playToClient(NetItemsPayload.TYPE, NetItemsPayload.STREAM_CODEC, ModNetworking::handleNetItems);
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

        player.serverLevel().getRecipeManager().byKey(payload.recipeId()).ifPresent(holder -> {
            if (!(holder.value() instanceof EquipmentForgeRecipe recipe)) return;
            if (!recipe.consume(player)) return;

            ItemStack result = recipe.getResultItem(player.registryAccess()).copy();
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
            // 锻造可能消耗了网络中的物品,同步最新快照
            syncNetItems(player);
        });
    }
}

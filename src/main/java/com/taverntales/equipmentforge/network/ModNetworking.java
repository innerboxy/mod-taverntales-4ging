package com.taverntales.equipmentforge.network;

import com.taverntales.equipmentforge.menu.EquipmentForgeMenu;
import com.taverntales.equipmentforge.recipe.EquipmentForgeRecipe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetworking {

    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(CraftEquipmentPayload.TYPE, CraftEquipmentPayload.STREAM_CODEC, ModNetworking::handleCraft);
    }

    /** 服务端处理锻造请求:校验界面仍打开、配方存在、材料足够,然后消耗并发放 */
    private static void handleCraft(CraftEquipmentPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!(player.containerMenu instanceof EquipmentForgeMenu menu) || !menu.stillValid(player)) return;

        player.serverLevel().getRecipeManager().byKey(payload.recipeId()).ifPresent(holder -> {
            if (!(holder.value() instanceof EquipmentForgeRecipe recipe)) return;
            if (!recipe.consume(player.getInventory())) return;

            ItemStack result = recipe.getResultItem(player.registryAccess()).copy();
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
        });
    }
}

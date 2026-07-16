package com.taverntales.equipmentforge.client;

import com.taverntales.equipmentforge.TavernTalesEquipmentForge;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 创造模式物品栏(搜索页)的物品顺序,用作"稀有度相同时"的次级排序依据。
 * 锻造界面与 JEI 的战利品袋分类共用同一份顺序,免得两处排法漂移。
 */
public final class CreativeOrder {
    private CreativeOrder() {
    }

    /**
     * 物品 -> 搜索页中的序号。查不到的物品(如被隐藏的)不在表里,调用方通常按
     * {@code getOrDefault(item, Integer.MAX_VALUE)} 排到最后。
     */
    public static Map<Item, Integer> build() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return Map.of();

        try {
            CreativeModeTabs.tryRebuildTabContents(
                    minecraft.player.connection.enabledFeatures(), false, minecraft.level.registryAccess());
        } catch (Exception e) {
            TavernTalesEquipmentForge.LOGGER.debug("重建创造物品栏内容失败,使用现有缓存", e);
        }

        Map<Item, Integer> order = new HashMap<>();
        int index = 0;
        for (ItemStack stack : CreativeModeTabs.searchTab().getDisplayItems()) {
            order.putIfAbsent(stack.getItem(), index++);
        }
        return order;
    }
}

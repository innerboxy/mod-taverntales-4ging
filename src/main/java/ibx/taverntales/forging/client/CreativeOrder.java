package ibx.taverntales.forging.client;

import ibx.taverntales.forging.TavernTalesEquipmentForge;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * 物品展示顺序:先按稀有度,稀有度相同时按创造模式物品栏(搜索页)顺序。
 * 锻造界面(配方、材料)与 JEI 的战利品袋分类共用这一个排序器,免得几处排法漂移。
 */
public final class CreativeOrder {
    private CreativeOrder() {
    }

    /**
     * 稀有度(普通→史诗) → 搜索页顺序;不在搜索页的物品(如被隐藏的)排到最后。
     * 每次调用都重建一次顺序表:创造栏内容会随数据包/配置变化,不能缓存。
     */
    public static Comparator<ItemStack> comparator() {
        Map<Item, Integer> order = searchTabOrder();
        return Comparator.<ItemStack>comparingInt(stack -> stack.getRarity().ordinal())
                .thenComparingInt(stack -> order.getOrDefault(stack.getItem(), Integer.MAX_VALUE));
    }

    /** 物品 -> 搜索页中的序号 */
    private static Map<Item, Integer> searchTabOrder() {
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

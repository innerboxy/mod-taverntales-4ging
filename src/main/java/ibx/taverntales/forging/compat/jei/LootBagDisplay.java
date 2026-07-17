package ibx.taverntales.forging.compat.jei;

import ibx.taverntales.forging.lootbag.LootBagDrop;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * JEI 里的一条"袋子 -> 掉落"展示。
 *
 * @param type     袋子类型(裸字符串),同时是 JEI 的去重依据
 * @param bagStack 已带好 loot_bag_type 组件的袋子物品,其 tooltip 即袋子显示名
 * @param drops    解析出的可能掉落
 */
public record LootBagDisplay(String type, ItemStack bagStack, List<LootBagDrop> drops) {
}

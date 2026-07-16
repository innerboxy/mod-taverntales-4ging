package com.taverntales.equipmentforge.lootbag;

import net.minecraft.world.item.ItemStack;

/**
 * JEI 展示用的单条掉落。刻意只含原版类型——解析虽由 JER 完成,但它的 {@code LootDrop} 不出现在
 * 本类型里,这样未装 JER 时相关类不会被加载。
 *
 * @param item   掉落物(数量无意义,看 min/max)
 * @param chance 掉落概率 0~1
 * @param min    最小数量
 * @param max    最大数量
 */
public record LootBagDrop(ItemStack item, float chance, int min, int max) {
}

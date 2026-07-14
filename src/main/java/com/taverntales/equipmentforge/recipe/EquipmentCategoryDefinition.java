package com.taverntales.equipmentforge.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

/**
 * 数据驱动的装备分类定义,存于数据包注册表 {@code taverntales_4ging:equipment_category}。
 * 分类 id 即其文件的 ResourceLocation(配方通过该 id 归类);此对象只描述展示信息。
 *
 * <pre>
 * data/&lt;命名空间&gt;/equipment_category/melee.json
 * {
 *   "name": { "translate": "taverntales_4ging.category.melee" },
 *   "order": 0,
 *   "icon": "minecraft:iron_sword"
 * }
 * </pre>
 *
 * @param name  左侧标签的显示名(完整 Component,通常用 translate)
 * @param order 从上到下的顺序,越小越靠上;相同则按分类 id 字母序
 * @param icon  标签图标物品 id,找不到该物品时回退为书本
 */
public record EquipmentCategoryDefinition(Component name, int order, ResourceLocation icon) {
    public static final Codec<EquipmentCategoryDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(EquipmentCategoryDefinition::name),
            Codec.INT.optionalFieldOf("order", 0).forGetter(EquipmentCategoryDefinition::order),
            ResourceLocation.CODEC.fieldOf("icon").forGetter(EquipmentCategoryDefinition::icon)
    ).apply(instance, EquipmentCategoryDefinition::new));
}

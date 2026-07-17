package ibx.taverntales.forging.recipe;

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
 * data/&lt;命名空间&gt;/category/melee.json
 * {
 *   "name": { "translate": "category.taverntales_4ging.melee" },
 *   "order": 0,                          // 可选,默认 0
 *   "icon": "minecraft:iron_sword"       // 可选,默认 minecraft:book
 * }
 * </pre>
 *
 * @param name  左侧标签的显示名(完整 Component,通常用 translate)
 * @param order 从上到下的顺序,越小越靠上;相同则按分类 id 字母序。可选,默认 0
 * @param icon  标签图标物品 id(可指向可选模组的物品)。可选,默认书本;
 *              若指定的物品不存在,回退图标在客户端代码中硬编码
 */
public record EquipmentCategoryDefinition(Component name, int order, ResourceLocation icon) {
    /** 未指定 icon 时的默认图标 */
    public static final ResourceLocation DEFAULT_ICON = ResourceLocation.withDefaultNamespace("book");

    public static final Codec<EquipmentCategoryDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(EquipmentCategoryDefinition::name),
            Codec.INT.optionalFieldOf("order", 0).forGetter(EquipmentCategoryDefinition::order),
            ResourceLocation.CODEC.optionalFieldOf("icon", DEFAULT_ICON).forGetter(EquipmentCategoryDefinition::icon)
    ).apply(instance, EquipmentCategoryDefinition::new));
}

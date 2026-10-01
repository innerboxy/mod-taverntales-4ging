package ibx.taverntales.forging.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 数据驱动的装备分类定义,存于数据包注册表 {@code minecraft:category}(见 ModRegistries)。
 * 分类 id 即其文件的 Identifier(配方通过该 id 归类);此对象只描述展示信息。
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
 *              若指定的物品不存在,同样回退为书本
 */
public record EquipmentCategoryDefinition(Component name, int order, Identifier icon) {
    /** 未指定 icon 或其物品不存在时的图标 */
    public static final Identifier DEFAULT_ICON = Identifier.withDefaultNamespace("book");

    public static final Codec<EquipmentCategoryDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(EquipmentCategoryDefinition::name),
            Codec.INT.optionalFieldOf("order", 0).forGetter(EquipmentCategoryDefinition::order),
            Identifier.CODEC.optionalFieldOf("icon", DEFAULT_ICON).forGetter(EquipmentCategoryDefinition::icon)
    ).apply(instance, EquipmentCategoryDefinition::new));

    /** 展示用图标(锻造界面标签与 JEI 分类角标共用);icon 物品不存在(如未装对应模组)时回退为默认图标 */
    public ItemStack iconStack() {
        Item item = BuiltInRegistries.ITEM.getOptional(icon)
                .filter(i -> i != Items.AIR)
                .orElseGet(() -> BuiltInRegistries.ITEM.getValue(DEFAULT_ICON));
        return new ItemStack(item);
    }
}

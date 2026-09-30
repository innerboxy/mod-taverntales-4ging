package ibx.taverntales.forging.recipe.condition;

import com.mojang.serialization.MapCodec;
import ibx.taverntales.forging.config.Config;
import net.neoforged.neoforge.common.conditions.ICondition;

import java.util.function.BooleanSupplier;

/**
 * 数据包条件:由一个配置开关决定被包裹的条目是否加载。JSON 里不带参数,
 * 每个实例以自己的 {@link #name()} 注册为独立的条件类型(见 ModConditions)。
 */
public final class ConfigCondition implements ICondition {
    /**
     * 仅当配置"移除被取代的原版配方"为关闭时加载。用它覆盖被锻造台取代的原版配方文件——
     * 配置开启即让原版配方不加载(等效移除)。以后新增锻造配方时,把对应原版配方文件用此条件覆盖即可复用同一开关。
     */
    public static final ConfigCondition VANILLA_RECIPE_ENABLED = new ConfigCondition(
            "vanilla_recipe_enabled", () -> !Config.REMOVE_VANILLA_RECIPES.getAsBoolean());

    /** 仅当配置"启用默认锻造配方"为开启时加载。挂在模组自带的锻造配方上,供整合包一键停用默认配方表 */
    public static final ConfigCondition DEFAULT_RECIPES_ENABLED = new ConfigCondition(
            "default_recipes_enabled", Config.ENABLE_DEFAULT_RECIPES::getAsBoolean);

    /** 仅当配置"启用默认分类标签"为开启时加载。挂在模组自带的分类定义上(数据包注册表条目同样支持条件) */
    public static final ConfigCondition DEFAULT_CATEGORIES_ENABLED = new ConfigCondition(
            "default_categories_enabled", Config.ENABLE_DEFAULT_CATEGORIES::getAsBoolean);

    private final String name;
    private final BooleanSupplier enabled;
    private final MapCodec<ConfigCondition> codec;

    private ConfigCondition(String name, BooleanSupplier enabled) {
        this.name = name;
        this.enabled = enabled;
        this.codec = MapCodec.unit(this);
    }

    /** 条件类型 id 的路径部分 */
    public String name() {
        return name;
    }

    @Override
    public boolean test(IContext context) {
        return enabled.getAsBoolean();
    }

    @Override
    public MapCodec<ConfigCondition> codec() {
        return codec;
    }

    @Override
    public String toString() {
        return name;
    }
}

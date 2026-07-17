package ibx.taverntales.forging.recipe.condition;

import com.mojang.serialization.MapCodec;
import ibx.taverntales.forging.config.Config;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * 数据包条件:仅当配置"启用默认分类标签"为开启时,被包裹的分类才加载。
 * 挂在模组自带的分类定义上(数据包注册表条目同样支持条件),供整合包一键停用默认分类。
 */
public record DefaultCategoriesEnabledCondition() implements ICondition {
    public static final DefaultCategoriesEnabledCondition INSTANCE = new DefaultCategoriesEnabledCondition();
    public static final MapCodec<DefaultCategoriesEnabledCondition> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public boolean test(IContext context) {
        return Config.ENABLE_DEFAULT_CATEGORIES.getAsBoolean();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "default_categories_enabled";
    }
}

package ibx.taverntales.forging.recipe.condition;

import com.mojang.serialization.MapCodec;
import ibx.taverntales.forging.config.Config;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * 数据包条件:仅当配置"启用默认锻造配方"为开启时,被包裹的配方才加载。
 * 挂在模组自带的锻造配方上,供整合包一键停用默认配方表、改用自定义配方。
 */
public record DefaultRecipesEnabledCondition() implements ICondition {
    public static final DefaultRecipesEnabledCondition INSTANCE = new DefaultRecipesEnabledCondition();
    public static final MapCodec<DefaultRecipesEnabledCondition> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public boolean test(IContext context) {
        return Config.ENABLE_DEFAULT_RECIPES.getAsBoolean();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "default_recipes_enabled";
    }
}

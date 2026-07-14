package com.taverntales.equipmentforge.recipe.condition;

import com.mojang.serialization.MapCodec;
import com.taverntales.equipmentforge.config.Config;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * 数据包配方条件:仅当配置"移除被取代的原版配方"为关闭时,被包裹的配方才加载。
 * 用它覆盖被锻造台取代的原版配方文件——配置开启即让原版配方不加载(等效移除)。
 * 以后新增锻造配方时,把对应原版配方文件用此条件覆盖即可复用同一开关。
 */
public record VanillaRecipeEnabledCondition() implements ICondition {
    public static final VanillaRecipeEnabledCondition INSTANCE = new VanillaRecipeEnabledCondition();
    public static final MapCodec<VanillaRecipeEnabledCondition> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public boolean test(IContext context) {
        return !Config.REMOVE_VANILLA_RECIPES.getAsBoolean();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "vanilla_recipe_enabled";
    }
}

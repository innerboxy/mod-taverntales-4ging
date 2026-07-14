package com.taverntales.equipmentforge.registry;

import com.mojang.serialization.MapCodec;
import com.taverntales.equipmentforge.TavernTalesEquipmentForge;
import com.taverntales.equipmentforge.recipe.condition.VanillaRecipeEnabledCondition;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** 数据包配方条件的编解码器注册。 */
public class ModConditions {
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, TavernTalesEquipmentForge.MODID);

    public static final Object VANILLA_RECIPE_ENABLED =
            CONDITION_CODECS.register("vanilla_recipe_enabled",
                    () -> VanillaRecipeEnabledCondition.CODEC);
}

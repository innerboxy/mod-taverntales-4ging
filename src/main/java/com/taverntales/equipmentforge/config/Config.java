package com.taverntales.equipmentforge.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** 模组通用配置。 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /**
     * 是否移除被装备锻造台配方所取代的原版工作台合成配方,使这些物品只能通过锻造台制作。默认开启。
     * 一个开关统一控制所有被取代的原版配方。
     */
    public static final ModConfigSpec.BooleanValue REMOVE_VANILLA_RECIPES = BUILDER
            .comment("移除被装备锻造台取代的原版工作台合成配方,使这些物品只能通过装备锻造台制作。",
                    "Remove the vanilla crafting-table recipes that are replaced by the Equipment Forge,",
                    "so those items can only be made at the forge. One switch controls all of them.")
            .define("removeVanillaRecipes", true);

    public static final ModConfigSpec SPEC = BUILDER.build();
}

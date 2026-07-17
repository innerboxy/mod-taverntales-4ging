package ibx.taverntales.forging.config;

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

    /** 是否加载模组自带的默认分类标签。默认开启。 */
    public static final ModConfigSpec.BooleanValue ENABLE_DEFAULT_CATEGORIES = BUILDER
            .comment("加载模组自带的默认分类标签(近战/远程/魔法/工具/盔甲/盾牌/饰品)。",
                    "关闭后可由数据包完全自定义分类;仍引用这些分类的配方将只出现在「全部」标签中。",
                    "Load the mod's built-in category tabs. Disable to define categories entirely via datapack.")
            .define("enableDefaultCategories", true);

    /** 是否加载模组自带的默认锻造配方。默认开启。 */
    public static final ModConfigSpec.BooleanValue ENABLE_DEFAULT_RECIPES = BUILDER
            .comment("加载模组自带的默认锻造配方(木/石/铁/金/钻石/下界合金装备、弓弩、盾牌等)。",
                    "关闭后可由数据包完全自定义配方表。不影响装备锻造台方块自身的合成配方。",
                    "Load the mod's built-in forge recipes. Disable to define recipes entirely via datapack.",
                    "Does not affect the crafting recipe of the Equipment Forge block itself.")
            .define("enableDefaultRecipes", true);

    public static final ModConfigSpec SPEC = BUILDER.build();
}

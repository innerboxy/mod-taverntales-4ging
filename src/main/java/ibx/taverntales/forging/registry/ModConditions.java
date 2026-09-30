package ibx.taverntales.forging.registry;

import com.mojang.serialization.MapCodec;
import ibx.taverntales.forging.TavernTalesEquipmentForge;
import ibx.taverntales.forging.recipe.condition.ConfigCondition;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** 数据包配方条件的编解码器注册。 */
public class ModConditions {
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, TavernTalesEquipmentForge.MODID);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<ConfigCondition>> VANILLA_RECIPE_ENABLED =
            register(ConfigCondition.VANILLA_RECIPE_ENABLED);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<ConfigCondition>> DEFAULT_RECIPES_ENABLED =
            register(ConfigCondition.DEFAULT_RECIPES_ENABLED);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<ConfigCondition>> DEFAULT_CATEGORIES_ENABLED =
            register(ConfigCondition.DEFAULT_CATEGORIES_ENABLED);

    private static DeferredHolder<MapCodec<? extends ICondition>, MapCodec<ConfigCondition>> register(ConfigCondition condition) {
        return CONDITION_CODECS.register(condition.name(), condition::codec);
    }
}

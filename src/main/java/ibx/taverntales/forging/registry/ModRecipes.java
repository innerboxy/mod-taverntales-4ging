package ibx.taverntales.forging.registry;

import ibx.taverntales.forging.recipe.EquipmentForgeRecipe;
import ibx.taverntales.forging.TavernTalesEquipmentForge;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, TavernTalesEquipmentForge.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, TavernTalesEquipmentForge.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<EquipmentForgeRecipe>> EQUIPMENT_FORGE_TYPE =
            RECIPE_TYPES.register("equipment_forge",
                    () -> RecipeType.simple(Identifier.fromNamespaceAndPath(TavernTalesEquipmentForge.MODID, "equipment_forge")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<EquipmentForgeRecipe>> EQUIPMENT_FORGE_SERIALIZER =
            RECIPE_SERIALIZERS.register("equipment_forge", () -> EquipmentForgeRecipe.SERIALIZER);
}

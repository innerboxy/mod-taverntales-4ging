package com.taverntales.equipmentforge.compat.jei;

import com.taverntales.equipmentforge.TavernTalesEquipmentForge;
import com.taverntales.equipmentforge.client.screen.EquipmentForgeScreen;
import com.taverntales.equipmentforge.recipe.EquipmentForgeRecipe;
import com.taverntales.equipmentforge.registry.ModItems;
import com.taverntales.equipmentforge.registry.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import java.util.List;

@JeiPlugin
public class EquipmentForgeJeiPlugin implements IModPlugin {
    // JEI 在注册表事件之前就会类加载插件,这里不能调用 DeferredHolder.get()
    public static final RecipeType<RecipeHolder<EquipmentForgeRecipe>> EQUIPMENT_FORGE =
            RecipeType.createRecipeHolderType(
                    ResourceLocation.fromNamespaceAndPath(TavernTalesEquipmentForge.MODID, "equipment_forge"));

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(TavernTalesEquipmentForge.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new EquipmentForgeRecipeCategory(
                registration.getJeiHelpers().getGuiHelper(), maxMaterialCount()));
    }

    /** 已加载配方中最多的材料数,决定 JEI 分类的行数(分类尺寸无法逐配方变化);无配方时至少留一行 */
    private static int maxMaterialCount() {
        return loadedRecipes().stream()
                .mapToInt(h -> h.value().materials().size())
                .max()
                .orElse(1);
    }

    private static List<RecipeHolder<EquipmentForgeRecipe>> loadedRecipes() {
        Level level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.EQUIPMENT_FORGE_TYPE.get());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(EQUIPMENT_FORGE, loadedRecipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.EQUIPMENT_FORGE.get()), EQUIPMENT_FORGE);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        // 「+」按钮:在打开的锻造台里选中该配方(不移动物品,材料取自背包/网络)
        registration.addRecipeTransferHandler(new EquipmentForgeTransferHandler(), EQUIPMENT_FORGE);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // 左侧分类子标签突出在 GUI 之外,让 JEI 书签面板避开这块区域
        registration.addGuiContainerHandler(EquipmentForgeScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(EquipmentForgeScreen screen) {
                return List.of(screen.getTabArea());
            }
        });
    }
}

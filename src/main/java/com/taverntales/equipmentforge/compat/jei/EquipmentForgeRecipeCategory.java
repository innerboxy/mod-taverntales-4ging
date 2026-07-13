package com.taverntales.equipmentforge.compat.jei;

import com.taverntales.equipmentforge.recipe.EquipmentForgeRecipe;
import com.taverntales.equipmentforge.registry.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.Arrays;
import java.util.List;

/**
 * JEI 展示:左侧最多 3x3 材料格(SizedIngredient 自带数量),箭头,右侧产物。
 */
public class EquipmentForgeRecipeCategory extends AbstractRecipeCategory<RecipeHolder<EquipmentForgeRecipe>> {
    private static final int INPUT_COLS = 4;
    private static final int WIDTH = 130;
    private static final int HEIGHT = 56; // 材料最多展示 3 行 x 4 列

    private final IDrawableStatic arrow;

    public EquipmentForgeRecipeCategory(IGuiHelper guiHelper) {
        super(EquipmentForgeJeiPlugin.EQUIPMENT_FORGE,
                Component.translatable("taverntales_4ging.jei.equipment_forge"),
                guiHelper.createDrawableItemLike(ModBlocks.EQUIPMENT_FORGE.get()),
                WIDTH, HEIGHT);
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<EquipmentForgeRecipe> holder, IFocusGroup focuses) {
        List<SizedIngredient> materials = holder.value().materials();
        // 材料格按行数垂直居中,与箭头、产物保持同一水平中线
        int rows = (materials.size() + INPUT_COLS - 1) / INPUT_COLS;
        int startY = Math.max(1, (HEIGHT - rows * 18) / 2 + 1);
        for (int i = 0; i < materials.size(); i++) {
            int x = (i % INPUT_COLS) * 18 + 1;
            int y = startY + (i / INPUT_COLS) * 18;
            builder.addInputSlot(x, y)
                    .setStandardSlotBackground()
                    // getItems() 已按 count 设置堆叠数量,标签材料展示全部可选项
                    .addItemStacks(Arrays.asList(materials.get(i).getItems()));
        }
        builder.addOutputSlot(108, HEIGHT / 2 - 8)
                .setOutputSlotBackground()
                .addItemStack(holder.value().getResultItem(null));
    }

    @Override
    public void draw(RecipeHolder<EquipmentForgeRecipe> holder, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, 78, HEIGHT / 2 - 9);
    }
}

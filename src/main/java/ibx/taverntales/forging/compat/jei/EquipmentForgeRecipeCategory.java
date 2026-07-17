package ibx.taverntales.forging.compat.jei;

import ibx.taverntales.forging.recipe.EquipmentForgeRecipe;
import ibx.taverntales.forging.registry.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.Arrays;
import java.util.List;

/**
 * JEI 展示:左上角大输出槽放产物,右侧「所需材料:」标题,下方每行 5 格的材料区。
 * 锻造台没有摆放形状,材料只是一份清单,所以不画合成箭头。
 *
 * <p>JEI 的分类尺寸是整个分类共用一个({@code getWidth/getHeight} 不带配方参数,
 * 且在 {@code AbstractRecipeCategory} 中为 final),无法逐配方变高。因此这里在构造时
 * 按"当前已加载配方中材料最多的那条"算出行数——对数据包新增的配方同样自适应。
 */
public class EquipmentForgeRecipeCategory extends AbstractRecipeCategory<RecipeHolder<EquipmentForgeRecipe>> {
    /** 每行 5 格 */
    private static final int INPUT_COLS = 5;

    /** 产物槽:背景 26x26 偏移 (-5,-5),故物品放在 (8,8) 时背景占 (3,3)-(29,29) */
    private static final int RESULT_X = 8;
    private static final int RESULT_Y = 8;
    /** 「所需材料:」与产物槽垂直居中(槽背景 3..29,字高 9) */
    private static final int LABEL_X = 38;
    private static final int LABEL_Y = 12;
    /** 材料格物品坐标;18x18 背景自动向左上偏移 1px,故背景从 (3,36) 起 */
    private static final int MAT_X = 4;
    private static final int MAT_Y = 37;

    /** 宽度取"标题右缘"与"材料格右缘"的较大者;标题较长(英文 Materials:),故由它决定 */
    private static final int WIDTH = 96;

    private static final Component MATERIALS_LABEL =
            Component.translatable("gui.taverntales_4ging.materials");

    private final int rows;

    /** @param maxMaterials 所有已加载配方中最多的材料数,用于决定分类高度 */
    public EquipmentForgeRecipeCategory(IGuiHelper guiHelper, int maxMaterials) {
        super(EquipmentForgeJeiPlugin.EQUIPMENT_FORGE,
                Component.translatable("jei.taverntales_4ging.equipment_forge"),
                guiHelper.createDrawableItemLike(ModBlocks.EQUIPMENT_FORGE.get()),
                WIDTH, heightFor(maxMaterials));
        this.rows = rowsFor(maxMaterials);
    }

    private static int rowsFor(int maxMaterials) {
        return Math.max(1, (maxMaterials + INPUT_COLS - 1) / INPUT_COLS);
    }

    private static int heightFor(int maxMaterials) {
        return MAT_Y - 1 + rowsFor(maxMaterials) * 18 + 3;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<EquipmentForgeRecipe> holder, IFocusGroup focuses) {
        builder.addOutputSlot(RESULT_X, RESULT_Y)
                .setOutputSlotBackground()
                .addItemStack(holder.value().getResultItem(null));

        List<SizedIngredient> materials = holder.value().materials();
        int shown = Math.min(materials.size(), INPUT_COLS * rows);
        for (int i = 0; i < shown; i++) {
            // 行优先:每行填满 INPUT_COLS 格再换下一行
            int x = MAT_X + (i % INPUT_COLS) * 18;
            int y = MAT_Y + (i / INPUT_COLS) * 18;
            builder.addInputSlot(x, y)
                    .setStandardSlotBackground()
                    // getItems() 已按 count 设置堆叠数量,标签材料展示全部可选项
                    .addItemStacks(Arrays.asList(materials.get(i).getItems()));
        }
    }

    @Override
    public void draw(RecipeHolder<EquipmentForgeRecipe> holder, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        guiGraphics.drawString(Minecraft.getInstance().font, MATERIALS_LABEL,
                LABEL_X, LABEL_Y, 0xFF404040, false);
    }
}

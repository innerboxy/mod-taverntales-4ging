package ibx.taverntales.forging.compat.jei;

import ibx.taverntales.forging.recipe.EquipmentCategoryDefinition;
import ibx.taverntales.forging.recipe.EquipmentForgeRecipe;
import ibx.taverntales.forging.registry.ModBlocks;
import ibx.taverntales.forging.registry.ModRegistries;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;
import java.util.Optional;

/**
 * JEI 展示:左上角大输出槽放产物,右上角一枚缩小的分类图标,下方每行 5 格的材料区。
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
    /** 右上角的分类图标:16x16 缩放到 12x12,右缘与材料格背景右缘(93)对齐 */
    private static final float CAT_ICON_SCALE = 0.75f;
    private static final int CAT_ICON_SIZE = 12;
    private static final int CAT_ICON_X = 93 - CAT_ICON_SIZE;
    private static final int CAT_ICON_Y = 3;
    /** 材料格物品坐标;18x18 背景自动向左上偏移 1px,故背景从 (3,36) 起 */
    private static final int MAT_X = 4;
    private static final int MAT_Y = 37;

    /** 宽度由每行 5 格的材料区决定(背景 3..93,右侧留 3px) */
    private static final int WIDTH = 96;

    private final int rows;

    /** @param maxMaterials 所有已加载配方中最多的材料数,用于决定分类高度 */
    public EquipmentForgeRecipeCategory(IGuiHelper guiHelper, int maxMaterials) {
        super(EquipmentForgeJeiPlugin.EQUIPMENT_FORGE,
                Component.translatable("jei.taverntales.equipment_forge"),
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
                .add(holder.value().resultView());

        List<SizedIngredient> materials = holder.value().materials();
        int shown = Math.min(materials.size(), INPUT_COLS * rows);
        for (int i = 0; i < shown; i++) {
            // 行优先:每行填满 INPUT_COLS 格再换下一行
            int x = MAT_X + (i % INPUT_COLS) * 18;
            int y = MAT_Y + (i / INPUT_COLS) * 18;
            IRecipeSlotBuilder slot = builder.addInputSlot(x, y).setStandardSlotBackground();
            slot.addItemStacks(displayStacks(materials.get(i), slot.getContextMap()));
        }
    }

    /**
     * 材料的全部可选物品(数量已设为所需数);标签材料展开为标签内所有物品。
     * 按 SlotDisplay 解析(26.1 起 Ingredient#items 已过时),context 取自 JEI 槽位的 getContextMap()。
     */
    private static List<ItemStack> displayStacks(SizedIngredient material, ContextMap context) {
        return material.ingredient().display().resolveForStacks(context).stream()
                .map(stack -> stack.copyWithCount(material.count()))
                .toList();
    }

    @Override
    public void draw(RecipeHolder<EquipmentForgeRecipe> holder, IRecipeSlotsView recipeSlotsView,
                     GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        // 右上角的分类角标:只是绘制的图标,不是 JEI 槽位(无悬停提示/高亮)
        ItemStack icon = categoryIcon(holder);
        if (icon.isEmpty()) return;
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(CAT_ICON_X, CAT_ICON_Y);
        pose.scale(CAT_ICON_SCALE, CAT_ICON_SCALE);
        graphics.item(icon, 0, 0);
        pose.popMatrix();
    }

    /** 悬停在分类角标上时,提示所属分类名 */
    @Override
    public void getTooltip(ITooltipBuilder tooltip, RecipeHolder<EquipmentForgeRecipe> holder,
                           IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (mouseX < CAT_ICON_X || mouseX >= CAT_ICON_X + CAT_ICON_SIZE
                || mouseY < CAT_ICON_Y || mouseY >= CAT_ICON_Y + CAT_ICON_SIZE) {
            return;
        }
        categoryDef(holder).ifPresent(def -> tooltip.add(
                Component.translatable("jei.taverntales.category", def.name())));
    }

    /** 配方所属分类的图标;分类未在数据包注册表中定义时为空 */
    private static ItemStack categoryIcon(RecipeHolder<EquipmentForgeRecipe> holder) {
        return categoryDef(holder)
                .map(EquipmentCategoryDefinition::iconStack)
                .orElse(ItemStack.EMPTY);
    }

    /** 配方所属分类的定义;分类未在数据包注册表中定义时为空 */
    private static Optional<EquipmentCategoryDefinition> categoryDef(RecipeHolder<EquipmentForgeRecipe> holder) {
        var level = Minecraft.getInstance().level;
        if (level == null) return Optional.empty();
        return level.registryAccess().lookup(ModRegistries.EQUIPMENT_CATEGORY)
                .map(registry -> registry.getValue(holder.value().category()));
    }
}

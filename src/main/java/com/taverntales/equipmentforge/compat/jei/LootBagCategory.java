package com.taverntales.equipmentforge.compat.jei;

import com.taverntales.equipmentforge.lootbag.LootBagDrop;
import com.taverntales.equipmentforge.lootbag.LootBagItem;
import com.taverntales.equipmentforge.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * JEI 展示:左上角是袋子本身、右侧写它的名字,下方网格列出它可能掉出的东西,
 * 每格 tooltip 附上概率与数量区间。
 *
 * <p>与锻造分类同理,JEI 的分类尺寸整个分类共用一个({@code getWidth/getHeight} 不带参数且
 * 在 {@code AbstractRecipeCategory} 中为 final),故构造时就得按"掉落项最多的袋子"算行数、
 * 按"名字最长的袋子"算宽度。
 */
public class LootBagCategory extends AbstractRecipeCategory<LootBagDisplay> {
    /** 每行 5 格,与锻造分类的材料区一致 */
    private static final int COLS = 5;

    /** 袋子槽:18x18 背景向左上偏移 1px,故背景占 (3,3)-(21,21) */
    private static final int BAG_X = 4;
    private static final int BAG_Y = 4;
    /** 袋子名:紧挨袋子槽右侧,与其垂直居中(槽 3..21 中心 12,字高 9) */
    private static final int NAME_X = 26;
    private static final int NAME_Y = 8;

    /** 掉落网格起点 */
    private static final int DROP_X = 4;
    private static final int DROP_Y = 26;

    /** 掉落网格自身所需宽度 */
    private static final int GRID_WIDTH = DROP_X - 1 + COLS * 18 + 3;

    private final int rows;

    public LootBagCategory(IGuiHelper guiHelper, List<LootBagDisplay> displays) {
        super(EquipmentForgeJeiPlugin.LOOT_BAG,
                Component.translatable("jei.taverntales_4ging.loot_bag"),
                guiHelper.createDrawableItemLike(ModItems.LOOT_BAG.get()),
                widthFor(displays), heightFor(displays));
        this.rows = rowsFor(displays);
    }

    private static int maxDrops(List<LootBagDisplay> displays) {
        return displays.stream().mapToInt(d -> d.drops().size()).max().orElse(1);
    }

    private static int rowsFor(List<LootBagDisplay> displays) {
        int max = maxDrops(displays);
        return Math.max(1, (max + COLS - 1) / COLS);
    }

    private static int heightFor(List<LootBagDisplay> displays) {
        return DROP_Y - 1 + rowsFor(displays) * 18 + 3;
    }

    /** 宽度取"掉落网格"与"最长袋子名"的较大者——名字由数据包定义,可能比网格还长 */
    private static int widthFor(List<LootBagDisplay> displays) {
        var font = Minecraft.getInstance().font;
        int widest = displays.stream()
                .mapToInt(d -> NAME_X + font.width(LootBagItem.displayName(d.type())) + 3)
                .max()
                .orElse(0);
        return Math.max(GRID_WIDTH, widest);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, LootBagDisplay display, IFocusGroup focuses) {
        // 袋子自身带着 loot_bag_type 组件,所以它的 tooltip 就是袋子的显示名
        builder.addInputSlot(BAG_X, BAG_Y)
                .setStandardSlotBackground()
                .addItemStack(display.bagStack());

        List<LootBagDrop> drops = display.drops();
        int shown = Math.min(drops.size(), COLS * rows);
        for (int i = 0; i < shown; i++) {
            int x = DROP_X + (i % COLS) * 18;
            int y = DROP_Y + (i / COLS) * 18;
            LootBagDrop drop = drops.get(i);
            builder.addOutputSlot(x, y)
                    .setStandardSlotBackground()
                    // 堆叠数显示最小数量;夹到 1 是因为 min 可能为 0(带概率的条目),
                    // 而 ItemStack 在 count<=0 时 isEmpty(),格子会渲染成空的。
                    // 0 和 1 都只显示物品不画数字,上限交给 tooltip 说。
                    .addItemStack(drop.item().copyWithCount(Math.max(1, drop.min())))
                    .addRichTooltipCallback((view, tooltip) -> {
                        tooltip.add(Component.translatable("jei.taverntales_4ging.loot_bag.chance",
                                formatChance(drop.chance())).withStyle(ChatFormatting.GRAY));
                        tooltip.add(Component.translatable("jei.taverntales_4ging.loot_bag.count",
                                formatCount(drop)).withStyle(ChatFormatting.GRAY));
                    });
        }
    }

    @Override
    public void draw(LootBagDisplay display, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // 名字自带 § 颜色码,这里给的默认色只在译文没写颜色时生效
        guiGraphics.drawString(Minecraft.getInstance().font, LootBagItem.displayName(display.type()),
                NAME_X, NAME_Y, 0xFF404040, false);
    }

    /** 概率:整数就不显示小数,免得满屏 100.0% */
    private static String formatChance(float chance) {
        float percent = chance * 100.0f;
        return percent == Math.rint(percent)
                ? String.valueOf((int) percent)
                : String.format("%.1f", percent);
    }

    private static String formatCount(LootBagDrop drop) {
        return drop.min() == drop.max()
                ? String.valueOf(drop.min())
                : drop.min() + "-" + drop.max();
    }
}

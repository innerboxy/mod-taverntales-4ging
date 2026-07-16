package com.taverntales.equipmentforge.client.screen;

import com.taverntales.equipmentforge.TavernTalesEquipmentForge;
import com.taverntales.equipmentforge.client.PinyinSearch;
import com.taverntales.equipmentforge.menu.EquipmentForgeMenu;
import com.taverntales.equipmentforge.network.CraftEquipmentPayload;
import com.taverntales.equipmentforge.recipe.EquipmentCategoryDefinition;
import com.taverntales.equipmentforge.recipe.EquipmentForgeRecipe;
import com.taverntales.equipmentforge.registry.ModRecipes;
import com.taverntales.equipmentforge.registry.ModRegistries;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class EquipmentForgeScreen extends AbstractContainerScreen<EquipmentForgeMenu> {
    /*
     * 布局:整体 176 宽(与原版 GUI 一致,仅比 162 的背包栏稍宽)。
     * 左栏(搜索框 + 装备选择)与右栏(所需材料 + 制作按钮)各 74px,左右对称:
     *   左栏内嵌板 x=6..80,右栏内嵌板 x=96..170,两侧边距均为 6。
     * 搜索框与"所需材料"标题上下对齐;两栏内嵌板顶部对齐(y=25);
     * 制作按钮底边与装备选择栏底边对齐,且在右栏水平居中。
     */
    private static final int GRID_COLS = 4;
    private static final int GRID_ROWS = 6;
    private static final int CELL = 18;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 26;

    /** 右栏(与左栏对称:左嵌板 7..81,右嵌板 95..169,两侧边距均 7) */
    private static final int RIGHT_X = 96;
    private static final int RIGHT_WIDTH = GRID_COLS * CELL;
    /** 制作按钮:20x20 方形贴图按钮,底边对齐装备格底边;按钮 + 右侧制作物品图标整体在右栏居中 */
    private static final int BUTTON_WIDTH = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_ICON_GAP = 16;
    private static final int BUTTON_X = RIGHT_X + (RIGHT_WIDTH - BUTTON_WIDTH - BUTTON_ICON_GAP - 16) / 2;
    private static final int BUTTON_Y = GRID_Y + GRID_ROWS * CELL + 1 - BUTTON_HEIGHT;

    /** 材料框:顶部与装备格对齐,底部止于制作按钮上方 4px */
    private static final int MAT_PANEL_Y = GRID_Y - 1;
    private static final int MAT_PANEL_H = BUTTON_Y - 4 - MAT_PANEL_Y;
    /** 最大可见行数;这几行在材料框内垂直居中,上下留出的空隙正好放滚动箭头 */
    private static final int MAX_MAT_ROWS = 4;
    private static final int MAT_ROW_Y = MAT_PANEL_Y + (MAT_PANEL_H - MAX_MAT_ROWS * CELL) / 2;
    private static final int MAT_ROWS_END = MAT_ROW_Y + MAX_MAT_ROWS * CELL;

    /** 上/下滚动箭头(5x3 贴图):右栏水平居中,分别嵌在材料行上下的空隙中 */
    private static final int ARROW_W = 5;
    private static final int ARROW_H = 3;
    private static final int ARROW_X = RIGHT_X + (RIGHT_WIDTH - ARROW_W) / 2;
    private static final int ARROW_UP_Y = MAT_PANEL_Y + (MAT_ROW_Y - MAT_PANEL_Y - ARROW_H) / 2;
    private static final int ARROW_DOWN_Y =
            MAT_ROWS_END + (MAT_PANEL_Y + MAT_PANEL_H - MAT_ROWS_END - ARROW_H) / 2;

    /** 可合成过滤开关(原版配方书 26x16 过滤按钮):位于搜索框右侧,与装备格右缘对齐 */
    private static final int FILTER_W = 26;
    private static final int FILTER_H = 16;
    private static final int FILTER_X = GRID_X + GRID_COLS * CELL - FILTER_W;
    private static final int FILTER_Y = 7;

    /** 清除所选按钮(X):位于"所需材料:"右侧,与材料框右缘对齐 */
    private static final int CLEAR_SIZE = 12;
    private static final int CLEAR_X = RIGHT_X + RIGHT_WIDTH - CLEAR_SIZE;
    private static final int CLEAR_Y = 9;

    /** 左侧分类子标签,顶部比搜索框上缘高 5px */
    private static final int TAB_SIZE = 20;
    private static final int TAB_GAP = 2;
    private static final int TAB_Y0 = 3;
    /** 16x16 图标在标签内的留白,随 TAB_SIZE 自动居中 */
    private static final int TAB_ICON_INSET = (TAB_SIZE - 16) / 2;

    private static final Component MATERIALS_LABEL = Component.translatable("gui.taverntales_4ging.materials");
    private static final Component CRAFT_LABEL = Component.translatable("gui.taverntales_4ging.craft");
    private static final Component SEARCH_HINT = Component.translatable("gui.taverntales_4ging.search");
    private static final Component SELECT_HINT = Component.translatable("gui.taverntales_4ging.select_hint");
    private static final Component FILTER_ALL_LABEL = Component.translatable("gui.taverntales_4ging.filter_all");
    private static final Component FILTER_CRAFTABLE_LABEL = Component.translatable("gui.taverntales_4ging.filter_craftable");

    private static final ResourceLocation BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            TavernTalesEquipmentForge.MODID, "textures/gui/equipment_forge_button.png");
    private static final ResourceLocation BUTTON_TEXTURE_CLICKED = ResourceLocation.fromNamespaceAndPath(
            TavernTalesEquipmentForge.MODID, "textures/gui/equipment_forge_button_clicked.png");

    /** 材料列表的上/下滚动箭头贴图 */
    private static final ResourceLocation ARROW_UP_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            TavernTalesEquipmentForge.MODID, "textures/gui/equipment_forge_gui_up.png");
    private static final ResourceLocation ARROW_DOWN_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            TavernTalesEquipmentForge.MODID, "textures/gui/equipment_forge_gui_down.png");

    /** 原版按钮底座 sprite(九宫格拉伸) */
    private static final ResourceLocation BUTTON_BASE_SPRITE =
            ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation BUTTON_BASE_HIGHLIGHTED_SPRITE =
            ResourceLocation.withDefaultNamespace("widget/button_highlighted");

    /** 原版配方书槽位 sprite(25x25,缩放到 18x18 绘制) */
    private static final ResourceLocation SLOT_CRAFTABLE_SPRITE =
            ResourceLocation.withDefaultNamespace("recipe_book/slot_craftable");
    private static final ResourceLocation SLOT_UNCRAFTABLE_SPRITE =
            ResourceLocation.withDefaultNamespace("recipe_book/slot_uncraftable");

    /** 原版配方书过滤按钮 sprite */
    private static final ResourceLocation FILTER_ENABLED_SPRITE =
            ResourceLocation.withDefaultNamespace("recipe_book/filter_enabled");
    private static final ResourceLocation FILTER_ENABLED_HIGHLIGHTED_SPRITE =
            ResourceLocation.withDefaultNamespace("recipe_book/filter_enabled_highlighted");
    private static final ResourceLocation FILTER_DISABLED_SPRITE =
            ResourceLocation.withDefaultNamespace("recipe_book/filter_disabled");
    private static final ResourceLocation FILTER_DISABLED_HIGHLIGHTED_SPRITE =
            ResourceLocation.withDefaultNamespace("recipe_book/filter_disabled_highlighted");

    /** 内置"全部"标签的翻译名与图标 */
    private static final Component ALL_LABEL = Component.translatable("category.taverntales_4ging.all");
    private static final ItemStack ALL_ICON = new ItemStack(Items.COMPASS);

    /**
     * 内置分类的图标回退:当分类数据里的 icon 物品不存在时使用
     * (未装对应模组,或被数据包覆盖成了无效物品)。未登记的分类一律回退书本。
     */
    private static final Map<ResourceLocation, Item> FALLBACK_ICONS = Map.of(
            categoryId("melee"), Items.IRON_SWORD,
            categoryId("ranged"), Items.BOW,
            categoryId("magic"), Items.KNOWLEDGE_BOOK,
            categoryId("tool"), Items.DIAMOND_PICKAXE,
            categoryId("armor"), Items.GOLDEN_CHESTPLATE,
            categoryId("shield"), Items.SHIELD,
            categoryId("curio"), Items.ELYTRA);

    private static ResourceLocation categoryId(String path) {
        return ResourceLocation.fromNamespaceAndPath(TavernTalesEquipmentForge.MODID, path);
    }

    /** 分类子标签,category 为 null 表示"全部" */
    private record CategoryTab(ResourceLocation category, ItemStack icon, Component label) {}

    /** 标签列表:内置"全部"置顶,其余由数据包注册表驱动,init 时构建 */
    private List<CategoryTab> tabs = List.of();

    /** 当前选中的分类 id,null 表示"全部" */
    private ResourceLocation activeCategory;
    /** 是否仅显示可合成配方 */
    private boolean craftableOnly;

    private List<RecipeHolder<EquipmentForgeRecipe>> allRecipes = List.of();
    private List<RecipeHolder<EquipmentForgeRecipe>> filtered = List.of();
    /** 配方 id -> [原文/全拼/首字母] 检索键 */
    private final Map<ResourceLocation, String[]> searchKeys = new HashMap<>();
    private final Set<ResourceLocation> craftableIds = new HashSet<>();
    private RecipeHolder<EquipmentForgeRecipe> selected;
    private int scrollRow;

    /** 创造模式搜索页顺序,物品 -> 序号;配方与材料排序共用 */
    private Map<Item, Integer> creativeOrder = Map.of();
    /** 已排序的材料列表缓存(按稀有度、再按创造物品栏顺序),随选中配方变化重算 */
    private RecipeHolder<EquipmentForgeRecipe> materialsCachedFor;
    private List<SizedIngredient> sortedMaterials = List.of();
    /** 材料列表的滚动行数(材料多于 MAX_MAT_ROWS 时可滚动) */
    private int matScrollRow;

    private EditBox searchBox;
    private Button craftButton;

    public EquipmentForgeScreen(EquipmentForgeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 227;
    }

    @Override
    protected void init() {
        super.init();

        // 搜索框缩短,给右侧的可合成过滤开关留出位置
        searchBox = new EditBox(font, leftPos + GRID_X + 1, topPos + 8,
                GRID_COLS * CELL - FILTER_W - 6, 14, SEARCH_HINT);
        searchBox.setHint(SEARCH_HINT);
        searchBox.setResponder(text -> refreshFiltered());
        addRenderableWidget(searchBox);

        craftButton = new CraftIconButton(leftPos + BUTTON_X, topPos + BUTTON_Y, b -> sendCraft());
        addRenderableWidget(craftButton);

        buildTabs();

        // 排序:先按结果物品稀有度(普通→史诗),再按创造模式物品栏(搜索页)顺序
        creativeOrder = buildCreativeOrder();
        var registries = minecraft.level.registryAccess();
        allRecipes = minecraft.level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.EQUIPMENT_FORGE_TYPE.get())
                .stream()
                .sorted(Comparator
                        .comparingInt((RecipeHolder<EquipmentForgeRecipe> h) ->
                                h.value().getResultItem(registries).getRarity().ordinal())
                        .thenComparingInt(h -> creativeOrder.getOrDefault(
                                h.value().getResultItem(registries).getItem(), Integer.MAX_VALUE))
                        .thenComparing(h -> h.id().toString()))
                .toList();
        searchKeys.clear();
        for (RecipeHolder<EquipmentForgeRecipe> holder : allRecipes) {
            String name = holder.value().getResultItem(minecraft.level.registryAccess()).getHoverName().getString();
            searchKeys.put(holder.id(), PinyinSearch.searchKeys(name));
        }
        refreshCraftable();
        refreshFiltered();
        warnUnknownCategories();
    }

    /** 从数据包注册表构建标签:内置"全部"置顶,其余按 order、再按 id 字母序 */
    private void buildTabs() {
        List<CategoryTab> list = new ArrayList<>();
        list.add(new CategoryTab(null, ALL_ICON, ALL_LABEL));
        minecraft.level.registryAccess()
                .registry(ModRegistries.EQUIPMENT_CATEGORY)
                .ifPresent(registry -> registry.entrySet().stream()
                        .sorted(Comparator
                                .comparingInt((Map.Entry<ResourceKey<EquipmentCategoryDefinition>, EquipmentCategoryDefinition> e) ->
                                        e.getValue().order())
                                .thenComparing(e -> e.getKey().location().toString()))
                        .forEach(e -> {
                            EquipmentCategoryDefinition def = e.getValue();
                            ResourceLocation id = e.getKey().location();
                            // icon 物品不存在(如未装对应模组)时,用硬编码的回退图标
                            Item icon = existingItem(def.icon())
                                    .orElseGet(() -> FALLBACK_ICONS.getOrDefault(id, Items.BOOK));
                            list.add(new CategoryTab(id, new ItemStack(icon), def.name()));
                        }));
        tabs = List.copyOf(list);
    }

    /** 注册表中存在且非空气的物品 */
    private static java.util.Optional<Item> existingItem(ResourceLocation id) {
        return BuiltInRegistries.ITEM.getOptional(id).filter(item -> item != Items.AIR);
    }

    /** 配方引用了未定义的分类 id 时打调试日志(这类配方只会出现在"全部"中) */
    private void warnUnknownCategories() {
        Set<ResourceLocation> known = new HashSet<>();
        for (CategoryTab tab : tabs) {
            if (tab.category() != null) known.add(tab.category());
        }
        for (RecipeHolder<EquipmentForgeRecipe> holder : allRecipes) {
            ResourceLocation cat = holder.value().category();
            if (!known.contains(cat)) {
                TavernTalesEquipmentForge.LOGGER.debug(
                        "配方 {} 的分类 {} 未定义,仅在\"全部\"标签中显示", holder.id(), cat);
            }
        }
    }

    @Override
    protected void containerTick() {
        Set<ResourceLocation> before = craftableOnly ? new HashSet<>(craftableIds) : null;
        refreshCraftable();
        // 开启过滤时,背包变动导致可合成集合变化要即时反映到列表
        if (craftableOnly && !craftableIds.equals(before)) {
            refreshFiltered();
        }
    }

    /** 创造模式搜索页的物品顺序,物品 -> 序号 */
    private Map<Item, Integer> buildCreativeOrder() {
        Map<Item, Integer> order = new HashMap<>();
        try {
            CreativeModeTabs.tryRebuildTabContents(
                    minecraft.player.connection.enabledFeatures(), false, minecraft.level.registryAccess());
        } catch (Exception e) {
            TavernTalesEquipmentForge.LOGGER.debug("重建创造物品栏内容失败,使用现有缓存", e);
        }
        int index = 0;
        for (ItemStack stack : CreativeModeTabs.searchTab().getDisplayItems()) {
            order.putIfAbsent(stack.getItem(), index++);
        }
        return order;
    }

    private void refreshCraftable() {
        craftableIds.clear();
        for (RecipeHolder<EquipmentForgeRecipe> holder : allRecipes) {
            if (holder.value().canCraft(menu.playerInventory, menu.netItems())) {
                craftableIds.add(holder.id());
            }
        }
    }

    private void refreshFiltered() {
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<RecipeHolder<EquipmentForgeRecipe>> list = new ArrayList<>();
        for (RecipeHolder<EquipmentForgeRecipe> holder : allRecipes) {
            if (activeCategory != null && !activeCategory.equals(holder.value().category())) continue;
            if (craftableOnly && !craftableIds.contains(holder.id())) continue;
            if (!query.isEmpty()) {
                String[] keys = searchKeys.get(holder.id());
                boolean matches = (keys != null && PinyinSearch.matches(keys, query))
                        || holder.id().getPath().contains(query);
                if (!matches) continue;
            }
            list.add(holder);
        }
        filtered = list;
        scrollRow = Math.min(scrollRow, maxScrollRow());
    }

    private int maxScrollRow() {
        return Math.max(0, (filtered.size() + GRID_COLS - 1) / GRID_COLS - GRID_ROWS);
    }

    /** JEI 物品转移的待选配方 id;在锻造界面下次渲染时消费(此时 JEI 配方界面已关闭) */
    private static ResourceLocation pendingSelect;

    /** 由 JEI 转移处理器调用:请求下次打开/返回锻造界面时选中该配方 */
    public static void requestSelect(ResourceLocation recipeId) {
        pendingSelect = recipeId;
    }

    /**
     * 在锻造界面中选中指定配方并使其可见。
     * 清空搜索、取消"仅可合成"过滤,并切换到该配方的分类(未知分类则回到"全部"),再滚动到该项。
     */
    private void selectRecipe(ResourceLocation recipeId) {
        for (RecipeHolder<EquipmentForgeRecipe> holder : allRecipes) {
            if (!holder.id().equals(recipeId)) continue;
            selected = holder;
            if (searchBox != null) searchBox.setValue("");
            craftableOnly = false;
            ResourceLocation cat = holder.value().category();
            boolean hasTab = tabs.stream().anyMatch(t -> cat.equals(t.category()));
            activeCategory = hasTab ? cat : null;
            refreshFiltered();
            int idx = filtered.indexOf(holder);
            if (idx >= 0) {
                scrollRow = Math.max(0, Math.min(maxScrollRow(), idx / GRID_COLS - GRID_ROWS / 2));
            }
            return;
        }
    }

    private void sendCraft() {
        if (selected != null && craftableIds.contains(selected.id())) {
            PacketDistributor.sendToServer(new CraftEquipmentPayload(selected.id()));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 消费 JEI 物品转移请求(此时已返回锻造界面)
        if (pendingSelect != null) {
            ResourceLocation id = pendingSelect;
            pendingSelect = null;
            selectRecipe(id);
        }
        craftButton.active = selected != null && craftableIds.contains(selected.id());
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderGrid(guiGraphics, mouseX, mouseY);
        renderMaterials(guiGraphics);
        renderTooltip(guiGraphics, mouseX, mouseY); // 背包槽位 tooltip
        renderGridTooltip(guiGraphics, mouseX, mouseY);
        renderMaterialTooltip(guiGraphics, mouseX, mouseY);
        int tab = tabIndexAt(mouseX, mouseY);
        if (tab >= 0) {
            guiGraphics.renderTooltip(font, tabs.get(tab).label(), mouseX, mouseY);
        }
        if (isFilterHovered(mouseX, mouseY)) {
            guiGraphics.renderTooltip(font,
                    craftableOnly ? FILTER_CRAFTABLE_LABEL : FILTER_ALL_LABEL, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 主面板
        drawPanel(guiGraphics, leftPos, topPos, imageWidth, imageHeight, 0xFFC6C6C6);
        // 左侧物品区
        drawInset(guiGraphics, leftPos + GRID_X - 1, topPos + GRID_Y - 1,
                GRID_COLS * CELL + 2, GRID_ROWS * CELL + 2);
        // 右侧材料区:顶部与装备选择栏对齐,底部止于制作按钮上方
        drawInset(guiGraphics, leftPos + RIGHT_X - 1, topPos + MAT_PANEL_Y,
                RIGHT_WIDTH + 2, MAT_PANEL_H);
        renderTabs(guiGraphics);
        renderFilterToggle(guiGraphics, mouseX, mouseY);
        renderClearButton(guiGraphics, mouseX, mouseY);
        // 玩家背包 + 快捷栏底格
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawInset(guiGraphics,
                        leftPos + EquipmentForgeMenu.INV_X - 1 + col * 18,
                        topPos + EquipmentForgeMenu.INV_Y - 1 + row * 18, 18, 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawInset(guiGraphics,
                    leftPos + EquipmentForgeMenu.INV_X - 1 + col * 18,
                    topPos + EquipmentForgeMenu.HOTBAR_Y - 1, 18, 18);
        }
    }

    /** 可合成过滤开关:复用原版配方书的过滤按钮材质 */
    private void renderFilterToggle(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        boolean hover = isFilterHovered(mouseX, mouseY);
        ResourceLocation sprite = craftableOnly
                ? (hover ? FILTER_ENABLED_HIGHLIGHTED_SPRITE : FILTER_ENABLED_SPRITE)
                : (hover ? FILTER_DISABLED_HIGHLIGHTED_SPRITE : FILTER_DISABLED_SPRITE);
        guiGraphics.blitSprite(sprite, leftPos + FILTER_X, topPos + FILTER_Y, FILTER_W, FILTER_H);
    }

    private boolean isFilterHovered(double mouseX, double mouseY) {
        return mouseX >= leftPos + FILTER_X && mouseX < leftPos + FILTER_X + FILTER_W
                && mouseY >= topPos + FILTER_Y && mouseY < topPos + FILTER_Y + FILTER_H;
    }

    /** 清除所选按钮,仅在选中配方时显示 */
    private void renderClearButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (selected == null) return;
        int x = leftPos + CLEAR_X;
        int y = topPos + CLEAR_Y;
        boolean hover = isClearHovered(mouseX, mouseY);
        drawPanel(guiGraphics, x, y, CLEAR_SIZE, CLEAR_SIZE, hover ? 0xFFD8D8D8 : 0xFFC6C6C6);
        String cross = "×";
        guiGraphics.drawString(font, cross,
                x + (CLEAR_SIZE - font.width(cross)) / 2 + 1, y + 2,
                hover ? 0xFFAA2222 : 0xFF404040, false);
    }

    private boolean isClearHovered(double mouseX, double mouseY) {
        return selected != null
                && mouseX >= leftPos + CLEAR_X && mouseX < leftPos + CLEAR_X + CLEAR_SIZE
                && mouseY >= topPos + CLEAR_Y && mouseY < topPos + CLEAR_Y + CLEAR_SIZE;
    }

    /** 左侧竖排分类子标签,选中的标签与主面板连成一体 */
    private void renderTabs(GuiGraphics guiGraphics) {
        for (int i = 0; i < tabs.size(); i++) {
            CategoryTab tab = tabs.get(i);
            boolean active = java.util.Objects.equals(tab.category(), activeCategory);
            int x = leftPos - TAB_SIZE;
            int y = topPos + TAB_Y0 + i * (TAB_SIZE + TAB_GAP);
            // 多画 1px 盖住主面板左边框,使标签看起来贴在面板上
            drawPanel(guiGraphics, x, y, TAB_SIZE + 1, TAB_SIZE, active ? 0xFFC6C6C6 : 0xFF8B8B8B);
            if (active) {
                // 抹掉标签右边框,与主面板打通
                guiGraphics.fill(leftPos, y + 1, leftPos + 1, y + TAB_SIZE - 1, 0xFFC6C6C6);
            }
            guiGraphics.renderItem(tab.icon(), x + TAB_ICON_INSET, y + TAB_ICON_INSET);
        }
    }

    /** 分类标签占用的额外屏幕区域,供 JEI 等覆盖层避让 */
    public Rect2i getTabArea() {
        return new Rect2i(leftPos - TAB_SIZE, topPos + TAB_Y0,
                TAB_SIZE + 1, tabs.size() * (TAB_SIZE + TAB_GAP) - TAB_GAP);
    }

    /** 鼠标所在的分类标签下标,不在标签上返回 -1 */
    private int tabIndexAt(double mouseX, double mouseY) {
        int gx = (int) (mouseX - (leftPos - TAB_SIZE));
        if (gx < 0 || gx >= TAB_SIZE) return -1;
        int gy = (int) (mouseY - (topPos + TAB_Y0));
        if (gy < 0 || gy % (TAB_SIZE + TAB_GAP) >= TAB_SIZE) return -1;
        int index = gy / (TAB_SIZE + TAB_GAP);
        return index < tabs.size() ? index : -1;
    }

    private static void drawPanel(GuiGraphics g, int x, int y, int w, int h, int fill) {
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w, y + 1, 0xFFFFFFFF);
        g.fill(x, y, x + 1, y + h, 0xFFFFFFFF);
        g.fill(x, y + h - 1, x + w, y + h, 0xFF555555);
        g.fill(x + w - 1, y, x + w, y + h, 0xFF555555);
    }

    private static void drawInset(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF8B8B8B);
        g.fill(x, y, x + w, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + h, 0xFF373737);
        g.fill(x, y + h - 1, x + w, y + h, 0xFFFFFFFF);
        g.fill(x + w - 1, y, x + w, y + h, 0xFFFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 与搜索框(y=8,高 14)垂直居中对齐
        guiGraphics.drawString(font, MATERIALS_LABEL, RIGHT_X, 11, 0xFF404040, false);
        // 选中配方时,在制作按钮右侧展示将要制作的物品
        if (selected != null) {
            ItemStack result = selected.value().getResultItem(minecraft.level.registryAccess());
            guiGraphics.renderItem(result, BUTTON_X + BUTTON_WIDTH + BUTTON_ICON_GAP, BUTTON_Y + 2);
        }
    }

    private void renderGrid(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int startIndex = scrollRow * GRID_COLS;
        for (int i = 0; i < GRID_ROWS * GRID_COLS; i++) {
            int index = startIndex + i;
            if (index >= filtered.size()) break;
            RecipeHolder<EquipmentForgeRecipe> holder = filtered.get(index);
            int x = leftPos + GRID_X + (i % GRID_COLS) * CELL + 1;
            int y = topPos + GRID_Y + (i / GRID_COLS) * CELL + 1;

            // 参照原版配方书:可合成/不可合成用各自的槽位底图,不再画边框
            boolean craftable = craftableIds.contains(holder.id());
            blitSlotSprite(guiGraphics, craftable ? SLOT_CRAFTABLE_SPRITE : SLOT_UNCRAFTABLE_SPRITE,
                    x - 1, y - 1);

            ItemStack result = holder.value().getResultItem(minecraft.level.registryAccess());
            guiGraphics.renderItem(result, x, y);
            guiGraphics.renderItemDecorations(font, result, x, y);

            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                guiGraphics.fill(x, y, x + 16, y + 16, 250, 0x66FFFFFF);
            }
        }
    }

    /**
     * 槽位 sprite 源图为 25x25:2px 边框 + 20px 纯色内部 + 右/下各 1px 黑色投影。
     * 非整数缩放会让像素画边框宽窄不一,这里改为四象限 1:1 拼接:
     * 取对称的 24x24 区域(丢掉投影),四个 9x9 角原样搬到 18x18 目标,内部纯色所以无接缝。
     */
    private static void blitSlotSprite(GuiGraphics guiGraphics, ResourceLocation sprite, int x, int y) {
        int half = CELL / 2; // 9
        guiGraphics.blitSprite(sprite, 25, 25, 0, 0, x, y, half, half);
        guiGraphics.blitSprite(sprite, 25, 25, 24 - half, 0, x + half, y, half, half);
        guiGraphics.blitSprite(sprite, 25, 25, 0, 24 - half, x, y + half, half, half);
        guiGraphics.blitSprite(sprite, 25, 25, 24 - half, 24 - half, x + half, y + half, half, half);
    }

    private void renderMaterials(GuiGraphics guiGraphics) {
        int x = leftPos + RIGHT_X + 3;
        int y = topPos + MAT_ROW_Y;
        if (selected == null) {
            guiGraphics.drawWordWrap(font, SELECT_HINT, x, y, RIGHT_WIDTH - 8, 0xFF555555);
            return;
        }
        List<SizedIngredient> materials = sortedMaterials();
        // 只画滚动窗口内的 MAX_MAT_ROWS 行
        int end = Math.min(materials.size(), matScrollRow + MAX_MAT_ROWS);
        for (int i = matScrollRow; i < end; i++) {
            SizedIngredient material = materials.get(i);
            ItemStack icon = materialIcon(material);
            if (icon.isEmpty()) continue;

            int owned = EquipmentForgeRecipe.countMatching(menu.playerInventory, menu.netItems(), material.ingredient());
            boolean enough = owned >= material.count();

            guiGraphics.renderItem(icon, x, y);
            // 栏位变窄:物品名改由悬停提示展示,此处只显示 拥有/所需
            String text = owned + "/" + material.count();
            guiGraphics.drawString(font, text,
                    leftPos + RIGHT_X + RIGHT_WIDTH - 4 - font.width(text), y + 4,
                    enough ? 0xFF3FB53F : 0xFFCC3333, false);
            y += 18;
        }
        // 上/下还有材料未显示时,在材料行上下的空隙里画滚动箭头
        if (matScrollRow > 0) {
            guiGraphics.blit(ARROW_UP_TEXTURE, leftPos + ARROW_X, topPos + ARROW_UP_Y,
                    0, 0, ARROW_W, ARROW_H, ARROW_W, ARROW_H);
        }
        if (matScrollRow < maxMatScrollRow()) {
            guiGraphics.blit(ARROW_DOWN_TEXTURE, leftPos + ARROW_X, topPos + ARROW_DOWN_Y,
                    0, 0, ARROW_W, ARROW_H, ARROW_W, ARROW_H);
        }
    }

    private int maxMatScrollRow() {
        return Math.max(0, sortedMaterials().size() - MAX_MAT_ROWS);
    }

    /** 鼠标是否在右侧材料框内(滚动区域) */
    private boolean isOverMaterialPanel(double mouseX, double mouseY) {
        int x = leftPos + RIGHT_X - 1;
        int y = topPos + MAT_PANEL_Y;
        return mouseX >= x && mouseX < x + RIGHT_WIDTH + 2
                && mouseY >= y && mouseY < y + MAT_PANEL_H;
    }

    /**
     * 选中配方的材料列表,先按稀有度、再按创造模式物品栏顺序排序(结果缓存,随选中配方变化重算)。
     * 标签类材料以其第一个可选物品作为排序代表,避免轮换图标导致顺序抖动。
     */
    private List<SizedIngredient> sortedMaterials() {
        if (materialsCachedFor != selected) {
            materialsCachedFor = selected;
            matScrollRow = 0; // 换配方后材料列表回到顶部
            sortedMaterials = selected == null ? List.of()
                    : selected.value().materials().stream()
                            .sorted(Comparator
                                    .comparingInt((SizedIngredient m) -> sortRepresentative(m).getRarity().ordinal())
                                    .thenComparingInt(m -> creativeOrder.getOrDefault(
                                            sortRepresentative(m).getItem(), Integer.MAX_VALUE)))
                            .toList();
        }
        return sortedMaterials;
    }

    /** 材料的排序代表物品:取 ingredient 的第一个可选项(稳定,不随轮换变化) */
    private static ItemStack sortRepresentative(SizedIngredient material) {
        ItemStack[] options = material.ingredient().getItems();
        return options.length == 0 ? ItemStack.EMPTY : options[0];
    }

    /** 标签材料轮换展示可用物品 */
    private static ItemStack materialIcon(SizedIngredient material) {
        ItemStack[] options = material.ingredient().getItems();
        if (options.length == 0) return ItemStack.EMPTY;
        return options[(int) (System.currentTimeMillis() / 1000 % options.length)];
    }

    /** 鼠标悬停的材料行下标,不在材料图标上返回 -1 */
    private int materialIndexAt(double mouseX, double mouseY) {
        if (selected == null) return -1;
        int gx = (int) (mouseX - (leftPos + RIGHT_X + 3));
        int gy = (int) (mouseY - (topPos + MAT_ROW_Y));
        if (gx < 0 || gx >= 16 || gy < 0 || gy % 18 >= 16) return -1;
        int row = gy / 18;
        if (row >= MAX_MAT_ROWS) return -1;
        int index = matScrollRow + row;
        return index < sortedMaterials().size() ? index : -1;
    }

    private void renderMaterialTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int index = materialIndexAt(mouseX, mouseY);
        if (index >= 0) {
            ItemStack icon = materialIcon(sortedMaterials().get(index));
            if (!icon.isEmpty()) {
                guiGraphics.renderTooltip(font, icon, mouseX, mouseY);
            }
        }
    }

    private void renderGridTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int index = gridIndexAt(mouseX, mouseY);
        if (index >= 0 && index < filtered.size()) {
            ItemStack result = filtered.get(index).value().getResultItem(minecraft.level.registryAccess());
            guiGraphics.renderTooltip(font, result, mouseX, mouseY);
        }
    }

    /** 鼠标位置对应的 filtered 下标,不在格子内返回 -1 */
    private int gridIndexAt(double mouseX, double mouseY) {
        int gx = (int) (mouseX - (leftPos + GRID_X));
        int gy = (int) (mouseY - (topPos + GRID_Y));
        if (gx < 0 || gy < 0 || gx >= GRID_COLS * CELL || gy >= GRID_ROWS * CELL) return -1;
        return (scrollRow + gy / CELL) * GRID_COLS + gx / CELL;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isFilterHovered(mouseX, mouseY)) {
                craftableOnly = !craftableOnly;
                scrollRow = 0;
                refreshFiltered();
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                return true;
            }
            if (isClearHovered(mouseX, mouseY)) {
                selected = null;
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                return true;
            }
            int tab = tabIndexAt(mouseX, mouseY);
            if (tab >= 0) {
                activeCategory = tabs.get(tab).category();
                scrollRow = 0;
                refreshFiltered();
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                return true;
            }
            int index = gridIndexAt(mouseX, mouseY);
            if (index >= 0 && index < filtered.size()) {
                selected = filtered.get(index);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // 仅在物品格区域内滚动翻页,避免影响背包区域
        if (gridIndexAt(mouseX, mouseY) >= 0) {
            scrollRow = Math.max(0, Math.min(maxScrollRow(), scrollRow - (int) Math.signum(scrollY)));
            return true;
        }
        // 右侧材料框:材料多于可见行数时同样支持滚动
        if (isOverMaterialPanel(mouseX, mouseY)) {
            matScrollRow = Math.max(0, Math.min(maxMatScrollRow(), matScrollRow - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 搜索框聚焦时拦截按键,避免按 E 关闭界面
        if (searchBox.isFocused() && keyCode != GLFW.GLFW_KEY_ESCAPE) {
            return searchBox.keyPressed(keyCode, scanCode, modifiers) || true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** 20x20 方形贴图按钮,平常/悬停/按下三态 */
    private static class CraftIconButton extends Button {
        private boolean held;

        CraftIconButton(int x, int y, OnPress onPress) {
            super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, CRAFT_LABEL, onPress, DEFAULT_NARRATION);
            setTooltip(Tooltip.create(CRAFT_LABEL));
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            held = true;
            super.onClick(mouseX, mouseY);
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            held = false;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // 底座:常亮,悬停高光,按下压暗;图标:按下换贴图,不可用时压暗
            boolean pressed = held && isHovered();
            ResourceLocation base = !pressed && isHoveredOrFocused()
                    ? BUTTON_BASE_HIGHLIGHTED_SPRITE : BUTTON_BASE_SPRITE;
            if (pressed) {
                guiGraphics.setColor(0.65f, 0.65f, 0.65f, 1.0f);
            }
            guiGraphics.blitSprite(base, getX(), getY(), width, height);
            if (pressed) {
                guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            }

            ResourceLocation icon = pressed ? BUTTON_TEXTURE_CLICKED : BUTTON_TEXTURE;
            if (!active) {
                guiGraphics.setColor(0.5f, 0.5f, 0.5f, 1.0f);
            }
            guiGraphics.blit(icon, getX(), getY(), 0, 0, width, height, width, height);
            if (!active) {
                guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
    }
}

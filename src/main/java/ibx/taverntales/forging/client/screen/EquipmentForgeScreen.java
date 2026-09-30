package ibx.taverntales.forging.client.screen;

import ibx.taverntales.forging.TavernTalesEquipmentForge;
import ibx.taverntales.forging.client.ClientForgeRecipes;
import ibx.taverntales.forging.client.CreativeOrder;
import ibx.taverntales.forging.client.PinyinSearch;
import ibx.taverntales.forging.menu.EquipmentForgeMenu;
import ibx.taverntales.forging.network.CraftEquipmentPayload;
import ibx.taverntales.forging.recipe.EquipmentCategoryDefinition;
import ibx.taverntales.forging.recipe.EquipmentForgeRecipe;
import ibx.taverntales.forging.registry.ModRegistries;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class EquipmentForgeScreen extends AbstractContainerScreen<EquipmentForgeMenu> {
    /*
     * 布局:整体 176 宽(与原版 GUI 一致,仅比 162 的背包栏稍宽)。
     * 左栏(搜索框 + 装备选择)与右栏(铭牌 + 所需材料 + 锻造行)各 74px,左右对称:
     *   左栏内嵌板 x=7..80,右栏内嵌板 x=95..168,两侧边距均为 7。
     * 搜索框与铭牌上下对齐;两栏内嵌板顶部对齐(y=25);
     * 锻造行(印章按钮 + 箭头 + 产物座)底边与装备选择栏底边对齐,且整体在右栏水平居中。
     *
     * 美术"紫晶黄铜":材质取自锻造台方块本身(紫色箱体、黄铜包角与边条、侧面黄铜框嵌紫宝石、顶上的铁砧)。
     * 静态部分烘焙在 equipment_forge.png,随状态变化的部分是 gui/sprites/equipment_forge/ 下的小图。
     */
    private static final int GRID_COLS = 4;
    private static final int GRID_ROWS = 6;
    private static final int CELL = 18;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 26;

    /** 右栏(与左栏对称) */
    private static final int RIGHT_X = 96;
    private static final int RIGHT_WIDTH = GRID_COLS * CELL;
    /** 锻造行:印章按钮 20x20 + 箭头间隙 + 22x22 产物座,整体在右栏居中,底边对齐装备格底边 */
    private static final int BUTTON_WIDTH = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_ICON_GAP = 24;
    private static final int OUTPUT_SIZE = 22;
    private static final int BUTTON_X = RIGHT_X + (RIGHT_WIDTH - BUTTON_WIDTH - BUTTON_ICON_GAP - OUTPUT_SIZE) / 2;
    private static final int BUTTON_Y = GRID_Y + GRID_ROWS * CELL + 1 - BUTTON_HEIGHT;
    /** 镂空黄铜箭头,在按钮与产物座的间隙内居中;可锻造时内槽注满紫光 */
    private static final int CRAFT_ARROW_W = 14;
    private static final int CRAFT_ARROW_H = 9;
    private static final int CRAFT_ARROW_X = BUTTON_X + BUTTON_WIDTH + (BUTTON_ICON_GAP - CRAFT_ARROW_W) / 2;
    private static final int CRAFT_ARROW_Y = BUTTON_Y + (BUTTON_HEIGHT - CRAFT_ARROW_H) / 2;
    /** 产物座:黄铜框 + 顶部状态宝石,产物图标画在框内 (+3,+3) */
    private static final int OUTPUT_X = BUTTON_X + BUTTON_WIDTH + BUTTON_ICON_GAP;
    private static final int OUTPUT_Y = BUTTON_Y + (BUTTON_HEIGHT - OUTPUT_SIZE) / 2;
    /** 点击锻造后箭头从左到右重新注光的时长 */
    private static final long CRAFT_ANIM_MS = 300;

    /** 材料框:顶部与装备格对齐,底部止于锻造行上方 4px */
    private static final int MAT_PANEL_Y = GRID_Y - 1;
    private static final int MAT_PANEL_H = BUTTON_Y - 4 - MAT_PANEL_Y;
    /** 最大可见行数;这几行在材料框内垂直居中,上下留出的空隙正好放滚动箭头 */
    private static final int MAX_MAT_ROWS = 4;
    private static final int MAT_ROW_Y = MAT_PANEL_Y + (MAT_PANEL_H - MAX_MAT_ROWS * CELL) / 2;
    private static final int MAT_ROWS_END = MAT_ROW_Y + MAX_MAT_ROWS * CELL;

    /** 上/下滚动箭头(5x3,贴图含 1px 投影):右栏水平居中,分别嵌在材料行上下的空隙中 */
    private static final int ARROW_W = 5;
    private static final int ARROW_H = 3;
    private static final int ARROW_X = RIGHT_X + (RIGHT_WIDTH - ARROW_W) / 2;
    private static final int ARROW_UP_Y = MAT_PANEL_Y + (MAT_ROW_Y - MAT_PANEL_Y - ARROW_H) / 2;
    private static final int ARROW_DOWN_Y =
            MAT_ROWS_END + (MAT_PANEL_Y + MAT_PANEL_H - MAT_ROWS_END - ARROW_H) / 2;

    /** 可合成过滤开关(金锭图标):位于搜索框右侧,与装备格右缘对齐 */
    private static final int FILTER_W = 26;
    private static final int FILTER_H = 16;
    private static final int FILTER_X = GRID_X + GRID_COLS * CELL - FILTER_W;
    private static final int FILTER_Y = 7;

    /** 搜索框外框(烘焙在底图里);EditBox 关掉原版边框,放在框内 */
    private static final int SEARCH_X = GRID_X - 1;
    private static final int SEARCH_Y = 8;
    private static final int SEARCH_W = FILTER_X - 3 - SEARCH_X;
    private static final int SEARCH_H = 14;

    /** 铭牌(烘焙在底图里):与搜索框同高,显示正在锻造的装备名 */
    private static final int NAMEPLATE_X = RIGHT_X - 1;
    private static final int NAMEPLATE_W = RIGHT_WIDTH + 2;
    private static final int NAME_X = NAMEPLATE_X + 5;
    private static final int NAME_Y = SEARCH_Y + (SEARCH_H - 8) / 2;

    /** 清除所选按钮(红晶):嵌在铭牌右端 */
    private static final int CLEAR_W = 11;
    private static final int CLEAR_H = 10;
    private static final int CLEAR_X = RIGHT_X + RIGHT_WIDTH - CLEAR_W - 1;
    private static final int CLEAR_Y = SEARCH_Y + 2;

    /** 左侧分类子标签,顶部比搜索框上缘高 5px */
    private static final int TAB_SIZE = 20;
    private static final int TAB_GAP = 2;
    private static final int TAB_Y0 = 3;
    /** 16x16 图标在标签内的留白,随 TAB_SIZE 自动居中 */
    private static final int TAB_ICON_INSET = (TAB_SIZE - 16) / 2;
    /** 不分页时最多显示的标签数,超出后进入分页模式(参考创造物品栏翻页) */
    private static final int MAX_VISIBLE_TABS = 6;
    /** 分页模式下每页的标签数:"全部"钉在顶部不参与翻页,故比 MAX_VISIBLE_TABS 少 1 */
    private static final int TAB_PAGE_SIZE = MAX_VISIBLE_TABS - 1;
    /** 翻页按钮尺寸:比标签窄,在标签列内水平居中 */
    private static final int TAB_PAGE_BTN_W = 14;
    private static final int TAB_PAGE_BTN_H = 10;
    /** 翻页按钮相对标签列左缘的水平偏移(居中) */
    private static final int TAB_PAGE_BTN_X = (TAB_SIZE - TAB_PAGE_BTN_W) / 2;

    /** 贴图尺寸(含 1px 投影/光晕);偏移量见各绘制处 */
    private static final int TAB_SPRITE_W = 23;
    private static final int SELECTION_SPRITE_SIZE = 21;
    private static final int SEAL_SPRITE_SIZE = 22;
    private static final int SOCKET_SPRITE_W = 23;
    private static final int SOCKET_SPRITE_H = 27;
    /** 空状态浮雕:铁砧宽 38、高 15,贴图向上多留 8px 放金锭与红晶 */
    private static final int TRAY_EMPTY_W = 40;
    private static final int TRAY_EMPTY_H = 25;
    private static final int TRAY_EMPTY_ANVIL_W = 38;
    private static final int TRAY_EMPTY_ANVIL_H = 15;

    /** 配色(取自方块贴图的紫/黄铜/金色阶) */
    private static final int COLOR_PARCHMENT = 0xFFE4CB89;
    private static final int COLOR_TEXT_DIM = 0xFF8C7AA8;
    private static final int COLOR_ENOUGH = 0xFFA6DE78;
    private static final int COLOR_SHORT = 0xFFFF5A5A;
    private static final int COLOR_LEDGER_DARK = 0xFF1A1228;
    private static final int COLOR_LEDGER_LIGHT = 0xFF362A54;
    private static final int OVERLAY_UNCRAFTABLE = 0x6E1A1228;
    private static final int OVERLAY_HOVER = 0x66FFFFFF;
    private static final int OVERLAY_OUTPUT_SHORT = 0x96211A33;
    private static final int OVERLAY_TAB_INACTIVE = 0x50211A33;

    private static final Component CRAFT_LABEL = Component.translatable("gui.taverntales_4ging.craft");
    private static final Component SEARCH_HINT = Component.translatable("gui.taverntales_4ging.search");
    private static final Component SELECT_HINT = Component.translatable("gui.taverntales_4ging.select_hint");
    private static final Component NO_SELECTION = Component.translatable("gui.taverntales_4ging.no_selection");
    private static final Component FILTER_ALL_LABEL = Component.translatable("gui.taverntales_4ging.filter_all");
    private static final Component FILTER_CRAFTABLE_LABEL = Component.translatable("gui.taverntales_4ging.filter_craftable");

    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            TavernTalesEquipmentForge.MODID, "textures/gui/equipment_forge.png");

    private static final Identifier SLOT_SPRITE = sprite("slot");
    private static final Identifier SLOT_READY_SPRITE = sprite("slot_ready");
    private static final Identifier SLOT_SHORT_SPRITE = sprite("slot_short");
    private static final Identifier SELECTION_SPRITE = sprite("selection");
    private static final Identifier SOCKET_NONE_SPRITE = sprite("socket_none");
    private static final Identifier SOCKET_READY_SPRITE = sprite("socket_ready");
    private static final Identifier SOCKET_SHORT_SPRITE = sprite("socket_short");
    private static final Identifier ARROW_SPRITE = sprite("arrow");
    private static final Identifier ARROW_FILL_SPRITE = sprite("arrow_fill");
    private static final Identifier SEAL_SPRITE = sprite("seal");
    private static final Identifier SEAL_HIGHLIGHTED_SPRITE = sprite("seal_highlighted");
    private static final Identifier SEAL_PRESSED_SPRITE = sprite("seal_pressed");
    private static final Identifier SEAL_DISABLED_SPRITE = sprite("seal_disabled");
    private static final Identifier FILTER_SPRITE = sprite("filter");
    private static final Identifier FILTER_HIGHLIGHTED_SPRITE = sprite("filter_highlighted");
    private static final Identifier FILTER_ENABLED_SPRITE = sprite("filter_enabled");
    private static final Identifier FILTER_ENABLED_HIGHLIGHTED_SPRITE = sprite("filter_enabled_highlighted");
    private static final Identifier CLEAR_SPRITE = sprite("clear");
    private static final Identifier CLEAR_HIGHLIGHTED_SPRITE = sprite("clear_highlighted");
    private static final Identifier TAB_SPRITE = sprite("tab");
    private static final Identifier TAB_SELECTED_SPRITE = sprite("tab_selected");
    private static final Identifier PAGE_UP_SPRITE = sprite("page_up");
    private static final Identifier PAGE_UP_HIGHLIGHTED_SPRITE = sprite("page_up_highlighted");
    private static final Identifier PAGE_DOWN_SPRITE = sprite("page_down");
    private static final Identifier PAGE_DOWN_HIGHLIGHTED_SPRITE = sprite("page_down_highlighted");
    private static final Identifier SCROLL_UP_SPRITE = sprite("scroll_up");
    private static final Identifier SCROLL_DOWN_SPRITE = sprite("scroll_down");
    private static final Identifier TRAY_EMPTY_SPRITE = sprite("tray_empty");

    private static Identifier sprite(String name) {
        return Identifier.fromNamespaceAndPath(TavernTalesEquipmentForge.MODID, "equipment_forge/" + name);
    }

    /** 内置"全部"标签的翻译名;图标在 buildTabs 里现造——26.1 起类加载时注册表未就绪,静态字段不能 new ItemStack */
    private static final Component ALL_LABEL = Component.translatable("category.taverntales_4ging.all");

    /** 分类子标签,category 为 null 表示"全部" */
    private record CategoryTab(Identifier category, ItemStack icon, Component label) {}

    /** 当前可见的一个标签:tabs 中的下标及其顶边 y(相对 topPos) */
    private record VisibleTab(int index, int y) {}

    /** 选中配方的一种材料,及其按 SlotDisplay 解析出的可选物品(标签材料有多个) */
    private record MaterialRow(SizedIngredient material, List<ItemStack> options) {
        /** 排序代表:第一个可选物品(稳定,不随轮换图标变化) */
        ItemStack representative() {
            return options.isEmpty() ? ItemStack.EMPTY : options.getFirst();
        }
    }

    /** 标签列表:内置"全部"置顶,其余由数据包注册表驱动,init 时构建 */
    private List<CategoryTab> tabs = List.of();
    /** 标签当前页(每页 TAB_PAGE_SIZE 个),标签总数不超过一页时恒为 0 */
    private int tabPage;

    /** 当前选中的分类 id,null 表示"全部" */
    private Identifier activeCategory;
    /** 是否仅显示可合成配方 */
    private boolean craftableOnly;

    private List<RecipeHolder<EquipmentForgeRecipe>> allRecipes = List.of();
    private List<RecipeHolder<EquipmentForgeRecipe>> filtered = List.of();
    /** 配方 id -> [原文/全拼/首字母] 检索键 */
    private final Map<ResourceKey<Recipe<?>>, String[]> searchKeys = new HashMap<>();
    private final Set<ResourceKey<Recipe<?>>> craftableIds = new HashSet<>();
    /** 上次计算 craftableIds 时的背包副本与网络快照,两者都没变就不必重算 */
    private List<ItemStack> lastInventory = List.of();
    private List<ItemStack> lastNetItems = List.of();
    private RecipeHolder<EquipmentForgeRecipe> selected;
    private int scrollRow;

    /** 物品排序(稀有度→创造物品栏顺序),配方与材料排序共用;init 时构建 */
    private Comparator<ItemStack> itemOrder;
    /** 已排序的材料列表缓存,随选中配方变化重算 */
    private RecipeHolder<EquipmentForgeRecipe> materialsCachedFor;
    private List<MaterialRow> sortedMaterials = List.of();
    /** 解析材料 SlotDisplay 所需的上下文(注册表等),init 时取自当前世界 */
    private ContextMap displayContext;
    /** 材料列表的滚动行数(材料多于 MAX_MAT_ROWS 时可滚动) */
    private int matScrollRow;
    /** 最近一次点击锻造的时间,用于箭头注光动画;-1 表示没有进行中的动画 */
    private long craftAnimStart = -1;

    private EditBox searchBox;
    private Button craftButton;

    public EquipmentForgeScreen(EquipmentForgeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 227);
    }

    @Override
    protected void init() {
        super.init();

        // 搜索框:外框烘焙在底图里,EditBox 去掉原版边框后放进框内(无边框时文字画在控件左上角)
        searchBox = new EditBox(font, leftPos + SEARCH_X + 4, topPos + SEARCH_Y + (SEARCH_H - 8) / 2,
                SEARCH_W - 8, 9, SEARCH_HINT);
        searchBox.setBordered(false);
        searchBox.setHint(SEARCH_HINT.copy().withColor(COLOR_TEXT_DIM & 0xFFFFFF));
        searchBox.setResponder(text -> refreshFiltered());
        addRenderableWidget(searchBox);

        craftButton = new SealButton(leftPos + BUTTON_X, topPos + BUTTON_Y, b -> sendCraft());
        addRenderableWidget(craftButton);

        buildTabs();

        // 排序:先按结果物品稀有度(普通→史诗),再按创造模式物品栏(搜索页)顺序。
        // 配方来自客户端缓存:1.21.2 起原版不再同步配方,由服务端经 NeoForge 代发(见 ClientForgeRecipes)
        itemOrder = CreativeOrder.comparator();
        displayContext = SlotDisplayContext.fromLevel(minecraft.level);
        allRecipes = ClientForgeRecipes.all()
                .stream()
                .sorted(Comparator
                        .comparing((RecipeHolder<EquipmentForgeRecipe> h) -> h.value().resultView(), itemOrder)
                        .thenComparing(h -> h.id().identifier().toString()))
                .toList();
        searchKeys.clear();
        for (RecipeHolder<EquipmentForgeRecipe> holder : allRecipes) {
            String name = holder.value().resultView().getHoverName().getString();
            searchKeys.put(holder.id(), PinyinSearch.searchKeys(name));
        }
        refreshCraftable();
        refreshFiltered();
        warnUnknownCategories();
    }

    /** 从数据包注册表构建标签:内置"全部"置顶,其余按 order、再按 id 字母序 */
    private void buildTabs() {
        List<CategoryTab> list = new ArrayList<>();
        list.add(new CategoryTab(null, new ItemStack(Items.COMPASS), ALL_LABEL));
        minecraft.level.registryAccess()
                .lookup(ModRegistries.EQUIPMENT_CATEGORY)
                .ifPresent(registry -> registry.entrySet().stream()
                        .sorted(Comparator
                                .comparingInt((Map.Entry<ResourceKey<EquipmentCategoryDefinition>, EquipmentCategoryDefinition> e) ->
                                        e.getValue().order())
                                .thenComparing(e -> e.getKey().identifier().toString()))
                        .forEach(e -> {
                            EquipmentCategoryDefinition def = e.getValue();
                            list.add(new CategoryTab(e.getKey().identifier(), def.iconStack(), def.name()));
                        }));
        tabs = List.copyOf(list);
        tabPage = Mth.clamp(tabPage, 0, tabPageCount() - 1);
    }

    /** 标签是否多到需要分页 */
    private boolean tabsPaged() {
        return tabs.size() > MAX_VISIBLE_TABS;
    }

    private int tabPageCount() {
        // "全部"不参与翻页,其余标签按每页 TAB_PAGE_SIZE 个分页
        if (!tabsPaged()) return 1;
        return (tabs.size() - 1 + TAB_PAGE_SIZE - 1) / TAB_PAGE_SIZE;
    }

    /** 上翻按钮的 y(相对 topPos):紧跟在钉住的"全部"标签之下 */
    private int tabUpButtonY() {
        return TAB_Y0 + TAB_SIZE + TAB_GAP;
    }

    /** "全部"之后第一个标签的 y(相对 topPos):不分页时紧跟"全部",分页时让出上翻按钮的位置 */
    private int tabListY0() {
        return tabsPaged() ? tabUpButtonY() + TAB_PAGE_BTN_H + TAB_GAP : tabUpButtonY();
    }

    /** 最后一个分页标签位的底边 y(相对 topPos) */
    private int tabListBottom() {
        return tabListY0() + TAB_PAGE_SIZE * (TAB_SIZE + TAB_GAP) - TAB_GAP;
    }

    /** 页码文字(如 1/2)的 y(相对 topPos):居中于最后一个标签位与下翻按钮之间 */
    private int tabPageLabelY() {
        return tabListBottom() + 4;
    }

    /** 下翻按钮的 y(相对 topPos):固定位置,末页不满时也不上移(数字字形高 7px,上下各留 4px 与页码居中) */
    private int tabDownButtonY() {
        return tabPageLabelY() + 7 + 4;
    }

    /** 翻页,dir 为 -1 上翻、+1 下翻;已在边界翻不动时返回 false */
    private boolean turnTabPage(int dir) {
        int page = Mth.clamp(tabPage + dir, 0, tabPageCount() - 1);
        if (page == tabPage) return false;
        tabPage = page;
        return true;
    }

    /** 让当前选中分类所在的标签页可见("全部"钉在顶部,任何页都可见,无需跳页) */
    private void ensureActiveTabVisible() {
        for (int i = 1; i < tabs.size(); i++) {
            if (Objects.equals(tabs.get(i).category(), activeCategory)) {
                tabPage = (i - 1) / TAB_PAGE_SIZE;
                return;
            }
        }
    }

    /** 当前可见的标签:"全部"钉在顶部,其后是当前页的标签(不分页时即其余全部) */
    private List<VisibleTab> visibleTabs() {
        List<VisibleTab> list = new ArrayList<>();
        if (tabs.isEmpty()) return list;
        list.add(new VisibleTab(0, TAB_Y0));
        int first = 1 + tabPage * TAB_PAGE_SIZE;
        int count = tabsPaged() ? Math.min(TAB_PAGE_SIZE, tabs.size() - first) : tabs.size() - 1;
        for (int row = 0; row < count; row++) {
            list.add(new VisibleTab(first + row, tabListY0() + row * (TAB_SIZE + TAB_GAP)));
        }
        return list;
    }

    /** 配方引用了未定义的分类 id 时打调试日志(这类配方只会出现在"全部"中) */
    private void warnUnknownCategories() {
        Set<Identifier> known = new HashSet<>();
        for (CategoryTab tab : tabs) {
            if (tab.category() != null) known.add(tab.category());
        }
        for (RecipeHolder<EquipmentForgeRecipe> holder : allRecipes) {
            Identifier cat = holder.value().category();
            if (!known.contains(cat)) {
                TavernTalesEquipmentForge.LOGGER.debug(
                        "配方 {} 的分类 {} 未定义,仅在\"全部\"标签中显示", holder.id(), cat);
            }
        }
    }

    @Override
    protected void containerTick() {
        // 背包与网络快照都没变时可合成集合不会变,免得每 tick 为每个配方各复制一遍背包
        if (!craftInputsChanged()) return;
        Set<ResourceKey<Recipe<?>>> before = craftableOnly ? new HashSet<>(craftableIds) : null;
        refreshCraftable();
        // 开启过滤时,背包变动导致可合成集合变化要即时反映到列表
        if (craftableOnly && !craftableIds.equals(before)) {
            refreshFiltered();
        }
    }

    /** 背包或网络快照自上次检查以来是否有变化;网络快照由同步包整表替换,比引用即可 */
    private boolean craftInputsChanged() {
        List<ItemStack> inventory = EquipmentForgeRecipe.availableStacks(menu.playerInventory, List.of());
        boolean changed = menu.netItems() != lastNetItems || inventory.size() != lastInventory.size();
        for (int i = 0; !changed && i < inventory.size(); i++) {
            changed = !ItemStack.matches(inventory.get(i), lastInventory.get(i));
        }
        if (changed) {
            lastInventory = inventory.stream().map(ItemStack::copy).toList();
            lastNetItems = menu.netItems();
        }
        return changed;
    }

    /** 重算当前背包(含网络快照)能锻造的配方集合 */
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
                        || holder.id().identifier().getPath().contains(query);
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
    private static ResourceKey<Recipe<?>> pendingSelect;

    /** 由 JEI 转移处理器调用:请求下次打开/返回锻造界面时选中该配方 */
    public static void requestSelect(ResourceKey<Recipe<?>> recipeId) {
        pendingSelect = recipeId;
    }

    /**
     * 在锻造界面中选中指定配方并使其可见。
     * 清空搜索、取消"仅可合成"过滤,并切换到该配方的分类(未知分类则回到"全部"),再滚动到该项。
     */
    private void selectRecipe(ResourceKey<Recipe<?>> recipeId) {
        for (RecipeHolder<EquipmentForgeRecipe> holder : allRecipes) {
            if (!holder.id().equals(recipeId)) continue;
            selected = holder;
            if (searchBox != null) searchBox.setValue("");
            craftableOnly = false;
            Identifier cat = holder.value().category();
            boolean hasTab = tabs.stream().anyMatch(t -> cat.equals(t.category()));
            activeCategory = hasTab ? cat : null;
            ensureActiveTabVisible();
            refreshFiltered();
            int idx = filtered.indexOf(holder);
            if (idx >= 0) {
                scrollRow = Mth.clamp(idx / GRID_COLS - GRID_ROWS / 2, 0, maxScrollRow());
            }
            return;
        }
    }

    /** 已选中配方且材料足够 */
    private boolean selectedCraftable() {
        return selected != null && craftableIds.contains(selected.id());
    }

    private void sendCraft() {
        if (selectedCraftable()) {
            ClientPacketDistributor.sendToServer(new CraftEquipmentPayload(selected.id()));
            craftAnimStart = System.currentTimeMillis();
        }
    }

    private void playClick() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    /** 点 (mouseX, mouseY) 是否落在左上角 (x, y)、宽 w 高 h 的矩形内(右、下边界不含) */
    private static boolean inRect(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    /**
     * 每帧最先调用的钩子(先于 extractRenderState,且在更低的图层)。装备格与材料区也画在这一层,
     * 与原版切石机一致:手持物品、槽位高亮与 tooltip 都会盖在它们上面。
     */
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // 消费 JEI 物品转移请求(此时已返回锻造界面)
        if (pendingSelect != null) {
            ResourceKey<Recipe<?>> id = pendingSelect;
            pendingSelect = null;
            selectRecipe(id);
        }
        // 控件在随后的 extractRenderState 里绘制,先把可用状态定下来
        craftButton.active = selectedCraftable();
        super.extractBackground(graphics, mouseX, mouseY, a);
        // 面板、两栏凹槽、搜索框与铭牌外框、背包格都烘焙在底图里
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        renderTabs(graphics, mouseX, mouseY);
        renderFilterToggle(graphics, mouseX, mouseY);
        renderNameplate(graphics, mouseX, mouseY);
        renderGrid(graphics, mouseX, mouseY);
        renderMaterials(graphics);
        renderCraftRow(graphics);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY); // 背包槽位 tooltip
        renderGridTooltip(graphics, mouseX, mouseY);
        renderMaterialTooltip(graphics, mouseX, mouseY);
        int tab = tabIndexAt(mouseX, mouseY);
        if (tab >= 0) {
            graphics.setTooltipForNextFrame(font, tabs.get(tab).label(), mouseX, mouseY);
        }
        if (isFilterHovered(mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font,
                    craftableOnly ? FILTER_CRAFTABLE_LABEL : FILTER_ALL_LABEL, mouseX, mouseY);
        }
    }

    /** 可合成过滤开关:金锭点亮 = 仅显示可合成 */
    private void renderFilterToggle(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        boolean hover = isFilterHovered(mouseX, mouseY);
        Identifier sprite = craftableOnly
                ? (hover ? FILTER_ENABLED_HIGHLIGHTED_SPRITE : FILTER_ENABLED_SPRITE)
                : (hover ? FILTER_HIGHLIGHTED_SPRITE : FILTER_SPRITE);
        // 贴图四周各多 1px(描边与投影)
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, leftPos + FILTER_X - 1, topPos + FILTER_Y - 1,
                FILTER_W + 2, FILTER_H + 2);
    }

    private boolean isFilterHovered(double mouseX, double mouseY) {
        return inRect(mouseX, mouseY, leftPos + FILTER_X, topPos + FILTER_Y, FILTER_W, FILTER_H);
    }

    /** 铭牌:显示选中装备名(过长时原版式来回滚动),选中时右端出现红晶清除按钮 */
    private void renderNameplate(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = leftPos + NAME_X;
        int y = topPos + NAME_Y;
        if (selected == null) {
            int maxWidth = NAMEPLATE_X + NAMEPLATE_W - 3 - NAME_X;
            graphics.text(font, fitWidth(NO_SELECTION.getString(), maxWidth), x, y, COLOR_TEXT_DIM, false);
            return;
        }
        Component name = selected.value().resultView().getHoverName().copy().withColor(COLOR_PARCHMENT & 0xFFFFFF);
        int right = leftPos + CLEAR_X - 2;
        if (font.width(name) <= right - x) {
            graphics.text(font, name, x, y, COLOR_PARCHMENT, true);
        } else {
            // 与原版按钮文字相同的滚动:裁剪在铭牌内框里,两端停顿、来回往复;竖直方向在铭牌内居中(即 y=NAME_Y)
            graphics.textRenderer().acceptScrolling(name, (x + right) / 2, x, right,
                    topPos + SEARCH_Y, topPos + SEARCH_Y + SEARCH_H);
        }
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                isClearHovered(mouseX, mouseY) ? CLEAR_HIGHLIGHTED_SPRITE : CLEAR_SPRITE,
                leftPos + CLEAR_X, topPos + CLEAR_Y, CLEAR_W + 1, CLEAR_H + 1);
    }

    /** 超出宽度时截断并以省略号结尾 */
    private String fitWidth(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "…";
        return font.plainSubstrByWidth(text, maxWidth - font.width(ellipsis)) + ellipsis;
    }

    private boolean isClearHovered(double mouseX, double mouseY) {
        return selected != null
                && inRect(mouseX, mouseY, leftPos + CLEAR_X, topPos + CLEAR_Y, CLEAR_W, CLEAR_H);
    }

    private boolean isOverSearchFrame(double mouseX, double mouseY) {
        return inRect(mouseX, mouseY, leftPos + SEARCH_X, topPos + SEARCH_Y, SEARCH_W, SEARCH_H);
    }

    /** 左侧竖排分类子标签,选中的标签与主面板连成一体;分页时"全部"钉在顶部,上下出现翻页按钮 */
    private void renderTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (VisibleTab tab : visibleTabs()) {
            renderTab(graphics, tabs.get(tab.index()), topPos + tab.y());
        }
        if (!tabsPaged()) return;
        // 页码,在下翻按钮上方,标签列内水平居中
        String pageLabel = (tabPage + 1) + "/" + tabPageCount();
        graphics.text(font, pageLabel,
                leftPos - TAB_SIZE + (TAB_SIZE - font.width(pageLabel)) / 2,
                topPos + tabPageLabelY(), COLOR_PARCHMENT, true);
        // 翻页按钮常驻,到达边界时点击只响声音不翻页
        int hovered = tabPageButtonAt(mouseX, mouseY);
        renderTabPageButton(graphics, topPos + tabUpButtonY(),
                hovered == -1 ? PAGE_UP_HIGHLIGHTED_SPRITE : PAGE_UP_SPRITE);
        renderTabPageButton(graphics, topPos + tabDownButtonY(),
                hovered == 1 ? PAGE_DOWN_HIGHLIGHTED_SPRITE : PAGE_DOWN_SPRITE);
    }

    private void renderTab(GuiGraphicsExtractor graphics, CategoryTab tab, int y) {
        boolean active = Objects.equals(tab.category(), activeCategory);
        int x = leftPos - TAB_SIZE;
        // 贴图宽 23:选中的标签多出 3px 盖住主面板左缘,与面板打通;未选中的标签右侧 2px 透明
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, active ? TAB_SELECTED_SPRITE : TAB_SPRITE,
                x, y, TAB_SPRITE_W, TAB_SIZE);
        int iconX = x + TAB_ICON_INSET + (active ? 1 : 0);
        int iconY = y + TAB_ICON_INSET;
        graphics.item(tab.icon(), iconX, iconY);
        if (!active) {
            graphics.fill(iconX, iconY, iconX + 16, iconY + 16, OVERLAY_TAB_INACTIVE);
        }
    }

    /** 标签列的翻页按钮(黄铜):比标签窄,在标签列内水平居中;贴图含 1px 投影 */
    private void renderTabPageButton(GuiGraphicsExtractor graphics, int y, Identifier sprite) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, leftPos - TAB_SIZE + TAB_PAGE_BTN_X, y,
                TAB_PAGE_BTN_W + 1, TAB_PAGE_BTN_H + 1);
    }

    /** 分类标签占用的额外屏幕区域(分页时含上下翻页按钮),供 JEI 等覆盖层避让 */
    public Rect2i getTabArea() {
        int height = tabsPaged()
                ? tabDownButtonY() + TAB_PAGE_BTN_H - TAB_Y0
                : tabs.size() * (TAB_SIZE + TAB_GAP) - TAB_GAP;
        return new Rect2i(leftPos - TAB_SIZE, topPos + TAB_Y0, TAB_SIZE + 1, height);
    }

    /** 鼠标所在的分类标签下标(tabs 中的绝对下标),不在标签上返回 -1 */
    private int tabIndexAt(double mouseX, double mouseY) {
        for (VisibleTab tab : visibleTabs()) {
            if (inRect(mouseX, mouseY, leftPos - TAB_SIZE, topPos + tab.y(), TAB_SIZE, TAB_SIZE)) {
                return tab.index();
            }
        }
        return -1;
    }

    /** 鼠标是否在标签列区域内(含翻页按钮) */
    private boolean isOverTabArea(double mouseX, double mouseY) {
        Rect2i area = getTabArea();
        return inRect(mouseX, mouseY, area.getX(), area.getY(), area.getWidth(), area.getHeight());
    }

    /** 鼠标所在的翻页按钮:-1 上翻,1 下翻,不在按钮上返回 0 */
    private int tabPageButtonAt(double mouseX, double mouseY) {
        if (!tabsPaged()) return 0;
        int x = leftPos - TAB_SIZE + TAB_PAGE_BTN_X;
        if (inRect(mouseX, mouseY, x, topPos + tabUpButtonY(), TAB_PAGE_BTN_W, TAB_PAGE_BTN_H)) return -1;
        if (inRect(mouseX, mouseY, x, topPos + tabDownButtonY(), TAB_PAGE_BTN_W, TAB_PAGE_BTN_H)) return 1;
        return 0;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 不画原版的标题与"物品栏"标签:装备名在铭牌里,其余文字都在背景层绘制
    }

    private void renderGrid(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int startIndex = scrollRow * GRID_COLS;
        int selectedX = Integer.MIN_VALUE;
        int selectedY = 0;
        for (int i = 0; i < GRID_ROWS * GRID_COLS; i++) {
            int index = startIndex + i;
            if (index >= filtered.size()) break;
            RecipeHolder<EquipmentForgeRecipe> holder = filtered.get(index);
            int x = leftPos + GRID_X + (i % GRID_COLS) * CELL;
            int y = topPos + GRID_Y + (i / GRID_COLS) * CELL;

            // 材料够的格子右下角带黄铜角标;不够的格子把物品压暗
            boolean craftable = craftableIds.contains(holder.id());
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, craftable ? SLOT_READY_SPRITE : SLOT_SPRITE,
                    x, y, CELL, CELL);

            ItemStack result = holder.value().resultView();
            graphics.item(result, x + 1, y + 1);
            graphics.itemDecorations(font, result, x + 1, y + 1);
            // 新渲染管线没有 z 值,同一图层内按提交顺序叠放:物品之后画即盖在物品上
            if (!craftable) {
                graphics.fill(x + 1, y + 1, x + 17, y + 17, OVERLAY_UNCRAFTABLE);
            }
            if (inRect(mouseX, mouseY, x + 1, y + 1, 16, 16)) {
                graphics.fill(x + 1, y + 1, x + 17, y + 17, OVERLAY_HOVER);
            }
            if (holder == selected) {
                selectedX = x;
                selectedY = y;
            }
        }
        // 选中框(四个黄铜夹角 + 紫光内框)外扩 1px,最后画,免得被相邻格子盖住
        if (selectedX != Integer.MIN_VALUE) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SELECTION_SPRITE, selectedX - 1, selectedY - 1,
                    SELECTION_SPRITE_SIZE, SELECTION_SPRITE_SIZE);
        }
    }

    private void renderMaterials(GuiGraphicsExtractor graphics) {
        int x = leftPos + RIGHT_X + 3;
        int y = topPos + MAT_ROW_Y;
        if (selected == null) {
            renderEmptyTray(graphics);
            return;
        }
        List<MaterialRow> materials = sortedMaterials();
        // 只画滚动窗口内的 MAX_MAT_ROWS 行
        int end = Math.min(materials.size(), matScrollRow + MAX_MAT_ROWS);
        for (int i = matScrollRow; i < end; i++) {
            SizedIngredient material = materials.get(i).material();
            ItemStack icon = materialIcon(i);
            if (icon.isEmpty()) continue;

            int owned = EquipmentForgeRecipe.countMatching(menu.playerInventory, menu.netItems(), material.ingredient());
            boolean enough = owned >= material.count();

            // 缺料的行槽位换成红晶框
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, enough ? SLOT_SPRITE : SLOT_SHORT_SPRITE,
                    x - 1, y - 1, CELL, CELL);
            graphics.item(icon, x, y);
            // 栏位变窄:物品名改由悬停提示展示,此处只显示 拥有/所需
            String text = owned + "/" + material.count();
            graphics.text(font, text,
                    leftPos + RIGHT_X + RIGHT_WIDTH - 4 - font.width(text), y + 4,
                    enough ? COLOR_ENOUGH : COLOR_SHORT, true);
            // 行与行之间刻一道分隔线(暗线 + 下方亮线)
            if (i < end - 1) {
                int right = leftPos + RIGHT_X + RIGHT_WIDTH - 2;
                graphics.fill(x + 19, y + 16, right, y + 17, COLOR_LEDGER_DARK);
                graphics.fill(x + 19, y + 17, right, y + 18, COLOR_LEDGER_LIGHT);
            }
            y += CELL;
        }
        // 上/下还有材料未显示时,在材料行上下的空隙里画滚动箭头(贴图含 1px 投影)
        if (matScrollRow > 0) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLL_UP_SPRITE, leftPos + ARROW_X, topPos + ARROW_UP_Y,
                    ARROW_W + 1, ARROW_H + 1);
        }
        if (matScrollRow < maxMatScrollRow()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLL_DOWN_SPRITE, leftPos + ARROW_X, topPos + ARROW_DOWN_Y,
                    ARROW_W + 1, ARROW_H + 1);
        }
    }

    /** 未选中配方时:托盘里浮雕方块顶上的铁砧、金锭与红晶,下方居中显示提示 */
    private void renderEmptyTray(GuiGraphicsExtractor graphics) {
        int anvilX = leftPos + RIGHT_X + (RIGHT_WIDTH - TRAY_EMPTY_ANVIL_W) / 2;
        int anvilY = topPos + MAT_PANEL_Y + 34;
        // 贴图顶部多留 8px 放铁砧上方的金锭与红晶
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TRAY_EMPTY_SPRITE, anvilX, anvilY - 8,
                TRAY_EMPTY_W, TRAY_EMPTY_H);
        int lineY = anvilY + TRAY_EMPTY_ANVIL_H + 8;
        for (FormattedCharSequence line : font.split(SELECT_HINT, RIGHT_WIDTH - 4)) {
            graphics.text(font, line, leftPos + RIGHT_X + (RIGHT_WIDTH - font.width(line)) / 2, lineY,
                    COLOR_TEXT_DIM, false);
            lineY += 9;
        }
    }

    /** 锻造行:箭头(可锻造时注满紫光,点击后重新注光)与产物座(顶部宝石:暗/紫/红) */
    private void renderCraftRow(GuiGraphicsExtractor graphics) {
        boolean craftable = selectedCraftable();
        int arrowX = leftPos + CRAFT_ARROW_X;
        int arrowY = topPos + CRAFT_ARROW_Y;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW_SPRITE, arrowX, arrowY,
                CRAFT_ARROW_W + 1, CRAFT_ARROW_H + 1);
        int fillWidth = Math.round(CRAFT_ARROW_W * arrowFill(craftable));
        if (fillWidth > 0) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW_FILL_SPRITE, CRAFT_ARROW_W, CRAFT_ARROW_H,
                    0, 0, arrowX, arrowY, fillWidth, CRAFT_ARROW_H);
        }

        int x = leftPos + OUTPUT_X;
        int y = topPos + OUTPUT_Y;
        Identifier socket = selected == null ? SOCKET_NONE_SPRITE
                : craftable ? SOCKET_READY_SPRITE : SOCKET_SHORT_SPRITE;
        // 贴图顶部多留 4px 放状态宝石及其光晕
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, socket, x, y - 4, SOCKET_SPRITE_W, SOCKET_SPRITE_H);
        if (selected != null) {
            ItemStack result = selected.value().resultView();
            graphics.item(result, x + 3, y + 3);
            graphics.itemDecorations(font, result, x + 3, y + 3);
            if (!craftable) {
                graphics.fill(x + 2, y + 2, x + OUTPUT_SIZE - 2, y + OUTPUT_SIZE - 2, OVERLAY_OUTPUT_SHORT);
            }
        }
    }

    /** 箭头注光比例:不可锻造为 0;可锻造时常亮,点击锻造后在 CRAFT_ANIM_MS 内从左到右重新注满 */
    private float arrowFill(boolean craftable) {
        if (!craftable) return 0;
        if (craftAnimStart < 0) return 1;
        long elapsed = System.currentTimeMillis() - craftAnimStart;
        if (elapsed >= CRAFT_ANIM_MS) {
            craftAnimStart = -1;
            return 1;
        }
        return (float) elapsed / CRAFT_ANIM_MS;
    }

    private int maxMatScrollRow() {
        return Math.max(0, sortedMaterials().size() - MAX_MAT_ROWS);
    }

    /** 鼠标是否在右侧材料框内(滚动区域) */
    private boolean isOverMaterialPanel(double mouseX, double mouseY) {
        return inRect(mouseX, mouseY, leftPos + RIGHT_X - 1, topPos + MAT_PANEL_Y, RIGHT_WIDTH + 2, MAT_PANEL_H);
    }

    /**
     * 选中配方的材料列表,先按稀有度、再按创造模式物品栏顺序排序(结果缓存,随选中配方变化重算)。
     * 可选物品按 SlotDisplay 只解析一次(与原版配方书、JEI 一致),不再每帧展开标签;
     * 标签类材料以第一个可选物品作为排序代表,避免轮换图标导致顺序抖动。
     */
    private List<MaterialRow> sortedMaterials() {
        if (materialsCachedFor != selected) {
            materialsCachedFor = selected;
            matScrollRow = 0; // 换配方后材料列表回到顶部
            sortedMaterials = selected == null ? List.of()
                    : selected.value().materials().stream()
                            .map(m -> new MaterialRow(m, m.ingredient().display().resolveForStacks(displayContext)))
                            .sorted(Comparator.comparing(MaterialRow::representative, itemOrder))
                            .toList();
        }
        return sortedMaterials;
    }

    /** 第 index 种材料的展示图标,标签材料每秒轮换一个可用物品 */
    private ItemStack materialIcon(int index) {
        List<ItemStack> options = sortedMaterials.get(index).options();
        if (options.isEmpty()) return ItemStack.EMPTY;
        return options.get((int) (System.currentTimeMillis() / 1000 % options.size()));
    }

    /** 鼠标悬停的材料行下标,不在材料图标上返回 -1 */
    private int materialIndexAt(double mouseX, double mouseY) {
        if (selected == null) return -1;
        int gx = (int) (mouseX - (leftPos + RIGHT_X + 3));
        int gy = (int) (mouseY - (topPos + MAT_ROW_Y));
        if (gx < 0 || gx >= 16 || gy < 0 || gy % CELL >= 16) return -1;
        int row = gy / CELL;
        if (row >= MAX_MAT_ROWS) return -1;
        int index = matScrollRow + row;
        return index < sortedMaterials().size() ? index : -1;
    }

    private void renderMaterialTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int index = materialIndexAt(mouseX, mouseY);
        if (index >= 0) {
            ItemStack icon = materialIcon(index);
            if (!icon.isEmpty()) {
                graphics.setTooltipForNextFrame(font, icon, mouseX, mouseY);
            }
        }
    }

    private void renderGridTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int index = gridIndexAt(mouseX, mouseY);
        if (index >= 0 && index < filtered.size()) {
            ItemStack result = filtered.get(index).value().resultView();
            graphics.setTooltipForNextFrame(font, result, mouseX, mouseY);
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
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (event.button() == 0) {
            if (isFilterHovered(mouseX, mouseY)) {
                craftableOnly = !craftableOnly;
                scrollRow = 0;
                refreshFiltered();
                playClick();
                return true;
            }
            if (isClearHovered(mouseX, mouseY)) {
                selected = null;
                playClick();
                return true;
            }
            // 搜索框只占外框内的一行文字高度,点到外框其余部分也让它获得焦点
            if (isOverSearchFrame(mouseX, mouseY) && !searchBox.isMouseOver(mouseX, mouseY)) {
                setFocused(searchBox);
                return true;
            }
            int pageDir = tabPageButtonAt(mouseX, mouseY);
            if (pageDir != 0) {
                // 边界处翻不动也响声音,给出按钮被点到的反馈
                turnTabPage(pageDir);
                playClick();
                return true;
            }
            int tab = tabIndexAt(mouseX, mouseY);
            if (tab >= 0) {
                activeCategory = tabs.get(tab).category();
                scrollRow = 0;
                refreshFiltered();
                playClick();
                return true;
            }
            int index = gridIndexAt(mouseX, mouseY);
            if (index >= 0 && index < filtered.size()) {
                selected = filtered.get(index);
                playClick();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // 仅在物品格区域内滚动翻页,避免影响背包区域
        if (gridIndexAt(mouseX, mouseY) >= 0) {
            scrollRow = Mth.clamp(scrollRow - (int) Math.signum(scrollY), 0, maxScrollRow());
            return true;
        }
        // 标签列:分页时滚轮直接翻页,翻动了才响声音(边界处静默,避免滚轮连响)
        if (tabsPaged() && isOverTabArea(mouseX, mouseY)) {
            if (turnTabPage(-(int) Math.signum(scrollY))) {
                playClick();
            }
            return true;
        }
        // 右侧材料框:材料多于可见行数时同样支持滚动
        if (isOverMaterialPanel(mouseX, mouseY)) {
            int max = maxMatScrollRow(); // 先取上限:换了配方时它会把 matScrollRow 归零
            matScrollRow = Mth.clamp(matScrollRow - (int) Math.signum(scrollY), 0, max);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // 搜索框聚焦时拦截按键,避免按 E 关闭界面
        if (searchBox.isFocused() && !event.isEscape()) {
            return searchBox.keyPressed(event) || true;
        }
        return super.keyPressed(event);
    }

    /** 锻纹印章按钮:八角黄铜印章刻着铁砧,可锻造时紫光从刻纹里透出;平常/悬停/按下/不可用四态 */
    private static class SealButton extends Button {
        private boolean held;

        SealButton(int x, int y, OnPress onPress) {
            super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, CRAFT_LABEL, onPress, DEFAULT_NARRATION);
            setTooltip(Tooltip.create(CRAFT_LABEL));
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            held = true;
            super.onClick(event, doubleClick);
        }

        @Override
        public void onRelease(MouseButtonEvent event) {
            held = false;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            Identifier sprite = !active ? SEAL_DISABLED_SPRITE
                    : held && isHovered() ? SEAL_PRESSED_SPRITE
                    : isHoveredOrFocused() ? SEAL_HIGHLIGHTED_SPRITE
                    : SEAL_SPRITE;
            // 贴图四周各多 1px:左上是光晕,右下是投影
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, getX() - 1, getY() - 1,
                    SEAL_SPRITE_SIZE, SEAL_SPRITE_SIZE);
        }
    }
}

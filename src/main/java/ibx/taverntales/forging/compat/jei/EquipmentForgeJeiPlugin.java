package ibx.taverntales.forging.compat.jei;

import ibx.taverntales.forging.TavernTalesEquipmentForge;
import ibx.taverntales.forging.client.ClientForgeRecipes;
import ibx.taverntales.forging.client.ClientLootBags;
import ibx.taverntales.forging.client.CreativeOrder;
import ibx.taverntales.forging.client.screen.EquipmentForgeScreen;
import ibx.taverntales.forging.compat.jer.JustEnoughResourcesCompat;
import ibx.taverntales.forging.lootbag.LootBagDrop;
import ibx.taverntales.forging.recipe.EquipmentForgeRecipe;
import ibx.taverntales.forging.registry.ModDataComponents;
import ibx.taverntales.forging.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@JeiPlugin
public class EquipmentForgeJeiPlugin implements IModPlugin {
    // JEI 在注册表事件之前就会类加载插件,这里不能调用 DeferredHolder.get()
    public static final IRecipeHolderType<EquipmentForgeRecipe> EQUIPMENT_FORGE =
            IRecipeHolderType.create(Identifier.fromNamespaceAndPath(TavernTalesEquipmentForge.MODID, "equipment_forge"));

    public static final IRecipeType<LootBagDisplay> LOOT_BAG =
            IRecipeType.create(TavernTalesEquipmentForge.MODID, "loot_bag", LootBagDisplay.class);

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(TavernTalesEquipmentForge.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new EquipmentForgeRecipeCategory(
                registration.getJeiHelpers().getGuiHelper(), maxMaterialCount()));

        // 没有 JER 就没人能解析战利品表,整个分类不注册(而不是注册一个永远空着的分类)
        if (JustEnoughResourcesCompat.isLoaded()) {
            registration.addRecipeCategories(new LootBagCategory(
                    registration.getJeiHelpers().getGuiHelper(), lootBagDisplays()));
        }
    }

    /**
     * 由客户端缓存(服务端同步来的表)构建展示项。JEI 启动时若同步包还没到就是空的,
     * 稍后 {@link #onRuntimeAvailable} 注册的回调会把数据补上。
     */
    private static List<LootBagDisplay> lootBagDisplays() {
        if (!JustEnoughResourcesCompat.isLoaded()) return List.of();
        Map<String, LootTable> tables = ClientLootBags.tables();
        // 每次重算一份:创造栏内容会随数据包/配置变化,不能缓存
        Comparator<LootBagDrop> byDrop = dropOrder(CreativeOrder.build());
        return tables.entrySet().stream()
                // 按类型名排序,免得 JEI 里的顺序随 HashMap 迭代顺序乱跳
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    List<LootBagDrop> drops = JustEnoughResourcesCompat.toDrops(e.getValue())
                            .stream().sorted(byDrop).toList();
                    return drops.isEmpty() ? null : new LootBagDisplay(e.getKey(), bagStack(e.getKey()), drops);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * 掉落排序:概率从大到小 → 稀有度(普通→史诗) → 创造模式物品栏顺序。
     * 与锻造界面的排序同源(见 {@link CreativeOrder}),最后用物品 id 兜底保证顺序稳定。
     */
    private static Comparator<LootBagDrop> dropOrder(Map<Item, Integer> creativeOrder) {
        return Comparator
                .comparingDouble((LootBagDrop d) -> d.chance()).reversed()
                .thenComparingInt(d -> d.item().getRarity().ordinal())
                .thenComparingInt(d -> creativeOrder.getOrDefault(d.item().getItem(), Integer.MAX_VALUE))
                .thenComparing(d -> BuiltInRegistries.ITEM.getKey(d.item().getItem()).toString());
    }

    private static ItemStack bagStack(String type) {
        ItemStack stack = new ItemStack(ModItems.LOOT_BAG.get());
        stack.set(ModDataComponents.LOOT_BAG_TYPE.get(), type);
        return stack;
    }

    /** 已加载配方中最多的材料数,决定 JEI 分类的行数(分类尺寸无法逐配方变化);无配方时至少留一行 */
    private static int maxMaterialCount() {
        return loadedRecipes().stream()
                .mapToInt(h -> h.value().materials().size())
                .max()
                .orElse(1);
    }

    /** 客户端缓存里的配方:原版不再同步配方,由服务端经 NeoForge 代发(见 ClientForgeRecipes) */
    private static List<RecipeHolder<EquipmentForgeRecipe>> loadedRecipes() {
        return ClientForgeRecipes.all();
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(EQUIPMENT_FORGE, loadedRecipes());
        if (JustEnoughResourcesCompat.isLoaded()) {
            registration.addRecipes(LOOT_BAG, lootBagDisplays());
        }
    }

    /**
     * JEI 与袋子同步包谁先到没有保证:
     * 同步先到 → {@link #registerRecipes} 直接就读到了;JEI 先起来 → 靠这个回调补。
     */
    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        if (!JustEnoughResourcesCompat.isLoaded()) return;
        ClientLootBags.setUpdateListener(() ->
                runtime.getRecipeManager().addRecipes(LOOT_BAG, lootBagDisplays()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(EQUIPMENT_FORGE, ModItems.EQUIPMENT_FORGE.get());
        if (JustEnoughResourcesCompat.isLoaded()) {
            // 空袋作为触媒:在 JEI 里查战利品袋这个物品,就能看到所有袋子的掉落
            registration.addCraftingStation(LOOT_BAG, ModItems.LOOT_BAG.get());
        }
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

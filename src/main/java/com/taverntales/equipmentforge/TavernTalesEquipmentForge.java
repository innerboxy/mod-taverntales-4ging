package com.taverntales.equipmentforge;

import com.mojang.logging.LogUtils;
import com.taverntales.equipmentforge.config.Config;
import com.taverntales.equipmentforge.network.ModNetworking;
import com.taverntales.equipmentforge.registry.ModBlocks;
import com.taverntales.equipmentforge.registry.ModConditions;
import com.taverntales.equipmentforge.registry.ModDataComponents;
import com.taverntales.equipmentforge.registry.ModItems;
import com.taverntales.equipmentforge.registry.ModLootContextParamSets;
import com.taverntales.equipmentforge.registry.ModMenus;
import com.taverntales.equipmentforge.registry.ModRecipes;
import com.taverntales.equipmentforge.registry.ModRegistries;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

@Mod(TavernTalesEquipmentForge.MODID)
public class TavernTalesEquipmentForge {
    public static final String MODID = "taverntales_4ging";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TavernTalesEquipmentForge(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModRecipes.RECIPE_TYPES.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        ModConditions.CONDITION_CODECS.register(modEventBus);
        ModDataComponents.COMPONENTS.register(modEventBus);
        modEventBus.addListener(ModNetworking::register);
        modEventBus.addListener(ModRegistries::register);
        modEventBus.addListener(this::addCreative);

        // 战利品袋的战利品表参数集:没有注册表事件可挂,构造器里直接写静态表,赶在数据包加载之前
        ModLootContextParamSets.register();

        // 战利品表纯服务端,客户端 JEI 要展示袋子掉落只能靠同步(游戏事件总线,非 mod 总线)
        NeoForge.EVENT_BUS.addListener(ModNetworking::onDatapackSync);

        // 通用配置:控制是否移除原版木质配方
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    //创造物品栏位:功能方块分类,同时兼容服务器整合包的 kubejs 专属分类
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.EQUIPMENT_FORGE);
        }
        // 战利品袋放原材料栏最前面。这里给的是空袋(不带 loot_bag_type 组件)——袋子种类由数据包定义,
        // 数量不定且随数据包变化,创造栏不宜穷举;要具体袋子请用 /give 指定组件。
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.insertFirst(new ItemStack(ModItems.LOOT_BAG.get()), TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}

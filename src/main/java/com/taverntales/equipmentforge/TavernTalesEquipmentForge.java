package com.taverntales.equipmentforge;

import com.mojang.logging.LogUtils;
import com.taverntales.equipmentforge.config.Config;
import com.taverntales.equipmentforge.network.ModNetworking;
import com.taverntales.equipmentforge.registry.ModBlocks;
import com.taverntales.equipmentforge.registry.ModConditions;
import com.taverntales.equipmentforge.registry.ModItems;
import com.taverntales.equipmentforge.registry.ModMenus;
import com.taverntales.equipmentforge.registry.ModRecipes;
import com.taverntales.equipmentforge.registry.ModRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
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
        modEventBus.addListener(ModNetworking::register);
        modEventBus.addListener(ModRegistries::register);
        modEventBus.addListener(this::addCreative);

        // 通用配置:控制是否移除原版木质配方
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    //创造物品栏位:功能方块分类,同时兼容服务器整合包的 kubejs 专属分类
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.EQUIPMENT_FORGE);
        }
    }
}

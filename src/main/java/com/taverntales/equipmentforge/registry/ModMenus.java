package com.taverntales.equipmentforge.registry;

import com.taverntales.equipmentforge.menu.EquipmentForgeMenu;
import com.taverntales.equipmentforge.TavernTalesEquipmentForge;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, TavernTalesEquipmentForge.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<EquipmentForgeMenu>> EQUIPMENT_FORGE =
            MENUS.register("equipment_forge",
                    () -> new MenuType<>(EquipmentForgeMenu::new, FeatureFlags.DEFAULT_FLAGS));
}

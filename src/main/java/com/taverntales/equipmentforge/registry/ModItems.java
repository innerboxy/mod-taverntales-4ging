package com.taverntales.equipmentforge.registry;

import com.taverntales.equipmentforge.TavernTalesEquipmentForge;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TavernTalesEquipmentForge.MODID);

    //装备锻造台
    public static final DeferredItem<Item> EQUIPMENT_FORGE =
            ITEMS.register("equipment_forge", () -> new BlockItem(
                    ModBlocks.EQUIPMENT_FORGE.get(), new Item.Properties()
            ));
}

package com.taverntales.equipmentforge.registry;

import com.taverntales.equipmentforge.TavernTalesEquipmentForge;
import com.taverntales.equipmentforge.lootbag.LootBagItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TavernTalesEquipmentForge.MODID);

    //装备锻造台
    public static final DeferredItem<Item> EQUIPMENT_FORGE =
            ITEMS.register("equipment_forge", () -> new BlockItem(
                    ModBlocks.EQUIPMENT_FORGE.get(), new Item.Properties()
            ));

    //战利品袋:史诗品质,掉落物形态不会被岩浆/火烧毁,也不会被爆炸炸掉(后者见 LootBagItem#canBeHurtBy)
    public static final DeferredItem<Item> LOOT_BAG =
            ITEMS.register("loot_bag", () -> new LootBagItem(
                    new Item.Properties().rarity(Rarity.EPIC).fireResistant()
            ));
}

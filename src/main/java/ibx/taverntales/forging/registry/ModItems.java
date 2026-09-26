package ibx.taverntales.forging.registry;

import ibx.taverntales.forging.TavernTalesEquipmentForge;
import ibx.taverntales.forging.lootbag.LootBagItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TavernTalesEquipmentForge.MODID);

    //装备锻造台。1.21.2 起 Properties 必须带上注册 id,registerSimpleBlockItem / registerItem 会代为 setId
    public static final DeferredItem<BlockItem> EQUIPMENT_FORGE =
            ITEMS.registerSimpleBlockItem("equipment_forge", ModBlocks.EQUIPMENT_FORGE);

    //战利品袋:史诗品质,掉落物形态不会被岩浆/火烧毁,也不会被爆炸炸掉(后者见 LootBagItem#canBeHurtBy)
    public static final DeferredItem<LootBagItem> LOOT_BAG =
            ITEMS.registerItem("loot_bag", LootBagItem::new, properties -> properties
                    .rarity(Rarity.EPIC)
                    .fireResistant());
}

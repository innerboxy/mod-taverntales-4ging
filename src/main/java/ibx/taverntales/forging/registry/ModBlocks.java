package ibx.taverntales.forging.registry;

import ibx.taverntales.forging.block.EquipmentForgeBlock;
import ibx.taverntales.forging.TavernTalesEquipmentForge;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TavernTalesEquipmentForge.MODID);

    //装备锻造台。1.21.2 起 Properties 必须带上注册 id,registerBlock 会代为 setId
    public static final DeferredBlock<EquipmentForgeBlock> EQUIPMENT_FORGE =
            BLOCKS.registerBlock("equipment_forge", EquipmentForgeBlock::new, properties -> properties
                    .mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion());
}

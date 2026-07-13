package com.taverntales.equipmentforge.registry;

import com.taverntales.equipmentforge.block.EquipmentForgeBlock;
import com.taverntales.equipmentforge.TavernTalesEquipmentForge;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TavernTalesEquipmentForge.MODID);

    //装备锻造台
    public static final DeferredBlock<EquipmentForgeBlock> EQUIPMENT_FORGE =
            BLOCKS.register("equipment_forge", () -> new EquipmentForgeBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.5f)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
            ));
}

package com.taverntales.equipmentforge.registry;

import com.mojang.serialization.Codec;
import com.taverntales.equipmentforge.TavernTalesEquipmentForge;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 自定义数据组件。 */
public class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TavernTalesEquipmentForge.MODID);

    /**
     * 战利品袋类型:裸字符串(如 {@code "iron_golem"}),固定解析为战利品表
     * {@code taverntales_4ging:loot_bag/<值>},即 {@code data/taverntales_4ging/loot_table/loot_bag/<值>.json}。
     * 组件不存在或为空串即视为空袋。
     * <p>显示名与颜色一并来自语言文件 {@code loot_bag.taverntales_4ging.<值>}(颜色写成译文里的
     * § 格式码),所以这是袋子唯一需要的组件。
     * <p>可直接用组件语法给予:
     * {@code /give @s taverntales_4ging:loot_bag[taverntales_4ging:loot_bag_type="iron_golem"]}
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> LOOT_BAG_TYPE =
            COMPONENTS.registerComponentType("loot_bag_type", builder -> builder
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8));
}

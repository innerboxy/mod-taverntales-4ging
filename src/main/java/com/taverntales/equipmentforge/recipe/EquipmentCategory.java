package com.taverntales.equipmentforge.recipe;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** 装备分类,对应配方数据中的 category 字段(必填,不允许为空) */
public enum EquipmentCategory implements StringRepresentable {
    MELEE("melee"),
    RANGED("ranged"),
    MAGIC("magic"),
    ARMOR("armor"),
    SHIELD("shield"),
    CURIO("curio");

    public static final Codec<EquipmentCategory> CODEC = StringRepresentable.fromEnum(EquipmentCategory::values);
    public static final StreamCodec<ByteBuf, EquipmentCategory> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(i -> values()[i], EquipmentCategory::ordinal);

    private final String name;

    EquipmentCategory(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}

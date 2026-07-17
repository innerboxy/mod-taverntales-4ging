package ibx.taverntales.forging.network;

import ibx.taverntales.forging.TavernTalesEquipmentForge;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端 -> 服务端:请求按指定配方 id 锻造一次 */
public record CraftEquipmentPayload(ResourceLocation recipeId) implements CustomPacketPayload {
    public static final Type<CraftEquipmentPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TavernTalesEquipmentForge.MODID, "craft_equipment"));

    public static final StreamCodec<ByteBuf, CraftEquipmentPayload> STREAM_CODEC =
            ResourceLocation.STREAM_CODEC.map(CraftEquipmentPayload::new, CraftEquipmentPayload::recipeId);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

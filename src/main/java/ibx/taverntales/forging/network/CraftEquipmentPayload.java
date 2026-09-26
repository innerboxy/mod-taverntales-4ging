package ibx.taverntales.forging.network;

import ibx.taverntales.forging.TavernTalesEquipmentForge;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

/** 客户端 -> 服务端:请求按指定配方 id 锻造一次 */
public record CraftEquipmentPayload(ResourceKey<Recipe<?>> recipeId) implements CustomPacketPayload {
    public static final Type<CraftEquipmentPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(TavernTalesEquipmentForge.MODID, "craft_equipment"));

    public static final StreamCodec<ByteBuf, CraftEquipmentPayload> STREAM_CODEC =
            ResourceKey.streamCodec(Registries.RECIPE).map(CraftEquipmentPayload::new, CraftEquipmentPayload::recipeId);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

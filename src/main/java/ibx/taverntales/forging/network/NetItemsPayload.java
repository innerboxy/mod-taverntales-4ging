package ibx.taverntales.forging.network;

import ibx.taverntales.forging.TavernTalesEquipmentForge;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 服务端 -> 客户端:玩家超越维度网络中的物品快照,供锻造界面的可制作检测与材料计数使用 */
public record NetItemsPayload(List<ItemStack> items) implements CustomPacketPayload {
    public static final Type<NetItemsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TavernTalesEquipmentForge.MODID, "net_items"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NetItemsPayload> STREAM_CODEC =
            ItemStack.LIST_STREAM_CODEC.map(NetItemsPayload::new, NetItemsPayload::items);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package ibx.taverntales.forging.network;

import ibx.taverntales.forging.TavernTalesEquipmentForge;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务端 -> 客户端:全部战利品袋的战利品表,供 JEI 展示掉落。
 *
 * <p>为什么要同步:战利品表是纯服务端的({@code ReloadableServerRegistries}),客户端根本没有;
 * 而 JEI 跑在客户端。JER 自己只从单人内置服务端取表,连专用服务器时拿不到——所以这份数据得我们自己发。
 *
 * <p><b>这等于把袋子内容公开给客户端</b>,是启用 JEI 展示的必然代价。
 *
 * <p><b>上限</b>:全部袋子挤在一个包里,而 {@code ClientboundCustomPayloadPacket} 的上限是 1 MB。
 * 单张表通常只有几 KB,几百个袋子也够用;真撞上了得改成分批发送。
 *
 * @param tables 袋子类型(裸字符串) -> 其战利品表
 */
public record LootBagSyncPayload(Map<String, LootTable> tables) implements CustomPacketPayload {
    public static final Type<LootBagSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TavernTalesEquipmentForge.MODID, "loot_bag_sync"));

    /** 表里含物品/附魔等注册表引用,故须用带注册表的编解码器 */
    public static final StreamCodec<RegistryFriendlyByteBuf, LootBagSyncPayload> STREAM_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, String, LootTable, Map<String, LootTable>>map(
                            HashMap::new,
                            ByteBufCodecs.STRING_UTF8,
                            ByteBufCodecs.fromCodecWithRegistries(LootTable.DIRECT_CODEC))
                    .map(LootBagSyncPayload::new, LootBagSyncPayload::tables);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

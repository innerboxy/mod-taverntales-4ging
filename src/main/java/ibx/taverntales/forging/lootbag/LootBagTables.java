package ibx.taverntales.forging.lootbag;

import ibx.taverntales.forging.TavernTalesEquipmentForge;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.HashMap;
import java.util.Map;

/** 战利品袋表的路径约定,以及服务端侧的枚举。 */
public final class LootBagTables {
    /** 所有袋子表都住在本模组命名空间的这个目录下 */
    public static final String DIRECTORY = "loot_bag/";

    private LootBagTables() {
    }

    /** 袋子类型(裸字符串)-> 战利品表键。非法字符会让 tryBuild 返回 null */
    public static ResourceKey<LootTable> keyOf(String type) {
        ResourceLocation id = ResourceLocation.tryBuild(TavernTalesEquipmentForge.MODID, DIRECTORY + type);
        return id == null ? null : ResourceKey.create(Registries.LOOT_TABLE, id);
    }

    /** 战利品表 id -> 袋子类型;不是袋子表则返回 null */
    private static String typeOf(ResourceLocation id) {
        if (!id.getNamespace().equals(TavernTalesEquipmentForge.MODID)) return null;
        if (!id.getPath().startsWith(DIRECTORY)) return null;
        String type = id.getPath().substring(DIRECTORY.length());
        // 只收一层:loot_bag/a/b 这种子目录拼不回组件里的裸字符串,忽略
        return (type.isEmpty() || type.contains("/")) ? null : type;
    }

    /**
     * 服务端:枚举当前数据包里所有袋子表。任何数据包往
     * {@code data/taverntales_4ging/loot_table/loot_bag/} 塞的文件都会被收进来。
     */
    public static Map<String, LootTable> collect(MinecraftServer server) {
        ReloadableServerRegistries.Holder holder = server.reloadableRegistries();
        Map<String, LootTable> out = new HashMap<>();
        for (ResourceLocation id : holder.getKeys(Registries.LOOT_TABLE)) {
            String type = typeOf(id);
            if (type == null) continue;
            out.put(type, holder.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id)));
        }
        return out;
    }
}

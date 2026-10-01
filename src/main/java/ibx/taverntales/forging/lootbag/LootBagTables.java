package ibx.taverntales.forging.lootbag;

import ibx.taverntales.forging.TavernTalesEquipmentForge;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
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
        Identifier id = Identifier.tryBuild(TavernTalesEquipmentForge.MODID, DIRECTORY + type);
        return id == null ? null : ResourceKey.create(Registries.LOOT_TABLE, id);
    }

    /**
     * 从文件路径截出的类型名是否可用:只收一层,loot_bag/a/b 这种子目录拼不回组件里的裸字符串。
     * 数据侧(战利品表)与资源侧(按类型换皮的模型)共用这条规则。
     */
    public static boolean isValidType(String type) {
        return !type.isEmpty() && !type.contains("/");
    }

    /** 战利品表 id -> 袋子类型;不是袋子表则返回 null */
    private static String typeOf(Identifier id) {
        if (!id.getNamespace().equals(TavernTalesEquipmentForge.MODID)) return null;
        if (!id.getPath().startsWith(DIRECTORY)) return null;
        String type = id.getPath().substring(DIRECTORY.length());
        return isValidType(type) ? type : null;
    }

    /**
     * 服务端:枚举当前数据包里所有袋子表。任何数据包往
     * {@code data/taverntales_4ging/loot_table/loot_bag/} 塞的文件都会被收进来。
     */
    public static Map<String, LootTable> collect(MinecraftServer server) {
        Map<String, LootTable> out = new HashMap<>();
        server.reloadableRegistries().lookup().lookupOrThrow(Registries.LOOT_TABLE).listElements().forEach(holder -> {
            String type = typeOf(holder.key().identifier());
            if (type != null) out.put(type, holder.value());
        });
        return out;
    }
}

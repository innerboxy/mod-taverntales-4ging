package ibx.taverntales.forging.client;

import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Map;

/**
 * 客户端缓存:登录/重载时由服务端同步来的袋子表(见 LootBagSyncPayload)。
 * 客户端本身没有战利品表注册表,这是唯一来源。
 */
public final class ClientLootBags {
    private static Map<String, LootTable> tables = Map.of();

    /** JEI 已启动时,新同步到的数据要主动补进去;为空表示 JEI 还没起来 */
    private static Runnable onUpdate;

    private ClientLootBags() {
    }

    public static Map<String, LootTable> tables() {
        return tables;
    }

    public static void accept(Map<String, LootTable> synced) {
        tables = Map.copyOf(synced);
        if (onUpdate != null) onUpdate.run();
    }

    /**
     * JEI 与同步包谁先到不确定:JEI 先起来就靠这个回调补数据,同步先到则 JEI 启动时直接读 {@link #tables()}。
     * 两条路都走得通,故不必关心顺序。
     */
    public static void setUpdateListener(Runnable listener) {
        onUpdate = listener;
    }
}

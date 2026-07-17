package ibx.taverntales.forging.compat.jer;

import ibx.taverntales.forging.lootbag.LootBagDrop;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.fml.ModList;

import java.util.List;

/**
 * JustEnoughResources 联动:把一张战利品表解析成"可能掉什么、概率多少、几个到几个"。
 *
 * <p>为什么要靠它:原版没有任何公开 API 能枚举战利品表的内容——{@code LootPool.entries}、
 * {@code LootItem.item} 都是 private 且无 getter。JER 内部已经做了这套解析,直接用它比自己
 * 再 AT 一遍私有字段划算。
 *
 * <p>软依赖——所有 JER 类只在嵌套的 {@link Impl} 中引用,未安装 JER 时该内部类不会被加载。
 *
 * <p><b>注意</b>:{@code jeresources.util.LootTableHelper} 不在 JER 的 api 包内,属内部实现,
 * JER 更新时可能不打招呼就改。解析结果只是近似值——权重、条件、嵌套表和 set_count 之类的函数
 * 共同决定实际掉落,任何静态预览都无法完全准确。
 */
public final class JustEnoughResourcesCompat {
    public static final String MOD_ID = "jeresources";
    private static Boolean loaded;

    private JustEnoughResourcesCompat() {
    }

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = ModList.get().isLoaded(MOD_ID);
        }
        return loaded;
    }

    /** 解析失败或未装 JER 时返回空列表,由调用方决定要不要展示 */
    public static List<LootBagDrop> toDrops(LootTable table) {
        if (!isLoaded()) return List.of();
        return Impl.toDrops(table);
    }

    private static final class Impl {
        static List<LootBagDrop> toDrops(LootTable table) {
            return jeresources.util.LootTableHelper.toDrops(table).stream()
                    .map(d -> new LootBagDrop(d.item, d.chance, d.minDrop, d.maxDrop))
                    .toList();
        }
    }
}

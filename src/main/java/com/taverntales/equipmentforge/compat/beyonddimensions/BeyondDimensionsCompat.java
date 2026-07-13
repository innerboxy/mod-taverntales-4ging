package com.taverntales.equipmentforge.compat.beyonddimensions;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/**
 * 超越维度(BeyondDimensions)联动:锻造检测/消耗时把玩家主网络中存储的物品也算进来。
 * 软依赖——所有 BD 类只在嵌套的 {@link Impl} 中引用,未安装 BD 时该内部类不会被加载。
 * 注意:网络存储是服务端 SavedData,以下方法均只能在服务端调用;
 * 客户端界面通过 NetItemsPayload 同步的快照参与显示。
 */
public final class BeyondDimensionsCompat {
    public static final String MOD_ID = "beyonddimensions";
    private static Boolean loaded;

    private BeyondDimensionsCompat() {
    }

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = ModList.get().isLoaded(MOD_ID);
        }
        return loaded;
    }

    /** 服务端:玩家主网络中所有物品的快照(数量裁剪到 int 上限),未装 BD 或无网络时为空 */
    public static List<ItemStack> getStoredItems(Player player) {
        if (!isLoaded()) return List.of();
        return Impl.getStoredItems(player);
    }

    /** 服务端:从玩家主网络提取与材料匹配的物品,返回实际提取数量 */
    public static int extractMatching(Player player, Ingredient ingredient, int amount) {
        if (!isLoaded() || amount <= 0) return 0;
        return Impl.extractMatching(player, ingredient, amount);
    }

    private static final class Impl {
        private static List<ItemStack> getStoredItems(Player player) {
            DimensionsNet net = DimensionsNet.getPrimaryNetFromPlayer(player);
            if (net == null) return List.of();
            List<ItemStack> items = new ArrayList<>();
            for (KeyAmount entry : net.getUnifiedStorage().getStorage()) {
                if (!entry.isEmpty() && entry.key() instanceof ItemStackKey key) {
                    items.add(key.copyStackWithCount(Math.min(entry.amount(), Integer.MAX_VALUE)));
                }
            }
            return items;
        }

        private static int extractMatching(Player player, Ingredient ingredient, int amount) {
            DimensionsNet net = DimensionsNet.getPrimaryNetFromPlayer(player);
            if (net == null) return 0;
            UnifiedStorage storage = net.getUnifiedStorage();
            // 先收集匹配的 key,避免在遍历存储视图时提取导致并发修改
            List<ItemStackKey> matching = new ArrayList<>();
            for (KeyAmount entry : storage.getStorage()) {
                if (!entry.isEmpty() && entry.key() instanceof ItemStackKey key
                        && ingredient.test(key.getReadOnlyStack())) {
                    matching.add(key);
                }
            }
            int extracted = 0;
            for (ItemStackKey key : matching) {
                if (extracted >= amount) break;
                KeyAmount taken = storage.extract(key, amount - extracted, false, false);
                extracted += (int) taken.amount();
            }
            return extracted;
        }
    }
}

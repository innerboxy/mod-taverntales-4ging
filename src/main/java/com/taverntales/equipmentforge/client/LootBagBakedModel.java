package com.taverntales.equipmentforge.client;

import com.taverntales.equipmentforge.registry.ModDataComponents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

import java.util.Map;

/**
 * 战利品袋的模型包装:按 {@code loot_bag_type} 组件挑对应类型的模型。
 *
 * <p>{@link ItemOverrides#resolve} 是原版唯一能"按物品堆换模型"的钩子。1.21.4 的物品模型映射
 * ({@code assets/<ns>/items/*.json} 的 select + component)干的正是这件事,但 1.21.1 没有,
 * 故在此手工实现。将来升到 1.21.4+ 可以整个删掉换成那份 json。
 */
public class LootBagBakedModel extends BakedModelWrapper<BakedModel> {
    private final ItemOverrides overrides;

    /**
     * @param base   回退模型,即 models/item/loot_bag.json(它自己 parent 到 loot_bag/default.json)
     * @param byType 袋子类型 -> 该类型的专属模型
     */
    public LootBagBakedModel(BakedModel base, Map<String, BakedModel> byType) {
        super(base);
        this.overrides = new TypeOverrides(base, byType);
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    private static final class TypeOverrides extends ItemOverrides {
        private final BakedModel fallback;
        private final Map<String, BakedModel> byType;

        TypeOverrides(BakedModel fallback, Map<String, BakedModel> byType) {
            this.fallback = fallback;
            this.byType = byType;
        }

        /** 空袋(无组件)、以及没有专属模型的类型,一律回退。故资源包只需为想换皮的类型建文件 */
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel level,
                                  LivingEntity entity, int seed) {
            String type = stack.get(ModDataComponents.LOOT_BAG_TYPE.get());
            if (type == null) return fallback;
            return byType.getOrDefault(type, fallback);
        }
    }
}

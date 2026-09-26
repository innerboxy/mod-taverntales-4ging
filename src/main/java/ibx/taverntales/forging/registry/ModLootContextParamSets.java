package ibx.taverntales.forging.registry;

import ibx.taverntales.forging.TavernTalesEquipmentForge;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * 战利品袋专用的战利品表参数集,即袋子表 JSON 里的 {@code "type": "taverntales_4ging:loot_bag"}。
 * 参数与原版 chest 集一致:只强制要求 ORIGIN(开袋位置),THIS_ENTITY(开袋玩家)可选,供条件/函数取用。
 */
public class ModLootContextParamSets {
    public static final Identifier LOOT_BAG_ID =
            Identifier.fromNamespaceAndPath(TavernTalesEquipmentForge.MODID, "loot_bag");

    public static final ContextKeySet LOOT_BAG = new ContextKeySet.Builder()
            .required(LootContextParams.ORIGIN)
            .optional(LootContextParams.THIS_ENTITY)
            .build();

    /**
     * 参数集没有注册表事件,只能直接写 {@link LootContextParamSets} 的静态表(其 register() 私有且
     * 强制 minecraft 命名空间,故经 AT 打开 REGISTRY 自行 put)。
     * <p>必须早于数据包加载。注意注册失败是**静默**的:{@code LootTable} 的 type 字段用的是
     * {@code lenientOptionalFieldOf},REGISTRY 里查不到时不报错,而是退回 ALL_PARAMS——那个集合
     * 声称提供一切参数,于是加载和校验全都风平浪静,直到开袋时某个条件/函数取不到参数才抛
     * NoSuchElementException。所以别指望日志报错来发现这里没注册上。
     */
    public static void register() {
        LootContextParamSets.REGISTRY.put(LOOT_BAG_ID, LOOT_BAG);
    }
}

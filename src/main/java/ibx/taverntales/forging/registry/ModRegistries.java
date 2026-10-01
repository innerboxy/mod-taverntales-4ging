package ibx.taverntales.forging.registry;

import ibx.taverntales.forging.recipe.EquipmentCategoryDefinition;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/** 自定义数据包注册表:装备分类。 */
public class ModRegistries {
    // 注册表键放在 minecraft 命名空间,使数据包加载路径为单层
    // data/<包>/category/(与原版数据包注册表一致);非 minecraft 命名空间会变成双层路径。
    // 分类条目 id 仍取各自文件的 Identifier(如 taverntales:melee),不受此影响。
    public static final ResourceKey<Registry<EquipmentCategoryDefinition>> EQUIPMENT_CATEGORY =
            ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("category"));

    /** 传入网络编解码器,使定义随登录同步到客户端(标签渲染 / 物品 tooltip 需要) */
    public static void register(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(EQUIPMENT_CATEGORY,
                EquipmentCategoryDefinition.CODEC, EquipmentCategoryDefinition.CODEC);
    }
}

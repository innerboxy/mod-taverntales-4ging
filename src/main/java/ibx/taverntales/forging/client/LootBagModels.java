package ibx.taverntales.forging.client;

import ibx.taverntales.forging.TavernTalesEquipmentForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * 战利品袋的按类型换皮:资源包往 {@code assets/taverntales_4ging/models/item/loot_bag/} 丢一个
 * {@code <类型>.json} 就生效,不需要改代码,也不需要数据包配合。
 *
 * <p><b>为什么由资源包决定而非数据包</b>:袋子类型是数据包(服务端)定义的,而模型烘焙发生在资源重载时——
 * 远早于登录服务器,那时客户端根本不知道有哪些类型。所以只能反过来:资源包提供了哪些类型的模型,
 * 就认哪些。类型对不上的(没建文件的)一律回退基础模型。
 */
@EventBusSubscriber(modid = TavernTalesEquipmentForge.MODID, value = Dist.CLIENT)
public final class LootBagModels {
    /** 扫描目录。注意这是资源路径,不带 assets/<命名空间>/ 前缀 */
    private static final String MODEL_DIR = "models/item/loot_bag";
    private static final String JSON = ".json";

    /** 回退模型的文件名。它是 models/item/loot_bag.json 的实际内容,不作为袋子类型 */
    private static final String DEFAULT_NAME = "default";

    /** 类型 -> 模型位置。RegisterAdditional 时填,ModifyBakingResult 时取烘焙结果 */
    private static Map<String, ModelResourceLocation> typeModels = Map.of();

    private LootBagModels() {
    }

    /** 扫资源包,把发现的类型模型登记进烘焙队列——不登记的话它们根本不会被加载 */
    @SubscribeEvent
    static void registerAdditional(ModelEvent.RegisterAdditional event) {
        Map<String, ModelResourceLocation> found = new HashMap<>();
        Minecraft.getInstance().getResourceManager()
                .listResources(MODEL_DIR, id -> id.getNamespace().equals(TavernTalesEquipmentForge.MODID)
                        && id.getPath().endsWith(JSON))
                .keySet()
                .forEach(id -> {
                    String type = typeOf(id);
                    if (type == null) return;
                    // 额外模型不走物品模型的 item/ 自动前缀(ModelBakery 直接拿 rl.id() 去查),
                    // 故必须用 standalone 给出含 item/ 的完整路径。用 inventory 会查到 models/loot_bag/…
                    ModelResourceLocation mrl = ModelResourceLocation.standalone(
                            ResourceLocation.fromNamespaceAndPath(
                                    TavernTalesEquipmentForge.MODID, "item/loot_bag/" + type));
                    found.put(type, mrl);
                    event.register(mrl);
                });
        typeModels = Map.copyOf(found);
    }

    /** 资源路径 -> 袋子类型;不是合法的类型模型则返回 null */
    private static String typeOf(ResourceLocation id) {
        String path = id.getPath();
        String prefix = MODEL_DIR + "/";
        if (!path.startsWith(prefix)) return null;
        String type = path.substring(prefix.length(), path.length() - JSON.length());
        // 类型不含斜杠(与数据侧一致,子目录里的不算);default 是回退模型,保留名
        if (type.isEmpty() || type.contains("/") || type.equals(DEFAULT_NAME)) return null;
        return type;
    }

    /** 把袋子的物品模型换成会按组件挑模型的包装 */
    @SubscribeEvent
    static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        if (typeModels.isEmpty()) return;

        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        ModelResourceLocation baseKey = ModelResourceLocation.inventory(
                ResourceLocation.fromNamespaceAndPath(TavernTalesEquipmentForge.MODID, "loot_bag"));
        BakedModel base = models.get(baseKey);
        if (base == null) return;

        Map<String, BakedModel> byType = new HashMap<>();
        typeModels.forEach((type, mrl) -> {
            BakedModel baked = models.get(mrl);
            if (baked != null) byType.put(type, baked);
        });
        if (byType.isEmpty()) return;

        models.put(baseKey, new LootBagBakedModel(base, Map.copyOf(byType)));
        TavernTalesEquipmentForge.LOGGER.debug("战利品袋专属模型:{}", byType.keySet());
    }
}

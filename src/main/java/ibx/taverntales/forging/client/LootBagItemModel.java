package ibx.taverntales.forging.client;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ibx.taverntales.forging.TavernTalesEquipmentForge;
import ibx.taverntales.forging.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 战利品袋的按类型换皮:资源包往 {@code assets/taverntales_4ging/models/item/loot_bag/} 丢一个
 * {@code <类型>.json} 就生效,不需要改代码,也不需要数据包配合。
 *
 * <p>原版 1.21.4 的物品模型映射({@code items/*.json} 的 select + component)要求把每个类型都列死在
 * json 里;这里注册一个自定义物品模型类型 {@code taverntales_4ging:loot_bag},在模型加载时扫描目录,
 * 保住"丢文件即生效"的约定。见 {@code assets/taverntales_4ging/items/loot_bag.json}。
 *
 * <p><b>为什么由资源包决定而非数据包</b>:袋子类型是数据包(服务端)定义的,而模型烘焙发生在资源重载时——
 * 远早于登录服务器,那时客户端根本不知道有哪些类型。所以只能反过来:资源包提供了哪些类型的模型,
 * 就认哪些。空袋(无组件)与没建文件的类型一律回退 {@code fallback}。
 */
@EventBusSubscriber(modid = TavernTalesEquipmentForge.MODID, value = Dist.CLIENT)
public final class LootBagItemModel implements ItemModel {
    /** 扫描目录。注意这是资源路径,不带 assets/<命名空间>/ 前缀 */
    private static final String MODEL_DIR = "models/item/loot_bag";
    private static final String JSON = ".json";

    /** 回退模型的文件名。它是 models/item/loot_bag.json 的实际内容,不作为袋子类型 */
    private static final String DEFAULT_NAME = "default";

    private final ItemModel fallback;
    private final Map<String, ItemModel> byType;

    private LootBagItemModel(ItemModel fallback, Map<String, ItemModel> byType) {
        this.fallback = fallback;
        this.byType = byType;
    }

    @SubscribeEvent
    static void register(RegisterItemModelsEvent event) {
        event.register(Identifier.fromNamespaceAndPath(TavernTalesEquipmentForge.MODID, "loot_bag"), Unbaked.MAP_CODEC);
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
                       ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        output.appendModelIdentityElement(this);
        String type = item.get(ModDataComponents.LOOT_BAG_TYPE.get());
        ItemModel model = type == null ? fallback : byType.getOrDefault(type, fallback);
        model.update(output, item, resolver, displayContext, level, owner, seed);
    }

    /** 扫资源包:袋子类型 -> 该类型的模型 id(含 item/ 前缀,即 models/ 下的路径) */
    private static Map<String, Identifier> scanTypeModels() {
        Map<String, Identifier> found = new HashMap<>();
        Minecraft.getInstance().getResourceManager()
                .listResources(MODEL_DIR, id -> id.getNamespace().equals(TavernTalesEquipmentForge.MODID)
                        && id.getPath().endsWith(JSON))
                .keySet()
                .forEach(id -> {
                    String type = typeOf(id);
                    if (type != null) {
                        found.put(type, Identifier.fromNamespaceAndPath(
                                TavernTalesEquipmentForge.MODID, "item/loot_bag/" + type));
                    }
                });
        return found;
    }

    /** 资源路径 -> 袋子类型;不是合法的类型模型则返回 null */
    private static @Nullable String typeOf(Identifier id) {
        String path = id.getPath();
        String prefix = MODEL_DIR + "/";
        if (!path.startsWith(prefix)) return null;
        String type = path.substring(prefix.length(), path.length() - JSON.length());
        // 类型不含斜杠(与数据侧一致,子目录里的不算);default 是回退模型,保留名
        if (type.isEmpty() || type.contains("/") || type.equals(DEFAULT_NAME)) return null;
        return type;
    }

    /**
     * {@code items/loot_bag.json} 里 {@code "type": "taverntales_4ging:loot_bag"} 的解码结果。
     * 扫描在 resolveDependencies 与 bake 各做一次:前者把类型模型登记进加载队列(不登记就不会被加载),
     * 后者取烘焙结果。同一次资源重载内资源包不变,两次结果一致。
     */
    public record Unbaked(ItemModel.Unbaked fallback) implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ItemModels.CODEC.fieldOf("fallback").forGetter(Unbaked::fallback)
        ).apply(instance, Unbaked::new));

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            fallback.resolveDependencies(resolver);
            scanTypeModels().values().forEach(resolver::markDependency);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            Map<String, ItemModel> byType = new HashMap<>();
            scanTypeModels().forEach((type, model) -> byType.put(type,
                    new CuboidItemModelWrapper.Unbaked(model, Optional.empty(), List.of()).bake(context, transformation)));
            if (!byType.isEmpty()) {
                TavernTalesEquipmentForge.LOGGER.debug("战利品袋专属模型:{}", byType.keySet());
            }
            return new LootBagItemModel(fallback.bake(context, transformation), Map.copyOf(byType));
        }
    }
}

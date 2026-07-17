package ibx.taverntales.forging.lootbag;

import ibx.taverntales.forging.TavernTalesEquipmentForge;
import ibx.taverntales.forging.registry.ModDataComponents;
import ibx.taverntales.forging.registry.ModLootContextParamSets;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.List;
import java.util.Optional;

/**
 * 战利品袋:{@code taverntales_4ging:loot_bag_type} 组件非空且能查到对应战利品表时,右键开袋获得战利品。
 * 空袋不可使用,tooltip 显示"空"。
 */
public class LootBagItem extends Item {
    private static final Component EMPTY_LABEL = Component.translatable("loot_bag.taverntales_4ging.null");

    /** 显示名的翻译键前缀,拼上组件里的裸字符串 */
    private static final String NAME_KEY_PREFIX = "loot_bag.taverntales_4ging.";

    public LootBagItem(Properties properties) {
        super(properties);
    }

    /** 组件里的裸字符串;不存在或为空串返回 null */
    private static String typeOf(ItemStack stack) {
        String type = stack.get(ModDataComponents.LOOT_BAG_TYPE.get());
        return (type == null || type.isEmpty()) ? null : type;
    }

    /** 组件值对应的战利品表键;组件为空或值里混入非法字符时返回 null */
    private static ResourceKey<LootTable> tableKey(ItemStack stack) {
        String type = typeOf(stack);
        return type == null ? null : LootBagTables.keyOf(type);
    }

    /** 服务端查表。不用 reloadableRegistries().getLootTable(),那个查不到会静默返回 EMPTY 表,分不出"空表"和"没这张表" */
    private static Optional<LootTable> lootTable(ServerLevel level, ResourceKey<LootTable> key) {
        return level.getServer().reloadableRegistries().get()
                .lookup(Registries.LOOT_TABLE)
                .flatMap(lookup -> lookup.get(key))
                .map(Holder::value);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ResourceKey<LootTable> key = tableKey(stack);
        if (key == null) return InteractionResultHolder.pass(stack);

        // 战利品表纯服务端持有,客户端无从判断这张表存不存在:凭组件先播动画,真正开袋在服务端
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        Optional<LootTable> found = lootTable((ServerLevel) level, key);
        if (found.isEmpty()) return InteractionResultHolder.pass(stack);

        for (ItemStack loot : rollLoot(found.get(), (ServerLevel) level, player)) {
            ItemHandlerHelper.giveItemToPlayer(player, loot);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS,
                SoundSource.PLAYERS, 1.0f, 1.0f);
        return InteractionResultHolder.success(stack);
    }

    /** 服务端:掷战利品表。参数按 loot_bag 参数集给全——开袋位置,外加玩家供条件/函数使用 */
    private static List<ItemStack> rollLoot(LootTable table, ServerLevel level, Player player) {
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withLuck(player.getLuck())
                .create(ModLootContextParamSets.LOOT_BAG);
        return table.getRandomItems(params);
    }

    /**
     * 掉落物形态弄不坏:免疫一切经 {@code ItemEntity#hurt} 的伤害(爆炸、仙人掌……)。
     * 火焰/岩浆走的是另一条路(实体的 fireImmune),由 Properties 的 fireResistant() 负责。
     * <p>注:虚空是 {@code Entity#onBelowWorld} 直接 discard,不经过 hurt,这里管不到。
     */
    @Override
    public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
        return false;
    }

    /** 掉落物永不消失:ItemEntity 用该值初始化 lifespan,而 age 为 short,永远追不上 */
    @Override
    public int getEntityLifespan(ItemStack stack, Level level) {
        return Integer.MAX_VALUE;
    }

    /** 掉落物常亮发光轮廓(隔墙可见),便于在战场上找到 */
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        entity.setGlowingTag(true);
        return false; // 继续原版的正常更新
    }

    /**
     * 显示名只认语言文件 {@code loot_bag.taverntales_4ging.<type>},没有任何自动推导:
     * 加袋子就必须配译文。颜色也来自译文里的 § 格式码,故这里不附加任何样式。
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String type = typeOf(stack);
        tooltip.add(type == null
                ? EMPTY_LABEL.copy().withStyle(ChatFormatting.GRAY)
                : displayName(type));
    }

    /**
     * 袋子种类的显示名。只认语言文件 {@code loot_bag.taverntales_4ging.<type>},颜色由译文里的
     * § 格式码自带,故不附加任何样式;没写译文(或 type 写错)则返回红色的原文,便于排错。
     * <p>JEI 分类也用它画袋子名,与 tooltip 保持一致。
     */
    public static Component displayName(String type) {
        String key = NAME_KEY_PREFIX + type;
        return Language.getInstance().has(key)
                ? Component.translatable(key)
                : Component.literal(type).withStyle(ChatFormatting.RED);
    }
}

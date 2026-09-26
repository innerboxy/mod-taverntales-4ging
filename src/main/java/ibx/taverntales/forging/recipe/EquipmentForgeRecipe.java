package ibx.taverntales.forging.recipe;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ibx.taverntales.forging.compat.beyonddimensions.BeyondDimensionsCompat;
import ibx.taverntales.forging.registry.ModRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 装备锻造配方:材料直接取自玩家背包(主背包 + 副手),不占用容器格子。
 * 数据包格式见 data/wmserver/recipe/ 下的示例。
 */
public class EquipmentForgeRecipe implements Recipe<RecipeInput> {
    /** 分类 id,对应数据包注册表 taverntales_4ging:equipment_category 中的某个分类 */
    private final Identifier category;
    private final List<SizedIngredient> materials;
    private final ItemStackTemplate result;
    /** 产物的只读展示实例。26.1 起 ItemStack 须在注册表加载后才能构造,故懒创建而非在解码时创建 */
    private ItemStack resultView;

    public EquipmentForgeRecipe(Identifier category, List<SizedIngredient> materials, ItemStackTemplate result) {
        this.category = category;
        this.materials = materials;
        this.result = result;
    }

    public Identifier category() {
        return category;
    }

    public List<SizedIngredient> materials() {
        return materials;
    }

    public ItemStackTemplate result() {
        return result;
    }

    /** 产物的只读展示实例(界面 / JEI 渲染用),调用方不得修改;要发给玩家请用 {@link #assemble} 拿新副本 */
    public ItemStack resultView() {
        if (resultView == null) resultView = result.create();
        return resultView;
    }

    /** 材料的全部可选物品(数量已设为所需数),供界面轮换展示与 JEI 使用;标签材料展开为标签内所有物品 */
    public static List<ItemStack> displayStacks(SizedIngredient material) {
        return material.ingredient().items().map(item -> new ItemStack(item, material.count())).toList();
    }

    /** 26.1 起副手不再是 Inventory 的公开列表,而是装备槽里的一格;返回的是槽内实物,可直接扣减 */
    private static ItemStack offhand(Inventory inventory) {
        return inventory.getItem(Inventory.SLOT_OFFHAND);
    }

    /** 背包(主背包 + 副手)与额外物品列表(如超越维度网络快照)中与该材料匹配的物品总数 */
    public static int countMatching(Inventory inventory, List<ItemStack> extraStacks, Ingredient ingredient) {
        long total = 0;
        for (ItemStack stack : inventory.getNonEquipmentItems()) {
            if (!stack.isEmpty() && ingredient.test(stack)) total += stack.getCount();
        }
        ItemStack offhand = offhand(inventory);
        if (!offhand.isEmpty() && ingredient.test(offhand)) total += offhand.getCount();
        for (ItemStack stack : extraStacks) {
            if (!stack.isEmpty() && ingredient.test(stack)) total += stack.getCount();
        }
        return (int) Math.min(total, Integer.MAX_VALUE);
    }

    /** 材料是否全部足够,extraStacks 为背包之外可用的物品(如超越维度网络快照) */
    public boolean canCraft(Inventory inventory, List<ItemStack> extraStacks) {
        // 逐项扣减快照,避免两种材料匹配同一批物品时重复计数
        List<ItemStack> snapshot = snapshot(inventory, extraStacks);
        for (SizedIngredient material : materials) {
            if (takeFrom(snapshot, material) < material.count()) return false;
        }
        return true;
    }

    /**
     * 服务端:校验并消耗材料,成功返回 true(失败不消耗任何物品)。
     * 背包(主背包 + 副手)优先,不足部分从玩家的超越维度网络提取。
     */
    public boolean consume(Player player) {
        Inventory inventory = player.getInventory();
        List<ItemStack> netItems = BeyondDimensionsCompat.getStoredItems(player);
        if (!canCraft(inventory, netItems)) return false;
        for (SizedIngredient material : materials) {
            int needed = material.count();
            needed -= shrinkMatching(inventory.getNonEquipmentItems(), material.ingredient(), needed);
            if (needed > 0) needed -= shrinkMatching(List.of(offhand(inventory)), material.ingredient(), needed);
            if (needed > 0) BeyondDimensionsCompat.extractMatching(player, material.ingredient(), needed);
        }
        return true;
    }

    private static List<ItemStack> snapshot(Inventory inventory, List<ItemStack> extraStacks) {
        List<ItemStack> copies = new ArrayList<>();
        inventory.getNonEquipmentItems().forEach(s -> { if (!s.isEmpty()) copies.add(s.copy()); });
        ItemStack offhand = offhand(inventory);
        if (!offhand.isEmpty()) copies.add(offhand.copy());
        extraStacks.forEach(s -> { if (!s.isEmpty()) copies.add(s.copy()); });
        return copies;
    }

    private static int takeFrom(List<ItemStack> stacks, SizedIngredient material) {
        int taken = 0;
        for (ItemStack stack : stacks) {
            if (taken >= material.count()) break;
            if (!stack.isEmpty() && material.ingredient().test(stack)) {
                int take = Math.min(stack.getCount(), material.count() - taken);
                stack.shrink(take);
                taken += take;
            }
        }
        return taken;
    }

    private static int shrinkMatching(List<ItemStack> stacks, Ingredient ingredient, int needed) {
        int removed = 0;
        for (ItemStack stack : stacks) {
            if (removed >= needed) break;
            if (!stack.isEmpty() && ingredient.test(stack)) {
                int take = Math.min(stack.getCount(), needed - removed);
                stack.shrink(take);
                removed += take;
            }
        }
        return removed;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false; // 不通过容器匹配,材料检测走 canCraft
    }

    @Override
    public ItemStack assemble(RecipeInput input) {
        return result.create();
    }

    @Override
    public boolean isSpecial() {
        return true; // 不进原版配方书
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    /** 不可放置:没有合成格,原版配方书与自动填充都不适用 */
    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    /** 接口强制要求;配方既 isSpecial 又不可放置,这个分类不会被用到 */
    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_EQUIPMENT;
    }

    @Override
    public RecipeSerializer<EquipmentForgeRecipe> getSerializer() {
        return ModRecipes.EQUIPMENT_FORGE_SERIALIZER.get();
    }

    @Override
    public RecipeType<EquipmentForgeRecipe> getType() {
        return ModRecipes.EQUIPMENT_FORGE_TYPE.get();
    }

    /**
     * 单项材料。沿用 README 里写明的数据包格式 {@code {"item": <物品id>}} / {@code {"tag": <标签id>}} + {@code count},
     * 同时接受 NeoForge 26.1 的嵌套写法 {@code {"ingredient": <原版 ingredient>, "count": n}}。
     * <p>26.1 删掉了 {@code SizedIngredient.FLAT_CODEC},这里先把旧写法改写成嵌套写法,再交给
     * {@code NESTED_CODEC}——标签解析仍走原版 {@code Ingredient.CODEC}(经同一个 ops,即 RegistryOps),
     * 不自己查注册表。编码一律输出嵌套写法。
     */
    public static final Codec<SizedIngredient> MATERIAL_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<SizedIngredient, T>> decode(DynamicOps<T> ops, T input) {
            Dynamic<T> dynamic = new Dynamic<>(ops, input);
            Optional<String> item = dynamic.get("item").asString().result();
            Optional<String> tag = dynamic.get("tag").asString().result();
            if (item.isPresent() && tag.isPresent()) {
                return DataResult.error(() -> "材料不能同时指定 item 和 tag: " + input);
            }
            if (item.isPresent()) {
                dynamic = dynamic.remove("item").set("ingredient", dynamic.createString(item.get()));
            } else if (tag.isPresent()) {
                dynamic = dynamic.remove("tag").set("ingredient", dynamic.createString("#" + tag.get()));
            }
            return SizedIngredient.NESTED_CODEC.decode(ops, dynamic.getValue());
        }

        @Override
        public <T> DataResult<T> encode(SizedIngredient input, DynamicOps<T> ops, T prefix) {
            return SizedIngredient.NESTED_CODEC.encode(input, ops, prefix);
        }
    };

    public static final MapCodec<EquipmentForgeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("category").forGetter(EquipmentForgeRecipe::category),
            MATERIAL_CODEC.listOf().fieldOf("materials").forGetter(EquipmentForgeRecipe::materials),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(EquipmentForgeRecipe::result)
    ).apply(instance, EquipmentForgeRecipe::new));

    /** 原版已不再把配方整体同步给客户端,这个编解码器供 OnDatapackSyncEvent#sendRecipes 使用 */
    public static final StreamCodec<RegistryFriendlyByteBuf, EquipmentForgeRecipe> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, EquipmentForgeRecipe::category,
            SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), EquipmentForgeRecipe::materials,
            ItemStackTemplate.STREAM_CODEC, EquipmentForgeRecipe::result,
            EquipmentForgeRecipe::new
    );

    public static final RecipeSerializer<EquipmentForgeRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);
}

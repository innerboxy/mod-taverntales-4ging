package com.taverntales.equipmentforge.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.taverntales.equipmentforge.compat.beyonddimensions.BeyondDimensionsCompat;
import com.taverntales.equipmentforge.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.ArrayList;
import java.util.List;

/**
 * 装备锻造配方:材料直接取自玩家背包(主背包 + 副手),不占用容器格子。
 * 数据包格式见 data/wmserver/recipe/ 下的示例。
 */
public class EquipmentForgeRecipe implements Recipe<RecipeInput> {
    /** 分类 id,对应数据包注册表 taverntales_4ging:equipment_category 中的某个分类 */
    private final ResourceLocation category;
    private final List<SizedIngredient> materials;
    private final ItemStack result;

    public EquipmentForgeRecipe(ResourceLocation category, List<SizedIngredient> materials, ItemStack result) {
        this.category = category;
        this.materials = materials;
        this.result = result;
    }

    public ResourceLocation category() {
        return category;
    }

    public List<SizedIngredient> materials() {
        return materials;
    }

    /** 背包(主背包 + 副手)与额外物品列表(如超越维度网络快照)中与该材料匹配的物品总数 */
    public static int countMatching(Inventory inventory, List<ItemStack> extraStacks, Ingredient ingredient) {
        long total = 0;
        for (ItemStack stack : inventory.items) {
            if (!stack.isEmpty() && ingredient.test(stack)) total += stack.getCount();
        }
        for (ItemStack stack : inventory.offhand) {
            if (!stack.isEmpty() && ingredient.test(stack)) total += stack.getCount();
        }
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
            needed -= shrinkMatching(inventory.items, material.ingredient(), needed);
            if (needed > 0) needed -= shrinkMatching(inventory.offhand, material.ingredient(), needed);
            if (needed > 0) BeyondDimensionsCompat.extractMatching(player, material.ingredient(), needed);
        }
        return true;
    }

    private static List<ItemStack> snapshot(Inventory inventory, List<ItemStack> extraStacks) {
        List<ItemStack> copies = new ArrayList<>();
        inventory.items.forEach(s -> { if (!s.isEmpty()) copies.add(s.copy()); });
        inventory.offhand.forEach(s -> { if (!s.isEmpty()) copies.add(s.copy()); });
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
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public boolean isSpecial() {
        return true; // 不进原版配方书
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.EQUIPMENT_FORGE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.EQUIPMENT_FORGE_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<EquipmentForgeRecipe> {
        public static final MapCodec<EquipmentForgeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("category").forGetter(EquipmentForgeRecipe::category),
                SizedIngredient.FLAT_CODEC.listOf().fieldOf("materials").forGetter(EquipmentForgeRecipe::materials),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result)
        ).apply(instance, EquipmentForgeRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, EquipmentForgeRecipe> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, EquipmentForgeRecipe::category,
                SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), EquipmentForgeRecipe::materials,
                ItemStack.STREAM_CODEC, r -> r.result,
                EquipmentForgeRecipe::new
        );

        @Override
        public MapCodec<EquipmentForgeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EquipmentForgeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}

package ibx.taverntales.forging.compat.jei;

import ibx.taverntales.forging.client.screen.EquipmentForgeScreen;
import ibx.taverntales.forging.menu.EquipmentForgeMenu;
import ibx.taverntales.forging.recipe.EquipmentForgeRecipe;
import ibx.taverntales.forging.registry.ModMenus;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Optional;

/**
 * JEI 物品转移:锻造台无合成格,材料取自背包/网络,因此这里不移动物品,
 * 而是把 JEI 的「+」按钮当作"跳转"——点击后在打开的锻造界面里选中对应配方。
 */
public class EquipmentForgeTransferHandler
        implements IRecipeTransferHandler<EquipmentForgeMenu, RecipeHolder<EquipmentForgeRecipe>> {

    @Override
    public Class<? extends EquipmentForgeMenu> getContainerClass() {
        return EquipmentForgeMenu.class;
    }

    @Override
    public Optional<MenuType<EquipmentForgeMenu>> getMenuType() {
        return Optional.of(ModMenus.EQUIPMENT_FORGE.get());
    }

    @Override
    public RecipeType<RecipeHolder<EquipmentForgeRecipe>> getRecipeType() {
        return EquipmentForgeJeiPlugin.EQUIPMENT_FORGE;
    }

    @Override
    public IRecipeTransferError transferRecipe(EquipmentForgeMenu container,
                                               RecipeHolder<EquipmentForgeRecipe> recipe,
                                               IRecipeSlotsView recipeSlots, Player player,
                                               boolean maxTransfer, boolean doTransfer) {
        // 返回 null = 允许转移(「+」按钮可用)。点击时此刻仍在 JEI 配方界面,
        // 只记下待选配方,待 JEI 关闭、返回锻造界面后由其渲染时选中。
        if (doTransfer) {
            EquipmentForgeScreen.requestSelect(recipe.id());
        }
        return null;
    }
}

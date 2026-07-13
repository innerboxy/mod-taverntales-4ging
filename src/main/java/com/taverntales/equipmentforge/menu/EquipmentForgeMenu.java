package com.taverntales.equipmentforge.menu;

import com.taverntales.equipmentforge.registry.ModBlocks;
import com.taverntales.equipmentforge.registry.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 装备锻造台菜单:无机器格子,材料直接读取玩家背包。
 * 下半部分展示玩家背包(槽位 0-26)与快捷栏(槽位 27-35)。
 */
public class EquipmentForgeMenu extends AbstractContainerMenu {
    /** 背包槽位区在界面中的起始坐标,Screen 绘制底格时需与此保持一致 */
    public static final int INV_X = 8;
    public static final int INV_Y = 144;
    public static final int HOTBAR_Y = 202;

    public final Inventory playerInventory;
    private final ContainerLevelAccess access;

    /** 客户端构造 */
    public EquipmentForgeMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    /** 服务端构造 */
    public EquipmentForgeMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(ModMenus.EQUIPMENT_FORGE.get(), containerId);
        this.playerInventory = playerInventory;
        this.access = access;

        // 玩家背包
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        // 快捷栏
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, INV_X + col * 18, HOTBAR_Y));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        // 背包与快捷栏之间互移
        if (index < 27) {
            if (!moveItemStackTo(stack, 27, 36, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 27, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.EQUIPMENT_FORGE.get());
    }
}

package ibx.taverntales.forging.block;

import com.mojang.serialization.MapCodec;
import ibx.taverntales.forging.menu.EquipmentForgeMenu;
import ibx.taverntales.forging.network.ModNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/** 装备锻造台:右键打开锻造界面 */
public class EquipmentForgeBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<EquipmentForgeBlock> CODEC = simpleCodec(EquipmentForgeBlock::new);
    private static final Component TITLE = Component.translatable("block.taverntales_4ging.equipment_forge");

    public EquipmentForgeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> new EquipmentForgeMenu(id, inventory, ContainerLevelAccess.create(level, pos)),
                    TITLE
            ));
            // 开界面时同步超越维度网络物品快照,供客户端可制作检测使用
            if (player instanceof ServerPlayer serverPlayer) {
                ModNetworking.syncNetItems(serverPlayer);
            }
        }
        return InteractionResult.SUCCESS;
    }
}

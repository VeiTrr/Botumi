package dev.behindthescenery.botumi.blocks;

import com.mojang.serialization.MapCodec;
import dev.behindthescenery.botumi.Botumi;
import dev.behindthescenery.botumi.blocks.entity.DomeBlockEntity;
import dev.behindthescenery.botumi.registry.BotumiRegistry;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

public class DomeBlock extends BlockWithEntity implements BlockEntityProvider, Waterloggable {
    public static final BooleanProperty WATERLOGGED;

    public static final Identifier ID = Identifier.of(Botumi.MOD_ID, "dome_block");
    public static final MapCodec<DomeBlock> CODEC = createCodec(DomeBlock::new);

    static {
        WATERLOGGED = Properties.WATERLOGGED;
    }

    public DomeBlock() {
        this(Settings.copy(Blocks.STONE).nonOpaque());
        this.setDefaultState(this.stateManager.getDefaultState().with(WATERLOGGED, false));
    }

    public DomeBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    public VoxelShape makeShape() {
        VoxelShape shape = VoxelShapes.empty();
        shape = VoxelShapes.union(shape, Block.createCuboidShape(0, 0, 0, 16, 4, 16));
        shape = VoxelShapes.union(shape, Block.createCuboidShape(0, 12, 0, 16, 16, 16));
        shape = VoxelShapes.union(shape, Block.createCuboidShape(0, 4, 0, 4, 12, 4));
        shape = VoxelShapes.union(shape, Block.createCuboidShape(12, 4, 0, 16, 12, 4));
        shape = VoxelShapes.union(shape, Block.createCuboidShape(0, 4, 12, 4, 12, 16));
        shape = VoxelShapes.union(shape, Block.createCuboidShape(12, 4, 12, 16, 12, 16));
        shape = VoxelShapes.union(shape, Block.createCuboidShape(4, 4, 4, 12, 12, 12));
        return shape;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return makeShape();
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DomeBlockEntity(pos, state);
    }

    protected FluidState getFluidState(BlockState state) {
        return (Boolean) state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if ((Boolean) state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{WATERLOGGED});
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof DomeBlockEntity dbe) {
                player.openHandledScreen(dbe);
                return ActionResult.CONSUME;
            }
        }
        return ActionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, BotumiRegistry.DOME_BLOCK_ENTITY, DomeBlockEntity::tick);
    }
}

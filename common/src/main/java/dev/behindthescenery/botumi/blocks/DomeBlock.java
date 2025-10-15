package dev.behindthescenery.botumi.blocks;

import com.mojang.serialization.MapCodec;
import dev.behindthescenery.botumi.Botumi;
import dev.behindthescenery.botumi.blocks.entity.DomeBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class DomeBlock extends BlockWithEntity implements BlockEntityProvider, Waterloggable {
    public static final BooleanProperty WATERLOGGED;

    public static final Identifier ID = Identifier.of(Botumi.MOD_ID, "dome_block");
    public static final MapCodec<DomeBlock> CODEC = createCodec(DomeBlock::new);

    static {
        WATERLOGGED = BooleanProperty.of("waterlogged");
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
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
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
}

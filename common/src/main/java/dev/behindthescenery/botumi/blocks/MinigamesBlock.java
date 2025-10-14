package dev.behindthescenery.botumi.blocks;

import com.mojang.serialization.MapCodec;
import dev.behindthescenery.botumi.Botumi;
import dev.behindthescenery.botumi.blocks.entity.MinigamesBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class MinigamesBlock extends BlockWithEntity implements BlockEntityProvider, Waterloggable {
    public static final BooleanProperty WATERLOGGED;

    public static final Identifier ID = Identifier.of(Botumi.MOD_ID, "minigames_block");
    public static final MapCodec<MinigamesBlock> CODEC = createCodec(MinigamesBlock::new);

    static {
        WATERLOGGED = Properties.WATERLOGGED;
    }

    public MinigamesBlock() {
        this(Settings.copy(Blocks.STONE).nonOpaque());
        this.setDefaultState(this.stateManager.getDefaultState().with(WATERLOGGED, false));
    }

    public MinigamesBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    public VoxelShape makeShape() {
        VoxelShape shape = VoxelShapes.empty();
        shape = VoxelShapes.union(shape, Block.createCuboidShape(0, 14, 0, 16, 15, 16));
        shape = VoxelShapes.union(shape, Block.createCuboidShape(2, 14, 2, 14, 16, 14));
        shape = VoxelShapes.union(shape, Block.createCuboidShape(0, 0, 0, 16, 2, 16));
        shape = VoxelShapes.union(shape, Block.createCuboidShape(1, 2, 1, 15, 14, 15));
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
        return new MinigamesBlockEntity(pos, state);
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
            if (be instanceof MinigamesBlockEntity minibe) {
                player.openHandledScreen(minibe);
                return ActionResult.CONSUME;
            }
        }
        return ActionResult.SUCCESS;
    }
}

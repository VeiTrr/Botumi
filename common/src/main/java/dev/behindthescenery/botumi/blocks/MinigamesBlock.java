package dev.behindthescenery.botumi.blocks;

import com.mojang.serialization.MapCodec;
import dev.behindthescenery.botumi.Botumi;
import dev.behindthescenery.botumi.blocks.entity.MinigamesBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class MinigamesBlock extends BlockWithEntity implements BlockEntityProvider {

    public static Identifier ID = Identifier.of(Botumi.MOD_ID, "minigames_block");

    public MinigamesBlock() {
        super(Block.Settings.copy(Blocks.STONE));
    }


    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    public VoxelShape makeShape() {
        VoxelShape shape = VoxelShapes.empty();
        return shape;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return makeShape();
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return null;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MinigamesBlockEntity(pos, state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient && world.getServer() != null) {
            if (player.isSneaking()) {
                return onShiftRightClick(state, world, pos, player, hit);
            } else {
                return onRightClick(state, world, pos, player, hit);
            }
        }
        return super.onUse(state, world, pos, player, hit);
    }

    protected ActionResult onRightClick(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        MinigamesBlockEntity be = (MinigamesBlockEntity) world.getBlockEntity(pos);
        if (be != null) {
            player.sendMessage(Text.of("Minigame Type: " + be.MinigameType), false);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    protected ActionResult onShiftRightClick(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        MinigamesBlockEntity be = (MinigamesBlockEntity) world.getBlockEntity(pos);
        if (be != null) {
            if (be.MinigameType.equals("none")) {
                be.MinigameType = "example_minigame";
            } else if (be.MinigameType.equals("example_minigame")) {
                be.MinigameType = "another_minigame";
            } else {
                be.MinigameType = "none";
            }
            be.markDirty();
            player.sendMessage(Text.of("Minigame Type set to: " + be.MinigameType), false);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

}

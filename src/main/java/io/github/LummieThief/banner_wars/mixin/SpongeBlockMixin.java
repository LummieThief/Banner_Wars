package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import static net.minecraft.block.Block.dropStacks;

@Mixin(SpongeBlock.class)
public class SpongeBlockMixin {
    @Shadow @Final private static final Direction[] field_43257 = Direction.values();

    /**
     * @author LummieThief
     * @reason I'm currently unaware of any other way to inject into a lambda, and all the sponge logic occurs in a lambda
     */
    @Overwrite
    private boolean absorbWater(World world, BlockPos pos) {
        return BlockPos.iterateRecursively(pos, 6, 65, (currentPos, queuer) -> {
            for (Direction direction : field_43257) {
                queuer.accept(currentPos.offset(direction));
            }

        }, (currentPos) -> {
            if (currentPos.equals(pos)) {
                return true;
            } else {
                BlockState blockState = world.getBlockState(currentPos);
                FluidState fluidState = world.getFluidState(currentPos);
                if (!fluidState.isIn(FluidTags.WATER)) {
                    return false;
                } else {
                    Block block = blockState.getBlock();
                    if (block instanceof FluidDrainable fluidDrainable) {
                        if (TerritoryManager.HasPermission(world, pos, currentPos) && !fluidDrainable.tryDrainFluid(world, currentPos, blockState).isEmpty()) {
                            return true;
                        }
                    }

                    if (blockState.getBlock() instanceof FluidBlock) {
                        if (TerritoryManager.HasPermission(world, pos, currentPos))
                            world.setBlockState(currentPos, Blocks.AIR.getDefaultState(), 3);
                    } else {
                        if (!blockState.isOf(Blocks.KELP) && !blockState.isOf(Blocks.KELP_PLANT) && !blockState.isOf(Blocks.SEAGRASS) && !blockState.isOf(Blocks.TALL_SEAGRASS)) {
                            return false;
                        }

                        if (TerritoryManager.HasPermission(world, pos, currentPos)) {
                            BlockEntity blockEntity = blockState.hasBlockEntity() ? world.getBlockEntity(currentPos) : null;
                            dropStacks(blockState, world, currentPos, blockEntity);
                            world.setBlockState(currentPos, Blocks.AIR.getDefaultState(), 3);
                        }
                    }

                    return true;
                }
            }
        }) > 1;
    }
}

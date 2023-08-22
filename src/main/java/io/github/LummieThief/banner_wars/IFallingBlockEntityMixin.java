package io.github.LummieThief.banner_wars;

import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;

public interface IFallingBlockEntityMixin {
    default void setSpawnPos(BlockPos pos) {}
}

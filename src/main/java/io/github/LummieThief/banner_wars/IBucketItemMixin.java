package io.github.LummieThief.banner_wars;

import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;

public interface IBucketItemMixin {
    default Fluid getFluid() {
        return Fluids.EMPTY;
    }
}

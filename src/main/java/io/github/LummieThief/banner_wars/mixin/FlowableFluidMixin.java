package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlowableFluid.class)
public class FlowableFluidMixin {
    @Inject(method = "flow", at = @At("HEAD"), cancellable = true)
    private void overrideFlow(WorldAccess world, BlockPos pos, BlockState state, Direction direction, FluidState fluidState, CallbackInfo info) {
        if (direction == direction.DOWN)
            return;

        BlockPos fluidPos = pos.subtract(direction.getVector());

        if (!TerritoryManager.HasPermission(fluidPos, pos)) {
            info.cancel();
        }
    }
}

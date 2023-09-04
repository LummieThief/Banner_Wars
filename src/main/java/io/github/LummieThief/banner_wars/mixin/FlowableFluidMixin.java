package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlowableFluid.class)
public class FlowableFluidMixin {
    @Inject(method = "flow", at = @At("HEAD"), cancellable = true)
    private void overrideFlow(WorldAccess world, BlockPos pos, BlockState state, Direction direction, FluidState fluidState, CallbackInfo info) {
        boolean overworld = world.getDimension().bedWorks();
        if (world.isClient() || !overworld)
            return;
        BlockPos fluidPos = pos.subtract(direction.getVector());
        MinecraftServer server = world.getServer();
        if (server == null) return;
        if (!TerritoryManager.HasPermission(server.getWorld(World.OVERWORLD), fluidPos, pos)) {
            info.cancel();
        }
    }
}

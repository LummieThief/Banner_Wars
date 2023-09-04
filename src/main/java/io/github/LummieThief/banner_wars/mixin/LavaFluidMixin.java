package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.LavaFluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LavaFluid.class)
public class LavaFluidMixin {
    @Redirect(method = "onRandomTick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    private boolean overrideLightFire(World world, BlockPos pos, BlockState state,
                                         World _world, BlockPos lavaPos, FluidState _state, Random random) {
        if (world.isClient || !world.getRegistryKey().equals(World.OVERWORLD))
            return world.setBlockState(pos, state);
        if (TerritoryManager.HasPermission(world, lavaPos, pos)) {
            return world.setBlockState(pos, state);
        }
        return false;
    }

}

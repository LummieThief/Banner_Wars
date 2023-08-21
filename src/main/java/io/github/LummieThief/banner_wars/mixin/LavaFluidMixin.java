package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.AbstractFireBlock;
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
/*    @Inject(method = "canLightFire", at = @At("RETURN"), cancellable = true)
    private void overrideCanLightFire(WorldView world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            cir.setReturnValue(canSpreadToChunk(lavaPos_bw, pos));
        }
    }*/

    @Redirect(method = "onRandomTick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    private boolean overrideFirstCanLightFire(World world, BlockPos pos, BlockState state,
                                         World _world, BlockPos _pos, FluidState _state, Random random) {
        BlockPos lavaPos = _pos;
        if (canSpreadToChunk(lavaPos, pos)) {
            return world.setBlockState(pos, state);
        }
        else {
            return false;
        }
    }

    private boolean canSpreadToChunk(BlockPos sourcePos, BlockPos spreadPos) {
        String blockBanner = TerritoryManager.GetBannerFromChunk(spreadPos.getX() >> 4, spreadPos.getZ() >> 4);
        if (blockBanner == null) {
            return true;
        }
        String lavaBanner = TerritoryManager.GetBannerFromChunk(sourcePos.getX() >> 4, sourcePos.getZ() >> 4);
        return blockBanner.equals(lavaBanner);
    }
}

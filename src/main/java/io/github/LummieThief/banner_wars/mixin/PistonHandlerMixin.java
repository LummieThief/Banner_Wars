package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PistonHandler.class)
public class PistonHandlerMixin {
    @Shadow @Final private List<BlockPos> movedBlocks;
    @Shadow @Final private Direction motionDirection;
    @Shadow @Final private BlockPos posTo;
    @Shadow @Final private BlockPos posFrom;

    @Inject(method = "calculatePush", at = @At(value="RETURN"), cancellable = true)
    private void overrideTryMove(CallbackInfoReturnable<Boolean> cir) {
        TerritoryManager.LOGGER.info("from: " + posFrom.toString() + " to: " + posTo.toString());
        // get the banner that the piston block is in
        String pistonBanner = TerritoryManager.GetBannerFromChunk(posFrom.getX() >> 4, posFrom.getZ() >> 4);
        String toBanner, fromBanner;

        // check if the block directly in front of the piston is in a different claim
        toBanner = TerritoryManager.GetBannerFromChunk(posTo.getX() >> 4, posTo.getZ() >> 4);
        if (toBanner != null && !toBanner.equals(pistonBanner)) {
            cir.setReturnValue(false);
        }
        else {
            // loop over all the blocks that will be moved by the piston
            for (BlockPos pos : movedBlocks) {
                BlockPos toPos = pos.add(motionDirection.getVector());
                // get the banner of the chunk the block is currently in and the chunk the block will be pushed into
                toBanner = TerritoryManager.GetBannerFromChunk(toPos.getX() >> 4, toPos.getZ() >> 4);
                fromBanner = TerritoryManager.GetBannerFromChunk(pos.getX() >> 4, pos.getZ() >> 4);
                // check if either of those blocks are in a different claim than the piston
                if ((toBanner != null && !toBanner.equals(pistonBanner)) ||
                        (fromBanner != null && !fromBanner.equals(pistonBanner))) {
                    cir.setReturnValue(false);
                    break;
                }
            }
        }
    }
}

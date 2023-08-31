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
        // Checks if the piston has permission to interact with the block directly in front of it.
        BlockPos pistonFrom = posFrom;
        BlockPos pistonTo = posTo;
        if (!TerritoryManager.HasPermission(pistonFrom, pistonTo)) {
            cir.setReturnValue(false);
        }
        else {
            // loop over all the blocks that will be moved by the piston
            for (BlockPos blockFrom : movedBlocks) {
                BlockPos blockTo = blockFrom.add(motionDirection.getVector());
                // checks if all of those blocks are in a chunk the piston can interact with and will be moving into a chunk the piston can interact with
                if (!TerritoryManager.HasPermission(pistonFrom, blockTo) || !TerritoryManager.HasPermission(pistonFrom, blockFrom)) {
                    cir.setReturnValue(false);
                    break;
                }
            }
        }
    }
}

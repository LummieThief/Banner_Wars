package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
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
    @Shadow @Final private BlockPos posFrom;
    @Shadow @Final private World world;

    @Shadow @Final private Direction pistonDirection;

    @Inject(method = "calculatePush", at = @At(value="RETURN"), cancellable = true)
    private void overrideTryMove(CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient || !world.getRegistryKey().equals(World.OVERWORLD))
            return;
        // Checks if the piston has permission to interact with the block directly in front of it.
        boolean cancelled = false;
        BlockPos pistonFrom = posFrom;
        BlockPos pistonTo = posFrom.add(pistonDirection.getVector());
        if (!TerritoryManager.HasPermission(world, pistonFrom, pistonTo)) {
            cir.setReturnValue(false);
        }
        else {
            // loop over all the blocks that will be moved by the piston
            for (BlockPos blockFrom : movedBlocks) {
                BlockPos blockTo = blockFrom.add(motionDirection.getVector());
                // checks if all of those blocks are in a chunk the piston can interact with and will be moving into a chunk the piston can interact with
                if (!cancelled && !TerritoryManager.HasPermission(world,pistonFrom, blockTo) || !TerritoryManager.HasPermission(world,pistonFrom, blockFrom)) {
                    cancelled = true;
                }

                BlockState state = world.getBlockState(blockFrom);
                world.updateListeners(blockFrom, state, state, Block.NOTIFY_LISTENERS);
                BlockState nextState = world.getBlockState(blockTo);
                world.updateListeners(blockTo, nextState, nextState, Block.NOTIFY_LISTENERS);
            }
        }

        if (cancelled)
            cir.setReturnValue(false);
    }
}

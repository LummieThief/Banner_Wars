package io.github.LummieThief.banner_wars.mixin;

import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockWithEntity.class)
public abstract class BlockWithEntityMixin extends AbstractBlock {
    public BlockWithEntityMixin(Settings settings) {
        super(settings);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
    }
}

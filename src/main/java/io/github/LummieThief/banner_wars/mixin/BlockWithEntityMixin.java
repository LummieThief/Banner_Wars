package io.github.LummieThief.banner_wars.mixin;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.item.VerticallyAttachableBlockItem;
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
        if (state.getBlock().asItem() instanceof VerticallyAttachableBlockItem)
            world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
        super.scheduledTick(state, world, pos, random);
    }
}

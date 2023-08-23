package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.*;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
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
        //TODO: FIX THIS
        TerritoryManager.LOGGER.info("Im gonna need to fix that");
        world.getRandomAlivePlayer().networkHandler.sendPacket(new BlockUpdateS2CPacket(pos, world.getBlockState(pos)));
        world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), Block.NOTIFY_LISTENERS);
    }
}

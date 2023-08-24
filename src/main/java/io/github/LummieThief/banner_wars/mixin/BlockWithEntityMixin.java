package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.IBlockWithEntityMixin;
import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;

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

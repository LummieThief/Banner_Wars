package io.github.LummieThief.banner_wars;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class BreakBlockHandler implements PlayerBlockBreakEvents.Before{
    @Override
    public boolean beforeBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (!(player instanceof ServerPlayerEntity) || world.isClient)
            return true;
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        boolean hasPermission = TerritoryManager.HasPermission(player, pos);
        if (!hasPermission) {
            BlockState worldState = world.getBlockState(pos);
            serverPlayer.networkHandler.sendPacket(new BlockUpdateS2CPacket(pos, worldState));
            world.updateListeners(pos, worldState, worldState, Block.NOTIFY_LISTENERS);
            world.scheduleBlockTick(pos, state.getBlock(), 10);
        }
        return hasPermission;
    }
}

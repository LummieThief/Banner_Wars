package io.github.LummieThief.banner_wars;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BannerItem;
import net.minecraft.item.Item;
import net.minecraft.item.VerticallyAttachableBlockItem;
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
        BlockState worldState = world.getBlockState(pos);
        Block block = state.getBlock();
        Item item = block.asItem();
        if (!hasPermission && item instanceof VerticallyAttachableBlockItem) {
            TerritoryManager.LOGGER.info("scheduling");
            world.scheduleBlockTick(pos, state.getBlock(), 10);
        }
        return hasPermission;
    }
}

package io.github.LummieThief.banner_wars;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class AfterBreakBlockHandler implements PlayerBlockBreakEvents.After{
    @Override
    public void afterBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (world.isClient || !world.getRegistryKey().equals(World.OVERWORLD) || !(blockEntity instanceof BannerBlockEntity bannerBlockEntity))
            return;
        ItemStack stack = bannerBlockEntity.getPickStack();
        if (stack.isEmpty())
            return;
        String pattern = TerritoryManager.BannerToString(stack);
        assert pattern != null; // pattern cannot be null because we already check for instanceof BannerBlockEntity
        if (pattern.equals(TerritoryManager.GetBannerInChunk(pos)) && pos.equals(TerritoryManager.GetBannerPosInChunk(pos))) {
            TerritoryManager.RemoveChunk(pos);
        }
    }
}

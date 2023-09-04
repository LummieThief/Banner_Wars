package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.*;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(AbstractBannerBlock.class)
public abstract class AbstractBannerBlockMixin extends BlockWithEntity {
    protected AbstractBannerBlockMixin(Settings settings) {
        super(settings);
    }
    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        List<ItemStack> items = super.getDroppedStacks(state, builder);
        if (items.size() == 0)
            return items;
        ItemStack firstItem = items.get(0);
        // if the dropped item is a banner
        if (TerritoryManager.IsBanner(firstItem)) {
            // drop the default stack version of it instead
            items.set(0, firstItem.getItem().getDefaultStack());
        }
        return items;
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.afterBreak(world, player, pos, state, blockEntity, tool);
        if (world.isClient || !world.getRegistryKey().equals(World.OVERWORLD) || !(blockEntity instanceof BannerBlockEntity bannerBlockEntity))
            return;
        ItemStack stack = bannerBlockEntity.getPickStack();
        if (stack == null || stack.isEmpty())
            return;
        String pattern = TerritoryManager.BannerToString(stack);
        if (pattern.equals(TerritoryManager.GetBannerInChunk(pos)) && pos.equals(TerritoryManager.GetBannerPosInChunk(pos))) {
            TerritoryManager.RemoveChunk(pos);
        }
    }
}

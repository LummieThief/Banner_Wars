package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.*;
import net.minecraft.item.*;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
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
            Vec3d origin = builder.get(LootContextParameters.ORIGIN);
            BlockPos pos = new BlockPos((int)origin.getX(), (int)origin.getY(), (int)origin.getZ());
            // if the dropped item is the same banner as the one that may or may not be claiming this chunk
            if (TerritoryManager.BannerToString(firstItem).equals(TerritoryManager.GetBannerInChunk(pos)) && builder.getWorld().getRegistryKey().equals(World.OVERWORLD)) {
                // remove the claim in this chunk
                TerritoryManager.RemoveChunk(pos);
            }
        }
        return items;
    }
}

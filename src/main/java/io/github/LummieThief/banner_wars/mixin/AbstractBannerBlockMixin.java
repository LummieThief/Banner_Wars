package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.*;
import net.minecraft.item.*;
import net.minecraft.loot.context.LootContextParameterSet;
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
}

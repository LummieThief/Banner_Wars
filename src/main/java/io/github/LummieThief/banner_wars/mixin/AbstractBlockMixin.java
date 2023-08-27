package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(AbstractBlock.class)
public class AbstractBlockMixin {
    @Inject(method = "getDroppedStacks", at = @At("RETURN"), cancellable = true)
    public void modifyDrops(BlockState state, LootContextParameterSet.Builder builder, CallbackInfoReturnable<List<ItemStack>> cir) {
        List<ItemStack> items = cir.getReturnValue();
        if (items.size() == 0)
            return;
        ItemStack firstItem = items.get(0);
        if (TerritoryManager.isBanner(firstItem)) {
            items.set(0, firstItem.getItem().getDefaultStack());
        }
        cir.setReturnValue(items);
    }
}

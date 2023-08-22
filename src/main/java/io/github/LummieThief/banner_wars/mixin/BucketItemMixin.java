package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.IBucketItemMixin;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.BucketItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin implements IBucketItemMixin {
    @Shadow @Final private Fluid fluid;
    @Override
    public Fluid getFluid() {
        return fluid;
    }
}

package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {
    @Inject(method = "hasBindingCurse", at = @At("HEAD"), cancellable = true)
    private static void giveBannerBinding(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack != null && TerritoryManager.HasPattern(stack))
            cir.setReturnValue(true);
    }
}

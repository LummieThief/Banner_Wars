package io.github.LummieThief.banner_wars.mixin;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;

@Mixin(ExperienceOrbEntity.class)
public class ExperienceOrbMixin {
    @Inject(method = "repairPlayerGears",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ExperienceOrbEntity;getMendingRepairAmount(I)I"),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true)
    public void PreventElytraRepair(PlayerEntity player, int arg1, CallbackInfoReturnable<Integer> cir, Map.Entry<EquipmentSlot, ItemStack> entry, ItemStack itemStack) {
        if (player.getWorld().isClient)
            return;
        if (itemStack.getDamage() == itemStack.getMaxDamage() - 1 && itemStack.isOf(Items.ELYTRA)) {
            cir.setReturnValue(arg1);
            cir.cancel();
        }
    }
}

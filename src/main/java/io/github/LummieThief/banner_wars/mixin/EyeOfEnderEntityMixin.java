package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.IEyeOfEnderEntityMixin;
import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.entity.EyeOfEnderEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EyeOfEnderEntity.class)
public class EyeOfEnderEntityMixin implements IEyeOfEnderEntityMixin {
    @Shadow private boolean dropsItem;
    @Unique
    private boolean tracking = false;

    @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EyeOfEnderEntity;dropsItem:Z", opcode = Opcodes.GETFIELD))
    private boolean injected(EyeOfEnderEntity instance) {
        if (tracking)
            return false;
        return dropsItem;
    }

    @Override
    public void markTracker() {
        tracking = true;
        TerritoryManager.LOGGER.info("tracker");
    }
}

package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.IEyeOfEnderEntityMixin;
import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.entity.EyeOfEnderEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

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
    @ModifyArg(method = "tick", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V",
            ordinal = 1),
            index = 0)
    private ParticleEffect changeParticle(ParticleEffect parameters) {
        if (tracking)
            return ParticleTypes.EFFECT;
        else
            return ParticleTypes.PORTAL;

    }

    @Override
    public void markTracker() {
        tracking = true;
        TerritoryManager.LOGGER.info("tracker");
    }
}

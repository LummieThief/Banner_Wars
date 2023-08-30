package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.IFireworkRocketEntityMixin;
import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin implements IFireworkRocketEntityMixin {
    @Shadow public abstract ItemStack getStack();

    @Override
    public void triggerExplosion() {
        FireworkRocketEntity rocket = ((FireworkRocketEntity)(Object)this);
        rocket.getWorld().sendEntityStatus(rocket, (byte)17);
        rocket.discard();
    }

    @Inject(method = "explodeAndRemove", at = @At("HEAD"))
    public void logNBT(CallbackInfo ci) {
        TerritoryManager.LOGGER.info(this.getStack().getNbt().toString());
    }

}

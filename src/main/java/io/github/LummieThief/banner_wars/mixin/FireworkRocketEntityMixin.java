package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.IFireworkRocketEntityMixin;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin implements IFireworkRocketEntityMixin {
    @Override
    public void triggerExplosion() {
        FireworkRocketEntity rocket = ((FireworkRocketEntity)(Object)this);
        rocket.getWorld().sendEntityStatus(rocket, (byte)17);
        rocket.discard();
    }
}

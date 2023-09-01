package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow public abstract World getEntityWorld();

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    public void overrideIsInvulnerableTo(DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        if (getEntityWorld().isClient || !getEntityWorld().getRegistryKey().equals(World.OVERWORLD))
            return;
        Entity t = ((Entity)(Object)this);
        if (t instanceof HostileEntity || t instanceof PlayerEntity)
            return;
        BlockPos pos = t.getBlockPos();
        if (damageSource.getAttacker() instanceof ServerPlayerEntity attacker)
        {
            if (!TerritoryManager.HasPermission(this.getEntityWorld(), attacker, pos)) {
                cir.setReturnValue(true);
            }
        }
        else if (TerritoryManager.HasBannerInChunk(pos) && !TerritoryManager.InDecay(this.getEntityWorld(), null, TerritoryManager.GetBannerInChunk(pos))) {
            cir.setReturnValue(true);
        }
    }
}

package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    public void overrideIsInvulnerableTo(DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        Entity t = ((Entity)(Object)this);
        if (t instanceof HostileEntity)
            return;
        BlockPos pos = t.getBlockPos();
        if (damageSource.getAttacker() instanceof ServerPlayerEntity)
        {
            ServerPlayerEntity attacker = (ServerPlayerEntity)damageSource.getAttacker();
            if (!TerritoryManager.HasPermission(attacker, pos)) {
                cir.setReturnValue(true);
            }
        }
        else if (TerritoryManager.HasBannerInChunk(pos)) {
            cir.setReturnValue(true);
        }
    }
}

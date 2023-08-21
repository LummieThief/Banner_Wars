package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmallFireballEntity.class)
public class SmallFireballEntityMixin {

    @Inject(method = "onBlockHit", at = @At("HEAD"), cancellable = true)
    protected void overrideOnBlockHit(BlockHitResult blockHitResult, CallbackInfo ci) {
        BlockPos pos = blockHitResult.getBlockPos().offset(blockHitResult.getSide());
        if (TerritoryManager.HasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            ci.cancel();
        }
    }
}

package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.Set;

@Mixin(Explosion.class)
public class ExplosionMixin {
    @Inject(
            method = "collectBlocksAndDamageEntities",
            at = @At(value="INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;addAll(Ljava/util/Collection;)Z"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    public void captureSet(CallbackInfo ci, Set<BlockPos> set) {
        Iterator<BlockPos> itr = set.iterator();
        while (itr.hasNext()) {
            BlockPos pos = itr.next();
            if (TerritoryManager.HasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                itr.remove();
            }
        }
    }
}
package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.Set;

@Mixin(Explosion.class)
public class ExplosionMixin {
    @Shadow @Final private World world;

    @Inject(
            method = "collectBlocksAndDamageEntities",
            at = @At(value="INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;addAll(Ljava/util/Collection;)Z"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    public void captureSet(CallbackInfo ci, Set<BlockPos> set) {
        if (world.isClient || !world.getRegistryKey().equals(World.OVERWORLD) || set == null)
            return;
        Iterator<BlockPos> itr = set.iterator();
        String banner;
        while (itr.hasNext()) {
            BlockPos pos = itr.next();
            banner = TerritoryManager.GetBannerInChunk(pos);
            if (banner != null && !TerritoryManager.InDecay(world, pos, banner)) {
                itr.remove();
            }
        }
    }
}

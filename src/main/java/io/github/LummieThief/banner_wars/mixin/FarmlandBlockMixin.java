package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FarmlandBlock.class)
public class FarmlandBlockMixin {

    @Redirect(method = "onLandedUpon",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/block/FarmlandBlock;setToDirt(Lnet/minecraft/entity/Entity;Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V"))
    public void preventStomping(Entity entity, BlockState state, World world, BlockPos pos) {
        if (!world.getRegistryKey().equals(World.OVERWORLD))
            return;
        String banner = TerritoryManager.GetBannerInChunk(pos);
        if (banner != null && !TerritoryManager.InDecay(world, pos, banner)) {
            // the territory is claimed by someone. Don't break the farmland unless you are the player who owns it.
            if (entity instanceof ServerPlayerEntity player && TerritoryManager.HasPermission(world, player, pos)) {
                FarmlandBlock.setToDirt(entity, state, world, pos);
            }
        }
        else {
            // the territory is claimed by no one
            FarmlandBlock.setToDirt(entity, state, world, pos);
        }
    }

}


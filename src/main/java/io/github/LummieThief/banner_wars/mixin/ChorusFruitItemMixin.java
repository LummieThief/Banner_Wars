package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ChorusFruitItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChorusFruitItem.class)
public class ChorusFruitItemMixin {
    @Redirect(method = "finishUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;teleport(DDDZ)Z"))
    public boolean PreventTeleportationIntoClaim(LivingEntity instance, double x, double y, double z, boolean particleEffects) {
        if (!(instance instanceof ServerPlayerEntity player))
            return instance.teleport(x, y, z, particleEffects);

        BlockPos teleportPos = new BlockPos((int)x, (int)y, (int)z);
        if (TerritoryManager.HasPermission(player.getWorld(), player, teleportPos)) {
            return instance.teleport(x, y, z, particleEffects);
        }
        return false;
    }
}

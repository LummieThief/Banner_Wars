package io.github.LummieThief.banner_wars.mixin;

import net.minecraft.network.packet.c2s.play.QueryBlockNbtC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Inject(method = "onQueryBlockNbt", at = @At("HEAD"), cancellable = true)
    public void cancelQuery(QueryBlockNbtC2SPacket packet, CallbackInfo ci) {
        ci.cancel();
    }
}

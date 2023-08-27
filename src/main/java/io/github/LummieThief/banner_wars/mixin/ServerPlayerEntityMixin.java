package io.github.LummieThief.banner_wars.mixin;

import com.mojang.authlib.GameProfile;
import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {
    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(method = "copyFrom", at = @At("RETURN"))
    public void keepBannerAfterSpawn(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        this.getInventory().clone(oldPlayer.getInventory());
    }

    @Override
    protected void dropInventory() {
        ServerPlayerEntity player = (ServerPlayerEntity)(Object)this;
        PlayerInventory inventory = player.getInventory();
        ItemStack headStack = inventory.getArmorStack(3); //head slot = 39 sometimes
        boolean isBanner = TerritoryManager.isBanner(headStack);
        if (isBanner) {
            player.getInventory().removeStack(39);
        }

        super.dropInventory();

        if (isBanner) {
            inventory.setStack(39, headStack);
        }
    }
}

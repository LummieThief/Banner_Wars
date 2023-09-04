package io.github.LummieThief.banner_wars.mixin;

import com.mojang.authlib.GameProfile;
import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
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
        ItemStack headStack = getEquippedStack(EquipmentSlot.HEAD);
        boolean isBanner = TerritoryManager.IsBanner(headStack);
        if (isBanner) {
            player.getInventory().removeStack(39); // head slot = 39
        }

        super.dropInventory();

        if (isBanner) {
            equipStack(EquipmentSlot.HEAD, headStack);
        }
    }

    @Inject(method = "playerTick", at = @At("RETURN"))
    public void applyTerritoryStatus(CallbackInfo ci) {
        int STATUS_EFFECT_CHECK_TIME = 60;
        if (this.age % STATUS_EFFECT_CHECK_TIME == 0) {
            String banner =  TerritoryManager.GetBannerInChunk(this.getBlockPos());
            if (banner != null) {
                if (TerritoryManager.HasPermission(getEntityWorld(), this, this.getBlockPos())) {
                    if (!TerritoryManager.InDecay(getEntityWorld(),null, banner)) {
                        StatusEffectInstance effect = new StatusEffectInstance(StatusEffects.HASTE, STATUS_EFFECT_CHECK_TIME + 20, 0, true, false, true);
                        this.addStatusEffect(effect, this);
                    }
                }
                else {
                    StatusEffectInstance effect = new StatusEffectInstance(StatusEffects.MINING_FATIGUE, STATUS_EFFECT_CHECK_TIME + 20, 0, true, false, true);
                    this.addStatusEffect(effect, this);
                }
            }

        }
    }
    @Override
    protected void onKilledBy(@Nullable LivingEntity adversary) {
        super.onKilledBy(adversary);
        if (!(adversary instanceof PlayerEntity)) return;
        ItemStack headStack = this.getEquippedStack(EquipmentSlot.HEAD);
        if (TerritoryManager.IsBanner(headStack) && TerritoryManager.HasPattern(headStack)) {
            String pattern = TerritoryManager.BannerToString(headStack);
            assert pattern != null; // we just checked isBanner and hasPattern
            if (TerritoryManager.DecayBanner(pattern, this.getEntityName())) {
                String cmd = String.format("/tellraw @a [{\"text\":\"[Server] \"},{\"text\":\"%s\",\"color\":\"yellow\"}," +
                        "{\"text\":\" has fallen in battle! Their territory is now open to attack for the next 15 minutes.\",\"color\":\"red\"}]", this.getEntityName());
                TerritoryManager.ExecuteCommand(cmd);
            }
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount)
    {
        boolean b = super.damage(source, amount);
        if (amount > 0 && source.getAttacker() != null && source.getAttacker().isPlayer()) {
            ItemStack headStack = getEquippedStack(EquipmentSlot.HEAD);
            ItemStack otherStack = ((PlayerEntity)source.getAttacker()).getEquippedStack(EquipmentSlot.HEAD);

            if (!TerritoryManager.IsBanner(headStack) || !headStack.equals(otherStack)) {
                ItemStack chestStack = getEquippedStack(EquipmentSlot.CHEST);
                TerritoryManager.LOGGER.info(chestStack.toString());
                if (chestStack.getItem().equals(Items.ELYTRA)) {
                    if (chestStack.getDamage() != chestStack.getMaxDamage() - 1) {
                        chestStack.setDamage(chestStack.getMaxDamage() - 1);
                        Criteria.ITEM_DURABILITY_CHANGED.trigger((ServerPlayerEntity)(Object)this, chestStack, 1);
                        sendEquipmentBreakStatus(EquipmentSlot.CHEST);
                        ScreenHandler screenHandler = currentScreenHandler;
                        int slot = 38;
                        ((ServerPlayerEntity)(Object)this).networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(
                                screenHandler.syncId,
                                screenHandler.getRevision(),
                                slot,
                                chestStack));
                    }
                }
            }
        }
        return b;
    }
}

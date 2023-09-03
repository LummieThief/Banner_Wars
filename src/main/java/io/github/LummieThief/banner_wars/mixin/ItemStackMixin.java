package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ClickType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// The first line of defense for grief protection. This cancels actions such as putting glow ink on a sign, stripping
// logs with an axe, or placing an armor stand. It doesnt stop blocks from being placed other than vertically attachable
// blocks which need their own special handling. This is also the place where banners are prevented from being placed
// if the player is not wearing a matching banner.
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow public abstract Item getItem();

    @Redirect(method = "useOnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/Item;useOnBlock(Lnet/minecraft/item/ItemUsageContext;)Lnet/minecraft/util/ActionResult;"))
    private ActionResult overrideUseOnBlock(Item item, ItemUsageContext context) {
        if (context.getWorld().isClient || !context.getWorld().getRegistryKey().equals(World.OVERWORLD))
            return item.useOnBlock(context);

        PlayerEntity playerEntity = context.getPlayer();
        if (!(playerEntity instanceof ServerPlayerEntity) || (item instanceof BlockItem && !(item instanceof VerticallyAttachableBlockItem))) {
            return item.useOnBlock(context);
        }
        ServerPlayerEntity player = (ServerPlayerEntity)playerEntity;

        ItemStack stack = (ItemStack)(Object)this;
        boolean territorialPermission = TerritoryManager.HasPermission(context.getWorld(), context.getPlayer(), context.getBlockPos());
        boolean personalPermission = !TerritoryManager.IsBanner(stack) ||
                (TerritoryManager.BannerToString(stack).equals(TerritoryManager.BannerToString(context.getPlayer().getInventory().getArmorStack(3))) &&
                        !TerritoryManager.HasBannerInChunk(context.getBlockPos()) ||
                        !TerritoryManager.HasPattern(stack));


        if (territorialPermission && personalPermission) {
            return item.useOnBlock(context);
        }
        else {
            Hand hand = context.getHand();
            ScreenHandler screenHandler = player.currentScreenHandler;
            PlayerInventory inventory = player.getInventory();

            int slot = hand == Hand.OFF_HAND ? 45 : PlayerInventory.MAIN_SIZE + inventory.selectedSlot;
            player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(
                    screenHandler.syncId,
                    screenHandler.getRevision(),
                    slot,
                    player.getStackInHand(hand)));
            return ActionResult.FAIL;
        }
    }

    @Inject(method = "onClicked", at = @At("HEAD"))
    public void EquipOrBurnBanner(ItemStack stack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference, CallbackInfoReturnable<Boolean> cir) {
        if (player.getWorld().isClient || stack == null || stack.isEmpty() || slot.getIndex() != 39 || !(slot.inventory instanceof PlayerInventory)) {
            return;
        }
        ItemStack equippedStack = (ItemStack)(Object)this;
        if (TerritoryManager.HasPattern(stack)) {
            // attempting to put a banner with a pattern in the head slot

            if (EnchantmentHelper.hasBindingCurse(equippedStack) || TerritoryManager.IsBanner(equippedStack) ||
                    TerritoryManager.HasBetrayal(player.getEntityName(), TerritoryManager.BannerToString(stack)))
                return;

            boolean b = false;
            if (stack.getCount() > 1) {
                // if we have multiple banners in our hand, we can only equip one if the helmet slot is currently empty.
                if (!slot.hasStack()) {
                    ItemStack newStack = stack.copyWithCount(1);
                    slot.setStack(newStack);
                    stack.setCount(stack.getCount() - 1);
                    b = true;
                }
            } else {
                // single banner in hand
                if (slot.hasStack()) {
                    slot.setStack(stack);
                    cursorStackReference.set(equippedStack);
                } else {
                    slot.setStack(stack.copyAndEmpty());
                }
                b = true;
            }
            if (b) {
                player.onEquipStack(EquipmentSlot.HEAD, equippedStack, Items.LEATHER_HELMET.getDefaultStack());
                if (stack.getItem() instanceof BannerItem bannerItem) {
                    TerritoryManager.CreateFireworkEffect(player.getWorld(), player.getX(), player.getY() + 2.5, player.getZ(),
                            BannerBlockEntity.getPatternsFromNbt(bannerItem.getColor(), BannerBlockEntity.getPatternListNbt(stack)));
                }
            }
        }
        else if (clickType.equals(ClickType.LEFT) && stack.isOf(Items.FLINT_AND_STEEL) && TerritoryManager.IsBanner(equippedStack)) {
            slot.inventory.removeStack(slot.getIndex());
            TerritoryManager.AddBetrayal(player.getEntityName(), TerritoryManager.BannerToString((ItemStack)(Object)this));

            LightningEntity lightningEntity = EntityType.LIGHTNING_BOLT.create(player.getWorld());
            if (lightningEntity != null) {
                lightningEntity.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(player.getBlockPos()));
                lightningEntity.setChanneler((ServerPlayerEntity) player);
                player.getWorld().spawnEntity(lightningEntity);
            }
        }
    }
}

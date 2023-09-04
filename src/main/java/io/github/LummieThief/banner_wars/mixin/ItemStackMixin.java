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
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ClickType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// The first line of defense for grief protection. This cancels actions such as putting glow ink on a sign, stripping
// logs with an axe, or placing an armor stand. It doesn't stop blocks from being placed other than vertically attachable
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
        if (!(playerEntity instanceof ServerPlayerEntity player) || (item instanceof BlockItem && !(item instanceof VerticallyAttachableBlockItem))) {
            return item.useOnBlock(context);
        }

        ItemStack stack = (ItemStack)(Object)this;
        boolean territorialPermission = TerritoryManager.HasPermission(context.getWorld(), context.getPlayer(), context.getBlockPos());

        // the player has personal permission to place a block if it isn't a banner, it is a blank banner, or it is a banner that matches the banner
        // on their head and the chunk isn't currently claimed.
        boolean personalPermission;
        // if stack is not a banner or is a banner without a pattern
        if (!TerritoryManager.IsBanner(stack) || !TerritoryManager.HasPattern(stack)) {
            personalPermission = true;
        }
        else {
            // stack is a banner with a pattern
            String thisPattern = TerritoryManager.BannerToString(stack);
            assert thisPattern != null; // this cannot be null because we can only reach this block if stack is a banner with a pattern;
            String headPattern = TerritoryManager.BannerToString(context.getPlayer().getInventory().getArmorStack(3));
            personalPermission = thisPattern.equals(headPattern) && (TerritoryManager.GetBannerInChunk(context.getBlockPos()) == null);
        }

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
            String pattern = TerritoryManager.BannerToString(stack);
            assert pattern != null; // pattern cannot be null because we just checked that stack has a pattern.
            if (EnchantmentHelper.hasBindingCurse(equippedStack) || TerritoryManager.IsBanner(equippedStack) ||
                    TerritoryManager.HasBetrayal(player.getEntityName(), pattern))
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
            String pattern = TerritoryManager.BannerToString((ItemStack)(Object)this);
            if (pattern == null) return; // this shouldn't be possible, but if the player somehow equips a banner without a pattern, we should check.
            TerritoryManager.AddBetrayal(player.getEntityName(), pattern);
            LightningEntity lightningEntity = EntityType.LIGHTNING_BOLT.create(player.getWorld());
            if (lightningEntity != null) {
                lightningEntity.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(player.getBlockPos()));
                lightningEntity.setChanneler((ServerPlayerEntity) player);
                player.getWorld().spawnEntity(lightningEntity);
            }

            String cmd = String.format("/tellraw @a [{\"text\":\"[Server] \"},{\"text\":\"%s\",\"color\":\"yellow\"}," +
                    "{\"text\":\" has abandoned their alliance and will NOT be coming back.\",\"color\":\"red\"}]", player.getEntityName());
            TerritoryManager.ExecuteCommand(cmd);
        }
    }
}

package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.Block;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.core.jmx.Server;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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

        PlayerEntity playerEntity = context.getPlayer();
        if (!(playerEntity instanceof ServerPlayerEntity) || (item instanceof BlockItem && !(item instanceof VerticallyAttachableBlockItem))) {
            return item.useOnBlock(context);
        }
        ServerPlayerEntity player = (ServerPlayerEntity)playerEntity;

        ItemStack stack = (ItemStack)(Object)this;
        boolean territorialPermission = TerritoryManager.HasPermission(context.getPlayer(), context.getBlockPos());
        boolean personalPermission = !TerritoryManager.isBanner(stack) ||
                (TerritoryManager.BannerToString(stack).equals(TerritoryManager.BannerToString(context.getPlayer().getInventory().getArmorStack(3))) &&
                        !TerritoryManager.HasBannerInChunk(context.getBlockPos()) ||
                        !TerritoryManager.HasPattern(stack));


        if (territorialPermission && personalPermission) {
            TerritoryManager.LOGGER.info("ItemStackMixin: pass");
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
            TerritoryManager.LOGGER.info("ItemStackMixin: fail");
            return ActionResult.FAIL;
        }
    }
}

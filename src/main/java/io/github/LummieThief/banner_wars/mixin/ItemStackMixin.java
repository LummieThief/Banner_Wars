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

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow public abstract Item getItem();

    @Redirect(method = "useOnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/Item;useOnBlock(Lnet/minecraft/item/ItemUsageContext;)Lnet/minecraft/util/ActionResult;"))
    private ActionResult overrideUseOnBlock(Item item, ItemUsageContext context) {
        PlayerEntity playerEntity = context.getPlayer();
        if (!(playerEntity instanceof ServerPlayerEntity) || item instanceof BlockItem && !(item instanceof VerticallyAttachableBlockItem)) {
            return item.useOnBlock(context);
        }
        ServerPlayerEntity player = (ServerPlayerEntity)playerEntity;
        BlockPos blockPos = context.getBlockPos();

        if (TerritoryManager.HasPermission(playerEntity, blockPos)) {
            return item.useOnBlock(context);
        }
        else {
            World world = context.getWorld();
            Hand hand = context.getHand();
            ScreenHandler screenHandler = player.currentScreenHandler;
            PlayerInventory inventory = player.getInventory();

            int slot = hand == Hand.OFF_HAND ? 45 : PlayerInventory.MAIN_SIZE + inventory.selectedSlot;
            player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(
                    screenHandler.syncId,
                    screenHandler.getRevision(),
                    slot,
                    player.getStackInHand(hand)));
            player.networkHandler.sendPacket(new BlockUpdateS2CPacket(blockPos, world.getBlockState(blockPos)));
            world.updateListeners(blockPos, world.getBlockState(blockPos), world.getBlockState(blockPos), Block.NOTIFY_LISTENERS);
            return ActionResult.FAIL;
        }
    }
}

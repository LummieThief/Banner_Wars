package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin {
    @Inject(method = "postPlacement", at = @At("HEAD"))
    protected void overridePostPlacement(BlockPos pos, World world, PlayerEntity player, ItemStack stack, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient)
            return;
        ChunkPos chunkPos = world.getChunk(pos).getPos();
        String existingBanner = TerritoryManager.GetBannerFromChunk(chunkPos.x, chunkPos.z);
        if (existingBanner == null && TerritoryManager.isBanner(stack)) {

            String banner = TerritoryManager.BannerToString(stack);
            TerritoryManager.LOGGER.info("adding chunk " + banner);
            TerritoryManager.AddChunk(banner, chunkPos.x, chunkPos.z);
        }
    }
}

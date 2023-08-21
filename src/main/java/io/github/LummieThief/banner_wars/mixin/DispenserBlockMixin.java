package io.github.LummieThief.banner_wars.mixin;

import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.item.FluidModificationItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPointerImpl;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(DispenserBlock.class)
public class DispenserBlockMixin {
    private static int shulkerMin, shulkerMax;

    @ModifyVariable(method = "dispense", at = @At(value = "STORE"), ordinal = 0)
    public int overrideDispense(int oldInt, ServerWorld world, BlockPos pos) {
        if (world.isClient)
            return oldInt;
        // if the dispenser is empty, proceed as normal
        if (oldInt < 0)
            return oldInt;
        // recalculate local variables
        BlockPointerImpl blockPointerImpl = new BlockPointerImpl(world, pos);
        DispenserBlockEntity dispenserBlockEntity = (DispenserBlockEntity)blockPointerImpl.getBlockEntity();
        // get the selected item
        ItemStack itemStack = dispenserBlockEntity.getStack(oldInt);

        Item item = itemStack.getItem();
        int stackId = Item.getRawId(item);
        // get all colors of shulker box
        boolean isShulker = stackId >= (shulkerMin == 0 ? shulkerMin = Item.getRawId(Items.SHULKER_BOX) : shulkerMin)
                && stackId <= (shulkerMax == 0 ? shulkerMax = Item.getRawId(Items.BLACK_SHULKER_BOX) : shulkerMax);

        // check if the selected item is one that needs to be restricted
        if (item instanceof FluidModificationItem || item.equals(Items.FLINT_AND_STEEL) || isShulker) {
            // get the position of the block the dispenser will fire into
            Direction dir = blockPointerImpl.getBlockState().get(DispenserBlock.FACING);
            BlockPos outputPos = pos.add(dir.getVector());
            // get the banner of the claim the dispenser will fire into
            String outputBanner = TerritoryManager.GetBannerFromChunk(outputPos.getX() >> 4, outputPos.getZ() >> 4);
            // if the block the dispenser will fire into is unclaimed, proceed as normal
            if (outputBanner == null) {
                return oldInt;
            }
            // the block the dispenser is firing into is claimed
            else {
                // get the claim the dispenser itself is in
                String blockBanner = TerritoryManager.GetBannerFromChunk(pos.getX() >> 4, pos.getZ() >> 4);
                // if it's the same claim as the block the dispenser is firing into, proceed as normal
                if (outputBanner.equals(blockBanner)) {
                    return oldInt;
                }
                // otherwise, the dispenser is firing into a different claim
                else {
                    return -1;
                }
            }
        }
        return oldInt;
    }

}

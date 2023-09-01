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
import net.minecraft.world.World;
import org.apache.logging.log4j.core.jmx.Server;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(DispenserBlock.class)
public class DispenserBlockMixin {
    private static int shulkerMin, shulkerMax;

    @ModifyVariable(method = "dispense", at = @At(value = "STORE"), ordinal = 0)
    public int overrideDispense(int oldInt, ServerWorld world, BlockPos pos) {
        if (world.isClient || !world.getRegistryKey().equals(World.OVERWORLD))
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

            // if the dispenser has permission, dispense as normal
            if (TerritoryManager.HasPermission(world, pos, outputPos)) {
                return oldInt;
            }
            else {
                return -1;
            }
        }
        return oldInt;
    }
}
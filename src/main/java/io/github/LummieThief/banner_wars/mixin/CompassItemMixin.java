package io.github.LummieThief.banner_wars.mixin;

import com.mojang.serialization.DataResult;
import io.github.LummieThief.banner_wars.IEyeOfEnderEntityMixin;
import io.github.LummieThief.banner_wars.TerritoryManager;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EyeOfEnderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.poi.PointOfInterestTypes;
import org.apache.logging.log4j.core.jmx.Server;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;
import java.util.Optional;

@Mixin(CompassItem.class)
public class CompassItemMixin extends Item {
    @Shadow
    private void writeNbt(RegistryKey<World> worldKey, BlockPos pos, NbtCompound nbt) {}


    public CompassItemMixin(Settings settings) {
        super(settings);
    }
/*    @Inject(method = "useOnBlock", at = @At("HEAD"))
    public void logOnUseBlock(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        TerritoryManager.LOGGER.info("useOnBlock");
    }*/

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (world.isClient() || !(user instanceof ServerPlayerEntity)){
            return TypedActionResult.pass(itemStack);
        }
        ServerWorld serverWorld = (ServerWorld)world;
        user.setCurrentHand(hand);
        user.getItemCooldownManager().set(this, 80);
        BlockPos blockPos = new BlockPos(26, -34,151); //TODO: Make this find a decaying banner
        if (blockPos != null) {
            ItemStack dyeStack = Items.RED_DYE.getDefaultStack();

            EyeOfEnderEntity eyeOfEnderEntity = new EyeOfEnderEntity(serverWorld, user.getX(), user.getBodyY(0.5), user.getZ());
            eyeOfEnderEntity.setItem(dyeStack);
            eyeOfEnderEntity.initTargetPos(blockPos);

            serverWorld.emitGameEvent(GameEvent.PROJECTILE_SHOOT, eyeOfEnderEntity.getPos(), GameEvent.Emitter.of(user));
            serverWorld.spawnEntity(eyeOfEnderEntity);

            ((IEyeOfEnderEntityMixin)eyeOfEnderEntity).markTracker();

            serverWorld.playSound((PlayerEntity)null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_ENDER_EYE_LAUNCH, SoundCategory.NEUTRAL, 0.5F, 0.4F / (serverWorld.getRandom().nextFloat() * 0.4F + 0.8F));

            user.swingHand(hand, true);

        }
        return TypedActionResult.success(itemStack);
    }
}

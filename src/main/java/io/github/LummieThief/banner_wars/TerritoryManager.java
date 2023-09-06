package io.github.LummieThief.banner_wars;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.AbstractBannerBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static net.minecraft.server.command.CommandManager.literal;

public class TerritoryManager implements ModInitializer {
    public static final String MOD_ID = "banner_wars";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static MinecraftServer server;
    public static ServerState state;
    public static ServerWorld overworld;
    public static ServerTickHandler serverTickHandler;

    private static final Map<BlockPos, BlockEntity> exileCache = new HashMap<>();
    private static long lastDecayedEpoch;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(srvr -> {
            server = srvr;
            state = ServerState.getServerState(server);
            overworld = server.getOverworld();

            serverTickHandler = new ServerTickHandler();
            ServerTickEvents.START_SERVER_TICK.register(serverTickHandler);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("kms")
                .executes(CommandManager::kmsCommand)));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("status")
                .executes(CommandManager::statusCommand)));

        UseBlockCallback.EVENT.register(new UseBlockHandler());
        UseItemCallback.EVENT.register(new UseItemHandler());
        ServerPlayConnectionEvents.DISCONNECT.register(new ServerPlayDisconnectHandler());

        PlayerBlockBreakEvents.BEFORE.register(new BreakBlockHandler());
        PlayerBlockBreakEvents.AFTER.register(new AfterBreakBlockHandler());
    }

    /**
     *  Determines if a given item is one of the 16 banner types, regardless of color.
     * @param stack the item to check.
     * @return whether the item is a banner.
     */
    public static boolean IsBanner(ItemStack stack) {
        return IsBanner(stack.getItem());
    }
    public static boolean IsBanner(Item item) {
        if (item == null)
            return false;
        int id = Item.getRawId(item);
        // 1087 is id of white banner
        // 1102 is id of black banner
        return id >= 1087 && id <= 1102;
    }

    @Nullable
    public static String BannerToString(ItemStack stack) {
        if (stack == null || !IsBanner(stack))
            return null;
        String s = String.valueOf(Item.getRawId(stack.getItem()));
        NbtList patternList = BannerBlockEntity.getPatternListNbt(stack);
        if (patternList != null)
            s += patternList.asString();
        return s;
    }

    // Encodes a blockPos as a single long, with the first 26 bits being the X value, the next 9 bits being the Y value,
    // the next 26 bits being the Z value, and the last 3 bits being the sign of each of X, Y, and Z
    public static Long EncodeBlockPosition(@NotNull BlockPos blockPos) {
        int bannerX = blockPos.getX();
        int bannerY = blockPos.getY();
        int bannerZ = blockPos.getZ();
        long code = 0;
        code |= Integer.toUnsignedLong(Math.abs(bannerX)) << 38;
        code |= Integer.toUnsignedLong(Math.abs(bannerY)) << 29;
        code |= Integer.toUnsignedLong(Math.abs(bannerZ)) << 3;
        if (bannerX < 0)
            code |= 0b100;
        if (bannerY < 0)
            code |= 0b010;
        if (bannerZ < 0)
            code |= 0b001;
        return code;
    }

    // Encodes the chunk that contains the specified block into as a single long by converting it into a block and
    // encoding the block position. If the block was below y = 0, then the Y value of the BlockPos will be -1.
    // Otherwise, it will be 1.
    public static Long EncodeChunkPosition(@NotNull BlockPos blockInChunk) {
        int y = blockInChunk.getY() < 0 ? -1 : 1;
        BlockPos chunkPos = new BlockPos(blockInChunk.getX() >> 4, y, blockInChunk.getZ() >> 4);
        return EncodeBlockPosition(chunkPos);
    }

    // Decodes an encoded block position. If a chunk position is desired, the user is expected to convert the BlockPos
    // into a ChunkPos for the sake of not requiring output parameters.
    public static BlockPos DecodePosition(long code) {
        long bannerX = code >>> 38;
        long bannerY = (code << 26) >>> 55;
        long bannerZ = (code << 35) >>> 38;
        if (((code >> 2) & 1) == 1) bannerX *= -1;
        if (((code >> 1) & 1) == 1) bannerY *= -1;
        if ((code & 1) == 1) bannerZ *= -1;
        return new BlockPos((int)bannerX, (int)bannerY, (int)bannerZ);
    }

    // Takes an encoded block position and re-encodes it as the chunk that contains that block
    public static long ConvertBlockEncodingToChunkEncoding(long bannerPos) {
        return EncodeChunkPosition(DecodePosition(bannerPos));
    }
    @Nullable
    public static String GetBannerInChunk(BlockPos pos) {
        if (state == null || pos == null)
            return null;
        ChunkData data = state.chunkMap.get(EncodeChunkPosition(pos));
        if (data != null) {
            return data.bannerPattern();
        }
        return null;
    }
    @Nullable
    public static BlockPos GetBannerPosInChunk(BlockPos pos) {
        if (state == null || pos == null)
            return null;
        ChunkData data = state.chunkMap.get(EncodeChunkPosition(pos));
        if (data != null) {
            return DecodePosition(data.bannerPos());
        }
        return null;
    }

    public static void AddChunk(@NotNull String banner, @NotNull BlockPos bannerPos) {
        if (state == null)
            return;
        long chunkCode = EncodeChunkPosition(bannerPos);
        long blockCode = EncodeBlockPosition(bannerPos);
        ChunkData data = new ChunkData(banner, blockCode, serverTickHandler.getEpoch());
        state.AddChunk(chunkCode, data);
    }

    public static boolean HasPermission(@NotNull World world, @NotNull PlayerEntity player, @NotNull BlockPos pos) {
        String banner = TerritoryManager.GetBannerInChunk(pos);
        if (banner == null ) {
            // chunk is unclaimed, so player has permission
            return true;
        }

        // chunk is claimed, so player only has permission if their banner matches the banner in the chunk
        ItemStack headStack = player.getEquippedStack(EquipmentSlot.HEAD);
        if (headStack != null) {
            String headBanner = TerritoryManager.BannerToString(headStack);
            if (banner.equals(headBanner)) {
                return true;
            }
        }
        // player's banner did not match
        return InDecay(world, pos, banner);
    }

    public static boolean HasPermission(@NotNull World world, @NotNull BlockPos source, @NotNull BlockPos dest) {
        String destBanner = TerritoryManager.GetBannerInChunk(dest);
        if (destBanner == null) {
            return true;
        }
        String sourceBanner = TerritoryManager.GetBannerInChunk(source);
        return destBanner.equals(sourceBanner) || InDecay(world, dest, destBanner);
    }
    public static boolean InDecay(@NotNull World world, BlockPos pos, @NotNull String banner) {
        if (world.isClient || !world.getRegistryKey().equals(World.OVERWORLD) || state == null) {
            return true;
        }
        DecayData data = state.decayMap.get(banner);
        // if there's no entry in the decay map, this chunk isn't in decay
        if (data == null) {
            return false;
        }
        // otherwise, this chunk is in decay if the epoch is greater or equal to than the last decay epoch
        // and the position of the block queried is not the position of the banner that claims the chunk.
        return data.epoch() >= lastDecayedEpoch && (pos == null || !pos.equals(GetBannerPosInChunk(pos)));
    }

    public static boolean HasPattern(@NotNull ItemStack bannerItem) {
        NbtCompound comp = bannerItem.getOrCreateNbt();
        NbtCompound blockEntityTag = comp.getCompound("BlockEntityTag");
        if (blockEntityTag == null)
            return false;
        NbtList list = blockEntityTag.getList("Patterns", NbtElement.COMPOUND_TYPE);
        return list != null && list.size() > 0;
    }

    @Nullable
    public static BlockHitResult GetPlayerHitResult(@NotNull PlayerEntity player, boolean includeFluid) {
        float maxDistance = 5f;
        HitResult hit = player.raycast(maxDistance, 0, includeFluid);
        if (hit.getType() == HitResult.Type.BLOCK) {
            return (BlockHitResult) hit;
        }
        else {
            return null;
        }
    }

    public static void RemoveChunk(BlockPos pos) {
        if (state == null || pos == null)
            return;
        state.RemoveChunk(EncodeChunkPosition(pos));
    }

    public static void FlickerFadingBanners(long lastLivingEpoch) {
        if (state == null)
            return;
        Queue<BlockPos> toRemove = new LinkedList<>();
        for (ChunkData chunk : state.chunkMap.values()) {
            if (chunk.epoch() < lastLivingEpoch) {
                BlockPos bannerPos = DecodePosition(chunk.bannerPos());
                toRemove.add(bannerPos);
                BlockState bannerState = overworld.getBlockState(bannerPos);
                if (bannerState.getBlock() instanceof AbstractBannerBlock) {
                    BlockEntity bannerEntity = new BannerBlockEntity(bannerPos, bannerState);
                    ScheduleETB(bannerPos, bannerEntity);
                }
            }
        }
        for(BlockPos pos : toRemove) {
            RemoveChunk(pos);
        }
    }

    public static void ETBBanners() {
        boolean c = false;
        for (BlockPos pos : exileCache.keySet()) {
            BlockEntity e = exileCache.get(pos);
            overworld.setBlockState(pos, e.getCachedState());
            overworld.addBlockEntity(e);
            overworld.updateListeners(pos, e.getCachedState(), e.getCachedState(), Block.NOTIFY_LISTENERS);
            c = true;
        }
        if (c)
            exileCache.clear();
    }

    public static void ScheduleETB(@NotNull BlockPos pos, @NotNull BlockEntity blockEntity) {
        exileCache.put(pos, blockEntity);
        overworld.removeBlock(pos, false);
    }

    public static boolean DecayBanner(@NotNull String banner, @NotNull String name) {
        if (state == null)
            return false;
        if (!state.decayMap.containsKey(banner)) {
            state.decayMap.put(banner, new DecayData(serverTickHandler.getEpoch(), name));
            return true;
        }
        return false;
    }

    public static void UnDecayBanners(long lastDecayedEpoch, long lastDecayProtectionEpoch) {
        if (state == null)
            return;
        Queue<String> toRemove = new LinkedList<>();
        TerritoryManager.lastDecayedEpoch = lastDecayedEpoch;
        for (String banner : state.decayMap.keySet()) {
            DecayData data = state.decayMap.get(banner);
            if (data.epoch() < lastDecayProtectionEpoch) {
                toRemove.add(banner);
                String cmd = String.format("/tellraw @a [{\"text\":\"[Server] \"},{\"text\":\"%s\",\"color\":\"yellow\"}," +
                        "{\"text\":\"'s territory has recovered.\",\"color\":\"green\"}]", data.name());
                CommandManager.ExecuteCommand(cmd);
            }
            else if (data.epoch() == lastDecayedEpoch - 1) {
                String cmd = String.format("/tellraw @a [{\"text\":\"[Server] \"},{\"text\":\"%s\",\"color\":\"yellow\"}," +
                        "{\"text\":\"'s territory can no longer be attacked and now has 15 minutes to recover.\",\"color\":\"aqua\"}]", data.name());
                CommandManager.ExecuteCommand(cmd);
            }
        }

        for (String s : toRemove) {
            state.decayMap.remove(s);
        }
    }

    public static void AddBetrayal(String player, String pattern) {
        if (state == null)
            return;
        Set<String> patterns = state.betrayalMap.getOrDefault(player, new HashSet<>());
        patterns.add(pattern);
        state.betrayalMap.put(player, patterns);
        state.markDirty();
    }
    public static boolean HasBetrayal(String player, String pattern) {
        if (state == null)
            return false;
        Set<String> patterns = state.betrayalMap.get(player);
        if (patterns == null)
            return false;
        return patterns.contains(pattern);
    }

    public static void CreateFireworkEffect(@NotNull World world, double x, double y, double z, List<Pair<RegistryEntry<BannerPattern>, DyeColor>> patternList) {
        if (patternList == null)
            return;
        DyeColor mainColor = DyeColor.WHITE;
        if (patternList.size() > 0)
            mainColor = patternList.get(0).getSecond();
        Set<DyeColor> uniqueColors = new HashSet<>();
        for (Pair<RegistryEntry<BannerPattern>, DyeColor> registryEntryDyeColorPair : patternList) {
            uniqueColors.add(registryEntryDyeColorPair.getSecond());
        }
        int m = 2;
        int[] fireworkColors = new int[uniqueColors.size() + m];
        for (int i = 0; i < m; i++) {
            fireworkColors[i] = mainColor.getFireworkColor();
        }
        for (DyeColor color : uniqueColors) {
            fireworkColors[m] = color.getFireworkColor();
            m++;
        }

        ItemStack fireworkStack = Items.FIREWORK_ROCKET.getDefaultStack();
        NbtCompound nbt = new NbtCompound();
        NbtCompound fireworkNbt = new NbtCompound();
        NbtList explosions = new NbtList();
        NbtCompound star = new NbtCompound();
        star.putIntArray("Colors", fireworkColors);
        star.putByte("Type", (byte) 0);
        explosions.add(star);
        fireworkNbt.put("Explosions", explosions);
        fireworkNbt.putByte("Flight", (byte) 0);
        nbt.put("Fireworks", fireworkNbt);
        fireworkStack.setNbt(nbt);
        FireworkRocketEntity firework = new FireworkRocketEntity(world, x, y, z, fireworkStack);
        world.spawnEntity(firework);
        ((IFireworkRocketEntityMixin)firework).triggerExplosion();
    }
}

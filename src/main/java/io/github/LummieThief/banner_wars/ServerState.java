package io.github.LummieThief.banner_wars;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.*;

// Saves each territory as an object called "territory#", with a field called "banner" and an arbitrary number of
// fields called "pos#" and "epoch#" for each chunk in the territory
public class ServerState extends PersistentState {
    private static boolean RESET = true;
    public Map<Long, ChunkData> chunkMap = new HashMap<>();
    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        if (RESET) {
            nbt = new NbtCompound();
        }
        else {
            int c = 0;
            for (long chunkCode : chunkMap.keySet()) {
                NbtCompound chunk = new NbtCompound();

                ChunkData chunkData = chunkMap.get(chunkCode);
                chunk.putString("banner", chunkData.bannerPattern());
                chunk.putLong("pos", chunkData.bannerPos());
                chunk.putInt("epoch", chunkData.epoch());

                nbt.put("chunk" + c, chunk);
                c++;
            }
        }
        return nbt;
    }

    public static ServerState createFromNbt(NbtCompound tag) {
        ServerState state = new ServerState();
        if (RESET) {
            state.markDirty();
        }
        else {
            state.chunkMap = new HashMap<>();
            for (int c = 0; tag.contains("chunk" + c); c++) {
                NbtCompound chunk = tag.getCompound("chunk" + c);
                ChunkData chunkData = new ChunkData(chunk.getString("bannerPattern"), chunk.getLong("bannerPos"), chunk.getInt("epoch"));
                state.chunkMap.put(TerritoryManager.ConvertBlockEncodingToChunkEncoding(chunkData.bannerPos()), chunkData);
            }
        }
        return state;
    }

    public static ServerState getServerState(MinecraftServer server) {
        // First we get the persistentStateManager for the OVERWORLD
        PersistentStateManager persistentStateManager = server
                .getWorld(World.OVERWORLD).getPersistentStateManager();

        // Calling this reads the file from the disk if it exists, or creates a new one and saves it to the disk
        // You need to use a unique string as the key. You should already have a MODID variable defined by you somewhere in your code. Use that.
        ServerState state = persistentStateManager.getOrCreate(
                ServerState::createFromNbt,
                ServerState::new,
                TerritoryManager.MOD_ID);

        return state;

    }
}

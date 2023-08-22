package io.github.LummieThief.banner_wars;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.*;

// Saves each territory as an object called "territory#", with a field called "banner" and an arbitrary number of
// fields called "pos#" for each chunk in the territory
public class ServerState extends PersistentState {
    private static boolean RESET = true;

    public Map<String, Set<Long>> bannerToPositionsMap = new HashMap<>(); // maps from a banner to a set of positions where that banner occurs in the world
    public Map<Long, String> chunkToBannerMap = new HashMap<>(); // maps from a chunk to the banner that claims it
    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        if (RESET) {
            nbt = new NbtCompound();
        }
        else {
            // loops over every banner pattern in the map
            int t = 0;
            for (String banner : bannerToPositionsMap.keySet()) {
                // gets the list of banner positions in the territory
                Set<Long> positions = bannerToPositionsMap.get(banner);
                // copies the banner pattern of the territory and will add to it
                NbtCompound territory = new NbtCompound();
                territory.putString("banner", banner);
                // loops over every chunk in the territory
                int c = 0;
                for (long pos : positions) {
                    territory.putLong("pos" + c, pos);
                    c++;
                }
                nbt.put("territory" + t, territory);
                t++;
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
            int t = 0;
            while (tag.contains("territory" + t)) {
                NbtCompound territory = tag.getCompound("territory" + t);

                String banner = territory.getString("banner");

                Set<Long> positions = new HashSet<>();
                int c = 0;
                while (territory.contains("pos" + c)) {
                    long pos = territory.getLong("pos" + c);
                    positions.add(pos);
                    c++;
                }
                state.bannerToPositionsMap.put(banner, positions);
                t++;
            }

            for (String banner : state.bannerToPositionsMap.keySet()) {
                for (long pos : state.bannerToPositionsMap.get(banner)) {
                    state.chunkToBannerMap.put(TerritoryManager.ConvertBlockEncodingToChunkEncoding(pos), banner);
                    //state.chunkToBannerMap.put(pos, banner);
                }
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

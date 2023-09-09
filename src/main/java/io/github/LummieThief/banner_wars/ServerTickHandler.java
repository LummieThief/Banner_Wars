package io.github.LummieThief.banner_wars;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

public class ServerTickHandler implements ServerTickEvents.StartTick {
    public static final int TICKS_PER_EPOCH = 600; // 600 = 2 ticks/minute
    public static final int CLAIM_LIFETIME = 2880; // 2880 = 24 hours * 60 minutes * 2 ticks/minute
    public static final int DECAY_TIME = 30; // 30 = 15 minutes * 2 ticks/minute
    private long time;

    public ServerTickHandler() {
    }

    @Override
    public void onStartTick(MinecraftServer server) {
        World world = server.getWorld(World.OVERWORLD);
        assert world != null;
        time = world.getTime();
        if (time % TICKS_PER_EPOCH == 0) {
            TerritoryManager.FlickerFadingBanners(getEpoch() - CLAIM_LIFETIME);
            TerritoryManager.UnDecayBanners(getEpoch() - DECAY_TIME, getEpoch() - DECAY_TIME * 2);
        }
        else
            TerritoryManager.ETBBanners();
    }
    public int getEpoch() {
        return (int)(time / TICKS_PER_EPOCH);
    }
}

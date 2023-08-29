package io.github.LummieThief.banner_wars;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

public class ServerTickHandler implements ServerTickEvents.StartTick {
    private static final int TICKS_PER_EPOCH = 100;
    private static final int CLAIM_LIFETIME = 2;
    private ServerWorld world;
    private long time;

    public ServerTickHandler(ServerWorld world) {
        this.world = world;
    }
    @Override
    public void onStartTick(MinecraftServer server) {
        time = world.getTime();
        if (time % TICKS_PER_EPOCH == 0) {
            TerritoryManager.LOGGER.info("updating banners: " + (time / TICKS_PER_EPOCH));
            TerritoryManager.FlickerDecayingBanners(getEpoch() - CLAIM_LIFETIME);

        }
        else
            TerritoryManager.ETBBanners();
    }
    public int getEpoch() {
        return (int)(time / TICKS_PER_EPOCH);
    }
}

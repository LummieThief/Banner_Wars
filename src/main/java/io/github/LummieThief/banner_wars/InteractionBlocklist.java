package io.github.LummieThief.banner_wars;

import net.minecraft.block.*;

import java.util.HashSet;
import java.util.Set;

public class InteractionBlocklist {

    private final Set<Block> blocks;
    private final Set<Class<?>> classes;

    public InteractionBlocklist() {
        classes = new HashSet<>();
        classes.add(DoorBlock.class);
        classes.add(FenceGateBlock.class);
        classes.add(ButtonBlock.class);
        classes.add(SignBlock.class);
        classes.add(HangingSignBlock.class);
        classes.add(TrapdoorBlock.class);
        classes.add(BedBlock.class);
        classes.add(AnvilBlock.class);
        classes.add(CakeBlock.class);
        classes.add(CandleCakeBlock.class);

        blocks = new HashSet<>();
        blocks.add(Blocks.DRAGON_EGG);
        blocks.add(Blocks.REPEATER);
        blocks.add(Blocks.COMPARATOR);
        blocks.add(Blocks.NOTE_BLOCK);
        blocks.add(Blocks.DAYLIGHT_DETECTOR);
        blocks.add(Blocks.LEVER);
        blocks.add(Blocks.BEACON);
        blocks.add(Blocks.RESPAWN_ANCHOR);
        blocks.add(Blocks.SWEET_BERRY_BUSH);
        blocks.add(Blocks.CAVE_VINES);
        blocks.add(Blocks.CAVE_VINES_PLANT);
    }

    public boolean isBlacklisted(Block block) {
        return classes.contains(block.getClass()) || blocks.contains(block);
    }
}

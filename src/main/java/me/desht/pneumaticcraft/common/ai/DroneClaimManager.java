package me.desht.pneumaticcraft.common.ai;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Keeps track of the positions the drones are working on, and allows the drones to pick a coordinate in a smart way.
 */
public class DroneClaimManager {

    private static final Map<ResourceLocation, DroneClaimManager> claimManagers = new HashMap<>();
    private final Map<BlockPos, Integer> currentPositions = new HashMap<>();
    private static final int TIMEOUT = DroneAIManager.TICK_RATE + 1;

    public static DroneClaimManager getInstance(World world) {
        return claimManagers.computeIfAbsent(world.dimension().location(), k -> new DroneClaimManager());
    }

    /**
     * unclaim any positions that have been claimed too long. this prevents positions being claimed forever by died drones.
     */
    public void update() {
        Iterator<Map.Entry<BlockPos, Integer>> iterator = currentPositions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = iterator.next();
            if (entry.getValue() < TIMEOUT) {
                entry.setValue(entry.getValue() + 1);
            } else {
                iterator.remove();
            }
        }
    }

    boolean isClaimed(BlockPos pos) {
        return currentPositions.containsKey(pos);
    }

    void claim(BlockPos pos) {
        currentPositions.put(pos, 0);
    }
}

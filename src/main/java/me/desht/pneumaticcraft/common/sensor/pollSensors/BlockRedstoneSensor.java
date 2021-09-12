package me.desht.pneumaticcraft.common.sensor.pollSensors;

import com.google.common.collect.ImmutableSet;
import me.desht.pneumaticcraft.api.item.EnumUpgrade;
import me.desht.pneumaticcraft.api.universal_sensor.IBlockAndCoordinatePollSensor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Set;

public class BlockRedstoneSensor implements IBlockAndCoordinatePollSensor {

    @Override
    public String getSensorPath() {
        return "Block/Redstone";
    }

    @Override
    public Set<EnumUpgrade> getRequiredUpgrades() {
        return ImmutableSet.of(EnumUpgrade.BLOCK_TRACKER);
    }

    @Override
    public int getPollFrequency() {
        return 2;
    }

    @Override
    public boolean needsTextBox() {
        return false;
    }

    @Override
    public int getRedstoneValue(World world, BlockPos pos, int sensorRange, String textBoxText, Set<BlockPos> positions) {
        int redstonePower = 0;
        for (BlockPos pos1 : positions) {
            redstonePower = Math.max(redstonePower, world.getBestNeighborSignal(pos1));
            if (redstonePower == 15) return 15;
        }
        return redstonePower;
    }
}
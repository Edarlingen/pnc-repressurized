package me.desht.pneumaticcraft.common.sensor.pollSensors;

import com.google.common.collect.ImmutableSet;
import me.desht.pneumaticcraft.api.item.EnumUpgrade;
import me.desht.pneumaticcraft.api.universal_sensor.IPollSensorSetting;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import java.util.Set;

public class WorldDayLightSensor implements IPollSensorSetting {

    @Override
    public String getSensorPath() {
        return "World/Daylight";
    }

    @Override
    public Set<EnumUpgrade> getRequiredUpgrades() {
        return ImmutableSet.of(EnumUpgrade.DISPENSER);
    }

    @Override
    public boolean needsTextBox() {
        return false;
    }

    @Override
    public int getPollFrequency(TileEntity te) {
        return 40;
    }

    @Override
    public int getRedstoneValue(World world, BlockPos pos, int sensorRange, String textBoxText) {
        return updatePower(world, pos);
    }

    private int updatePower(World worldIn, BlockPos pos) {
        if (worldIn.dimensionType().hasSkyLight()) {
            int i = worldIn.getBrightness(LightType.SKY, pos) - worldIn.getSkyDarken();
            float f = worldIn.getSunAngle(1.0F);
            float f1 = f < (float) Math.PI ? 0.0F : (float) Math.PI * 2F;
            f = f + (f1 - f) * 0.2F;
            i = Math.round(i * MathHelper.cos(f));
            i = MathHelper.clamp(i, 0, 15);
            return i;
        }
        return 0;
    }
}

package me.desht.pneumaticcraft.common.sensor.pollSensors;

import com.google.common.collect.ImmutableSet;
import me.desht.pneumaticcraft.api.item.IItemRegistry;
import me.desht.pneumaticcraft.api.universal_sensor.IPollSensorSetting;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WorldWeatherForecaster implements IPollSensorSetting {

    @Override
    public String getSensorPath() {
        return "World/Weather Forecast";
    }

    @Override
    public Set<Item> getRequiredUpgrades() {
        return ImmutableSet.of(IItemRegistry.EnumUpgrade.DISPENSER.getItem());
    }

    @Override
    public boolean needsTextBox() {
        return false;
    }

    @Override
    public List<String> getDescription() {
        List<String> text = new ArrayList<>();
        text.add(TextFormatting.BLACK + "Emits a redstone signal of which the strength gets higher how closer the rain gets. The strenght increases by one for every minute.");
        text.add(TextFormatting.RED + "strength = 15 - time till rain (min)");
        text.add(TextFormatting.GREEN + "Example: If it will rain in 10 minutes, the strength is 5.");
        return text;
    }

    @Override
    public int getPollFrequency(TileEntity te) {
        return 40;
    }

    @Override
    public int getRedstoneValue(World world, BlockPos pos, int sensorRange, String textBoxText) {
        return Math.max(0, 15 - world.getWorldInfo().getRainTime() / 1200);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawAdditionalInfo(FontRenderer fontRenderer) {
    }
}

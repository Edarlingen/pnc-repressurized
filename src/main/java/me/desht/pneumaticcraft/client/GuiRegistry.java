package me.desht.pneumaticcraft.client;

import me.desht.pneumaticcraft.api.client.IClientRegistry;
import me.desht.pneumaticcraft.api.client.IGuiAnimatedStat;
import me.desht.pneumaticcraft.api.client.assembly_machine.IAssemblyRenderOverriding;
import me.desht.pneumaticcraft.client.gui.widget.GuiAnimatedStat;
import me.desht.pneumaticcraft.client.util.GuiUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nonnull;
import java.util.HashMap;

public class GuiRegistry implements IClientRegistry {

    private static final GuiRegistry INSTANCE = new GuiRegistry();
    public static final HashMap<ResourceLocation, IAssemblyRenderOverriding> renderOverrides = new HashMap<>();

    private GuiRegistry() {}

    public static GuiRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public IGuiAnimatedStat getAnimatedStat(Screen gui, int backgroundColor) {
        return new GuiAnimatedStat(gui, backgroundColor);
    }

    @Override
    public IGuiAnimatedStat getAnimatedStat(Screen gui, ItemStack iconStack, int backgroundColor) {
        return new GuiAnimatedStat(gui, backgroundColor, iconStack);
    }

    @Override
    public IGuiAnimatedStat getAnimatedStat(Screen gui, String iconTexture, int backgroundColor) {
        return new GuiAnimatedStat(gui, backgroundColor, iconTexture);
    }

    @Override
    public void drawPressureGauge(FontRenderer fontRenderer, float minPressure, float maxPressure, float dangerPressure, float minWorkingPressure, float currentPressure, int xPos, int yPos) {
        GuiUtils.drawPressureGauge(fontRenderer, minPressure, maxPressure, dangerPressure, minWorkingPressure, currentPressure, xPos, yPos, 0x00000000);
    }

    @Override
    public void registerRenderOverride(@Nonnull IForgeRegistryEntry<?> entry, @Nonnull IAssemblyRenderOverriding renderOverride) {
        renderOverrides.put(entry.getRegistryName(), renderOverride);
    }
}

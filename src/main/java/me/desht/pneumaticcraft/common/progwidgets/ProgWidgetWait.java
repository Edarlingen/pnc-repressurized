package me.desht.pneumaticcraft.common.progwidgets;

import com.google.common.collect.ImmutableList;
import me.desht.pneumaticcraft.api.drone.ProgWidgetType;
import me.desht.pneumaticcraft.common.ai.IDroneBase;
import me.desht.pneumaticcraft.common.core.ModProgWidgets;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.DyeColor;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.List;

public class ProgWidgetWait extends ProgWidget {

    public ProgWidgetWait() {
        super(ModProgWidgets.WAIT.get());
    }

    @Override
    public boolean hasStepInput() {
        return true;
    }

    @Override
    public ProgWidgetType<?> returnType() {
        return null;
    }

    @Override
    public List<ProgWidgetType<?>> getParameters() {
        return ImmutableList.of(ModProgWidgets.TEXT.get());
    }

    @Override
    protected boolean hasBlacklist() {
        return false;
    }

    @Override
    public ResourceLocation getTexture() {
        return Textures.PROG_WIDGET_WAIT;
    }

    @Override
    public Goal getWidgetAI(IDroneBase drone, IProgWidget widget) {
        return widget instanceof ProgWidgetWait ? widget.getConnectedParameters()[0] != null ? new DroneAIWait((ProgWidgetText) widget.getConnectedParameters()[0]) : null : null;
    }

    private static class DroneAIWait extends Goal {

        private final int maxTicks;
        private int ticks;

        private DroneAIWait(ProgWidgetText widget) {
            String time = widget.string;
            int multiplier = 1;
            if (time.endsWith("s") || time.endsWith("S")) {
                multiplier = 20;
                time = time.substring(0, time.length() - 1);
            } else if (time.endsWith("m") || time.endsWith("M")) {
                multiplier = 1200;
                time = time.substring(0, time.length() - 1);
            }
            maxTicks = NumberUtils.toInt(time) * multiplier;
        }

        @Override
        public boolean canUse() {
            return ticks < maxTicks;
        }

        @Override
        public boolean canContinueToUse() {
            ticks++;
            return canUse();
        }

    }

    @Override
    public WidgetDifficulty getDifficulty() {
        return WidgetDifficulty.EASY;
    }

    @Override
    public DyeColor getColor() {
        return DyeColor.WHITE;
    }
}

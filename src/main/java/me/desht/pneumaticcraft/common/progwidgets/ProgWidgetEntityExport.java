package me.desht.pneumaticcraft.common.progwidgets;

import me.desht.pneumaticcraft.common.ai.DroneAIBlockInteraction;
import me.desht.pneumaticcraft.common.ai.IDroneBase;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.DyeColor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public class ProgWidgetEntityExport extends ProgWidgetAreaItemBase {

    @Override
    public String getWidgetString() {
        return "entityExport";
    }

    @Override
    public ResourceLocation getTexture() {
        return Textures.PROG_WIDGET_ENTITY_EX;
    }

    @Override
    public Class<? extends IProgWidget>[] getParameters() {
        return new Class[]{ProgWidgetArea.class, ProgWidgetString.class};
    }

    @Override
    public DyeColor getColor() {
        return DyeColor.ORANGE;
    }

    @Override
    public Goal getWidgetAI(IDroneBase drone, IProgWidget widget) {
        return new DroneAIBlockInteraction<ProgWidgetEntityExport>(drone, (ProgWidgetEntityExport) widget) {

            @Override
            public boolean shouldExecute() {
                if (drone.getCarryingEntities().isEmpty()) return false;
                for (Entity e : drone.getCarryingEntities()) {
                    if (!progWidget.isEntityValid(e)) return false;
                }
                return super.shouldExecute();
            }

            @Override
            protected boolean isValidPosition(BlockPos pos) {
                return true;
            }

            @Override
            protected boolean moveIntoBlock() {
                return true;
            }

            @Override
            protected boolean doBlockInteraction(BlockPos pos, double distToBlock) {
                drone.setCarryingEntity(null);
                return false;
            }

        };
    }
}

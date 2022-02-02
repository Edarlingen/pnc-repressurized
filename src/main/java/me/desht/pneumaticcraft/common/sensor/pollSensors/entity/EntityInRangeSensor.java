/*
 * This file is part of pnc-repressurized.
 *
 *     pnc-repressurized is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     pnc-repressurized is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with pnc-repressurized.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.desht.pneumaticcraft.common.sensor.pollSensors.entity;

import me.desht.pneumaticcraft.common.util.EntityFilter;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.ITextComponent;

import java.util.List;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class EntityInRangeSensor extends EntityPollSensor {
    @Override
    public String getSensorPath() {
        return "Within Range";
    }

    @Override
    public boolean needsTextBox() {
        return true;
    }

    @Override
    public boolean isEntityFilter() {
        return true;
    }

    @Override
    public int getRedstoneValue(List<Entity> entities, String textboxText) {
        EntityFilter filter = EntityFilter.fromString(textboxText);
        if (filter == null) return 0;

        int entitiesFound = textboxText.isEmpty() ?
                entities.size() :
                (int) entities.stream().filter(filter).count();
        return Math.min(15, entitiesFound);
    }

    @Override
    public Class<? extends Entity> getEntityTracked() {
        return Entity.class;
    }

    @Override
    public void getAdditionalInfo(List<ITextComponent> info) {
        info.add(xlate("pneumaticcraft.gui.entityFilter"));
    }
}

package me.desht.pneumaticcraft.common.block.tubes;

import me.desht.pneumaticcraft.common.item.ItemTubeModule;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;

import java.util.List;

public abstract class TubeModuleRedstoneEmitting extends TubeModule {
    protected int redstone;

    TubeModuleRedstoneEmitting(ItemTubeModule item) {
        super(item);
    }

    /**
     * @param level signal level
     * @return true if the redstone has changed compared to last time.
     */
    boolean setRedstone(int level) {
        level = MathHelper.clamp(level, 0, 15);
        if (redstone != level) {
            redstone = level;
            updateNeighbors();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getRedstoneLevel() {
        return redstone;
    }

    @Override
    public void addInfo(List<ITextComponent> curInfo) {
        super.addInfo(curInfo);
        curInfo.add(PneumaticCraftUtils.xlate("pneumaticcraft.waila.redstoneModule.emitting", redstone));
    }

    @Override
    public CompoundNBT writeToNBT(CompoundNBT tag) {
        super.writeToNBT(tag);
        tag.putInt("redstone", redstone);
        return tag;
    }

    @Override
    public void readFromNBT(CompoundNBT tag) {
        super.readFromNBT(tag);
        redstone = tag.getInt("redstone");
    }

    @Override
    public void update() {
        if (upgraded && !advancedConfig) {
            if (higherBound < lowerBound) {
                if (higherBound != lowerBound - 0.1F) {
                    higherBound = lowerBound - 0.1F;
                    if (!pressureTube.getLevel().isClientSide) sendDescriptionPacket();
                }
            } else {
                if (higherBound != lowerBound + 0.1F) {
                    higherBound = lowerBound + 0.1F;
                    if (!pressureTube.getLevel().isClientSide) sendDescriptionPacket();
                }
            }
        }
    }
}

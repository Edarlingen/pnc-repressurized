package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.client.render.area.AreaRenderManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class RangeManager {
    private final TileEntity te;
    private final int renderColour;
    private int range = 0;
    private boolean showRange = false;
    private AxisAlignedBB extents;
    private Supplier<AxisAlignedBB> extentsGenerator;
    private Set<BlockPos> frame;  // for rendering

    public RangeManager(TileEntity te, int renderColour) {
        this.te = te;
        this.renderColour = renderColour;
        this.extentsGenerator = () -> new AxisAlignedBB(te.getBlockPos(), te.getBlockPos()).inflate(range);
        this.setRange(1);
    }

    public RangeManager withCustomExtents(Supplier<AxisAlignedBB> generator) {
        this.extentsGenerator = generator;
        return this;
    }

    public int getRange() {
        return range;
    }

    public void setRange(int newRange) {
        if (newRange != range) {
            range = newRange;
            this.extents = extentsGenerator.get();
            this.frame = te.getLevel() != null && te.getLevel().isClientSide() ? getFrame(extents) : Collections.emptySet();
            if (shouldShowRange() && te.getLevel() != null && te.getLevel().isClientSide()) {
                toggleShowRange();
                toggleShowRange();
            }
        }
    }

    public void toggleShowRange() {
        showRange = !showRange;
        if (te.getLevel() != null && te.getLevel().isClientSide()) {
            if (showRange) {
                AreaRenderManager.getInstance().showArea(frame, renderColour, te, false);
            } else {
                AreaRenderManager.getInstance().removeHandlers(te);
            }
        }
    }

    public boolean shouldShowRange() {
        return showRange;
    }

    public AxisAlignedBB getExtents() {
        return extents;
    }

    public static Set<BlockPos> getFrame(AxisAlignedBB extents) {
        Set<BlockPos> res = new HashSet<>();
        int minX = (int) extents.minX;
        int minY = (int) extents.minY;
        int minZ = (int) extents.minZ;
        int maxX = (int) extents.maxX;
        int maxY = (int) extents.maxY;
        int maxZ = (int) extents.maxZ;
        for (int x = minX; x <= maxX; x++) {
            for (int y = Math.max(0, minY); y <= maxY && y < 256; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ) {
                        res.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return res;
    }
}

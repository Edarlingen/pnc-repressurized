package me.desht.pneumaticcraft.common.block.tubes;

import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.client.render.area.AreaRenderManager;
import me.desht.pneumaticcraft.common.entity.semiblock.EntitySemiblockBase;
import me.desht.pneumaticcraft.common.item.ItemTubeModule;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketUpdatePressureBlock;
import me.desht.pneumaticcraft.common.particle.AirParticleData;
import me.desht.pneumaticcraft.common.tileentity.RangeManager;
import me.desht.pneumaticcraft.common.tileentity.TileEntityHeatSink;
import me.desht.pneumaticcraft.common.util.DirectionUtil;
import me.desht.pneumaticcraft.common.util.EntityFilter;
import me.desht.pneumaticcraft.common.util.IOHelper;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;

import java.util.*;

public class ModuleAirGrate extends TubeModule {
    private int grateRange;
    private boolean vacuum;
    private final Set<TileEntityHeatSink> heatSinks = new HashSet<>();
    private boolean showRange;
    private EntityFilter entityFilter = null;
    private TileEntity adjacentInsertionTE = null;
    private Direction adjacentInsertionSide;
    private final Map<BlockPos,Boolean> traceabilityCache = new HashMap<>();

    public ModuleAirGrate(ItemTubeModule itemTubeModule) {
        super(itemTubeModule);
    }

    @Override
    public double getWidth() {
        return 16D;
    }

    @Override
    public void update() {
        super.update();

        World world = pressureTube.getWorld();
        BlockPos pos = pressureTube.getPos();

        if ((world.getGameTime() & 0x1f) == 0) traceabilityCache.clear();

        int oldGrateRange = grateRange;
        grateRange = calculateRange();
        if (oldGrateRange != grateRange) {
            if (!world.isRemote) {
                getTube().getCapability(PNCCapabilities.AIR_HANDLER_MACHINE_CAPABILITY)
                        .ifPresent(h -> NetworkHandler.sendToAllTracking(new PacketUpdatePressureBlock(getTube(), null, h.getSideLeaking(), h.getAir()), getTube()));
            } else {
                if (showRange) {
                    AreaRenderManager.getInstance().showArea(RangeManager.getFrame(getAffectedAABB()), 0x60FFC060, pressureTube, false);
                }
            }
        }

        if (!world.isRemote) coolHeatSinks();
        pushEntities(world, pos, new Vector3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D));
    }

    private AxisAlignedBB getAffectedAABB() {
        BlockPos pos = pressureTube.getPos().offset(getDirection(), grateRange + 1);
        return new AxisAlignedBB(pos, pos).grow(grateRange);
    }

    private int calculateRange() {
        float range = pressureTube.getPressure() * 4f;
        vacuum = range < 0;
        if (vacuum) range *= -4f;
        return (int) range;
    }

    private void pushEntities(World world, BlockPos pos, Vector3d tileVec) {
        AxisAlignedBB bbBox = getAffectedAABB();
        List<Entity> entities = world.getEntitiesWithinAABB(Entity.class, bbBox, entityFilter);
        double d0 = grateRange * 3;
        int entitiesMoved = 0;
        for (Entity entity : entities) {
            if (!entity.world.isRemote && entity instanceof ItemEntity && entity.isAlive()
                    && entity.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) < 1D) {
                tryItemInsertion((ItemEntity) entity);
            } else if (!ignoreEntity(entity) && rayTraceOK(entity, tileVec)) {
                double x = (entity.getPosX() - pos.getX() - 0.5D) / d0;
                double y = (entity.getPosY() + entity.getEyeHeight() - pos.getY() - 0.5D) / d0;
                BlockPos entityPos = entity.getPosition();
                if (!Block.hasEnoughSolidSide(world, entityPos, Direction.UP) && !world.isAirBlock(entityPos)) {
                    y -= 0.15;  // kludge: avoid entities getting stuck on edges, e.g. farmland->full block
                }
                double z = (entity.getPosZ() - pos.getZ() - 0.5D) / d0;
                double d4 = Math.sqrt(x * x + y * y + z * z);
                double d5 = 1.0D - d4;

                if (d5 > 0.0D) {
                    d5 *= d5;
                    if (vacuum) d5 *= -1;
                    entity.move(MoverType.SELF, new Vector3d(x * d5, y * d5, z * d5));
                    entitiesMoved++;
                    if (world.isRemote && world.rand.nextDouble() < 0.2) {
                        if (vacuum) {
                            world.addParticle(AirParticleData.DENSE, entity.getPosX(), entity.getPosY(), entity.getPosZ(), -x, -y, -z);
                        } else {
                            world.addParticle(AirParticleData.DENSE, pos.getX() + 0.5 + getDirection().getXOffset(), pos.getY() + 0.5 + getDirection().getYOffset(), pos.getZ() + 0.5 + getDirection().getZOffset(), x, y, z);
                        }
                    }
                }
            }
        }
        if (!world.isRemote) {
            pressureTube.addAir(-entitiesMoved * PneumaticValues.USAGE_AIR_GRATE);
        }
    }

    private boolean ignoreEntity(Entity entity) {
        if (entity instanceof PlayerEntity) {
            return ((PlayerEntity) entity).isCreative() || entity.isSneaking() || entity.isSpectator();
        } else {
            // don't touch semiblocks, at all
            return !entity.canBePushed() || entity instanceof EntitySemiblockBase;
        }
    }

    private boolean rayTraceOK(Entity entity, Vector3d tileVec) {
        BlockPos pos = new BlockPos(entity.getEyePosition(0f));
        return traceabilityCache.computeIfAbsent(pos, k -> {
            Vector3d entityVec = new Vector3d(entity.getPosX(), entity.getPosY() + entity.getEyeHeight(), entity.getPosZ());
            RayTraceContext ctx = new RayTraceContext(entityVec, tileVec, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, entity);
            BlockRayTraceResult trace = entity.getEntityWorld().rayTraceBlocks(ctx);
            return trace.getPos().equals(pressureTube.getPos());
        });
    }

    private void tryItemInsertion(ItemEntity entity) {
        if (getAdjacentInventory() != null) {
            ItemStack stack = entity.getItem();
            ItemStack excess = IOHelper.insert(getAdjacentInventory(), stack, adjacentInsertionSide, false);
            if (excess.isEmpty()) {
                entity.remove();
            } else {
                entity.setItem(excess);
            }
        }
    }

    private TileEntity getAdjacentInventory() {
        if (adjacentInsertionTE != null && !adjacentInsertionTE.isRemoved()) {
            return adjacentInsertionTE;
        }

        adjacentInsertionTE = null;
        for (Direction dir : DirectionUtil.VALUES) {
            TileEntity inv = pressureTube.getWorld().getTileEntity(pressureTube.getPos().offset(dir));
            if (inv != null && inv.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir.getOpposite()).isPresent()) {
                adjacentInsertionTE = inv;
                adjacentInsertionSide = dir.getOpposite();
                break;
            }
        }
        return adjacentInsertionTE;
    }

    private void coolHeatSinks() {
        if (grateRange >= 2) {
            int curTeIndex = (int) (pressureTube.getWorld().getGameTime() % 27);
            BlockPos curPos = pressureTube.getPos().offset(dir, 2).add(-1 + curTeIndex % 3, -1 + curTeIndex / 3 % 3, -1 + curTeIndex / 9 % 3);
            TileEntity te = pressureTube.getWorld().getTileEntity(curPos);
            if (te instanceof TileEntityHeatSink) heatSinks.add((TileEntityHeatSink) te);

            Iterator<TileEntityHeatSink> iterator = heatSinks.iterator();
            int tubesCooled = 0;
            while (iterator.hasNext()) {
                TileEntityHeatSink heatSink = iterator.next();
                if (heatSink.isRemoved()) {
                    iterator.remove();
                } else {
                    for (int i = 0; i < 4; i++) {
                        heatSink.onFannedByAirGrate();
                    }
                    tubesCooled++;
                }
            }
            if (tubesCooled > 0) {
                pressureTube.addAir(-(5 + (tubesCooled / 3)));
            }
        }
    }

    @Override
    public void readFromNBT(CompoundNBT tag) {
        super.readFromNBT(tag);
        vacuum = tag.getBoolean("vacuum");
        grateRange = tag.getInt("grateRange");
        String f = tag.getString("entityFilter");
        entityFilter = f.isEmpty() ? null : EntityFilter.fromString(f);
    }

    @Override
    public CompoundNBT writeToNBT(CompoundNBT tag) {
        super.writeToNBT(tag);
        tag.putBoolean("vacuum", vacuum);
        tag.putInt("grateRange", grateRange);
        tag.putString("entityFilter", entityFilter == null ? "" : entityFilter.toString());
        return tag;
    }

    @Override
    public void addInfo(List<ITextComponent> curInfo) {
        super.addInfo(curInfo);
        String txt = grateRange == 0 ? "Idle" : vacuum ? "Attracting" : "Repelling";
        curInfo.add(new StringTextComponent("Status: ").appendString(txt).mergeStyle(TextFormatting.WHITE));
        curInfo.add(new StringTextComponent("Range: ").appendString(grateRange + " blocks").mergeStyle(TextFormatting.WHITE));
        if (entityFilter != null)
            curInfo.add(new StringTextComponent("Entity Filter: \"").appendString(entityFilter.toString()).appendString("\"").mergeStyle(TextFormatting.WHITE));
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(pressureTube.getPos().offset(getDirection(), grateRange + 1)).grow(grateRange * 2);
    }

    public String getEntityFilterString() {
        return entityFilter == null ? "" : entityFilter.toString();
    }

    public void setEntityFilter(String filter) {
        entityFilter = EntityFilter.fromString(filter);
    }

    @Override
    public void onPlaced() {
//        showRange = true;
    }

    public boolean isShowRange() {
        return showRange;
    }

    public void setShowRange(boolean showRange) {
        this.showRange = showRange;
        if (showRange) {
            AreaRenderManager.getInstance().showArea(RangeManager.getFrame(getAffectedAABB()), 0x60FFC060, pressureTube, false);
        } else {
            AreaRenderManager.getInstance().removeHandlers(pressureTube);
        }
    }

    @Override
    public void onRemoved() {
        if (pressureTube.getWorld().isRemote) {
            AreaRenderManager.getInstance().removeHandlers(pressureTube);
        }
    }
}

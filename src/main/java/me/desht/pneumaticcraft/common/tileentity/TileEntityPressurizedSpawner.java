package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.api.item.EnumUpgrade;
import me.desht.pneumaticcraft.api.item.ISpawnerCoreStats;
import me.desht.pneumaticcraft.common.core.ModTileEntities;
import me.desht.pneumaticcraft.common.inventory.ContainerPressurizedSpawner;
import me.desht.pneumaticcraft.common.item.ItemSpawnerCore;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TileEntityPressurizedSpawner extends TileEntityPneumaticBase implements
        IMinWorkingPressure, IRedstoneControl<TileEntityPressurizedSpawner>,
        INamedContainerProvider, IRangedTE
{
    public static final int BASE_SPAWN_INTERVAL = 200;
    private static final int MAX_NEARBY_ENTITIES = 32;

    private final ItemSpawnerCore.SpawnerCoreItemHandler inventory = new ItemSpawnerCore.SpawnerCoreItemHandler(this);
    private final LazyOptional<IItemHandler> invCap = LazyOptional.of(() -> inventory);
    @GuiSynced
    public TileEntityVacuumTrap.Problems problem = TileEntityVacuumTrap.Problems.OK;
    @GuiSynced
    private final RedstoneController<TileEntityPressurizedSpawner> rsController = new RedstoneController<>(this);
    private int counter = -1;  // -1 => re-init on next tick
    @DescSynced
    private boolean running;
    private final RangeManager rangeManager = new RangeManager(this, 0x60400040).withCustomExtents(this::buildCustomExtents);

    public TileEntityPressurizedSpawner() {
        super(ModTileEntities.PRESSURIZED_SPAWNER.get(), PneumaticValues.DANGER_PRESSURE_TIER_TWO, PneumaticValues.MAX_PRESSURE_TIER_TWO, PneumaticValues.VOLUME_PRESSURIZED_SPAWNER, 4);
    }

    @Override
    public void tick() {
        super.tick();

        rangeManager.setRange(2 + getUpgrades(EnumUpgrade.RANGE));
        if (counter < 0) counter = getSpawnInterval();

        if (!level.isClientSide) {
            ISpawnerCoreStats stats = inventory.getStats();
            running = false;
            problem = TileEntityVacuumTrap.Problems.OK;
            if (stats == null) {
                problem = TileEntityVacuumTrap.Problems.NO_CORE;
            } else if (getPressure() > getMinWorkingPressure() && rsController.shouldRun()) {
                running = true;
                if (--counter <= 0) {
                    if (!trySpawnSomething(stats)) {
                        ((ServerWorld) level).sendParticles(ParticleTypes.POOF, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, 5, 0, 0, 0, 0);
                    }
                    addAir(-getAirUsage());
                    counter = getSpawnInterval();
                }
            }
        } else {
            if (running) {
                double x = (double)worldPosition.getX() + level.random.nextDouble();
                double y = (double)worldPosition.getY() + level.random.nextDouble();
                double z = (double)worldPosition.getZ() + level.random.nextDouble();
                level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.0D, 0.0D);
                level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private AxisAlignedBB buildCustomExtents() {
        // following vanilla spawner behaviour of constrained Y-value (-1 .. +2)
        AxisAlignedBB aabb = new AxisAlignedBB(getBlockPos(), getBlockPos());
        return aabb.inflate(getRange(), 0, getRange()).expandTowards(0, 2, 0).expandTowards(0, -1, 0);
    }

    private boolean trySpawnSomething(ISpawnerCoreStats stats) {
        EntityType<?> type = stats.pickEntity(true);
        if (type != null && level instanceof ServerWorld) {
            ServerWorld serverworld = (ServerWorld)level;
            int spawnRange = getRange();
            double x = (double)worldPosition.getX() + (serverworld.random.nextDouble() - level.random.nextDouble()) * (double)spawnRange + 0.5D;
            double y = worldPosition.getY() + serverworld.random.nextInt(3) - 1;
            double z = (double)worldPosition.getZ() + (serverworld.random.nextDouble() - level.random.nextDouble()) * (double)spawnRange + 0.5D;
            if (serverworld.noCollision(type.getAABB(x, y, z))) {
                Entity entity = type.create(serverworld);
                if (!(entity instanceof MobEntity)) return false;
                MobEntity mobentity = (MobEntity) entity;
                int entityCount = serverworld.getEntitiesOfClass(MobEntity.class, rangeManager.getExtents()).size();
                if (entityCount >= MAX_NEARBY_ENTITIES) return false;
                entity.moveTo(x, y, z, level.random.nextFloat() * 360.0F, 0.0F);
                if (ForgeEventFactory.doSpecialSpawn(mobentity, level, (float)entity.getX(), (float)entity.getY(), (float)entity.getZ(), null, SpawnReason.SPAWNER)) {
                    return false;
                }
                mobentity.finalizeSpawn(serverworld, level.getCurrentDifficultyAt(entity.blockPosition()), SpawnReason.SPAWNER, null, null);
                if (!serverworld.tryAddFreshEntityWithPassengers(entity)) return false;
                level.levelEvent(Constants.WorldEvents.MOB_SPAWNER_PARTICLES, worldPosition, 0);
                mobentity.spawnAnim();
                mobentity.setPersistenceRequired();
                return true;
            }
        }
        return false;
    }

    public int getSpawnInterval() {
        return (int)(BASE_SPAWN_INTERVAL / getSpeedMultiplierFromUpgrades());
    }

    public int getAirUsage() { return PneumaticValues.USAGE_PRESSURIZED_SPAWNER * (getUpgrades(EnumUpgrade.SPEED) + 1); }

    @Override
    public IItemHandler getPrimaryInventory() {
        return inventory;
    }

    @Nonnull
    @Override
    protected LazyOptional<IItemHandler> getInventoryCap() {
        return invCap;
    }

    @Override
    public float getMinWorkingPressure() {
        return 10f;
    }

    @Override
    public RedstoneController<TileEntityPressurizedSpawner> getRedstoneController() {
        return rsController;
    }

    @Override
    public void handleGUIButtonPress(String tag, boolean shiftHeld, ServerPlayerEntity player) {
        rsController.parseRedstoneMode(tag);
    }

    @Nullable
    @Override
    public Container createMenu(int windowId, PlayerInventory inv, PlayerEntity player) {
        return new ContainerPressurizedSpawner(windowId, inv, getBlockPos());
    }

    @Override
    public CompoundNBT save(CompoundNBT tag) {
        super.save(tag);

        tag.put("Inventory", inventory.serializeNBT());

        return tag;
    }

    @Override
    public void load(BlockState state, CompoundNBT tag) {
        super.load(state, tag);

        inventory.deserializeNBT(tag.getCompound("Inventory"));
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return rangeManager.shouldShowRange() ? rangeManager.getExtents() : super.getRenderBoundingBox();
    }

    @Override
    public RangeManager getRangeManager() {
        return rangeManager;
    }
}

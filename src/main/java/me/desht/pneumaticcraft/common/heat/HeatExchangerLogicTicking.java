package me.desht.pneumaticcraft.common.heat;

import me.desht.pneumaticcraft.api.heat.HeatBehaviour;
import me.desht.pneumaticcraft.api.heat.IHeatExchangerLogic;
import me.desht.pneumaticcraft.common.heat.behaviour.HeatBehaviourManager;
import me.desht.pneumaticcraft.common.network.GuiSynced;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiPredicate;

public class HeatExchangerLogicTicking implements IHeatExchangerLogic {
    private final List<IHeatExchangerLogic> hullExchangers = new ArrayList<>();
    private final List<IHeatExchangerLogic> connectedExchangers = new ArrayList<>();
    private List<HeatBehaviour<?>> behaviours = new ArrayList<>();
    private List<HeatBehaviour<?>> newBehaviours; // required to prevent CME problems
    private double ambientTemperature = -1;
    private double temperature = HeatExchangerLogicAmbient.BASE_AMBIENT_TEMP;  // degrees Kelvin, 300K by default
    @GuiSynced
    private int temperatureInt = (int) HeatExchangerLogicAmbient.BASE_AMBIENT_TEMP;
    private double thermalResistance = 1;
    private double thermalCapacity = 1;
    private final BitSet connections = new BitSet(6);

    // prevent infinite recursion when adding/removing a connected exchanger
    private static boolean isAddingOrRemovingLogic;

    @Override
    public void initializeAsHull(World world, BlockPos pos, BiPredicate<IWorld,BlockPos> blockFilter, Direction... validSides) {
        if (ambientTemperature < 0) {
            initializeAmbientTemperature(world, pos);
        }

        if (world.isRemote) return;

        for (IHeatExchangerLogic logic : hullExchangers) {
            removeConnectedExchanger(logic);
        }
        hullExchangers.clear();
        newBehaviours = new ArrayList<>();
        connections.clear();
        for (Direction dir : validSides) {
            if (HeatBehaviourManager.getInstance().addHeatBehaviours(world, pos.offset(dir), dir, blockFilter, this, newBehaviours) > 0) {
                connections.set(dir.getIndex());
            }
            HeatExchangerManager.getInstance().getLogic(world, pos.offset(dir), dir.getOpposite(), blockFilter).ifPresent(logic -> {
                hullExchangers.add(logic);
                addConnectedExchanger(logic);
                connections.set(dir.getIndex());
            });
        }
    }

    @Override
    public boolean isSideConnected(Direction side) {
        return connections.get(side.getIndex());
    }

    @Override
    public void addConnectedExchanger(IHeatExchangerLogic exchanger) {
        connectedExchangers.add(exchanger);
        if (!isAddingOrRemovingLogic) {
            isAddingOrRemovingLogic = true;
            exchanger.addConnectedExchanger(this);
            isAddingOrRemovingLogic = false;
        }
    }

    @Override
    public void removeConnectedExchanger(IHeatExchangerLogic exchanger) {
        connectedExchangers.remove(exchanger);
        if (!isAddingOrRemovingLogic) {
            isAddingOrRemovingLogic = true;
            exchanger.removeConnectedExchanger(this);
            isAddingOrRemovingLogic = false;
        }
    }

    @Override
    public void initializeAmbientTemperature(World world, BlockPos pos) {
        ambientTemperature = HeatExchangerLogicAmbient.atPosition(world, pos).getAmbientTemperature();
    }

    @Override
    public double getTemperature() {
        return temperature;
    }

    @Override
    public int getTemperatureAsInt() { return temperatureInt; }

    @Override
    public void setTemperature(double temperature) {
        this.temperature = temperature;
        this.temperatureInt = (int) temperature;
    }

    @Override
    public void setThermalResistance(double thermalResistance) {
        this.thermalResistance = thermalResistance;
    }

    @Override
    public double getThermalResistance() {
        return thermalResistance;
    }

    @Override
    public void setThermalCapacity(double capacity) {
        thermalCapacity = capacity;
    }

    @Override
    public double getThermalCapacity() {
        return thermalCapacity;
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT tag = new CompoundNBT();
        tag.putDouble("temperature", temperature);
        ListNBT tagList = new ListNBT();
        for (HeatBehaviour<?> behaviour : behaviours) {
            CompoundNBT t = behaviour.serializeNBT();
            t.putString("id", behaviour.getId().toString());
            tagList.add(t);
        }
        tag.put("behaviours", tagList);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        temperature = nbt.getDouble("temperature");
        temperatureInt = (int) temperature;
        behaviours.clear();
        ListNBT tagList = nbt.getList("behaviours", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++) {
            CompoundNBT t = tagList.getCompound(i);
            HeatBehaviour<?> behaviour = HeatBehaviourManager.getInstance().createBehaviour(new ResourceLocation(t.getString("id")));
            if (behaviour != null) {
                behaviour.deserializeNBT(t);
                behaviours.add(behaviour);
            }
        }
    }

    @Override
    public void tick() {
        temperatureInt = (int) temperature;

        if (getThermalCapacity() < 0.1D) {
            setTemperature(ambientTemperature);
            return;
        }
        if (newBehaviours != null) {
            List<HeatBehaviour<?>> oldBehaviours = behaviours;
            behaviours = newBehaviours;
            newBehaviours = null;
            // Transfer over equal heat behaviour's info.
            for (HeatBehaviour<?> oldBehaviour : oldBehaviours) {
                int equalBehaviourIndex = behaviours.indexOf(oldBehaviour);
                if (equalBehaviourIndex >= 0) {
                    behaviours.get(equalBehaviourIndex).deserializeNBT(oldBehaviour.serializeNBT());
                }
            }
        }
        Iterator<HeatBehaviour<?>> iterator = behaviours.iterator();
        while (iterator.hasNext()) {
            HeatBehaviour<?> behaviour = iterator.next();
            // upon loading from NBT the world is null. gets initialized once 'initializeAsHull' is invoked.
            if (behaviour.getWorld() != null) {
                if (behaviour.isApplicable()) {
                    behaviour.tick();
                } else {
                    iterator.remove();
                }
            }
        }
        for (IHeatExchangerLogic logic : connectedExchangers) {
            // Counting the connected ticking heat exchangers here is important, since they will all tick;
            // this count acts as a divider so the total heat dispersal is constant
            exchange(logic, this, getTickingHeatExchangers());
        }
    }

    @Override
    public double getAmbientTemperature() {
        return ambientTemperature;
    }

    public static void exchange(IHeatExchangerLogic logic, IHeatExchangerLogic logic2) {
        exchange(logic, logic2, 1);
    }

    private static void exchange(IHeatExchangerLogic logic, IHeatExchangerLogic logic2, double dispersionDivider) {
        if (logic.getThermalCapacity() < 0.1D) {
            logic.setTemperature(logic.getAmbientTemperature());
            return;
        }
        double deltaTemp = logic.getTemperature() - logic2.getTemperature();

        double totalResistance = logic2.getThermalResistance() + logic.getThermalResistance();
        deltaTemp /= dispersionDivider;
        deltaTemp /= totalResistance;

        // Calculate the heat needed to exactly equalize the heat.
        double maxDeltaTemp = (logic.getTemperature() * logic.getThermalCapacity() - logic2.getTemperature() * logic2.getThermalCapacity()) / 2;
        if (maxDeltaTemp >= 0 && deltaTemp > maxDeltaTemp || maxDeltaTemp <= 0 && deltaTemp < maxDeltaTemp)
            deltaTemp = maxDeltaTemp;
        logic2.addHeat(deltaTemp);
        logic.addHeat(-deltaTemp);
    }

    private int getTickingHeatExchangers() {
        int tickingHeatExchangers = 1;
        for (IHeatExchangerLogic logic : connectedExchangers) {
            if (logic instanceof HeatExchangerLogicTicking) tickingHeatExchangers++;
        }
        return tickingHeatExchangers;
    }

    @Override
    public void addHeat(double amount) {
        setTemperature(MathHelper.clamp(temperature + amount / getThermalCapacity(), 0, 2273));
    }
}

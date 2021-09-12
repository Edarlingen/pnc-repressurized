package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.common.block.BlockElevatorCaller;
import me.desht.pneumaticcraft.common.core.ModTileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.IItemHandler;

public class TileEntityElevatorCaller extends TileEntityTickableBase implements ICamouflageableTE, IRedstoneControl<TileEntityElevatorCaller> {
    private ElevatorButton[] floors = new ElevatorButton[0];
    private int thisFloor;
    private boolean emittingRedstone;
    private boolean shouldUpdateNeighbors;
    private BlockState camoState;
    private final RedstoneController<TileEntityElevatorCaller> rsController = new RedstoneController<>(this);

    public TileEntityElevatorCaller() {
        super(ModTileEntities.ELEVATOR_CALLER.get());
    }

    public void setEmittingRedstone(boolean emittingRedstone) {
        if (emittingRedstone != this.emittingRedstone) {
            this.emittingRedstone = emittingRedstone;
            shouldUpdateNeighbors = true;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (shouldUpdateNeighbors) {
            updateNeighbours();
            shouldUpdateNeighbors = false;
        }
    }

    public boolean getEmittingRedstone() {
        return emittingRedstone;
    }

    @Override
    public void load(BlockState state, CompoundNBT tag) {
        super.load(state, tag);
        emittingRedstone = tag.getBoolean("emittingRedstone");
        thisFloor = tag.getInt("thisFloor");
        shouldUpdateNeighbors = tag.getBoolean("shouldUpdateNeighbors");
    }

    @Override
    public CompoundNBT save(CompoundNBT tag) {
        super.save(tag);
        tag.putBoolean("emittingRedstone", emittingRedstone);
        tag.putInt("thisFloor", thisFloor);
        tag.putBoolean("shouldUpdateNeighbors", shouldUpdateNeighbors);
        return tag;
    }

    @Override
    public void readFromPacket(CompoundNBT tag) {
        super.readFromPacket(tag);
        int floorAmount = tag.getInt("floors");
        floors = new ElevatorButton[floorAmount];
        for (int i = 0; i < floorAmount; i++) {
            floors[i] = new ElevatorButton(tag.getCompound("floor" + i));
        }
        camoState = ICamouflageableTE.readCamo(tag);
    }

    @Override
    public void writeToPacket(CompoundNBT tag) {
        super.writeToPacket(tag);
        tag.putInt("floors", floors.length);
        for (ElevatorButton floor : floors) {
            tag.put("floor" + floor.floorNumber, floor.writeToNBT(new CompoundNBT()));
        }
        ICamouflageableTE.writeCamo(tag, camoState);
    }

    @Override
    public void onNeighborBlockUpdate(BlockPos fromPos) {
        boolean wasPowered = getRedstoneController().getCurrentRedstonePower() > 0;
        super.onNeighborBlockUpdate(fromPos);
        if (getRedstoneController().getCurrentRedstonePower() > 0 && !wasPowered) {
            BlockElevatorCaller.setSurroundingElevators(getLevel(), getBlockPos(), thisFloor);
        }
    }

    @Override
    public IItemHandler getPrimaryInventory() {
        return null;
    }

    void setFloors(ElevatorButton[] floors, int thisFloorLevel) {
        this.floors = floors;
        thisFloor = thisFloorLevel;
        sendDescriptionPacket();
    }

    public ElevatorButton[] getFloors() {
        return floors;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), getBlockPos().getX() + 1, getBlockPos().getY() + 1, getBlockPos().getZ() + 1);
    }

    @Override
    public BlockState getCamouflage() {
        return camoState;
    }

    @Override
    public void setCamouflage(BlockState state) {
        camoState = state;
        ICamouflageableTE.syncToClient(this);
    }

    @Override
    public RedstoneController<TileEntityElevatorCaller> getRedstoneController() {
        return rsController;
    }

    public static class ElevatorButton {
        public final float posX, posY, width, height;
        public final int floorNumber;
        public final int floorHeight;
        public float red, green, blue;
        public String buttonText;

        ElevatorButton(float posX, float posY, float width, float height, int floorNumber, int floorHeight) {
            this.posX = posX;
            this.posY = posY;
            this.width = width;
            this.height = height;
            this.floorNumber = floorNumber;
            this.floorHeight = floorHeight;
            this.buttonText = floorNumber + 1 + "";
        }

        ElevatorButton(CompoundNBT tag) {
            this.posX = tag.getFloat("posX");
            this.posY = tag.getFloat("posY");
            this.width = tag.getFloat("width");
            this.height = tag.getFloat("height");
            this.buttonText = tag.getString("buttonText");
            this.floorNumber = tag.getInt("floorNumber");
            this.floorHeight = tag.getInt("floorHeight");
            this.red = tag.getFloat("red");
            this.green = tag.getFloat("green");
            this.blue = tag.getFloat("blue");
        }

        void setColor(float red, float green, float blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        public CompoundNBT writeToNBT(CompoundNBT tag) {
            tag.putFloat("posX", posX);
            tag.putFloat("posY", posY);
            tag.putFloat("width", width);
            tag.putFloat("height", height);
            tag.putString("buttonText", buttonText);
            tag.putInt("floorNumber", floorNumber);
            tag.putInt("floorHeight", floorHeight);
            tag.putFloat("red", red);
            tag.putFloat("green", green);
            tag.putFloat("blue", blue);
            return tag;
        }
    }
}

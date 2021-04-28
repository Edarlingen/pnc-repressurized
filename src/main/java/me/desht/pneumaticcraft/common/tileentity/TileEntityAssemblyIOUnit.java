package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.api.crafting.recipe.AssemblyRecipe;
import me.desht.pneumaticcraft.common.core.ModBlocks;
import me.desht.pneumaticcraft.common.core.ModTileEntities;
import me.desht.pneumaticcraft.common.inventory.handler.BaseItemStackHandler;
import me.desht.pneumaticcraft.common.network.DescSynced;
import me.desht.pneumaticcraft.common.network.LazySynced;
import me.desht.pneumaticcraft.common.recipes.assembly.AssemblyProgram;
import me.desht.pneumaticcraft.common.util.CountedItemStacks;
import me.desht.pneumaticcraft.common.util.DirectionUtil;
import me.desht.pneumaticcraft.common.util.IOHelper;
import me.desht.pneumaticcraft.lib.TileEntityConstants;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Collection;

public class TileEntityAssemblyIOUnit extends TileEntityAssemblyRobot {
    private static final byte SLEEP_TICKS = 50;

    private static final byte STATE_IDLE = 0;
    private static final byte STATE_SEARCH_SRC = 1;
    private static final byte STATE_CLOSECLAW_AFTER_PICKUP = 5;
    private static final byte STATE_RESET_CLOSECLAW_AFTER_PICKUP = 20;
    private static final byte STATE_RESET_GOTO_IDLE = 26;
    private static final byte STATE_MAX = 127;

    @DescSynced
    private boolean shouldClawClose;
    @DescSynced
    @LazySynced
    public float clawProgress;
    public float oldClawProgress;
    @DescSynced
    private final BaseItemStackHandler itemHandler = new BaseItemStackHandler(this, 1);

    private Collection<AssemblyRecipe> recipeList;
    private ItemStack searchedItemStack = ItemStack.EMPTY;
    private byte state = 0;
    private byte tickCounter = 0;

    public TileEntityAssemblyIOUnit() {
        super(ModTileEntities.ASSEMBLY_IO_UNIT.get());
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isRemote) {
            if (!isClawDone()) moveClaw();
        } else {
            slowMode = false;
            switch (state) {
                case STATE_IDLE:
                    break;
                case STATE_SEARCH_SRC:
                    if (findPickupLocation()) state++;
                    break;
                // rise to the right height for target location
                case 2: // for pickup
                case 7: // for drop-off
                case 22: // for reset
                    if (hoverOverTarget()) state++;
                    break;
                // turn and move to target
                case 3: // for pickup
                case 8: // for drop-off
                case 23: // for reset
                    slowMode = true;
                    if (gotoTarget()) state++;
                    break;
                case 4: // pickup item - need to pick up before closeClaw; claw needs to know item size to 'grab' it!
                    if (getItemFromCurrentDirection()) state++;
                    break;
                case STATE_CLOSECLAW_AFTER_PICKUP:
                case STATE_RESET_CLOSECLAW_AFTER_PICKUP:
                    if (closeClaw()) state++;
                    break;
                case 6:
                case 21:
                    if (findDropOffLocation()) state++;
                    break;
                case 9:
                case 24:
                    if (openClaw()) state++;
                    break;
                case 10: // drop off item
                case 25:
                    if (putItemToCurrentDirection()) state++;
                    break;
                case 11:
                case STATE_RESET_GOTO_IDLE:
                    if (gotoIdlePos()) state = 0;
                case STATE_MAX: // this will be set if we encounter an unknown state; prevents log-spam that would result from default-case
                    break;
                default:
                    System.out.printf("unexpected state: %d%n", state);
                    state = STATE_MAX;
                    break;
            }
        }
    }

    @Override
    public boolean reset() {
        if (state >= STATE_RESET_CLOSECLAW_AFTER_PICKUP) {
            return false;
        } else if (!itemHandler.getStackInSlot(0).isEmpty()) {
            state = STATE_RESET_CLOSECLAW_AFTER_PICKUP;
            return false;
        } else if (state == STATE_IDLE) {
            return true;
        } else {
            state = STATE_RESET_GOTO_IDLE;
            return isIdle();
        }
    }

    /**
     * @return true if the controller should use air and display 'running'
     */
    public boolean pickupItem(Collection<AssemblyRecipe> list) {
        recipeList = list;

        if (state == STATE_IDLE) state++;

        // don't use air while waiting for item/inventory to be available
        return state > STATE_IDLE && !isSleeping() && state < STATE_MAX;
    }

    private boolean gotoIdlePos() {
        gotoHomePosition();
        return isDoneMoving();
    }

    private boolean findPickupLocation() {
        if (shouldSleep()) return false;

        TargetDirections inventoryDir = null;

        if (isImportUnit()) {
            searchedItemStack = ItemStack.EMPTY;
            if (recipeList != null) {
                ItemImportResult result = findImportInventory();
                if (result != null) {
                    searchedItemStack = result.stack;
                    inventoryDir = result.targetDirs;
                }
            }
        } else {
            inventoryDir = getPlatformDirection();
        }

        targetDirection = inventoryDir;

        if (targetDirection == null) {
            sleepBeforeNextSearch();
            return false;
        } else return true;
    }

    private ItemImportResult findImportInventory() {
        for (Direction dir : DirectionUtil.HORIZONTALS) {
            TileEntity te = getCachedNeighbor(dir);
            if (te != null) {
                ItemStack res = searchImportInventory(te);
                if (!res.isEmpty()) {
                    return new ItemImportResult(dir, res);
                }
            }
        }
        for (Direction secDir : new Direction[]{Direction.WEST, Direction.EAST}) {
            for (Direction primDir : new Direction[]{Direction.NORTH, Direction.SOUTH}) {
                TileEntity te = getWorld().getTileEntity(getPos().offset(primDir).offset(secDir));
                if (te != null) {
                    ItemStack res = searchImportInventory(te);
                    if (!res.isEmpty()) {
                        return new ItemImportResult(primDir, secDir, res);
                    }
                }
            }
        }
        return null;
    }

    private ItemStack searchImportInventory(TileEntity te) {
        CountedItemStacks counted = IOHelper.getInventoryForTE(te, Direction.UP)
                .map(CountedItemStacks::new)
                .orElse(null);
        if (counted != null) {
            NonNullList<ItemStack> stacks = counted.coalesce();
            for (AssemblyRecipe recipe : recipeList) {
                for (ItemStack stack : stacks) {
                    if (recipe.matches(stack)) {
                        return ItemHandlerHelper.copyStackWithSize(stack, recipe.getInputAmount());
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean isSleeping() {
        return tickCounter > 0;
    }

    private boolean shouldSleep() {
        if (tickCounter > 0 && tickCounter++ < SLEEP_TICKS) {
            return true;
        } else {
            tickCounter = 0;
            return false;
        }
    }

    private void sleepBeforeNextSearch() {
        tickCounter = 1;
    }

    private boolean findDropOffLocation() {
        if (shouldSleep()) {
            return false;
        }
        targetDirection = isImportUnit() ? getPlatformDirection() : getExportLocationForItem(itemHandler.getStackInSlot(0));
        if (targetDirection == null) {
            sleepBeforeNextSearch();
            return false;
        } else {
            return true;
        }
    }

    private boolean getItemFromCurrentDirection() {
        TileEntity tile = getTileEntityForCurrentDirection();

        boolean extracted = false;

        if (isImportUnit()) {
            if (searchedItemStack.isEmpty()) { // we don't know what we're supposed to pick up
                reset();
            } else {
                extracted = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.UP).map(sourceInv -> {
                    ItemStack heldStack = itemHandler.getStackInSlot(0);
                    int initialHeldAmount = heldStack.getCount();
                    boolean foundIt = false;
                    int needed = searchedItemStack.getCount() - heldStack.getCount();
                    for (int i = 0; i < sourceInv.getSlots() && !foundIt; i++) {
                        ItemStack stack = sourceInv.getStackInSlot(i);
                        if (stack.isEmpty()) continue;
                        if (heldStack.isEmpty() && stack.isItemEqual(searchedItemStack)
                                || ItemHandlerHelper.canItemStacksStack(heldStack, stack)) {
                            ItemStack takenStack = sourceInv.extractItem(i, needed, false);
                            ItemStack excess = itemHandler.insertItem(0, takenStack, false);
                            needed -= (takenStack.getCount() - excess.getCount());
                            // excess will be empty under all normal circumstances, but let's be careful...
                            ItemHandlerHelper.insertItem(sourceInv, excess, false);
                        }
                        foundIt = needed <= 0;
                    }

                    if (initialHeldAmount == itemHandler.getStackInSlot(0).getCount()) { // nothing picked up, search for different inventory
                        state = STATE_SEARCH_SRC;
                    }
                    return foundIt;
                }).orElseGet(() -> {
                    state = STATE_SEARCH_SRC; // inventory gone
                    return false;
                });
            }
        } else {
            if (tile instanceof TileEntityAssemblyPlatform) {
                TileEntityAssemblyPlatform plat = (TileEntityAssemblyPlatform) tile;

                if (plat.openClaw()) {
                    itemHandler.setStackInSlot(0, plat.getHeldStack());
                    plat.setHeldStack(ItemStack.EMPTY);
                    extracted = !itemHandler.getStackInSlot(0).isEmpty();
                    if (!extracted) { // something went wrong - either the platform is gone altogether, or the item is not there anymore
                        state = STATE_SEARCH_SRC;
                    }
                }
            }
        }

        return extracted;
    }

    private boolean putItemToCurrentDirection() {
        if (isImportUnit()) {
            TileEntity tile = getTileEntityForCurrentDirection();
            if (tile instanceof TileEntityAssemblyPlatform) {
                TileEntityAssemblyPlatform plat = (TileEntityAssemblyPlatform) tile;

                if (itemHandler.getStackInSlot(0).isEmpty()) {
                    return plat.closeClaw();
                }

                if (plat.isIdle()) {
                    plat.setHeldStack(itemHandler.getStackInSlot(0));
                    itemHandler.setStackInSlot(0, ItemStack.EMPTY);
                    return plat.closeClaw();
                }
            } else {
                repeatDropOffSearch(); // platform gone; close claw and search new drop-off-location
            }
        } else {
            TileEntity te = getTileEntityForCurrentDirection();
            if (te == null) repeatDropOffSearch(); // inventory gone; close claw and search new drop-off-location
            else {
                ItemStack currentStack = itemHandler.getStackInSlot(0);
                int startSize = currentStack.getCount();
                ItemStack excess = IOHelper.insert(te, currentStack, Direction.UP, false);
                itemHandler.setStackInSlot(0, excess);
                currentStack = itemHandler.getStackInSlot(0);
                if (!currentStack.isEmpty() && startSize == currentStack.getCount())
                    repeatDropOffSearch(); // target-inventory full or unavailable
            }

            return itemHandler.getStackInSlot(0).isEmpty();
        }

        return false;
    }

    private void repeatDropOffSearch() {
        state = state >= STATE_RESET_CLOSECLAW_AFTER_PICKUP ? STATE_RESET_CLOSECLAW_AFTER_PICKUP : STATE_CLOSECLAW_AFTER_PICKUP;
    }

    private boolean closeClaw() {
        shouldClawClose = true;
        return moveClaw();
    }

    private boolean openClaw() {
        shouldClawClose = false;
        return moveClaw();
    }

    private boolean moveClaw() {
        oldClawProgress = clawProgress;

        if (!shouldClawClose && clawProgress > 0F) {
            clawProgress = Math.max(clawProgress - TileEntityConstants.ASSEMBLY_IO_UNIT_CLAW_SPEED * speed, 0);
        } else if (shouldClawClose && clawProgress < 1F) {
            clawProgress = Math.min(clawProgress + TileEntityConstants.ASSEMBLY_IO_UNIT_CLAW_SPEED * speed, 1);
        }
        markDirty();
        return isClawDone();
    }

    private boolean isClawDone() {
        // need to make sure that clawProgress and oldClawProgress are the same, or we will get rendering artifacts
        return clawProgress == oldClawProgress && clawProgress == (shouldClawClose ? 1F : 0F);
    }

    public boolean isImportUnit() {
        return getBlockState().getBlock() == ModBlocks.ASSEMBLY_IO_UNIT_IMPORT.get();
    }

    @Override
    public void gotoHomePosition() {
        super.gotoHomePosition();

        if (isClawDone()) openClaw();
    }

    @Override
    public boolean isIdle() {
        return state == STATE_IDLE;
    }

    @Override
    public AssemblyProgram.EnumMachine getAssemblyType() {
        return isImportUnit() ? AssemblyProgram.EnumMachine.IO_UNIT_IMPORT : AssemblyProgram.EnumMachine.IO_UNIT_EXPORT;
    }

    private static class ItemImportResult {
        final TargetDirections targetDirs;
        final ItemStack stack;

        ItemImportResult(Direction targetDirs, ItemStack stack) {
            this(targetDirs, null, stack);
        }

        ItemImportResult(Direction primDir, Direction secDir, ItemStack stack) {
            this.targetDirs = new TargetDirections(primDir, secDir);
            this.stack = stack;
        }
    }

    private TargetDirections getExportLocationForItem(ItemStack exportedItem) {
        if (!exportedItem.isEmpty()) {
            for (Direction dir : DirectionUtil.HORIZONTALS) {
                TileEntity te = getWorld().getTileEntity(getPos().offset(dir));
                int slot = getPlacementSlot(exportedItem, te);
                if (slot >= 0) return new TargetDirections(dir);
            }
            if (canMoveToDiagonalNeighbours()) {
                for (Direction secDir : new Direction[]{Direction.WEST, Direction.EAST}) {
                    for (Direction primDir : new Direction[]{Direction.NORTH, Direction.SOUTH}) {
                        TileEntity te = getWorld().getTileEntity(getPos().offset(primDir).offset(secDir));
                        int slot = getPlacementSlot(exportedItem, te);
                        if (slot >= 0) return new TargetDirections(primDir, secDir);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Find a slot into which to place an exported item.  Note that other assembly robots are not valid export
     * locations, but any other TE which provides CAPABILITY_ITEM_HANDLER on the top face is a valid candidate.
     *
     * @param exportedItem item to export
     * @param te where the item is being attempted to insert to (will use the top face for IItemHandler cap.)
     * @return the placement slot, or -1 when the item can't be placed / accessed
     */
    private static int getPlacementSlot(ItemStack exportedItem, TileEntity te) {
        if (te == null || te instanceof TileEntityAssemblyRobot) return -1;

        return te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.UP).map(handler -> {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack excess = handler.insertItem(slot, exportedItem, true);
                if (excess.getCount() < exportedItem.getCount()) {
                    return slot;
                }
            }
            return -1;
        }).orElse(-1);
    }

    @Override
    public void read(BlockState blockState, CompoundNBT tag) {
        super.read(blockState, tag);

        clawProgress = tag.getFloat("clawProgress");
        shouldClawClose = tag.getBoolean("clawClosing");
        state = tag.getByte("state");
        itemHandler.deserializeNBT(tag.getCompound("Items"));
    }

    @Override
    public IItemHandler getPrimaryInventory() {
        return itemHandler;
    }

    @Override
    public CompoundNBT write(CompoundNBT tag) {
        super.write(tag);
        tag.putFloat("clawProgress", clawProgress);
        tag.putBoolean("clawClosing", shouldClawClose);
        tag.putByte("state", state);
        tag.put("Items", itemHandler.serializeNBT());
        return tag;
    }

    @Override
    public boolean canMoveToDiagonalNeighbours() {
        return true;
    }
}

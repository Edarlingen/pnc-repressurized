package me.desht.pneumaticcraft.common.ai;

import me.desht.pneumaticcraft.api.harvesting.IHarvestHandler;
import me.desht.pneumaticcraft.common.harvesting.HarvestRegistry;
import me.desht.pneumaticcraft.common.progwidgets.IToolUser;
import me.desht.pneumaticcraft.common.progwidgets.ProgWidgetAreaItemBase;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DroneAIHarvest extends DroneAIBlockInteraction<ProgWidgetAreaItemBase> {

    /**
     * @param drone the drone
     * @param widget needs to implement IBlockOrdered, IToolUser
     */
    public DroneAIHarvest(IDroneBase drone, ProgWidgetAreaItemBase widget) {
        super(drone, widget);
    }

    @Override
    public boolean shouldExecute(){
        if(abortIfRequiredHoeIsMissing()) return false;        
        return super.shouldExecute();
    }
    
    @Override
    protected boolean isValidPosition(BlockPos pos) {
        if(abortIfRequiredHoeIsMissing()) return false;        
        return getApplicableHandler(pos) != null;
    }
    
    private boolean abortIfRequiredHoeIsMissing(){
        if(((IToolUser) progWidget).requiresTool() && getDamageableHoe() == null){
            abort();
            drone.addDebugEntry("gui.progWidget.harvest.debug.missingHoe");
            return true;
        }else{
            return false;
        }
    }
    
    private IHarvestHandler getApplicableHandler(BlockPos pos){
        BlockState state = worldCache.getBlockState(pos);
        return HarvestRegistry.getInstance()
                              .getHarvestHandlers()
                              .stream()
                              .filter(handler -> handler.canHarvest(drone.world(), worldCache, pos, state, drone) &&
                                                  hasApplicableItemFilters(handler, pos, state))
                              .findFirst()
                              .orElse(null);
    }
    
    private boolean hasApplicableItemFilters(IHarvestHandler harvestHandler, BlockPos pos, BlockState blockState){
        List<ItemStack> droppedStacks = harvestHandler.addFilterItems(drone.world(), worldCache, pos, blockState, drone);
        for (ItemStack droppedStack : droppedStacks) {
            if (progWidget.isItemValidForFilters(droppedStack, blockState)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean respectClaims() {
        return true;
    }

    @Override
    protected boolean doBlockInteraction(BlockPos pos, double distToBlock) {
        IHarvestHandler applicableHandler = getApplicableHandler(pos);
        if(applicableHandler != null){
            BlockState state = worldCache.getBlockState(pos);
            if(applicableHandler.canHarvest(drone.world(), worldCache, pos, state, drone)){
                Consumer<PlayerEntity> damageableHoe = getDamageableHoe();
                if(damageableHoe != null){
                    if(applicableHandler.harvestAndReplant(drone.world(), worldCache, pos, state, drone)){
                        damageableHoe.accept(drone.getFakePlayer());
                    }
                }else{
                    applicableHandler.harvest(drone.world(), worldCache, pos, state, drone);
                }
                
            }
        }
        return false;
    }
    
    private Consumer<PlayerEntity> getDamageableHoe(){
        for(int i = 0; i < drone.getInv().getSlots(); i++){
            ItemStack stack = drone.getInv().getStackInSlot(i);
            BiConsumer<ItemStack, PlayerEntity> damageableHoe = HarvestRegistry.getInstance().getDamageableHoe(stack);
            if(damageableHoe != null) return player -> damageableHoe.accept(stack, player);
        }
        return null;
    }
}

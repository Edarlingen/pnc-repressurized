package me.desht.pneumaticcraft.common.entity.living;

import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.item.EnumUpgrade;
import me.desht.pneumaticcraft.common.core.ModEntities;
import me.desht.pneumaticcraft.common.core.ModItems;
import me.desht.pneumaticcraft.common.util.UpgradableItemUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;

public class EntityAmadrone extends EntityDrone {
    private static ItemStack amadroneStack = ItemStack.EMPTY;

    public enum AmadronAction { TAKING_PAYMENT, RESTOCKING }

    private ResourceLocation handlingOffer; // ID of an Amadron offer
    private int offerTimes;
    private ItemStack usedTablet = ItemStack.EMPTY;  // Tablet used to place the order.
    private String buyingPlayer;
    private AmadronAction amadronAction;

    public EntityAmadrone(EntityType<? extends EntityDrone> type, World world) {
        super(type, world, null);

        setCustomName(new TranslationTextComponent("pneumaticcraft.drone.amadronDeliveryDrone"));
    }

    public static EntityAmadrone makeAmadrone(World world, BlockPos pos) {
        EntityAmadrone drone = new EntityAmadrone(ModEntities.AMADRONE.get(), world);
        drone.readFromItemStack(getAmadroneStack());

        int startY = world.getHeight(Heightmap.Type.WORLD_SURFACE, pos.add(30, 0, 0)).getY() + 27 + world.rand.nextInt(6);
        drone.setPosition(pos.getX() + 27 + world.rand.nextInt(6), startY, pos.getZ() + world.rand.nextInt(6) - 3);

        return drone;
    }

    private static ItemStack getAmadroneStack() {
        if (amadroneStack.isEmpty()) {
            amadroneStack = new ItemStack(ModItems.DRONE.get());
            ItemStackHandler upgradeInv = new ItemStackHandler(9);
            upgradeInv.setStackInSlot(0, EnumUpgrade.SPEED.getItemStack(10));
            upgradeInv.setStackInSlot(1, EnumUpgrade.INVENTORY.getItemStack(35));
            upgradeInv.setStackInSlot(2, EnumUpgrade.ITEM_LIFE.getItemStack(10));
            upgradeInv.setStackInSlot(3, EnumUpgrade.SECURITY.getItemStack());
            UpgradableItemUtils.setUpgrades(amadroneStack, upgradeInv);
            amadroneStack.getCapability(PNCCapabilities.AIR_HANDLER_ITEM_CAPABILITY)
                    .orElseThrow(RuntimeException::new).addAir(100000);
        }
        return amadroneStack;
    }

    public void setHandlingOffer(ResourceLocation offerId, int times, @Nonnull ItemStack usedTablet, String buyingPlayer, AmadronAction amadronAction) {
        this.handlingOffer = offerId;
        this.offerTimes = times;
        this.usedTablet = usedTablet.copy();
        this.buyingPlayer = buyingPlayer;
        this.amadronAction = amadronAction;
    }

    public ResourceLocation getHandlingOffer() {
        return handlingOffer;
    }

    public AmadronAction getAmadronAction() {
        return amadronAction;
    }

    public int getOfferTimes() {
        return offerTimes;
    }

    public ItemStack getUsedTablet() {
        return usedTablet;
    }

    public String getBuyingPlayer() {
        return buyingPlayer;
    }

    @Override
    public boolean shouldDropAsItem() {
        return false;
    }

    @Override
    protected boolean canDropLoot() {
        return false;
    }

    @Override
    protected void dropInventory() {
        // The DroneSuicideEvent for amadrones *should* ensure the drone's inventory is emptied before death,
        // but see https://github.com/TeamPneumatic/pnc-repressurized/issues/507
        // So let's be extra-paranoid and drop nothing here
    }

    @Override
    public int getUpgrades(EnumUpgrade upgrade) {
        switch (upgrade) {
            case SECURITY:
                return 1;
            case ITEM_LIFE:
            case SPEED:
                return 10;
            case INVENTORY:
                return 35;
            default:
                return 0;
        }
    }

    @Override
    public void writeAdditional(CompoundNBT tag) {
        super.writeAdditional(tag);

        if (handlingOffer != null) {
            CompoundNBT subTag = new CompoundNBT();
            subTag.putString("offerId", handlingOffer.toString());
            subTag.putInt("offerTimes", offerTimes);
            subTag.putString("buyingPlayer", buyingPlayer);
            if (!usedTablet.isEmpty()) subTag.put("usedTablet", usedTablet.write(new CompoundNBT()));
            subTag.putString("amadronAction", amadronAction.toString());
            tag.put("amadron", subTag);
        }
    }

    @Override
    public void readAdditional(CompoundNBT tag) {
        super.readAdditional(tag);

        if (tag.contains("amadron")) {
            CompoundNBT subTag = tag.getCompound("amadron");
            handlingOffer = new ResourceLocation(subTag.getString("offerId"));
            usedTablet = ItemStack.read(subTag.getCompound("usedTablet"));
            offerTimes = subTag.getInt("offerTimes");
            buyingPlayer = subTag.getString("buyingPlayer");
            amadronAction = AmadronAction.valueOf(subTag.getString("amadronAction"));
        }
    }
}

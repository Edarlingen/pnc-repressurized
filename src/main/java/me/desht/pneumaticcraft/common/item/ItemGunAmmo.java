package me.desht.pneumaticcraft.common.item;

import me.desht.pneumaticcraft.api.item.IItemRegistry;
import me.desht.pneumaticcraft.common.config.ConfigHandler;
import me.desht.pneumaticcraft.common.minigun.Minigun;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityShulkerBullet;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public abstract class ItemGunAmmo extends ItemPneumatic {

    public ItemGunAmmo(String name) {
        super(name);
        setMaxStackSize(1);
        setMaxDamage(getCartridgeSize());
    }

    /**
     * Get the cartridge size.
     *
     * @return the max number of shots for this ammo cartridge
     */
    protected abstract int getCartridgeSize();

    /**
     * Get this ammo's range modifier.
     *
     * @param ammoStack this ammo
     * @return the range modifier; base minigun range is multiplied by this value
     */
    public float getRangeMultiplier(ItemStack ammoStack) {
        return 1.0f;
    }

    /**
     * Get the air usage multiplier.
     *
     *
     * @param minigun
     * @param ammoStack this ammo
     * @return the usage multiplier; base minigun air usage is multiplied by this value
     */
    public float getAirUsageMultiplier(Minigun minigun, ItemStack ammoStack) {
        return 1.0f;
    }

    /**
     * Get the damage multiplier.
     *
     * @param ammoStack this ammo
     * @return the damage multiplier; standard physical minigun bullet damage is multiplied by this value
     */
    public float getDamageMultiplier(ItemStack ammoStack) {
        return 1.0f;
    }

    /**
     * Get the color used to render this ammo, both when rendering the minigun model, and when drawing the bullet
     * traces.
     *
     * @param ammo the ammo cartridge
     * @return a rendering color (ARGB format)
     */
    @SideOnly(Side.CLIENT)
    public abstract int getAmmoColor(ItemStack ammo);

    /**
     * Get the cost to fire this ammo, which is the number of rounds used up in one shot.
     *
     * @param ammoStack the ammo stack
     * @return the ammo cost
     */
    public int getAmmoCost(ItemStack ammoStack) {
        return 1;
    }

    protected DamageSource getDamageSource(EntityPlayer shooter) {
        return DamageSource.causePlayerDamage(shooter);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World world, List<String> infoList, ITooltipFlag extraInfo) {
        infoList.add(I18n.format("gui.tooltip.gunAmmo.ammoRemaining", stack.getMaxDamage() - stack.getItemDamage(), stack.getMaxDamage()));
        super.addInformation(stack, world, infoList, extraInfo);
    }

    /**
     * Called when an entity is shot by the minigun's wielder. This method is responsible for applying any damage and
     * other possible effects to the entity.
     *
     * @param minigun the minigun being used
     * @param ammo the ammo cartridge stack used
     * @param target the entity which has been hit
     * @return the number of rounds fired
     */
    public int onTargetHit(Minigun minigun, ItemStack ammo, Entity target) {
        int times = 1;
        int nSpeed = minigun.getUpgrades(IItemRegistry.EnumUpgrade.SPEED);
        for (int i = 0; i < nSpeed; i++) {
            if (minigun.getWorld().rand.nextBoolean()) times++;
        }

        if (getDamageMultiplier(ammo) > 0) {
            if (target instanceof EntityLivingBase) {
                target.attackEntityFrom(getDamageSource(minigun.getPlayer()), ConfigHandler.general.configMinigunDamage * getDamageMultiplier(ammo) * times);
            } else if (target instanceof EntityShulkerBullet || target instanceof EntityFireball) {
                target.setDead();
            }
        }
        return times;
    }

    /**
     * Called when a block is hit by the minigun's wielder.
     *
     * @param minigun the minigun being used
     * @param ammo the ammo cartridge stack used
     * @param pos the block that was hit
     * @param face the side of the block that was hit
     * @return the number of rounds fired
     */
    public int onBlockHit(Minigun minigun, ItemStack ammo, BlockPos pos, EnumFacing face) {
        double x = pos.getX() + face.getXOffset();
        double y = pos.getY() + face.getYOffset();
        double z = pos.getZ() + face.getZOffset();
        World w = minigun.getPlayer().world;
        IBlockState state = w.getBlockState(pos);
        ((WorldServer) w).spawnParticle(EnumParticleTypes.BLOCK_DUST, x + 0.5, y + 0.5, z + 0.5, 15,
                face.getXOffset() * 0.2, face.getYOffset() * 0.2, face.getZOffset() * 0.2, 0.05, Block.getStateId(state));

        // not taking speed upgrades into account here; being kind to players who miss a lot...
        return 1;
    }
}

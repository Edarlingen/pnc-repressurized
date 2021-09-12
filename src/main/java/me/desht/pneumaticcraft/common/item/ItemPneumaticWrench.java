package me.desht.pneumaticcraft.common.item;

import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.block.IPneumaticWrenchable;
import me.desht.pneumaticcraft.api.pressure.IPressurizableItem;
import me.desht.pneumaticcraft.common.advancements.AdvancementTriggers;
import me.desht.pneumaticcraft.common.core.ModItems;
import me.desht.pneumaticcraft.common.core.ModSounds;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketPlaySound;
import me.desht.pneumaticcraft.lib.PneumaticValues;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemPneumaticWrench extends ItemPressurizable {

    public ItemPneumaticWrench() {
        super(ModItems.toolProps(), PneumaticValues.PNEUMATIC_WRENCH_MAX_AIR, PneumaticValues.PNEUMATIC_WRENCH_VOLUME);
    }

    @Override
    public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext ctx) {
        Hand hand = ctx.getHand();
        World world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        if (!world.isClientSide) {
            BlockState state = world.getBlockState(pos);
            boolean didWork = stack.getCapability(PNCCapabilities.AIR_HANDLER_ITEM_CAPABILITY).map(h -> {
                float pressure = h.getPressure();
                IPneumaticWrenchable wrenchable = IPneumaticWrenchable.forBlock(state.getBlock());
                if (wrenchable != null && pressure > 0.1f) {
                    if (wrenchable.onWrenched(world, ctx.getPlayer(), pos, ctx.getClickedFace(), hand)) {
                        if (ctx.getPlayer() != null && !ctx.getPlayer().isCreative()) {
                            h.addAir(-PneumaticValues.USAGE_PNEUMATIC_WRENCH);
                        }
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    // rotating normal blocks doesn't use pressure
                    BlockState rotated = state.rotate(Rotation.CLOCKWISE_90);
                    if (rotated != state) {
                        world.setBlockAndUpdate(pos, rotated);
                        return true;
                    } else {
                        return false;
                    }
                }
            }).orElse(false);
            if (didWork) playWrenchSound(world, pos);
            return didWork ? ActionResultType.SUCCESS : ActionResultType.PASS;
        } else {
            // client-side: prevent GUI's opening etc.
            return ActionResultType.SUCCESS;
        }
    }

    private void playWrenchSound(World world, BlockPos pos) {
        NetworkHandler.sendToAllTracking(new PacketPlaySound(ModSounds.PNEUMATIC_WRENCH.get(), SoundCategory.PLAYERS, pos, 1.0F, 1.0F, true), world, pos);
    }

    @Override
    public ActionResultType interactLivingEntity(ItemStack iStack, PlayerEntity player, LivingEntity target, Hand hand) {
        if (player.level.isClientSide) {
            return ActionResultType.SUCCESS;
        } else if (target.isAlive() && target instanceof IPneumaticWrenchable) {
            return iStack.getCapability(PNCCapabilities.AIR_HANDLER_ITEM_CAPABILITY).map(h -> {
                if (!player.isCreative() && h.getAir() < PneumaticValues.USAGE_PNEUMATIC_WRENCH) {
                    return ActionResultType.FAIL;
                }
                if (((IPneumaticWrenchable) target).onWrenched(target.level, player, null, null, hand)) {
                    if (!player.isCreative()) {
                        h.addAir(-PneumaticValues.USAGE_PNEUMATIC_WRENCH);
                    }
                    playWrenchSound(target.level, target.blockPosition());
                    return ActionResultType.SUCCESS;
                } else {
                    return ActionResultType.PASS;
                }
            }).orElseThrow(RuntimeException::new);
        }
        return ActionResultType.PASS;
    }

    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        if (entityIn instanceof ServerPlayerEntity && stack.getItem() instanceof IPressurizableItem
                && ((IPressurizableItem) stack.getItem()).getPressure(stack) >= 3f) {
            AdvancementTriggers.CHARGED_WRENCH.trigger((ServerPlayerEntity) entityIn);
        }
    }
}

package me.desht.pneumaticcraft.common.thirdparty.theoneprobe;

import mcjty.theoneprobe.api.*;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.lib.Names;
import me.desht.pneumaticcraft.api.misc.IPneumaticCraftProbeable;
import me.desht.pneumaticcraft.api.semiblock.IDirectionalSemiblock;
import me.desht.pneumaticcraft.api.semiblock.ISemiBlock;
import me.desht.pneumaticcraft.common.PneumaticCraftTags;
import me.desht.pneumaticcraft.common.semiblock.SemiblockTracker;
import me.desht.pneumaticcraft.common.thirdparty.ModNameCache;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Log;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class TOPInit implements Function<ITheOneProbe, Void> {
    private static final TextFormatting COLOR = TextFormatting.GRAY;
    static int elementPressure;

    @Override
    public Void apply(ITheOneProbe theOneProbe) {
        Log.info("Enabled support for The One Probe");

        elementPressure = theOneProbe.registerElementFactory(ElementPressure::new);

        theOneProbe.registerProvider(new IProbeInfoProvider() {
            @Override
            public String getID() {
                return Names.MOD_ID + ":default";
            }

            @Override
            public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, PlayerEntity player, World world, BlockState blockState, IProbeHitData data) {
                if (blockState.getBlock() instanceof IPneumaticCraftProbeable || blockState.is(PneumaticCraftTags.Blocks.PROBE_TARGET)) {
                    TOPInfoProvider.handleBlock(mode, probeInfo, player, world, blockState, data);
                }
                SemiblockTracker.getInstance().getAllSemiblocks(world, data.getPos(), data.getSideHit())
                        .filter(sb -> !(sb instanceof IDirectionalSemiblock) || ((IDirectionalSemiblock) sb).getSide() == data.getSideHit())
                        .forEach(sb -> TOPInfoProvider.handleSemiblock(player, mode, probeInfo, sb));
            }
        });

        theOneProbe.registerEntityProvider(new IProbeInfoEntityProvider() {
            @Override
            public String getID() {
                return Names.MOD_ID + ":entity";
            }

            @Override
            public void addProbeEntityInfo(ProbeMode mode, IProbeInfo probeInfo, PlayerEntity player, World world, Entity entity, IProbeHitEntityData data) {
                if (entity instanceof ISemiBlock) {
                    List<ITextComponent> tip = new ArrayList<>();
                    CompoundNBT tag = ((ISemiBlock) entity).serializeNBT(new CompoundNBT());
                    ((ISemiBlock) entity).addTooltip(tip, player, tag, player.isShiftKeyDown());
                    tip.forEach(probeInfo::text);
                    BlockPos pos = ((ISemiBlock) entity).getBlockPos();
                    BlockState state = world.getBlockState(pos);
                    if (!state.isAir(world, pos)) {
                        IProbeInfo h = probeInfo.horizontal();
                        h.item(new ItemStack(state.getBlock()));
                        IProbeInfo v = h.vertical();
                        ITextComponent text = new TranslationTextComponent(state.getBlock().getDescriptionId());
                        v.text(text.copy().withStyle(TextFormatting.YELLOW));
                        v.text(new StringTextComponent(TextFormatting.BLUE.toString() + TextFormatting.ITALIC + ModNameCache.getModName(state.getBlock())));
                    }
                }
                entity.getCapability(PNCCapabilities.AIR_HANDLER_CAPABILITY).ifPresent(h -> {
                    String p = PneumaticCraftUtils.roundNumberTo(h.getPressure(), 1);
                    probeInfo.text(xlate("pneumaticcraft.gui.tooltip.pressure", p).withStyle(COLOR));
                });
                entity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
                        .ifPresent(h -> TOPInfoProvider.handleFluidTanks(mode, probeInfo, h));
            }
        });
        return null;
    }
}

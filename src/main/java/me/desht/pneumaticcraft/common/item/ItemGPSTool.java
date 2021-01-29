package me.desht.pneumaticcraft.common.item;

import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.item.IPositionProvider;
import me.desht.pneumaticcraft.client.gui.GuiGPSTool;
import me.desht.pneumaticcraft.client.util.ClientUtils;
import me.desht.pneumaticcraft.common.core.ModItems;
import me.desht.pneumaticcraft.common.core.ModSounds;
import me.desht.pneumaticcraft.common.util.NBTUtils;
import me.desht.pneumaticcraft.common.variables.GlobalVariableManager;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.*;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class ItemGPSTool extends Item implements IPositionProvider {
    public ItemGPSTool() {
        super(ModItems.defaultProps());
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext ctx) {
        BlockPos pos = ctx.getPos();
        setGPSLocation(ctx.getPlayer().getHeldItem(ctx.getHand()), pos);
        if (!ctx.getWorld().isRemote)
            ctx.getPlayer().sendStatusMessage(new TranslationTextComponent("pneumaticcraft.message.gps_tool.targetSet" ,pos.getX(), pos.getY(), pos.getZ()).mergeStyle(TextFormatting.GREEN), false);
        ctx.getPlayer().playSound(ModSounds.CHIRP.get(), 1.0f, 1.5f);
        return ActionResultType.SUCCESS; // we don't want to use the item.
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack stack = playerIn.getHeldItem(handIn);
        if (worldIn.isRemote) {
            GuiGPSTool.showGUI(stack, handIn, getGPSLocation(worldIn, stack));
        }
        return ActionResult.resultSuccess(stack);
    }

    @Override
    public void addInformation(ItemStack stack, World worldIn, List<ITextComponent> infoList, ITooltipFlag par4) {
        super.addInformation(stack, worldIn, infoList, par4);

        ClientUtils.addGuiContextSensitiveTooltip(stack, infoList);
        BlockPos pos = getGPSLocation(stack);
        if (pos != null) {
            ITextComponent translated = new TranslationTextComponent(worldIn.getBlockState(pos).getBlock().getTranslationKey());
            IFormattableTextComponent blockName = worldIn.getChunkProvider().isChunkLoaded(new ChunkPos(pos)) ?
                    new StringTextComponent(" (").append(translated).appendString(")") :
                    StringTextComponent.EMPTY.copyRaw();
            String str = String.format("[%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ());
            infoList.add(new StringTextComponent(str).mergeStyle(TextFormatting.YELLOW).append(blockName.mergeStyle(TextFormatting.GREEN)));
        }
        String varName = getVariable(stack);
        if (!varName.isEmpty()) {
            infoList.add(xlate("pneumaticcraft.gui.tooltip.gpsTool.variable", varName));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean heldItem) {
        String var = getVariable(stack);
        if (!var.equals("") && !world.isRemote) {
            BlockPos pos = GlobalVariableManager.getInstance().getPos(var);
            setGPSLocation(stack, pos);
        }
    }

    public static BlockPos getGPSLocation(ItemStack stack) {
        return getGPSLocation(null, stack);
    }

    public static BlockPos getGPSLocation(World world, ItemStack gpsTool) {
        CompoundNBT compound = gpsTool.getTag();
        if (compound != null) {
            String var = getVariable(gpsTool);
            if (!var.isEmpty() && world != null && !world.isRemote) {
                BlockPos pos = GlobalVariableManager.getInstance().getPos(var);
                setGPSLocation(gpsTool, pos);
            }
            BlockPos pos = net.minecraft.nbt.NBTUtil.readBlockPos(compound.getCompound("Pos"));
            return pos.equals(BlockPos.ZERO) ? null : pos;
        } else {
            return null;
        }
    }

    public static void setGPSLocation(ItemStack gpsTool, BlockPos pos) {
        gpsTool.getOrCreateTag().put("Pos", net.minecraft.nbt.NBTUtil.writeBlockPos(pos));
        String var = getVariable(gpsTool);
        if (!var.isEmpty()) GlobalVariableManager.getInstance().set(var, pos);
    }

    public static void setVariable(ItemStack gpsTool, String variable) {
        NBTUtils.setString(gpsTool, "variable", variable);
    }

    public static String getVariable(ItemStack gpsTool) {
        return gpsTool.hasTag() ? gpsTool.getTag().getString("variable") : "";
    }

    @Override
    public List<BlockPos> getStoredPositions(World world, @Nonnull ItemStack stack) {
        return Collections.singletonList(getGPSLocation(world, stack));
    }

    @Override
    public int getRenderColor(int index) {
        return 0x90FFFF00;
    }

    @Override
    public void syncVariables(ServerPlayerEntity player, ItemStack stack) {
        String varName = getVariable(stack);
        if (!varName.isEmpty()) PneumaticRegistry.getInstance().syncGlobalVariable(player, varName);
    }
}

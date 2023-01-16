/*
 * This file is part of pnc-repressurized.
 *
 *     pnc-repressurized is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     pnc-repressurized is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with pnc-repressurized.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.desht.pneumaticcraft.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.lib.Names;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketSetGlobalVariable;
import me.desht.pneumaticcraft.common.util.GlobalPosHelper;
import me.desht.pneumaticcraft.common.util.IOHelper;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.common.variables.GlobalVariableHelper;
import me.desht.pneumaticcraft.common.variables.GlobalVariableManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.EntityArgument.getPlayer;
import static net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos;

public class ModCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("pncr")
                .then(literal("dump_nbt")
                        .requires(cs -> cs.hasPermission(2))
                        .executes(ModCommands::dumpNBT)
                )
                .then(literal("global_var")
                        .then(literal("get")
                                .then(argument("varname", new VarnameType())
                                        .executes(c -> getGlobalVar(c, StringArgumentType.getString(c,"varname")))
                                )
                        )
                        .then(literal("set")
                                .then(argument("varname", new VarnameType())
                                        .then(argument("pos", BlockPosArgument.blockPos())
                                                .executes(c -> setGlobalVar(c, StringArgumentType.getString(c,"varname"), Either.left(BlockPosArgument.getLoadedBlockPos(c, "pos"))))
                                        )
                                        .then(argument("item", ItemArgument.item())
                                                .executes(c -> setGlobalVar(c, StringArgumentType.getString(c,"varname"), Either.right(ItemArgument.getItem(c, "item"))))
                                        )
                                )
                        )
                        .then(literal("delete")
                                .then(argument("varname", new VarnameType())
                                        .executes(c -> delGlobalVar(c, StringArgumentType.getString(c,"varname")))
                                )
                        )
                        .then(literal("list")
                                .executes(ModCommands::listGlobalVars)
                        )
                )
                .then(literal("amadrone_deliver")
                        .requires(cs -> cs.hasPermission(2))
                        .then(argument("toPos", BlockPosArgument.blockPos())
                                .then(argument("fromPos", BlockPosArgument.blockPos())
                                        .executes(ctx -> amadroneDeliver(ctx.getSource(), getLoadedBlockPos(ctx, "toPos"), getLoadedBlockPos(ctx, "fromPos")))
                                )
                        )
                        .then(argument("player", EntityArgument.player())
                                .then(argument("fromPos", BlockPosArgument.blockPos())
                                        .executes(ctx -> amadroneDeliver(ctx.getSource(), getPlayer(ctx, "player").blockPosition(), getLoadedBlockPos(ctx, "fromPos")))
                                )
                        )
                )
        );
    }

    private static int dumpNBT(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (source.getEntity() instanceof Player) {
            ItemStack held = ((Player) source.getEntity()).getMainHandItem();
            if (held.getTag() == null) {
                source.sendFailure(new TextComponent("No NBT"));
                return 0;
            } else if (held.getTag().isEmpty()) {
                source.sendFailure(new TextComponent("Empty NBT"));
                return 0;
            }
            source.sendSuccess(new TextComponent(held.getTag().toString()), false);
            return 1;
        }
        return 0;
    }

    private static int amadroneDeliver(CommandSourceStack source, BlockPos toPos, BlockPos fromPos) {
        BlockEntity te = source.getLevel().getBlockEntity(fromPos);

        int status = IOHelper.getInventoryForTE(te).map(inv -> {
            List<ItemStack> deliveredStacks = new ArrayList<>();
            for (int i = 0; i < inv.getSlots() && deliveredStacks.size() < 36; i++) {
                if (!inv.getStackInSlot(i).isEmpty()) deliveredStacks.add(inv.getStackInSlot(i));
            }
            if (deliveredStacks.size() > 0) {
                GlobalPos gPos = GlobalPosHelper.makeGlobalPos(source.getLevel(), toPos);
                PneumaticRegistry.getInstance().getDroneRegistry().deliverItemsAmazonStyle(gPos, deliveredStacks.toArray(new ItemStack[0]));
                source.sendSuccess(xlate("pneumaticcraft.command.deliverAmazon.success", PneumaticCraftUtils.posToString(fromPos), PneumaticCraftUtils.posToString(toPos)), false);
                return 1;
            } else {
                source.sendFailure(xlate("pneumaticcraft.command.deliverAmazon.noItems", PneumaticCraftUtils.posToString(fromPos)));
                return 0;
            }
        }).orElse(-1);

        if (status == -1) source.sendFailure(xlate("pneumaticcraft.command.deliverAmazon.noInventory", PneumaticCraftUtils.posToString(fromPos)));
        return status;
    }

    private static int listGlobalVars(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Player playerEntity = source.getEntity() instanceof Player ? (Player) source.getEntity() : null;
        UUID id = playerEntity == null ? null : playerEntity.getUUID();
        Collection<String> varNames = GlobalVariableManager.getInstance().getAllActiveVariableNames(playerEntity);
        source.sendSuccess(new TextComponent(varNames.size() + " vars").withStyle(ChatFormatting.GREEN, ChatFormatting.UNDERLINE), false);
        varNames.stream().sorted().forEach(var -> {
            BlockPos pos = GlobalVariableHelper.getPos(id, var);
            ItemStack stack = GlobalVariableHelper.getStack(id, var);
            String val = PneumaticCraftUtils.posToString(pos);
            if (!stack.isEmpty()) val += " / " + stack.getItem().getRegistryName();
            source.sendSuccess(new TextComponent(var).append(" = [").append(val).append("]"), false);
        });
        return 1;
    }

    private static int getGlobalVar(CommandContext<CommandSourceStack> ctx, String varName) {
        CommandSourceStack source = ctx.getSource();
        if (!GlobalVariableHelper.hasPrefix(varName)) {
            source.sendSuccess(xlate("pneumaticcraft.command.globalVariable.prefixReminder", varName).withStyle(ChatFormatting.GOLD), false);
            varName = "#" + varName;
        }
        UUID id = varName.startsWith("%") || !(ctx.getSource().getEntity() instanceof Player player) ? null : player.getUUID();
        BlockPos pos = GlobalVariableHelper.getPos(id, varName);
        ItemStack stack = GlobalVariableHelper.getStack(id, varName);
        String val = PneumaticCraftUtils.posToString(pos);
        if (!stack.isEmpty()) val += " / " + stack.getItem().getRegistryName();
        if (pos == null && stack.isEmpty()) {
            source.sendFailure(xlate("pneumaticcraft.command.globalVariable.missing", varName));
        } else {
            source.sendSuccess(xlate("pneumaticcraft.command.globalVariable.output", varName, val), false);
        }

        return 1;
    }

    private static int setGlobalVar(CommandContext<CommandSourceStack> ctx, String varName, Either<BlockPos, ItemInput> posOrItem) {
        CommandSourceStack source = ctx.getSource();

        if (!GlobalVariableHelper.hasPrefix(varName)) {
            source.sendSuccess(xlate("pneumaticcraft.command.globalVariable.prefixReminder", varName).withStyle(ChatFormatting.GOLD), false);
            varName = "#" + varName;
        }

        try {
            UUID id = varName.startsWith("%") ? null : ctx.getSource().getPlayerOrException().getUUID();
            final String v = varName;
            posOrItem.ifLeft(pos -> {
                GlobalVariableHelper.setPos(id, v, pos);
                source.sendSuccess(xlate("pneumaticcraft.command.globalVariable.output", v, PneumaticCraftUtils.posToString(pos)), true);
            }).ifRight(item -> {
                ItemStack stack = new ItemStack(item.getItem());
                GlobalVariableHelper.setStack(id, v, stack);
                source.sendSuccess(xlate("pneumaticcraft.command.globalVariable.output", v, stack.getItem().getRegistryName()), true);
            });
        } catch (CommandSyntaxException e) {
            source.sendFailure(new TextComponent("Player-globals require player context!"));
        }

        return 1;
    }

    private static int delGlobalVar(CommandContext<CommandSourceStack> ctx, String varName) {
        CommandSourceStack source = ctx.getSource();
        if (!GlobalVariableHelper.hasPrefix(varName)) {
            source.sendSuccess(xlate("pneumaticcraft.command.globalVariable.prefixReminder", varName).withStyle(ChatFormatting.GOLD), false);
            varName = "#" + varName;
        }

        try {
            UUID id = varName.startsWith("%") ? null : ctx.getSource().getPlayerOrException().getUUID();
            if (GlobalVariableHelper.getPos(id, varName) == null && GlobalVariableHelper.getStack(id, varName).isEmpty()) {
                source.sendFailure(xlate("pneumaticcraft.command.globalVariable.missing", varName));
            } else {
                GlobalVariableHelper.setPos(id, varName, null);
                GlobalVariableHelper.setStack(id, varName, ItemStack.EMPTY);
                // global var deletions need to get sync'd to players; syncing normally happens when remote/gps tool/etc GUI's
                // are opened, but deleted vars won't get sync'd there, so could wrongly hang around on the client
                if (id != null) {
                    PneumaticRegistry.getInstance().getMiscHelpers().syncGlobalVariable(ctx.getSource().getPlayerOrException(), varName);
                } else {
                    NetworkHandler.sendToAll(new PacketSetGlobalVariable(varName, (BlockPos) null));
                    NetworkHandler.sendToAll(new PacketSetGlobalVariable(varName, ItemStack.EMPTY));
                }
                source.sendSuccess(xlate("pneumaticcraft.command.globalVariable.delete", varName), true);
            }
        } catch (CommandSyntaxException e) {
            source.sendFailure(new TextComponent("Player-globals require player context!"));
        }
        return 1;
    }

    public static void postInit() {
        ArgumentTypes.register(Names.MOD_ID + ":varname_type", ModCommands.VarnameType.class,
                new EmptyArgumentSerializer<>(ModCommands.VarnameType::new));
    }

    private static class VarnameType implements ArgumentType<String> {
        @Override
        public String parse(StringReader reader) throws CommandSyntaxException {
            int start = reader.getCursor();
            if (reader.peek() == '#' || reader.peek() == '%') reader.skip();
            while (reader.canRead() && (StringUtils.isAlphanumeric(String.valueOf(reader.peek())) || reader.peek() == '_')) {
                reader.skip();
            }
            return reader.getString().substring(start, reader.getCursor());
        }
    }
}

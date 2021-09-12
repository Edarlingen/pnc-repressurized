package me.desht.pneumaticcraft.client.gui;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import me.desht.pneumaticcraft.client.gui.widget.WidgetAnimatedStat;
import me.desht.pneumaticcraft.client.gui.widget.WidgetButtonExtended;
import me.desht.pneumaticcraft.client.render.RenderHackSimulation;
import me.desht.pneumaticcraft.client.util.ClientUtils;
import me.desht.pneumaticcraft.client.util.GuiUtils;
import me.desht.pneumaticcraft.client.util.PointXY;
import me.desht.pneumaticcraft.common.core.ModBlocks;
import me.desht.pneumaticcraft.common.core.ModItems;
import me.desht.pneumaticcraft.common.hacking.secstation.HackSimulation;
import me.desht.pneumaticcraft.common.hacking.secstation.ISimulationController.HackingSide;
import me.desht.pneumaticcraft.common.inventory.ContainerSecurityStationHacking;
import me.desht.pneumaticcraft.common.item.ItemNetworkComponent.NetworkComponentType;
import me.desht.pneumaticcraft.common.network.NetworkHandler;
import me.desht.pneumaticcraft.common.network.PacketGuiButton;
import me.desht.pneumaticcraft.common.tileentity.TileEntitySecurityStation;
import me.desht.pneumaticcraft.common.util.PneumaticCraftUtils;
import me.desht.pneumaticcraft.lib.Textures;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.List;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class GuiSecurityStationHacking extends GuiPneumaticContainerBase<ContainerSecurityStationHacking, TileEntitySecurityStation> {
    private WidgetAnimatedStat statusStat;

    private RenderHackSimulation hackRenderer;
    private HackSimulation bgSimulation;

    private int stopWorms = 0;
    private int nukeViruses = 0;

    private final ItemStack stopWorm = new ItemStack(ModItems.STOP_WORM.get());
    private final ItemStack nukeVirus = new ItemStack(ModItems.NUKE_VIRUS.get());
    private WidgetButtonExtended nukeVirusButton;
    private WidgetButtonExtended stopWormButton;

    public GuiSecurityStationHacking(ContainerSecurityStationHacking container, PlayerInventory inv, ITextComponent displayString) {
        super(container, inv, displayString);

        imageHeight = 238;
    }

    @Override
    public void init() {
        super.init();

        statusStat = addAnimatedStat(xlate("pneumaticcraft.gui.securityStation.status"), new ItemStack(ModBlocks.SECURITY_STATION.get()), 0xFFFFAA00, false);

        addInfoTab(GuiUtils.xlateAndSplit("pneumaticcraft.gui.tab.info.security_station.hacking"));
        addAnimatedStat(xlate(ModItems.NUKE_VIRUS.get().getDescriptionId()), new ItemStack(ModItems.NUKE_VIRUS.get()), 0xFF18c9e8, false)
                .setText(xlate("pneumaticcraft.gui.tab.info.security_station.nukeVirus"));
        addAnimatedStat(xlate(ModItems.STOP_WORM.get().getDescriptionId()), new ItemStack(ModItems.STOP_WORM.get()), 0xFFc13232, false)
                .setText(xlate("pneumaticcraft.gui.tab.info.security_station.stopWorm"));

        addButton(nukeVirusButton = new WidgetButtonExtended(leftPos + 152, topPos + 95, 18, 18, "")
                .setRenderStacks(nukeVirus));
        addButton(stopWormButton = new WidgetButtonExtended(leftPos + 152, topPos + 143, 18, 18, "", b -> {
            if (!te.getSimulationController().getSimulation(HackingSide.AI).isStopWormed()) {
                PneumaticCraftUtils.consumeInventoryItem(ClientUtils.getClientPlayer().inventory, ModItems.STOP_WORM.get());
                ClientUtils.getClientPlayer().playSound(SoundEvents.SLIME_BLOCK_BREAK, 1f, 1f);
            }
        })).withTag("stop_worm").setRenderStacks(stopWorm);

        initConnectionRendering();
    }

    @Override
    protected boolean shouldAddProblemTab() {
        return false;
    }

    private void initConnectionRendering() {
        hackRenderer = new RenderHackSimulation(leftPos + 16, topPos + 30, ContainerSecurityStationHacking.NODE_SPACING);
        bgSimulation = HackSimulation.dummySimulation();
        bgSimulation.wakeUp();
        for (int i = 0; i < te.getPrimaryInventory().getSlots(); i++) {
            bgSimulation.addNode(i, te.getPrimaryInventory().getStackInSlot(i));
        }
    }

    public static void addExtraHackInfoStatic(List<ITextComponent> curInfo) {
        if (Minecraft.getInstance().screen instanceof GuiSecurityStationHacking) {
            ((GuiSecurityStationHacking) Minecraft.getInstance().screen).addExtraHackInfo(curInfo);
        }
    }

    @Override
    protected ResourceLocation getGuiTexture() {
        return Textures.GUI_HACKING;
    }

    @Override
    protected boolean shouldAddInfoTab() {
        return false;
    }

    @Override
    protected boolean shouldAddUpgradeTab() {
        return false;
    }

    @Override
    protected boolean shouldAddRedstoneTab() {
        return false;
    }

    @Override
    protected PointXY getInvNameOffset() {
        return null;
    }

    @Override
    protected PointXY getInvTextOffset() {
        return null;
    }

    @Override
    protected void renderLabels(MatrixStack matrixStack, int x, int y) {
        super.renderLabels(matrixStack, x, y);

        if (te.getSimulationController() != null) {
            HackSimulation aiSim = te.getSimulationController().getSimulation(HackingSide.AI);
            HackSimulation playerSim = te.getSimulationController().getSimulation(HackingSide.PLAYER);
            if (aiSim.isAwake()) {
                drawCenteredString(matrixStack, font, xlate("pneumaticcraft.gui.tooltip.hacking.aiTracing").withStyle(TextFormatting.RED), imageWidth / 2, 7, 0xFFFFFF);
            } else {
                drawCenteredString(matrixStack, font, xlate("pneumaticcraft.gui.tooltip.hacking.detectionChance", te.getDetectionChance()).withStyle(TextFormatting.GOLD), imageWidth / 2, 7, 0xFFFFFF);
            }

            if (aiSim.isHackComplete()) {
                ImmutableList.Builder<ITextComponent> builder = ImmutableList.builder();
                builder.add(xlate("pneumaticcraft.message.securityStation.hackFailed.1").withStyle(TextFormatting.RED));
                if (!te.getSimulationController().isJustTesting()) {
                    builder.add(StringTextComponent.EMPTY);
                    builder.add(xlate("pneumaticcraft.message.securityStation.hackFailed.2").withStyle(TextFormatting.RED));
                }
                GuiUtils.showPopupHelpScreen(matrixStack, this, font, builder.build());
            } else if (playerSim.isHackComplete()) {
                ImmutableList.Builder<ITextComponent> builder = ImmutableList.builder();
                builder.add(xlate("pneumaticcraft.message.securityStation.hackSucceeded.1").withStyle(TextFormatting.GREEN));
                if (!te.getSimulationController().isJustTesting()) {
                    builder.add(StringTextComponent.EMPTY);
                    builder.add(xlate("pneumaticcraft.message.securityStation.hackSucceeded.2").withStyle(TextFormatting.GREEN));
                }
                GuiUtils.showPopupHelpScreen(matrixStack, this, font, builder.build());
            }
        }
        renderConsumables(matrixStack);
    }

    @Override
    protected void renderBg(MatrixStack matrixStack, float partialTicks, int x, int y) {
        super.renderBg(matrixStack, partialTicks, x, y);

        hackRenderer.render(matrixStack, bgSimulation, 0xFF2222FF);
        if (te.getSimulationController() != null) {
            HackSimulation aiSim = te.getSimulationController().getSimulation(HackingSide.AI);
            if (!aiSim.isStopWormed() || (te.getLevel().getGameTime() & 0xf) < 8) {
                hackRenderer.render(matrixStack, te.getSimulationController().getSimulation(HackingSide.AI), 0xFFFF0000);
            }
            hackRenderer.render(matrixStack, te.getSimulationController().getSimulation(HackingSide.PLAYER), 0xFF00FF00);
        }
    }

    private void renderConsumables(MatrixStack matrixStack) {
        font.draw(matrixStack, PneumaticCraftUtils.convertAmountToString(nukeViruses), 158, 112, nukeViruses == 0 ? 0xFFFF6060: 0xFFFFFFFF);
        font.draw(matrixStack, PneumaticCraftUtils.convertAmountToString(stopWorms), 158, 160, stopWorms == 0 ? 0xFFFF6060: 0xFFFFFFFF);
    }

    @Override
    public void tick() {
        super.tick();

        stopWorms = 0;
        nukeViruses = 0;
        for (ItemStack stack : inventory.items) {
            if (stack.getItem() == ModItems.STOP_WORM.get()) stopWorms += stack.getCount();
            if (stack.getItem() == ModItems.NUKE_VIRUS.get()) nukeViruses += stack.getCount();
        }

        bgSimulation.tick();

        statusStat.setText(getStatusText());

        HackSimulation playerSim = te.getSimulationController() == null ? null : te.getSimulationController().getSimulation(HackingSide.PLAYER);
        HackSimulation aiSim = te.getSimulationController() == null ? null : te.getSimulationController().getSimulation(HackingSide.AI);

        if (aiSim != null && aiSim.isAwake()) {
            stopWormButton.active = stopWorms > 0;
            stopWormButton.setTooltipText(stopWorms > 0 ?
                    xlate("pneumaticcraft.gui.securityStation.stopWorm") :
                    xlate("pneumaticcraft.gui.securityStation.stopWorm.none").withStyle(TextFormatting.GOLD)
            );
        } else {
            stopWormButton.active = false;
            stopWormButton.setTooltipText(xlate("pneumaticcraft.gui.securityStation.stopWorm.notTracing").withStyle(TextFormatting.GOLD));
        }

        if (playerSim != null) {
            nukeVirusButton.active = hasNukeViruses() && playerSim.isNukeVirusReady();
            if (playerSim.isNukeVirusReady()) {
                nukeVirusButton.setTooltipText(hasNukeViruses() ?
                        xlate("pneumaticcraft.gui.securityStation.nukeVirus") :
                        xlate("pneumaticcraft.gui.securityStation.nukeVirus.none").withStyle(TextFormatting.GOLD)
                );
            } else {
                nukeVirusButton.setTooltipText(xlate("pneumaticcraft.gui.securityStation.nukeVirus.coolDown").withStyle(TextFormatting.GOLD));
            }
        }
    }

    private List<ITextComponent> getStatusText() {
        List<ITextComponent> text = new ArrayList<>();
        text.add(xlate("pneumaticcraft.gui.tab.status.securityStation.securityLevel").withStyle(TextFormatting.WHITE));
        text.add(new StringTextComponent("L" + te.getSecurityLevel()).withStyle(TextFormatting.BLACK));
        text.add(xlate("pneumaticcraft.gui.tab.status.securityStation.securityRange").withStyle(TextFormatting.WHITE));
        text.add(new StringTextComponent((te.getRange() * 2 + 1) + "m²").withStyle(TextFormatting.BLACK));
        return text;
    }

    @Override
    protected void slotClicked(Slot slotIn, int slotId, int mouseButton, ClickType type) {
        // slotIn *can* be null here
        //noinspection ConstantConditions
        if (slotIn != null && slotIn.hasItem() && te.getSimulationController() != null) {
            switch (mouseButton) {
                case 0:
                    tryHackSlot(slotId);
                    break;
                case 1:
                    tryFortifySlot(slotId);
                    break;
                case 2:
                    tryNukeVirus(slotId);
                    break;
            }
        } else {
            super.slotClicked(slotIn, slotId, mouseButton, type);
        }
    }

    private void tryFortifySlot(int slotId) {
        HackSimulation playerSim = te.getSimulationController().getSimulation(HackingSide.PLAYER);
        HackSimulation.Node node = playerSim.getNodeAt(slotId);
        if (node.isHacked() && !node.isFortified() && node.getFortification() == 0) {
            playerSim.fortify(slotId);
            NetworkHandler.sendToServer(new PacketGuiButton("fortify:" + slotId));
        }
    }

    private void tryHackSlot(int slotId) {
        HackSimulation playerSim = te.getSimulationController().getSimulation(HackingSide.PLAYER);
        HackSimulation.Node node = playerSim.getNodeAt(slotId);
        if (!node.isHacked() && playerSim.getHackedNeighbour(slotId) >= 0) {
            playerSim.startHack(slotId);
            NetworkHandler.sendToServer(new PacketGuiButton("hack:" + slotId));
        }
    }

    private void tryNukeVirus(int slotId) {
        if (hasNukeViruses() && te.getSimulationController() != null) {
            HackSimulation playerSim = te.getSimulationController().getSimulation(HackingSide.PLAYER);
            HackSimulation.Node node = playerSim.getNodeAt(slotId);
            if (!node.isHacked() && playerSim.getHackedNeighbour(slotId) >= 0) {
                // node must have a hacked neighbour for this to work
                if (playerSim.initiateNukeVirus(slotId)) {
                    NetworkHandler.sendToServer(new PacketGuiButton("nuke:" + slotId));
                    PneumaticCraftUtils.consumeInventoryItem(minecraft.player.inventory, ModItems.NUKE_VIRUS.get());
                }
            }
        }
    }

    public void addExtraHackInfo(List<ITextComponent> toolTip) {
        if (hoveredSlot != null && te.getSimulationController() != null) {
            HackSimulation playerSim = te.getSimulationController().getSimulation(HackingSide.PLAYER);
            HackSimulation.Node node = playerSim.getNodeAt(hoveredSlot.index);
            if (node != null) {
                if (node.isHacked()) {
                    if (node.getFortification() == 0) {
                        toolTip.add(xlate("pneumaticcraft.gui.tooltip.hacking.rightClickFortify").withStyle(TextFormatting.DARK_AQUA));
                    } else if (node.getFortificationProgress() < 1f) {
                        toolTip.add(xlate("pneumaticcraft.gui.tooltip.hacking.fortifyProgress", (int)(node.getFortificationProgress() * 100)).withStyle(TextFormatting.DARK_AQUA));
                    } else {
                        toolTip.add(xlate("pneumaticcraft.gui.tooltip.hacking.fortified").withStyle(TextFormatting.AQUA));
                    }
                } else {
                    if (playerSim.getHackedNeighbour(hoveredSlot.index) >= 0) {
                        if (node.getHackProgress() == 0F) {
                            toolTip.add(xlate("pneumaticcraft.gui.tooltip.hacking.leftClickHack").withStyle(TextFormatting.GREEN));
                        } else {
                            toolTip.add(xlate("pneumaticcraft.gui.tooltip.hacking.hackProgress", (int)(node.getHackProgress() * 100)).withStyle(TextFormatting.GREEN));
                        }
                        if (nukeViruses > 0 && playerSim.isNukeVirusReady() && node.getType() == NetworkComponentType.NETWORK_NODE) {
                            toolTip.add(xlate("pneumaticcraft.gui.tooltip.hacking.middleClickNuke").withStyle(TextFormatting.YELLOW));
                        }
                    }
                }
            }
        }
    }

    boolean hasNukeViruses() {
        return nukeViruses > 0;
    }
}

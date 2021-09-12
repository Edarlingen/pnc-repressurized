package me.desht.pneumaticcraft.common.tileentity;

import me.desht.pneumaticcraft.api.lib.NBTKeys;
import me.desht.pneumaticcraft.client.util.ClientUtils;
import me.desht.pneumaticcraft.common.block.BlockAphorismTile;
import me.desht.pneumaticcraft.common.core.ModTileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.BitSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TileEntityAphorismTile extends TileEntityBase {
    public static final String NBT_BORDER_COLOR = "borderColor";
    public static final String NBT_BACKGROUND_COLOR = "backgroundColor";
    private static final String NBT_TEXT_ROTATION = "textRot";
    public static final String NBT_TEXT_LINES = "lines";
    public static final String NBT_MARGIN = "margin";
    private static final String NBT_INVISIBLE = "invisible";

    private static final Pattern ITEM_PAT = Pattern.compile("^\\{item:(\\w+:[a-z0-9_.]+)}$");

    private String[] textLines = new String[]{""};
    private ItemStack[] icons = new ItemStack[]{ItemStack.EMPTY};
    private BitSet rsLines = new BitSet(1);

    public int textRotation;
    private int borderColor = DyeColor.BLUE.getId();
    private int backgroundColor = DyeColor.WHITE.getId();
    private int maxLineWidth = -1;  // cached width for rendering purposes
    private byte marginSize; // 0..9
    private boolean invisible;
    public int currentRedstonePower = 0;
    private long lastPoll = 0L;
    public int cursorX = -1, cursorY = -1; // stored in client TE only to remember last editor cursor pos

    public TileEntityAphorismTile() {
        super(ModTileEntities.APHORISM_TILE.get());
    }

    @Override
    public IItemHandler getPrimaryInventory() {
        return null;
    }

    @Override
    public boolean shouldPreserveStateOnBreak() {
        return true;
    }

    @Override
    public void serializeExtraItemData(CompoundNBT blockEntityTag, boolean preserveState) {
        writeToPacket(blockEntityTag);
    }

    @Override
    public void writeToPacket(CompoundNBT tag) {
        super.writeToPacket(tag);

        CompoundNBT subTag = new CompoundNBT();
        subTag.put(NBT_TEXT_LINES, Arrays.stream(textLines).map(StringNBT::valueOf).collect(Collectors.toCollection(ListNBT::new)));
        subTag.putInt(NBT_TEXT_ROTATION, textRotation);
        subTag.putInt(NBT_BORDER_COLOR, borderColor);
        subTag.putInt(NBT_BACKGROUND_COLOR, backgroundColor);
        subTag.putByte(NBT_MARGIN, marginSize);
        subTag.putBoolean(NBT_INVISIBLE, invisible);
        tag.put(NBTKeys.NBT_EXTRA, subTag);
    }

    @Override
    public void readFromPacket(CompoundNBT tag) {
        super.readFromPacket(tag);

        if (tag.contains(NBTKeys.NBT_EXTRA)) {
            CompoundNBT subTag = tag.getCompound(NBTKeys.NBT_EXTRA);
            ListNBT l = subTag.getList(NBT_TEXT_LINES, Constants.NBT.TAG_STRING);
            if (l.isEmpty()) {
                textLines = new String[] { "" };
            } else {
                textLines = new String[l.size()];
                IntStream.range(0, textLines.length).forEach(i -> textLines[i] = l.getString(i));
            }
            updateLineMetadata();
            textRotation = subTag.getInt(NBT_TEXT_ROTATION);
            if (subTag.contains(NBT_BORDER_COLOR)) {
                borderColor = subTag.getInt(NBT_BORDER_COLOR);
                backgroundColor = subTag.getInt(NBT_BACKGROUND_COLOR);
            } else {
                borderColor = DyeColor.BLUE.getId();
                backgroundColor = DyeColor.WHITE.getId();
            }
            setMarginSize(subTag.getByte(NBT_MARGIN));
            setInvisible(subTag.getBoolean(NBT_INVISIBLE));
            if (level != null) rerenderTileEntity();
        }
    }

    public String[] getTextLines() {
        return textLines;
    }

    public void setTextLines(String[] textLines) {
        setTextLines(textLines, true);
    }

    public void setTextLines(String[] textLines, boolean notifyClient) {
        this.textLines = textLines;
        this.maxLineWidth = -1; // force recalc
        icons = new ItemStack[textLines.length];
        if (level.isClientSide) {
            updateLineMetadata();
        } else {
            // server
            if (notifyClient) sendDescriptionPacket();
        }
    }

    private void updateLineMetadata() {
        icons = new ItemStack[textLines.length];
        rsLines = new BitSet(textLines.length);
        for (int i = 0; i < textLines.length; i++) {
            Matcher m = ITEM_PAT.matcher(textLines[i]);
            if (m.matches()) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(m.group(1)));
                icons[i] = new ItemStack(item);
            } else {
                icons[i] = ItemStack.EMPTY;
                if (textLines[i].contains("{redstone}")) {
                    rsLines.set(i);
                }
            }
        }
    }

    public ItemStack getIconAt(int line) {
        return line >= 0 && line < icons.length ? icons[line] : ItemStack.EMPTY;
    }

    public boolean isRedstoneLine(int line) {
        return (line >= 0 && line < rsLines.size()) && rsLines.get(line);
    }

    public void setBorderColor(int color) {
        this.borderColor = color;
        if (!level.isClientSide) sendDescriptionPacket();
    }

    public int getBorderColor() {
        return borderColor;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
        if (!level.isClientSide) sendDescriptionPacket();
    }

    public byte getMarginSize() {
        return marginSize;
    }

    public void setMarginSize(byte marginSize) {
        this.marginSize = (byte) MathHelper.clamp(marginSize, 0, 9);
        needMaxLineWidthRecalc();
    }

    public boolean isInvisible() {
        return invisible;
    }

    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
        if (level != null) {
            BlockState state = getBlockState();
            if (state.getBlock() instanceof BlockAphorismTile) {
                level.setBlockAndUpdate(worldPosition, getBlockState().setValue(BlockAphorismTile.INVISIBLE, invisible));
            }
        }
    }

    public int getMaxLineWidth(boolean editing) {
        // client only!
        if (maxLineWidth < 0) {
            for (int i = 0; i < textLines.length; i++) {
                String line = textLines[i];
                if (!editing && isRedstoneLine(i)) line = line.replaceAll(Pattern.quote("{redstone}"), Integer.toString(currentRedstonePower));
                int stringWidth = !editing && !getIconAt(i).isEmpty() ? 6 : ClientUtils.getStringWidth(line);
                if (stringWidth > maxLineWidth) {
                    maxLineWidth = stringWidth;
                }
            }
            float mul = 1f + (marginSize + 1) * 0.075f;
            maxLineWidth *= mul;  // multiplier allows for a small margin; looks better
        }
        return maxLineWidth;
    }

    public void needMaxLineWidthRecalc() {
        maxLineWidth = -1;
    }

    public int pollRedstone() {
        if (level.getGameTime() - lastPoll >= 2) {
            Direction d = getRotation();
            int p = level.getSignal(worldPosition.relative(d), d);
            if (p != currentRedstonePower) needMaxLineWidthRecalc();
            currentRedstonePower = p;
            lastPoll = level.getGameTime();
        }
        return currentRedstonePower;
    }

    public Pair<Integer,Integer> getCursorPos() {
        int cx, cy;
        cy = cursorY >= 0 && cursorY < textLines.length ? cursorY : textLines.length - 1;
        cx = cursorX >= 0 && cursorX <= textLines[cy].length() ? cursorX : textLines[cy].length();
        return Pair.of(cx, cy);
    }

    public void setCursorPos(int cursorX, int cursorY) {
        this.cursorX = cursorX;
        this.cursorY = cursorY;
    }
}

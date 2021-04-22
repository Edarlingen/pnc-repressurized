package me.desht.pneumaticcraft.common.progwidgets;

import me.desht.pneumaticcraft.api.drone.IProgWidgetBase;
import me.desht.pneumaticcraft.api.drone.ProgWidgetType;
import me.desht.pneumaticcraft.common.ai.IDroneBase;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.DyeColor;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.List;

public interface IProgWidget extends IProgWidgetBase {
    int getX();

    int getY();

    void setX(int x);

    void setY(int y);

    int getWidth();

    int getHeight();

    ResourceLocation getTexture();

    Pair<Float,Float> getMaxUV();

    int getTextureSize();

    void getTooltip(List<ITextComponent> curTooltip);

    void addWarnings(List<ITextComponent> curInfo, List<IProgWidget> widgets);

    void addErrors(List<ITextComponent> curInfo, List<IProgWidget> widgets);

    /**
     * Can this widget connect to a widget above?
     * @return true if it can, false otherwise
     */
    boolean hasStepInput();

    /**
     * Can another widget be connected below this widget?
     * @return true if it can, false otherwise
     */
    boolean hasStepOutput();

    default boolean freeToUse() { return false; }

    /**
     * Get the AI for this progwidget
     * @param drone the drone
     * @param widget will be 'this' most of the time, but not when controlled externally (e.g. ComputerCraft)
     * @return widget AI
     */
    Goal getWidgetAI(IDroneBase drone, IProgWidget widget);

    /**
     * Get the targeting AI for this progwidget
     * @param drone the drone
     * @param widget Will be 'this' most of the time, but not when controlled externally (e.g. ComputerCraft)
     * @return widget targeting AI
     */
    Goal getWidgetTargetAI(IDroneBase drone, IProgWidget widget);

    /**
     * Set the output widget for this widget, i.e. next in the program.  Called when building the program.
     * @param widget the next widget in the program
     */
    void setOutputWidget(IProgWidget widget);

    /**
     * Get the next widget in the program; the widget attached to the bottom of this one.
     *
     * @return the next widget to run
     */
    IProgWidget getOutputWidget();

    /**
     * Get the next widget in the program, which may or may not be the widget attached to the bottom of this one.
     * This method variant is called when running in a live program, and has access to the drone
     * context and a view of the full program so it can deal with special conditions like jumps etc.
     *
     * @param drone the drone
     * @param allWidgets a list of widgets
     * @return the next widget to run
     */
    IProgWidget getOutputWidget(IDroneBase drone, List<IProgWidget> allWidgets);

    /**
     * For "auxiliary" widgets that are added onto the left or right of another widget, get the type of the widget
     * being connected to.
     *
     * @return the widget being connected to, or null if this widget isn't an "auxiliary" widget.
     */
    ProgWidgetType<?> returnType();

    /**
     * Get the types of the widgets which connect to this widget on the right, in order (top to bottom). Note that the
     * length of the widget array returned by {@link #getConnectedParameters()} will be double the length of this list,
     * since these widgets can be added on both the right (whitelist) and left (blacklist) sides.
     *
     * @return a list of widget types, or an empty list if no widgets can be added to the side of this widget
     */
    @Nonnull
    List<ProgWidgetType<?>> getParameters();

    void setParameter(int index, IProgWidget parm);

    boolean canSetParameter(int index);

    IProgWidget[] getConnectedParameters();

    void setParent(IProgWidget widget);

    IProgWidget getParent();

    ResourceLocation getTypeID();

    default String getTranslationKey() {
        String s = getTypeID().toString().replace(':', '.');
        return "programmingPuzzle." + s + ".name";
    }

    DyeColor getColor();

    /**
     * Is this widget currently available in this world?  i.e. make sure it's not blacklisted by server admin,
     * and any mod dependencies are loaded
     * @return true if the widget is available for use by players
     */
    boolean isAvailable();

    /**
     * At least do <code>tag.putString("name", getTypeID().toString());</code>
     * <p>Note that the base implementation {@link ProgWidget} does this.</p>
     *
     * @param tag NBT tag to write to
     */
    void writeToNBT(CompoundNBT tag);

    void readFromNBT(CompoundNBT tag);

    IProgWidget copy();

    boolean canBeRunByComputers(IDroneBase drone, IProgWidget widget);

    default boolean isDifficultyOK(WidgetDifficulty difficulty) {
        return getDifficulty().isNotMoreDifficult(difficulty);
    }

    WidgetDifficulty getDifficulty();

    ProgWidgetType<?> getType();

    void readFromPacket(PacketBuffer buf);

    void writeToPacket(PacketBuffer buf);

    @Nonnull
    List<ITextComponent> getExtraStringInfo();

    enum WidgetDifficulty {
        EASY("easy", 0),
        MEDIUM("medium", 1),
        ADVANCED("advanced", 2);

        private final String name;
        private final int difficultyLevel;

        WidgetDifficulty(String name, int difficultyLevel) {
            this.name = name;
            this.difficultyLevel = difficultyLevel;
        }

        public String getTranslationKey() {
            return "pneumaticcraft.gui.progWidget.difficulty." + name;
        }

        public String getTooltipTranslationKey() { return "pneumaticcraft.gui.programmer.difficulty." + name + ".tooltip"; }

        public boolean isNotMoreDifficult(WidgetDifficulty other) {
            return this.difficultyLevel <= other.difficultyLevel;
        }
    }

    /**
     * Cast from the API interface to our internal interface.  Should always succeed!
     *
     * @param type type of the progwidget
     * @return the internal non-API progwidget type
     */
    static IProgWidget create(ProgWidgetType<?> type) {
        IProgWidgetBase base = type.create();
        Validate.isTrue(base instanceof IProgWidget);
        return (IProgWidget) base;
    }
}

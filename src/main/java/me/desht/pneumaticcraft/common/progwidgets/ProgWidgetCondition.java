package me.desht.pneumaticcraft.common.progwidgets;

import me.desht.pneumaticcraft.common.ai.DroneAIBlockCondition;
import me.desht.pneumaticcraft.common.ai.IDroneBase;
import me.desht.pneumaticcraft.lib.Log;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.DyeColor;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.List;

import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public abstract class ProgWidgetCondition extends ProgWidgetInventoryBase implements ICondition, IJump {

    private DroneAIBlockCondition evaluator;
    private boolean isAndFunction;
    private ICondition.Operator operator = ICondition.Operator.HIGHER_THAN_EQUALS;

    @Override
    public Goal getWidgetAI(IDroneBase drone, IProgWidget widget) {
        evaluator = getEvaluator(drone, widget);
        return evaluator;
    }

    @Override
    public void addErrors(List<ITextComponent> curInfo, List<IProgWidget> widgets) {
        super.addErrors(curInfo, widgets);
        if (getConnectedParameters()[getParameters().length - 1] == null && getConnectedParameters()[getParameters().length * 2 - 1] == null) {
            curInfo.add(xlate("gui.progWidget.condition.error.noFlowControl"));
        }
    }

    @Override
    public abstract Class<? extends IProgWidget>[] getParameters();

    protected abstract DroneAIBlockCondition getEvaluator(IDroneBase drone, IProgWidget widget);

    @Override
    public IProgWidget getOutputWidget(IDroneBase drone, List<IProgWidget> allWidgets) {
        if (evaluator != null) {
            boolean evaluation = evaluate(drone, this);
            if (evaluation) {
                drone.addDebugEntry("gui.progWidget.condition.evaluatedTrue");
            } else {
                drone.addDebugEntry("gui.progWidget.condition.evaluatedFalse");
            }
            return ProgWidgetJump.jumpToLabel(drone, allWidgets, this, evaluation);
        } else {
            Log.error("Shouldn't be happening! ProgWidgetCondition");
            return super.getOutputWidget(drone, allWidgets);
        }
    }

    @Override
    public boolean evaluate(IDroneBase drone, IProgWidget widget) {
        return evaluator.getResult();
    }

    @Override
    public boolean isAndFunction() {
        return isAndFunction;
    }

    @Override
    public void setAndFunction(boolean isAndFunction) {
        this.isAndFunction = isAndFunction;
    }

    @Override
    public WidgetDifficulty getDifficulty() {
        return WidgetDifficulty.MEDIUM;
    }

    @Override
    public List<String> getPossibleJumpLocations() {
        IProgWidget widget = getConnectedParameters()[getParameters().length - 1];
        IProgWidget widget2 = getConnectedParameters()[getParameters().length * 2 - 1];
        ProgWidgetString textWidget = widget != null ? (ProgWidgetString) widget : null;
        ProgWidgetString textWidget2 = widget2 != null ? (ProgWidgetString) widget2 : null;
        List<String> locations = new ArrayList<>();
        if (textWidget != null) locations.add(textWidget.string);
        if (textWidget2 != null) locations.add(textWidget2.string);
        return locations;
    }

    @Override
    public int getRequiredCount() {
        return getCount();
    }

    @Override
    public void setRequiredCount(int count) {
        setCount(count);
    }

    @Override
    public Operator getOperator() {
        return operator;
    }

    @Override
    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    @Override
    public void writeToNBT(CompoundNBT tag) {
        super.writeToNBT(tag);
        tag.putBoolean("isAndFunction", isAndFunction);
        tag.putByte("operator", (byte) operator.ordinal());
    }

    @Override
    public void readFromNBT(CompoundNBT tag) {
        super.readFromNBT(tag);
        isAndFunction = tag.getBoolean("isAndFunction");
        operator = ICondition.Operator.values()[tag.getByte("operator")];
    }

    @Override
    protected boolean isUsingSides() {
        return false;
    }

    @Override
    public String getExtraStringInfo() {
        String anyAll = I18n.format(isAndFunction() ? "gui.progWidget.condition.all" : "gui.progWidget.condition.any");
        return anyAll + " " + getOperator().toString() + " " + getRequiredCount();
    }

    @Override
    public DyeColor getColor() {
        return DyeColor.CYAN;
    }
}

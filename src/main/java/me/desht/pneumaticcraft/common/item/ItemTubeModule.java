package me.desht.pneumaticcraft.common.item;

import me.desht.pneumaticcraft.common.block.BlockPressureTube;
import me.desht.pneumaticcraft.common.block.tubes.TubeModule;
import me.desht.pneumaticcraft.common.core.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Function;

public class ItemTubeModule extends Item {
    private final Function<ItemTubeModule, TubeModule> moduleFactory;

    public ItemTubeModule(Function<ItemTubeModule, TubeModule> moduleFactory) {
        super(ModItems.defaultProps());
        this.moduleFactory = moduleFactory;
    }

    @Nonnull
    public TubeModule createModule() {
        return moduleFactory.apply(this);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);

        TubeModule module = createModule();
        tooltip.add(new StringTextComponent("In line: " + (module.isInline() ? "Yes" : "No")).withStyle(TextFormatting.DARK_AQUA));
    }

    @Override
    public ActionResultType useOn(ItemUseContext context) {
        if (context.getPlayer() != null && context.getPlayer().isCrouching()) {
            // sneak-click module to attach it to opposite side of tube, if possible
            BlockState state = context.getLevel().getBlockState(context.getClickedPos());
            if (state.getBlock() instanceof BlockPressureTube) {
                BlockRayTraceResult brtr = new BlockRayTraceResult(context.getClickLocation(), context.getClickedFace().getOpposite(), context.getClickedPos(), false);
                return state.use(context.getLevel(), context.getPlayer(), context.getHand(), brtr);
            }
        }
        return super.useOn(context);
    }
}

package me.desht.pneumaticcraft.common.entity.living;

import me.desht.pneumaticcraft.api.item.EnumUpgrade;
import me.desht.pneumaticcraft.common.core.ModEntities;
import me.desht.pneumaticcraft.common.progwidgets.*;
import me.desht.pneumaticcraft.common.util.DroneProgramBuilder;
import me.desht.pneumaticcraft.common.util.IOHelper;
import me.desht.pneumaticcraft.common.util.UpgradableItemUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EntityCollectorDrone extends EntityBasicDrone {
    public EntityCollectorDrone(EntityType<? extends EntityDrone> type, World world) {
        super(type, world);
    }

    public EntityCollectorDrone(World world, PlayerEntity player) {
        super(ModEntities.COLLECTOR_DRONE.get(), world, player);
    }

    @Override
    public boolean addProgram(BlockPos clickPos, Direction facing, BlockPos pos, ItemStack droneStack, List<IProgWidget> widgets) {
        DroneProgramBuilder builder = new DroneProgramBuilder();
        builder.add(new ProgWidgetStart());

        BlockPos invPos = clickPos;

        List<IProgWidget> params = new ArrayList<>();
        int rangeUpgrades = UpgradableItemUtils.getUpgrades(droneStack, EnumUpgrade.RANGE);
        params.add(ProgWidgetArea.fromPosition(pos, 16 + rangeUpgrades * 2));
        LazyOptional<IItemHandler> itemCap = IOHelper.getInventoryForTE(level.getBlockEntity(clickPos), facing);
        if (itemCap.isPresent()) {
            // placed on a chest; filter on the chest's contents, if any
            Set<Item> filtered = getFilteredItems(itemCap);
            if (!filtered.isEmpty()) {
                filtered.forEach(item -> params.add(ProgWidgetItemFilter.withFilter(new ItemStack(item))));
            }
        } else {
            // find a nearby chest to insert to
            invPos = findAdjacentInventory(pos);
        }
        builder.add(new ProgWidgetPickupItem(), params.toArray(new IProgWidget[0]));

        ProgWidgetInventoryExport export = new ProgWidgetInventoryExport();
        boolean[] sides = new boolean[6];
        sides[facing.get3DDataValue()] = true;
        export.setSides(sides);
        builder.add(export, ProgWidgetArea.fromPosition(invPos));

        maybeAddStandbyInstruction(builder, droneStack);

        builder.add(new ProgWidgetWait(), ProgWidgetText.withText("2s"));  // be kind to server

        widgets.addAll(builder.build());

        return true;
    }

    private BlockPos findAdjacentInventory(BlockPos pos) {
        return Arrays.stream(Direction.values())
                .filter(d -> IOHelper.getInventoryForTE(level.getBlockEntity(pos.relative(d)), d.getOpposite()).isPresent())
                .findFirst()
                .map(pos::relative)
                .orElse(pos);
    }

    private Set<Item> getFilteredItems(LazyOptional<IItemHandler> cap) {
        return cap.map(handler -> IntStream.range(0, handler.getSlots())
                .filter(i -> !handler.getStackInSlot(i).isEmpty())
                .mapToObj(i -> handler.getStackInSlot(i).getItem())
                .collect(Collectors.toSet())
        ).orElse(Collections.emptySet());
    }
}

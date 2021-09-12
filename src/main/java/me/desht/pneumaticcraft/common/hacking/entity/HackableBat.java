package me.desht.pneumaticcraft.common.hacking.entity;

import me.desht.pneumaticcraft.api.client.pneumatic_helmet.IHackableEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.Explosion;

import java.util.List;

import static me.desht.pneumaticcraft.api.PneumaticRegistry.RL;
import static me.desht.pneumaticcraft.common.util.PneumaticCraftUtils.xlate;

public class HackableBat implements IHackableEntity {
    @Override
    public ResourceLocation getHackableId() {
        return RL("bat");
    }

    @Override
    public boolean canHack(Entity entity, PlayerEntity player) {
        return entity.getClass() == BatEntity.class;
    }

    @Override
    public void addHackInfo(Entity entity, List<ITextComponent> curInfo, PlayerEntity player) {
        curInfo.add(xlate("pneumaticcraft.armor.hacking.result.kill"));
    }

    @Override
    public void addPostHackInfo(Entity entity, List<ITextComponent> curInfo, PlayerEntity player) {
        curInfo.add(xlate("pneumaticcraft.armor.hacking.finished.killed"));
    }

    @Override
    public int getHackTime(Entity entity, PlayerEntity player) {
        return 60;
    }

    @Override
    public void onHackFinished(Entity entity, PlayerEntity player) {
        if (!entity.level.isClientSide) {
            entity.remove();
            entity.level.explode(null, entity.getX(), entity.getY(), entity.getZ(), 0, Explosion.Mode.NONE);
        }
    }

    @Override
    public boolean afterHackTick(Entity entity) {
        return false;
    }

}

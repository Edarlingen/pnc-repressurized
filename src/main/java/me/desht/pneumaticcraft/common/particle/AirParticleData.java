package me.desht.pneumaticcraft.common.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.desht.pneumaticcraft.common.core.ModParticleTypes;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleType;

import java.util.Calendar;
import java.util.Locale;

public class AirParticleData implements IParticleData {
    public static final AirParticleData NORMAL = new AirParticleData(0.1f);
    public static final AirParticleData DENSE = new AirParticleData(0.3f);

    public static final IDeserializer<AirParticleData> DESERIALIZER = new IDeserializer<AirParticleData>() {
        @Override
        public AirParticleData fromCommand(ParticleType<AirParticleData> particleType, StringReader stringReader) throws CommandSyntaxException {
            stringReader.expect(' ');
            float alpha = stringReader.readFloat();
            return new AirParticleData(alpha);
        }

        @Override
        public AirParticleData fromNetwork(ParticleType<AirParticleData> particleType, PacketBuffer packetBuffer) {
            return new AirParticleData(packetBuffer.readFloat());
        }
    };
    static final Codec<AirParticleData> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(Codec.FLOAT.fieldOf("alpha")
                    .forGetter((d) -> d.alpha))
                    .apply(instance, AirParticleData::new));

    private static boolean checkDate;
    private static boolean useAlt;

    private final float alpha;

    public AirParticleData(float alpha) {
        this.alpha = alpha;
    }

    @Override
    public ParticleType<?> getType() {
        return useAltParticles() ? ModParticleTypes.AIR_PARTICLE_2.get() : ModParticleTypes.AIR_PARTICLE.get();
    }

    @Override
    public void writeToNetwork(PacketBuffer packetBuffer) {
        packetBuffer.writeFloat(alpha);
    }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "%s %f", getType().getRegistryName(), alpha);
    }

    public float getAlpha() {
        return useAlt ? alpha * 2 : alpha;
    }

    private boolean useAltParticles() {
        if (!checkDate) {
            Calendar calendar = Calendar.getInstance();
            useAlt = calendar.get(Calendar.MONTH) == Calendar.MARCH && calendar.get(Calendar.DAY_OF_MONTH) >= 31
                    || calendar.get(Calendar.MONTH) == Calendar.APRIL && calendar.get(Calendar.DAY_OF_MONTH) <= 2;
            checkDate = true;
        }
        return useAlt;
    }
}

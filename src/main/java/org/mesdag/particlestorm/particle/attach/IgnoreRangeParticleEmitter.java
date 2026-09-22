package org.mesdag.particlestorm.particle.attach;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.mesdag.particlestorm.data.event.ParticleEffect;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.ParticleEmitter;

public abstract class IgnoreRangeParticleEmitter extends ParticleEmitter {
    public final boolean ignoreRange;

    public IgnoreRangeParticleEmitter(Identifier type, Level level, Vec3 pos, Identifier particleId, MolangExp expression, boolean ignoreRange) {
        super(type, level, pos, particleId, expression);
        this.ignoreRange = ignoreRange;
    }

    public IgnoreRangeParticleEmitter(Level level, CompoundTag tag) {
        super(level, tag);
        this.ignoreRange = tag.getBooleanOr("ignoreRange", false);
    }

    public IgnoreRangeParticleEmitter(ParticleEmitter parent, ParticleEffect effect) {
        super(parent, effect);
        this.ignoreRange = parent instanceof IgnoreRangeParticleEmitter irpe && irpe.ignoreRange;
    }

    @Override
    public void serialize(CompoundTag tag) {
        super.serialize(tag);
        tag.putBoolean("ignoreRange", ignoreRange);
    }
}

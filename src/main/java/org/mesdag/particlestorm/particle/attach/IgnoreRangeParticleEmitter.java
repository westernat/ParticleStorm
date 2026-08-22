package org.mesdag.particlestorm.particle.attach;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.mesdag.particlestorm.data.event.ParticleEffect;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.ParticleEmitter;

public abstract class IgnoreRangeParticleEmitter extends ParticleEmitter {
    public final boolean ignoreRange;

    public IgnoreRangeParticleEmitter(ResourceLocation type, Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression, boolean ignoreRange) {
        super(type, level, pos, particleId, expression);
        this.ignoreRange = ignoreRange;
    }

    public IgnoreRangeParticleEmitter(Level level, CompoundTag tag) {
        super(level, tag);
        this.ignoreRange = tag.getBoolean("ignoreRange");
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

package org.mesdag.particlestorm.particle;

import net.minecraft.core.particles.ParticleGroup;

public class MutableParticleGroup extends ParticleGroup {
    private int mutableLimit;

    public MutableParticleGroup(int limit) {
        super(limit);
        this.mutableLimit = limit;
    }

    public void setLimit(int limit) {
        this.mutableLimit = limit;
    }

    @Override
    public int getLimit() {
        return mutableLimit;
    }
}

package org.mesdag.particlestorm.particle;

import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.mesdag.particlestorm.GameClient;
import org.mesdag.particlestorm.data.component.EmitterRate;
import org.mesdag.particlestorm.data.component.IEmitterComponent;
import org.mesdag.particlestorm.data.event.ParticleEffect;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.MolangInstance;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.data.molang.compiler.MathParser;

import java.util.List;

public class ParticleEmitter implements MolangInstance {
    public ResourceLocation particleId;
    public ParticleEffect.Type effectType;
    public MolangExp expression;
    protected boolean haveHadSync = false;

    protected EmitterDetail detail;
    protected VariableTable variableTable;
    protected List<IEmitterComponent> components;

    protected double emitterRandom1;
    protected double emitterRandom2;
    protected double emitterRandom3;
    protected double emitterRandom4;
    public float invTickRate;
    public int id;

    public int age = 0;
    public int lifetime = 0;
    public boolean active = false;
    public int loopingTime = 0;
    public int activeTime = 0;
    public int fullLoopTime = 0;
    public ParticleGroup particleGroup;
    public int spawnDuration = 1;
    public int spawnRate = 0;
    public boolean spawned = false;
    public Entity attached;
    public int lastTimeline = 0;
    public float moveDist = 0.0F;
    public float moveDistO = 0.0F;
    public int lastTravelDist = 0;
    public float[] cachedLooping;

    public final Level level;
    public Vec3 pos;
    public Vector3f rot = new Vector3f();
    public Vec3 deltaMovement = Vec3.ZERO;
    private boolean removed = false;

    public ParticleEmitter(Level level, Vector3f pos, ResourceLocation particleId, ParticleEffect.Type type, MolangExp expression) {
        this.level = level;
        setPos(new Vec3(pos.x, pos.y, pos.z));
        this.particleId = particleId;
        this.effectType = type;
        this.expression = expression;
        this.emitterRandom1 = level.random.nextDouble();
        this.emitterRandom2 = level.random.nextDouble();
        this.emitterRandom3 = level.random.nextDouble();
        this.emitterRandom4 = level.random.nextDouble();
        this.invTickRate = 1.0F / level.tickRateManager().tickrate();
    }

    public ParticleEmitter(Level level, Vector3f pos, ResourceLocation particleId) {
        this(level, pos, particleId, ParticleEffect.Type.EMITTER, MolangExp.EMPTY);
    }

    public ParticleEmitter(Level level, CompoundTag tag) {
        this.level = level;
        deserialize(tag);
        this.invTickRate = 1.0F / level.tickRateManager().tickrate();
    }

    public void tick() {
        if (haveHadSync) {
            this.invTickRate = 1.0F / level.tickRateManager().tickrate();
            this.moveDistO = moveDist;
            for (IEmitterComponent component : components) {
                component.update(this);
            }
            if (detail.emitterRateType == EmitterRate.Type.MANUAL) {
                remove();
                return;
            }
            this.age++;
        } else if (particleId != null) {
            this.detail = GameClient.LOADER.ID_2_EMITTER.get(particleId);
            this.variableTable = new VariableTable(detail.variableTable);
            if (expression != null && !expression.initialized()) {
                expression.compile(new MathParser(variableTable));
            }
            // todo effect type
            detail.assignments.forEach(assignment -> {
                // 重定向，防止污染变量表
                variableTable.setValue(assignment.variable().name(), assignment.value());
            });
            this.components = detail.components.stream().filter(e -> {
                e.apply(this);
                return e.requireUpdate();
            }).toList();
            this.haveHadSync = true;
        }
        move(deltaMovement);
    }

    public void remove() {
        if (detail.lifetimeEvents != null) {
            detail.lifetimeEvents.onExpiration(this);
        }
        this.removed = true;
    }

    public boolean isRemoved() {
        return removed;
    }

    public void move(Vec3 delta) {
        this.moveDist += (float) delta.length();
        pos.add((float) delta.x, (float) delta.y, (float) delta.z);
    }

    public void setPos(Vec3 pos) {
        this.pos = pos;
    }

    public EmitterDetail getDetail() {
        return detail;
    }

    public void deserialize(CompoundTag compound) {
        this.particleId = ResourceLocation.parse(compound.getString("particleId"));
        this.effectType = ParticleEffect.Type.getById(compound.getInt("effectType"));
        this.expression = new MolangExp(compound.getString("expression"));
        this.emitterRandom1 = compound.getDouble("emitterRandom1");
        this.emitterRandom2 = compound.getDouble("emitterRandom2");
        this.emitterRandom3 = compound.getDouble("emitterRandom3");
        this.emitterRandom4 = compound.getDouble("emitterRandom4");
        this.pos = new Vec3(compound.getDouble("posX"), compound.getDouble("posY"), compound.getDouble("posZ"));
        this.rot.set(compound.getFloat("rotX"), compound.getFloat("rotY"), compound.getFloat("rotZ"));
        this.deltaMovement = new Vec3(compound.getDouble("movX"), compound.getDouble("movY"), compound.getDouble("movZ"));
    }

    public void serialize(CompoundTag compound) {
        compound.putString("particleId", particleId.toString());
        compound.putInt("effectType", effectType.getId());
        compound.putString("expression", expression.getExpStr());
        compound.putDouble("emitterRandom1", emitterRandom1);
        compound.putDouble("emitterRandom2", emitterRandom2);
        compound.putDouble("emitterRandom3", emitterRandom3);
        compound.putDouble("emitterRandom4", emitterRandom4);
        compound.putDouble("posX", pos.x);
        compound.putDouble("posY", pos.y);
        compound.putDouble("posZ", pos.z);
        compound.putFloat("rotX", rot.x);
        compound.putFloat("rotY", rot.y);
        compound.putFloat("rotZ", rot.z);
        compound.putDouble("movX", deltaMovement.x);
        compound.putDouble("movY", deltaMovement.y);
        compound.putDouble("movZ", deltaMovement.z);
    }

    public double getX() {
        return pos.x;
    }

    public double getY() {
        return pos.y;
    }

    public double getZ() {
        return pos.z;
    }

    public float getXRot() {
        return rot.x;
    }

    public float getYRot() {
        return rot.y;
    }

    @Override
    public VariableTable getVariableTable() {
        return variableTable;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public float tickAge() {
        return age * invTickRate;
    }

    @Override
    public float tickLifetime() {
        return lifetime * invTickRate;
    }

    @Override
    public double getRandom1() {
        return emitterRandom1;
    }

    @Override
    public double getRandom2() {
        return emitterRandom2;
    }

    @Override
    public double getRandom3() {
        return emitterRandom3;
    }

    @Override
    public double getRandom4() {
        return emitterRandom4;
    }

    @Override
    public ResourceLocation getIdentity() {
        return particleId;
    }

    @Override
    public Vec3 getPosition() {
        return pos;
    }

    @Override
    public Entity getAttachedEntity() {
        return attached;
    }

    @Override
    public float getInvTickRate() {
        return invTickRate;
    }

    @Override
    public ParticleEmitter getEmitter() {
        return this;
    }
}

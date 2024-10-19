package org.mesdag.particlestorm.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.mesdag.particlestorm.GameClient;
import org.mesdag.particlestorm.data.component.EmitterRate;
import org.mesdag.particlestorm.data.component.IEmitterComponent;
import org.mesdag.particlestorm.data.event.ParticleEffect;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.MolangInstance;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.data.molang.compiler.MolangParser;

import java.util.ArrayList;
import java.util.List;

public class ParticleEmitter implements MolangInstance {
    public ResourceLocation particleId;
    public ParticleEffect.Type effectType;
    public MolangExp expression;

    protected transient boolean initialized = false;
    public transient ParentMode parentMode = ParentMode.WORLD;
    public transient Vec3 offsetPos = Vec3.ZERO;
    public transient Vector3f offsetRot = new Vector3f();
    public transient Vector3f parentRotation;
    protected transient EmitterDetail detail;
    protected transient VariableTable variableTable;
    public transient VariableTable subTable;
    protected transient List<IEmitterComponent> components;
    public transient ParticleEmitter parent;
    public transient final List<ParticleEmitter> children = new ArrayList<>();

    protected double emitterRandom1;
    protected double emitterRandom2;
    protected double emitterRandom3;
    protected double emitterRandom4;
    public int id;

    public transient float invTickRate;
    public transient int age = 0;
    public transient int lifetime = 0;
    public transient boolean active = false;
    public transient int loopingTime = 0;
    public transient int activeTime = 0;
    public transient int fullLoopTime = 0;
    public transient ParticleGroup particleGroup;
    public transient int spawnDuration = 1;
    public transient int spawnRate = 0;
    public transient boolean spawned = false;
    public transient Entity attached;
    public transient BlockEntity attachedBlock;
    public transient int lastTimeline = 0;
    public transient float moveDist = 0.0F;
    public transient float moveDistO = 0.0F;
    public transient int lastTravelDist = 0;
    public transient float[] cachedLooping;

    public transient final Level level;
    public Vec3 pos;
    public Vector3f rot = new Vector3f();
    public Vec3 deltaMovement = Vec3.ZERO;
    private transient boolean removed = false;

    public ParticleEmitter(Level level, Vec3 pos, ResourceLocation particleId, ParticleEffect.Type type, MolangExp expression) {
        this.level = level;
        setPos(pos);
        this.particleId = particleId;
        this.effectType = type;
        this.expression = expression;
        this.emitterRandom1 = level.random.nextDouble();
        this.emitterRandom2 = level.random.nextDouble();
        this.emitterRandom3 = level.random.nextDouble();
        this.emitterRandom4 = level.random.nextDouble();
        this.invTickRate = 1.0F / level.tickRateManager().tickrate();
    }

    public ParticleEmitter(Level level, Vec3 pos, ResourceLocation particleId) {
        this(level, pos, particleId, ParticleEffect.Type.EMITTER, MolangExp.EMPTY);
    }

    public ParticleEmitter(Level level, CompoundTag tag) {
        this.level = level;
        deserialize(tag);
        this.invTickRate = 1.0F / level.tickRateManager().tickrate();
    }

    public void tick() {
        if (initialized) {
            this.invTickRate = 1.0F / level.tickRateManager().tickrate();
            this.moveDistO = moveDist;
            for (IEmitterComponent component : components) {
                component.update(this);
            }
            this.age++;
            move(deltaMovement);
            if (detail.emitterRateType == EmitterRate.Type.MANUAL) {
                remove();
                return;
            }
            if (attached != null) {
                if (attached.isRemoved()) {
                    remove();
                    return;
                }
                if (parentRotation != null) {
                    rot.set(parentRotation).add(offsetRot.x, offsetRot.y - getAttachedYRot() * Mth.DEG_TO_RAD, offsetRot.z);
                }
                Vector3f rotated = offsetPos.toVector3f().rotateZ(rot.z).rotateY(rot.y).rotateX(rot.x);
                this.pos = new Vec3(attached.getX() + rotated.x, attached.getY() + rotated.y, attached.getZ() + rotated.z);
            } else if (attachedBlock != null) {
                if (attachedBlock.isRemoved()) {
                    remove();
                    return;
                }
                if (parentRotation != null) {
                    rot.set(parentRotation).add(offsetRot);
                }
                Vector3f rotated = offsetPos.toVector3f().rotateZ(rot.z).rotateY(rot.y).rotateX(rot.x);
                BlockPos pos1 = attachedBlock.getBlockPos();
                this.pos = new Vec3(pos1.getX() + 0.5 + rotated.x, pos1.getY() + 0.5 + rotated.y, pos1.getZ() + 0.5 + rotated.z);
            }
            if (parent != null && parent.isRemoved()) {
                remove();
            }
        } else if (particleId != null) {
            this.detail = GameClient.LOADER.ID_2_EMITTER.get(particleId);
            if (detail == null) {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.sendSystemMessage(Component.translatable("particle.notFound", particleId.toString()));
                }
                remove();
                return;
            }
            this.variableTable = new VariableTable(detail.variableTable);
            if (subTable != null && variableTable.subTable == null) {
                variableTable.subTable = subTable;
            }
            if (expression != null && !expression.initialized()) {
                expression.compile(new MolangParser(variableTable));
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
            this.initialized = true;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    private float getAttachedYRot() {
        return attached instanceof LivingEntity living ? living.yBodyRot : attached.getYRot();
    }

    public void remove() {
        if (detail != null && detail.lifetimeEvents != null) {
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

    public enum ParentMode {
        LOCATOR,
        WORLD
    }
}

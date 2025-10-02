package org.mesdag.particlestorm.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.mesdag.particlestorm.PSGameClient;
import org.mesdag.particlestorm.api.IEmitterComponent;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.MathHelper;
import org.mesdag.particlestorm.data.component.EmitterLifetime;
import org.mesdag.particlestorm.data.component.EmitterRate;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.data.molang.compiler.MathValue;
import org.mesdag.particlestorm.data.molang.compiler.MolangParser;
import org.mesdag.particlestorm.data.molang.compiler.value.VariableAssignment;
import org.mesdag.particlestorm.mixed.IEntity;

import java.util.ArrayList;
import java.util.List;

public class ParticleEmitter implements MolangInstance {
    public ResourceLocation particleId;
    public MolangExp expression;

    public transient ParentMode parentMode = ParentMode.WORLD;
    public transient Vec3 offsetPos = Vec3.ZERO;
    public transient Vector3f offsetRot = new Vector3f();
    public transient Vector3f parentPosition;
    public transient Vector3f parentRotation;
    protected transient EmitterPreset preset;
    protected transient VariableTable vars;
    protected transient List<IEmitterComponent> components;
    public transient ParticleEmitter parent;
    public transient final List<ParticleEmitter> children = new ArrayList<>();
    public transient Vector3f inheritedParticleSpeed;
    public transient boolean isManual;

    protected double emitterRandom1;
    protected double emitterRandom2;
    protected double emitterRandom3;
    protected double emitterRandom4;
    public int id;

    public transient float invTickRate;
    public transient int age = 0;
    public transient int lifetime = 0;
    public transient boolean active = true;
    public transient int loopingTime = 0;
    public transient int activeTime = 0;
    public transient int fullLoopTime = 0;
    public transient MutableParticleGroup particleGroup;
    public transient int spawnDuration = 1;
    public transient int spawnRate = 0;
    public transient boolean spawned = false;
    protected transient Entity attached;
    public transient BlockEntity attachedBlock;
    public transient int lastTimeline = 0;
    public transient float moveDist = 0.0F;
    public transient float moveDistO = 0.0F;
    public transient int lastTravelDist = 0;
    public transient float[] cachedLooping;

    public transient final Level level;
    public Vec3 pos;
    public Vec3 posO = Vec3.ZERO;
    public Vector3f rot = new Vector3f();
    private transient boolean removed = false;

    public ParticleEmitter(Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression) {
        this.level = level;
        setPos(pos);
        this.particleId = particleId;
        this.expression = expression;
        updateRandoms(level.random);
        this.invTickRate = 1.0F / level.tickRateManager().tickrate();
        init();
    }

    public ParticleEmitter(Level level, Vec3 pos, ResourceLocation particleId) {
        this(level, pos, particleId, MolangExp.EMPTY);
    }

    public ParticleEmitter(Level level, CompoundTag tag) {
        this.level = level;
        deserialize(tag);
        this.invTickRate = 1.0F / level.tickRateManager().tickrate();
        init();
    }

    public void attachEntity(@Nullable Entity entity) {
        if (entity == null) {
            this.vars = new VariableTable(vars.table, preset.vars);
            this.attached = null;
        } else {
            VariableTable parent = IEntity.of(entity).particlestorm$getVariableTable();
            parent.setParent(preset.vars);
            this.vars = new VariableTable(vars.table, parent);
            this.attached = entity;
        }
    }

    private void init() {
        this.preset = PSGameClient.LOADER.id2Emitter().get(particleId);
        if (preset == null) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(Component.translatable("particle.notFound", particleId.toString()));
            }
            remove();
            return;
        }
        this.vars = new VariableTable(preset.vars);
        if (expression != null && !expression.initialized()) {
            expression.compile(new MolangParser(vars));
            MathValue variable = expression.getVariable();
            List<VariableAssignment> toInit = new ArrayList<>();
            if (variable != null && !MathHelper.forAssignment(vars.table, toInit, variable)) {
                MathHelper.forCompound(vars.table, toInit, variable);
            }
            MathHelper.redirect(toInit, vars);
        }
        MathHelper.redirect(preset.assignments, vars);
        this.components = preset.components.stream().filter(e -> {
            e.apply(this);
            return e.requireUpdate();
        }).toList();
    }

    public synchronized void updateRandoms(RandomSource random) {
        this.emitterRandom1 = random.nextDouble();
        this.emitterRandom2 = random.nextDouble();
        this.emitterRandom3 = random.nextDouble();
        this.emitterRandom4 = random.nextDouble();
    }

    public void tick() {
        this.invTickRate = 1.0F / level.tickRateManager().tickrate();
        this.moveDistO = moveDist;
        this.posO = pos;
        for (IEmitterComponent component : components) {
            if (active || component instanceof EmitterLifetime.Looping) {
                component.update(this);
            }
        }
        this.age++;
        if (!posO.equals(pos)) {
            this.moveDist += (float) pos.subtract(posO).length();
        }
        if (isManual || preset.emitterRateType == EmitterRate.Type.MANUAL) {
            remove();
            return;
        }
        if (attached != null) {
            if (attached.isRemoved()) {
                remove();
                return;
            }
            if (parentRotation != null) {
                rot.set(parentRotation).add(offsetRot.x, offsetRot.y + getAttachedYRot() * Mth.DEG_TO_RAD, offsetRot.z);
            }
            Vector3f rotated = offsetPos.toVector3f().rotateZ(rot.z).rotateY(rot.y).rotateX(rot.x);
            if (parentPosition != null) {
                rotated.add(parentPosition);
            }
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
            if (parentPosition != null) {
                rotated.add(parentPosition);
            }
            BlockPos pos1 = attachedBlock.getBlockPos();
            this.pos = new Vec3(pos1.getX() + 0.5 + rotated.x, pos1.getY() + rotated.y, pos1.getZ() + 0.5 + rotated.z);
        }
        if (parent != null && parent.isRemoved()) {
            remove();
        }
    }

    private float getAttachedYRot() {
        return attached instanceof LivingEntity living ? -living.yBodyRot : attached.getYRot();
    }

    public void remove() {
        this.removed = true;
    }

    public void onRemove() {
        children.removeIf(child -> {
            child.parent = null;
            child.remove();
            return true;
        });
        if (preset != null && preset.lifetimeEvents != null) {
            preset.lifetimeEvents.onExpiration(this);
        }
    }

    public void addParent(ParticleEmitter parent) {
        parent.children.add(this);
        this.parent = parent;
    }

    public boolean isRemoved() {
        return removed || (attached != null && attached.isRemoved());
    }

    public void setPos(Vec3 pos) {
        this.pos = pos;
    }

    public EmitterPreset getPreset() {
        return preset;
    }

    public void deserialize(CompoundTag compound) {
        this.particleId = ResourceLocation.parse(compound.getString("particleId"));
        this.expression = new MolangExp(compound.getString("expression"));
        this.emitterRandom1 = compound.getDouble("emitterRandom1");
        this.emitterRandom2 = compound.getDouble("emitterRandom2");
        this.emitterRandom3 = compound.getDouble("emitterRandom3");
        this.emitterRandom4 = compound.getDouble("emitterRandom4");
        this.pos = new Vec3(compound.getDouble("posX"), compound.getDouble("posY"), compound.getDouble("posZ"));
        this.rot.set(compound.getFloat("rotX"), compound.getFloat("rotY"), compound.getFloat("rotZ"));
        this.posO = new Vec3(compound.getDouble("movX"), compound.getDouble("movY"), compound.getDouble("movZ"));
    }

    public void serialize(CompoundTag compound) {
        compound.putString("particleId", particleId.toString());
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
        compound.putDouble("movX", posO.x);
        compound.putDouble("movY", posO.y);
        compound.putDouble("movZ", posO.z);
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

    @Override
    public VariableTable getVars() {
        return vars;
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
    public @Nullable Entity getAttachedEntity() {
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

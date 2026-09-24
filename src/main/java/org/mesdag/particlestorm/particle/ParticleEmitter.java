package org.mesdag.particlestorm.particle;

import net.minecraft.core.particles.ParticleLimit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4x3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.PSDiagnostics;
import org.mesdag.particlestorm.api.IEmitterComponent;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.api.ParticleEmitterAttachable;
import org.mesdag.particlestorm.data.component.EmitterLifetime;
import org.mesdag.particlestorm.data.component.EmitterRate;
import org.mesdag.particlestorm.data.event.ParticleEffect;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.data.molang.compiler.MolangParser;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;
import org.mesdag.particlestorm.mixed.IBlockEntity;
import org.mesdag.particlestorm.mixed.IEntity;

import java.util.ArrayList;
import java.util.List;

public class ParticleEmitter implements MolangInstance {
    public static final String TYPE_KEY = "type";
    public static final Identifier TYPE = ParticleStorm.asResource("default");

    public final Identifier type;
    public Identifier particleId;
    public MolangExp expression;

    public transient Matrix4x3f parentSpace;
    public transient ParentMode parentMode = ParentMode.WORLD;
    public transient Vec3 offsetPos = Vec3.ZERO;
    public transient Vector3f offsetRot = new Vector3f();
    public transient Vector3f parentPosition;
    public transient Vector3f parentRotation;
    protected transient EmitterPreset preset;
    protected transient VariableTable vars;
    protected transient List<IEmitterComponent> components;
    public transient ParticleEmitter parent;
    public transient @Nullable Runnable afterParentInit;
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
    public transient ParticleLimit particleGroup;
    public transient int activeParticleCount = 0;
    public transient float spawnChance;
    public transient int spawnRate = 0;
    public transient boolean spawned = false;
    public boolean hideOutline;
    protected transient ParticleEmitterAttachable attached;
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

    public ParticleEmitter(Identifier type, Level level, Vec3 pos, Identifier particleId, MolangExp expression) {
        this.type = type;
        this.level = level;
        setPos(pos);
        this.posO = pos;
        this.particleId = particleId;
        this.expression = expression;
        updateRandoms(level.getRandom());
        this.invTickRate = 1.0F / level.tickRateManager().tickrate();
        init();
    }

    public ParticleEmitter(Level level, Vec3 pos, Identifier particleId, MolangExp expression) {
        this(TYPE, level, pos, particleId, expression);
    }

    public ParticleEmitter(Level level, Vec3 pos, Identifier particleId) {
        this(level, pos, particleId, MolangExp.EMPTY);
    }

    public ParticleEmitter(Level level, CompoundTag tag) {
        Identifier type = Identifier.tryParse(tag.getStringOr(TYPE_KEY, ""));
        this.type = type == null ? TYPE : type;
        this.level = level;
        deserialize(tag);
        this.invTickRate = 1.0F / level.tickRateManager().tickrate();
        init();
    }

    public ParticleEmitter(ParticleEmitter parent, ParticleEffect effect) {
        this.type = parent.type;
        this.hideOutline = parent.hideOutline;
        this.level = parent.level;
        setPos(parent.pos);
        this.posO = pos;
        this.particleId = effect.effect();
        this.expression = effect.preEffectExpression();
        updateRandoms(level.getRandom());
        this.invTickRate = 1.0F / level.tickRateManager().tickrate();
        this.afterParentInit = () -> {
            switch (effect.type()) {
                case EMITTER -> {}
                case EMITTER_BOUND -> {
                    attach(parent.getAttached());
                    this.offsetPos = parent.offsetPos;
                    this.offsetRot = parent.offsetRot;
                    this.parentPosition = parent.parentPosition;
                    this.parentRotation = parent.parentRotation;
                    this.parentMode = parent.parentMode;
                    this.parentSpace = parent.parentSpace;
                }
                case PARTICLE -> this.isManual = true;
                case PARTICLE_WITH_VELOCITY -> {
                    this.isManual = true;
                    if (parent.getAttachedEntity() != null) {
                        this.inheritedParticleSpeed = parent.getAttachedEntity().getDeltaMovement().toVector3f();
                    }
                }
            }
        };
        addParent(parent);
        createVars();
        for (String name : effect.sharedVars()) {
            Variable variable = parent.getVars().getVariable(name);
            if (variable == null) throw new IllegalArgumentException("Shared vars must defined in parent directly!");
            vars.table.put(name, variable);
        }
        initVars();
        createComponents();
    }

    public void attachEntity(@Nullable Entity entity) {
        attach(entity == null ? null : IEntity.of(entity));
    }

    public void attachBlock(@Nullable BlockEntity blockEntity) {
        attach(blockEntity == null ? null : IBlockEntity.of(blockEntity));
    }

    public void attach(@Nullable ParticleEmitterAttachable attachable) {
        this.attached = attachable;
        if (attachable == null) {
            this.vars = new VariableTable(vars.table, preset.vars);
        } else {
            VariableTable parent = attachable.getVariableTable();
            parent.setParent(preset.vars);
            this.vars = new VariableTable(vars.table, parent);
        }
    }

    public @Nullable BlockEntity getAttachedBlock() {
        return attached instanceof BlockEntity blockEntity ? blockEntity : null;
    }

    @Override
    public @Nullable ParticleEmitterAttachable getAttached() {
        return attached;
    }

    protected void init() {
        createVars();
        initVars();
        createComponents();
    }

    protected void createVars() {
        Identifier requestedId = particleId;
        Identifier resolvedId = MolangParticleEngine.INSTANCE.resolveParticleId(particleId);
        if (resolvedId != null) {
            this.particleId = resolvedId;
        }
        this.preset = MolangParticleEngine.INSTANCE.id2Emitter().get(particleId);
        if (preset == null) {
            throw new IllegalArgumentException("Unknown particle id: '" + particleId + "'!");
        }
        this.vars = new VariableTable(preset.vars);
        PSDiagnostics.infoFirstN("emitter-vars:" + particleId, 32, "emitter vars requested={} resolved={} expression={} presetComponents={} eventKeys={}",
                requestedId,
                particleId,
                expression == null ? "" : expression.getExpStr(),
                preset.components.stream().map(component -> component.getClass().getSimpleName()).toList(),
                preset.events.keySet()
        );
    }

    protected void initVars() {
        if (expression != null && !expression.initialized()) {
            expression.compile(new MolangParser(vars));
            expression.calculate(this);
        }
    }

    protected void createComponents() {
        this.components = preset.components.stream().filter(e -> {
            e.apply(this);
            return e.requireUpdate();
        }).toList();
        PSDiagnostics.infoFirstN("emitter-components:" + particleId, 32, "emitter ready runtimeId={} particle={} active={} lifetime={} rateType={} spawnRate={} particleGroup={} localPosition={} localRotation={} localVelocity={} updateComponents={}",
                id,
                particleId,
                active,
                lifetime,
                preset.emitterRateType,
                spawnRate,
                particleGroup,
                preset.localPosition,
                preset.localRotation,
                preset.localVelocity,
                components.stream().map(component -> component.getClass().getSimpleName()).toList()
        );
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

        if (attached instanceof Entity entity) {
            if (entity.isRemoved()) {
                remove();
                return;
            }
            Vector3f rotated;
            if (isLocalSpace()) {
                rotated = new Vector3f(parentSpace.m30(), parentSpace.m31(), parentSpace.m32());
            } else {
                if (parentRotation != null) {
                    rot.set(parentRotation).add(offsetRot.x, offsetRot.y + getAttachedYRot(entity) * Mth.DEG_TO_RAD, offsetRot.z);
                }
                rotated = offsetPos.toVector3f().rotateZ(rot.z).rotateY(rot.y).rotateX(rot.x);
                if (parentPosition != null) {
                    rotated.add(parentPosition);
                }
            }
            this.pos = new Vec3(entity.getX() + rotated.x, entity.getY() + rotated.y, entity.getZ() + rotated.z);
        } else if (attached != null) {
            ParticleEmitterAttachable attachable = attached;
            if (attachable.isDiscarded()) {
                remove();
                return;
            }
            Vector3f rotated;
            if (isLocalSpace()) {
                rotated = new Vector3f(parentSpace.m30(), parentSpace.m31(), parentSpace.m32());
            } else {
                if (parentRotation != null) {
                    rot.set(parentRotation).add(offsetRot);
                }
                rotated = offsetPos.toVector3f().rotateZ(rot.z).rotateY(rot.y).rotateX(rot.x);
                if (parentPosition != null) {
                    rotated.add(parentPosition);
                }
            }
            this.pos = attachable.getPos().add(rotated.x, rotated.y, rotated.z);
        }

        for (IEmitterComponent component : components) {
            if (active || component instanceof EmitterLifetime.Looping) {
                component.update(this);
            }
        }
        this.age++;

        if (!posO.equals(pos)) {
            this.moveDist += (float) pos.subtract(posO).length();
        }

        if (afterParentInit != null && parent != null) {
            afterParentInit.run();
            this.afterParentInit = null;
        }

        if (parent != null && parent.isRemoved()) {
            remove();
        } else if (isManual || preset.emitterRateType == EmitterRate.Type.MANUAL) {
            remove();
        }
    }

    private float getAttachedYRot(Entity entity) {
        return entity instanceof LivingEntity living ? -living.yBodyRot : entity.getYRot();
    }

    public void local2World(Vector3f vec, float partialTick) {
        if (isLocalSpace()) {
            if (preset.localRotation) {
                vec.mulDirection(parentSpace);
            }
            if (preset.localPosition) {
                vec.add(
                        (float) Mth.lerp((double) partialTick, posO.x, pos.x),
                        (float) Mth.lerp((double) partialTick, posO.y, pos.y),
                        (float) Mth.lerp((double) partialTick, posO.z, pos.z)
                );
            }
        }
    }

    public boolean isLocalSpace() {
        return parentSpace != null;
    }

    public @Nullable Quaternionf getLocalSpaceRotation() {
        return isLocalSpace() && preset != null && preset.localRotation
                ? parentSpace.getNormalizedRotation(new Quaternionf())
                : null;
    }

    public final Matrix4x3f getLocalSpace() {
        return parentSpace;
    }

    public final void setLocalSpace(@Nullable Matrix4x3f space) {
        setLocalSpace(space, true);
    }

    public final void setLocalSpace(@Nullable Matrix4x3f space, boolean updatePos) {
        this.parentSpace = space;
        if (updatePos && space != null) {
            ParticleEmitterAttachable attachable = getAttached();
            Vec3 base = attachable != null ? attachable.getPos() : pos;
            this.pos = base.add(space.m30(), space.m31(), space.m32());
        }
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
        ParticleEmitterAttachable attachable = getAttached();
        return removed || (attachable != null && attachable.isDiscarded());
    }

    /// Whether this emitter may spawn another particle. Without a particle group it is unlimited; otherwise bounded by the living particle count of this emitter only.
    public boolean hasSpace() {
        return particleGroup == null || activeParticleCount < particleGroup.limit();
    }

    /// Must be called once per particle successfully added to the particle engine for this emitter.
    public void onAdded() {
        activeParticleCount++;
    }

    /// Must be called once when a particle owned by this emitter is removed. Clamped to zero to stay safe against redundant removals.
    public void onRemoved() {
        if (activeParticleCount > 0) {
            activeParticleCount--;
        }
    }

    public void setPos(Vec3 pos) {
        this.pos = pos;
    }

    public EmitterPreset getPreset() {
        return preset;
    }

    public void deserialize(CompoundTag compound) {
        this.hideOutline = compound.getBoolean("hideOutline").orElse(false);
        this.particleId = Identifier.parse(compound.getString("particleId").orElse(""));
        this.expression = new MolangExp(compound.getString("expression").orElse(""));
        this.emitterRandom1 = compound.getDouble("emitterRandom1").orElse(0.0);
        this.emitterRandom2 = compound.getDouble("emitterRandom2").orElse(0.0);
        this.emitterRandom3 = compound.getDouble("emitterRandom3").orElse(0.0);
        this.emitterRandom4 = compound.getDouble("emitterRandom4").orElse(0.0);
        this.posO = this.pos = new Vec3(
                compound.getDouble("posX").orElse(0.0),
                compound.getDouble("posY").orElse(0.0),
                compound.getDouble("posZ").orElse(0.0)
        );
        this.rot.set(
                compound.getFloat("rotX").orElse(0.0F),
                compound.getFloat("rotY").orElse(0.0F),
                compound.getFloat("rotZ").orElse(0.0F)
        );
    }

    public final CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString(TYPE_KEY, type.toString());
        serialize(tag);
        return tag;
    }

    public void serialize(CompoundTag compound) {
        compound.putString("particleId", particleId.toString());
        compound.putString("expression", expression.getExpStr());
        compound.putBoolean("hideOutline", hideOutline);
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
    public Identifier getIdentity() {
        return particleId;
    }

    @Override
    public Vec3 getPosition() {
        return pos;
    }

    @Override
    public @Nullable Entity getAttachedEntity() {
        return attached instanceof Entity entity ? entity : null;
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

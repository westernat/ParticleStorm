package org.mesdag.particlestorm.particle;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4x3f;
import org.joml.Vector3f;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.IEmitterComponent;
import org.mesdag.particlestorm.api.MolangInstance;
import org.mesdag.particlestorm.data.component.EmitterLifetime;
import org.mesdag.particlestorm.data.component.EmitterRate;
import org.mesdag.particlestorm.data.event.ParticleEffect;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.data.molang.compiler.MolangParser;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;
import org.mesdag.particlestorm.mixed.IPSBlockEntity;
import org.mesdag.particlestorm.mixed.IPSEntity;

import java.util.ArrayList;
import java.util.List;

public class ParticleEmitter implements MolangInstance {
    public static final String TYPE_KEY = "type";
    public static final ResourceLocation TYPE = ParticleStorm.asResource("default");

    public final ResourceLocation type;
    public ResourceLocation particleId;
    public MolangExp expression;

    private transient Matrix4x3f localSpace;

    protected transient EmitterPreset preset;
    protected transient VariableTable vars;
    protected transient List<IEmitterComponent> components;
    public transient ParticleEmitter parent;
    public transient @Nullable Runnable afterParentInit;
    protected transient @Nullable List<ParticleEmitter> children;
    public transient Vector3f inheritedParticleSpeed;
    public transient boolean isManual;

    protected float emitterRandom1;
    protected float emitterRandom2;
    protected float emitterRandom3;
    protected float emitterRandom4;
    public int id;

    public transient float invTickRate;
    public transient int age;
    public transient int lifetime;
    public transient boolean active = true;
    public transient int loopingTime;
    public transient int activeTime;
    public transient int fullLoopTime;
    public transient MutableParticleGroup particleGroup;
    public transient int spawnDuration = 1;
    public transient int spawnRate;
    /// The probability of spawning one extra particle per tick, i.e. the fractional part of spawn_rate / 20. Only used by EmitterRate.Steady; defaults to 0.
    public transient float spawnChance;
    public transient boolean spawned;
    protected transient Entity attached;
    protected transient BlockEntity attachedBlock;
    public transient int lastTimeline;
    public transient float moveDist;
    public transient float moveDistO;
    public transient int lastTravelDist;
    public transient float[] cachedLooping;

    public transient final Level level;
    protected Vec3 pos;
    public Vec3 posO;
    public boolean hideOutline;
    private transient boolean removed;

    public ParticleEmitter(ResourceLocation type, Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression) {
        this.type = type;
        this.level = level;
        setPos(pos);
        this.posO = pos;
        this.particleId = particleId;
        this.expression = expression;
        updateRandoms(level.random);
        this.invTickRate = 1.0F / 20.0F;
        init();
    }

    public ParticleEmitter(Level level, Vec3 pos, ResourceLocation particleId, MolangExp expression) {
        this(TYPE, level, pos, particleId, expression);
    }

    public ParticleEmitter(Level level, Vec3 pos, ResourceLocation particleId) {
        this(level, pos, particleId, MolangExp.EMPTY);
    }

    public ParticleEmitter(Level level, CompoundTag tag) {
        ResourceLocation type;
        try {
            type = ResourceLocation.parse(tag.getString(TYPE_KEY));
        } catch (Exception e) {
            type = TYPE;
        }
        this.type = type;
        this.level = level;
        deserialize(tag);
        this.invTickRate = 1.0F / 20.0F;
        init();
    }

    public ParticleEmitter(ParticleEmitter parent, ParticleEffect effect) {
        this.type = parent.type;
        this.level = parent.level;
        setPos(parent.pos);
        this.posO = pos;
        this.particleId = effect.effect();
        this.expression = effect.preEffectExpression();
        updateRandoms(level.random);
        this.invTickRate = 1.0F / 20.0F;
        this.hideOutline = parent.hideOutline;
        this.afterParentInit = () -> {
            switch (effect.type()) {
                case EMITTER -> {}
                case EMITTER_BOUND -> {
                    attachEntity(parent.getAttachedEntity());
                    this.attachedBlock = parent.attachedBlock;
                    this.localSpace = parent.localSpace;
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
            Variable variable = parent.getVars().table.get(name);
            if (variable == null) throw new IllegalArgumentException("Shared vars must defined in parent directly!");
            vars.table.put(name, variable);
        }
        initVars();
        createComponents();
    }

    public void attachEntity(@Nullable Entity entity) {
        if (entity == null) {
            this.vars = new VariableTable(vars.table, preset.vars);
            this.attached = null;
        } else {
            VariableTable parent = IPSEntity.of(entity).particlestorm$getVariableTable();
            parent.setParent(preset.vars);
            this.vars = new VariableTable(vars.table, parent);
            this.attached = entity;
        }
    }

    public void attachBlock(@Nullable BlockEntity entity) {
        if (entity == null) {
            this.vars = new VariableTable(vars.table, preset.vars);
            this.attachedBlock = null;
        } else {
            VariableTable parent = IPSBlockEntity.of(entity).particlestorm$getVariableTable();
            parent.setParent(preset.vars);
            this.vars = new VariableTable(vars.table, parent);
            this.attachedBlock = entity;
        }
    }

    public BlockEntity getAttachedBlock() {
        return attachedBlock;
    }

    protected void init() {
        createVars();
        initVars();
        createComponents();
    }

    protected void createVars() {
        this.preset = MolangParticleEngine.INSTANCE.id2Emitter().get(particleId);
        if (preset == null) {
            throw new IllegalArgumentException("Unknown particle id: '" + particleId + "'!");
        }
        this.vars = new VariableTable(preset.vars);
    }

    protected void initVars() {
        if (expression != null && !expression.initialized()) {
            expression.compile(new MolangParser(vars));
        }
    }

    protected void createComponents() {
        this.components = preset.components.stream().filter(e -> {
            e.apply(this);
            return e.requireUpdate();
        }).toList();
    }

    public synchronized void updateRandoms(RandomSource random) {
        this.emitterRandom1 = random.nextFloat();
        this.emitterRandom2 = random.nextFloat();
        this.emitterRandom3 = random.nextFloat();
        this.emitterRandom4 = random.nextFloat();
    }

    public void tick() {
        this.invTickRate = 1.0F / 20.0F;
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

        if (attached != null) {
            if (attached.isRemoved()) {
                remove();
                return;
            }
            updatePos(attached.getX(), attached.getY(), attached.getZ());
        } else if (attachedBlock != null) {
            if (attachedBlock.isRemoved()) {
                remove();
                return;
            }
            BlockPos bp = attachedBlock.getBlockPos();
            updatePos(bp.getX() + 0.5, bp.getY(), bp.getZ() + 0.5);
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

    protected void updatePos(double x, double y, double z) {
        if (isLocalSpace()) {
            x += localSpace.m30();
            y += localSpace.m31();
            z += localSpace.m32();
        }
        this.pos = new Vec3(x, y, z);
    }

    public void local2World(Vector3f vec, float partialTick) {
        if (isLocalSpace()) {
            if (preset.localRotation) {
                vec.mulDirection(localSpace);
            }
            if (preset.localPosition) {
                vec.add(
                        (float) Mth.lerp(partialTick, posO.x, pos.x),
                        (float) Mth.lerp(partialTick, posO.y, pos.y),
                        (float) Mth.lerp(partialTick, posO.z, pos.z)
                );
            }
        }
    }

    public final boolean isLocalSpace() {
        return localSpace != null;
    }

    public final Matrix4x3f getLocalSpace() {
        return localSpace;
    }

    public final void setLocalSpace(@Nullable Matrix4x3f space) {
        setLocalSpace(space, true);
    }

    public final void setLocalSpace(@Nullable Matrix4x3f space, boolean updatePos) {
        this.localSpace = space;
        if (updatePos) {
            updatePos(getX(), getY(), getZ());
        }
    }

    public void remove() {
        this.removed = true;
    }

    @Contract(value = "true -> !null", pure = true)
    public @Nullable List<ParticleEmitter> getChildren(boolean create) {
        if (create && children == null) {
            this.children = new ArrayList<>();
        }
        return children;
    }

    public void onRemove() {
        if (children != null) {
            children.removeIf(child -> {
                child.parent = null;
                child.remove();
                return true;
            });
        }
        if (preset != null && preset.lifetimeEvents != null) {
            preset.lifetimeEvents.onExpiration(this);
        }
    }

    public void addParent(ParticleEmitter parent) {
        parent.getChildren(true).add(this);
        this.parent = parent;
    }

    public boolean isRemoved() {
        return removed || (attached != null && attached.isRemoved()) || (attachedBlock != null && attachedBlock.isRemoved());
    }

    public void setPos(Vec3 pos) {
        this.pos = pos;
    }

    public EmitterPreset getPreset() {
        return preset;
    }

    public void deserialize(CompoundTag tag) {
        this.particleId = ResourceLocation.parse(tag.getString("particleId"));
        this.expression = new MolangExp(tag.getString("expression"));
        this.emitterRandom1 = tag.getFloat("emitterRandom1");
        this.emitterRandom2 = tag.getFloat("emitterRandom2");
        this.emitterRandom3 = tag.getFloat("emitterRandom3");
        this.emitterRandom4 = tag.getFloat("emitterRandom4");
        this.posO = this.pos = new Vec3(tag.getDouble("posX"), tag.getDouble("posY"), tag.getDouble("posZ"));
        this.hideOutline = tag.getBoolean("hideOutline");
    }

    public final CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString(TYPE_KEY, type.toString());
        serialize(tag);
        return tag;
    }

    public void serialize(CompoundTag tag) {
        tag.putString("particleId", particleId.toString());
        tag.putString("expression", expression.getExpStr());
        tag.putDouble("emitterRandom1", emitterRandom1);
        tag.putDouble("emitterRandom2", emitterRandom2);
        tag.putDouble("emitterRandom3", emitterRandom3);
        tag.putDouble("emitterRandom4", emitterRandom4);
        tag.putDouble("posX", pos.x);
        tag.putDouble("posY", pos.y);
        tag.putDouble("posZ", pos.z);
        tag.putBoolean("hideOutline", hideOutline);
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
    public float getRandom1() {
        return emitterRandom1;
    }

    @Override
    public float getRandom2() {
        return emitterRandom2;
    }

    @Override
    public float getRandom3() {
        return emitterRandom3;
    }

    @Override
    public float getRandom4() {
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
}

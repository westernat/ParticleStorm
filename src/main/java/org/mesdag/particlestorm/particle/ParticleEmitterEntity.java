package org.mesdag.particlestorm.particle;

import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.mesdag.particlestorm.GameClient;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.data.component.EmitterRate;
import org.mesdag.particlestorm.data.component.IEmitterComponent;
import org.mesdag.particlestorm.data.molang.MolangData;
import org.mesdag.particlestorm.data.molang.VariableTable;
import org.mesdag.particlestorm.network.EmitterManualPacketC2S;

import java.util.Collection;
import java.util.List;

public class ParticleEmitterEntity extends Entity implements MolangData {
    private static final EntityDataAccessor<String> DATA_PARTICLE_ID = SynchedEntityData.defineId(ParticleEmitterEntity.class, EntityDataSerializers.STRING);
    public ManualData manualData;
    public ResourceLocation particleId;
    protected boolean haveHadSync = false;

    protected EmitterDetail detail;
    protected VariableTable variableTable;
    protected List<IEmitterComponent> components;

    protected double emitterRandom1 = 0.0;
    protected double emitterRandom2 = 0.0;
    protected double emitterRandom3 = 0.0;
    protected double emitterRandom4 = 0.0;

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

    // Client Only
    public ParticleEmitterEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    // Server Only
    public ParticleEmitterEntity(Level level, ManualData manualData, ResourceLocation particleId, Vec3 pos) {
        super(ParticleStorm.PARTICLE_EMITTER.get(), level);
        this.manualData = manualData;
        this.particleId = particleId;
        setPos(pos);
        this.emitterRandom1 = level.random.nextDouble();
        this.emitterRandom2 = level.random.nextDouble();
        this.emitterRandom3 = level.random.nextDouble();
        this.emitterRandom4 = level.random.nextDouble();
    }

    public EmitterDetail getDetail() {
        return detail;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (haveHadSync) {
                if (detail.emitterRateType == EmitterRate.Type.MANUAL) {
                    PacketDistributor.sendToServer(new EmitterManualPacketC2S(getId(), 0));
                    return;
                }
                for (IEmitterComponent component : components) {
                    component.update(this);
                }
                this.age++;
            } else {
                this.detail = GameClient.LOADER.ID_2_EMITTER.get(ResourceLocation.parse(entityData.get(DATA_PARTICLE_ID)));
                this.variableTable = new VariableTable(detail.variableTable);
                detail.assignments.forEach(assignment -> {
                    // 重定向，防止污染变量表
                    variableTable.setValue(assignment.variable().name(), assignment.variable());
                });
                this.components = detail.components.stream().filter(e -> {
                    e.apply(this);
                    return e.requireUpdate();
                }).toList();
                this.haveHadSync = true;
            }
        } else if (!haveHadSync) {
            entityData.set(DATA_PARTICLE_ID, particleId.toString());
            this.haveHadSync = true;
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_PARTICLE_ID, "snowstorm:rainbow");
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.particleId = ResourceLocation.parse(compound.getString("particleId"));
        this.emitterRandom1 = compound.getDouble("emitterRandom1");
        this.emitterRandom2 = compound.getDouble("emitterRandom2");
        this.emitterRandom3 = compound.getDouble("emitterRandom3");
        this.emitterRandom4 = compound.getDouble("emitterRandom4");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putString("particleId", particleId.toString());
        compound.putDouble("emitterRandom1", emitterRandom1);
        compound.putDouble("emitterRandom2", emitterRandom2);
        compound.putDouble("emitterRandom3", emitterRandom3);
        compound.putDouble("emitterRandom4", emitterRandom4);
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public VariableTable getVariableTable() {
        return variableTable;
    }

    @Override
    public Level getLevel() {
        return level();
    }

    @Override
    public int getAge() {
        return age;
    }

    @Override
    public int getLifetime() {
        return lifetime;
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

    public record ManualData(ServerLevel serverLevel, MolangParticleOption particleData, Vec3 pos, Vec3 delta, float speed, int count, boolean force, Collection<ServerPlayer> viewers) {
        public void doSendParticle(int count) {
            int actually = count == 0 ? this.count : count;
            for (ServerPlayer serverplayer : viewers) {
                serverLevel.sendParticles(serverplayer, particleData, force, pos.x, pos.y, pos.z, actually, delta.x, delta.y, delta.z, speed);
            }
        }
    }
}

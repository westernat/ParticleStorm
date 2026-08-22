package org.mesdag.particlestorm.particle.attach;

import com.mojang.datafixers.util.Function3;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectBooleanImmutablePair;
import it.unimi.dsi.fastutil.objects.ObjectBooleanPair;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModLoader;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.PSClientConfigs;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.AttachEmitterToBlockEvent;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.mesdag.particlestorm.particle.ParticleEmitter;

import java.util.*;

public final class EmitterAttachHandler {
    public static final Map<BlockPos, ObjectBooleanPair<WithBlockParticleEmitter>> attachedToBlockEmitters = new Object2ObjectOpenHashMap<>(64);
    public static final Queue<IgnoreRangeParticleEmitter> ignoreRangeEmitters = new ArrayDeque<>(64);

    private EmitterAttachHandler() {}

    private static final Map<BlockState, AttachData> stateMap = new Object2ObjectOpenHashMap<>();
    private static final Map<Block, AttachData> blockMap = new Object2ObjectOpenHashMap<>();

    public static void postEvent() {
        if (stateMap.isEmpty()) {
            ModLoader.get().postEvent(new AttachEmitterToBlockEvent(stateMap, blockMap));
        }
    }

    public static boolean attachTo(Block block, BlockState state, Level level, BlockPos pos) {
        if (!ableToAddEmitter()) {
            ObjectBooleanPair<WithBlockParticleEmitter> pair = attachedToBlockEmitters.remove(pos);
            if (pair != null) {
                pair.left().remove();
            }
            return PSClientConfigs.allowsVanillaParticleWhenReachLimit;
        }
        ObjectBooleanPair<WithBlockParticleEmitter> pair = attachedToBlockEmitters.get(pos);
        if (pair == null) {
            AttachData data = blockMap.get(block);
            if (data == null && (data = stateMap.get(state)) == null) return true;
            WithBlockParticleEmitter emitter = data.apply(level, pos, state);
            if (emitter == null) return data.allowsVanilla;
            MolangParticleEngine.INSTANCE.addEmitter(emitter);
            attachedToBlockEmitters.put(pos.immutable(), pair = new ObjectBooleanImmutablePair<>(emitter, data.allowsVanilla));
        }
        return pair.rightBoolean();
    }

    public static void tick(Camera camera) {
        if (!attachedToBlockEmitters.isEmpty()) {
            Iterator<ObjectBooleanPair<WithBlockParticleEmitter>> iterator = attachedToBlockEmitters.values().iterator();
            while (iterator.hasNext()) {
                WithBlockParticleEmitter emitter = iterator.next().left();
                if (shouldRemoveEmitter(camera, emitter)) {
                    emitter.remove();
                    iterator.remove();
                }
            }
        }
        if (!ignoreRangeEmitters.isEmpty()) {
            Iterator<IgnoreRangeParticleEmitter> iterator = ignoreRangeEmitters.iterator();
            while (iterator.hasNext()) {
                IgnoreRangeParticleEmitter emitter = iterator.next();
                List<ParticleEmitter> children = emitter.getChildren(false);
                if (children != null) {
                    for (ParticleEmitter child : children) {
                        if (child instanceof IgnoreRangeParticleEmitter irpe && shouldRemoveEmitter(camera, irpe)) {
                            child.remove();
                        }
                    }
                }
                if (shouldRemoveEmitter(camera, emitter)) {
                    emitter.remove();
                    iterator.remove();
                }
            }
        }
    }

    public static boolean addEmitter(Level level, Vec3 pos, ResourceLocation particle, Variable... variables) {
        if (ableToAddEmitter()) {
            PresetVarsParticleEmitter emitter = new PresetVarsParticleEmitter(level, pos, particle, false, variables);
            MolangParticleEngine.INSTANCE.addEmitter(emitter);
            ignoreRangeEmitters.add(emitter);
            return false;
        }
        return PSClientConfigs.allowsVanillaParticleWhenReachLimit;
    }

    public static boolean ableToAddEmitter() {
        return Minecraft.fps > PSClientConfigs.fpsThreshold &&
                attachedToBlockEmitters.size() < PSClientConfigs.emitterLimit;
    }

    public static boolean shouldRemoveEmitter(Camera camera, IgnoreRangeParticleEmitter emitter) {
        if (emitter.isRemoved()) return true;
        if (emitter.ignoreRange) return false;
        return isFarAwayFromCamera(camera, emitter);
    }

    public static boolean isFarAwayFromCamera(Camera camera, IgnoreRangeParticleEmitter emitter) {
        double v = camera.getPosition().distanceToSqr(emitter.getPosition());
        if (v < Mth.square(PSClientConfigs.emitterAutoRemoveMinimumDistance)) return false;
        v = Math.sqrt(v) - PSClientConfigs.emitterAutoRemoveMinimumDistance;
        double c = 0;
        do {
            c += PSClientConfigs.emitterAutoRemoveAttenuationCoefficient;
            if (emitter.level.random.nextDouble() < c) {
                return true;
            }
            v -= PSClientConfigs.emitterAutoRemoveAttenuationDistance;
        } while (v > 0 && c < 1);
        return false;
    }

    public static void clearEmitters() {
        attachedToBlockEmitters.clear();
    }

    public static class AttachData implements Function3<Level, BlockPos, BlockState, @Nullable WithBlockParticleEmitter> {
        public boolean disabled = false;
        public final ResourceLocation particleId;
        public final Function3<Level, BlockPos, BlockState, MolangExp> expression;
        public final boolean ignoreSameBlock;
        public final boolean allowsVanilla;
        public final boolean ignoreRange;

        public AttachData(ResourceLocation particleId, Function3<Level, BlockPos, BlockState, MolangExp> expression, boolean ignoreSameBlock, boolean allowsVanilla, boolean ignoreRange) {
            this.particleId = particleId;
            this.expression = expression;
            this.ignoreSameBlock = ignoreSameBlock;
            this.allowsVanilla = allowsVanilla;
            this.ignoreRange = ignoreRange;
        }

        public AttachData(ResourceLocation particleId, MolangExp expression, boolean ignoreSameBlock, boolean allowsVanilla, boolean ignoreRange) {
            this(particleId, (level, pos, state) -> expression, ignoreSameBlock, allowsVanilla, ignoreRange);
        }

        /// Returns null means skip add emitter
        @Override
        public @Nullable WithBlockParticleEmitter apply(Level level, BlockPos pos, BlockState state) {
            if (disabled) return null;
            return new WithBlockParticleEmitter(level, pos.getCenter(), particleId, expression.apply(level, pos, state), ignoreSameBlock, ignoreRange);
        }

        public static class Wrapped extends AttachData {
            private final static ResourceLocation defaultParticle = ParticleStorm.asResource("blend");

            private final Function3<Level, BlockPos, BlockState, @Nullable WithBlockParticleEmitter> factory;

            public Wrapped(Function3<Level, BlockPos, BlockState, @Nullable WithBlockParticleEmitter> factory, boolean ignoreSameBlock, boolean allowsVanilla, boolean ignoreRange) {
                super(defaultParticle, MolangExp.EMPTY, ignoreSameBlock, allowsVanilla, ignoreRange);
                this.factory = factory;
            }

            @Override
            public @Nullable WithBlockParticleEmitter apply(Level level, BlockPos pos, BlockState state) {
                return factory.apply(level, pos, state);
            }
        }
    }
}

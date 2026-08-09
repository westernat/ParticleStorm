package org.mesdag.particlestorm.api;

import com.mojang.datafixers.util.Function3;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.data.molang.MolangExp;
import org.mesdag.particlestorm.particle.attach.EmitterAttachHandler;
import org.mesdag.particlestorm.particle.attach.WithBlockParticleEmitter;

import java.util.Map;

public class AttachEmitterToBlockEvent extends Event implements IModBusEvent {
    private final Map<BlockState, EmitterAttachHandler.AttachData> stateMap;
    private final Map<Block, EmitterAttachHandler.AttachData> blockMap;

    @ApiStatus.Internal
    public AttachEmitterToBlockEvent(Map<BlockState, EmitterAttachHandler.AttachData>stateMap, Map<Block, EmitterAttachHandler.AttachData> blockMap) {
        this.stateMap = stateMap;
        this.blockMap = blockMap;
    }

    public EmitterAttachHandler.AttachData attach(BlockState state, boolean allowsVanilla, Function3<Level, BlockPos, BlockState, @Nullable WithBlockParticleEmitter> factory, boolean ignoreRange) {
        EmitterAttachHandler.AttachData data = new EmitterAttachHandler.AttachData.Wrapped(factory, false, allowsVanilla, ignoreRange);
        stateMap.put(state, data);
        return data;
    }

    public EmitterAttachHandler.AttachData attach(Block block, boolean allowsVanilla, Function3<Level, BlockPos, BlockState, @Nullable WithBlockParticleEmitter> factory, boolean ignoreRange) {
        EmitterAttachHandler.AttachData data = new EmitterAttachHandler.AttachData.Wrapped(factory, true, allowsVanilla, ignoreRange);
        blockMap.put(block, data);
        return data;
    }

    public EmitterAttachHandler.AttachData attach(BlockState state, ResourceLocation particleId, MolangExp expression, boolean allowsVanilla, boolean ignoreRange) {
        EmitterAttachHandler.AttachData data = new EmitterAttachHandler.AttachData(particleId, expression, false, allowsVanilla, ignoreRange);
        stateMap.put(state, data);
        return data;
    }

    public EmitterAttachHandler.AttachData attach(BlockState state, ResourceLocation particleId, Function3<Level, BlockPos, BlockState, MolangExp> expression, boolean allowsVanilla, boolean ignoreRange) {
        EmitterAttachHandler.AttachData data = new EmitterAttachHandler.AttachData(particleId, expression, false, allowsVanilla, ignoreRange);
        stateMap.put(state, data);
        return data;
    }

    public EmitterAttachHandler.AttachData attach(Block block, ResourceLocation particleId, MolangExp expression, boolean allowsVanilla, boolean ignoreRange) {
        EmitterAttachHandler.AttachData data = new EmitterAttachHandler.AttachData(particleId, expression, true, allowsVanilla, ignoreRange);
        blockMap.put(block, data);
        return data;
    }

    public EmitterAttachHandler.AttachData attach(Block block, ResourceLocation particleId, Function3<Level, BlockPos, BlockState, MolangExp> expression, boolean allowsVanilla, boolean ignoreRange) {
        EmitterAttachHandler.AttachData data = new EmitterAttachHandler.AttachData(particleId, expression, true, allowsVanilla, ignoreRange);
        blockMap.put(block, data);
        return data;
    }
}

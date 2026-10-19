package org.mesdag.particlestorm.particle.screen;

import org.mesdag.particlestorm.particle.MolangParticleManager;

/// 用于[net.minecraft.client.gui.screens.Screen]，其使用的粒子发射器必须继承自[ScreenParticleEmitter]
public interface IParticleManagerScreen {
    MolangParticleManager getManager();
}

package org.mesdag.particlestorm.api;

import net.minecraft.client.particle.ParticleRenderType;

/// 标记一个颜色 pass 不写深度的 {@link ParticleRenderType}。
///
/// `particles_blend` / `particles_add` 这类软边半透明材质在 `begin` 里会 `depthMask(false)`，
/// 好处是粒子之间不会互相深度裁切；代价是深度缓冲里没有它们——之后绘制的 pass（原版云层、
/// Fabulous 的半透明分层合成）不知道那里有东西，远处的云会直接盖在近处粒子上。
///
/// 实现该接口后，{@link org.mesdag.particlestorm.particle.MolangParticleManager} 会在颜色 pass
/// 之后再补一遍**仅深度** pass（`colorMask(false)` + `depthMask(true)` + 原版粒子着色器的
/// `alpha < 0.1` discard），把最近一层的深度写进缓冲，供后续 pass 正确裁剪。
///
/// 仅深度 pass 放在颜色 pass 之后：这批粒子画完才写深度，不会回头裁掉它们自己。
public interface IDepthPrepassParticleType extends ParticleRenderType {
}

package org.mesdag.particlestorm.editor;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrays;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.mesdag.particlestorm.ParticleStorm;
import org.mesdag.particlestorm.api.IComponent;
import org.mesdag.particlestorm.api.IMolangParticleInstance;
import org.mesdag.particlestorm.api.RegisterCustomParticleTypeEvent;
import org.mesdag.particlestorm.data.DefinedParticleEffect;
import org.mesdag.particlestorm.data.component.*;
import org.mesdag.particlestorm.data.description.DescriptionMaterial;
import org.mesdag.particlestorm.data.description.DescriptionParameters;
import org.mesdag.particlestorm.data.description.ParticleDescription;
import org.mesdag.particlestorm.data.molang.*;
import org.mesdag.particlestorm.data.molang.compiler.value.Variable;
import org.mesdag.particlestorm.particle.MolangParticleEngine;
import org.mesdag.particlestorm.particle.MolangParticleManager;
import org.mesdag.particlestorm.particle.ParticleEmitter;
import org.mesdag.particlestorm.particle.screen.IParticleManagerScreen;

import java.util.*;
import java.util.function.Consumer;

/// 粒子编辑器主界面（当前阶段：场景预览 + 基本相机操作 + 左侧工具框）。
///
/// 场景里的所有发射器都由 [MolangParticleManager] 管理，本界面**不持有任何 `ParticleEmitter` 字段**：
/// 打开时往 manager 里放一个默认发射器（`snowstorm:loading`），之后可以通过顶部工具栏的
/// 「Loaded」下拉切换/添加已加载的其它粒子；推进/渲染/统计都是 manager 级别的操作。
///
/// 左侧工具框是组件 / 事件 / 曲线 / 描述的编辑面板：每个页签是一个 [EditorTab]，
/// 页签栏由本类绘制，内容区交给当前页签。
///
/// 渲染注意点：本界面在 **GUI pass** 里绘制，默认为 GUI 的正交像素投影 + GUI 平移 model-view。
/// 粒子顶点是**相机相对坐标**，所以绘制前要把 RenderSystem 的投影换成编辑器正交投影、
/// model-view 换成相机视图矩阵（`rotation(conjugate(camera.rotation()))`），画完再恢复。
public class EditorScreen extends Screen implements IParticleManagerScreen {
    /// 打开编辑器时默认发射的粒子（`loading.particle.json` 里定义的效果）。
    private static final ResourceLocation DEFAULT_PARTICLE = ResourceLocation.fromNamespaceAndPath("snowstorm", "loading");
    /// 描述页在页签列表里的下标（`tabs` = 描述 / 组件 / 事件 / 曲线）。
    private static final int DESCRIPTION_TAB = 0;
    /// 标准正交视角。
    private static final float STANDARD_YAW = 45.0F;
    private static final float STANDARD_PITCH = 30.0F;
    /// 正交视口在 zoom = 1 时的半高（世界单位）。
    private static final float BASE_HALF_HEIGHT = 4.0F;
    private static final float MIN_ZOOM = 0.25F;
    private static final float MAX_ZOOM = 8.0F;
    /// 相机只用于近/远裁剪与朝向，不参与缩放。
    private static final double CAMERA_DISTANCE = 64.0D;

    private static final float ORBIT_SENSITIVITY = 0.8F;
    private static final float PITCH_LIMIT = 89.0F;

    private static final int TOOLBAR_HEIGHT = 20;
    /// 工具栏最左边的「新建」按钮；Loaded / Scene 依次右移。
    private static final int NEW_X = 4;
    private static final int TOOLBAR_BUTTON_Y = 2;
    private static final int TOOLBAR_BUTTON_HEIGHT = 16;
    private static final int TOOLBAR_ROW_HEIGHT = 12;
    /// 按钮左右内边距之和（4 + 4）。
    private static final int BUTTON_PADDING = 8;
    /// 下拉按钮右侧箭头占位。
    private static final int DROPDOWN_ARROW_AREA = 14;
    /// 工具栏按钮之间的间距。
    private static final int BUTTON_GAP = 6;
    /// 工具栏「⏸ / ▶」暂停按钮的宽度（图标按钮，固定方形）。
    private static final int PAUSE_BUTTON_WIDTH = 18;

    /// 场景网格：1m 一格，总共 16m × 16m（以场景原点为中心）。
    private static final int GRID_RADIUS = 8;
    private static final int AXIS_LENGTH = 1;

    /// 左侧工具框：组件 / 事件 / 曲线 / 描述 的编辑面板，宽度为屏幕宽度的 1/3（场景占右侧 2/3）。
    /// 每个页签是一个 [EditorTab]：页签栏由本类绘制，内容区交给当前页签负责。

    private static final int DRAG_NONE = -1;
    private static final int DRAG_ORBIT = 0;
    private static final int DRAG_PAN = 1;

    MolangParticleManager manager;
    private PreviewCamera camera;
    private Frustum frustum;

    private Vec3 focus = Vec3.ZERO;
    /// 场景原点 = 初始焦点；网格锚定在这里，平移焦点时网格不跟着动。
    private Vec3 sceneOrigin = Vec3.ZERO;
    private float yaw = STANDARD_YAW;
    private float pitch = STANDARD_PITCH;
    private float zoom = 1.0F;

    private int dragging = DRAG_NONE;
    private List<ResourceLocation> particleIds = List.of();
    @Nullable ResourceLocation selectedParticle;
    /// 编辑器缓存的完整效果副本（当前编辑中的文档）；修改只在编辑器内生效，暂不写回引擎。
    @Nullable DefinedParticleEffect effect;
    private boolean gridVisible = false;
    /// 场景原点处的 XYZ 红绿蓝轴线段开关。
    private boolean axesVisible = true;
    /// 渲染场景原点附近的方块（区域大小以方块为单位，可在 Scene 下拉里改）。
    private boolean blocksVisible = true;
    private int blockSizeX = 16;
    private int blockSizeY = 16;
    private int blockSizeZ = 16;
    /// 正在编辑的方块区域尺寸（0/1/2 = x/y/z）；对应行上临时盖一个原版 EditBox。
    @Nullable
    private EditBox sceneSizeBox;
    private int sceneSizeAxis = -1;
    /// 工具框页签（描述 / 组件 / 事件 / 曲线），当前页签由 [EditorTab] 自己负责内容区。
    private List<EditorTab> tabs = List.of();
    private int activeTabIndex;
    private DropdownWidget loadedDropdown;
    private DropdownWidget sceneDropdown;
    /// 本次 overlay 渲染中，悬停在被截断文本上时要显示的完整文本。
    @Nullable Component hoveredTooltip;
    /// 悬停在被截断的 Molang 文本上时显示的完整文本（用 [MolangSyntax] 着色绘制）。
    @Nullable String highlightedTooltip;
    @Nullable Component error;
    /// 场景暂停开关（工具栏「⏸ / ▶」）：暂停时不推进 manager，粒子冻结在当前位置。
    boolean playing = true;
    @Nullable Float pausePartialTick;

    // region 发射器变量表（顶部工具栏「变量」工具）

    private static final int VAR_ROW_HEIGHT = 16;
    private static final int VAR_KEY_WIDTH = 112;
    private static final String VARIABLE_PREFIX = "variable.";

    /// 变量表面板里的行：`[变量名] = [值/molang 表达式]`。
    /// `key` 统一规范化为 `variable.<名字>`；`expr` 为空表示「不覆盖」，值是发射器变量表的实时值。
    private final List<VariableRow> variableRows = new ArrayList<>();
    private boolean variablesOpen;
    private int variablePanelScroll;
    private int variablePanelX;
    private int variablePanelY;
    private int variablePanelWidth;
    private int variablePanelHeight;
    /// 正在编辑的变量行 + 字段（0 = 名字，1 = 值）；同一时间只编辑一个。
    @Nullable
    private VariableRow editingVariable;
    private int editingVariableField = -1;

    /// 纹理图集查看器（描述页签「texture」行点开，盖住大部分屏幕）；null = 未打开。
    @Nullable
    private AtlasViewer atlasViewer;

    // endregion

    /// 工具框宽度：屏幕宽度的 1/3（场景占剩下的 2/3）。
    int toolboxWidth() {
        return Math.max(120, this.width / 3);
    }

    protected EditorScreen() {
        super(Component.translatable("particle_editor.title"));
    }

    @Override
    public MolangParticleManager getManager() {
        return manager;
    }

    @Override
    protected void init() {
        // 编辑器专属的绘制顺序：半透明粒子按相机距离从远到近排序（alpha 混合有顺序要求），
        // 世界渲染保持 MolangParticleManager 默认的队列顺序。
        this.manager = new MolangParticleManager(MolangParticleEngine.INSTANCE.allocator) {
            @Override
            protected Iterator<IMolangParticleInstance> orderForRender(ParticleRenderType type, Queue<IMolangParticleInstance> queue, Camera camera, float partialTick) {
                return EditorScreen.this.sortedForRender(type, queue, camera, partialTick);
            }
        };
        this.camera = new PreviewCamera();
        this.frustum = new Frustum(new Matrix4f(), new Matrix4f());
        this.loadedDropdown = new DropdownWidget(this.loadedButtonX(), TOOLBAR_HEIGHT, this.loadedButtonWidth(), TOOLBAR_ROW_HEIGHT);
        this.sceneDropdown = new DropdownWidget(this.sceneButtonX(), TOOLBAR_HEIGHT, this.sceneButtonWidth(), TOOLBAR_ROW_HEIGHT);
        this.tabs = List.of(
                new DescriptionTab(this, this.font),
                new ComponentsTab(this, this.font),
                new EventsTab(this, this.font),
                new CurvesTab(this, this.font)
        );

        List<ResourceLocation> ids = new ArrayList<>(MolangParticleEngine.INSTANCE.id2Effect().keySet());
        ids.sort(Comparator.comparing(ResourceLocation::toString));
        this.particleIds = ids;

        this.resetView();
        // 先缓存效果，再发射
        this.effect = MolangParticleEngine.INSTANCE.id2Effect().get(DEFAULT_PARTICLE);
        this.spawnEmitter(DEFAULT_PARTICLE);
    }

    @Override
    public void tick() {
        // 暂停时不推进：粒子/发射器年龄都冻结，恢复后从暂停点继续
        if (!this.playing) {
            return;
        }
        this.manager.tick(this.minecraft, this.minecraft.player);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 世界照常渲染，但编辑器整屏铺一层**不透明纯黑**底色把它盖掉。
        // 注意：GUI pass 不会给 Screen 重置混合状态，画这个黑底时用的是「上一帧/世界渲染」留下的
        // 混合函数——如果停在叠加混合（例如粒子用 particles_add 材质，blendFunc = SRC_ALPHA, ONE），
        // 黑色 quad 在叠加混合下等于「什么都不加」，世界就会直接从编辑器背景里透出来。
        // 所以先把状态复位成 GUI 默认的 alpha 混合，再铺底。
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.fill(0, 0, this.width, this.height, 0xFF000000);
        // 场景只画在工具框右侧的 2/3 视口里（工具框/工具栏的底色是半透明的，
        // 不裁剪的话粒子会透到左侧栏和工具栏下面）
        int viewLeft = this.toolboxWidth();
        // 图集查看器打开时场景被完全盖住：这一遍直接跳过（省掉整帧的粒子/方块渲染）
        if (this.atlasViewer == null) {
            guiGraphics.enableScissor(viewLeft, TOOLBAR_HEIGHT, this.width, this.height);
            try {
                this.renderScene(partialTick);
            } finally {
                guiGraphics.disableScissor();
            }
            // 场景 pass 写的是编辑器自己的正交深度，和 GUI 的深度不在同一投影里：
            // 不清掉的话工具栏下拉、工具框这些 GUI 元素在方块像素上会被深度测试挡掉（看起来像被方块盖住）。
            RenderSystem.clear(256, Minecraft.ON_OSX);
        }
        // 场景 pass 会把混合模式换成粒子材质用的那套（PARTICLE_ADD 是 SRC_ALPHA/ONE，且不会还原），
        // 之后画 GUI 的元素（半透明底、图集贴图）就会变成叠加混合：这里把 GUI 的默认状态复位。
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        this.renderOverlay(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen() {
        // 世界继续跑：发射器/粒子靠客户端 tick 推进
        return false;
    }

    // region 相机与场景

    /// 重置为标准正交视角：焦点归到玩家所在坐标（同时作为场景原点），yaw/pitch 取标准 3/4 角，zoom 归 1。
    private void resetView() {
        this.yaw = STANDARD_YAW;
        this.pitch = STANDARD_PITCH;
        this.zoom = 1.0F;
        this.focus = this.minecraft.player.position();
        this.sceneOrigin = this.focus;
        this.updateCamera();
    }

    private void updateCamera() {
        this.camera.orbit(this.focus, this.yaw, this.pitch, CAMERA_DISTANCE);
    }

    /// 用当前缓存的 [EditorScreen#effect] 发射一个发射器（目标明确，直接走 manager）。
    ///
    /// 调用方负责先更新缓存再清场景；id 只在编辑器文档里使用（identifier 可被编辑），不再要求出现在引擎数据里。
    /// 发射位置固定用**场景原点**（[#sceneOrigin]）：相机平移只移动视角，重播的粒子不能跟着跑到屏幕中心。
    void spawnEmitter(ResourceLocation particle) {
        try {
            this.ensureSpriteRegistered();
            this.manager._addEmitter(new EditorParticleEmitter(this.minecraft.level, this.sceneOrigin, particle, MolangExp.EMPTY,
                    this.effect, this.variablesMap()));
            this.selectedParticle = particle;
            this.error = null;
        } catch (RuntimeException e) {
            this.error = Component.literal(e.getMessage());
            ParticleStorm.LOGGER.warn("Editor failed to spawn emitter for {}", particle, e);
        }
    }

    /// 编辑器预览的绘制顺序：半透明粒子按相机距离从远到近排序。
    ///
    /// 每个粒子的相机距离只算一次（O(n)），排序用 `IntArrays.quickSort` 的 int 比较器，只比较 double、不装箱；
    /// 返回惰性迭代器（按排好的 `int[]` 下标取 `instances`），避免每帧再拷贝一个 ArrayList。
    private Iterator<IMolangParticleInstance> sortedForRender(ParticleRenderType type, Queue<IMolangParticleInstance> queue, Camera camera, float partialTick) {
        if (!type.isTranslucent() || queue.size() < 2) {
            return queue.iterator();
        }
        Object[] instances = queue.toArray();
        double[] keys = new double[instances.length];
        int[] order = new int[instances.length];
        Vec3 cameraPos = camera.getPosition();
        Vector3f scratch = new Vector3f();
        for (int i = 0; i < instances.length; i++) {
            order[i] = i;
            IMolangParticleInstance instance = (IMolangParticleInstance) instances[i];
            instance.getWorldPosition(scratch, partialTick);
            keys[i] = -Mth.lengthSquared(scratch.x - cameraPos.x, scratch.y - cameraPos.y, scratch.z - cameraPos.z);
        }
        IntArrays.quickSort(order, (a, b) -> Double.compare(keys[a], keys[b]));
        return new Iterator<>() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < order.length;
            }

            @Override
            public IMolangParticleInstance next() {
                return (IMolangParticleInstance) instances[order[index++]];
            }
        };
    }

    /// 用正交投影 + 相机视图矩阵绘制场景里的粒子（场景视口 = 左侧工具框右边的 2/3 区域）。
    private void renderScene(float partialTick) {
        this.updateCamera();

        int viewLeft = this.toolboxWidth();
        int viewTop = TOOLBAR_HEIGHT;
        int viewWidth = Math.max(1, this.width - viewLeft);
        int viewHeight = Math.max(1, this.height - viewTop);
        float halfHeight = BASE_HALF_HEIGHT / this.zoom;
        // 正交投影的 NDC 始终映射到整个窗口，所以 aspect 必须用屏幕宽高比（保持方形像素），
        // 视口只是靠 scissor 裁出来的子区域；用视口宽高比会把场景横向压扁。
        float aspect = (float) Math.max(1, this.width) / (float) Math.max(1, this.height);
        float halfWidth = halfHeight * aspect;

        // 把投影中心平移到视口中心（NDC 原点 → 视口中心），场景就只出现在右 2/3
        float viewCenterX = viewLeft + viewWidth * 0.5F;
        float viewCenterY = viewTop + viewHeight * 0.5F;
        float offsetX = halfWidth * (this.width - 2.0F * viewCenterX) / this.width;
        float offsetY = halfHeight * (2.0F * viewCenterY - this.height) / this.height;
        Matrix4f projection = new Matrix4f().setOrtho(
                -halfWidth + offsetX, halfWidth + offsetX,
                -halfHeight + offsetY, halfHeight + offsetY,
                0.05F, 2048.0F);
        Matrix4f view = new Matrix4f().rotation(this.camera.rotation().conjugate(new Quaternionf()));

        this.frustum = new Frustum(view, projection);
        Vec3 cameraPos = this.camera.getPosition();
        this.frustum.prepare(cameraPos.x, cameraPos.y, cameraPos.z);

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(projection, VertexSorting.ORTHOGRAPHIC_Z);
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.identity();
        modelViewStack.mul(view);
        RenderSystem.applyModelViewMatrix();
        // renderParticles 原本是注入进原版 ParticleEngine#render 里用的，深度测试由那段原版逻辑开启；
        // GUI pass 里没有这层环境，不补的话粒子之间不参与深度测试，会互相穿透出现“视觉错觉”。
        RenderSystem.enableDepthTest();
        try {
            if (this.gridVisible) {
                this.renderGrid(cameraPos);
            }
            if (this.blocksVisible) {
                // 方块必须先写深度：后面的粒子要靠它做深度测试，被方块挡住的部分就不画
                RenderSystem.depthMask(true);
                Lighting.setupLevel();
                this.renderBlocks(cameraPos);
            }
            // 粒子参与深度测试但不写深度（半透明混合），所以会被前面的方块正常遮挡
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            this.manager.renderParticles(
                    this.minecraft.gameRenderer.lightTexture(),
                    this.minecraft.getTextureManager(),
                    this.camera,
                    this.playing ? partialTick : (pausePartialTick == null ? pausePartialTick = partialTick : pausePartialTick),
                    this.frustum,
                    type -> true
            );
            // 坐标轴最后画、关掉深度测试 → 透视方块和粒子
            if (this.axesVisible) {
                this.renderAxes(cameraPos);
            }
        } finally {
            // 与 ParticleEngine#render 收尾一致：把 depthMask 还给 GUI 的默认状态
            RenderSystem.depthMask(true);
            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
        }
    }

    /// 场景网格：锚定在场景原点的 XZ 平面网格（1m 一格、总共 16m）。
    private void renderGrid(Vec3 cameraPos) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        double y = this.sceneOrigin.y;
        double minX = this.sceneOrigin.x - GRID_RADIUS;
        double maxX = this.sceneOrigin.x + GRID_RADIUS;
        double minZ = this.sceneOrigin.z - GRID_RADIUS;
        double maxZ = this.sceneOrigin.z + GRID_RADIUS;
        if (this.gridVisible) {
            for (int i = -GRID_RADIUS; i <= GRID_RADIUS; i++) {
                double x = this.sceneOrigin.x + i;
                double z = this.sceneOrigin.z + i;
                float shade = i == 0 ? 0.55F : 0.32F;
                this.gridLine(builder, cameraPos, x, y, minZ, x, y, maxZ, shade, shade, shade, 0.7F);
                this.gridLine(builder, cameraPos, minX, y, z, maxX, y, z, shade, shade, shade, 0.7F);
            }
        }
        // 网格原点处的 XYZ 轴线段（只画正方向，红 / 绿 / 蓝）在 renderAxes 里单独画（透视方块/粒子）

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        MeshData mesh = builder.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
        RenderSystem.depthMask(true);
    }

    /// 网格/轴线：世界坐标转相机相对坐标后写入线段。
    private void gridLine(BufferBuilder builder, Vec3 cameraPos, double x0, double y0, double z0, double x1, double y1, double z1,
                          float red, float green, float blue, float alpha) {
        builder.addVertex((float) (x0 - cameraPos.x), (float) (y0 - cameraPos.y), (float) (z0 - cameraPos.z)).setColor(red, green, blue, alpha);
        builder.addVertex((float) (x1 - cameraPos.x), (float) (y1 - cameraPos.y), (float) (z1 - cameraPos.z)).setColor(red, green, blue, alpha);
    }

    /// 场景原点的 XYZ 红绿蓝轴线段（只画正方向）。在方块/粒子之后绘制并关闭深度测试，所以能“透视”挡在前面的方块和粒子。
    private void renderAxes(Vec3 cameraPos) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        double ox = this.sceneOrigin.x;
        double oy = this.sceneOrigin.y;
        double oz = this.sceneOrigin.z;
        this.gridLine(builder, cameraPos, ox, oy, oz, ox + AXIS_LENGTH, oy, oz, 1.0F, 0.25F, 0.25F, 1.0F);
        this.gridLine(builder, cameraPos, ox, oy, oz, ox, oy + AXIS_LENGTH, oz, 0.25F, 1.0F, 0.25F, 1.0F);
        this.gridLine(builder, cameraPos, ox, oy, oz, ox, oy, oz + AXIS_LENGTH, 0.25F, 0.45F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        MeshData mesh = builder.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    /// 渲染场景原点附近的方块（区域 = `blockSizeX/Y/Z` 个方块，居中于 sceneOrigin）。
    private void renderBlocks(Vec3 cameraPos) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        PoseStack poseStack = new PoseStack();
        int originX = Mth.floor(this.sceneOrigin.x);
        int originY = Mth.floor(this.sceneOrigin.y);
        int originZ = Mth.floor(this.sceneOrigin.z);
        int halfX = this.blockSizeX / 2;
        int halfY = this.blockSizeY / 2;
        int halfZ = this.blockSizeZ / 2;
        for (int dx = -halfX; dx <= halfX; dx++) {
            for (int dy = -halfY; dy <= halfY; dy++) {
                for (int dz = -halfZ; dz <= halfZ; dz++) {
                    int x = originX + dx;
                    int y = originY + dy;
                    int z = originZ + dz;
                    pos.set(x, y, z);
                    BlockState state = this.minecraft.level.getBlockState(pos);
                    if (state.isAir()) continue;
                    poseStack.pushPose();
                    poseStack.translate(x - cameraPos.x, y - cameraPos.y, z - cameraPos.z);
                    boolean boundary = dx == -halfX || dx == halfX
                            || dy == -halfY || dy == halfY
                            || dz == -halfZ || dz == halfZ;
                    BlockScene.tesselateWithoutAO(this.minecraft, state, pos, poseStack.last(), !boundary);
                    poseStack.popPose();
                }
            }
        }
        this.minecraft.renderBuffers().bufferSource().endBatch();
    }

    // endregion

    // region 鼠标操作

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 图集查看器是模态浮层：打开时吞掉所有点击
        if (this.atlasViewer != null) {
            this.atlasViewer.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        // 按钮先处理（再次点击按钮 = 关闭它自己）；toggleMenu → closeMenus 会顺带收起页签里的编辑/下拉
        if (button == 0 && this.mouseOverNewButton(mouseX, mouseY)) {
            this.newEffect();
            return true;
        }
        if (button == 0 && this.mouseOverLoadedButton(mouseX, mouseY)) {
            this.toggleMenu(this.loadedDropdown);
            return true;
        }
        if (button == 0 && this.mouseOverSceneButton(mouseX, mouseY)) {
            this.toggleMenu(this.sceneDropdown);
            return true;
        }
        if (button == 0 && this.mouseOverReplayButton(mouseX, mouseY)) {
            this.replayFromStart();
            return true;
        }
        if (button == 0 && this.mouseOverPauseButton(mouseX, mouseY)) {
            this.playing = !this.playing;
            this.pausePartialTick = null;
            return true;
        }
        if (button == 0 && this.mouseOverVariablesButton(mouseX, mouseY)) {
            if (this.variablesOpen) {
                this.closeVariablesPanel();
            } else {
                this.closeMenus();
                this.openVariablesPanel();
            }
            return true;
        }
        if (button == 0 && this.variablesOpen) {
            if (this.mouseOverVariablePanel(mouseX, mouseY)) {
                this.onVariablesPanelClick(mouseX, mouseY);
                return true;
            }
            // 点在面板外（工具栏按钮 / 页签 / 场景）：收起面板，但这次点击继续按常规处理（比如切页签）
            this.closeVariablesPanel();
        }
        // 下拉展开时优先处理（列表从 y=20 开始，和左侧工具框区域重叠，否则点列表项会被工具框吃掉）
        if (button == 0 && this.loadedDropdown.isOpen()) {
            this.selectLoaded(this.loadedDropdown.hitTest(mouseX, mouseY));
            this.loadedDropdown.close();
            return true;
        }
        if (button == 0 && this.sceneDropdown.isOpen()) {
            if (this.sceneSizeBox != null && this.sceneSizeBox.isMouseOver(mouseX, mouseY)) {
                this.sceneSizeBox.mouseClicked(mouseX, mouseY, 0);
                return true;
            }
            int index = this.sceneDropdown.hitTest(mouseX, mouseY);
            if (index == 0) {
                // 选完保持展开，等失焦（点别处 / ESC / 打开其它下拉）再关
                this.gridVisible = !this.gridVisible;
            } else if (index == 1) {
                this.axesVisible = !this.axesVisible;
            } else if (index == 2) {
                this.blocksVisible = !this.blocksVisible;
                this.closeSceneSizeEditor();
            } else if (index >= 3 && index <= 5 && this.blocksVisible) {
                this.openSceneSizeEditor(index - 3);
            } else {
                // 点在下拉列表之外：失焦，关闭
                this.sceneDropdown.close();
                this.closeSceneSizeEditor();
            }
            return true;
        }
        if (button == 0 && this.isInToolbox(mouseX, mouseY)) {
            this.onToolboxClick(mouseX, mouseY);
            return true;
        }
        if (button == 0 && this.tabs.get(this.activeTabIndex).isCapturingInput()) {
            // 点击工具框之外：关闭下拉/取消编辑，并吞掉这次点击（避免误触相机操作）
            this.closeMenus();
            return true;
        }
        if (button == 0) {
            this.dragging = DRAG_ORBIT;
            return true;
        }
        if (button == 1) {
            this.dragging = DRAG_PAN;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.atlasViewer != null) {
            this.atlasViewer.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            return true;
        }
        MolangEditBox editingBox = this.editingVariableBox();
        if (editingBox != null && editingBox.mouseDragged(mouseX, mouseY, 0, 0, 0)) {
            return true;
        }
        // 工具框里的拖拽交互（例如渐变色标）优先；没在拖时返回 false 再走相机
        if (this.tabs.get(this.activeTabIndex).mouseDragged(mouseX, mouseY)) {
            return true;
        }
        if (this.dragging == DRAG_ORBIT && button == 0) {
            this.yaw += (float) dragX * ORBIT_SENSITIVITY;
            this.pitch = Mth.clamp(this.pitch + (float) dragY * ORBIT_SENSITIVITY, -PITCH_LIMIT, PITCH_LIMIT);
            this.updateCamera();
            return true;
        }
        if (this.dragging == DRAG_PAN && button == 1) {
            // 平移场景：焦点沿相机的 right/up 移动，内容跟随光标；XZ 限制在 16m 网格内
            float halfHeight = BASE_HALF_HEIGHT / this.zoom;
            double worldPerPixel = 2.0D * halfHeight / Math.max(1, this.height);
            Vec3 right = PreviewCamera.rightOf(this.yaw);
            Vec3 up = PreviewCamera.upOf(this.yaw, this.pitch);
            Vec3 moved = this.focus
                    .add(up.scale(dragY * worldPerPixel))
                    .subtract(right.scale(dragX * worldPerPixel));
            double minX = this.sceneOrigin.x - GRID_RADIUS;
            double maxX = this.sceneOrigin.x + GRID_RADIUS;
            double minZ = this.sceneOrigin.z - GRID_RADIUS;
            double maxZ = this.sceneOrigin.z + GRID_RADIUS;
            this.focus = new Vec3(Mth.clamp(moved.x, minX, maxX), moved.y, Mth.clamp(moved.z, minZ, maxZ));
            this.updateCamera();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.atlasViewer != null) {
            this.atlasViewer.mouseReleased(mouseX, mouseY, button);
            return true;
        }
        MolangEditBox editingBox = this.editingVariableBox();
        if (editingBox != null) {
            editingBox.mouseReleased(mouseX, mouseY, 0);
        }
        this.tabs.get(this.activeTabIndex).mouseReleased(mouseX, mouseY);
        this.dragging = DRAG_NONE;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.atlasViewer != null) {
            this.atlasViewer.scroll(mouseX, mouseY, scrollY);
            return true;
        }
        // 指针在展开的下拉列表上时，滚轮滚动列表而不是缩放场景
        for (DropdownWidget widget : List.of(this.loadedDropdown, this.sceneDropdown)) {
            if (widget.contains(mouseX, mouseY)) {
                widget.scroll(scrollY);
                return true;
            }
        }
        // 变量表抽屉盖在页签内容上：它优先吃滚轮
        if (this.variablesOpen && this.mouseOverVariablePanel(mouseX, mouseY)) {
            int contentHeight = 6 + this.variableRows.size() * VAR_ROW_HEIGHT + VAR_ROW_HEIGHT;
            int maxScroll = Math.max(0, contentHeight - this.variablePanelHeight);
            this.variablePanelScroll = Mth.clamp(this.variablePanelScroll - (int) (scrollY * 12), 0, maxScroll);
            return true;
        }
        if (this.tabs.get(this.activeTabIndex).scroll(mouseX, mouseY, scrollY)) {
            return true;
        }
        this.zoom = Mth.clamp(this.zoom * (scrollY > 0.0D ? 1.15F : 1.0F / 1.15F), MIN_ZOOM, MAX_ZOOM);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 图集查看器：ESC 关闭，其余按键交给它的搜索框（避免漏到页签的编辑框里）
        if (this.atlasViewer != null) {
            this.atlasViewer.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        // 变量表面板打开时优先处理（ESC 收起面板；正在编辑的框 ENTER 提交 / ESC 取消）
        if (this.variablesOpen) {
            MolangEditBox editingBox = this.editingVariableBox();
            if (editingBox != null) {
                if (keyCode == 256) {
                    this.cancelVariableEditing();
                    return true;
                }
                if (keyCode == 257) {
                    this.commitVariableEditing();
                    return true;
                }
                return editingBox.keyPressed(keyCode, scanCode, modifiers);
            }
            if (keyCode == 256) {
                this.closeVariablesPanel();
                return true;
            }
        }
        // 正在改方块区域尺寸：优先处理（Enter 提交 / ESC 取消）
        if (this.sceneSizeBox != null) {
            if (keyCode == 256) {
                this.closeSceneSizeEditor();
                return true;
            }
            if (keyCode == 257) {
                this.commitSceneSize();
                return true;
            }
            return this.sceneSizeBox.keyPressed(keyCode, scanCode, modifiers);
        }
        // 页签优先：编辑框（ENTER 提交 / ESC 取消 / 文本输入）、下拉收起都交给当前页签
        if (this.tabs.get(this.activeTabIndex).keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == 256) { // ESC：关闭工具栏下拉
            if (this.loadedDropdown.isOpen() || this.sceneDropdown.isOpen()) {
                this.closeMenus();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.atlasViewer != null) {
            return this.atlasViewer.charTyped(codePoint, modifiers);
        }
        MolangEditBox editingBox = this.editingVariableBox();
        if (editingBox != null && editingBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        if (this.sceneSizeBox != null) {
            return this.sceneSizeBox.charTyped(codePoint, modifiers);
        }
        if (this.tabs.get(this.activeTabIndex).charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    // endregion

    // region 顶部工具栏与粒子下拉

    private void renderOverlay(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        this.hoveredTooltip = null;
        this.highlightedTooltip = null;
        // 先画左侧工具框，再画工具栏（含下拉列表），保证工具栏和下拉不被工具框盖住
        this.renderToolbox(guiGraphics, mouseX, mouseY);
        this.renderToolbar(guiGraphics, mouseX, mouseY);
        Component stats = Component.translatable("particle_editor.stats",
                this.manager.totalEmitterCount(), this.manager.totalParticleCount(), Minecraft.fps);
        guiGraphics.drawString(this.font, stats, this.width - this.font.width(stats) - 8, 6, 0xFFFFFF, true);
        int textX = this.toolboxWidth() + 6;
        if (this.error != null) {
            guiGraphics.drawString(this.font, this.error, textX, this.height - 24, 0xFF5555, true);
        }
        // 变量表面板在工具栏之上绘制（编辑行、按钮都在里面）
        this.renderVariablesPanel(guiGraphics, mouseX, mouseY);
        if (this.highlightedTooltip != null) {
            this.renderMolangTooltip(guiGraphics, this.highlightedTooltip, mouseX, mouseY);
        } else if (this.hoveredTooltip != null) {
            guiGraphics.renderTooltip(this.font, this.hoveredTooltip, mouseX, mouseY);
        }
        // 图集查看器盖在最上面（模态）
        if (this.atlasViewer != null) {
            this.atlasViewer.render(guiGraphics, mouseX, mouseY);
        }
    }

    /// Molang 文本的 tooltip：折行后用 [MolangSyntax] 着色绘制（超过屏幕宽度的长表达式也不会被截断）。
    private void renderMolangTooltip(GuiGraphics guiGraphics, String text, int mouseX, int mouseY) {
        int maxTextWidth = Math.min(this.width - 24, 260);
        List<List<MolangSyntax.Token>> lines = new ArrayList<>();
        List<MolangSyntax.Token> line = new ArrayList<>();
        int lineWidth = 0;
        for (MolangSyntax.Token token : MolangSyntax.tokenize(text)) {
            String remaining = token.text();
            while (!remaining.isEmpty()) {
                int space = maxTextWidth - lineWidth;
                int width = this.font.width(remaining);
                if (width <= space) {
                    line.add(new MolangSyntax.Token(remaining, token.color()));
                    lineWidth += width;
                    remaining = "";
                } else {
                    String part = this.font.plainSubstrByWidth(remaining, space);
                    if (part.isEmpty()) {
                        if (line.isEmpty()) {
                            part = remaining.substring(0, 1);
                        } else {
                            lines.add(line);
                            line = new ArrayList<>();
                            lineWidth = 0;
                            continue;
                        }
                    }
                    remaining = remaining.substring(part.length());
                    line.add(new MolangSyntax.Token(part, token.color()));
                    lineWidth += this.font.width(part);
                    if (!remaining.isEmpty()) {
                        lines.add(line);
                        line = new ArrayList<>();
                        lineWidth = 0;
                    }
                }
            }
        }
        if (!line.isEmpty() || lines.isEmpty()) {
            lines.add(line);
        }
        int maxLine = 0;
        for (List<MolangSyntax.Token> current : lines) {
            int width = 0;
            for (MolangSyntax.Token token : current) {
                width += this.font.width(token.text());
            }
            maxLine = Math.max(maxLine, width);
        }
        int pad = 4;
        int lineHeight = 10;
        int boxWidth = maxLine + pad * 2;
        int boxHeight = lines.size() * lineHeight + pad * 2;
        int x = Mth.clamp(mouseX + 10, 4, Math.max(4, this.width - boxWidth - 4));
        int y = Mth.clamp(mouseY - 12, 4, Math.max(4, this.height - boxHeight - 4));
        guiGraphics.fill(x, y, x + boxWidth, y + boxHeight, 0xF0100010);
        guiGraphics.renderOutline(x, y, boxWidth, boxHeight, 0xFF5050FF);
        int textY = y + pad;
        for (List<MolangSyntax.Token> current : lines) {
            int textX = x + pad;
            for (MolangSyntax.Token token : current) {
                guiGraphics.drawString(this.font, token.text(), textX, textY, token.color(), false);
                textX += this.font.width(token.text());
            }
            textY += lineHeight;
        }
    }

    /// 截断文本；若确实被截断且当前悬停，记录完整文本作为 tooltip（在 overlay 末尾统一渲染）。
    String truncatedFor(String fullText, int maxWidth, boolean hovered) {
        if (this.font.width(fullText) <= maxWidth) {
            return fullText;
        }
        if (hovered) {
            this.hoveredTooltip = Component.literal(fullText);
        }
        return this.font.plainSubstrByWidth(fullText, maxWidth - this.font.width("...")) + "...";
    }

    private void renderToolbar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.fill(0, 0, this.width, TOOLBAR_HEIGHT, 0xF0181820);

        // 「New」按钮：宽 = 文本宽 + 内边距
        int newWidth = this.newButtonWidth();
        String newLabel = this.newButtonText();
        guiGraphics.fill(NEW_X, TOOLBAR_BUTTON_Y, NEW_X + newWidth, TOOLBAR_BUTTON_Y + TOOLBAR_BUTTON_HEIGHT,
                this.mouseOverNewButton(mouseX, mouseY) ? 0xFF3A3A48 : 0xFF2A2A34);
        guiGraphics.renderOutline(NEW_X, TOOLBAR_BUTTON_Y, newWidth, TOOLBAR_BUTTON_HEIGHT, 0xFF4A4A58);
        guiGraphics.drawString(this.font, newLabel, NEW_X + (newWidth - this.font.width(newLabel)) / 2, TOOLBAR_BUTTON_Y + 4, 0xFFFFFF, true);

        // 「Loaded」下拉按钮：宽 = 文本宽 + 内边距 + 箭头占位
        int loadedX = this.loadedButtonX();
        int loadedWidth = this.loadedButtonWidth();
        String loadedLabel = this.loadedButtonText();
        guiGraphics.fill(loadedX, TOOLBAR_BUTTON_Y, loadedX + loadedWidth, TOOLBAR_BUTTON_Y + TOOLBAR_BUTTON_HEIGHT,
                this.mouseOverLoadedButton(mouseX, mouseY) ? 0xFF3A3A48 : 0xFF2A2A34);
        guiGraphics.renderOutline(loadedX, TOOLBAR_BUTTON_Y, loadedWidth, TOOLBAR_BUTTON_HEIGHT, 0xFF4A4A58);
        guiGraphics.drawString(this.font, loadedLabel, loadedX + 4, TOOLBAR_BUTTON_Y + 4, 0xFFFFFF, true);
        this.renderDropdownArrow(guiGraphics, loadedX + loadedWidth - 11, TOOLBAR_BUTTON_Y + 7);

        // 「Scene」下拉按钮
        int sceneX = this.sceneButtonX();
        int sceneWidth = this.sceneButtonWidth();
        String sceneLabel = this.sceneButtonText();
        guiGraphics.fill(sceneX, TOOLBAR_BUTTON_Y, sceneX + sceneWidth, TOOLBAR_BUTTON_Y + TOOLBAR_BUTTON_HEIGHT,
                this.mouseOverSceneButton(mouseX, mouseY) ? 0xFF3A3A48 : 0xFF2A2A34);
        guiGraphics.renderOutline(sceneX, TOOLBAR_BUTTON_Y, sceneWidth, TOOLBAR_BUTTON_HEIGHT, 0xFF4A4A58);
        guiGraphics.drawString(this.font, sceneLabel, sceneX + 4, TOOLBAR_BUTTON_Y + 4, 0xFFFFFF, true);
        this.renderDropdownArrow(guiGraphics, sceneX + sceneWidth - 11, TOOLBAR_BUTTON_Y + 7);

        this.renderPlaybackButtons(guiGraphics, mouseX, mouseY);
        this.renderVariablesButton(guiGraphics, mouseX, mouseY);

        this.loadedDropdown.setPosition(loadedX, TOOLBAR_HEIGHT);
        this.loadedDropdown.setItems(this.particleIds.stream().map(ResourceLocation::toString).toList());
        this.loadedDropdown.setSelectedIndex(this.particleIds.indexOf(this.selectedParticle));
        this.loadedDropdown.render(guiGraphics, this.font, mouseX, mouseY, this.height);
        this.tooltipForMenu(this.loadedDropdown, mouseX, mouseY);

        String gridLabel = Component.translatable("particle_editor.scene.grid").getString();
        List<String> sceneItems = new ArrayList<>();
        sceneItems.add((this.gridVisible ? "[x] " : "[ ] ") + gridLabel);
        sceneItems.add((this.axesVisible ? "[x] " : "[ ] ")
                + Component.translatable("particle_editor.scene.axes").getString());
        sceneItems.add((this.blocksVisible ? "[x] " : "[ ] ")
                + Component.translatable("particle_editor.scene.blocks").getString());
        if (this.blocksVisible) {
            sceneItems.add(Component.translatable("particle_editor.scene.size_x", this.blockSizeX).getString());
            sceneItems.add(Component.translatable("particle_editor.scene.size_y", this.blockSizeY).getString());
            sceneItems.add(Component.translatable("particle_editor.scene.size_z", this.blockSizeZ).getString());
        }
        this.sceneDropdown.setPosition(sceneX, TOOLBAR_HEIGHT);
        this.sceneDropdown.setItems(sceneItems);
        this.sceneDropdown.render(guiGraphics, this.font, mouseX, mouseY, this.height);
        this.tooltipForMenu(this.sceneDropdown, mouseX, mouseY);
        if (this.sceneSizeBox != null) {
            this.sceneSizeBox.render(guiGraphics, mouseX, mouseY, 0);
        }
    }

    void renderDropdownArrow(GuiGraphics guiGraphics, int arrowX, int arrowY) {
        guiGraphics.fill(arrowX, arrowY, arrowX + 3, arrowY + 1, 0xFFCCCCCC);
        guiGraphics.fill(arrowX + 1, arrowY + 1, arrowX + 2, arrowY + 2, 0xFFCCCCCC);
    }

    /// 若当前悬停的下拉条目文本被截断，记录完整文本作为 tooltip。
    private void tooltipForMenu(DropdownWidget dropdown, int mouseX, int mouseY) {
        if (!dropdown.isOpen()) {
            return;
        }
        int index = dropdown.hitTest(mouseX, mouseY);
        String full = dropdown.item(index);
        if (full != null && this.font.width(full) > dropdown.textWidth()) {
            this.hoveredTooltip = Component.literal(full);
        }
    }

    /// 鼠标是否落在展开的工具栏下拉列表上：是的话，下拉底下的文本不应再触发 tooltip。
    boolean isOverToolbarMenu(double mouseX, double mouseY) {
        return (this.loadedDropdown.isOpen() && this.loadedDropdown.hitTest(mouseX, mouseY) >= 0)
                || (this.sceneDropdown.isOpen() && this.sceneDropdown.hitTest(mouseX, mouseY) >= 0);
    }

    /// 打开某个菜单时关闭其它菜单；再次点击按钮则关闭它自己。
    void toggleMenu(DropdownWidget dropdown) {
        boolean open = !dropdown.isOpen();
        this.closeMenus();
        if (open) {
            dropdown.toggle();
        }
    }

    void closeMenus() {
        this.loadedDropdown.close();
        this.sceneDropdown.close();
        this.closeSceneSizeEditor();
        this.tabs.get(this.activeTabIndex).closeEditing();
    }

    private void selectLoaded(int index) {
        if (index < 0 || index >= this.particleIds.size()) {
            return;
        }
        ResourceLocation id = this.particleIds.get(index);
        // 换粒子：先更新缓存，再清除场景并重新发射；自定义变量属于旧文档，换文档时清空
        this.effect = MolangParticleEngine.INSTANCE.id2Effect().get(id);
        this.clearVariables();
        this.manager.clearScene();
        this.spawnEmitter(id);
        this.onEffectReplaced();
    }

    /// 清空变量表工具里的自定义变量（换粒子 / 新建文档时调用）：收起面板、丢掉编辑状态和所有行。
    /// 行在下次打开面板时会按新发射器的变量表重新补出来。
    private void clearVariables() {
        this.cancelVariableEditing();
        this.variableRows.clear();
        this.variablePanelScroll = 0;
        this.variablesOpen = false;
    }

    /// 整份效果被替换（换粒子 / 新建）：让所有页签丢掉针对旧效果的编辑状态，并切回描述页。
    private void onEffectReplaced() {
        this.atlasViewer = null;
        for (EditorTab tab : this.tabs) {
            tab.onEffectReplaced();
        }
        this.activeTabIndex = DESCRIPTION_TAB;
    }

    // region 工具栏按钮的动态宽度

    private String newButtonText() {
        return Component.translatable("particle_editor.new").getString();
    }

    private String loadedButtonText() {
        return Component.translatable("particle_editor.loaded").getString();
    }

    private String sceneButtonText() {
        return Component.translatable("particle_editor.scene").getString();
    }

    private int newButtonWidth() {
        return this.font.width(this.newButtonText()) + BUTTON_PADDING;
    }

    private int loadedButtonWidth() {
        return this.font.width(this.loadedButtonText()) + BUTTON_PADDING + DROPDOWN_ARROW_AREA;
    }

    private int sceneButtonWidth() {
        return this.font.width(this.sceneButtonText()) + BUTTON_PADDING + DROPDOWN_ARROW_AREA;
    }

    private int loadedButtonX() {
        return NEW_X + this.newButtonWidth() + BUTTON_GAP;
    }

    private int sceneButtonX() {
        return this.loadedButtonX() + this.loadedButtonWidth() + BUTTON_GAP;
    }

    // endregion

    private boolean mouseOverNewButton(double mouseX, double mouseY) {
        return mouseX >= NEW_X && mouseX < NEW_X + this.newButtonWidth()
                && mouseY >= TOOLBAR_BUTTON_Y && mouseY < TOOLBAR_BUTTON_Y + TOOLBAR_BUTTON_HEIGHT;
    }

    /// 新建编辑器文档：用一套默认组件组一个能直接发射的效果；贴图/材质沿用当前文档，避免重新绑定 sprite。
    ///
    /// identifier 只在编辑器内使用（`particlestorm:new_particle`），之后可以在描述页签里改。
    private void newEffect() {
        DefinedParticleEffect base = this.effect;
        DescriptionParameters parameters = base != null
                ? base.description.parameters()
                : new DescriptionParameters(DescriptionMaterial.PARTICLE_SHEET_TRANSLUCENT, ResourceLocation.withDefaultNamespace("missingno"));
        ParticleType<?> type = base != null ? base.description.type() : ParticleStorm.MOLANG.get();
        Map<ResourceLocation, IComponent> components = new HashMap<>();
        components.put(EmitterRate.Steady.ID, new EmitterRate.Steady(FloatMolangExp.ofConstant(10), FloatMolangExp.ofConstant(50)));
        components.put(EmitterLifetime.Looping.ID, new EmitterLifetime.Looping(FloatMolangExp.ofConstant(10), FloatMolangExp.ZERO));
        components.put(EmitterShape.Point.ID, new EmitterShape.Point(FloatMolangExp3.ZERO, EmitterShape.Direction.OUTWARDS));
        components.put(ParticleInitialSpeed.ID, new ParticleInitialSpeed(Either.left(FloatMolangExp.ofConstant(1))));
        components.put(ParticleLifetimeExpression.ID, new ParticleLifetimeExpression(FloatMolangExp.ZERO, FloatMolangExp.ofConstant(1)));
        components.put(ParticleAppearanceBillboard.ID, new ParticleAppearanceBillboard(FloatMolangExp2.ZERO,
                ParticleAppearanceBillboard.FaceCameraMode.ROTATE_XYZ, ParticleAppearanceBillboard.Direction.DEFAULT,
                ParticleAppearanceBillboard.UV.EMPTY));
        ParticleDescription description = new ParticleDescription(ParticleStorm.asResource("new_particle"), parameters, type);
        this.effect = new DefinedParticleEffect(description, Map.of(), components, Map.of());
        this.error = null;
        this.closeMenus();
        this.clearVariables();
        this.manager.clearScene();
        this.spawnEmitter(description.identifier());
        this.onEffectReplaced();
    }

    private boolean mouseOverLoadedButton(double mouseX, double mouseY) {
        int x = this.loadedButtonX();
        return mouseX >= x && mouseX < x + this.loadedButtonWidth()
                && mouseY >= TOOLBAR_BUTTON_Y && mouseY < TOOLBAR_BUTTON_Y + TOOLBAR_BUTTON_HEIGHT;
    }

    private boolean mouseOverSceneButton(double mouseX, double mouseY) {
        int x = this.sceneButtonX();
        return mouseX >= x && mouseX < x + this.sceneButtonWidth()
                && mouseY >= TOOLBAR_BUTTON_Y && mouseY < TOOLBAR_BUTTON_Y + TOOLBAR_BUTTON_HEIGHT;
    }

    /// 工具栏的两个播放控制按钮：`▶`（从头播放）与 `⏸`（切换暂停/播放）。
    private int replayButtonX() {
        return this.sceneButtonX() + this.sceneButtonWidth() + BUTTON_GAP;
    }

    private int pauseButtonX() {
        return this.replayButtonX() + PAUSE_BUTTON_WIDTH + BUTTON_GAP;
    }

    private boolean mouseOverReplayButton(double mouseX, double mouseY) {
        int x = this.replayButtonX();
        return mouseX >= x && mouseX < x + PAUSE_BUTTON_WIDTH
                && mouseY >= TOOLBAR_BUTTON_Y && mouseY < TOOLBAR_BUTTON_Y + TOOLBAR_BUTTON_HEIGHT;
    }

    private boolean mouseOverPauseButton(double mouseX, double mouseY) {
        int x = this.pauseButtonX();
        return mouseX >= x && mouseX < x + PAUSE_BUTTON_WIDTH
                && mouseY >= TOOLBAR_BUTTON_Y && mouseY < TOOLBAR_BUTTON_Y + TOOLBAR_BUTTON_HEIGHT;
    }

    private void renderPlaybackButtons(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // ▶：从头播放（清场景后用当前效果重新发射）
        int replayX = this.replayButtonX();
        this.renderIconButton(guiGraphics, replayX, this.mouseOverReplayButton(mouseX, mouseY), false);
        // ⏸：切换暂停与播放（图标固定为暂停，不随播放状态变化）
        int pauseX = this.pauseButtonX();
        this.renderIconButton(guiGraphics, pauseX, this.mouseOverPauseButton(mouseX, mouseY), true);
    }

    /// 画一个 18px 的方形图标按钮：`pauseIcon` 为 true 画两条竖条，否则画右向三角。
    private void renderIconButton(GuiGraphics guiGraphics, int x, boolean hover, boolean pauseIcon) {
        int y = TOOLBAR_BUTTON_Y;
        guiGraphics.fill(x, y, x + PAUSE_BUTTON_WIDTH, y + TOOLBAR_BUTTON_HEIGHT, hover ? 0xFF3A3A48 : 0xFF2A2A34);
        guiGraphics.renderOutline(x, y, PAUSE_BUTTON_WIDTH, TOOLBAR_BUTTON_HEIGHT, 0xFF4A4A58);
        if (pauseIcon) {
            guiGraphics.fill(x + 6, y + 4, x + 8, y + 12, 0xFFFFFFFF);
            guiGraphics.fill(x + 10, y + 4, x + 12, y + 12, 0xFFFFFFFF);
        } else {
            for (int row = 0; row < 8; row++) {
                int width = row <= 3 ? row + 1 : 8 - row;
                guiGraphics.fill(x + 6, y + 4 + row, x + 6 + width, y + 5 + row, 0xFFFFFFFF);
            }
        }
    }

    /// 从头播放：清空场景，用当前缓存的 [EditorScreen#effect] 重新发射，并恢复播放。
    private void replayFromStart() {
        this.playing = true;
        this.pausePartialTick = null;
        this.manager.clearScene();
        if (this.selectedParticle != null) {
            this.spawnEmitter(this.selectedParticle);
        }
    }

    // region 发射器变量表工具

    private String variablesButtonText() {
        return Component.translatable("particle_editor.variables").getString();
    }

    private int variablesButtonWidth() {
        return this.font.width(this.variablesButtonText()) + BUTTON_PADDING;
    }

    private int variablesButtonX() {
        int x = this.pauseButtonX() + PAUSE_BUTTON_WIDTH + BUTTON_GAP;
        // 窗口很窄（GUI 缩放很大）时整个工具栏会顶出屏幕：把按钮左移，保证始终可见
        int max = this.width - this.variablesButtonWidth() - 2;
        return Math.max(0, Math.min(x, max));
    }

    private boolean mouseOverVariablesButton(double mouseX, double mouseY) {
        int x = this.variablesButtonX();
        return mouseX >= x && mouseX < x + this.variablesButtonWidth()
                && mouseY >= TOOLBAR_BUTTON_Y && mouseY < TOOLBAR_BUTTON_Y + TOOLBAR_BUTTON_HEIGHT;
    }

    private void renderVariablesButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = this.variablesButtonX();
        int width = this.variablesButtonWidth();
        boolean hover = this.mouseOverVariablesButton(mouseX, mouseY) || this.variablesOpen;
        guiGraphics.fill(x, TOOLBAR_BUTTON_Y, x + width, TOOLBAR_BUTTON_Y + TOOLBAR_BUTTON_HEIGHT,
                hover ? 0xFF3A3A48 : 0xFF2A2A34);
        guiGraphics.renderOutline(x, TOOLBAR_BUTTON_Y, width, TOOLBAR_BUTTON_HEIGHT, 0xFF4A4A58);
        guiGraphics.drawString(this.font, this.variablesButtonText(), x + 4, TOOLBAR_BUTTON_Y + 4, 0xFFFFFF, true);
    }

    // region 变量表面板

    /// 打开面板：把当前发射器变量表里**所有变量**（默认的 `v.emitter_age`、`v.emitter_random_1` 等
    /// 加上自定义变量）补进可编辑行，之后每帧显示实时值。
    /// 变量名登记在预设变量表（实例表的父表）里，所以要沿 `getParent()` 一路收集。
    private void openVariablesPanel() {
        this.variablesOpen = true;
        this.variablePanelScroll = 0;
        ParticleEmitter emitter = this.firstEmitter();
        if (emitter == null) {
            return;
        }
        Set<String> names = new LinkedHashSet<>();
        for (VariableTable table = emitter.getVars(); table != null; table = table.getParent()) {
            names.addAll(table.table.keySet());
        }
        List<String> keys = new ArrayList<>(names);
        keys.sort(String::compareTo);
        for (String key : keys) {
            String canonical = canonicalVariableKey(key);
            if (canonical.isEmpty() || this.findVariableRow(canonical) != null) {
                continue;
            }
            this.variableRows.add(new VariableRow(canonical));
        }
    }

    private void closeVariablesPanel() {
        this.cancelVariableEditing();
        this.variablesOpen = false;
    }

    private @Nullable VariableRow findVariableRow(String canonicalKey) {
        for (VariableRow row : this.variableRows) {
            if (row.key.equals(canonicalKey)) {
                return row;
            }
        }
        return null;
    }

    /// 变量名统一规范化为 `variable.<名字>`（面板里显示成 `v.<名字>`）。
    private static String canonicalVariableKey(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.startsWith(VARIABLE_PREFIX)) {
            return trimmed;
        }
        if (trimmed.startsWith("v.")) {
            return VARIABLE_PREFIX + trimmed.substring(2);
        }
        return VARIABLE_PREFIX + trimmed;
    }

    private static String displayVariableKey(String canonicalKey) {
        return canonicalKey.startsWith(VARIABLE_PREFIX)
                ? "v." + canonicalKey.substring(VARIABLE_PREFIX.length())
                : canonicalKey;
    }

    /// 变量在发射器变量表里的**实时值**（没有这个变量时返回 null）。
    /// 用 `getVariable` 而不是 `table.get`：变量名登记在预设的变量表里（父表），实例表只放赋值后的值。
    private @Nullable Float liveVariableValue(String canonicalKey) {
        if (canonicalKey.isEmpty()) {
            return null;
        }
        ParticleEmitter emitter = this.firstEmitter();
        if (emitter == null) {
            return null;
        }
        Variable variable = emitter.getVars().getVariable(canonicalKey);
        if (variable == null) {
            return null;
        }
        try {
            return variable.get(emitter);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /// 面板里显示的值：能取到实时值就显示实时值（`v.emitter_random_1` 这类会变），否则显示表达式。
    private String variableRowValueText(VariableRow row) {
        Float live = this.liveVariableValue(row.key);
        if (live != null) {
            return formatVariableValue(live);
        }
        return row.expr;
    }

    private static String formatVariableValue(float value) {
        if (Math.abs(value) >= 0.0005F && Math.abs(value - Math.round(value)) < 0.0005F) {
            return String.valueOf(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    /// 变量表内容：`[变量名, molang 表达式]`（表达式为空的行不注入，保持发射器自己的值）。
    private Map<String, String> variablesMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (VariableRow row : this.variableRows) {
            if (!row.key.isEmpty() && !row.expr.isEmpty()) {
                map.put(row.key, row.expr);
            }
        }
        return map;
    }

    /// 变量表改动后重建发射器（清场景 + 重新发射），新预设会带上这些变量。
    private void applyVariables() {
        if (this.selectedParticle != null) {
            this.manager.clearScene();
            this.spawnEmitter(this.selectedParticle);
        }
    }

    // region 纹理图集查看器

    /// 打开图集查看器（描述页签点 texture 行时调用）。按当前材质选图集，选中的 sprite 交给回调绑定。
    void openAtlasViewer(DescriptionMaterial material, @Nullable ResourceLocation currentTexture,
                         Consumer<TextureAtlasSprite> onSelect) {
        this.closeMenus();
        this.tabs.get(this.activeTabIndex).closeEditing();
        this.atlasViewer = new AtlasViewer(this, this.font, material, currentTexture, onSelect);
    }

    void closeAtlasViewer() {
        this.atlasViewer = null;
    }

    /// 保证编辑器文档的贴图在当前材质对应的图集里、并已登记进共享 sprite 集合（按贴图名查找）：
    /// 资源重载后共享集合会按引擎数据重建，所以每次发射前按名字重新登记一遍。
    private void ensureSpriteRegistered() {
        DefinedParticleEffect current = this.effect;
        if (current == null) {
            return;
        }
        DescriptionParameters parameters = current.description.parameters();
        TextureAtlasSprite sprite = AtlasViewer.spriteFor(parameters.material(), parameters.texture());
        if (sprite != null) {
            RegisterCustomParticleTypeEvent.sprites().addSprite(sprite);
        }
    }

    // endregion

    /// 面板挂在按钮正下方（像下拉列表），每行 `[变量名] = [值]`，右侧 `-` 删除，底部 `+ 添加`。
    private void renderVariablesPanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!this.variablesOpen) {
            return;
        }
        int x = this.variablesButtonX();
        int width = Math.max(200, Math.min(340, this.toolboxWidth() + 60));
        width = Math.max(160, Math.min(width, this.width - x - 4));
        int y = TOOLBAR_HEIGHT;
        int contentHeight = 6 + this.variableRows.size() * VAR_ROW_HEIGHT + VAR_ROW_HEIGHT;
        int height = Math.max(20, Math.min(this.height - y - 4, contentHeight));
        int maxScroll = Math.max(0, contentHeight - height);
        this.variablePanelScroll = Mth.clamp(this.variablePanelScroll, 0, maxScroll);
        this.variablePanelX = x;
        this.variablePanelY = y;
        this.variablePanelWidth = width;
        this.variablePanelHeight = height;
        guiGraphics.fill(x, y, x + width, y + height, 0xF0121218);
        guiGraphics.renderOutline(x, y, width, height, 0xFF3A3A46);
        guiGraphics.enableScissor(x, y, x + width, y + height);
        try {
            int rowTop = y + 3 - this.variablePanelScroll;
            for (VariableRow row : this.variableRows) {
                if (rowTop + VAR_ROW_HEIGHT > y && rowTop < y + height) {
                    this.renderVariableRow(guiGraphics, row, x, width, rowTop, mouseX, mouseY);
                }
                rowTop += VAR_ROW_HEIGHT;
            }
            // 底部「+ 添加变量」
            int addTop = rowTop;
            boolean addHover = mouseX >= x + 4 && mouseX < x + width - 4 && mouseY >= addTop && mouseY < addTop + VAR_ROW_HEIGHT;
            if (addHover) {
                guiGraphics.fill(x + 2, addTop, x + width - 2, addTop + VAR_ROW_HEIGHT, 0x203A6FD8);
            }
            String addLabel = "+ " + Component.translatable("particle_editor.variable_add").getString();
            guiGraphics.drawString(this.font, addLabel, x + 6, addTop + 4, 0x9CD0FF, false);
        } finally {
            guiGraphics.disableScissor();
        }
    }

    /// 画一行：`key = value`，右侧 `-`；正在编辑的字段换成拉伸到**整个面板宽度**的 [MolangEditBox]。
    private void renderVariableRow(GuiGraphics guiGraphics, VariableRow row, int panelX, int panelWidth, int rowTop,
                                   int mouseX, int mouseY) {
        MolangEditBox editing = row.keyBox != null ? row.keyBox : row.valueBox;
        if (editing != null) {
            editing.setX(panelX + 4);
            editing.setY(rowTop);
            editing.setWidth(Math.max(20, panelWidth - 8));
            editing.render(guiGraphics, mouseX, mouseY, 0);
            return;
        }
        int keyX = panelX + 4;
        int keyWidth = Math.min(VAR_KEY_WIDTH, panelWidth - 90);
        int sepX = keyX + keyWidth + 2;
        int valueX = sepX + 10;
        int valueWidth = Math.max(24, panelX + panelWidth - 22 - valueX);
        int deleteX = panelX + panelWidth - 18;

        boolean keyHover = mouseX >= keyX && mouseX < keyX + keyWidth && mouseY >= rowTop && mouseY < rowTop + VAR_ROW_HEIGHT;
        boolean valueHover = mouseX >= valueX && mouseX < valueX + valueWidth && mouseY >= rowTop && mouseY < rowTop + VAR_ROW_HEIGHT;
        boolean deleteHover = mouseX >= deleteX && mouseX < deleteX + 12 && mouseY >= rowTop && mouseY < rowTop + VAR_ROW_HEIGHT;
        if (keyHover) {
            guiGraphics.fill(keyX, rowTop, keyX + keyWidth, rowTop + VAR_ROW_HEIGHT, 0x20FFFFFF);
        }
        if (valueHover) {
            guiGraphics.fill(valueX, rowTop, valueX + valueWidth, rowTop + VAR_ROW_HEIGHT, 0x20FFFFFF);
        }
        if (deleteHover) {
            guiGraphics.fill(deleteX, rowTop, deleteX + 12, rowTop + VAR_ROW_HEIGHT, 0x40FF5555);
        }

        String keyText = row.key.isEmpty()
                ? Component.translatable("particle_editor.variable_name").getString()
                : displayVariableKey(row.key);
        guiGraphics.drawString(this.font, keyText, keyX + 2, rowTop + 4, row.key.isEmpty() ? 0x777777 : 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "=", sepX, rowTop + 4, 0xBBBBBB, false);
        MolangSyntax.draw(guiGraphics, this.font, this.variableRowValueText(row), valueX, rowTop + 4, valueWidth);
        guiGraphics.drawString(this.font, "-", deleteX + 4, rowTop + 4, deleteHover ? 0xFF8080 : 0xCCCCCC, false);
    }

    private boolean mouseOverVariablePanel(double mouseX, double mouseY) {
        return this.variablesOpen
                && mouseX >= this.variablePanelX && mouseX < this.variablePanelX + this.variablePanelWidth
                && mouseY >= this.variablePanelY && mouseY < this.variablePanelY + this.variablePanelHeight;
    }

    /// 面板内点击：先提交正在编辑的框，再命中键区/值区/删除/添加行。
    private void onVariablesPanelClick(double mouseX, double mouseY) {
        MolangEditBox editingBox = this.editingVariableBox();
        if (editingBox != null) {
            if (editingBox.isMouseOver(mouseX, mouseY)) {
                editingBox.mouseClicked(mouseX, mouseY, 0);
                return;
            }
            // 编辑时整行都属于编辑框（框高 14、行高 16，上下各差一点也算点击框内）
            if (mouseX >= this.variablePanelX && mouseX < this.variablePanelX + this.variablePanelWidth
                    && mouseY >= editingBox.getY() && mouseY < editingBox.getY() + VAR_ROW_HEIGHT) {
                editingBox.mouseClicked(mouseX, mouseY, 0);
                return;
            }
            this.commitVariableEditing();
        }
        int x = this.variablePanelX;
        int width = this.variablePanelWidth;
        int rowTop = this.variablePanelY + 3 - this.variablePanelScroll;
        int keyX = x + 4;
        int keyWidth = Math.min(VAR_KEY_WIDTH, width - 90);
        int valueX = keyX + keyWidth + 12;
        int valueWidth = Math.max(24, x + width - 22 - valueX);
        int deleteX = x + width - 18;
        for (int i = 0; i < this.variableRows.size(); i++) {
            if (mouseY >= rowTop && mouseY < rowTop + VAR_ROW_HEIGHT) {
                VariableRow row = this.variableRows.get(i);
                if (mouseX >= deleteX && mouseX < deleteX + 12) {
                    this.variableRows.remove(i);
                    this.applyVariables();
                    return;
                }
                if (mouseX >= keyX && mouseX < keyX + keyWidth) {
                    this.openVariableEditor(row, 0, rowTop);
                    return;
                }
                if (mouseX >= valueX && mouseX < valueX + valueWidth) {
                    this.openVariableEditor(row, 1, rowTop);
                    return;
                }
                return;
            }
            rowTop += VAR_ROW_HEIGHT;
        }
        // 底部「+ 添加变量」行
        if (mouseY >= rowTop && mouseY < rowTop + VAR_ROW_HEIGHT) {
            this.variableRows.add(new VariableRow(""));
        }
    }

    /// 打开某一行的字段编辑：编辑框拉伸到整个面板宽度（`面板宽 - 8`），
    /// 名字框里是 `v.<名字>`，值框里是当前表达式（没改过就带上实时值）。
    private void openVariableEditor(VariableRow row, int field, int boxTop) {
        this.cancelVariableEditing();
        int boxX = this.variablePanelX + 4;
        int boxWidth = Math.max(20, this.variablePanelWidth - 8);
        MolangEditBox box = new MolangEditBox(this.font, boxX, boxTop, boxWidth, 14, Component.empty());
        box.setMaxLength(512);
        String initial;
        if (field == 0) {
            initial = row.key.isEmpty() ? "" : displayVariableKey(row.key);
            row.keyBox = box;
        } else {
            initial = row.expr.isEmpty() ? this.variableRowValueText(row) : row.expr;
            row.valueBox = box;
        }
        box.setValue(initial);
        box.setFocused(true);
        row.initialText = initial;
        this.editingVariable = row;
        this.editingVariableField = field;
    }

    /// 提交当前编辑：名字规范化为 `variable.<名字>`，值原样作为 molang 表达式；有改动就重建发射器。
    private void commitVariableEditing() {
        VariableRow row = this.editingVariable;
        int field = this.editingVariableField;
        MolangEditBox box = field == 0 ? (row == null ? null : row.keyBox) : (row == null ? null : row.valueBox);
        String initial = row == null ? "" : row.initialText;
        if (row != null) {
            row.keyBox = null;
            row.valueBox = null;
        }
        this.editingVariable = null;
        this.editingVariableField = -1;
        if (row == null || box == null) {
            return;
        }
        String text = box.getValue().trim();
        if (field == 0) {
            String key = canonicalVariableKey(text);
            if (!key.equals(row.key)) {
                removeDuplicateVariableRow(row, key);
                row.key = key;
                this.applyVariables();
            }
        } else if (!text.equals(initial) && !text.equals(row.expr)) {
            // 值没改动（只是点开又提交，或者原样是实时值）就不覆盖，保持发射器自己的变量
            row.expr = text;
            this.applyVariables();
        }
    }

    /// 改名后如果和别的行撞名，丢掉被撞的那行（避免同一个变量名出现两次）。
    private void removeDuplicateVariableRow(VariableRow row, String key) {
        if (key.isEmpty()) {
            return;
        }
        this.variableRows.removeIf(other -> other != row && other.key.equals(key));
    }

    private void cancelVariableEditing() {
        VariableRow row = this.editingVariable;
        if (row != null) {
            row.keyBox = null;
            row.valueBox = null;
        }
        this.editingVariable = null;
        this.editingVariableField = -1;
    }

    private @Nullable MolangEditBox editingVariableBox() {
        VariableRow row = this.editingVariable;
        if (row == null) {
            return null;
        }
        return this.editingVariableField == 0 ? row.keyBox : row.valueBox;
    }

    // endregion

    // endregion

    /// 在 Scene 下拉的「size x/y/z」行上盖一个原版 EditBox（行号固定为 3 + axis）。
    private void openSceneSizeEditor(int axis) {
        int value = switch (axis) {
            case 0 -> this.blockSizeX;
            case 1 -> this.blockSizeY;
            default -> this.blockSizeZ;
        };
        int rowY = TOOLBAR_HEIGHT + (3 + axis) * TOOLBAR_ROW_HEIGHT;
        this.sceneSizeAxis = axis;
        int sceneX = this.sceneButtonX();
        this.sceneSizeBox = new EditBox(this.font, sceneX + 1, rowY, this.sceneButtonWidth() - 2, TOOLBAR_ROW_HEIGHT, Component.literal("size"));
        this.sceneSizeBox.setMaxLength(3);
        this.sceneSizeBox.setValue(String.valueOf(value));
        this.sceneSizeBox.setFocused(true);
    }

    private void commitSceneSize() {
        EditBox box = this.sceneSizeBox;
        int axis = this.sceneSizeAxis;
        this.closeSceneSizeEditor();
        if (box == null) {
            return;
        }
        try {
            int value = Mth.clamp(Integer.parseInt(box.getValue().trim()), 1, 16);
            switch (axis) {
                case 0 -> this.blockSizeX = value;
                case 1 -> this.blockSizeY = value;
                default -> this.blockSizeZ = value;
            }
        } catch (NumberFormatException e) {
            this.error = Component.literal("Invalid size: " + box.getValue());
        }
    }

    private void closeSceneSizeEditor() {
        this.sceneSizeBox = null;
        this.sceneSizeAxis = -1;
    }

    // endregion

    // region 左侧工具框（组件 / 事件 / 曲线 / 描述）

    private void renderToolbox(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int top = TOOLBAR_HEIGHT;
        int width = this.toolboxWidth();
        guiGraphics.fill(0, top, width, this.height, 0xF0121218);
        guiGraphics.fill(width - 1, top, width, this.height, 0xFF3A3A46);

        int tabY = this.toolboxTabY();
        int tabWidth = Math.max(1, (width - 4) / this.tabs.size());
        for (int i = 0; i < this.tabs.size(); i++) {
            int tabX = 2 + i * tabWidth;
            boolean active = this.activeTabIndex == i;
            boolean hover = mouseX >= tabX && mouseX < tabX + tabWidth && mouseY >= tabY && mouseY < tabY + 14;
            guiGraphics.fill(tabX, tabY, tabX + tabWidth - 1, tabY + 14,
                    active ? 0xFF3A3A48 : (hover ? 0xFF2E2E38 : 0xFF1C1C24));
            String label = this.tabs.get(i).title().getString();
            guiGraphics.drawString(this.font, label, tabX + (tabWidth - this.font.width(label)) / 2, tabY + 3,
                    active ? 0xFFFFFF : 0xBBBBBB, false);
        }

        // 页签内容区由当前页签自己负责
        this.tabs.get(this.activeTabIndex).render(guiGraphics, this.toolboxContentTop(), mouseX, mouseY);
    }

    private int toolboxTabY() {
        return TOOLBAR_HEIGHT + 2;
    }

    int toolboxContentTop() {
        return this.toolboxTabY() + 16;
    }

    private boolean isInToolbox(double mouseX, double mouseY) {
        return mouseX >= 0 && mouseX < this.toolboxWidth() && mouseY >= TOOLBAR_HEIGHT && mouseY < this.height;
    }

    private void onToolboxClick(double mouseX, double mouseY) {
        int tabY = this.toolboxTabY();
        if (mouseY >= tabY && mouseY < tabY + 14) {
            // 点页签栏：先让旧页签收起自己的编辑/下拉，再切过去
            int tabWidth = Math.max(1, (this.toolboxWidth() - 4) / this.tabs.size());
            int index = (int) ((mouseX - 2) / tabWidth);
            if (index >= 0 && index < this.tabs.size()) {
                this.tabs.get(this.activeTabIndex).onHidden();
                this.activeTabIndex = index;
            }
            this.closeMenus();
            return;
        }
        // 内容区交给当前页签；页签没消费的点击按“点空白”处理（收起编辑/下拉）
        if (this.tabs.get(this.activeTabIndex).mouseClicked(mouseX, mouseY, 0)) {
            return;
        }
        this.closeMenus();
    }

    // endregion

    // region 变量表实时显示

    /// 场景里的第一个发射器（变量表面板用它取当前变量表）。
    /// 暂停时发射器还排在待加入队列里，也要能拿到（否则变量表显示「无发射器」）。
    @Nullable
    private ParticleEmitter firstEmitter() {
        Iterator<Int2ObjectMap.Entry<ParticleEmitter>> iterator = this.manager.getEmitters();
        if (iterator.hasNext()) {
            return iterator.next().getValue();
        }
        return this.manager._peekPendingEmitter();
    }

    /// 变量表面板的一行：`[变量名] = [值]`，名字和数据都可编辑。
    /// `key` 规范化为 `variable.<名字>`；`expr` 为空表示不覆盖（显示发射器变量表里的实时值）。
    private static final class VariableRow {
        String key;
        String expr;
        /// 打开编辑框时的初始文本（值字段没改动就不覆盖实时变量）。
        String initialText = "";
        @Nullable
        MolangEditBox keyBox;
        @Nullable
        MolangEditBox valueBox;

        VariableRow(String key) {
            this.key = key;
            this.expr = "";
        }
    }

    // endregion

    public static void open() {
        Minecraft.getInstance().setScreen(new EditorScreen());
    }
}

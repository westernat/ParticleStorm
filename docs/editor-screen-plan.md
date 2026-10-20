# EditorScreen 计划书

> 粒子编辑器主界面（`org.mesdag.particle_editor.EditorScreen`）的设计与实现说明。
> 本文随实现同步更新；如与代码冲突，以代码为准，并回头修正本文。

## 1. 目标与范围

- 提供一个**客户端 GUI**，用独立的轨道相机预览已加载的粒子效果；
- 场景里可以有**多个发射器**，全部由 `MolangParticleManager` 管理；
- 打开时默认发射 `snowstorm:loading`，通过顶部工具栏的「Loaded」下拉可切换到其它已加载粒子（切换 = 清空场景后重建）；
- 当前阶段只做「显示 + 相机操作」，JSON 编辑、时间轴、导出等后续再加。

编辑器只能在世界内由指令打开：`/particlestorm editor [<particle>]` → 服务端发给发起玩家 `OpenEditorPacketS2C` → 客户端 `EditorScreen.open()`。

## 2. 设计原则（已与需求方对齐，不可随意推翻）

1. **生命周期**：`EditorScreen` 由 `minecraft.screen` 唯一持有；关闭/替换/`init()` 重建后整棵树（manager、发射器、粒子）交给 GC。**不做任何“释放先前状态”的逻辑**，也没有 `removed()` 清理。
2. **空值判断**：`init()` 先于 tick/render/鼠标回调执行，所以 `manager`/`camera`/`frustum` 不需要判空；编辑器只能在世界内由玩家指令打开，所以 `minecraft`、`minecraft.player`、`minecraft.level` 也必然非空。
3. **manager 是场景容器**：`EditorScreen` **不持有 `ParticleEmitter` 字段**；遍历/统计/添加/删除/清空都通过 manager 完成。
4. **路由分工**：
   - 编辑器内部明确知道目标 → 直接调 manager（`_addEmitter` / `_removeEmitter` / `clearScene` 等）；
   - `MolangParticleEngine.addEmitter/addParticle` 这些静态方法只为「创建点拿不到 manager 的子发射器/粒子」按类型路由到编辑器 manager 或世界引擎。
5. **相机**：正交投影（建模软件风格），初始重置为标准正交视角；左键拖拽绕焦点旋转、右键拖拽平移场景、滚轮缩放（改 `zoom` 而不是 `distance`）。
6. **深度**：`MolangParticleManager.renderParticles` 原本由 mixin 注入进原版 `ParticleEngine#render`，依赖那层原版的深度环境（渲染前 `enableDepthTest`，渲染后 `depthMask(true)`）。编辑器在 GUI pass 里要自己补这层环境。
7. **半透明排序**：alpha 混合有顺序要求（先远后近）。排序只在编辑器启用（覆写 manager 的 `orderForRender` 钩子），世界渲染保持原版行为、零额外开销。

## 3. 场景与发射器管理

- `init()` 里创建 `manager = new MolangParticleManager(MolangParticleEngine.INSTANCE.allocator)`，共享 id 分配器；
- 默认发射器：`manager._addEmitter(new EditorParticleEmitter(level, focus, snowstorm:loading, MolangExp.EMPTY))`；
- 下拉列表来自 `MolangParticleEngine.INSTANCE.id2Effect().keySet()`（按 id 字符串排序）；
- 点击下拉项：`manager.clearScene()` → `spawnEmitter(id)`（切换粒子 = 重建场景）；
- `clearScene()`（`MolangParticleManager` 新增，`@ApiStatus.Internal`）：移除全部发射器、回收 id、清空 `particlesForEmitter` / `particlesToAdd` / `groupedParticles`，不动 allocator 本身。

## 4. 渲染管线

`render(guiGraphics, ...)` 的顺序：铺不透明底色 → `renderScene` → `renderOverlay`（工具栏/文字）。

### 4.0 场景背景

世界**照常渲染**（不做任何跳过/拦截），编辑器在 `render` 开头整屏铺一层**不透明纯黑**底色（`fill(0, 0, width, height, 0xFF000000)`）把它盖住：这样既省掉了一个脆弱的世界渲染 hook（之前 `GameRendererMixin` 取消 `renderLevel` 的写法太依赖原版内部结构，已移除），也不用操心清屏/光照贴图更新——全都回到原版流程。

工具框、工具栏、变量表面板的半透明底色都是叠在这层黑底上的；3D 场景照样只画在右 2/3 视口里（scissor）。

### 4.1 为什么不能直接画

GUI pass 里 RenderSystem 的状态是：正交像素投影（`0..guiW, 0..guiH, near 1000, far ~11000`）+ 带 GUI 平移的 model-view。而粒子顶点是**相机相对坐标**，直接用 GUI 矩阵会落到屏幕外或裁剪范围外。

`renderScene` 临时替换（场景视口 = 左侧工具框右边的 2/3 区域，宽度 `width/3` 给工具框）：

- 视口：`left = toolboxWidth()`、`top = TOOLBAR_HEIGHT`；正交投影的 NDC 始终映射整个窗口，视口只是 scissor 裁出的子区域；
- 投影：`setOrtho(-halfW+offsetX, halfW+offsetX, -halfH+offsetY, halfH+offsetY, 0.05F, 2048.0F)`，`halfH = BASE_HALF_HEIGHT / zoom`，`halfW = halfH * (width/height)`（**屏幕宽高比**，保持方形像素；用视口宽高比会横向压扁）；`offsetX/offsetY` 把 NDC 原点平移到视口中心（`halfW*(W-2·cx)/W`、`halfH*(2·cy-H)/H`）；
- 裁剪：`renderScene` 外面套 `guiGraphics.enableScissor(viewLeft, TOOLBAR_HEIGHT, width, height)`，保证场景只出现在右 2/3 视口里（工具框/工具栏底色是半透明的，不裁剪粒子会透进左侧栏）；
- model-view：`rotation(conjugate(camera.rotation()))`（与 `GameRenderer#renderLevel` 给 `LevelRenderer` 的矩阵同构）；
- `RenderSystem.backupProjectionMatrix()` / `getModelViewStack().pushMatrix()` 保存，`finally` 里恢复。

### 4.2 深度环境

- 画之前 `RenderSystem.enableDepthTest()`（原版 `ParticleEngine#render` 提供这层）；
- `finally` 里 `RenderSystem.depthMask(true)`（原版收尾同样这么做）。

### 4.3 视锥

每帧用同一对矩阵构造 `new Frustum(view, projection)` 并 `prepare(cameraPos.x, cameraPos.y, cameraPos.z)`，供 `isVisible` 使用（粒子 AABB 是世界坐标）。

        ### 4.4 场景网格与绘制顺序

粒子绘制前，如果开启了「Scene → Grid」，先画场景网格（见第 6 节）。网格用 `DEBUG_LINES + POSITION_COLOR` 直接构建线段、相机相对坐标、`getPositionColorShader`，以 `depthMask(false)` 绘制（网格不写深度、粒子在其上覆盖）。

### 4.5 绘制顺序（半透明排序）

`MolangParticleManager#orderForRender(type, queue, camera, partialTick)` 是受保护钩子，默认返回 `queue.iterator()`。编辑器在匿名子类里覆写：

- 仅对 `type.isTranslucent()` 且 `queue.size() > 1` 生效；
- 每个粒子的相机距离只算一次（O(n)，`getWorldPosition`）；
- `IntArrays.quickSort(int[] order, IntComparator)` 按 double 键排序（不装箱、不在比较器里重复换算位置）；
- 返回惰性迭代器（按 `int[] order` 取 `instances[...]`），避免每帧 `ArrayList` 及填充拷贝；
- 非半透明类型/单粒子直接返回 `queue.iterator()`。

### 4.6 材质与深度写入

- `particles_alpha` → `PARTICLE_SHEET_LIT`，`particles_opaque`/`particle_sheet_opaque` → `PARTICLE_SHEET_OPAQUE`，默认 `PARTICLE_SHEET_TRANSLUCENT`，这些原版类型都写深度（`depthMask(true)`）；
- 本模组的 `PARTICLE_ADD` / `PARTICLE_BLEND` **不写深度**（`depthMask(false)`）。原因：写深度必须 `discard` 透明纹素，而 `discard` 是二元的（淡出会在阈值处硬切）；因此平滑淡出 + 正确遮挡只能靠**按距离排序后混合**；
- `renderParticles` 的着色器选择：半透明类型（未装 Iris）使用 `particle_no_discard`（保留平滑淡出），其余原版 `particle` 着色器。

## 5. 相机与交互

- `PreviewCamera extends Camera`：用 protected 的 `setPosition` / `setRotation` 摆放相机；`orbit(focus, yaw, pitch, distance)`，并提供 `forwardOf/rightOf/upOf` 基向量换算；
- 初始视角：`yaw = 45°`、`pitch = 30°`、`zoom = 1`，焦点 = `minecraft.player.position()`（同时记为场景原点 `sceneOrigin`）；
- 鼠标覆写：
  - 左键长按拖动：`yaw += dx * ORBIT_SENSITIVITY`，`pitch = clamp(pitch + dy * ORBIT_SENSITIVITY, -89, 89)`；
  - 右键长按拖动：焦点沿相机 right/up 平移（按“像素 → 世界单位”换算，`2·halfH / height`），**XZ 方向限制在场景原点 ±8m 的 16m 网格内**（Y 不限）；
  - 滚轮：`zoom *= 1.15 / 1.15`，范围 [0.25, 8]；
  - `mouseClicked` 里对 left/right 记录拖动模式，`mouseDragged` 分派，`mouseReleased` 复位；
- 相机距离 `CAMERA_DISTANCE = 64` 只影响近/远裁剪，不影响正交缩放。

## 6. 顶部工具栏、粒子下拉与场景工具

- 顶部一条工具栏（高 `20`）：
  - 按钮宽度**按文本动态计算**（`EditorScreen#fileButtonWidth/loadedButtonWidth/sceneButtonWidth`）：普通按钮 = 文本宽 + 8px 内边距；下拉按钮 = 文本宽 + 8px 内边距 + 14px 箭头占位；按钮间距 6px，依次排布；
  - 最左边「File」下拉按钮（`particle_editor.file`），和 Loaded/Scene 一样是下拉列表，三项：
    - **New / 新建**：新建编辑器文档——默认组件集（steady 发射率 / looping 生命周期 / point 形状 / 初速 / 寿命 / billboard 外观）+ 沿用当前文档的贴图与材质，identifier 为 `particlestorm:new_particle`（可在描述页签改），清场景后用新效果重新发射；
    - **Save / 保存（保存到内存）**：把编辑器里当前编辑的完整效果写回 `MolangParticleEngine`
      （`MolangParticleEngine#saveEffect`）：用 `ParticlePreset` / `EmitterPreset` 重建该 id 的预设，
      并按 `description.identifier()` 覆盖 `id2Effect` / `id2Particle` / `id2Emitter`（`HashMap` 拷贝后替换引用，不破坏别的条目）。
      保存后立刻刷新 Loaded 下拉、选中该 id 并清场景重新发射——内存里的数据一直有效到下一次资源重载
      （重载会按磁盘上的定义覆盖回来）。成功显示绿色「已保存到内存：<id>」，失败显示红色「保存失败：<原因>」；
    - **Export / 导出（写盘）**：把编辑器里当前编辑的完整效果按引擎的粒子定义格式写盘：
      `{"format_version": "1.10.0", "particle_effect": <DefinedParticleEffect.CODEC 编码结果>}`
      → `<游戏目录>/particlestorm/editor/<namespace>/<path>.particle.json`（id 里的 `/` 换成 `_`），
      编码/写盘失败时在左下角显示红色错误，成功则显示绿色「已导出：<相对路径>」；文件可以直接拷进资源包的
      `assets/<namespace>/particle_definitions/` 里被引擎加载（引擎按 `description.identifier` 建索引，文件名不参与 id）；
  - 「Loaded」下拉按钮（只显示 `Loaded`，不显示粒子 id；当前粒子内容见左侧工具框「描述」页）；
  - 「Scene」下拉按钮（场景工具）；
  - 两个播放控制按钮（18px 方形图标按钮，紧跟 Scene 之后）：
    - `▶`：**从头播放**——清空场景后用当前缓存的 `effect` 重新发射，并把 `playing` 置回 true；
    - `⏸`：**切换暂停与播放**——图标固定为「两条竖条」，不随播放状态变化；`playing` 为 false 时 `EditorScreen#tick` 不推进 `manager`（粒子/发射器年龄冻结，恢复后从暂停点继续）；
  - **`Variables` 工具**（⏸ 右侧的文本按钮，配色与其它工具栏按钮一致）：在按钮**正下方**展开变量表面板（像下拉列表，宽 `max(200, min(340, toolboxWidth+60))`，超出屏幕高度时面板内滚轮滚动）。打开时把**当前发射器变量表里的所有变量**（默认的 `v.emitter_age` / `v.emitter_lifetime` / `v.emitter_random_1..4` 以及自定义变量）补成可编辑行，每行显示 `key = value`：
    - **key 可改**：点名字区打开 [MolangEditBox]，**编辑框拉伸到整个面板宽度**（`面板宽 - 8`，编辑时该行只剩输入框），输入 `v.foo` / `foo` / `variable.foo` 都行，提交时统一规范化为 `variable.<名字>`（面板显示成 `v.<名字>`）；改名撞上别的行会替换掉那一行；
    - **value 可改**：点值区同样打开整行宽的编辑框，内容是 `key = value` 里显示的**实时值**（没改过就原样提交，不会覆盖）；改成别的数字/molang 表达式后作为该变量的表达式注入；
    - 编辑框横向滚动后按**整段文本**分词绘制（`MolangSyntax#drawRange`），所以 `v.xxx` 的 `v.` 被滚出可视区、只剩 `.xxx` 时颜色/高亮也不会丢；
    - 右侧 `-` 删除该行，底部 `+ 添加变量` 新增空行（key 为空时显示占位名）；
    - **换文档清空**：从 `Loaded` 切换粒子或点 `New` 新建文档时清空自定义变量（`clearVariables()`：收起面板、清空所有行、重置滚动），打开面板时再按新发射器的变量表重新补行；
    - 改动后重建发射器（清场景 + 重新发射）。**注入方式不改 `EmitterPreset`**：预设照常用 3 参数构造，然后
      `MolangParser parser = new MolangParser(preset.vars);` + `new MolangExp(变量名→表达式).compile(parser)`，
      把变量表编译成 `variable.x = 表达式;variable.y = 表达式` 这样的**赋值表达式**（`EditorParticleEmitter`）：
      编译时 `MolangParser#getVariableFor` 会把变量名登记进 `preset.vars`（`computeIfAbsent`），赋值则在这段表达式
      求值时执行（`VariableAssignment#get` → `instance.getVars().setValue`）。发射器在 `createVars()`（组件初始化前）
      和每 tick 开头各求值一次，所以组件的 molang 里可以直接引用、`v.emitter_random_1` 这类默认变量也能被覆盖；
      整批编译失败时逐个编译，仍失败的登记成 `0`（避免组件引用一个永远没被赋值的变量导致无限递归求值）；
- 下拉统一由 `editor/DropdownMenu` 组件负责：列表渲染、悬停/选中高亮、按屏幕高度截断行数、滚动（`contains`/`scroll`，放不下时带滚动条）、命中检测；按钮本身由 `EditorScreen` 绘制；**列表宽度不跟按钮走**——每帧按「当前条目里最长的文本宽度 + 10px 内边距」计算，按钮宽度只是 `minWidth` 下限（工具栏按钮 48px 时列表仍按内容展开）；
- 四个下拉（Loaded / Scene / type / material）同一时间只展开一个（`closeMenus()` 互斥）；ESC 或点击下拉以外区域关闭；
- 「Loaded」下拉（宽 `170`、行高 `12`）列出全部已加载粒子 id；点击 → `manager.clearScene()` + `spawnEmitter(id)`，并收起；
- 「Scene」下拉列出 `[x] Grid (16m)`、`[x] Axes`、`[ ] Blocks` 三个开关（Blocks 打开时多出 `size x/y/z` 三行）；**选中开关后下拉保持展开**，等失焦（点下拉以外区域 / ESC / 打开其它下拉）才关闭，方便连续切换多个开关；
- 工具栏右侧显示发射器/粒子数量统计，底部是操作提示。

### 6.1 场景网格、坐标轴与方块

- 以**场景原点**（打开编辑器时的玩家坐标）为锚点的 XZ 平面网格：1m 一格、总共 16m × 16m（`GRID_RADIUS = 8`，每个方向 17 条线）；中心线略亮；
- **网格原点**处画 1m 长的红/绿/蓝 XYZ 轴线段（只画**正方向**：原点 → +X / +Y / +Z，`AXIS_LENGTH = 1`），与网格一样锚定在世界里，不随相机/焦点移动；轴线段在方块/粒子之后绘制并关闭深度测试，所以会“透视”挡在前面的方块和粒子；
- 场景绘制顺序：网格 → 方块 → 粒子 → 坐标轴。方块先写深度；粒子开启深度测试但不写深度，被方块挡住的部分不绘制；坐标轴最后画、关深度测试，始终可见；
- 场景 pass 结束后（`renderScene` 的 finally 之后）会 `RenderSystem.clear(GL_DEPTH_BUFFER_BIT)`：场景写的是编辑器正交投影的深度，和 GUI 深度不在同一投影里，不清掉的话工具栏下拉等 GUI 元素会在方块像素上被深度测试挡掉；
- 平移（右键拖动）时焦点被限制在该网格的 XZ 范围内，网格本身不移动；网格不写深度，粒子绘制在其上。
- Scene 下拉项：
  - `[x] Grid (16m)`：网格显示开关；
  - `[x] Axes`：XYZ 红绿蓝轴线段开关（与网格独立，关闭网格仍可单独显示/隐藏轴线）；
  - `[ ] Blocks`：渲染场景原点附近的方块开关；打开后下拉里多出 `size x / size y / size z` 三行，点行就地弹原版 EditBox 编辑该轴方块数（范围 1..64，`ENTER` 提交、`ESC` 取消）；
- 方块渲染以 `sceneOrigin` 为中心、按 `blockSizeX/Y/Z` 取区域，**按原版 `tesselateWithoutAO` 流程自己实现，并把 `renderModelFaceFlat` 的取光逻辑改到我们这一层**：每个方块 `pushPose` → `translate(方块坐标 - 相机坐标)` → `BakedModel#getQuads(state, direction, random, ModelData, RenderType)`/`getQuads(state, null, …)` → `Block.shouldRenderFace` 剔除 → `getShade` 平光 + `BlockColors` 染色 → `putBulkData`；**光照选择**：区域内部方块的面用邻居的光（`getLightColor(level, state, relativePos)`），区域最外层“被切出来”的面和 `side == null` 的面用方块自身的光（`getLightColor(level, state, pos)`）——原版 `renderModelFaceFlat` 默认拿邻居的光，邻居在渲染区域外/被切面贴着实心方块时就是黑的；另外地下方块的 block light 和 sky light 都是 0，采样到的光也是 0（纯黑），所以取到的光再经 `ambientLight()` 保底（block light ≥ 2、sky light ≥ 4），地下被切出来的面就是暗灰色而不是黑；`Lighting.setupLevel()` 在场景 pass 里设置光照方向；**只有区域内部方块开启剔除**；场景 pass 前 `FogRenderer.setupNoFog()`、`shaderColor` 归白；写深度，能正常遮挡其后的粒子；只渲染非空气方块。

### 6.2 左侧工具框（组件 / 事件 / 曲线 / 描述）

- 工具栏下方、屏幕左侧的面板，**初始宽度 = 屏幕宽度 1/3**（场景显示在右侧剩下的 2/3）；顶部 4 个页签：描述 / 组件 / 事件 / 曲线；
- **页签是组件**：每个页签继承 `editor/EditorTab`（`title()` / `render()` / `mouseClicked()` / `keyPressed()` / `charTyped()` / `scroll()` / `onHidden()`）；页签栏由 `EditorScreen` 绘制，按到哪个页签就切到哪个页签的内容区——「描述」用 `DescriptionTab`，组件 / 事件 / 曲线用 `EffectListTab`（只读列表）；切走时旧页签 `onHidden()` 收起自己的编辑框与下拉；
- 绘制顺序：**先工具框、后工具栏**——工具栏的下拉列表从 y=20 开始、位于工具框同一高度区间，若工具框后画会把「Loaded / Scene」下拉盖住；
- 点击顺序同样让下拉优先于工具框判定（列表与工具框区域重叠，否则点列表项会被工具框吃掉）；
- 文本被截断显示 `...` 时，鼠标悬停会显示完整文本的 tooltip（`hoveredTooltip` 在 overlay 末尾统一渲染）；展开的下拉会盖住其下方的文本，`isOverOpenMenu` 保证这些被盖住的文本不再触发 tooltip；
- **渲染时不做无用收集**：只有当前页签会被 `EditorScreen` 调用 `render`；`ComponentsTab` 的组件列表**不再每帧为每个族 new 一个 Grid**（`Family` 只存 entry / present 列表），点开某族时才用 `entry.factory()` 创建 Grid 并 `load`；「添加组件」候选（`canAdd` 需要实例化 Grid 询问）按**当前效果实例缓存**，效果被替换时自然失效，避免每帧实例化一堆 Grid。- **组件页 `ComponentsTab`**：按**组件族**列出当前效果里的组件（`RegisterComponentGridEvent.Entry`，按 `IComponent#order` 排序），点族 → 进入该族的编辑 Grid（上方「返回」行；非必需族下方还有「删除组件」行）；
  - 每个组件族一个 `ComponentGrid` 子类：每行是「标签在上、值缩进在下一行」（行高 24）——`text`（原版 EditBox）/`select`（DropdownMenu）/`toggle`/`info`/`button`（点一下执行动作）五种内置行，另有直接 `addRow` 加进去的自定义行（调色盘 `PickerRow`、渐变色条 `GradientRow`）；`vector`/`pair` 行把 N 个分量各占一行（FloatMolangExp3 三行、FloatMolangExp2 两行，行高随数量变高；另有通用的 `vector(label, count, values, commit)` 直接按 N 行文本收发，供自定义行数使用），**和单行文本框一样平时显示纯文本、点某一行才在该行打开 EditBox**；`ENTER` 提交当前行、`ESC` 取消、失焦自动提交；行提交后由 `ComponentsTab` 收走 `build()` 结果（`BuiltComponent(id, component)`）；
  - **Molang 语法高亮**：`MolangSyntax` 把文本切成 token 着色（数字绿 / 字符串橙 / 关键字紫 / `math.*`、`query.*`、`variable.*` 等变量蓝 / 函数黄 / 注释绿灰），`text`/`vector` 行的值显示和编辑框都用它绘制；编辑框是**完全自绘的 `MolangEditBox extends AbstractWidget`**（不再继承原版 EditBox，避免原版文字色/批次顺序与自绘高亮互相覆盖）：背景、token 文字、光标都由本类画，支持输入 / 退格 / Delete / 左右移动（Ctrl 按词）/ Home / End / 点击定位 / **鼠标拖动选中** / **Shift+方向键选中** / **Ctrl+A 全选** / **Ctrl+C/X/V 复制、剪切、粘贴**（选中部分半透明蓝底；`ENTER` 提交、`ESC` 取消由宿主行处理）；超过内宽时自己维护显示起点保证光标可见；光标在末尾画 `_`、中间画竖线，300ms 闪烁；
  - **被截断的 Molang 文本 tooltip 也带高亮**：`TextRow` / `VectorRow` 悬停截断值时把原文放进 `EditorScreen#highlightedTooltip`，overlay 末尾用 `renderMolangTooltip` 折行后按 token 着色画（普通 tooltip 仍走 `hoveredTooltip`）；
  - 列表里的组件名和 Grid 里的行标签都用翻译键：组件 `particle_editor.<namespace>.<path>`、字段 `particle_editor.<namespace>.<path>.<field>`（例如 `particle_editor.minecraft.particle_initialization.per_render_expression`）；**多子类型族显示族名**（`particle_editor.minecraft.emitter_shape` → "Emitter Shape"，rate/lifetime 同理），单 id 组件显示自身组件名；没有对应翻译时原样显示键，方便补 lang；
  - **下拉选项也走翻译键** `particle_editor.minecraft.<group>.<value>`：`ComponentGrid#optionLabel` / `localizedOptions` / `variantOptionLabels` 负责显示名（值本身仍是下标语义，不影响 build）。同族子类型用组件 id 的完整 path 作 value（如 `particle_editor.minecraft.emitter_rate.emitter_rate_instant`）；其它选项用「字段名_值」作 value（如 `...minecraft.emitter_shape.direction_inwards`、`...minecraft.particle_initial_speed.mode_scalar`、`...minecraft.particle_appearance_billboard.face_camera_mode_rotate_xyz`）；
  - lang 里已经补全全部组件的显示键：29 个组件名 + 所有 Grid 行字段（含各子类型）、`particle_appearance_billboard` 嵌套的 `uv.*` / `uv.flipbook.*` 参数、`particle_appearance_tinting` 的 `mode*` / `interpolant` / `range` / `color`、事件 / 碰撞 / 多形状 Grid 的字段与按钮、以及全部下拉选项（`particle_editor.minecraft.<group>.<value>`），中英各 262 条；
  - **同族多子类型只写一个 Grid**：`EmitterRateGrid`（instant/steady/manual）、`EmitterLifetimeGrid`（looping/once/expression）、`EmitterShapeGrid`（point/sphere/box/disc/entity_aabb）各自带一个「类型」下拉，切换时重建字段行并重新发射；
  - **族特性决定交互**：`required` 族（emitter_rate / emitter_lifetime / emitter_shape）不可删除；其它族可选，用底部「添加组件」下拉增删；同族只能存在一个实例（编辑/切换类型时先移除同族全部 id 再放入新 id 的实例）；
  - Grid 里展开的下拉列表在 `grid.render` 最后绘制，会盖住下方的「删除组件」行（删除行先于 Grid 绘制，点击判定也是先 Grid 后删除行）；
  - **Grid 超出屏幕高度可滚动**：`ComponentsTab` 持有 `gridScroll`（像素），行从 `rowsTop - gridScroll` 开始绘制并用 `enableScissor` 裁剪到 `[返回行下方, 屏幕底]`；滚轮在可视区内滚动内容（每次 12px，clamp 到 `[0, content - 可视高]`），展开的 `SelectRow` 下拉先把滚轮吃掉滚自己的列表；「返回」行固定不滚，切换/关闭 Grid 时偏移归零；
  - **事件 / 碰撞 / 多形状 Grid**（`GridEventComponents.java`，原先都是 `GridReadOnly`）：
    - 事件类 Grid 统一继承 `EventListGrid`：**所有**事件引用入口（`creation_event` / `expiration_event` / `timeline` / `travel_distance_events` / `looping_travel_distance_events` / 碰撞 `events`）都用「＋ 添加…」按钮点开**可用事件下拉**（列出当前效果 `effect.events` 里定义过的事件名；没有可用事件时下拉里只有一条 `No events available` 占位项，点了不做事，不再弹常驻错误），选中后才会把该事件引用加进条目；每个条目下面都有独立的 `× 删除` 按钮（删的是该条，不是只删最后一个）；下拉的渲染/点击/滚轮/tooltip/ESC 收起都在基类里处理；
    - `particle_lifetime_events`：`creation_event` / `expiration_event` 各是一组「一条一个事件引用」的行（行内可直接改事件名）+ `timeline` 条目（每项两行向量：时间、事件列表）；
    - `emitter_lifetime_events`：同上再加上 `travel_distance_events`（移动距离）与 `looping_travel_distance_events`（循环移动距离）两组条目；
    - `particle_motion_collision`：`enabled`（bool 或 molang 表达式文本）、`collision_drag` / `coefficient_of_restitution` / `collision_radius` 三个数值文本行、`expire_on_contact` 开关、`events` 条目（事件名 + 最小速度）；
    - `emitter_multi_shape`：每个条目 = `condition`（bool 或 molang 表达式）+ 一整套形状字段（和单独的形状 Grid 共用 `ShapeFields`，子类型/方向/平面切换时会重建全部行），按钮增删条目；多条目行标签用字面量 `shape[i].字段`；
  - **组件字段行构造统一走 `ShapeFields`**：`EmitterShapeGrid` 与 `emitter_multi_shape` 共用同一套「类型下拉 + 各子类型字段 + 自定义方向/平面按需显隐」的实现，`prefix` 为 null 时用翻译键、非 null 时用字面量前缀；
  - **`particle_appearance_tinting` 专用 Grid**（`TintingGrid.java`）：组件 codec 是 `Codec.either(Color.CODEC, ColorField.CODEC)`，Grid 用「模式」下拉在**三种**形态间切换、数据各自保留：
    - `Static`（`ParticleAppearanceTinting$ColorField` 常量）：一个调色盘（`ColorPicker`：**SV 方形色块**（横轴饱和度、纵轴明度，按住拖动同时改 S/V）+ **色相横条** + **不透明度横条**，最底一行是 `RGB` / `HSV` / `HEX` 三选一**可编辑读数**——点格子直接输入（`ENTER` / 点别处提交、`ESC` 取消；RGB 为 0..255、HSV 角度/百分比、HEX 为 `#RRGGBB`/`#RRGGBBAA`），右侧 `^` / `v` 两个箭头按钮**上下叠放**循环切换显示模式），输出四个常量通道；
    - `Gradient`（`ParticleAppearanceTinting$Color`）：`interpolant` 表达式行 + `range` 行（**色标位置 × range = 渐变键**，即用 range 自动计算 gradient；载入时 range 取 `gradient.range`、位置 = 键 / range）+ 一条横向渐变条：
      - 渐变条按每个色标的颜色逐像素插值预览，每个色标是一个色块，**长按左键可左右拖动**（拖动时不能越过相邻色标）；
      - 条右侧 `+` 添加色标：插到最大空隙中间、颜色取该点插值结果、**插入后自动选中**；
      - 选中色标后下方展开调色盘（拖动只改草稿，**松手**才把颜色写进组件并重新发射）和 `×` 删除（至少保留一个色标）；
    - `Expression`（`ColorField` 表达式）：红 / 绿 / 蓝 / 透明四个通道各一行 molang 表达式；
    - 载入分类：`colorField` 非空时全常量 → Static、否则 → Expression；`colorField == EMPTY` → Gradient。渐变/Static 的色标与常量在编辑器里只能编辑常量颜色，所以载入时会把表达式折叠成常量（渐变里的表达式通道也按常量读）；
    - `build()` 只填当前模式那一边、另一边写 `EMPTY`（与 codec 写回时选 Either 的逻辑一致）；`range` 非法或非正数时抛 `IllegalArgumentException`（宿主显示错误、保留旧组件）；
  - 拖拽交互走 `Row#mouseDragged` / `#mouseReleased`（默认 no-op）：`ComponentGrid` → `ComponentsTab` 逐层转发，`EditorScreen#mouseDragged` 先把拖动交给当前页签（页签不消费才走相机）；
  - **拖拽不逐帧重建**：重建组件会清场景重新发射，拖着调色盘 / 渐变色标就是不停重放粒子。`PickerRow`、`GradientRow` 拖动期间只改内部草稿并记 `pendingChange`，
    `mouseReleased`（以及单击读数格子、`+` / `×`、`ENTER` 提交这类离散操作）才 `flushPendingChange()` 调一次 `onChange` → `markChanged()` → 重建组件并重新发射；拖拽全程 `consumeChanged()` 保持 false；
  - **按模式显隐的行**：`ParticleAppearanceBillboardGrid` 的 `direction_mode` = 由速度推导时只显示 `min_speed_threshold`，= 自定义方向时只显示 `custom_direction`（下拉切换后 `rebuildRows()`，`load()` 末尾也重建行，不再持有行字段引用）；`EmitterShapeGrid` 同理：`direction` 选「自定义」才显示 `custom_direction`，`plane_normal` 选「自定义」才显示 `custom_plane`；
  - **不用 codec**：Grid 的 `build()` 直接用当前字段 **new 一个新组件** 替换 `effect.components`，再清场景并重新发射；
  - 内置 Grid 在 `editor/ComponentGrids` 注册，注册 id 全部直接用组件类的 `ID` 常量（Grid 本身不持有 id 字段）；**自定义组件**可在模组总线监听 `api/RegisterComponentGridEvent` 注册自己的 Grid（`register(MyComponent.ID, (screen, font) -> new MyGrid(...))`，多子类型用 `register(defaultId, label, variants, required, factory)`）；
  - 目前**全部组件都可编辑**：`emitter_rate`、`emitter_lifetime`、`emitter_shape`、`emitter_local_space`、`emitter_initialization`、`emitter_lifetime_events`、`emitter_multi_shape`、`particle_initial_speed`、`particle_initial_spin`、`particle_initialization`、`particle_motion_dynamic`、`particle_motion_parametric`、`particle_motion_collision`、`particle_lifetime_expression`、`particle_lifetime_events`、`particle_kill_plane`、`particle_expire_if_(not_)in_blocks`、`particle_appearance_billboard`、`particle_appearance_lighting`、`particle_appearance_tinting`（`GridReadOnly` 已删除）；
- **事件页 `EventsTab`**（`editor/EventsTab.java`）：编辑 `DefinedParticleEffect#events`（组名 → 节点树），三级视图：（内容超过屏幕高度时可滚轮滚动，行按滚动偏移绘制并裁剪到内容区）
  - 节点在 JSON 里的**键就是节点类型名**（`IEventNode.CODEC` 是按名字分发的 map），所以编辑器不提供「键」字段；同一容器里每种类型最多一个节点，需要多个同类节点请放进 `sequence`；
  - **添加规则**：`sequence` / `randomize` 只能加进空容器，加进来之后该容器不能再加其它节点（只能进「子节点」里加）；**叶子节点**（`log` / `expression` / `sound_effect` / `particle_effect`）同一容器里每种最多一个；容器里出现任意一种叶子节点后，只能再添加**其它种类**的叶子节点（例如已有 `log` 时只剩 `expression` / `sound_effect` / `particle_effect`，已有 `expression` 时只剩 `log` / `sound_effect` / `particle_effect`），`sequence` / `randomize` 不再可加；`randomize` 的每个条目是独立 map，条目内可添加任意节点类型；
  - `weight` **不是可添加节点**：它只是 `randomize` 条目的权重字段（条目编辑器里多一行「权重」，条目列表里显示 `w权重 · 类型`）；
  - **事件组列表**：第一行「新事件」文本行（`ENTER` 创建空组）；每行组名 + 节点数；组内视图底部可「删除事件组」；
  - **组内节点列表**：每行节点摘要（如 `log: 消息`、`particle_effect: 效果id`、`sequence (n)`），点进入节点编辑器；`+ 添加节点` 下拉列出当前可添加的类型（没有可加类型时显示 `No node types available` 占位项）；
  - **节点编辑器**：类型字段（log→消息；expression→表达式+记录日志；sound_effect→音效 id；particle_effect→效果 id / 效果类型 / 前置表达式 / 共享变量（每个变量一行，`+` 添加、`-` 删除）；weight→权重），`sequence`/`randomize` 有「子节点」入口与「删除节点」；
  - 数据是**草稿树**：载入时从 `effect.events` 深拷贝（sequence 展开子节点；randomize 的条目折叠成「权重 + 单个节点」，多节点时折成 sequence），每次提交后用草稿重建整个 `events` map → 替换 `screen.effect` → 清场景重新发射；解析失败（无效 id、同容器重复类型）显示错误并保留旧效果；
  - 行复用组件 Grid 的行组件（`ComponentGrid.TextRow/ButtonRow/ToggleRow/SelectRow` + `DropdownWidget`），事件 UI 文案键 `particle_editor.event.*` / `particle_editor.event_node.*`。
- **曲线页 `CurvesTab`**（`editor/CurvesTab.java`）：编辑 `DefinedParticleEffect#curves`（曲线名 → `ParticleCurve`），两级视图（内容超过屏幕高度时可滚轮滚动，渲染裁剪到内容区）：
  - **曲线列表**：第一行「新曲线」文本行（`ENTER` 创建默认 LINEAR 曲线：`input=query.life_time`、`horizontal_range=1`、节点 `0,1,0`）；每行显示 `名字 (类型)`，点击进入编辑器；
  - **曲线编辑器**：`name`（曲线名可改，即输出变量名；重命名换 map 键、保持顺序、重名报错）+ `type` 下拉（显示名为翻译键 `particle_editor.curve_type.*`：Catmull-Rom / 线性 / 贝塞尔 / 贝塞尔链）+ `input` / `horizontal_range` 两个 molang 文本行 + **可交互曲线预览**（60px 高的采样折线图，按类型分别用线性 / Catmull-Rom / de Casteljau / 分段三次贝塞尔求值，节点表达式无法求值时跳过该点）：点预览空白处**添加节点**、点节点**选中**（白色高亮）、按住节点**拖动**改 value（`BEZIER_CHAIN` 还能左右拖时间，且不会越过相邻节点），`BEZIER_CHAIN` 的每个节点还带一个**切线手柄**，拖手柄改 slope；选中节点后预览下方出现该节点的值编辑行（`BEZIER_CHAIN` 为三行：时间 / value / slope；其它类型为一行 molang），并可「删除选中节点」；
    - `BEZIER_CHAIN`：每个节点一个三行向量行（时间 / value / slope），`+` 追加（时间自动取最大值 +1）、每条自带 `-` 删除；
    - 其它类型：每个节点一行 molang 表达式文本，`+` 追加、每条自带 `-` 删除；
    - 底部可「删除曲线」；
  - 切类型时草稿会自动把另一种节点表示补齐（`values` ↔ `nodes` 互转），不会出现空的节点列表导致引擎崩；
  - 数据是草稿，改动后整体重建 `curves` map → 替换 `screen.effect` → 清场景重新发射；拖节点松手（`mouseReleased`）与按键/点击都会 `flushDirty()` 立刻落地；文案键 `particle_editor.curve.*`。
  - **坑（已修）**：换曲线时 `effect.components` 是原样复用的，而 `MolangExp#compile` 把结果缓存在 exp 对象上、绑死当时的 `Variable`，
    曲线变量原本又闭包捕获构建时的 `ParticleCurve`，于是「改曲线后引用它的组件（如广告牌外观）不更新」。现在
    `ParticlePreset` 注册的曲线变量改成**按名字晚绑定**：求值时用 `((IMolangParticleInstance) instance).getPreset().effect.curves.get(曲线名)`
    现查实例所属预设的曲线，旧表达式也能读到新曲线；非粒子实例（发射器侧求值）退回构建时那条曲线，曲线被删则返回 0。
    这样不用重建组件对象，也不会把编辑器里的曲线改动漏给引擎的同名预设（世界里的粒子仍按加载时的定义走，除非点了 Save）。
- **描述页 `DescriptionTab`**：description 编辑的**全部入口都在这一个页签里**——ParticleDescription 表单（`identifier`、`type`、`material`、`texture` 四行）连同编辑框/下拉都由这个页签负责，不再散落在 `EditorScreen` 里；四行标签用 `particle_editor.description.<field>` 翻译键（lang 已补）；**行布局和组件 Grid 一致：标签在上、值缩进在下一行**（行高 24，`LABEL_Y=2` / `VALUE_Y=13` / `VALUE_X=12`），type / material 的值行右侧有下拉箭头，下拉列表展开在行下方；
  - `identifier` 可编辑：点击行打开原版 `EditBox`（盖在该行的**值行**上，是否在编辑直接看 `identifierBox.isFocused()`），`ENTER` 提交、`ESC` 取消、点别处取消，提交时 `ResourceLocation.tryParse` 校验；新 id 只存在于编辑器文档里（引擎数据不变）；
  - `type` 为**下拉框**：值来自 `RegisterCustomParticleTypeEvent.map()` 的已注册粒子类型（按注册名排序显示），选中即生效；
  - `material` 为**下拉框**：值来自 `DescriptionMaterial.values()`（`@ApiStatus.Internal`，与 CODEC 共用同一份 `values` 数组），显示名走翻译键 `particle_editor.minecraft.material.<serialized_name>`（如 `particle_sheet_translucent` → 「粒子贴图集（半透明）」），值行显示同一个翻译名；其它模组注册的自定义材质没有翻译时原样显示序列化名；
  - 生效流程：**先更新 `EditorScreen#effect`**（完整 `DefinedParticleEffect` 副本）→ **`manager.clearScene()`** → **`spawnEmitter(当前粒子)` 重新发射**；新发射器由 `EditorParticleEmitter` 从缓存重建 `EmitterPreset` / `ParticlePreset`，所以改完立刻生效、不写回引擎；页签直接按包级可见性访问 `EditorScreen` 的字段（`effect` / `error` / `manager` / `selectedParticle` 等），不写 getter/setter；
  - `texture` 可编辑：点该行打开**纹理图集查看器**（`editor/AtlasViewer.java`，盖住屏幕大部分区域的模态浮层）。图集来自材质自己声明的那张：`DescriptionMaterial` 注册时带一个 `Supplier<TextureAtlas>`（`RegisterCustomMaterialEvent#register(name)` 默认粒子图集，`register(name, supplier)` 可指定别的图集），`terrain_sheet` 是方块图集，其余（各种粒子贴图集 / 自定义 / 不渲染）是粒子图集；查看器直接 `material.getAtlas()` 取（懒取，supplier 返回 null 或没给时回落到粒子图集）。交互按 XFactHD/AtlasViewer 的 `AtlasScreen` 移植（那个仓库面向更新的 MC 版本，绘制 API 换成了 1.21.1 的 `GuiGraphics#blit`，交互语义保持一致）：
    - **图集视图**（默认）：直接把整张图集贴图画出来（`blit(atlas, x, y, w, h, 0, 0, atlasW, atlasH, atlasW, atlasH)`；图集像素尺寸从 sprite 的 `getX()/getU0()` 反推）；
    - **缩放**：滚轮**等比缩放**（每格 ×1.2 / ÷1.2，`[1, 32]` 倍「适应窗口」），以光标为锚点——步长乘在当前倍率上，所以高倍率下也不会出现「越放大越难放大」；**平移**：左键在图上拖动（拖动距离 > 3px 才算平移，否则松手时算点击绑定）；
    - **悬停高亮**：光标下的 sprite 画红框（和 AtlasViewer 的 `drawColoredBox(..., expand=true)` 一样外扩 1px、裁到可视区），标题栏显示 `名字 (w×h)`；当前已绑定的贴图画绿框；
    - **搜索**：标题栏下的输入框（复用 `MolangEditBox`）输入名字片段 → 匹配项画橙框并 `1 / N` 计数，`ENTER` / `SHIFT+ENTER` 在匹配项之间跳转（当前项紫框闪烁），跳转即把该 sprite 移到视野中心（格子视图则滚到对应行）；
    - **格子视图**（标题栏右侧按钮切换）：所有 sprite 按名字排序、各自放大成一格（默认 32px，`Ctrl`+滚轮 12~96px），滚轮滚动；方块图集里小 sprite 太多时更好挑；
    - 左键点一个 sprite 即绑定：`RegisterCustomParticleTypeEvent.sprites()` 的共享 `ExtendMutableSpriteSet#addSprite(sprite)` 把它**按贴图资源名**登记进集合 → 新建 `DescriptionParameters(material, sprite.contents().name())` → 替换 `screen.effect` → 清场景重发。`ESC` 或点面板外关闭；打开期间场景 pass 也跳过不画、所有点击/按键被浮层吞掉。
    - sprite 集合**不用下标索引**：`ExtendMutableSpriteSet` 内部是 `Map<ResourceLocation, TextureAtlasSprite>`，渲染时 `MolangParticleInstance` 直接 `sprites.get(parameters.texture())`；所以资源重载后集合重建也不会串图，`EditorScreen#spawnEmitter` 发射前只做一次「按贴图名登记」的兜底（`ensureSpriteRegistered()`，换材质时同样处理，找不到就落到 missing）；
    - **面板布局**：四周各留 16px；顶栏两行（第一行标题 + 右侧悬停 sprite / 搜索状态，第二行搜索框 + 提示 + 视图切换按钮，文本都按可用宽度截断），顶栏与内容区之间有分隔线；内容区统一内缩 4px，命中判定和绘制共用同一套 `layout()` 算出来的坐标；
    - **混合状态**：图集贴图是带 alpha 的，而 GUI 的 blit 底层 `GuiGraphics#innerBlit` 的 10 参数重载**只管画不管混合函数**，16/20 参数重载画完还会 `disableBlend()`；编辑器场景 pass 里 `PARTICLE_ADD` 材质又会 `blendFunc(SRC_ALPHA, ONE)` 且不还原。所以 `EditorScreen#render` 在画 overlay 前复位 GUI 状态，`AtlasViewer` 在**每一次画图集 / sprite 之前**都调用 `ensureAlphaBlend()`：`setShaderColor(1,1,1,1)` + 直接压 GL（`GlStateManager._enableBlend()` + `_blendFuncSeparate(SRC_ALPHA, ONE_MINUS_SRC_ALPHA, ONE, ZERO)`），不依赖 `RenderSystem` 的缓存状态；
    - **绘制顺序（坑）**：`fill` / `renderOutline` 走 `RenderType`，顶点先攒在 `GuiGraphics` 的 BufferSource 里，要等 flush 才真正绘制（这一帧最近的 flush 在 `disableScissor`；`enableScissor` 也会顺手 flush 掉面板底色和顶栏），而 sprite 的 `blit` 是**立即绘制**。所以格子视图里悬停格的白色高亮底必须在 `blit` 前手动 `guiGraphics.flush()`，否则它会排到 sprite 之后、把悬停 sprite 的透明像素糊成白色（看起来就是「半透明失效」）；flush 完之后 `RenderType.gui()` 的 `clearRenderState` 会关掉混合，需要再 `ensureAlphaBlend()` 一次；
    - 未移植（编辑器用不上）：图集下拉切换、隐藏 padding、高亮动画/坏 mip、导出图集、mip 等级滑块、右键 sprite 详情页；
- **换粒子时**：先 `this.effect = id2Effect().get(id)`，再清场景并 `spawnEmitter(id)`（之前的修改不会带到别的粒子）；随后调用 `onEffectReplaced()`：遍历所有页签执行 `EditorTab#onEffectReplaced`（默认收起编辑，`ComponentsTab` 直接丢弃打开的 Grid / 下拉 / 滚动偏移，**不提交旧 Grid 的改动**），再切回**描述页**（`DESCRIPTION_TAB = 0`）；「新建」同样会走这套重置；
- **组件 / 事件 / 曲线页**都已可编辑（组件 Grid、事件节点树、曲线编辑器）。

## 7. 关键代码位置

| 内容 | 位置 |
|---|---|
| 主界面/相机/渲染/交互/工具框骨架 | `editor/EditorScreen.java` |
| 工具框页签基类（页签栏由 EditorScreen 绘制） | `editor/EditorTab.java` |
| 描述页签（description 全部编辑入口） | `editor/DescriptionTab.java` |
| 组件页签（列表 + 增删 + 编辑 Grid 宿主） | `editor/ComponentsTab.java` |
| 组件编辑 Grid 基类与字段行 | `editor/ComponentGrid.java` |
| 内置组件 Grid（发射器侧 / 粒子侧） | `editor/GridEmitterComponents.java`、`editor/GridParticleComponents.java` |
| 内置 Grid 注册 / 自定义 Grid 事件 | `editor/ComponentGrids.java`、`api/RegisterComponentGridEvent.java` |
| 曲线页签（可编辑） | `editor/CurvesTab.java` |
| 下拉列表组件 | `editor/DropdownMenu.java` |
| 预览轨道相机 | `editor/PreviewCamera.java` |
| 编辑器发射器类型（从缓存重建预设） | `editor/EditorParticleEmitter.java` |
| 场景容器/渲染循环/排序钩子 | `particle/MolangParticleManager.java` |
| 预设取用点（可被编辑器覆写） | `particle/ParticleEmitter#getEmitterPreset` / `#getParticlePreset`（`createVars`/`createParticle` 调用）+ `editor/EditorParticleEmitter` 的静态中转位（`super(...)` 参数求值时暂存缓存效果，父类构造期间交给编辑器角色） |
| 编辑器路由（子发射器/粒子） | `particle/MolangParticleEngine.java` |
| 渲染类型（ADD/BLEND）与着色器选择 | `PSGameClient.java` |
| 编辑器背景用全黑不透明填充，不做世界渲染 hook | 世界渲染照常走原版流程，最省心也最兼容；一块不透明黑底就能把世界完全盖住，场景只画在右 2/3 视口里 |
| 纹理图集查看器（按材质挑图集、选 sprite 绑定贴图） | `editor/AtlasViewer.java` + `editor/DescriptionTab#applyTexture`；sprite 集合按贴图资源名索引（`particle/ExtendMutableSpriteSet#get(ResourceLocation)`） |
| 打开命令与数据包 | `particle/MolangParticleCommand.java`、`network/OpenEditorPacketS2C.java` |
| 文案 | `assets/particlestorm/lang/en_us.json`、`zh_cn.json` |

## 8. 已知限制 / 后续计划

- [x] 下拉列表滚动：粒子多到一屏放不下时用滚轮在列表内滚动（指针悬停列表时滚轮不再缩放场景）；分组暂不需要；
- [ ] 场景重建时 allocator 的 id 释放（`clearScene` 已回收；关闭界面后旧 id 不回收，按“交给 GC”原则暂不处理）；
- [ ] 冻结/单步、重放、对比基线等预览工具（旧版 `PreviewSession` 有类似功能，尚未移植）；
- [ ] 实体绑定预览（把发射器 attach 到预览实体）；
- [ ] 材质/深度：若某效果确实需要“硬遮挡”的半透明粒子，应使用 `particles_opaque` / `particle_sheet_opaque` 材质，而不是让 BLEND 写深度；
- [ ] 工具框编辑能力：描述页 `identifier` / `type` / `material` 已可改，`texture` 只读；组件 / 事件 / 曲线页均已可编辑；
- [x] 保存 / 导出：File 下拉里的 **Save** 写回引擎内存（`MolangParticleEngine#saveEffect`，立即生效到重载为止），**Export** 按引擎定义格式写盘成 `.particle.json`；两者都用 `DefinedParticleEffect.CODEC` 编码，组件/事件/曲线页的编辑结果因此能一起带出去；
- [ ] 改 `texture` 后还需要重新绑定 sprite（导出/保存只带数据，不带图集绑定）。

## 9. 决策记录

| 决策 | 原因 |
|---|---|
| 不持有 `ParticleEmitter` 字段 | 场景可含多个发射器，manager 才是容器 |
| 编辑器内直接调 manager | 目标明确；静态 `addEmitter/addParticle` 只为子发射器/粒子路由 |
| 不做生命周期释放 | `minecraft.screen` 唯一持有，GC 回收 |
| 正交投影 + 标准视角 + 三种鼠标操作 | 建模软件式操作习惯 |
| `renderParticles` 不改签名，加 `orderForRender` 钩子 | 编辑器专属行为留在 `EditorScreen` 的匿名子类里 |
| BLEND/ADD 不写深度、按距离排序 | 写深度需 `discard`，会破坏平滑淡出 |
| 排序只在编辑器启用 | 世界渲染保持原版行为，零额外开销 |
| 编辑器用缓存效果建预设（`EditorParticleEmitter` 覆写 `getEmitterPreset/getParticlePreset`） | 改完立即生效，不写回引擎数据；世界渲染不受影响 |
| 工具框页签做成组件（`EditorTab` 基类 + `DescriptionTab` / `EffectListTab`） | 每页只管自己的内容区与交互；切页签时 `onHidden` 收起编辑框/下拉，状态不再散落在 `EditorScreen` |
| editor 包内字段直接按包级可见性互访 | 省掉一堆 getter/setter；页签需要的 `effect` / `error` / `manager` / `selectedParticle` 等字段都放在包内可见 |
| 组件编辑按「组件族」组织：每族一个 Grid + 直接 new 替换 | 同族子类型（rate/lifetime/shape）只写一个 Grid，用「类型」下拉切换；必需/可选取自 `Entry.required`，同族只能存在一个实例 |
| 自定义组件 Grid 走 `RegisterComponentGridEvent` | 与 `RegisterCustomComponentEvent` 对称；`register(defaultId, label, variants, required, factory)` 描述组件族的特性 |
| 编辑器背景用全黑不透明填充、不做世界渲染 hook | 世界渲染照常走原版流程（含清屏、光照贴图更新），最省心也最兼容；一块不透明黑底就能把世界完全盖住，场景只画在右 2/3 视口里 |
| 语言键一律 `Component.translatable`，不写内联英文 fallback | 游戏语言解析本来就会回退到 `en_us`，在代码里再抄一份英文只会两边漂移；缺键时按原版行为显示键名，正好提示补 lang（也避免了把 `ResourceLocation` 之类当成翻译参数传进去炸 `TranslatableContents` 的坑） |

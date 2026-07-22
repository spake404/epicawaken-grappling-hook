# Changelog

## 2.0.0 - 2026-07-20

### 中文

以下内容汇总了从 1.5.0 到 2.0.0 的主要更新：

- 在 1.5.0 的副手支持基础上，完善普通钩锁与幻翼钩锁在副手、Curios、Epic Fight 第一人称及第三人称下的使用和渲染兼容。
- 钩锁改为 64 点耐久物品；新增绳子物品、绳子合成配方，以及工作台和铁砧修理机制。
- 新增幻翼钩锁及对应的挂载模型、投射物模型、幻翼飞行动画和独立渲染效果。
- 幻翼钩锁取消使用冷却，支持快速衔接下一次钩锁或连续摆荡。
- 幻翼钩锁现在区分短按和长按：短按执行普通钩锁拉拽，长按 7 tick 后进入摆荡；玩家站在地面时只允许短按。
- 长按判定前摇期间加入空中缓降，避免玩家在等待长按判定时过早落地；短按正式进入拉拽后缓降立即结束。
- 长按摆荡支持以空气位置作为锚点，不再要求命中方块表面；默认发射距离约 12 格，并将摆荡绳长快速收缩到约 7 格。
- 新增完整的摆荡绳长约束、自动切向推进、被动加速、动能保持、最大速度、最大摆荡角度和方向反转能量衰减机制，无需持续按方向键即可完成往返摆荡。
- 修复摆荡过程中无限下坠、到达钩锁正下方停止、只向单侧摆荡、速度过高导致 360 度绕点旋转，以及向斜下方发射时运动方向异常等问题。
- 修复长按目标点偶尔低于玩家视角、地面长按产生异常拉拽、接触地面后松开按键绳子不消失等状态同步和清理问题。
- 新增长按摆荡动画流程：首次正向摆荡使用 hook_hold，后续正向使用 hook_hold_forward，反向使用 hook_hold_back。
- 摆荡动画会根据正向和反向阶段时长分别调整 Epic Fight 原生播放速度，并在预测时间偏短时保持最后一帧，避免动作之间回到自由态或出现模型闪烁。
- 正式运行环境会隐藏并关闭客户端暂停、慢动作、模型位置调参、回车钩锁预览和详细调试日志；相关工具代码仍保留，并仅在 Forge 开发环境中注册和启用。
- 更新 Combat Evolution 与 EpicFight Awaken 依赖版本，为 2.0.0 发布版完成兼容性整理。

### English

The following summarizes the major changes from 1.5.0 through 2.0.0:

- Expanded the 1.5.0 offhand work into complete normal and Phantom Grappling Hook support across offhand, Curios, Epic Fight first-person, and Epic Fight third-person rendering paths.
- Converted grappling hooks into 64-durability items and added the rope item, rope recipe, crafting-table repair, and anvil repair support.
- Added the Phantom Grappling Hook with dedicated equipped rendering, projectile rendering, phantom flight animation, and visual effects.
- Removed the Phantom Grappling Hook cooldown so players can quickly chain another hook or continue consecutive swings.
- Added short-press and long-press behavior to the Phantom Grappling Hook: short presses use the normal pull, while holding for 7 ticks enters swinging; players on the ground can only use the short-press behavior.
- Added reduced falling during the airborne long-press windup so the player does not land before the hold decision completes; the effect ends as soon as a short press begins its normal pull.
- Allowed long-press swings to anchor to an air position without requiring a block hit; the hook launches about 12 blocks and rapidly reels the swing rope toward about 7 blocks.
- Added a full rope-length constraint, automatic tangential propulsion, passive acceleration, momentum preservation, maximum speed, maximum swing angle, and reversal energy loss so swings travel back and forth without continuous movement input.
- Fixed infinite falling, stopping directly below the anchor, failing to swing back, excessive speed causing full rotations around the anchor, and incorrect movement when firing toward a downward anchor.
- Fixed occasional long-press targets appearing below the view direction, abnormal ground long-press pulling, and ropes remaining after releasing the key following ground contact.
- Added the complete swing animation sequence: hook_hold for the first forward swing, hook_hold_forward for later forward swings, and hook_hold_back for backward swings.
- Swing animations now use separate forward and backward phase timing to adjust Epic Fight's native playback speed, and hold their final frame when a timing estimate is short to prevent free-state gaps and model flicker.
- Production environments hide and disable client pause, slow motion, model transform editing, Enter-key hook previews, and verbose debug logging; the tools remain available and are registered only in Forge development environments.
- Updated the Combat Evolution and EpicFight Awaken dependency versions as part of the 2.0.0 compatibility pass.

### 2.0.0 补充更新（2026-07-22）

- 修复部分无限耐久模组修改物品耐久属性后，钩锁会被误判为已损坏并禁止发射的问题；普通钩锁和幻翼钩锁现在都能兼容无限耐久物品。
- 重写墙面上方安全落点处理，改为检查真实支撑碰撞面、完整玩家碰撞箱和可站立空间，并通过墙外抬升点再进入墙顶落点，减少卡墙、拉拽中断和到达后钩锁不消失的问题。
- 为幻翼钩锁短按钩空加入独立冲量系统，提高水平推进和向上冲量，并分别限制水平、垂直及合成速度。
- 完善幻翼钩锁物品栏渲染，使物品栏模型同时显示钩锁主体和幻翼；更新绳子物品图标与模型资源。
- 将钩锁独立创造模式标签移动到第二页，避免挤占原版第一页并将“刷怪蛋”标签推到下一页。
- 清理和校正 Forge 配置：移除不再生效的旧摆荡参数与误导性的生命周期选项，统一自动生命周期计算，修正配置说明，并补齐中文配置名称。
- 将普通钩锁和幻翼钩锁的 Curios 装备位置从 `charm` 迁移到独立的 `glove` 槽位；两种钩锁不再接受护符栏或通用 Curio 槽位，副手使用仍然保留。
- 版本号保持为 `2.0.0`。

### 2.0.0 Additional Updates (2026-07-22)

- Fixed compatibility with infinite-durability mods that caused grappling hooks to be treated as broken and prevented them from firing.
- Reworked safe wall-top targeting to validate real support collision surfaces, the player's full collision box, and standing space, using a two-stage outward lift and wall-top destination to reduce wall clipping, interrupted pulls, and hooks remaining after arrival.
- Added a dedicated empty-air short-press impulse system for the Phantom Grappling Hook with stronger horizontal and upward momentum plus separate horizontal, vertical, and total speed limits.
- Improved Phantom Grappling Hook inventory rendering so both the hook body and phantom are visible, and updated the rope item icon and model resources.
- Moved the grappling hook creative tab to the second page so it no longer displaces vanilla tabs or pushes Spawn Eggs off the first page.
- Audited and cleaned the Forge configuration, removing obsolete swing parameters and the misleading lifetime option, centralizing automatic lifetime calculations, correcting descriptions, and completing Chinese configuration names.
- Moved both grappling hooks from the Curios `charm` slot to a dedicated `glove` slot. Charm and generic Curio slots are no longer accepted, while offhand use remains supported.
- The version remains `2.0.0`.

## 1.7.0 - 2026-07-03

### 中文

- 新增幻翼钩锁物品，作为升级版钩锁的第一版实现。
- 幻翼钩锁暂时复用普通钩锁的释放、命中、拉拽、耐久、冷却和绳子修理逻辑。
- 幻翼钩锁挂载和投射物渲染使用 `grapping_hook_pull` 无头备用模型作为主体，并叠加 Minecraft 幻翼模型作为钩锁头。
- 幻翼钩锁支持副手和 Curios 槽位，并与普通钩锁共享使用冷却。
- 修复 Epic Fight 渲染路径下 Curios 槽位里的幻翼钩锁不会显示手臂模型的问题。
- 修复 Epic Fight 第一人称副手装备钩锁或幻翼钩锁时，手臂挂载模型和手持物品模型重复渲染的问题。
- 幻翼钩锁投射物现在只渲染幻翼模型，保留原有绳子渲染，并播放原版幻翼飞行动画。
- 幻翼钩锁投射物会按钩锁锁定时间线性从 0.50 放大到 1.00，使动作速度调整后仍能在最远有效距离达到目标大小。
- 清理幻翼投射物首次渲染调试日志和临时记录集合，减少发布版日志噪音和渲染路径额外开销。
- 长按摆荡玩法尚未接入，将在后续版本单独实现和调参。

### English

- Added the Phantom Grappling Hook item as the first-pass upgraded grappling hook.
- The Phantom Grappling Hook currently reuses the normal grappling hook release, hit, pull, durability, cooldown, and rope repair behavior.
- Its equipped and projectile renders use the `grapping_hook_pull` headless fallback model as the body and overlay the Minecraft phantom model as the hook head.
- The Phantom Grappling Hook supports both offhand and Curios slots, sharing cooldown with the normal grappling hook.
- Fixed the Phantom Grappling Hook arm model not showing from Curios in the Epic Fight render path.
- Fixed duplicate first-person Epic Fight rendering when a normal or phantom grappling hook is equipped in the offhand: the arm-mounted model remains, while the vanilla in-hand item render is skipped.
- Added render debug targets for mounted and projectile phantom transforms so their position, rotation, and scale can be tuned in-game.
- Added render debug targets for the Phantom Grappling Hook's wing base/tip rotations, allowing the phantom wings to be folded and tuned in-game.
- Phantom Grappling Hook projectiles now render only the phantom model while keeping the existing rope render and playing the vanilla phantom flight animation.
- Phantom Grappling Hook projectiles now scale linearly from 0.50 to 1.00 over the grappling hook lock delay, so the target size is reached at the effective maximum range even when hook animation speed changes.
- Removed temporary first-render projectile diagnostics to reduce release-build log noise and extra render-path bookkeeping.
- Long-press swinging is not implemented yet and will be handled in a later tuning pass.

## 1.6.0 - 2026-07-02

### 中文

- 将钩锁改为 64 点耐久物品，成功释放钩锁时消耗 1 点耐久。
- 钩锁耐久耗尽后无法释放；尝试使用时播放 `hook_pull` 占位动作和物品损坏音效，不生成钩锁投射物，并进入正常冷却。
- 新增绳子物品，暂时使用占位模型。
- 新增三根 `minecraft:string` 竖直摆放合成 3 根绳子的配方。
- 绳子现在可作为钩锁修理材料，支持铁砧修理和工作台直接修理；每根绳子恢复 1 点耐久。

### English

- Changed the grappling hook into a 64-durability item, consuming 1 durability on each successful release.
- Prevented releasing the grappling hook when its durability is depleted; failed attempts play the `hook_pull` placeholder animation and item break sound, spawn no hook projectile, and enter the normal cooldown.
- Added a rope item with a placeholder model.
- Added a recipe that crafts 3 rope from three vertically stacked `minecraft:string`.
- Rope can now repair the grappling hook through both an anvil and direct crafting-table repair; each rope restores 1 durability.
- Crafting-table and anvil repairs now require enough rope to cover all missing durability, then restore the grappling hook to full durability in one operation.

## 1.5.0 - 2026-07-01

### 中文

- 新增副手槽位支持：抓钩放在副手时，也可以通过原有快捷键使用抓钩功能。
- 副手抓钩现在会复用已有的手臂挂载渲染方式，优先使用 Curios，Curios 未装备时再使用副手。
- Epic Fight 第一人称和第三人称渲染现在都支持从副手读取抓钩并显示手臂模型。
- 非 Epic Fight 的普通玩家模型新增副手抓钩手臂渲染层，使副手装备时也能像 Curios 一样挂在手臂上。
- 隐藏第三人称下原版副手抓钩显示，避免副手物品模型和自定义手臂模型重复显示。

### English

- Added offhand slot support, allowing the grappling hook to be used from the offhand with the existing keybind.
- Reused the existing arm-mounted render path for offhand hooks, with Curios taking priority when both sources are present.
- Added Epic Fight first-person and third-person rendering support for offhand-equipped hooks.
- Added a vanilla player render layer so offhand hooks are mounted on the arm outside Epic Fight as well.
- Suppressed the vanilla third-person offhand item render for the grappling hook to avoid duplicate models.

## 1.4.0 - 2026-07-01

### 中文

- 将抓钩物品栏图标从原版拴绳替换为本模组的抓钩 OBJ 模型。
- 为抓钩物品模型新增 GUI、地面、展示框、第一人称和第三人称 display transform。
- 绑定抓钩手臂贴图作为物品模型材质和粒子材质，使物品栏显示与模组自有模型保持一致。
- 保留临时资源包调参流程，方便在游戏内通过 `F3+T` 重新加载并微调模型显示参数。

### English

- Replaced the vanilla lead inventory icon with the mod's own grappling hook OBJ model.
- Added GUI, ground, fixed, first-person, and third-person display transforms for the grappling hook item model.
- Bound the grappling hook arm texture as the item model and particle texture so the inventory render uses the mod-owned visual asset.
- Kept the temporary resource-pack tuning workflow available for quick in-game `F3+T` display transform iteration.

## 1.3.0 - 2026-06-29

### 中文

- 修复钩锁手臂模型在游戏内重载资源包后位置/朝向错乱的问题。
- 移除了手臂模型 JSON 中 `epicawaken_grappling_hook:worn` display transform 的额外旋转，避免资源重载后模型被重复应用 180 度旋转。
- 将钩锁手臂模型的佩戴位置统一交由 Java 渲染 transform 控制，减少资源 bake 状态差异导致的不一致。
- 增加受 debug 配置控制的渲染路径、模型 transform 和 Epic Fight 骨骼矩阵日志，便于后续定位类似渲染问题。

### English

- Fixed grappling hook arm model position/orientation becoming incorrect after an in-game resource pack reload.
- Removed the extra `epicawaken_grappling_hook:worn` display transform rotation from the arm model JSON files, preventing the model from receiving an additional 180-degree rotation after resource reloads.
- Consolidated the worn arm model placement under the Java render transform path to avoid inconsistent results from model bake state changes.
- Added debug-gated logs for render path selection, baked model transforms, and Epic Fight joint matrices to help diagnose future rendering issues.

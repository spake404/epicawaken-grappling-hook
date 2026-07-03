# Changelog

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

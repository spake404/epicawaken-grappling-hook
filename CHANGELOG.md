# Changelog

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

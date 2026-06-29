# Changelog

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

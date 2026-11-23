感谢您使用 HyperPGuard。

这一版没有新增功能，界面与开关行为与 2.0.2 完全一致，只有内部代码结构整理：主页底栏的构建、胶囊弹簧、切页与玻璃刷新从 `MainActivity` 挪进独立的 `BottomBar` 类（`MainActivity` 3000 行 → 2560 行）。

## 代码结构

- 底栏集中在 `BottomBar.java`：`Stage`（横向分页）+ 4 个 View（外壳、模糊、lens、胶囊）+ `BottomBarController`；`MainActivity` 只负责组装页面内容。
- 深浅色切换后保留当前页、拖拽松手吸附、玻璃定期刷新这些处理逻辑没有改动。
- 8 个拦截层（hook 侧）零改动。

## 建议

已经装了 2.0.2 的话，升不升都一样；差异只在源码可读性。

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v2.0.2...v2.0.3

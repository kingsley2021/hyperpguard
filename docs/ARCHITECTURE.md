# 项目结构与扩展指引

给后续维护和协作用。写完新功能后请顺手更新 `docs/RULES.md` 与 `docs/RELEASE-<版本>.md`。

## 一、拦截分层

模块在四个位置拦截，彼此独立（方法层按组件拆成五个文件）：

| 层 | 文件 | 说明 |
|---|---|---|
| 网络出口 | `NetGuard.java` | `URL#openConnection`、`InetAddress`、`Socket` 构造/`connect`、okhttp `Dns`；规则为「域名后缀 + 可选路径前缀」 |
| 隐私 API | `PrivacyApis.java` | 应用列表、使用记录、设备标识、短信、XMSF 代发广播 |
| 本地存储 | `CaptureHooks.java` | 行为库八张表的 `SQLiteDatabase` 写入、静默录屏 |
| 方法层 | `XsofHooks.java`、`MsaHooks.java`、`AnalyticsHooks.java`、`TrackHooks.java`、`ImHooks.java` | 按结构或签名定位方法后直接返回常量；`TrackHooks` 统一收口组件内嵌的 OneTrack 埋点，`ImHooks` 管四个预装输入法的统计与剪贴板记录 |

`Hooks.java` 是公共工具：`ret`、`retAllByName`、`classNames`、`hookBySignature`、`customMethods`。
`hookBySignature` 逐类兜住 `getDeclaredMethods()` 的异常——安全中心里有类的方法签名引用 `javax.lang.model.*`，不兜会整轮中断。

## 二、文件职责

```
HookEntry.java     入口；onPackageReady 后按包名分发到各层
Config.java        开关常量、配置通道（Settings.Global 的 hyperpguard_conf）
I18n.java          界面英文查表（中文是唯一来源，查不到原样返回）
NetGuard.java      网络层与规则匹配
PrivacyApis.java   隐私 API 层
CaptureHooks.java  行为库写入与静默录屏
XsofHooks.java     XSOF 四层 + MRM
MsaHooks.java      MSA 使用画像与 OneTrack
AnalyticsHooks.java 统计方法层
TrackHooks.java    组件内嵌 OneTrack 埋点的统一收口（track / adTrack 系列 void 方法）
ImHooks.java       预装输入法的统计与剪贴板记录（搜狗 / 百度 / 讯飞 / 小米）
Hooks.java         公共 hook 工具
PgSwitch.java      设置页开关（自绘，点击可由外部接管）
Logs.java          统一日志标签 HyperPGuard
MainActivity.java  设置界面（程序化构建，无 layout 资源）：生命周期、三页组装、作用域与弹窗

界面层（依赖单向，均不反向依赖 MainActivity）：
Theme.java         主题色板（bg/card/line/text/sub/accent/night），改配色只动这里
UiKit.java         无状态组件工厂：card/divider/line/sectionRow/sectionTitle/
                   infoRow/linkRow/singleButton/addListRow/pressBackground
BottomBar.java     底栏全部：Stage 页面容器 + GlassPill 玻璃球 + GlossView 高光 +
                   TabIcon + BottomBarController（构建、胶囊弹簧、切页、玻璃刷新）
ConfigIO.java      配置导入导出（读 UiKit 取色，不持 Activity 字段）
LogTools.java      日志纯函数：拦截统计、分段、域名用途注解（可单测）
Misight.java       MiSight 事件库只读快照（su 复制 + 只读查询）
MisightActivity.java 事件库浏览页
HotReload.java     开关热重载（libxposed API 102）
```

界面代码的分层约定（2026-11-22）：

```
MainActivity（组装 + 状态）
  → 页面构建方法（buildGeneralPage / buildComponentsPage / buildSettingsPage）
  → UiKit（只依赖 Theme + Context）
  → Theme（纯数据）
```

- 底栏是唯一持有 `MainActivity` 引用的部件（`BottomBarController.host`）：胶囊位置、当前页、
  切页时的 scrollRoot 都要和页面状态联动，换成接口回调只是把同样的引用换个名字。
  改底栏、回滚底栏都只动 `BottomBar.java`；`MainActivity` 只保留 `bar.build(rootFrame)`、
  `bar.updateTabUi(...)`、`bar.onPageScrolled(...)`、`bar.scheduleGlassRefresh()` 四处调用。
- 页面构建方法暂时留在 `MainActivity`：它们要读写 `groupSwitches`、`scopeHints`、`missingGroups` 等
  页面状态，搬到独立类会让每个字段变成 `host.xxx`，可读性反而下降。**除非状态先抽成对象，否则不拆页面。**
- 新增界面元素：先看 `UiKit` 有没有现成的（card/row/section/button），没有再在 `UiKit` 加方法，
  不要把构建代码写回 `MainActivity`。
- 改底栏绘制只动 `BottomBar.java`；改配色只动 `Theme`；改日志解析只动 `LogTools`。

## 三、新增一个组件分组

以新增「组件 X」为例，六处改动：

1. **`META-INF/xposed/scope.list`**：加包名。staticScope 模块，覆盖安装后需要在 LSPosed 里重新确认作用域，旧进程不会自动补注入
2. **`Config.java`**：加分组开关 `K_G_X` 与子项常量；默认值登记到 `Config.DEFAULTS`，调用点只写 `Config.on(K)` / `Config.def(K)`，不要写字面量（`scripts/check_structure.py` 会报）
3. **`HookEntry.dispatch()`**：加 `if ("包名".equals(pkg)) { ... }` 分支，先按分组开关 return，再调各层
4. **`MainActivity.GROUPS`**：加 `Group` 与 `Item`，文案过 `T()`；同时在 `I18n.java` 补英文
5. **`docs/RULES.md`**：写清判据（域名 / 方法签名 / 表名）与默认开关的理由
6. **验证**：`sh build.sh` → 装到设备 → 在 LSPosed 确认作用域 → 看 `attach <包>` 与对应 `hook ok` / `X(host)` 日志

方法层如果代码量超过 80 行，单独建 `XxxHooks.java`，不要继续堆进 `HookEntry`。

## 四、规则判据的写法

- 网络规则优先用完整子域，不要用 `xxx.com` 这种宽泛后缀
- 路径规则是 `startsWith`，只对 `URL#openConnection` 生效
- 类名会随版本漂移：能按「修饰符 + 参数类型 + 返回类型 + 声明类所在包」判定时，不要写死混淆名
- 每次 hook 失败都要留日志（`hook miss ...`），不要静默失败

## 五、界面约定

- 色板集中在 `MainActivity.applyPalette()`：浅色 `#F2F3F5/#FFFFFF/#1A1A1A/#8A8A8E/#EFEFF0`，深色 `#000000/#1C1C1E/#FFFFFF/#8E8E93/#2C2C2E`
- 可点条目统一用 `pressBackground()`（浅色 `#E5E5E5`、深色 `#3A3A3C`），文字保持强调色
- 弹窗统一走 `showStyled()` / `trackDialog()`，保证深色与切换主题时就地换色
- 开关用 `PgSwitch`；需要「等外部确认再切换」时用 `setOnToggleRequest()`，自身不翻转
- 行规格：标题 17sp、副标题 14sp、行高 56dp、卡片圆角 20dp、分组标题 14sp/35dp

## 六、开关热生效

改开关后不必重启目标进程：界面把新配置写进 `Settings.Global` 的 `hyperpguard_conf`，随后调用
`IXposedService#hotReloadModule` 让已注入的进程重装 hook。

```
MainActivity.scheduleHotReload(pkg)   // 连点合并成一次，600ms 后触发
  → HotReload.start()                 // 按包名挑运行中目标，逐个 hotReloadModule
  → HookEntry.onHotReloaded()         // 卸载旧 hook → Config.reload() → 按新开关重装
```

- 只重载受影响的进程：改某个组件的子项只重载该组件，总开关 / 路径特征 / 未命中记录 / 自定义规则才重载全部
- 作用域变化不走热重载：新加入的作用域要等进程重新启动才会注入，界面仍保留「重启」「全部重启」
- `HotReloadResult` 是 record（Java 16+），本模块按 `--release 11` 编译，结果按访问器名反射读取
- 框架不支持（API < 102）或进程正在退出时，界面提示一次后由用户改用「重启」

## 七、构建与发布

```sh
sh build.sh          # javac --release 11 → d8 → aapt2 link → zipalign → apksigner
git tag -a vX.Y.Z -m "HyperPGuard X.Y.Z"
git push origin main vX.Y.Z
```

CI 在 tag 上做三件事：构建 APK、源码仓建 Release、同步到分发仓（tag 为 `<versionCode>-<versionName>`）。发版前先在 `docs/RELEASE-<版本>.md` 写好正文，否则 CI 会失败。

不引入第三方依赖：注入的 dex 现在只含 libxposed service 一项必需组件，加库会直接放大 13 个进程的注入开销。

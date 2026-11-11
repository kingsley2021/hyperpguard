感谢您使用 HyperPGuard。

这一版修的是「界面上的开关状态和实际拦截行为不一致」：默认值原先在界面与 hook 两处各写一份，1.6.12 改搜狗剪贴板默认值、1.6.11 加百度语音时都只改了一处，结果界面显示关闭、进程里却仍然装着 hook。

## 修复

- 默认值收敛到 `Config.DEFAULTS` 一处（90 项），界面与 hook 侧统一从 `Config.def(key)` 取，调用点不再写字面默认值
- 搜狗「剪贴板采集」与百度「语音上传」在 1.6.11–1.6.13 上界面显示关闭、hook 仍然生效；本版起两侧一致
- 1.6.12 的三个旧键（`block_sogou_kpi2`、`block_sogou_instant`、`block_mitype_hosts`）补上迁移：旧键任一为关则新键取关，新键已写过值时不动；迁移标记单独存放，不随 `pg` 同步
- `hookBySignature` 补记跳过的类名（前 5 个），`allByName` 兜住 `getDeclaredMethods()` 的异常，AutoValue 类里的运行时缺失类型不再冒到调用方

## 文档

- `docs/RULES.md`、`README.md`：小米输入法的击键统计据第三方静态分析经 `OneTrack#track` 发出，由 OneTrack 方法层覆盖；原来写的「无稳定域名，未覆盖」会让人去找一个不存在的独立上报通道

## 说明

- 除上述默认值与迁移外，开关划分、拦截规则与作用域逻辑未改动
- 结构自查新增三项（默认值表与引用双向核对、调用点不许写字面默认值、I18n 同一个中文键只能登记一次），CI 改用 `--strict`

全部提交：https://github.com/vivibibi30/hyperpguard/compare/v1.7.0...v1.7.1

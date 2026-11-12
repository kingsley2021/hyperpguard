<div align="center">

# HyperPGuard

![license](https://img.shields.io/badge/license-GPL--3.0-blue)
![language](https://img.shields.io/badge/language-Java-orange)
![release](https://img.shields.io/github/v/release/vivibibi30/hyperpguard?label=release&color=green)

在采集入口、本地存储、对外接口、网络出口四层拦截 HyperOS 预装组件的隐私上报

</div>

> 源码与构建说明：[vivibibi30/hyperpguard](https://github.com/vivibibi30/hyperpguard)
>
> 本项目源码

## 当前支持的版本

Android 10 – Android 16 的 HyperOS，LSPosed 2.2.x（libxposed API 100+）。

## 使用前说明

1. 在 LSPosed 中启用「HyperPGuard」
2. 打开应用，首次会向 LSPosed 请求全部作用域，允许即可；也可在 LSPosed 里手动勾选
3. 在模块里对目标应用点一次「重启」，或点批量行的「全部重启」，让应用重新加载
4. 「重启」「查看记录」需要 root，首次使用请允许 KernelSU / Magisk 授权

作用域外的应用不会被注入。

## 功能

| 层 | 内容 |
|---|---|
| 采集入口 | 在数据来源处截断：应用列表、使用记录、设备标识、位置与网络环境查询返回空 |
| 本地存储 | 拦截行为记录类数据库的写入 |
| 对外接口 | 拦截跨进程查询与评分接口 |
| 网络出口 | 拦截目标进程内的域名解析与连接，按「域名后缀 + 可选路径」匹配 |
| 兜底 | 反射定位方法后返回默认值；类名变化时网络层仍生效 |

按应用分组，每个应用的开关等同于它在 LSPosed 里的作用域开关。会影响系统功能的规则一律默认关闭，副标题写明代价。

## 常见问题

- **某个应用开关打不开**：该包可能不存在、被冻结或与系统共享 UID，LSPosed 无法把它加入作用域
- **日志为空**：`logcat` 环形缓冲已覆盖，或目标应用还没重启过
- **功能异常**：对应的默认关闭子项被打开了，按需关掉即可
- **方法层出现 `hook miss`**：组件版本变化，网络层与隐私 API 层不受影响

## 致谢

[HyperCeiler](https://github.com/ReChronoRain/HyperCeiler) · [XSOF Privacy](https://github.com/yunyizea/xsof-privacy) · [HyperOS4-XSOF-Analysis](https://github.com/FAQQAQ/HyperOS4-XSOF-Analysis) · [酷安 @OSAKOo](https://www.coolapk.com/feed/74882729?s=MzNlYzk4NWUxNWZhMjRnNmFlZTFjMDJ6a1664) · [Android OS Privacy Under the Loupe](https://ar5iv.labs.arxiv.org/html/2302.01890)（ACM WiSec '23）· [酷安 RooyCo](https://www.coolapk.com/feed/74911090) · [frauddb](https://github.com/acnekot/frauddb) · [misight-viewer](https://github.com/Artriai/misight-viewer) · [libxposed API](https://github.com/libxposed/api) · LSPosed

## 免责声明

仅供在本人拥有完全控制权的设备上学习研究使用，不做商业分发。完整条款见 [DISCLAIMER.txt](https://github.com/vivibibi30/hyperpguard/blob/main/DISCLAIMER.txt)。

[GPL-3.0](https://github.com/vivibibi30/hyperpguard/blob/main/LICENSE)；第三方组件的许可见 [NOTICE](https://github.com/vivibibi30/hyperpguard/blob/main/NOTICE)。

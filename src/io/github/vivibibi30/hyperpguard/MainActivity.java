package io.github.vivibibi30.hyperpguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import java.util.Collections;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 卡片式开关面板。
 * 每组一个「启用」总开关（等同作用域开关），关闭时该组子项全部置灰不可操作。
 * 负责：生命周期、三页组装、弹窗、系统命令的调用入口。不负责：底栏与分页（BottomBar）、色板与组件工厂（Theme / UiKit）、配置读写（Config / ConfigIO）、日志解析（LogTools）。
 */
public class MainActivity extends Activity {

    private int BG = 0xFFF2F3F5;
    private int CARD = 0xFFFFFFFF;
    private int TEXT = 0xFF1A1A1A;
    int SUB = 0xFF8A8A8E;
    private int LINE = 0xFFEFEFF0;
    private UiKit kit;
    static final int ACCENT = 0xFF3478F6;
    boolean nightMode;

    private static final String URL_REPO = "https://github.com/vivibibi30/hyperpguard";
    private static final String URL_CUSTOM_RULES_DOC =
            URL_REPO + "/blob/main/docs/CUSTOM-RULES.md";
    private static final String URL_ISSUES = URL_REPO + "/issues";
    private static final String URL_RELEASES = URL_REPO + "/releases";
    private static final String URL_MODULE =
            "https://modules.lsposed.org/module/io.github.vivibibi30.hyperpguard";
    private static final String API_LATEST =
            "https://api.github.com/repos/vivibibi30/hyperpguard/releases/latest";

    /**
     * 子项。默认值不再由调用点传：统一取 Config.def(key)，界面和 hook 侧同一份。
     */
    private static final class Item {
        final String name, desc, key;
        final boolean def;

        Item(String name, String desc, String key) {
            this.name = name;
            this.desc = desc;
            this.key = key;
            this.def = Config.def(key);
        }
    }

    private static final class Group {
        final String title, pkg, key, note;
        final Item[] items;

        Group(String title, String pkg, String key, Item[] items) {
            this(title, pkg, key, items, null);
        }

        Group(String title, String pkg, String key, Item[] items, String note) {
            this.title = title;
            this.pkg = pkg;
            this.key = key;
            this.items = items;
            this.note = note;
        }
    }

    /** 界面文案：中文原文查表，英文环境返回英文，查不到原样返回 */
    static String T(String zh) {
        return I18n.t(zh);
    }

    /** 组总开关默认开启：状态由 LSPosed 真实作用域回填，不看本地默认值 */
    private static final Group[] GROUPS = {
            new Group(T("SystemHelper"), "com.mobiletools.systemhelper", Config.K_G_SYSHELPER, new Item[]{
                    new Item(T("上报"), T("dm.wo.com.cn，设备 / SIM / 基站 / 应用列表"), Config.K_SYSHELPER),
                    new Item(T("整进程断网"), T("默认关：只拦上报域名与方法层。开启后该进程所有外网请求一并拦截，只留本机回环"), Config.K_SYSHELPER_ALLNET),
                    new Item(T("定位（GPS）"), T("只对本应用：位置查询返回空"), Config.K_LOC_SH),
                    new Item(T("基站信息"), T("只对本应用：基站查询返回空，不影响信号显示"), Config.K_CELL_SH),
                    new Item(T("Wi-Fi 扫描"), T("只对本应用：扫描列表置空，SSID / BSSID 清空"), Config.K_WIFI_SH),
            }, T("权限在开机激活时确认；本模块只拦上报，不回收已授予的权限")),
            new Group(T("系统安全组件"), "com.miui.guardprovider", Config.K_G_GUARD, new Item[]{
                    new Item(T("应用列表云查"), T("仅拦 flash.sec.miui.com，不动病毒库"), Config.K_GUARD_APPLIST),
                    new Item(T("AI 通话预警云检测"), T("detection-aigc.engine.miui.com"), Config.K_GUARD_AIGC),
                    new Item(T("反诈配置拉取"), T("fraud-detection.engine.miui.com"), Config.K_GUARD_FRAUD),
                    new Item(T("云查上传（Flamingo）"), T("virus-engine.sec.miui.com；关闭后不再上传应用清单，本地病毒库照常"), Config.K_GP_CLOUDSCAN),
                    new Item(T("静默录屏"), T("阻止 createVirtualDisplay 整屏录制，不影响正常截屏"), Config.K_GP_SCREENGUARD),
                    new Item(T("引擎日志上传"), T("logupdate.avlyun.sec.miui.com；只影响日志回传，不影响本地查毒与病毒库更新"), Config.K_GP_ENGINELOG),
                    new Item(T("定位（GPS）"), T("只对本应用：位置查询返回空"), Config.K_LOC_GP),
                    new Item(T("Wi-Fi 扫描"), T("只对本应用：扫描列表置空，SSID / BSSID 清空"), Config.K_WIFI_GP),
                    new Item(T("通讯录 / 通话读取"), T("只对本应用：查询返回空；来电识别会受影响"), Config.K_SOCIAL_GP),
            }, T("AI 反诈逻辑在本组件的 AiGuardService 内，用于通话场景；SystemHelper 只做运营商侧上报")),
            new Group(T("安全中心"), "com.miui.securitycenter", Config.K_G_SECCENTER, new Item[]{
                    new Item(T("XSOF 行为查询"), T("第三方查不到截图 / 通话 / 屏幕共享记录"), Config.K_XSOF),
                    new Item(T("反诈配置拉取"), T("fraud-detection.engine.miui.com"), Config.K_SECCENTER),
                    new Item(T("病毒库云查"), T("virus-engine.sec.miui.com，关闭后不再云端查毒"), Config.K_SC_VIRUS),
                    new Item(T("XSOF 合作方鉴权"), T("framework.sec.miui.com，可能影响第三方调用"), Config.K_SC_FRAMEWORK),
                    new Item(T("月报与配置上报"), T("data.sec.miui.com，含防窥/广告等接口"), Config.K_SC_DATA),
                    new Item(T("防窥分类上报"), T("上传非系统应用包名数组；关闭后防窥识别可能变差"), Config.K_SC_APPCLASS),
                    new Item(T("志愿者传感器上报"), T("OAID + 经纬度 + 时间；非志愿者无影响"), Config.K_SC_VOLUNTEER),
                    new Item(T("地震/报警/EPS 定位"), T("关闭后地震预警、紧急报警定位可能失效"), Config.K_SC_LOCATION),
                    new Item(T("ADB 授权验证"), T("srv.sec.miui.com/data/adb；关闭后 ADB 安装授权可能失败"), Config.K_SC_ADB),
                    new Item(T("行为库写入"), T("拦截 screenshot / screen_share / voip_record 等 8 张表的写入"), Config.K_SC_DBGUARD),
                    new Item(T("设备标识"), T("IMSI / ICCID / 手机号 / IMEI；打开后剩余流量、套餐查询会失效"), Config.K_SC_IDS),
                    new Item(T("广告位与推送下发"), T("adv 广告位、网络助手推送、满意度问卷；关闭后这几处不再拉取配置"), Config.K_SC_MISC),
                    new Item(T("统计埋点上报"), T("tracking.*.miui.com / sdkconfig.*.xiaomi.com；只影响统计，不影响功能"), Config.K_SC_TRACKING),
                    new Item(T("定位（GPS）"), T("只对本应用：位置查询返回空"), Config.K_LOC_SC),
                    new Item(T("通讯录 / 通话 / 短信读取"), T("只对本应用：查询返回空；来电识别会受影响"), Config.K_SOCIAL_SC),
            }),
            new Group(T("小米统计"), "com.miui.analytics", Config.K_G_ANALYTICS, new Item[]{
                    new Item(T("上报"), T("tracking / om.tracking / sdkconfig"), Config.K_ANALYTICS),
                    new Item(T("阻断 DoH 旁路"), T("dns.google 与 8.8.8.8，防解析绕过域名规则"), Config.K_ANALYTICS_DOH),
                    new Item(T("全断网（推荐）"), T("该进程所有外网请求一并拦截；云端换域名也不用管"), Config.K_ANALYTICS_ALLNET),
            }),
            new Group(T("应用安装器"), "com.miui.packageinstaller", Config.K_G_PKG, new Item[]{
                    new Item(T("AVL 上报"), T("网络层 + 方法层；关闭后安装信息不再上传，病毒扫描结果可能缺失"), Config.K_PKG_AVL),
                    new Item(T("安装活跃统计"), T("网络层 + 方法层；关闭后不再上报安装活跃数据，无功能影响"), Config.K_PKG_STAT),
                    new Item(T("安装页广告位"), T("网络层 + 方法层；关闭后安装页不再拉取推荐位配置"), Config.K_AD_PKG),
                    new Item(T("设备标识（OAID）"), T("MSA SDK 返回空串；只影响广告归因，不影响安装"), Config.K_PKG_OAID),
                    new Item(T("云端配置下发"), T("阻断安装器云端参数，含「未查询到备案」提示；关闭后按内置默认值展示"), Config.K_PKG_CLOUD),
                    new Item(T("风险校验查询"), T("verify / tz.sec.xiaomi.com；打开后安装风险检测失效，包名与哈希不再外发"), Config.K_PKG_RISK),
                    new Item(T("游戏防篡改检查"), T("game/interceptcheck；打开后游戏安装时的完整性校验失效"), Config.K_PKG_GAME),
            }),
            new Group(T("应用商店"), "com.xiaomi.market", Config.K_G_AD_MARKET, new Item[]{
                    new Item(T("广告配置下发"), T("sdkconfig.ad.xiaomi.com"), Config.K_AD_MARKET),
                    new Item(T("使用统计上报"), T("tracking.miui.com/track；只影响统计，不影响下载与安装"), Config.K_MARKET_TRACK),
            }),
            new Group(T("小米服务框架"), "com.xiaomi.xmsf", Config.K_G_AD_XMSF, new Item[]{
                    new Item(T("广告接口"), T("adv.sec.miui.com"), Config.K_AD_XMSF),
                    new Item(T("使用统计上报"), T("tracking.miui.com/track；只影响统计，不影响推送与登录"), Config.K_XMSF_TRACK),
            }),
            new Group(T("广告组件（MSA）"), "com.miui.systemAdSolution", Config.K_G_MSA, new Item[]{
                    new Item(T("广告使用画像"), T("读取应用今日是否打开、最后使用时间；按少推广告的一侧返回"), Config.K_MSA_USAGE),
                    new Item(T("广告埋点上报"), T("OneTrack 的 track / adTrack 方法直接返回空"), Config.K_MSA_TRACK),
                    new Item(T("开屏广告"), T("requestSplashScreen 系列返回空 Intent；应用自己的启动页不受影响"), Config.K_MSA_SPLASH),
            }),
            new Group(T("网络管理服务"), "com.miui.wmsvc", Config.K_G_WM, new Item[]{
                    new Item(T("诊断统计上报"), T("OneTrack 埋点 + tracking.miui.com；只影响统计，不影响网络设置与信号"), Config.K_WM_TRACK),
            }),
            new Group(T("AI 内容识别"), "com.xiaomi.aicr", Config.K_G_AICR, new Item[]{
                    new Item(T("统计埋点"), T("OneTrack 埋点 + tracking.miui.com；不影响本地识别与模型推理"), Config.K_AICR_TRACK),
            }),
            new Group(T("搜狗输入法"), "com.sohu.inputmethod.sogou.xiaomi", Config.K_G_IME_SOGOU, new Item[]{
                    new Item(T("统计上报"), T("kpi2 与 instantpb 两个埋点域名；后果：只停统计，输入、候选与词库不受影响"), Config.K_SOGOU_PINGBACK),
                    new Item(T("剪贴板采集"), T("ClipboardLogHelper 记录剪贴板内容与长度；后果：关闭后剪贴板历史可能不可用，复制粘贴本身不受影响"), Config.K_SOGOU_CLIPBOARD),
            }),
            new Group(T("小米输入法"), "com.xiaomi.type", Config.K_G_IME_MITYPE, new Item[]{
                    new Item(T("统计埋点"), T("OneTrack 方法层 + tracking/sdkconfig 域名；后果：只停埋点，输入与联想不受影响。自研击键统计无稳定域名，未覆盖"), Config.K_MITYPE_TRACK),
            }),
            new Group(T("百度输入法"), "com.baidu.input_mi", Config.K_G_IME_BAIDU, new Item[]{
                    new Item(T("统计上报"), T("com.baidu.stats 记录与上传入口；只影响统计"), Config.K_BAIDU_STATS),
                    new Item(T("语音采集上传"), T("com.baidu.speech.utils.analysis；默认关：开启后不再上传 ASR 数据，语音输入仍可用"), Config.K_BAIDU_SPEECH),
            }),
            new Group(T("讯飞输入法"), "com.iflytek.inputmethod.miui", Config.K_G_IME_IFLYTEK, new Item[]{
                    new Item(T("统计上报"), T("com.iflytek.statssdk 记录与上传入口；只影响统计"), Config.K_IFLYTEK_STATS),
            }),
            new Group(T("微信输入法"), "com.tencent.wetype", Config.K_G_IME_WETYPE, new Item[]{
                    new Item(T("统计埋点"), T("灯塔 h.trace.qq.com 与配置/灰度下发；后果：只停埋点与下发，输入与候选不受影响"), Config.K_WETYPE_TRACK),
                    new Item(T("崩溃与性能上报"), T("Bugly 崩溃上报与 rmonitor 性能监控；后果：只停这两类数据，应用照常运行"), Config.K_WETYPE_CRASH),
                    new Item(T("云输入与词库"), T("微信云与 CDN；默认关：开启后云联想、词库同步、皮肤下载会不可用"), Config.K_WETYPE_CLOUD),
            }),
            new Group(T("豆包输入法"), "com.bytedance.android.doubaoime", Config.K_G_IME_DOUBAO, new Item[]{
                    new Item(T("统计埋点"), T("AppLog 埋点域名；后果：只停埋点，输入与候选不受影响"), Config.K_DOUBAO_TRACK),
                    new Item(T("配置与校验"), T("配置、灰度与校验域名；默认关：开启后配置下发可能失败"), Config.K_DOUBAO_CONFIG),
                    new Item(T("云输入服务"), T("输入法服务端；默认关：开启后云联想不可用"), Config.K_DOUBAO_CLOUD),
            }),
            new Group(T("运营商服务"), "com.xiaomi.registration", Config.K_G_REGISTRATION, new Item[]{
                    new Item(T("设备注册（zzhc）"), T("IMEI/IMSI/ICCID；关闭后 VoLTE / 运营商功能可能异常"), Config.K_REG_ZZHC),
                    new Item(T("明文 CTEI 上报"), T("http://typdm.tydevice.com，IMEI / MAC / BSSID 明文 GET"), Config.K_REG_TYPDM),
                    new Item(T("定位（GPS）"), T("只对本应用：位置查询返回空"), Config.K_LOC_REG),
                    new Item(T("基站信息"), T("只对本应用：基站查询返回空，不影响信号显示"), Config.K_CELL_REG),
            }),
            new Group(T("移动 DM（RegService）"), "com.miui.dmregservice", Config.K_G_DMREG, new Item[]{
                    new Item(T("上报"), T("fxltsbl.com；App 清单 / SIM 标识"), Config.K_DMREG),
                    new Item(T("基站信息"), T("只对本应用：基站查询返回空，不影响信号显示"), Config.K_CELL_DM),
                    new Item(T("Wi-Fi 扫描"), T("只对本应用：扫描列表置空，SSID / BSSID 清空"), Config.K_WIFI_DM),
            }),
            new Group(T("系统诊断（MiSight）"), "com.miui.misightservice", Config.K_G_MISIGHT, new Item[]{
                    new Item(T("日志上传"), T("feedback.miui.com/misight/upload；关闭后主动反馈传不了日志包"), Config.K_MISIGHT_UPLOAD),
                    new Item(T("售后检测"), T("port.sec.miui.com/mqsas；关闭后售后远程检测拿不到日志"), Config.K_MISIGHT_MQSAS),
                    new Item(T("客服诊断"), T("chat.kefu.mi.com/openapi/misight/diagnostic；关闭后在线客服远程诊断不可用"), Config.K_MISIGHT_KEFU),
                    new Item(T("事件日志上传"), T("event-log.cnbj1.mi-fds.com；诊断事件不再回传，本地记录不受影响"), Config.K_MISIGHT_EVENTLOG),
            }, T("触发条件是用户主动反馈或售后远程诊断；本地记录不等于已经上传")),
            new Group(T("小米可信服务"), "com.xiaomi.trustservice", Config.K_G_TRUST, new Item[]{
                    new Item(T("屏蔽 MRM 认证链"), T("会影响设备证书 / 查找设备"), Config.K_TRUST),
                    new Item(T("设备证书/密钥上报"), T("find.api.micloud.xiaomi.net；关闭后查找设备、设备证书可能异常"), Config.K_TRUST_FIND),
            }),
            new Group(T("翻译组件"), "com.xiaomi.aiasst.vision", Config.K_G_AIASST, new Item[]{
                    new Item(T("语音识别上传"), T("只断 /speech/ 路径，保留文字翻译"), Config.K_AIASST),
            }),
    };

    /** 组 key → 该组所有子项 Switch / 行，用于刷新真实状态（不置灰） */
    private final Map<String, List<Switch>> childSwitches = new LinkedHashMap<>();
    private final Map<String, List<LinearLayout>> childRows = new LinkedHashMap<>();
    /** 组 key → 组开关 */
    ScrollView scrollRoot;
    private AlertDialog shownDialog;
    private int appliedNight = -1;
    /** DISCLAIMER.txt 的版本号，变了就重新弹一次确认 */
    private static final int DISCLAIMER_VER = 18;
    private int restoreScrollY;
    final Handler ui = new Handler(Looper.getMainLooper());
    private XposedService xposed;
    private boolean updatingSwitch = false;
    private int bindRetry = 0;
    private boolean autoRequested = false;

    /* ---------------- 开关热生效 ---------------- */

    /** 待热重载的目标包；hotReloadAll 为 true 时对作用域内全部进程 */
    private final java.util.Set<String> hotReloadPkgs = new java.util.LinkedHashSet<>();
    private boolean hotReloadAll = false;
    private boolean hotReloadRunning = false;
    private boolean hotReloadQueued = false;
    /** 框架不支持热重载时只提示一次，别每点一个开关弹一遍 */
    /** 三页共享的界面状态（开关视图、作用域提示、缺组件、滚动容器） */
    final PageState page = new PageState();

    private boolean hotReloadHinted = false;
    private final Runnable hotReloadTask = this::flushHotReload;

    @Override
    // ===== 生命周期与主题 =====
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        applyPalette(getResources().getConfiguration());
        applyWindowChrome();
        bindServiceHelper();
        restoreScrollY = b == null ? 0 : b.getInt("scrollY", 0);
        Config.migrateLegacyKeys(this);
        buildUi();
        try {
            registerReceiver(themeReceiver,
                    new android.content.IntentFilter(android.content.Intent.ACTION_CONFIGURATION_CHANGED));
        } catch (Throwable t) {
            android.util.Log.w("HyperPGuard", "theme receiver fail", t);
        }
        if (restoreScrollY > 0 && scrollRoot != null) {
            scrollRoot.post(() -> scrollRoot.scrollTo(0, restoreScrollY));
        }
        maybeShowDisclaimer();
    }

    /** 深浅色变化时不重建 Activity，避免对话框被关掉；只重建内容视图 */
    @Override
    public void onConfigurationChanged(android.content.res.Configuration cfg) {
        super.onConfigurationChanged(cfg);
        int y = scrollRoot == null ? 0 : scrollRoot.getScrollY();
        applyPalette(cfg);
        applyWindowChrome();
        buildUi();
        if (y > 0 && scrollRoot != null) {
            scrollRoot.post(() -> scrollRoot.scrollTo(0, y));
        }
        if (shownDialog != null && shownDialog.isShowing()) {
            styleDialog(shownDialog);
        }
    }

    private void applyPalette(android.content.res.Configuration cfg) {
        android.content.res.Configuration c =
                cfg != null ? cfg : getResources().getConfiguration();
        applyPalette((c.uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES);
    }

    /** 以最终判定的深浅色重设色板与窗口背景 */
    private void applyPalette(boolean night) {
        nightMode = night;
        appliedNight = night ? 1 : 0;
        if (nightMode) {
            BG = 0xFF000000;
            CARD = 0xFF1C1C1E;
            TEXT = 0xFFFFFFFF;
            SUB = 0xFF8E8E93;
            LINE = 0xFF2C2C2E;
        } else {
            BG = 0xFFF2F3F5;
            CARD = 0xFFFFFFFF;
            TEXT = 0xFF1A1A1A;
            SUB = 0xFF8A8A8E;
            LINE = 0xFFEFEFF0;
        }
        kit = new UiKit(this, new Theme(BG, CARD, LINE, TEXT, SUB, ACCENT, nightMode));
        try {
            getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                    nightMode ? 0xFF000000 : BG));
        } catch (Throwable ignored) {
        }

    }

    private void applyWindowChrome() {
        // 状态栏透明，让背景延伸到状态栏下面；图标颜色按深浅模式切换
        try {
            getWindow().addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(0);
            getWindow().setNavigationBarColor(0);
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                // 30 起必须关掉 decorFitsSystemWindows 才会真的延伸到状态栏/导航栏下面
                getWindow().setDecorFitsSystemWindows(false);
            } else {
                int flags = android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                if (!nightMode) flags |= android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                getWindow().getDecorView().setSystemUiVisibility(flags);
            }
        } catch (Throwable ignored) {
        }
    }

    /* ───────── 悬浮底栏（1.7.0 界面改版） ─────────
     * 尺寸、弹簧、按下倍率、翻页联动的判据逐项对照下面两个开源项目源码后重写，
     * 没有复制代码，也没有引入它们的运行时依赖；这里是纯 framework + Canvas 实现。
     *
     *   KernelSU（GPL-3.0）— manager 模块的底栏
     *     ui/component/FloatingBottomBar.kt
     *        底栏 64dp、内边 4dp、指示器 56dp 整格宽、10% 底色、velocity 形变公式
     *     ui/component/miuix/animation/DampedDragAnimation.kt
     *        value 以「格」为单位、spring(1f, 1000f)、animateToValue 里 press/release 的时序
     *     ui/component/bottombar/MainPagerState.kt
     *        isNavigating 与 syncPage：滑动过半切选中态，点底栏期间不回改
     *     https://github.com/tiann/KernelSU
     *
     *   compose-miuix-ui / miuix（Apache-2.0）— KernelSU 底栏的上游
     *     utils/PagerGestureUtils.kt 的 PagerNavigationSpringSpec
     *        stiffness 322.2、dampingRatio 32.31 / (2 * sqrt(322.2))、visibilityThreshold 0.5
     *     blur 的 IosLiquidGlassNavigationBar 示例（KernelSU 的浮动底栏由它改写而来）
     *     https://github.com/compose-miuix-ui/miuix
     *
     * 没照搬的部分：lens() 折射要用 Compose + miuix-blur 的 shader，本项目没有 AndroidX/Compose，
     * 用 BitmapShader 玻璃球加渐变高光近似（GlassPill / GlossView）。
     */
    /* 组件拦截页的搜索：一行一个开关，记下它所在的卡片与前置分隔线，
     * 过滤时整卡没有命中就一起隐藏，避免留下空的分组标题。 */
    private static final class SearchHit {
        final android.view.View card;
        final android.view.View row;
        final android.view.View divider;
        final String text;

        SearchHit(android.view.View card, android.view.View row,
                  android.view.View divider, String text) {
            this.card = card;
            this.row = row;
            this.divider = divider;
            this.text = text;
        }
    }

    /* 本机没有安装的组件：作用域永远加不上，提示要常显、点击要挡掉，
     * 否则会陷入「点加入 → 失败 → 开关回滚」的循环（issue #26）。 */

    private final java.util.List<SearchHit> searchHits = new ArrayList<>();
    private android.widget.EditText searchBox;
    private TextView searchEmpty;
    private String searchQuery = "";

    private Stage stage;
    private BottomBarController bar;

    /** 把胶囊推向目标格位；已在跑就只换目标，速度连续 */

    /**
     * 松手吸附：取 targetValue，不是当前 value。
     * 源码 onDragStopped 里是 targetValue.roundToInt()——快速点击时弹簧还没走到，
     * 用 pillValue 会取到上一格，表现就是「点了不切页」。
     */


    private void buildUi() {
        FrameLayout rootFrame = new FrameLayout(this);
        rootFrame.setBackgroundColor(BG);

        stage = new Stage(this);
        stage.setPageListener(new Stage.PageListener() {
            @Override
            public void onPageSelected(int index) {
                /* 手指正按着底栏时，上一段页面吸附动画结束不能改选中态：
                 * 它会把手点击的那一格覆盖掉，松手就变成「切回当前页」。 */
                if (!bar.isTouching()) bar.updateTabUi(index);
            }

            @Override
            public void onPageScrolled(float progress) {
                bar.onPageScrolled(progress);
            }
        });
        rootFrame.addView(stage, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        page.pageScrolls.clear();
        page.pageScrolls.add(buildPage(stage, buildGeneralPage()));
        page.pageScrolls.add(buildPage(stage, buildComponentsPage()));
        page.pageScrolls.add(buildPage(stage, buildSettingsPage()));
        stage.setPageCount(page.pageScrolls.size());
        // 深浅色切换会重建界面，必须把当前页还原回去，否则会跳回第一页
        bar = new BottomBarController(this, stage, page.pageScrolls);
        stage.resetTo(bar.currentTab());
        bar.build(rootFrame);

        setContentView(rootFrame);
        I18n.apply(rootFrame);
        // 深浅两种模式都要设状态栏图标颜色，否则浅色下会出现白图标压白底
        try {
            android.view.WindowInsetsController c =
                    getWindow().getDecorView().getWindowInsetsController();
            if (c != null) {
                int light = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;
                c.setSystemBarsAppearance(nightMode ? 0 : light, light);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * 横向分页容器：三页同屏叠放，全部靠 translationX 定位。
     * 不用 scrollTo，规避 onLayout/测量时机导致的偏移错乱；
     * 命中测试会跟随 translationX，所以不会误触到屏幕外那一页的按钮。
     */


    /** 一页 = 一个 ScrollView；三页都保持 VISIBLE，位置靠 translationX 分开 */
    private ScrollView buildPage(Stage s, LinearLayout root) {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        sv.setFillViewport(true);
        sv.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        sv.setOnScrollChangeListener((v, sx, sy, osx, osy) -> bar.scheduleGlassRefresh());
        s.addView(sv, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return sv;
    }

    /** 底栏外壳的折射 shader（与胶囊各用一个实例，uniform 不共享） */
    private android.graphics.RuntimeShader shellLens;

    /** 内容滚动或切页时节流刷新底栏模糊，保证玻璃后面一直是当前真实内容 */

    private LinearLayout pageRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(30) + statusBarHeight(), dp(16), dp(24) + dp(104));
        return root;
    }

    /** 切到第 idx 页：页面滑过去，胶囊同时吸过去 */

    /** 只更新选中态与胶囊目标；由点击、拖动吸附、页面吸附三处共用 */

    private TextView pageTitle(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(TEXT);
        title.setPadding(dp(12), dp(8), 0, dp(18));
        return title;
    }

    /** 首页：总开关、路径特征、诊断、规则、日志、批量操作 */
    // ===== 页面：主页 =====
    private LinearLayout buildGeneralPage() {
        LinearLayout root = pageRoot();
        root.addView(pageTitle("HyperPGuard"));
        root.addView(kit.sectionRow("通用", "tip：开关打开 = 拦截该行为，关闭 = 放行"));
        LinearLayout global = kit.card();
        makeSwitchRow(global, "总开关", "关闭后本模块在所有进程不生效",
                Config.K_MASTER, true);
        global.addView(kit.divider());
        makeSwitchRow(global, "路径特征", "按路径特征识别上报，是否生效",
                Config.K_PATH, true);
        global.addView(kit.divider());
        makeSwitchRow(global, "记录未命中访问", "把没命中规则的域名写进日志，排查漏拦时临时打开",
                Config.K_SNIFF, true);
        global.addView(kit.divider());
        makeSwitchRow(global, "开关热重载", "改开关后自动让已注入的进程换新配置；关闭则需手动重启",
                Config.K_HOT_RELOAD, true);
        global.addView(kit.divider());
        addFraudDbRow(global);
        global.addView(kit.divider());
        addMisightRow(global);
        global.addView(kit.divider());
        addCustomRulesRow(global);
        global.addView(kit.divider());
        addSelfCheckRow(global);
        root.addView(global);

        return root;
    }

    /** 组件拦截：批次操作 + 十九个组件分组 */
    // ===== 页面：组件拦截 =====
    private LinearLayout buildComponentsPage() {
        LinearLayout root = pageRoot();
        root.addView(pageTitle("组件拦截"));
        searchHits.clear();
        root.addView(buildSearchBox());
        LinearLayout bulk = new LinearLayout(this);
        bulk.setOrientation(LinearLayout.HORIZONTAL);
        bulk.setPadding(0, 0, 0, dp(10));
        bulk.addView(bulkButton("全部启用", this::requestAll));
        bulk.addView(bulkButton("全部停用", this::removeAll));
        bulk.addView(bulkButton("全部重启", this::restartAll));
        root.addView(bulk);
        for (final Group g : GROUPS) {
            LinearLayout c = kit.card();
            List<Switch> children = new ArrayList<>();
            List<LinearLayout> rows = new ArrayList<>();
            childSwitches.put(g.pkg, children);
            childRows.put(g.pkg, rows);

            c.addView(groupHeader(g, rows));
            if (g.note != null) c.addView(groupNote(g.note));
            boolean present = isInstalled(g.pkg);
            if (!present) page.missingGroups.add(g.pkg);
            TextView scopeHint = new TextView(this);
            scopeHint.setText(T(present ? "未在作用域，点此加入" : "本机未安装该组件"));
            scopeHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            scopeHint.setTextColor(present ? ACCENT : SUB);
            scopeHint.setPadding(dp(4), dp(10), 0, dp(8));
            scopeHint.setVisibility(android.view.View.GONE);
            if (present) {
                scopeHint.setOnClickListener(v -> ensureInScope(g));
            }
            c.addView(scopeHint);
            page.scopeHints.put(g.pkg, scopeHint);
            for (Item it : g.items) {
                android.view.View dv = null;
                if (c.getChildCount() > 0) {
                    dv = kit.divider();
                    c.addView(dv);
                }
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setMinimumHeight(dp(56));
                row.setPadding(0, dp(9), 0, dp(9));
                Switch sw = buildSwitchRow(row, it.name, it.desc, it.key, true, g);
                children.add(sw);
                rows.add(row);
                c.addView(row);
                /* 组名与组说明也进索引：不然搜「输入法」这种只出现在分组标题里的词
                 * 会一条都命中不了（搜「输入」能中，是因为个别描述里带这两个字）。 */
                searchHits.add(new SearchHit(c, row, dv,
                        (g.title + " " + (g.note == null ? "" : g.note) + " "
                                + it.name + " " + (it.desc == null ? "" : it.desc))
                                .toLowerCase(java.util.Locale.ROOT)));
            }
            root.addView(c);
            setGroupUi(g, Config.get(this, g.key, Config.def(g.key)));
        }

        searchEmpty = new TextView(this);
        searchEmpty.setText(T("没有匹配的开关"));
        searchEmpty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        searchEmpty.setTextColor(SUB);
        searchEmpty.setGravity(Gravity.CENTER);
        searchEmpty.setPadding(0, dp(24), 0, dp(24));
        searchEmpty.setVisibility(android.view.View.GONE);
        root.addView(searchEmpty);

        if (!searchQuery.isEmpty()) applySearchFilter(searchQuery);
        return root;
    }

    /** 搜索框：48dp 胶囊，左侧自绘放大镜，形状照 HyperCeiler 的设置页搜索条 */
    private android.view.View buildSearchBox() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        // 浅色与卡片同色（页面底色是浅灰），深色用卡片色，圆角取高度的一半
        bg.setColor(nightMode ? 0xFF1C1C1E : 0xFFFFFFFF);
        bg.setCornerRadius(dp(24));
        box.setBackground(bg);
        box.setPadding(dp(16), 0, dp(16), 0);

        android.view.View glass = new SearchGlassIcon(this, SUB);
        LinearLayout.LayoutParams ilp =
                new LinearLayout.LayoutParams(dp(18), dp(18));
        ilp.rightMargin = dp(10);
        box.addView(glass, ilp);

        searchBox = new android.widget.EditText(this);
        searchBox.setBackground(null);
        searchBox.setHint(T("搜索"));
        searchBox.setHintTextColor(SUB);
        searchBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        searchBox.setTextColor(TEXT);
        searchBox.setSingleLine(true);
        searchBox.setPadding(0, 0, 0, 0);
        searchBox.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        searchBox.setText(searchQuery);
        searchBox.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence c, int a, int b, int d) {
            }

            @Override
            public void onTextChanged(CharSequence c, int a, int b, int d) {
            }

            @Override
            public void afterTextChanged(android.text.Editable e) {
                searchQuery = e.toString();
                applySearchFilter(searchQuery);
            }
        });
        box.addView(searchBox, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        lp.bottomMargin = dp(10);
        box.setLayoutParams(lp);
        return box;
    }

    /** 按名称/描述过滤；整张卡片都不命中时连分组标题一起隐藏 */
    private void applySearchFilter(String q) {
        if (searchHits.isEmpty()) return;
        String k = q == null ? "" : q.trim().toLowerCase(java.util.Locale.ROOT);
        java.util.HashMap<android.view.View, Boolean> cards = new java.util.HashMap<>();
        boolean any = false;
        for (SearchHit h : searchHits) {
            boolean show = k.isEmpty() || h.text.contains(k);
            h.row.setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
            if (h.divider != null) {
                h.divider.setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
            }
            Boolean cur = cards.get(h.card);
            cards.put(h.card, show || (cur != null && cur));
            if (show) any = true;
        }
        for (java.util.Map.Entry<android.view.View, Boolean> e : cards.entrySet()) {
            e.getKey().setVisibility(e.getValue()
                    ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        if (searchEmpty != null) {
            searchEmpty.setVisibility(k.isEmpty() || any
                    ? android.view.View.GONE : android.view.View.VISIBLE);
        }
    }

    /** 放大镜：一个圆环加一条斜手柄，尺寸跟着视图走 */
    private static final class SearchGlassIcon extends android.view.View {
        private final android.graphics.Paint paint =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final int color;

        SearchGlassIcon(android.content.Context c, int color) {
            super(c);
            this.color = color;
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            float s = Math.min(getWidth(), getHeight());
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = s * 0.29f;
            float ox = cx - s * 0.07f;
            float oy = cy - s * 0.07f;
            paint.setColor(color);
            paint.setStyle(android.graphics.Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1.5f, s * 0.10f));
            paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
            canvas.drawCircle(ox, oy, r, paint);
            canvas.drawLine(ox + r * 0.70f, oy + r * 0.70f,
                    cx + s * 0.30f, cy + s * 0.30f, paint);
        }
    }

    /** 关于：仓库、模块页、反馈、版本、更新与免责声明 */
    // ===== 页面：设置 =====
    private LinearLayout buildSettingsPage() {
        LinearLayout root = pageRoot();
        root.addView(pageTitle("设置"));

        LinearLayout actions = kit.card();
        kit.addListRow(actions, "查看日志", "读取本模块在 logcat 里的输出", () -> loadLog(false));
        actions.addView(kit.divider());
        kit.addListRow(actions, "分享日志", "把日志文本导出分享", () -> loadLog(true));
        actions.addView(kit.divider());
        addCleanLogRow(actions);
        actions.addView(kit.divider());
        addHideIconRow(actions);
        actions.addView(kit.divider());
        kit.addListRow(actions, "配置导入导出", "导出到 Download / 从剪贴板导入",
                () -> ConfigIO.show(this, kit));
        root.addView(actions);

        root.addView(kit.sectionRow("关于", ""));
        LinearLayout about = kit.card();
        about.addView(kit.linkRow("GitHub 仓库", "vivibibi30/hyperpguard", () -> openUrl(URL_REPO)));
        about.addView(kit.line());
        about.addView(kit.linkRow("LSPosed 模块页", "modules.lsposed.org", () -> openUrl(URL_MODULE)));
        about.addView(kit.line());
        about.addView(kit.linkRow("问题反馈", "Issues", () -> openUrl(URL_ISSUES)));
        about.addView(kit.line());
        about.addView(kit.infoRow("版本", "v" + Config.VERSION));
        Button checkUpdateBtn = new Button(this);
        checkUpdateBtn.setText("检查更新");
        checkUpdateBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        checkUpdateBtn.setBackground(null);
        checkUpdateBtn.setTextColor(ACCENT);
        checkUpdateBtn.setAllCaps(false);
        checkUpdateBtn.setPadding(0, dp(10), 0, dp(10));
        checkUpdateBtn.setOnClickListener(v -> checkUpdate());
        about.addView(checkUpdateBtn);
        root.addView(about);

        Button disclaimer = new Button(this);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.topMargin = dp(4);
        disclaimer.setLayoutParams(dlp);
        disclaimer.setText("免责声明 / 使用条款");
        disclaimer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        disclaimer.setBackground(null);
        disclaimer.setTextColor(SUB);
        disclaimer.setOnClickListener(v -> showDisclaimer());
        root.addView(disclaimer);
        return root;
    }

    /** 整行一个动作按钮：和组件页的批量按钮同一版式 */




    /** 服务未连接时暂存的动作，绑定成功后自动执行一次 */
    private Runnable pendingAction;

    /** 没连上就把动作排队，绑定成功后执行；4 秒还没连上才提示失败 */
    private void runWhenBound(final Runnable action) {
        if (xposed != null) {
            action.run();
            return;
        }
        pendingAction = action;
        bindServiceHelper();
        Toast.makeText(this, "正在连接 LSPosed…", Toast.LENGTH_SHORT).show();
        ui.postDelayed(() -> {
            if (pendingAction == action && xposed == null) {
                pendingAction = null;
                Toast.makeText(MainActivity.this,
                        "LSPosed 服务未连接，请确认已在 LSPosed 中启用本模块后重开本界面",
                        Toast.LENGTH_LONG).show();
            }
        }, 4000);
    }

    /** LSPosed 服务断开后重连：覆盖安装模块时服务端会重启一次 */
    private void bindServiceHelper() {
        try {
            XposedServiceHelper.registerListener(serviceListener);
        } catch (Throwable t) {
            android.util.Log.w("HyperPGuard", "registerListener fail", t);
        }
    }

    private final XposedServiceHelper.OnServiceListener serviceListener =
            new XposedServiceHelper.OnServiceListener() {
                @Override
                public void onServiceBind(XposedService service) {
                    xposed = service;
                    bindRetry = 0;
                    android.util.Log.i("HyperPGuard", "XposedService bound: "
                            + service.getFrameworkName() + " " + service.getFrameworkVersion());
                    ui.post(MainActivity.this::syncFromService);
                    final Runnable pending = pendingAction;
                    pendingAction = null;
                    if (pending != null) ui.post(pending);
                }

                @Override
                public void onServiceDied(XposedService service) {
                    xposed = null;
                    android.util.Log.w("HyperPGuard", "XposedService died");
                    bindRetry++;
                    if (bindRetry < 30) {
                        ui.postDelayed(MainActivity.this::bindServiceHelper, 2000);
                    }
                }
            };

    private final android.content.BroadcastReceiver themeReceiver =
            new android.content.BroadcastReceiver() {
                @Override
                public void onReceive(android.content.Context context, android.content.Intent intent) {
                    syncThemeIfNeeded();
                }
            };

    /** 系统深浅色变了就重建一次；onConfigurationChanged 没回调时靠它兜底 */
    private void syncThemeIfNeeded() {
        android.content.res.Configuration c =
                getApplicationContext().getResources().getConfiguration();
        int want = (c.uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES ? 1 : 0;
        // 部分 ROM 上配置对象会滞后，直接用 UiModeManager 的显式设置兜底
        try {
            android.app.UiModeManager um =
                    (android.app.UiModeManager) getSystemService(UI_MODE_SERVICE);
            if (um != null) {
                int mode = um.getNightMode();
                if (mode == android.app.UiModeManager.MODE_NIGHT_YES) want = 1;
                else if (mode == android.app.UiModeManager.MODE_NIGHT_NO) want = 0;
            }
        } catch (Throwable ignored) {
        }
        android.util.Log.i("HyperPGuard", "theme check want=" + want + " applied=" + appliedNight);
        if (appliedNight == -1 || want == appliedNight) return;
        int y = scrollRoot == null ? 0 : scrollRoot.getScrollY();
        applyPalette(want == 1);
        applyWindowChrome();
        buildUi();
        if (y > 0 && scrollRoot != null) {
            scrollRoot.post(() -> scrollRoot.scrollTo(0, y));
        }
        if (shownDialog != null && shownDialog.isShowing()) styleDialog(shownDialog);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) return;
        syncThemeIfNeeded();
        // 系统主题广播可能晚于焦点变化，稍后再核一次
        ui.postDelayed(this::syncThemeIfNeeded, 400);
    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(themeReceiver);
        } catch (Throwable ignored) {
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (scrollRoot != null) out.putInt("scrollY", scrollRoot.getScrollY());
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncThemeIfNeeded();
        if (xposed != null) {
            syncFromService();
        } else {
            bindServiceHelper();
        }
    }

    /* ---------------- 软重启 ---------------- */

    private void requestAdd(final String pkg, final Group g, final Switch sw) {
        if (xposed == null) {
            runWhenBound(() -> requestAdd(pkg, g, sw));
            return;
        }
        android.util.Log.i("HyperPGuard", "requestScope(" + pkg + ") sending");
        try {
            xposed.requestScope(Collections.singletonList(pkg),
                    new XposedService.OnScopeEventListener() {
                        @Override
                        public void onScopeRequestApproved(java.util.List<String> approved) {
                            ui.post(() -> commitState(g, sw, true,
                                    "已加入作用域：" + approved));
                        }

                        @Override
                        public void onScopeRequestFailed(String message) {
                            android.util.Log.w("HyperPGuard", "requestScope(" + pkg + ") failed: " + message);
                            ui.post(() -> {
                                showScopeFailure("「" + g.title + "」加入作用域失败", pkg, message);
                                syncFromService();
                            });
                        }
                    });
        } catch (Throwable t) {
            Toast.makeText(this, "请求失败：" + t, Toast.LENGTH_LONG).show();
        }
    }

    private void requestRemove(final String pkg, final Group g, final Switch sw) {
        if (xposed == null) return;
        try {
            xposed.removeScope(Collections.singletonList(pkg));
            commitState(g, sw, false, "已移出作用域：" + pkg);
        } catch (Throwable t) {
            Toast.makeText(this, "移除失败：" + t, Toast.LENGTH_LONG).show();
        }
    }

    /** 一次性请求全部作用域 */
    // ===== 作用域：批量与单组 =====
    private void requestAll() {
        if (xposed == null) {
            runWhenBound(this::requestAll);
            return;
        }
        final java.util.List<String> all = new java.util.ArrayList<>();
        for (Group g : GROUPS) {
            if (isInstalled(g.pkg)) all.add(g.pkg);
        }
        if (all.isEmpty()) {
            Toast.makeText(this, "没有可加入的目标应用", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            xposed.requestScope(all, new XposedService.OnScopeEventListener() {
                @Override
                public void onScopeRequestApproved(java.util.List<String> approved) {
                    ui.post(() -> {
                        for (Group g : GROUPS) {
                            if (approved != null && approved.contains(g.pkg)) {
                                Config.uiSet(MainActivity.this, g.key, true);
                            }
                        }
                        Toast.makeText(MainActivity.this,
                                "已加入 " + (approved == null ? 0 : approved.size())
                                        + " 个作用域", Toast.LENGTH_SHORT).show();
                        syncFromService();
                        scheduleHotReload(null);
                    });
                }

                @Override
                public void onScopeRequestFailed(String message) {
                    ui.post(() -> showScopeFailure("批量加入作用域失败", "（多个应用）", message));
                }
            });
        } catch (Throwable t) {
            Toast.makeText(this, "请求失败：" + t, Toast.LENGTH_LONG).show();
        }
    }

    private void removeAll() {
        if (xposed == null) return;
        final java.util.List<String> all = new java.util.ArrayList<>();
        for (Group g : GROUPS) all.add(g.pkg);
        try {
            xposed.removeScope(all);
            for (Group g : GROUPS) Config.uiSet(this, g.key, false);
            Toast.makeText(this, "已移出全部作用域", Toast.LENGTH_SHORT).show();
            syncFromService();
            scheduleHotReload(null);
        } catch (Throwable t) {
            Toast.makeText(this, "移除失败：" + t, Toast.LENGTH_LONG).show();
        }
    }

    /** LSPosed 确认后才更新开关与配置 */
    private void commitState(final Group g, final Switch sw, boolean on, String msg) {
        Config.uiSet(this, g.key, on);
        setGroupUi(g, on, true);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /* ---------------- 环境检测 ---------------- */

    private boolean isInstalled(String pkg) {
        try {
            getPackageManager().getApplicationInfo(pkg, 0);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /* ---------------- 组状态联动（真实作用域驱动，不置灰） ---------------- */

    /**
     * 把一组的状态刷到界面上。
     * 子项永远可点、永远满透明度；on 只反映这一组真实是否在作用域里。
     */
    /**
     * 作用域请求被拒时给出可操作的说明。
     * 常见于系统 priv-app：LSPosed 的 requestScope 会拒绝，但管理器里手动勾选可以。
     */
    private void showScopeFailure(String title, String pkg, String message) {
        showStyled(new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("包名：" + pkg
                        + "\n\nLSPosed 返回：" + (message == null || message.isEmpty() ? "（无）" : message)
                        + "\n\n可以在 LSPosed 管理器 → 模块 → HyperPGuard → 作用域 里手动勾选该应用。"
                        + "\n手动勾选后回到本界面，开关会跟着变成开启。")
                .setPositiveButton(T("知道了"), null));
    }

    /**
     * 液态玻璃背景：把底栏背后那一块页面画到位图，模糊后作为底栏背景。
     * 只绘制页面容器，不会把底栏自己拍进去。
     */
    // ===== 底栏玻璃与刷新 =====

    private void setGroupUi(Group g, boolean on) {
        setGroupUi(g, on, false);
    }

    /** animate=false 用于作用域同步与回滚，避免状态还没确认就先播放动画 */
    private void setGroupUi(Group g, boolean on, boolean animate) {
        TextView scopeHint = page.scopeHints.get(g.pkg);
        if (scopeHint != null) {
            boolean missing = page.missingGroups.contains(g.pkg);
            scopeHint.setVisibility(missing || !on
                    ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        updatingSwitch = true;
        Switch gs = page.groupSwitches.get(g.pkg);
        if (gs instanceof PgSwitch) {
            PgSwitch pg = (PgSwitch) gs;
            if (animate) {
                pg.setChecked(on);
            } else {
                pg.setCheckedImmediate(on);
            }
        } else if (gs != null) {
            gs.setChecked(on);
        }
        updatingSwitch = false;

        List<Switch> children = childSwitches.get(g.pkg);
        if (children != null) {
            for (Switch sw : children) {
                sw.setEnabled(on);
                sw.setAlpha(on ? 1f : 0.4f);
            }
        }
        List<LinearLayout> rows = childRows.get(g.pkg);
        if (rows != null) {
            for (LinearLayout r : rows) r.setAlpha(on ? 1f : 0.4f);
        }
    }

    java.util.List<String> safeScope() {
        if (xposed == null) return null;
        try {
            java.util.List<String> sc = xposed.getScope();
            android.util.Log.i("HyperPGuard", "getScope -> " + sc);
            return sc;
        } catch (Throwable t) {
            android.util.Log.w("HyperPGuard", "getScope fail", t);
            return null;
        }
    }

    /**
     * 从 LSPosed 读真实作用域，刷新所有组开关与配置。
     * 这是「开关状态跟着框架走」的唯一入口：onServiceBind / onResume 都调它。
     */
    // ===== 作用域：与服务同步 =====
    private void syncFromService() {
        java.util.List<String> scope = safeScope();
        for (Group g : GROUPS) {
            boolean on;
            if (scope == null) {
                on = Config.get(this, g.key, Config.def(g.key));
            } else {
                on = scope.contains(g.pkg);
                Config.uiSet(this, g.key, on);
            }
            setGroupUi(g, on);
        }
        if (scope != null && scope.isEmpty() && !autoRequested) {
            // 首次安装且框架里还没有任何作用域：只自动请求一次，之后交给用户
            autoRequested = true;
            requestAll();
        }
    }

    /** 子项被打开时，如果该应用还没进作用域，顺手请求加入（不再靠置灰拦人） */
    // ===== 作用域：加入请求 =====
    private void ensureInScope(final Group g) {
        if (xposed == null) {
            runWhenBound(() -> ensureInScope(g));
            return;
        }
        java.util.List<String> cur = safeScope();
        if (cur != null && cur.contains(g.pkg)) return;
        try {
            xposed.requestScope(Collections.singletonList(g.pkg),
                    new XposedService.OnScopeEventListener() {
                        @Override
                        public void onScopeRequestApproved(java.util.List<String> approved) {
                            ui.post(() -> {
                                Config.uiSet(MainActivity.this, g.key, true);
                                setGroupUi(g, true);
                            });
                        }

                        @Override
                        public void onScopeRequestFailed(String message) {
                            ui.post(() -> showScopeFailure("「" + g.title + "」加入作用域失败",
                                    g.pkg, message));
                        }
                    });
        } catch (Throwable t) {
            Toast.makeText(this, "请求失败：" + t, Toast.LENGTH_LONG).show();
        }
    }

    private android.view.View bulkButton(final String label, final Runnable action) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setTextColor(ACCENT);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(kit.pressBackground());
        tv.setPadding(0, dp(10), 0, dp(10));
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tv.setOnClickListener(v -> action.run());
        return tv;
    }

    /* ---------------- 卡片与控件 ---------------- */



    /** 系统状态栏高度，用于沉浸式布局的顶部留白 */
    private int statusBarHeight() {
        try {
            int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (id > 0) return getResources().getDimensionPixelSize(id);
        } catch (Throwable ignored) {
        }
        return dp(24);
    }

    /** 系统导航栏高度，沉浸式布局下补底部留白 */
    int navBarHeight() {
        try {
            int id = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (id > 0) return getResources().getDimensionPixelSize(id);
        } catch (Throwable ignored) {
        }
        return 0;
    }

    /** 列表分隔线：0.75dp，左侧从内容边缘开始，和 Settings 的 item divider 一致 */




    private android.view.View groupNote(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setTextColor(SUB);
        tv.setPadding(dp(4), 0, dp(4), dp(8));
        return tv;
    }

    private ViewGroup groupHeader(final Group g, List<LinearLayout> rows) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        // 左右 4dp 与子项行、批量按钮同一条左边线
        row.setPadding(dp(4), dp(12), dp(4), dp(8));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(0, 0, dp(12), 0);
        texts.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView t1 = new TextView(this);
        t1.setText(g.title);
        t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        t1.setTextColor(TEXT);

        TextView t2 = new TextView(this);
        t2.setText(g.pkg);
        t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        t2.setTextColor(SUB);
        t2.setMaxLines(1);
        t2.setEllipsize(android.text.TextUtils.TruncateAt.END);
        t2.setPadding(0, dp(2), 0, 0);

        texts.addView(t1);
        texts.addView(t2);
        row.addView(texts);

        TextView reboot = new TextView(this);
        reboot.setText("重启");
        reboot.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        reboot.setTextColor(ACCENT);
        reboot.setBackground(kit.pressBackground());
        reboot.setPadding(dp(14), dp(8), dp(14), dp(8));
        reboot.setOnClickListener(v -> restartApp(g.pkg));
        row.addView(reboot);

        final Switch sw = new PgSwitch(this, ACCENT,
                nightMode ? 0xFF3A3A3C : 0xFFE9E9EB);
        sw.setLayoutParams(new LinearLayout.LayoutParams(dp(49), dp(28)));
        sw.setLayoutParams(new LinearLayout.LayoutParams(dp(49), dp(28)));
        page.groupSwitches.put(g.pkg, sw);
        sw.setChecked(Config.get(this, g.key, Config.def(g.key)));
        if (sw instanceof PgSwitch) {
            // 点击只上报目标状态，开关本身不动；等作用域真的变了再切
            ((PgSwitch) sw).setOnToggleRequest(target -> {
                if (updatingSwitch) return;
                if (target) {
                    if (!isInstalled(g.pkg)) {
                        Toast.makeText(this,
                                T("本机未安装该组件，无需加入作用域"),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    requestAdd(g.pkg, g, sw);
                } else {
                    requestRemove(g.pkg, g, sw);
                }
            });
        }
        sw.setOnCheckedChangeListener(null);
        row.addView(sw);
        return row;
    }

    /** 自建一行并挂到 parent，返回该 Switch */
    private Switch makeSwitchRow(LinearLayout parent, String name, String desc,
                                 final String key, boolean enabled) {
        LinearLayout row = new LinearLayout(this);
        parent.addView(row);
        return buildSwitchRow(row, name, desc, key, enabled);
    }

    /** 把控件填进已有 row，返回该 Switch */
    private Switch buildSwitchRow(LinearLayout row, String name, String desc,
                                  final String key, boolean enabled) {
        return buildSwitchRow(row, name, desc, key, enabled, null);
    }

    /** 带所属组的版本：打开子项时顺手保证该应用在作用域内，不再靠置灰拦人 */
    private Switch buildSwitchRow(LinearLayout row, String name, String desc,
                                  final String key, boolean enabled,
                                  final Group group) {
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(56));
        // 左右 4dp 与其他行一致，否则标题比下面几行更贴卡片边缘
        row.setPadding(dp(4), dp(9), dp(4), dp(9));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(0, 0, dp(12), 0);
        texts.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView t1 = new TextView(this);
        t1.setText(name);
        t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t1.setTextColor(TEXT);

        TextView t2 = new TextView(this);
        t2.setText(desc);
        t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t2.setTextColor(SUB);
        t2.setPadding(0, dp(2), 0, 0);

        texts.addView(t1);
        texts.addView(t2);
        row.addView(texts);

        Switch sw = new PgSwitch(this, ACCENT,
                nightMode ? 0xFF3A3A3C : 0xFFE9E9EB);
        sw.setLayoutParams(new LinearLayout.LayoutParams(dp(49), dp(28)));
        sw.setChecked(Config.get(this, key, Config.def(key)));
        sw.setEnabled(enabled);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton v, boolean checked) {
                Config.uiSet(MainActivity.this, key, checked);
                if (checked && group != null) ensureInScope(group);
                scheduleHotReload(group == null ? null : group.pkg);
            }
        });
        row.addView(sw);
        return sw;
    }

    /* ---------------- 清理日志缓冲 ---------------- */

    private void addCleanLogRow(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setMinimumHeight(dp(56));
        row.setPadding(dp(4), dp(9), dp(4), dp(9));
        row.setBackground(kit.pressBackground());
        row.setClickable(true);

        TextView t1 = new TextView(this);
        t1.setText("清理日志缓冲");
        t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t1.setTextColor(TEXT);

        TextView t2 = new TextView(this);
        t2.setText("清空系统 logcat 环形缓冲；只删日志文本，其他应用的日志也会一起清掉。模块配置与行为库不受影响");
        t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t2.setTextColor(SUB);
        t2.setPadding(0, dp(2), 0, 0);

        row.addView(t1);
        row.addView(t2);
        row.setOnClickListener(v -> showStyled(new AlertDialog.Builder(this)
                .setTitle(T("清空日志缓冲？"))
                .setMessage("会清空系统 logcat 的全部缓冲，包括其他应用的日志。"
                        + "模块配置、行为库和已保存的文件都不受影响。")
                .setPositiveButton(T("清空"), (d, w) -> new Thread(() -> {
                    Shell.out("logcat -b all -c");
                    ui.post(() -> Toast.makeText(this, "已清空", Toast.LENGTH_SHORT).show());
                }, "hyperpguard-clean-log").start())
                .setNegativeButton(T("取消"), null)));
        parent.addView(row);
    }

    /* ---------------- 自定义规则 ---------------- */

    /** 主页的「自检」：蓝色文字、居中的整行按钮 */
    private void addSelfCheckRow(LinearLayout parent) {
        parent.addView(kit.singleButton("自检", () -> showDiagnostics()));
    }

    /** 列表行：标题 16sp + 副标题 12sp，与「清理日志缓冲」等完全同一版式 */


    private void addCustomRulesRow(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setMinimumHeight(dp(56));
        row.setPadding(0, dp(9), 0, dp(9));
        row.setClickable(true);
        row.setBackground(kit.pressBackground());
        row.setPadding(dp(4), row.getPaddingTop(), dp(4), row.getPaddingBottom());

        TextView t1 = new TextView(this);
        t1.setText("自定义规则");
        t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t1.setTextColor(TEXT);

        TextView t2 = new TextView(this);
        String raw = Config.customRulesRaw(this).trim();
        int count = raw.isEmpty() ? 0 : raw.split(";;").length;
        t2.setText(I18n.text("按域名、域名/路径 或 类名#方法 追加拦截，当前 %d 条", count));
        t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t2.setTextColor(SUB);
        t2.setPadding(0, dp(2), 0, 0);

        row.addView(t1);
        row.addView(t2);
        row.setOnClickListener(v -> showCustomRules());
        parent.addView(row);
    }

    private void openCustomRulesDoc() {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(URL_CUSTOM_RULES_DOC));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Throwable t) {
            Toast.makeText(this, "没有可以打开链接的应用", Toast.LENGTH_SHORT).show();
        }
    }

    private void renderRules(final LinearLayout ruleList) {
        ruleList.removeAllViews();
        String raw = Config.customRulesRaw(MainActivity.this).trim();
        if (raw.isEmpty()) {
            TextView empty = new TextView(MainActivity.this);
            empty.setText(T("还没有规则"));
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            empty.setTextColor(SUB);
            empty.setPadding(0, dp(14), 0, dp(8));
            ruleList.addView(empty);
            return;
        }
        for (String one : raw.split(";;")) {
            final String rule = one.trim();
            if (rule.isEmpty()) continue;
            LinearLayout row = new LinearLayout(MainActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(dp(44));
            TextView name = new TextView(MainActivity.this);
            name.setText(rule);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            name.setTextColor(TEXT);
            name.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(name);
            TextView del = new TextView(MainActivity.this);
            del.setText(T("删除"));
            del.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            del.setTextColor(ACCENT);
            del.setTag("accent");
            del.setBackground(kit.pressBackground());
            del.setPadding(dp(14), dp(8), dp(14), dp(8));
            del.setOnClickListener(v -> {
                StringBuilder sb = new StringBuilder();
                for (String x : Config.customRulesRaw(MainActivity.this).trim().split(";;")) {
                    String t = x.trim();
                    if (t.isEmpty() || t.equals(rule)) continue;
                    if (sb.length() > 0) sb.append(";;");
                    sb.append(t);
                }
                Config.setCustomRules(MainActivity.this, sb.toString());
                scheduleHotReload(null);
                renderRules(ruleList);
            });
            row.addView(del);
            ruleList.addView(row);
        }
    }

    private void showCustomRules() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(4), dp(20), dp(6));

        final EditText input = new EditText(this);
        input.setHint(T("域名、域名/路径 或 类名#方法"));
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        input.setTextColor(TEXT);
        input.setHintTextColor(SUB);
        input.setSingleLine(true);
        GradientDrawable fieldBg = new GradientDrawable();
        fieldBg.setColor(nightMode ? 0xFF2C2C2E : 0xFFF2F3F5);
        fieldBg.setCornerRadius(dp(12));
        input.setBackground(fieldBg);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.addView(input);

        final LinearLayout ruleList = new LinearLayout(this);
        ruleList.setOrientation(LinearLayout.VERTICAL);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(0, dp(6), 0, dp(2));

        TextView add = new TextView(this);
        add.setText(T("添加"));
        add.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        add.setTextColor(ACCENT);
        add.setTag("accent");
        add.setGravity(Gravity.CENTER);
        add.setBackground(kit.pressBackground());
        add.setPadding(0, dp(10), 0, dp(10));
        add.setOnClickListener(v -> {
            String val = input.getText().toString().trim();
            boolean host = val.matches("[A-Za-z0-9._\\-]+(/[A-Za-z0-9._\\-]*)*");
            boolean method = val.matches("[A-Za-z0-9._$]+#[A-Za-z0-9_$*<>]+");
            if (!host && !method) {
                Toast.makeText(this, "格式：域名、域名/路径 或 类名#方法", Toast.LENGTH_SHORT).show();
                return;
            }
            String raw = Config.customRulesRaw(this).trim();
            for (String one : raw.split(";;")) {
                if (one.trim().equals(val)) {
                    Toast.makeText(this, "已存在", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Config.setCustomRules(this, raw.isEmpty() ? val : raw + ";;" + val);
            scheduleHotReload(null);
            input.setText("");
            renderRules(ruleList);
        });
        actions.addView(add, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView clear = new TextView(this);
        clear.setText(T("清空"));
        clear.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        clear.setTextColor(ACCENT);
        clear.setTag("accent");
        clear.setGravity(Gravity.CENTER);
        clear.setBackground(kit.pressBackground());
        clear.setPadding(0, dp(10), 0, dp(10));
        clear.setOnClickListener(v -> {
            Config.setCustomRules(this, "");
            scheduleHotReload(null);
            renderRules(ruleList);
        });
        actions.addView(clear, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView doc = new TextView(this);
        doc.setText(T("使用说明"));
        doc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        doc.setTextColor(ACCENT);
        doc.setTag("accent");
        doc.setBackground(kit.pressBackground());
        doc.setPadding(dp(4), dp(10), dp(4), dp(10));
        doc.setOnClickListener(v -> openCustomRulesDoc());

        TextView note = new TextView(this);
        note.setText(T("添加或删除后需要重启对应应用才生效"));
        note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        note.setTextColor(SUB);

        box.addView(actions);
        box.addView(ruleList);
        box.addView(doc);
        box.addView(note);

        ScrollView sv = new ScrollView(this);
        sv.addView(box);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(T("自定义规则"))
                .setView(sv)
                .setNegativeButton(T("关闭"), null)
                .create();
        dialog.show();
        trackDialog(dialog);
        renderRules(ruleList);
    }

    /* ---------------- 行为库状态 ---------------- */

    private static final String FRAUD_DB =
            "/data/misc/user/0/securitycenter/fraud_detection_data.db";
    private static java.util.Map<String, Integer> lastSeq;

    private void addFraudDbRow(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setMinimumHeight(dp(56));
        row.setPadding(0, dp(9), 0, dp(9));
        row.setClickable(true);
        row.setBackground(kit.pressBackground());
        row.setPadding(dp(4), row.getPaddingTop(), dp(4), row.getPaddingBottom());

        TextView t1 = new TextView(this);
        t1.setText("安全中心行为库");
        t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t1.setTextColor(TEXT);

        TextView t2 = new TextView(this);
        t2.setText("本机行为库写入计数，用来看拦截是否生效");
        t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t2.setTextColor(SUB);
        t2.setPadding(0, dp(2), 0, 0);

        row.addView(t1);
        row.addView(t2);
        row.setOnClickListener(v -> showFraudDb());
        parent.addView(row);
    }

    /** 与行为库同一张卡、同一版式：标题 16sp + 副标题 12sp，点开进入只读页 */
    private void addMisightRow(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setMinimumHeight(dp(56));
        row.setPadding(0, dp(9), 0, dp(9));
        row.setClickable(true);
        row.setBackground(kit.pressBackground());
        row.setPadding(dp(4), row.getPaddingTop(), dp(4), row.getPaddingBottom());

        TextView t1 = new TextView(this);
        t1.setText("MiSight 事件库");
        t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t1.setTextColor(TEXT);

        TextView t2 = new TextView(this);
        t2.setText("系统行为采集的事件库，只读查看触发场景与参数");
        t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t2.setTextColor(SUB);
        t2.setPadding(0, dp(2), 0, 0);

        row.addView(t1);
        row.addView(t2);
        row.setOnClickListener(v -> startActivity(
                new android.content.Intent(this, MisightActivity.class)));
        parent.addView(row);
    }

    private void showFraudDb() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(8), dp(16), dp(8));

        TextView tv = new TextView(this);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setTextColor(TEXT);
        tv.setTextIsSelectable(false);
        tv.setLongClickable(false);
        tv.setText("读取中…");
        box.addView(tv);

        /* 竖排：清空行为库在上（主操作），刷新在下；两个按钮都占满整行，
         * 点击区域大，也不会出现横排时两段短文字被拉得很开的问题。 */
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        actions.setGravity(Gravity.CENTER);

        TextView refresh = new TextView(this);
        refresh.setText(T("刷新"));
        refresh.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        refresh.setTextColor(ACCENT);
        refresh.setTag("accent");
        refresh.setGravity(Gravity.CENTER);
        refresh.setBackground(kit.pressBackground());
        refresh.setPadding(dp(24), dp(10), dp(24), dp(10));   // 按压范围只比文字大一圈

        TextView clear = new TextView(this);
        clear.setText(T("清空行为库"));
        clear.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        clear.setTextColor(ACCENT);
        clear.setTag("accent");      // paintDialogText 只保留带这个标记的颜色
        clear.setGravity(Gravity.CENTER);
        clear.setBackground(kit.pressBackground());
        clear.setPadding(dp(24), dp(10), dp(24), dp(10));

        // 按压高亮只在文字附近，铺满整行会让整条变色
        actions.addView(clear, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        actions.addView(refresh, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        box.addView(actions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView sv = new ScrollView(this);
        sv.addView(box);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(T("安全中心行为库"))
                .setView(sv)
                .setNegativeButton(T("关闭"), null)
                .create();

        final Runnable load = () -> new Thread(() -> {
            final String text = readFraudDb();
            ui.post(() -> tv.setText(text));
        }, "hyperpguard-frauddb").start();

        refresh.setOnClickListener(v -> {
            tv.setText("读取中…");
            load.run();
        });
        clear.setOnClickListener(v -> showStyled(new AlertDialog.Builder(this)
                .setTitle(T("清空行为库？"))
                .setMessage("会停止安全中心进程，并删除它的行为库文件（含 -wal / -shm）。"
                        + "历史记录不可恢复；安全中心下次启动会重建一个空库。"
                        + "本模块的开关与拦截不受影响。")
                .setPositiveButton(T("清空"), (d2, w2) -> new Thread(() -> {
                    // 通配删掉主库与所有附属文件（-wal / -shm / -journal），
                    // 前缀唯一，不会影响目录里的其它文件
                    Shell.out("am force-stop com.miui.securitycenter; "
                            + "rm -f " + FRAUD_DB + "*");
                    ui.post(() -> {
                        Toast.makeText(this, T("已清空，安全中心下次启动会重建空库"),
                                Toast.LENGTH_LONG).show();
                        tv.setText("读取中…");
                        load.run();
                    });
                }, "hyperpguard-cleardb").start())
                .setNegativeButton(T("取消"), null)));
        dialog.show();
        trackDialog(dialog);
        load.run();
    }

    /** 只读计数：复制主库与 -wal 到私有目录，查 sqlite_sequence，不读任何记录内容 */
    private String readFraudDb() {
        String dir = getFilesDir().getAbsolutePath();
        Shell.out("cp " + FRAUD_DB + " " + dir + "/fd.db 2>/dev/null; "
                + "cp " + FRAUD_DB + "-wal " + dir + "/fd.db-wal 2>/dev/null; "
                + "chmod 644 " + dir + "/fd.db 2>/dev/null; "
                + "chmod 644 " + dir + "/fd.db-wal 2>/dev/null; echo ok");
        java.io.File f = new java.io.File(dir, "fd.db");
        if (!f.exists() || f.length() == 0) {
            return "读不到行为库。\n\n需要 root 授权，且设备上存在 securitycenter 下的行为库；"
                    + "刚清空过的会读不到，安全中心重新打开后会自动重建。";
        }
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(f.getAbsolutePath(), null,
                    SQLiteDatabase.OPEN_READONLY);
            boolean hasSeq = false;
            android.database.Cursor probe = db.rawQuery(
                    "select count(*) from sqlite_master where type='table'"
                            + " and name='sqlite_sequence'", null);
            if (probe.moveToFirst()) hasSeq = probe.getInt(0) > 0;
            probe.close();
            if (!hasSeq) {
                java.util.List<String> tables = new java.util.ArrayList<>();
                android.database.Cursor tc = db.rawQuery(
                        "select name from sqlite_master where type='table'"
                                + " and name not like 'sqlite_%' and name != 'android_metadata'"
                                + " order by name", null);
                while (tc.moveToNext()) tables.add(tc.getString(0));
                tc.close();
                if (tables.isEmpty()) {
                    return "库里还没有行为表（只有系统元数据）。\n\n"
                            + "安全中心产生记录后才会建表；拦截生效时这里不会出现写入。";
                }
                return "库里还没有写入计数。\n已有表：" + tables;
            }
            java.util.TreeMap<String, Integer> now = new java.util.TreeMap<>();
            android.database.Cursor c = db.rawQuery("select name, seq from sqlite_sequence", null);
            while (c.moveToNext()) now.put(c.getString(0), c.getInt(1));
            c.close();
            if (now.isEmpty()) return "库里没有写入记录。";
            StringBuilder sb = new StringBuilder();
            int total = 0;
            for (java.util.Map.Entry<String, Integer> e : now.entrySet()) {
                int v = e.getValue();
                total += v;
                sb.append(e.getKey()).append("  ").append(v);
                Integer old = lastSeq == null ? null : lastSeq.get(e.getKey());
                if (old != null && v != old) sb.append("  (+").append(v - old).append(')');
                sb.append('\n');
            }
            lastSeq = now;
            sb.append("\n共 ").append(now.size()).append(" 张表，累计 ").append(total).append(" 次写入");
            sb.append("\n\n这些数字只增不减。刷新后出现 (+N) 说明期间有写入；");
            sb.append("拦截生效时应当保持不变。");
            return sb.toString();
        } catch (Throwable t) {
            return "读取失败：" + t;
        } finally {
            if (db != null) try { db.close(); } catch (Throwable ignored) { }
            new java.io.File(dir, "fd.db-wal").delete();
            new java.io.File(dir, "fd.db-shm").delete();
            f.delete();
        }
    }

    /* ---------------- 桌面图标 ---------------- */

    private static final String LAUNCHER_ALIAS = "io.github.vivibibi30.hyperpguard.Launcher";

    private boolean launcherIconEnabled() {
        try {
            int st = getPackageManager().getComponentEnabledSetting(
                    new android.content.ComponentName(this, LAUNCHER_ALIAS));
            return st != android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        } catch (Throwable t) {
            return true;
        }
    }

    private void setLauncherIcon(boolean on) {
        try {
            getPackageManager().setComponentEnabledSetting(
                    new android.content.ComponentName(this, LAUNCHER_ALIAS),
                    on ? android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                       : android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP);
        } catch (Throwable t) {
            Toast.makeText(this, "切换失败：" + t, Toast.LENGTH_LONG).show();
        }
    }

    /** 隐藏桌面图标，入口保留在 LSPosed 模块页 */
    private void addHideIconRow(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(56));
        row.setPadding(dp(4), dp(9), dp(4), dp(9));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(0, 0, dp(12), 0);
        texts.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView t1 = new TextView(this);
        t1.setText("隐藏桌面图标");
        t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t1.setTextColor(TEXT);

        TextView t2 = new TextView(this);
        t2.setText("隐藏后从 LSPosed 模块页进入本界面");
        t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t2.setTextColor(SUB);
        t2.setPadding(0, dp(2), 0, 0);

        texts.addView(t1);
        texts.addView(t2);
        row.addView(texts);

        final Switch sw = new PgSwitch(this, ACCENT,
                nightMode ? 0xFF3A3A3C : 0xFFE9E9EB);
        sw.setLayoutParams(new LinearLayout.LayoutParams(dp(49), dp(28)));
        sw.setLayoutParams(new LinearLayout.LayoutParams(dp(49), dp(28)));
        sw.setChecked(!launcherIconEnabled());
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton v, boolean checked) {
                setLauncherIcon(!checked);
                boolean now = !launcherIconEnabled();
                if (now != checked) sw.setChecked(now);
                if (checked) {
                    Toast.makeText(MainActivity.this,
                            "图标已隐藏，可从 LSPosed 模块页进入", Toast.LENGTH_LONG).show();
                }
            }
        });
        row.addView(sw);
        parent.addView(row);
    }



    /** 框架连接、真实作用域、开关数量、已注入进程，用来判断「为什么没拦住」 */
    private boolean diagRetried;

    // ===== 自检 =====
    private void showDiagnostics() {
        if (xposed == null && !diagRetried) {
            diagRetried = true;
            bindServiceHelper();
            ui.postDelayed(this::showDiagnostics, 1500);
            return;
        }
        diagRetried = false;
        Toast.makeText(this, "检测中…（需 root）", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                StringBuilder sb = new StringBuilder();
                sb.append("模块版本：").append(Config.VERSION).append('\n');
                sb.append("LSPosed：").append(xposed == null
                        ? "未连接（重开本界面会自动重连；已注入进程仍以下面的日志为准）"
                        : (xposed.getFrameworkName() + " " + xposed.getFrameworkVersion())).append('\n');

                java.util.List<String> scope = safeScope();
                sb.append("作用域：")
                        .append(scope == null ? "读取失败" : scope.size() + " 个").append("\n\n");

                for (Group g : GROUPS) {
                    boolean in = scope != null && scope.contains(g.pkg);
                    sb.append(in ? "[✓] " : "[ ] ").append(g.pkg);
                    if (!isInstalled(g.pkg)) sb.append("（未安装）");
                    sb.append('\n');
                }

                int local = Config.ui(MainActivity.this).getAll().size();
                int remote = 0;
                try {
                    String raw = android.provider.Settings.Global.getString(
                            getContentResolver(), Config.GLOBAL_KEY);
                    if (raw != null && !raw.isEmpty()) {
                        for (String kv : raw.split("\\|")) if (kv.indexOf('=') > 0) remote++;
                    }
                } catch (Throwable ignored) {
                }
                boolean master = Config.get(MainActivity.this, Config.K_MASTER, Config.def(Config.K_MASTER));
                sb.append("\n框架运行中的目标（getRunningTargets）：\n");
                try {
                    java.util.List<io.github.libxposed.service.HookedTarget> targets =
                            xposed == null ? null : xposed.getRunningTargets();
                    if (targets == null || targets.isEmpty()) {
                        sb.append("（服务未连接，或当前没有运行中的目标）\n");
                    } else {
                        java.util.List<String> names = new java.util.ArrayList<>();
                        for (io.github.libxposed.service.HookedTarget t : targets) {
                            String n = t.getProcessName();
                            if (n != null) names.add(n);
                        }
                        java.util.Collections.sort(names);
                        sb.append(names.size()).append(" 个进程：\n");
                        for (String n : names) sb.append("· ").append(n).append('\n');
                    }
                } catch (Throwable t) {
                    sb.append("读取失败：").append(t).append('\n');
                }

                sb.append("\n总开关：").append(master ? "已开启" : "已关闭（所有进程都会跳过安装）");
                sb.append("\n本地开关 ").append(local).append(" 项，系统设置 ").append(remote).append(" 项\n");

                sb.append("\nLSPosed 加载记录：\n");
                String lsp = Shell.out(
                        "f=$(ls -t /data/adb/lspd/log/modules_*.log 2>/dev/null | head -1); "
                                + "[ -n \"$f\" ] && tail -n 4000 \"$f\""
                                + " | grep -oE '\\([a-z0-9_.]+\\)\\[io\\.github\\.vivibibi30\\.hyperpguard'"
                                + " | sed 's/^(//; s/).*//' | sort -u");
                sb.append(lsp.trim().isEmpty() ? "（日志里没有本模块的加载记录）" : lsp.trim());

                sb.append("\n安装失败记录：\n");
                String fails = Shell.out(
                        "logcat -d -b all -v time | grep -E 'HyperPGuard(\\( *[0-9]+\\))?: (stage|dispatch|net install|privacy install)' | tail -n 40");
                sb.append(fails.trim().isEmpty() ? "（没有）" : fails.trim());

                sb.append("\n最近 attach 日志：\n");
                String attach = Shell.out(
                        "logcat -d -b all -v time | grep -E 'HyperPGuard(\\( *[0-9]+\\))?: attach' | tail -n 15");
                sb.append(attach.trim().isEmpty()
                        ? "（logcat 缓冲里没有，长时间运行后会被冲掉）"
                        : attach.trim());

                final String body = sb.toString();
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        showText("环境自检", body);
                    }
                });
            }
        }).start();
    }

    /** 一次 ps 取全部进程名（去掉 :remote 后缀），避免对每个包各起一次 su */
    private java.util.Set<String> runningProcesses() {
        java.util.Set<String> set = new java.util.HashSet<>();
        String out = Shell.out("ps -A -o NAME 2>/dev/null");
        if (out == null || out.trim().isEmpty()) out = Shell.out("ps -A 2>/dev/null");
        if (out == null) return set;
        for (String line : out.split("\n")) {
            String n = line.trim();
            if (n.isEmpty() || "NAME".equals(n)) continue;
            int sp = n.lastIndexOf(' ');
            if (sp >= 0) n = n.substring(sp + 1);
            int c = n.indexOf(':');
            set.add(c > 0 ? n.substring(0, c) : n);
        }
        return set;
    }

    private boolean isRunning(String pkg) {
        return runningProcesses().contains(pkg);
    }

    private void restartApp(final String pkg) {
        Toast.makeText(this, "正在重启 " + pkg + "（需 root）", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                try {
                    if (!isRunning(pkg)) {
                        /* 没在跑就不用重启：LSPosed 在进程启动时才注入，
                         * 下次打开自然会读到新配置，force-stop 只会白丢它的后台状态。 */
                        msg = pkg + " 未在运行，下次启动会加载新配置";
                    } else {
                        int code = Shell.run("am force-stop " + Shell.q(pkg));
                        msg = code == 0 ? (pkg + " 已重启") : (pkg + " 重启失败 code=" + code);
                    }
                } catch (Throwable t) {
                    msg = "重启失败：" + t;
                }
                final String m = msg;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, m, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    /** 重启作用域内全部应用，让 hook 重新加载 */
    private void restartAll() {
        final List<String> pkgs = new ArrayList<>();
        java.util.List<String> sc = safeScope();
        if (sc != null && !sc.isEmpty()) {
            pkgs.addAll(sc);
        } else {
            for (Group g : GROUPS) pkgs.add(g.pkg);
        }
        if (pkgs.isEmpty()) {
            Toast.makeText(this, "作用域为空", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "正在检查哪些应用在运行（需 root）", Toast.LENGTH_SHORT).show();
        final List<String> all = new ArrayList<>(pkgs);
        new Thread(() -> {
            /* 只重启真在运行的进程：没运行的下次启动本来就会重新注入，
             * 逐个 force-stop 只是白等并让系统多做一次拉起。 */
            final java.util.Set<String> running = runningProcesses();
            final List<String> live = new ArrayList<>();
            for (String p : all) {
                if (running.contains(p)) live.add(p);
            }
            ui.post(() -> restartAllLive(all, live));
        }, "hyperpguard-restartall").start();
    }

    /** 执行部分：live 为空时直接说明 */
    private void restartAllLive(final List<String> all, final List<String> live) {
        if (live.isEmpty()) {
            Toast.makeText(this, "作用域内 " + all.size() + " 个应用都没在运行，无需重启",
                    Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, "正在重启 " + live.size() + " 个运行中的应用（" 
                + (all.size() - live.size()) + " 个未运行，跳过）（需 root）",
                Toast.LENGTH_LONG).show();
        final StringBuilder cmd = new StringBuilder();
        for (String p : live) cmd.append("am force-stop ").append(Shell.q(p)).append("; ");
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                try {
                    int code = Shell.run(cmd.toString());
                    msg = "已重启 " + live.size() + " 个应用" + (code == 0 ? "" : "（code=" + code + "）");
                } catch (Throwable t) {
                    msg = "重启失败：" + t;
                }
                final String m = msg;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, m, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    /* ---------------- 开关热生效 ---------------- */

    /**
     * 开关改动后安排一次热重载。
     *
     * pkg 为空表示这次改动影响所有组件（总开关、路径特征、未命中记录、自定义规则），
     * 否则只重载该组件自己的进程——改一个开关不该让十几个进程一起重扫。
     * 作用域变化不走这里：新加入的作用域要等进程重新启动才会注入。
     */
    void scheduleHotReload(String pkg) {
        if (!Config.get(this, Config.K_HOT_RELOAD, Config.def(Config.K_HOT_RELOAD))) return;
        if (pkg == null || pkg.isEmpty()) hotReloadAll = true;
        else hotReloadPkgs.add(pkg);
        ui.removeCallbacks(hotReloadTask);
        ui.postDelayed(hotReloadTask, 600);
    }

    private void flushHotReload() {
        final List<String> pkgs = hotReloadAll ? null : new ArrayList<>(hotReloadPkgs);
        hotReloadAll = false;
        hotReloadPkgs.clear();
        hotReload(pkgs);
    }

    /** 让已注入的进程换用新配置，不 force-stop；pkgs 为 null 时是作用域内全部 */
    private void hotReload(final List<String> pkgs) {
        if (xposed == null) return;   // 服务没连上：保持原流程，界面仍提示用「重启」
        if (hotReloadRunning) {       // 上一轮还没回完，合并到它之后
            hotReloadQueued = true;
            if (pkgs == null) hotReloadAll = true;
            else hotReloadPkgs.addAll(pkgs);
            return;
        }
        final HotReload.Callback callback = (done, failed, unsupported, message) -> {
            hotReloadRunning = false;
            if (hotReloadQueued) {
                hotReloadQueued = false;
                flushHotReload();
            }
            reportHotReload(done, failed, unsupported, message);
        };
        hotReloadRunning = true;
        int n = 0;
        try {
            n = new HotReload(xposed, pkgs, callback).start();
        } catch (Throwable t) {
            // 服务在发起前断开：别让点击事件把界面带崩，保持「重启」可用
            Logs.i("hot reload start fail: " + t);
            hotReloadRunning = false;
            return;
        }
        if (n == 0) {
            hotReloadRunning = false;
            // 没有在运行的进程：下次启动本来就会读到新配置
            Logs.i("hot reload skipped: no running target"
                    + (pkgs == null ? "" : " " + pkgs));
            Toast.makeText(this, "目标未运行，下次启动生效", Toast.LENGTH_SHORT).show();
        }
    }

    private void reportHotReload(int done, int failed, boolean unsupported, String message) {
        if (done == 0 && failed == 0) return;
        String msg;
        if (unsupported) {
            if (hotReloadHinted) return;
            hotReloadHinted = true;
            msg = "当前框架不支持热重载，请用「重启」或「全部重启」";
        } else if (failed == 0) {
            msg = "已热重载 " + done + " 个进程，开关立即生效";
        } else if (done == 0) {
            msg = "热重载未成功（" + failed + " 个进程，含超时未回执），请用「重启」"
                    + (message == null ? "" : "：" + message);
        } else {
            msg = "已热重载 " + done + " 个进程，" + failed + " 个未回执或失败，可用「重启」重试"
                    + (message == null ? "" : "：" + message);
        }
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    /* ---------------- 日志 ---------------- */

    // ===== 日志：读取与展示 =====
    private void loadLog(final boolean share) {
        Toast.makeText(this, "正在读取日志，约需数秒（需 root）", Toast.LENGTH_LONG).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String log = execLogcat();
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        if (share) doShare(log);
                        else showLog(log);
                    }
                });
            }
        }).start();
    }

    private String execLogcat() {
        String out = Shell.out(
                "logcat -d -b all -v time | grep -E 'HyperPGuard(\\( *[0-9]+\\))?: |blocked by HyperPGuard' | tail -n 600");
        if (out.trim().isEmpty()) {
            return "没有记录。\n\n"
                    + "这里只列模块自身的运行日志（attach、hook 结果）和被拦截的域名访问；"
                    + "没有发生拦截时就是空的，不代表模块没工作。\n\n"
                    + "可能原因：\n"
                    + "1. root 未授权（su 弹窗没点允许）\n"
                    + "2. 模块刚启用，目标应用还没重启\n"
                    + "3. 对应应用的「启用」开关未打开\n"
                    + "4. 旧日志已被 logcat 环形缓冲覆盖\n\n"
                    + "点「自检」可以看框架连接、作用域和已注入进程。";
        }
        return out;
    }

    /** 从日志行里聚合「哪个域名被拦了几次」「多少次 hook miss」，不落库 */


    private void showLog(final String log) {
        String body = "改动开关后会自动热重载已注入的进程；提示热重载失败时，"
                + "再点该组右上角「重启」或批量行的「全部重启」。\n\n"
                + LogTools.blockStats(log) + LogTools.classify(log);
        showText("模块日志（" + log.split("\n").length + " 行）", body);
    }

    /** 按未拦截 / 已拦截 / 其他分段，避免混在一起看不出结果 */


    /** 每段只显示最近若干行，附总数与说明 */


    private void showText(String title, final String log) {
        TextView tv = new TextView(this);
        tv.setText(log);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setTextIsSelectable(true);
        tv.setPadding(dp(14), dp(8), dp(14), dp(8));

        ScrollView sv = new ScrollView(this);
        sv.addView(tv);

        LinearLayout titleBar = new LinearLayout(this);
        titleBar.setOrientation(LinearLayout.HORIZONTAL);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setPadding(dp(24), dp(18), dp(24), dp(6));
        TextView titleText = new TextView(this);
        titleText.setText(title);
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setTextColor(TEXT);
        titleBar.addView(titleText, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (title != null && !title.contains("自检")) {
            TextView titleVersion = new TextView(this);
            titleVersion.setText("v" + Config.VERSION);
            titleVersion.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            titleVersion.setTextColor(SUB);
            titleBar.addView(titleVersion);
        }

        showStyled(new AlertDialog.Builder(this)
                .setCustomTitle(titleBar)
                .setView(sv)
                .setPositiveButton(T("复制"), (d, w) -> {
                    ClipboardManager cm =
                            (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("pg", log));
                    Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton(T("分享"), (d, w) -> doShare(log))
                .setNegativeButton(T("关闭"), null));
    }

    private void doShare(String log) {
        if (log == null || log.trim().isEmpty()) {
            Toast.makeText(this, "没有可分享的内容", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, "运行日志");
        i.putExtra(Intent.EXTRA_TEXT, log);
        startActivity(Intent.createChooser(i, "分享运行日志"));
    }

    /* ---------------- 关于 / 更新 ---------------- */

    /** 与 linkRow 同规格的信息行：右侧值用次要色，不可点击 */




    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)));
        } catch (Throwable t) {
            Toast.makeText(this, "没有可打开链接的应用", Toast.LENGTH_SHORT).show();
        }
    }

    /** 只在用户点击时请求一次 GitHub API 取最新 tag，不做后台轮询 */
    private void checkUpdate() {
        Toast.makeText(this, "正在检查更新…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            String tag = null, err = null;
            java.net.HttpURLConnection c = null;
            try {
                c = (java.net.HttpURLConnection) new java.net.URL(API_LATEST).openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                c.setRequestProperty("Accept", "application/vnd.github+json");
                c.setRequestProperty("User-Agent", "HyperPGuard/" + Config.VERSION);
                int code = c.getResponseCode();
                if (code == 200) {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(c.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String ln;
                    while ((ln = br.readLine()) != null) sb.append(ln);
                    br.close();
                    tag = new org.json.JSONObject(sb.toString()).optString("tag_name", "");
                } else {
                    err = "HTTP " + code;
                }
            } catch (Throwable t) {
                err = t.getClass().getSimpleName();
            } finally {
                if (c != null) c.disconnect();
            }
            final String fTag = tag, fErr = err;
            ui.post(() -> showUpdateResult(fTag, fErr));
        }, "hyperpguard-check-update").start();
    }

    private void showUpdateResult(String tag, String err) {
        if (err != null) {
            Toast.makeText(this, "检查失败：" + err, Toast.LENGTH_LONG).show();
            return;
        }
        String v = tag == null ? "" : tag.trim().replaceAll("^[vV]", "");
        if (v.length() == 0) {
            Toast.makeText(this, "没拿到版本号，稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        if (compareVersion(v, Config.VERSION) > 0) {
            showStyled(new AlertDialog.Builder(this)
                    .setTitle(T("发现新版本 v") + v)
                    .setMessage("当前 v" + Config.VERSION + "，最新 v" + v + "。")
                    .setPositiveButton(T("打开发布页"), (d, w) -> openUrl(URL_RELEASES))
                    .setNegativeButton(T("稍后"), null));
        } else {
            Toast.makeText(this, "已是最新版本（v" + Config.VERSION + "）",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private static int compareVersion(String a, String b) {
        String[] x = a.split("\\.");
        String[] y = b.split("\\.");
        int n = Math.max(x.length, y.length);
        for (int i = 0; i < n; i++) {
            int xi = i < x.length ? versionPart(x[i]) : 0;
            int yi = i < y.length ? versionPart(y[i]) : 0;
            if (xi != yi) return xi > yi ? 1 : -1;
        }
        return 0;
    }

    private static int versionPart(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9].*$", ""));
        } catch (Throwable t) {
            return 0;
        }
    }

    /* ---------------- 免责声明 ---------------- */

    private String readDisclaimerText() {
        String full;
        try {
            java.io.InputStream is = getAssets().open("DISCLAIMER.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            br.close();
            full = sb.toString();
        } catch (Throwable t) {
            return "免责声明读取失败：" + t;
        }
        if (!I18n.english()) return full;
        // 英文界面只展示文件末尾的 English Summary 段
        int idx = full.indexOf("English Summary");
        if (idx < 0) return full;
        return full.substring(idx + "English Summary".length()).trim();
    }

    /** 首次启动（或免责声明版本变化）时展示摘要并要求确认 */
    private void maybeShowDisclaimer() {
        try {
            final android.content.SharedPreferences sp = Config.ui(this);
            if (sp.getInt("disclaimer_ver", 0) == DISCLAIMER_VER) return;
            String brief = readDisclaimerBrief();
            AlertDialog.Builder b = new AlertDialog.Builder(this)
                    .setTitle(T("免责声明"))
                    .setMessage(brief)
                    .setPositiveButton(T("我已阅读"), (d, w) ->
                            sp.edit().putInt("disclaimer_ver", DISCLAIMER_VER).apply())
                    .setNeutralButton(T("查看完整条款"), (d, w) -> {
                        sp.edit().putInt("disclaimer_ver", DISCLAIMER_VER).apply();
                        showDisclaimer();
                    })
                    .setCancelable(false);
            showStyled(b);
        } catch (Throwable ignored) {
        }
    }

    /** 首启弹窗的摘要：单独一份文件，避免按字符截断句子 */
    private String readDisclaimerBrief() {
        try {
            java.io.InputStream is = getAssets().open("DISCLAIMER_BRIEF.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            br.close();
            String text = sb.toString().trim();
            int idx = text.indexOf("English Summary");
            if (idx < 0) return text;
            // 中文摘要不带英文段，英文界面只取 English Summary 之后
            if (I18n.english()) return text.substring(idx + "English Summary".length()).trim();
            return text.substring(0, idx).trim();
        } catch (Throwable t) {
            return readDisclaimerText();
        }
    }

    private void showDisclaimer() {
        TextView tv = new TextView(this);
        tv.setText(styleDisclaimer(readDisclaimerText()));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setTextIsSelectable(true);
        tv.setLineSpacing(0f, 1.25f);
        tv.setTextColor(TEXT);
        int padH = dp(18);
        int padV = dp(10);
        tv.setPadding(padH, padV, padH, padV);

        ScrollView sv = new ScrollView(this);
        sv.addView(tv);

        // 正文首行就是「上报行为拦截（HyperPGuard）免责声明」，再加 setTitle 会重复一行
        showStyled(new AlertDialog.Builder(this)
                .setView(sv)
                .setPositiveButton(T("我知道了"), null));
    }

    /**
     * 免责声明排版：标题、版本行、章节、列表项分别上样式。
     * 文本已按段一行，列表项用 LeadingMarginSpan，折行后的行也保持缩进。
     */
    private CharSequence styleDisclaimer(String text) {
        android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder();
        boolean en = I18n.english();
        boolean first = true;
        for (String raw : text.split("\n", -1)) {
            String line = raw.trim();
            if (line.isEmpty()) {
                if (sb.length() == 0) continue;
                if (sb.length() >= 2 && sb.charAt(sb.length() - 2) == '\n') continue;
                sb.append('\n');
                continue;
            }
            int start = sb.length();
            sb.append(line);
            int end = sb.length();
            sb.append('\n');
            if (first) {
                if (!en) {
                    sb.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), start, end,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.setSpan(new android.text.style.RelativeSizeSpan(1.15f), start, end,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                first = false;
            } else if (!en && line.startsWith("版本 ")) {
                sb.setSpan(new android.text.style.RelativeSizeSpan(0.85f), start, end,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new android.text.style.ForegroundColorSpan(SUB), start, end,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (!en && line.matches("^[一二三四五六七八九十]+、.*")) {
                sb.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), start, end,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (line.matches("^\\d+\\)\\s.*") || (en && line.startsWith("- "))) {
                sb.setSpan(new android.text.style.LeadingMarginSpan.Standard(0, dp(18)),
                        start, end + 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return sb;
    }

    /** 按下时的高亮底色：浅色模式比背景深，深色模式比背景浅 */


    /** 对话框跟随深浅色：圆角卡片底、按钮强调色、正文与附注分色 */
    private void styleDialog(AlertDialog d) {
        if (d == null || d.getWindow() == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(20));
        d.getWindow().setBackgroundDrawable(bg);
        paintDialogText(d.getWindow().getDecorView());
    }

    private void paintDialogText(android.view.View v) {
        if (v == null) return;
        if (v instanceof EditText) {
            EditText et = (EditText) v;
            et.setTextColor(TEXT);
            et.setHintTextColor(SUB);
            GradientDrawable field = new GradientDrawable();
            field.setColor(nightMode ? 0xFF2C2C2E : 0xFFF2F3F5);
            field.setCornerRadius(dp(12));
            et.setBackground(field);
        } else if (v instanceof Button) {
            ((Button) v).setTextColor(ACCENT);
        } else if (v instanceof TextView) {
            TextView tv = (TextView) v;
            if (!"accent".equals(tv.getTag())) {
                float sp = tv.getTextSize() / getResources().getDisplayMetrics().scaledDensity;
                tv.setTextColor(sp <= 12.5f ? SUB : TEXT);
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) paintDialogText(g.getChildAt(i));
        }
    }

    private AlertDialog showStyled(AlertDialog.Builder b) {
        AlertDialog d = b.create();
        d.show();
        trackDialog(d);
        return d;
    }

    /** 记录当前对话框，切深浅色时可以就地重新着色 */
    private void trackDialog(AlertDialog d) {
        shownDialog = d;
        d.setOnDismissListener(x -> {
            if (shownDialog == d) shownDialog = null;
        });
        styleDialog(d);
    }

    /** 底栏图标：房子 / 拼图块 / 圆圈感叹号，纯 Canvas 绘制，不依赖 drawable 资源 */
    /**
     * 玻璃球：把底栏模糊图的对应区域画出来，再叠 10% 黑与一圈渐变高光。
     * 对应 KernelSU 的 drawBackdrop(shape = pillShape) + onDrawSurface(drawRect)。
     * 源码里那个胶囊是有实感的玻璃，因为背后内容被折射进来；纯色块做不到。
     */
    int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}

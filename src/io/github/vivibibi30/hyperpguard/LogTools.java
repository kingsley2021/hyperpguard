package io.github.vivibibi30.hyperpguard;

/**
 * 日志解析：拦截统计、分段与域名用途注解。纯函数，不依赖 Activity。
 */
final class LogTools {

    private LogTools() {
    }

    /** 每段最多显示的行数 */
    private static final int LOG_SECTION_LIMIT = 20;

    static String blockStats(String log) {
        java.util.Map<String, Integer> hosts = new java.util.LinkedHashMap<>();
        int miss = 0;
        for (String l : log.split("\n")) {
            int i = l.indexOf("blocked by HyperPGuard: ");
            if (i >= 0) {
                String h = l.substring(i + 25).trim();
                int sp = h.indexOf(' ');
                if (sp > 0) h = h.substring(0, sp);
                Integer c = hosts.get(h);
                hosts.put(h, c == null ? 1 : c + 1);
            } else if (l.contains("hook miss")) {
                miss++;
            }
        }
        if (hosts.isEmpty() && miss == 0) return "";
        StringBuilder out = new StringBuilder("拦截统计（当前日志范围）：\n");
        java.util.List<java.util.Map.Entry<String, Integer>> list =
                new java.util.ArrayList<>(hosts.entrySet());
        list.sort((a, b) -> b.getValue() - a.getValue());
        int n = 0;
        for (java.util.Map.Entry<String, Integer> e : list) {
            if (n++ >= 8) break;
            out.append("· ").append(e.getKey()).append(" × ").append(e.getValue()).append('\n');
        }
        if (miss > 0) out.append("hook miss：").append(miss).append(" 条\n");
        out.append('\n');
        return out.toString();
    }
    static String classify(String log) {
        java.util.List<String> blocked = new java.util.ArrayList<>();
        java.util.List<String> passed = new java.util.ArrayList<>();
        java.util.List<String> other = new java.util.ArrayList<>();
        for (String line : log.split("\n")) {
            if (line.trim().isEmpty()) continue;
            if (line.contains("blocked by HyperPGuard")
                    || line.contains(" X(host") || line.contains(" X(strict)")) {
                blocked.add(line);
            } else if (line.contains(": ? ")) {
                passed.add(line);
            } else {
                other.add(line);
            }
        }
        if (blocked.isEmpty() && passed.isEmpty()) return log;
        StringBuilder sb = new StringBuilder();
        sb.append(section("未拦截", passed,
                "未拦截指该域名不在当前规则里，不是拦截失败。\n"));
        sb.append(section("已拦截", blocked, null));
        sb.append(section("模块日志", other, null));
        return sb.toString();
    }
    static String hintFor(String line) {
        int i = line.indexOf(": ? ");
        if (i < 0) return null;
        String rest = line.substring(i + 4).trim();
        int sp = rest.indexOf(' ');
        if (sp < 0) return null;
        String target = rest.substring(sp + 1).trim();
        int slash = target.indexOf('/');
        String host = (slash > 0 ? target.substring(0, slash) : target).toLowerCase();
        for (String[] h : HOST_HINTS) {
            if (host.contains(h[0])) return h[1];
        }
        return null;
    }
    static String section(String name, java.util.List<String> lines, String note) {
        if (lines.isEmpty()) return "";
        int total = lines.size();
        int from = Math.max(0, total - LOG_SECTION_LIMIT);
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("（共 ").append(total).append(" 条");
        if (total > LOG_SECTION_LIMIT) sb.append("，显示最近 ").append(LOG_SECTION_LIMIT).append(" 条");
        sb.append("）\n");
        if (note != null) sb.append(note);
        for (int i = from; i < total; i++) {
            String line = lines.get(i);
            String hint = note == null ? null : hintFor(line);
            sb.append(hint == null ? line : line + "   ← " + hint).append('\n');
        }
        sb.append('\n');
        return sb.toString();
    }
    /** 常见上报域名的用途，用于给「未拦截」行加注 */
    private static final String[][] HOST_HINTS = {
            {"tracking.miui.com", "统计埋点"},
            {"msg.xiaomi.net", "推送接入调度"},
            {"adv.sec.miui.com", "广告位"},
            {"logupdate.avlyun", "病毒引擎日志"},
            {"rcpc-sdk", "腾讯 RCPC SDK"},
            {"net-assistant", "网络助手"},
            {"game.sec.miui.com", "游戏加速"},
            {"pre-api.miui.security", "游戏加速用户信息"},
            {"flash.sec.miui.com", "安全检测接口"},
            {"api.sec.miui.com", "安全中心接口"},
            {"data.sec.miui.com", "月报与配置上报"},
            {"framework.sec.miui.com", "XSOF 鉴权"},
            {"virus-engine", "病毒库云查"},
            {"deviceapi.micloud", "设备证书/查找设备"},
            {"find.api.micloud", "查找设备"},
            {"zzhc.vnet.cn", "运营商注册"},
            {"typdm.tydevice.com", "CTEI 明文上报"},
            {"fxltsbl.com", "移动 DM 上报"},
            {"dm.wo.com.cn", "运营商遥测"},
            {"xiaomimobile.com", "运营商合作服务"},
            {"m.qq.com", "腾讯 SDK"},
    };



}

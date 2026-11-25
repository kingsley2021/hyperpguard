package io.github.vivibibi30.hyperpguard;

import android.app.AlertDialog;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 配置导入导出：从 MainActivity 拆出。
 * 读取 Settings.Global 的配置串 + 作用域清单导出到 Download；从剪贴板导入并热重载。
 * 负责：配置文本的导出与导入校验。不负责：配置存储（Config）与界面布局。
 */
final class ConfigIO {

    private ConfigIO() {
    }

    static void show(MainActivity a, UiKit kit) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(kit.dp(20), kit.dp(8), kit.dp(20), kit.dp(8));
        TextView tip = new TextView(a);
        tip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tip.setTextColor(kit.sub());
        tip.setText("导出：把当前开关与作用域清单写成文本存到 Download。\n"
                + "导入：从剪贴板读取同一格式，校验后写回并热重载。\n"
                + "文本是明文，含自定义规则里的域名，分享前注意。");
        box.addView(tip);
        TextView exp = new TextView(a);
        exp.setText("导出到 Download");
        exp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        exp.setTextColor(kit.accent());
        exp.setPadding(0, kit.dp(14), 0, kit.dp(14));
        exp.setOnClickListener(v -> export(a));
        box.addView(exp);
        TextView imp = new TextView(a);
        imp.setText("从剪贴板导入");
        imp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        imp.setTextColor(kit.accent());
        imp.setPadding(0, kit.dp(4), 0, kit.dp(14));
        imp.setOnClickListener(v -> importFromClipboard(a));
        box.addView(imp);
        new AlertDialog.Builder(a)
                .setTitle("配置导入导出")
                .setView(box)
                .setNegativeButton("关闭", null)
                .show();
    }

    private static void export(MainActivity a) {
        new Thread(() -> {
            String raw = "";
            try {
                raw = android.provider.Settings.Global.getString(
                        a.getContentResolver(), Config.GLOBAL_KEY);
            } catch (Throwable ignored) {
            }
            java.util.List<String> scope = a.safeScope();
            String content = "# HyperPGuard " + Config.VERSION + " 配置备份\n"
                    + "# 作用域：" + (scope == null ? "读取失败" : scope.toString()) + "\n"
                    + (raw == null ? "" : raw) + "\n";
            String b64 = android.util.Base64.encodeToString(
                    content.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    android.util.Base64.NO_WRAP);
            String path = "/sdcard/Download/hyperpguard-" + Config.VERSION + "-config.txt";
            String out = Shell.out("echo " + Shell.q(b64) + " | base64 -d > " + Shell.q(path)
                    + " && chmod 644 " + Shell.q(path) + " && echo ok");
            final boolean ok = out.contains("ok");
            a.ui.post(() -> Toast.makeText(a,
                    ok ? "已导出到 " + path : "导出失败：" + out.trim(),
                    Toast.LENGTH_LONG).show());
        }, "hyperpguard-export").start();
    }

    private static void importFromClipboard(MainActivity a) {
        final CharSequence cs = clipboard(a);
        if (cs == null || cs.length() == 0) {
            Toast.makeText(a, "剪贴板是空的", Toast.LENGTH_SHORT).show();
            return;
        }
        String raw = null;
        for (String l : cs.toString().split("\n")) {
            String t = l.trim();
            if (t.startsWith("#") || !t.contains("=")) continue;
            raw = t;
        }
        if (raw == null) {
            Toast.makeText(a, "没有找到 key=value 配置行", Toast.LENGTH_SHORT).show();
            return;
        }
        int applied = 0;
        for (String kv : raw.split("\\|")) {
            int i = kv.indexOf('=');
            if (i <= 0) continue;
            String k = kv.substring(0, i).trim();
            String v = kv.substring(i + 1).trim();
            if (Config.K_CUSTOM.equals(k)) {
                Config.setCustomRules(a, v);
                applied++;
            } else {
                Config.uiSet(a, k, "true".equals(v));
                applied++;
            }
        }
        Toast.makeText(a, "已导入 " + applied + " 项，正在热重载", Toast.LENGTH_SHORT).show();
        a.scheduleHotReload(null);
    }

    private static CharSequence clipboard(MainActivity a) {
        try {
            android.content.ClipboardManager cm =
                    (android.content.ClipboardManager) a.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            if (cm == null || cm.getPrimaryClip() == null
                    || cm.getPrimaryClip().getItemCount() == 0) return null;
            return cm.getPrimaryClip().getItemAt(0).coerceToText(a);
        } catch (Throwable t) {
            return null;
        }
    }
}

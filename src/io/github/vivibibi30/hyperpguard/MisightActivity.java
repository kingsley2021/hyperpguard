package io.github.vivibibi30.hyperpguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 被采集数据（MiSight 事件库只读浏览）。
 *
 * 只读：通过 su 复制一份快照到 cache 再打开，不改系统库、不留副本。
 * 内容：汇总（事件数 / 累计次数 / 上传记录 / 最近时间）+ 按次数排序的事件与参数。
 */
public final class MisightActivity extends Activity {

    private boolean night;
    private int BG;
    private int CARD;
    private int TEXT;
    private int SUB;
    private int ACCENT;
    private int DIVIDER;

    private LinearLayout content;
    private TextView hint;
    private TextView refresh;
    private long seq;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        night = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        paint();
        setContentView(build());
        reload();
    }

    private void paint() {
        if (night) {
            BG = 0xFF000000;
            CARD = 0xFF1C1C1E;
            TEXT = 0xFFFFFFFF;
            SUB = 0xFF8E8E93;
            ACCENT = 0xFF7FB2FF;
            DIVIDER = 0x1FFFFFFF;
        } else {
            BG = 0xFFF2F3F5;
            CARD = 0xFFFFFFFF;
            TEXT = 0xFF111111;
            SUB = 0xFF8A8A8E;
            ACCENT = 0xFF3482FF;
            DIVIDER = 0x14000000;
        }
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(
                night ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private TextView text(String s, int sizeSp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        v.setTextColor(color);
        if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD);
        return v;
    }

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(8), dp(16), dp(8));
        header.setBackgroundColor(BG);

        TextView back = text("←", 20, TEXT, false);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));

        TextView title = text("被采集数据", 20, TEXT, true);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = dp(4);
        header.addView(title, tlp);

        refresh = text("刷新", 14, ACCENT, false);
        refresh.setPadding(dp(8), dp(8), dp(8), dp(8));
        refresh.setOnClickListener(v -> reload());
        header.addView(refresh);
        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(4), dp(16), dp(24));
        scroll.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(20));
        c.setBackground(bg);
        c.setPadding(dp(16), dp(6), dp(16), dp(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        c.setLayoutParams(lp);
        return c;
    }

    private View row(String label, String value, int valueColor) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, dp(11), 0, dp(11));
        TextView l = text(label, 15, TEXT, false);
        r.addView(l, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView v = text(value, 14, valueColor, false);
        v.setMaxLines(2);
        v.setEllipsize(TextUtils.TruncateAt.END);
        v.setGravity(Gravity.END);
        r.addView(v);
        return r;
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(DIVIDER);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Math.max(1, (int) (0.6f * getResources().getDisplayMetrics().density))));
        return v;
    }

    private void reload() {
        final long my = ++seq;
        refresh.setEnabled(false);
        content.removeAllViews();
        LinearLayout loading = card();
        ProgressBar bar = new ProgressBar(this);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(28), dp(28));
        blp.topMargin = dp(14);
        blp.bottomMargin = dp(14);
        bar.setLayoutParams(blp);
        loading.addView(bar);
        hint = text("正在读取事件库…首次会请求 root 授权", 13, SUB, false);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.bottomMargin = dp(12);
        hint.setLayoutParams(hlp);
        loading.addView(hint);
        content.addView(loading);

        new Thread(() -> {
            final Misight.Snapshot s = Misight.load(this);
            runOnUiThread(() -> {
                if (my != seq || isFinishing()) return;
                refresh.setEnabled(true);
                render(s);
            });
        }, "misight-load").start();
    }

    private void render(Misight.Snapshot s) {
        content.removeAllViews();
        if (!s.ok) {
            LinearLayout failCard = card();
            failCard.addView(row("读取失败", "看下面原因", SUB));
            failCard.addView(divider());
            TextView e = text(s.error == null ? "未知错误" : s.error, 13, SUB, false);
            e.setPadding(0, dp(12), 0, dp(12));
            failCard.addView(e);
            content.addView(failCard);

            LinearLayout note = card();
            TextView n = text("本页只读：通过 su 把 /data/miuilog/misight/event.db 复制一份到应用缓存，"
                    + "查询完即删除，不会改动系统库。没有 root 或拒绝授权时读不到。", 13, SUB, false);
            n.setPadding(0, dp(12), 0, dp(12));
            note.addView(n);
            content.addView(note);
            return;
        }

        LinearLayout summary = card();
        summary.addView(row("事件记录", s.records + " 条", TEXT));
        summary.addView(divider());
        summary.addView(row("累计上报次数", s.hits + " 次", TEXT));
        summary.addView(divider());
        summary.addView(row("上传记录", s.uploads == 0 ? "0 条（未出本机）" : s.uploads + " 条",
                s.uploads == 0 ? TEXT : ACCENT));
        summary.addView(divider());
        summary.addView(row("最近一次", time(s.lastTime), TEXT));
        summary.addView(divider());
        summary.addView(row("库版本 / 快照大小", (s.version.isEmpty() ? "?" : s.version)
                + " / " + String.format(Locale.US, "%.1f MB", s.bytes / 1048576.0), SUB));
        content.addView(summary);

        LinearLayout listCard = card();
        TextView head = text("按次数排序（点条目看参数）", 13, SUB, false);
        head.setPadding(0, dp(10), 0, dp(8));
        listCard.addView(head);
        if (s.top.isEmpty()) {
            TextView empty = text("库里没有事件记录", 14, SUB, false);
            empty.setPadding(0, dp(10), 0, dp(14));
            listCard.addView(empty);
        }
        for (int i = 0; i < s.top.size(); i++) {
            final Misight.Event e = s.top.get(i);
            if (i > 0) listCard.addView(divider());
            listCard.addView(eventRow(e));
        }
        content.addView(listCard);

        LinearLayout note = card();
        TextView n = text("只读快照，来源 /data/miuilog/misight/event.db。"
                + "「事件」是系统埋点的编号，参数是该事件最近一次上报时带的字段；"
                + "「上传记录」为空说明还没有数据被送出本机。", 13, SUB, false);
        n.setPadding(0, dp(12), 0, dp(12));
        note.addView(n);
        content.addView(note);
    }

    private View eventRow(final Misight.Event e) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(0, dp(10), 0, dp(10));

        LinearLayout line1 = new LinearLayout(this);
        line1.setOrientation(LinearLayout.HORIZONTAL);
        line1.setGravity(Gravity.CENTER_VERTICAL);
        TextView id = text("事件 " + e.id, 15, TEXT, true);
        line1.addView(id, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView cnt = text("×" + e.count, 14, ACCENT, true);
        line1.addView(cnt);
        r.addView(line1);

        String summary = summarize(e.sample);
        TextView sub = text(time(e.last) + (summary.isEmpty() ? "" : "　" + summary),
                12, SUB, false);
        sub.setMaxLines(2);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(3);
        r.addView(sub, slp);

        r.setOnClickListener(v -> showSample(e));
        return r;
    }

    private void showSample(Misight.Event e) {
        StringBuilder sb = new StringBuilder();
        sb.append("首次：").append(time(e.first)).append('\n');
        sb.append("最近：").append(time(e.last)).append('\n');
        sb.append("次数：").append(e.count).append("\n\n");
        sb.append("参数：\n").append(e.sample == null || e.sample.isEmpty()
                ? "（无）" : e.sample);

        ScrollView sc = new ScrollView(this);
        TextView tv = text(sb.toString(), 13, TEXT, false);
        tv.setTextIsSelectable(true);
        tv.setPadding(dp(20), dp(8), dp(20), dp(8));
        sc.addView(tv);
        new AlertDialog.Builder(this)
                .setTitle("事件 " + e.id)
                .setView(sc)
                .setPositiveButton("关闭", null)
                .show();
    }

    /** 参数 JSON 压成一行摘要，避免列表被长字段撑开 */
    private String summarize(String sample) {
        if (sample == null || sample.isEmpty()) return "";
        String s = sample.replace('\n', ' ').trim();
        s = s.replaceAll("\\s+", " ");
        return s.length() > 90 ? s.substring(0, 90) + "…" : s;
    }

    private String time(long sec) {
        if (sec <= 0) return "从未";
        return new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                .format(new Date(sec * 1000L));
    }
}

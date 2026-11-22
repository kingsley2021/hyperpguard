package io.github.vivibibi30.hyperpguard;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * 界面组件工厂：只依赖 Context 与颜色，不反向依赖 MainActivity。
 * 改卡片圆角/内边距/分隔线/按压效果只动这个文件。
 */
final class UiKit {
    private final Context c;
    private final Theme t;

    UiKit(Context c, Theme theme) {
        this.c = c;
        this.t = theme;
    }

    int sub() {
        return t.sub;
    }

    int accent() {
        return t.accent;
    }

    /** 标签 + 右侧灰色值，不可点 */
    ViewGroup infoRow(String label, String value) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(56));
        row.setPadding(dp(4), dp(9), dp(4), dp(9));
        android.widget.TextView name = new android.widget.TextView(c);
        name.setText(label);
        name.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        name.setTextColor(t.text);
        name.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(name);
        android.widget.TextView val = new android.widget.TextView(c);
        val.setText(value);
        val.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
        val.setTextColor(t.sub);
        row.addView(val);
        return row;
    }

    /** 标签 + 右侧强调色链接，点击动作由调用方给 */
    ViewGroup linkRow(String label, String value, Runnable onClick) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(56));
        row.setClickable(true);
        row.setBackground(pressBackground());
        row.setPadding(dp(4), dp(9), dp(4), dp(9));
        android.widget.TextView name = new android.widget.TextView(c);
        name.setText(label);
        name.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        name.setTextColor(t.text);
        name.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(name);
        android.widget.TextView link = new android.widget.TextView(c);
        link.setText(value);
        link.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
        link.setTextColor(t.accent);
        row.addView(link);
        row.setOnClickListener(v -> onClick.run());
        return row;
    }

    /** 整行一个强调色按钮 */
    android.widget.TextView singleButton(String label, Runnable onClick) {
        android.widget.TextView b = new android.widget.TextView(c);
        b.setText(label);
        b.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        b.setTextColor(t.accent);
        b.setGravity(android.view.Gravity.CENTER);
        b.setBackground(pressBackground());
        b.setPadding(0, dp(12), 0, dp(12));
        b.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        b.setOnClickListener(v -> onClick.run());
        return b;
    }

    /** 列表行：16sp 标题 + 12sp 副标题 */
    void addListRow(LinearLayout parent, String title, String desc, Runnable onClick) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setMinimumHeight(dp(56));
        row.setClickable(true);
        row.setBackground(pressBackground());
        row.setPadding(dp(4), dp(9), dp(4), dp(9));

        android.widget.TextView t1 = new android.widget.TextView(c);
        t1.setText(title);
        t1.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);
        t1.setTextColor(t.text);

        android.widget.TextView t2 = new android.widget.TextView(c);
        t2.setText(desc);
        t2.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
        t2.setTextColor(t.sub);
        t2.setPadding(0, dp(2), 0, 0);

        row.addView(t1);
        row.addView(t2);
        row.setOnClickListener(v -> onClick.run());
        parent.addView(row);
    }

    /** 分组标题：左边 14sp 标题，右边可选 12sp 说明 */
    View sectionRow(String title, String right) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.BOTTOM);
        row.setMinimumHeight(dp(35));
        row.setPadding(dp(12), dp(16), dp(12), dp(8));

        android.widget.TextView left = new android.widget.TextView(c);
        left.setText(title);
        left.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        left.setTextColor(t.sub);
        row.addView(left, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (right != null && !right.isEmpty()) {
            android.widget.TextView r = new android.widget.TextView(c);
            r.setText(right);
            r.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
            r.setTextColor(t.sub);
            r.setMaxLines(1);
            r.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.addView(r);
        }
        return row;
    }

    android.widget.TextView sectionTitle(String text) {
        android.widget.TextView tv = new android.widget.TextView(c);
        tv.setText(text);
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setTextColor(t.sub);
        tv.setMinHeight(dp(35));
        tv.setGravity(android.view.Gravity.BOTTOM);
        tv.setPadding(dp(12), dp(16), 0, dp(8));
        return tv;
    }

    int dp(int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    LinearLayout card() {
        LinearLayout c = new LinearLayout(this.c);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(t.card);
        bg.setCornerRadius(dp(20));
        c.setBackground(bg);
        c.setPadding(dp(16), dp(4), dp(16), dp(4));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        c.setLayoutParams(lp);
        return c;
    }

    View divider() {
        View v = new View(c);
        v.setBackgroundColor(t.line);
        int h = Math.max(1, Math.round(c.getResources().getDisplayMetrics().density * 0.75f));
        v.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, h));
        return v;
    }

    ViewGroup line() {
        LinearLayout l = new LinearLayout(c);
        l.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(t.line);
        l.setBackground(bg);
        return l;
    }

    Drawable pressBackground() {
        int pressed = t.night ? 0xFF3A3A3C : 0xFFE5E5E5;
        GradientDrawable on = new GradientDrawable();
        on.setColor(pressed);
        on.setCornerRadius(dp(12));
        GradientDrawable off = new GradientDrawable();
        off.setColor(0x00000000);
        StateListDrawable d = new StateListDrawable();
        d.addState(new int[]{android.R.attr.state_pressed}, on);
        d.addState(new int[]{}, off);
        return d;
    }
}

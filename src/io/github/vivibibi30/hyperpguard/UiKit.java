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
    private final int cardColor;
    private final int lineColor;
    private final boolean night;
    private final int subColor;

    UiKit(Context c, int cardColor, int lineColor, int subColor, boolean night) {
        this.c = c;
        this.cardColor = cardColor;
        this.lineColor = lineColor;
        this.subColor = subColor;
        this.night = night;
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
        left.setTextColor(subColor);
        row.addView(left, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (right != null && !right.isEmpty()) {
            android.widget.TextView r = new android.widget.TextView(c);
            r.setText(right);
            r.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
            r.setTextColor(subColor);
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
        tv.setTextColor(subColor);
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
        bg.setColor(cardColor);
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
        v.setBackgroundColor(lineColor);
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
        bg.setColor(lineColor);
        l.setBackground(bg);
        return l;
    }

    Drawable pressBackground() {
        int pressed = night ? 0xFF3A3A3C : 0xFFE5E5E5;
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

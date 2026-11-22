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

    UiKit(Context c, int cardColor, int lineColor, boolean night) {
        this.c = c;
        this.cardColor = cardColor;
        this.lineColor = lineColor;
        this.night = night;
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

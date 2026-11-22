package io.github.vivibibi30.hyperpguard;

/** 主题色板：UI 组件只依赖它，不依赖 Activity。 */
final class Theme {
    final int bg;
    final int card;
    final int line;
    final int text;
    final int sub;
    final int accent;
    final boolean night;

    Theme(int bg, int card, int line, int text, int sub, int accent, boolean night) {
        this.bg = bg;
        this.card = card;
        this.line = line;
        this.text = text;
        this.sub = sub;
        this.accent = accent;
        this.night = night;
    }
}

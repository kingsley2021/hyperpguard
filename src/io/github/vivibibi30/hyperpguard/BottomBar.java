package io.github.vivibibi30.hyperpguard;

import java.util.ArrayList;
import android.view.ViewGroup;
import android.view.Gravity;
import android.view.View;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;
import android.widget.FrameLayout;

/**
 * 底栏的三个自绘 View：玻璃球（GlassPill）、高光描边（GlossView）、tab 图标（TabIcon）。
 *
 * 从 MainActivity 拆出来，改底栏只动这个文件；回滚也可以整文件替换。
 */
    final class GlassPill extends android.view.View {
        private boolean night;
        private android.graphics.BitmapShader shader;
        private float srcOffsetX;
        private float srcWidth = 1f;
        private final android.graphics.Matrix matrix = new android.graphics.Matrix();
        private final android.graphics.Paint fill =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Paint edge =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);

        GlassPill(android.content.Context c) {
            super(c);
        }

        void setNight(boolean n) {
            if (night == n) return;
            night = n;
            invalidate();
        }

        void setBackdrop(android.graphics.Bitmap b) {
            if (b == null) {
                shader = null;
                fill.setShader(null);
            } else {
                srcWidth = Math.max(1f, b.getWidth());
                shader = new android.graphics.BitmapShader(
                        b, android.graphics.Shader.TileMode.CLAMP,
                        android.graphics.Shader.TileMode.CLAMP);
                fill.setShader(shader);
            }
            invalidate();
        }

        void setSrcOffsetX(float x) {
            if (Math.abs(x - srcOffsetX) < 0.01f) return;
            srcOffsetX = x;
            invalidate();
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            if (w <= 0 || h <= 0) return;
            float r = h / 2f;

            if (shader != null) {
                // 位图是整条底栏的模糊结果，偏移到胶囊这一格
                matrix.setTranslate(-srcOffsetX, 0f);
                shader.setLocalMatrix(matrix);
                canvas.drawRoundRect(0f, 0f, w, h, r, r, fill);
            }

            fill.setShader(null);
            // 源码 onDrawSurface：浅色 Black.copy(0.1f)，深色 White.copy(0.1f)
            fill.setColor(night ? 0x1AFFFFFF : 0x1A000000);
            canvas.drawRoundRect(0f, 0f, w, h, r, r, fill);

            float stroke = Math.max(1f, getResources().getDisplayMetrics().density);
            edge.setStyle(android.graphics.Paint.Style.STROKE);
            edge.setStrokeWidth(stroke);
            edge.setShader(new android.graphics.LinearGradient(0f, 0f, 0f, h,
                    new int[]{0x99FFFFFF, 0x26FFFFFF, 0x0DFFFFFF, 0x66FFFFFF},
                    new float[]{0f, 0.3f, 0.7f, 1f},
                    android.graphics.Shader.TileMode.CLAMP));
            float inset = stroke / 2f;
            canvas.drawRoundRect(inset, inset, w - inset, h - inset,
                    Math.max(0f, r - inset), Math.max(0f, r - inset), edge);
        }
    }

    /** 玻璃高光的描边：上沿白、下沿淡，模拟液态玻璃折射出的亮边 */
    final class GlossView extends android.view.View {
        private final android.graphics.Paint paint =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final float radius;
        private final float stroke;

        GlossView(android.content.Context c, float radius, float stroke) {
            super(c);
            this.radius = radius;
            this.stroke = stroke;
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            if (w <= 0 || h <= 0) return;
            float inset = stroke / 2f;
            paint.setStyle(android.graphics.Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setShader(new android.graphics.LinearGradient(0, 0, 0, h,
                    new int[]{0x8CFFFFFF, 0x33FFFFFF, 0x14FFFFFF, 0x4DFFFFFF},
                    new float[]{0f, 0.32f, 0.72f, 1f},
                    android.graphics.Shader.TileMode.CLAMP));
            float r = Math.max(0f, radius - inset);
            canvas.drawRoundRect(inset, inset, w - inset, h - inset, r, r, paint);
            // 内阴影：上沿内侧提亮、下沿内侧压暗，白底上也能看出玻璃厚度
            paint.setStyle(android.graphics.Paint.Style.FILL);
            paint.setShader(new android.graphics.LinearGradient(0f, 0f, 0f, h,
                    new int[]{0x26FFFFFF, 0x00FFFFFF, 0x00000000, 0x1A000000},
                    new float[]{0f, 0.28f, 0.62f, 1f},
                    android.graphics.Shader.TileMode.CLAMP));
            canvas.drawRoundRect(inset, inset, w - inset, h - inset,
                    Math.max(0f, r - inset), Math.max(0f, r - inset), paint);
        }
    }

    /** 纯 Canvas 绘制图标：选中实心（Filled）、未选中描边（Outlined），对齐 Material Symbols */
    final class TabIcon extends android.view.View {
        private final int kind;
        private int color = 0xFF8A8A8E;
        private boolean filled;
        private final android.graphics.Paint paint =
                new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);

        TabIcon(android.content.Context c, int kind) {
            super(c);
            this.kind = kind;
        }

        void setIconColor(int c) {
            color = c;
            invalidate();
        }

        @Override
        public void setSelected(boolean selected) {
            super.setSelected(selected);
            if (filled != selected) {
                filled = selected;
                invalidate();
            }
        }

        @Override
        protected void onDraw(android.graphics.Canvas canvas) {
            float s = Math.min(getWidth(), getHeight());
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            paint.setColor(color);
            paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);
            paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);

            if (kind == 0) {
                // Material Home：24 网格路径，带门洞的房子
                float k = s / 24f * 0.86f;
                android.graphics.Path p = new android.graphics.Path();
                p.moveTo(cx + (10f - 12f) * k, cy + (20f - 12f) * k);
                p.lineTo(cx + (10f - 12f) * k, cy + (14f - 12f) * k);
                p.lineTo(cx + (14f - 12f) * k, cy + (14f - 12f) * k);
                p.lineTo(cx + (14f - 12f) * k, cy + (20f - 12f) * k);
                p.lineTo(cx + (19f - 12f) * k, cy + (20f - 12f) * k);
                p.lineTo(cx + (19f - 12f) * k, cy + (12f - 12f) * k);
                p.lineTo(cx + (22f - 12f) * k, cy + (12f - 12f) * k);
                p.lineTo(cx + (12f - 12f) * k, cy + (3f - 12f) * k);
                p.lineTo(cx + (2f - 12f) * k, cy + (12f - 12f) * k);
                p.lineTo(cx + (5f - 12f) * k, cy + (12f - 12f) * k);
                p.lineTo(cx + (5f - 12f) * k, cy + (20f - 12f) * k);
                p.close();
                if (filled) {
                    paint.setStyle(android.graphics.Paint.Style.FILL);
                } else {
                    paint.setStyle(android.graphics.Paint.Style.STROKE);
                    paint.setStrokeWidth(s * 0.075f);
                }
                canvas.drawPath(p, paint);
            } else if (kind == 1) {
                // Material Extension：圆角主体 − 顶部圆 + 右侧圆，用 Path.op 合并成连续轮廓
                /* 主体半宽 0.30s + 凸圆半径 0.15s + 描边半宽 0.04s = 0.49s，
                 * 完整落在 24dp 视图里；原来的 0.32 + 0.16 + 0.04 = 0.52s 会超出自身边界，
                 * 右侧凸圆被裁掉一条。 */
                float left = cx - s * 0.30f;
                float top = cy - s * 0.30f;
                float right = cx + s * 0.30f;
                float bottom = cy + s * 0.30f;
                float n = s * 0.15f;
                float cr = s * 0.07f;
                android.graphics.Path body = new android.graphics.Path();
                body.addRoundRect(new android.graphics.RectF(left, top, right, bottom),
                        cr, cr, android.graphics.Path.Direction.CW);
                android.graphics.Path notch = new android.graphics.Path();
                notch.addCircle(cx, top, n, android.graphics.Path.Direction.CW);
                body.op(notch, android.graphics.Path.Op.DIFFERENCE);
                android.graphics.Path bump = new android.graphics.Path();
                bump.addCircle(right, cy, n, android.graphics.Path.Direction.CW);
                body.op(bump, android.graphics.Path.Op.UNION);
                if (filled) {
                    paint.setStyle(android.graphics.Paint.Style.FILL);
                } else {
                    paint.setStyle(android.graphics.Paint.Style.STROKE);
                    paint.setStrokeWidth(s * 0.08f);
                }
                canvas.drawPath(body, paint);
            } else {
                // 齿轮：8 个齿 + 中心孔
                float R = s * 0.34f;
                float r0 = s * 0.24f;
                float hole = s * 0.10f;
                android.graphics.Path gear = new android.graphics.Path();
                gear.addCircle(cx, cy, r0, android.graphics.Path.Direction.CW);
                for (int i2 = 0; i2 < 8; i2++) {
                    android.graphics.Path tooth = new android.graphics.Path();
                    tooth.addRoundRect(new android.graphics.RectF(
                            cx - s * 0.075f, cy - R,
                            cx + s * 0.075f, cy - r0 + s * 0.02f),
                            s * 0.02f, s * 0.02f, android.graphics.Path.Direction.CW);
                    android.graphics.Matrix m = new android.graphics.Matrix();
                    m.setRotate(i2 * 45f, cx, cy);
                    tooth.transform(m);
                    gear.op(tooth, android.graphics.Path.Op.UNION);
                }
                android.graphics.Path holePath = new android.graphics.Path();
                holePath.addCircle(cx, cy, hole, android.graphics.Path.Direction.CW);
                gear.op(holePath, android.graphics.Path.Op.DIFFERENCE);
                if (filled) {
                    paint.setStyle(android.graphics.Paint.Style.FILL);
                } else {
                    paint.setStyle(android.graphics.Paint.Style.STROKE);
                    paint.setStrokeWidth(s * 0.075f);
                }
                canvas.drawPath(gear, paint);
            }
        }
    }

    final class Stage extends FrameLayout {
        interface PageListener {
            void onPageSelected(int index);

            /** 页面滚动进度，单位是「第几页」的浮点值；拖动和松手吸附期间每帧回调 */
            void onPageScrolled(float progress);
        }

        private final android.view.VelocityTracker tracker;
        private final int touchSlop;
        private float downX, downY, lastX;
        private boolean dragging;
        private int pageCount = 1;
        private int currentPage;
        private float scroll;      // 内容水平偏移 = 当前页 * 宽 + 拖动量
        private int pageW;
        private PageListener listener;

        /* 吸附曲线照抄 miuix 的 PagerNavigationSpringSpec（KernelSU 的 fling snapAnimationSpec
         * 与 springAnimateToPage 共用同一条）：
         *   spring(stiffness = 322.2f,
         *          dampingRatio = 32.31f / (2f * sqrt(322.2f)),   // 约 0.9，欠阻尼带一点回弹
         *          visibilityThreshold = 0.5f)                     // 0.5px
         * 换成逐帧积分就是 k = 322.2、c = 32.31、停止阈值 0.5px。 */
        private static final float SPRING_K = 322.2f;
        private static final float SPRING_C = 32.31f;
        private static final float SPRING_STOP_PX = 0.5f;
        private static final float SPRING_STOP_V = 40f;

        private boolean springing;
        /* 对应 MainPagerState.isNavigating：底栏点击触发的页面动画期间，syncPage() 不生效。
         * 我们在动画期间直接不回报进度，否则进度前半段 round() 还是旧页，
         * 会把刚切好的选中态打回去，胶囊跟着往回弹一下。 */
        private boolean navigating;
        private float springVel;
        private long springLast;
        private float springTo;
        private int springPage;

        private final android.view.Choreographer.FrameCallback springTicker =
                new android.view.Choreographer.FrameCallback() {
                    @Override
                    public void doFrame(long frameTimeNanos) {
                        if (!springing) return;
                        if (springLast == 0L) springLast = frameTimeNanos - 16000000L;
                        float dt = (frameTimeNanos - springLast) / 1.0e9f;
                        springLast = frameTimeNanos;
                        if (dt > 0.032f) dt = 0.032f;
                        if (dt <= 0f) dt = 0.016f;

                        /* 显式欧拉在 c*dt > 1 时会放大速度（掉帧到 30fps 时 c*dt = 1.03），
                         * 表现为页面来回摆、底栏选中态跟着跳。固定 4ms 子步积分，
                         * 一帧拆成几段，c*step 只有 0.13，任何帧率都稳。 */
                        float left = dt;
                        while (left > 0f) {
                            float step = Math.min(0.004f, left);
                            float accel = (springTo - scroll) * SPRING_K - springVel * SPRING_C;
                            springVel += accel * step;
                            scroll += springVel * step;
                            left -= step;
                        }
                        // 首尾页：不允许弹到页面范围外，否则会露出空白底色
                        float maxScroll = (pageCount - 1) * (float) pageWidth();
                        if (scroll < 0f) {
                            scroll = 0f;
                            springVel *= -0.25f;
                        } else if (scroll > maxScroll) {
                            scroll = maxScroll;
                            springVel *= -0.25f;
                        }
                        setScroll(scroll);

                        if (Math.abs(springTo - scroll) < SPRING_STOP_PX
                                && Math.abs(springVel) < SPRING_STOP_V) {
                            springing = false;
                            springLast = 0L;
                            setScroll(springTo);        // 此时 navigating 仍为真，进度回报会被上层忽略
                            navigating = false;
                            if (listener != null) listener.onPageSelected(springPage);
                            return;
                        }
                        android.view.Choreographer.getInstance().postFrameCallback(this);
                    }
                };

        Stage(android.content.Context c) {
            super(c);
            tracker = android.view.VelocityTracker.obtain();
            touchSlop = android.view.ViewConfiguration.get(c).getScaledTouchSlop();
        }

        void setPageCount(int n) {
            pageCount = Math.max(1, n);
        }

        void setPageListener(PageListener l) {
            listener = l;
        }

        int pageWidth() {
            return pageW > 0 ? pageW
                    : getResources().getDisplayMetrics().widthPixels;
        }

        @Override
        protected void onMeasure(int wSpec, int hSpec) {
            int w = MeasureSpec.getSize(wSpec);
            int h = MeasureSpec.getSize(hSpec);
            pageW = w;
            setMeasuredDimension(w, h);
            for (int i = 0; i < getChildCount(); i++) {
                getChildAt(i).measure(
                        MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
            }
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            pageW = getWidth();
            for (int i = 0; i < getChildCount(); i++) {
                getChildAt(i).layout(0, 0, pageW, getHeight());
            }
            applyScroll();
        }

        private void applyScroll() {
            int w = pageWidth();
            for (int i = 0; i < getChildCount(); i++) {
                getChildAt(i).setTranslationX(i * w - scroll);
            }
        }

        private void setScroll(float v) {
            scroll = v;
            applyScroll();
            /* 页面每动一像素就把进度交给底栏，胶囊和页面同帧移动。
             * 原来只在吸附动画结束后 onPageSelected，胶囊要等页面停住才开始追，慢一整拍。 */
            if (listener != null) {
                listener.onPageScrolled(v / Math.max(1, pageWidth()));
            }
        }

        /** 底栏拖动驱动：progress ∈ [0, n-1]，页面 1:1 跟手 */
        void dragTo(float progress) {
            cancelAnim();
            setScroll(progress * pageWidth());
        }

        /** 重建界面时直接落到某一页，不播动画 */
        void resetTo(int page) {
            currentPage = Math.max(0, Math.min(pageCount - 1, page));
            cancelAnim();
            setScroll(currentPage * (float) pageWidth());
        }

        private void cancelAnim() {
            springing = false;
            navigating = false;
            springLast = 0L;
            springVel = 0f;
        }

        /**
         * 弹到某一页。velocityX 是 scroll 方向初速（px/s）：点底栏传 0，
         * 松手吸附传手指速度取负，和 Compose 的 fling snapAnimationSpec 一致。
         */
        void springToPage(int page, float velocityX) {
            startSpring(page, velocityX, false);
        }

        /** 底栏点击触发的翻页：无初速，且动画期间不回报进度 */
        void springToPageFromBar(int page) {
            startSpring(page, 0f, true);
        }

        boolean isNavigating() {
            return navigating;
        }

        private void startSpring(int page, float velocityX, boolean fromBar) {
            int target = Math.max(0, Math.min(pageCount - 1, page));
            currentPage = target;
            springTo = target * (float) pageWidth();
            springPage = target;
            springVel = velocityX;
            navigating = fromBar;
            if (!springing) {
                springing = true;
                springLast = 0L;
                android.view.Choreographer.getInstance().postFrameCallback(springTicker);
            }
        }

        @Override
        public boolean onInterceptTouchEvent(android.view.MotionEvent e) {
            switch (e.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    downX = e.getX();
                    downY = e.getY();
                    lastX = downX;
                    dragging = false;
                    tracker.clear();
                    tracker.addMovement(e);
                    return false;
                case android.view.MotionEvent.ACTION_MOVE: {
                    tracker.addMovement(e);
                    float dx = e.getX() - downX;
                    float dy = e.getY() - downY;
                    // 横向意图明显才接管：纵向滚动与页内点击都不受影响
                    if (!dragging && Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                        dragging = true;
                        lastX = e.getX();
                        cancelAnim();
                        android.view.ViewParent p = getParent();
                        if (p != null) p.requestDisallowInterceptTouchEvent(true);
                        return true;
                    }
                    return false;
                }
            }
            return false;
        }

        @Override
        public boolean onTouchEvent(android.view.MotionEvent e) {
            tracker.addMovement(e);
            switch (e.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    downX = e.getX();
                    downY = e.getY();
                    lastX = downX;
                    dragging = false;
                    cancelAnim();
                    return true;
                case android.view.MotionEvent.ACTION_MOVE: {
                    if (!dragging) {
                        float dx = e.getX() - downX;
                        float dy = e.getY() - downY;
                        if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
                            dragging = true;
                            lastX = e.getX();
                            cancelAnim();
                        } else {
                            return true;
                        }
                    }
                    float delta = lastX - e.getX();
                    lastX = e.getX();
                    float v = scroll + delta;
                    float max = (pageCount - 1) * (float) pageWidth();
                    if (v < 0f) v *= 0.35f;                       // 边界阻尼，拉不动但有回弹感
                    else if (v > max) v = max + (v - max) * 0.35f;
                    setScroll(v);
                    return true;
                }
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL: {
                    if (dragging) settle();
                    dragging = false;
                    tracker.clear();
                    return true;
                }
            }
            return true;
        }

        /** 松手吸附：先看速度，再看离哪一页近 */
        private void settle() {
            tracker.computeCurrentVelocity(1000);
            float vx = tracker.getXVelocity();
            int w = Math.max(1, pageWidth());
            float max = (pageCount - 1) * (float) w;
            if (scroll < 0f) {
                springToPage(0, -vx);
                return;
            }
            if (scroll > max) {
                springToPage(pageCount - 1, -vx);
                return;
            }
            int page;
            if (Math.abs(vx) > 700f) {
                page = vx < 0f ? (int) Math.ceil(scroll / w) : (int) Math.floor(scroll / w);
            } else {
                page = Math.round(scroll / w);
            }
            springToPage(Math.max(0, Math.min(pageCount - 1, page)), -vx);
        }
    }


/** 底栏构建与交互：胶囊弹簧、切页、玻璃刷新。改底栏只动这个文件。 */
/**
 * 负责：底栏结构、分页容器 Stage、胶囊弹簧与玻璃刷新。不负责：页面内容与开关状态（MainActivity / PageState）。
 */
final class BottomBarController {

    private final MainActivity host;
    private final Stage stage;
    private final java.util.List<android.widget.ScrollView> pageScrolls;

    private long glassLastAt;
    private boolean glassPending;
    private float barDownX;
    private float barDownY;
    private boolean barDragging;
    private boolean barTouching;

    BottomBarController(MainActivity host, Stage stage,
                        java.util.List<android.widget.ScrollView> pageScrolls) {
        this.host = host;
        this.stage = stage;
        this.pageScrolls = pageScrolls;
    }

    private final java.util.List<LinearLayout> tabViews = new ArrayList<>();
    private int currentTab = 0;
    private boolean tabUiReady;

    private GlassPill tabPill;
    private LinearLayout tabBar;
    private final java.util.List<TabIcon> tabIcons = new ArrayList<>();
    private final java.util.List<TextView> tabLabels = new ArrayList<>();
    private ImageView glassBackdrop;

    /* ───────── 胶囊驱动：照 DampedDragAnimation.kt 的物理模型 ─────────
     * 源码用的是
     *   valueAnimationSpec = spring(dampingRatio = 1f, stiffness = 1000f, minVisibleChange = 0.001f)
     * 每一个帧换算成积分就是 k = 1000、c = 2*sqrt(k) = 63.2（临界阻尼，不回过冲）。
     * value 的单位是"第几格"（0 .. n-1），不是像素，和源码的 valueRange 一致。
     * 按下、拖动、松手共用同一条曲线，任何一帧都不会被硬设覆盖。
     */
    private float pillValue;        // 当前格位（浮点）
    private float pillVelocity;     // 格/s
    private float pillTarget;       // 目标格位
    private float pressProgress;    // 0..1 按下进度
    private float pressVelocity;
    private float pressTarget;
    private long pillLastFrame;
    private boolean pillRunning;
    private boolean pressHold;      // 胶囊还在赶路，先别缩回（对应 release() 的等值逻辑）
    private static final float PILL_STIFFNESS = 1000f;
    private static final float PILL_DAMPING = 63.2f;      // 2*sqrt(1000)
    /* 按下放大倍率。KernelSU 源码写的是 78f/56f ≈ 1.393，但那是 56dp 胶囊在一块不裁剪的
     * Compose 容器里长到 78dp：上下各顶出 64dp 外壳 7dp、边缘格左右各顶出 20dp，
     * 靠不裁剪加 lens 折射高光才成立。这里是纯 Canvas，被父容器或屏幕边缘切一刀就是硬边。
     * 上限由两件事决定：
     *   竖向 56dp * s <= 64dp 外壳                  -> s <= 1.1429
     *   横向 胶囊宽就是满格 cell，左右各只剩 4dp 内边 -> s <= 1 + 8dp / cell
     *        （本机 cell = (384-52-8)/3 ≈ 108dp -> 1.074）
     * 取 1.07：常见屏宽下都完整留在 64dp 外壳里，首尾格也一点不溢出。 */
    private static final float PRESSED_SCALE = 1.07f;

    private final android.view.Choreographer.FrameCallback pillTicker =
            new android.view.Choreographer.FrameCallback() {
                @Override
                public void doFrame(long frameTimeNanos) {
                    if (!pillRunning) return;
                    if (pillLastFrame == 0L) pillLastFrame = frameTimeNanos - 16000000L;
                    float dt = (frameTimeNanos - pillLastFrame) / 1.0e9f;
                    pillLastFrame = frameTimeNanos;
                    if (dt > 0.032f) dt = 0.032f;   // 掉帧时钳住，否则弹簧会炸
                    if (dt <= 0f) dt = 0.016f;

                    // 与 Stage 同样的理由：一帧拆成 4ms 子步，掉帧时不发散
                    float pillLeft = dt;
                    while (pillLeft > 0f) {
                        float step = Math.min(0.004f, pillLeft);
                        pillVelocity += ((pillTarget - pillValue) * PILL_STIFFNESS
                                - pillVelocity * PILL_DAMPING) * step;
                        pillValue += pillVelocity * step;
                        pillLeft -= step;
                    }

                    // release()：|value - targetValue| 小于跨度 2.5% 才开始缩回
                    if (pressHold) {
                        float holdSpan = Math.max(1f, tabViews.size() - 1);
                        if (Math.abs(pillTarget - pillValue) < holdSpan * 0.025f) {
                            pressTarget = 0f;
                            pressHold = false;
                        }
                    }

                    float pressLeft = dt;
                    while (pressLeft > 0f) {
                        float step = Math.min(0.004f, pressLeft);
                        pressVelocity += ((pressTarget - pressProgress) * PILL_STIFFNESS
                                - pressVelocity * PILL_DAMPING) * step;
                        pressProgress += pressVelocity * step;
                        pressLeft -= step;
                    }

                    boolean valueSettled = Math.abs(pillTarget - pillValue) < 0.0015f
                            && Math.abs(pillVelocity) < 0.01f;
                    boolean pressSettled = Math.abs(pressTarget - pressProgress) < 0.002f
                            && Math.abs(pressVelocity) < 0.02f;
                    if (valueSettled) {
                        pillValue = pillTarget;
                        pillVelocity = 0f;
                    }
                    if (pressSettled) {
                        pressProgress = pressTarget;
                        pressVelocity = 0f;
                    }
                    applyPill();
                    if (valueSettled && pressSettled) {
                        pillRunning = false;
                        pillLastFrame = 0L;
                        return;
                    }
                    android.view.Choreographer.getInstance().postFrameCallback(this);
                }
            };

    private void drivePill(float target) {
        drivePill(target, true);
    }

    /**
     * press = true 时复刻 DampedDragAnimation.animateToValue：
     * 先 press() 放大，整段位移都是按下态，到位后由 release() 缩回。
     */
    private void drivePill(float target, boolean press) {
        pillTarget = target;
        if (press) {
            pressTarget = 1f;
            pressHold = true;
        }
        startPillTicker();
    }

    private void pressPill(boolean down) {
        pressTarget = down ? 1f : 0f;
        if (!down) pressHold = false;
        startPillTicker();
    }

    /** 手指抬起：不立刻缩回，等胶囊走到目标格附近再缩（同源码 release()） */
    private void releasePillAfterSettle() {
        pressHold = true;
        startPillTicker();
    }

    private void startPillTicker() {
        if (pillRunning) return;
        pillRunning = true;
        pillLastFrame = 0L;
        android.view.Choreographer.getInstance().postFrameCallback(pillTicker);
    }

    /** 位置 + 按下缩放 + 速度形变，公式和 FloatingBottomBar 的 layerBlock 一致 */
    private void applyPill() {
        if (tabPill == null) return;
        int cell = barCellWidth();
        if (cell <= 0) return;
        // 胶囊中心对齐 pillValue 对应的格心：首尾格也完整落在底栏内，左右对称
        float left = pillValue * cell + (cell - pillWidth()) / 2f;
        tabPill.setTranslationX(left);
        // 模糊位图和底栏同坐标系（barShell 内边距 4dp），胶囊该取哪块由它自己算
        tabPill.setSrcOffsetX(host.dp(4) + left);

        float scale = 1f + (PRESSED_SCALE - 1f) * pressProgress;
        /* 速度形变只在手指拖底栏时生效。KernelSU 的 velocity 只在 updateValue()（拖动）里更新，
         * 点底栏走的是 animateToValue()，那里根本不动 velocity，所以点击时不会有拉伸；
         * 我们用的是弹簧的实时速度，点击跨两格时会瞬间到 20+ 格/秒，被 clamp 到上限 0.2，
         * 胶囊横向被拉到 1.25 倍，看着就像「弹出去又弹回来」——所以补上这个条件。 */
        float span = Math.max(1f, tabViews.size() - 1);
        float v = barDragging ? pillVelocity / span / 10f : 0f;
        float kx = Math.max(-0.2f, Math.min(0.2f, v * 0.75f));
        float ky = Math.max(-0.2f, Math.min(0.2f, v * 0.25f));
        tabPill.setScaleX(scale / (1f - kx));   // 快速右滑时横向拉长
        tabPill.setScaleY(scale * (1f - ky));   // 同时纵向压扁
        tabPill.setAlpha(1f - pressProgress * 0.55f);  // 按下时玻璃球淡出，露底下的模糊
    }

    private int barInnerWidth() {
        if (tabBar == null) return 0;
        return tabBar.getWidth();
    }

    private int barCellWidth() {
        int w = barInnerWidth();
        int n = Math.max(1, tabViews.size());
        if (w <= 0) return 0;
        return Math.max(1, w / n);
    }

    /**
     * 指示器宽度 = 整格减 8dp（两边各留 4dp）。
     * KernelSU 的指示器就是 tabWidth 宽，第一格时左边缘几乎贴着底栏内边，
     * 这才是「边缘贴合」的来源；固定 76dp 一定留出 20dp 以上的空隙。
     */
    private int pillWidth() {
        int cell = barCellWidth();
        if (cell <= 0) return host.dp(72);
        return Math.max(host.dp(40), cell);
    }

    private void barPillWidth() {
        if (tabPill == null) return;
        ViewGroup.LayoutParams lp = tabPill.getLayoutParams();
        if (lp.width == pillWidth() && lp.height == host.dp(56)) return;
        lp.width = pillWidth();
        lp.height = host.dp(56);
        tabPill.setLayoutParams(lp);
    }

    /** idx 号 tab 的胶囊格位：胶囊宽度就是整格，所以直接用索引 */
    private float pillXForIndex(int idx) {
        int n = Math.max(1, tabViews.size());
        return Math.max(0, Math.min(n - 1, idx));
    }

    /** 手指 x → 胶囊格位；胶囊中心贴手指，两端夹在首尾格内 */
    private float pillXForTouch(float touchX) {
        int cell = barCellWidth();
        int n = Math.max(1, tabViews.size());
        if (cell <= 0) return pillValue;
        float v = touchX / (float) cell - 0.5f;
        return Math.max(0f, Math.min(n - 1f, v));
    }

    void scheduleGlassRefresh() {
        if (glassPending || glassBackdrop == null) return;
        long now = android.os.SystemClock.uptimeMillis();
        long wait = Math.max(0L, 90L - (now - glassLastAt));
        glassPending = true;
        glassBackdrop.postDelayed(() -> {
            glassPending = false;
            glassLastAt = android.os.SystemClock.uptimeMillis();
            updateGlassBackdrop();
        }, wait);
    }

    private void selectTab(int idx) {
        if (host.page.pageScrolls.isEmpty()) return;
        int i = Math.max(0, Math.min(host.page.pageScrolls.size() - 1, idx));
        if (stage != null) stage.springToPageFromBar(i);
        updateTabUi(i);
    }

    void updateTabUi(int idx) {
        if (idx < 0 || idx >= tabViews.size()) return;
        if (idx == currentTab && tabUiReady) {
            // 同一页会被调用两次（滑动过半一次、吸附结束一次）。第二次只认胶囊目标，
            // 不再重设 typeface / 颜色：重建 Typeface 会让文字重排、底栏图标闪一下，
            // 再 drivePill(press) 还会让胶囊多放大一次。
            if (idx < host.page.pageScrolls.size()) host.scrollRoot = host.page.pageScrolls.get(idx);
            drivePill(pillXForIndex(idx), false);
            return;
        }
        currentTab = idx;
        tabUiReady = true;
        for (int i = 0; i < tabViews.size(); i++) {
            boolean on = i == idx;
            if (i < tabIcons.size()) {
                tabIcons.get(i).setSelected(on);
                tabIcons.get(i).setIconColor(on ? host.ACCENT : host.SUB);
            }
            if (i < tabLabels.size()) {
                tabLabels.get(i).setTextColor(on ? host.ACCENT : host.SUB);
                tabLabels.get(i).setTypeface(null, on ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            }
        }
        if (idx < host.page.pageScrolls.size()) host.scrollRoot = host.page.pageScrolls.get(idx);
        drivePill(pillXForIndex(idx));
        if (glassBackdrop != null) glassBackdrop.postDelayed(this::updateGlassBackdrop, 360);
    }

    private void updateGlassBackdrop() {
        if (glassBackdrop == null || stage == null) return;
        int w = glassBackdrop.getWidth();
        int h = glassBackdrop.getHeight();
        if (w <= 0 || h <= 0) return;
        try {
            int[] loc = new int[2];
            glassBackdrop.getLocationInWindow(loc);
            int[] pagerLoc = new int[2];
            stage.getLocationInWindow(pagerLoc);
            int dx = pagerLoc[0] - loc[0];
            int dy = pagerLoc[1] - loc[1];
            // 降采样绘制，模糊后观感一致，开销小很多
            int sw = Math.max(1, w / 4);
            int sh = Math.max(1, h / 4);
            android.graphics.Bitmap small = android.graphics.Bitmap.createBitmap(sw, sh, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas c = new android.graphics.Canvas(small);
            c.scale(1f / 4f, 1f / 4f);
            c.translate(-dx, -dy);
            stage.draw(c);
            android.graphics.Bitmap scaled = android.graphics.Bitmap.createScaledBitmap(small, w, h, true);
            small.recycle();
            glassBackdrop.setImageBitmap(scaled);
            if (tabPill != null) {
                tabPill.setBackdrop(scaled);
                tabPill.setSrcOffsetX(host.dp(4) + pillValue * barCellWidth()
                        + (barCellWidth() - pillWidth()) / 2f);
            }
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                // 源码：blur(4.dp) + vibrancy()，不是大半径糊成一片
                float r = 4f * host.getResources().getDisplayMetrics().density;
                android.graphics.RenderEffect blur =
                        android.graphics.RenderEffect.createBlurEffect(
                                r, r, android.graphics.Shader.TileMode.CLAMP);
                android.graphics.ColorMatrix cm = new android.graphics.ColorMatrix();
                cm.setSaturation(1.6f);
                android.graphics.RenderEffect sat =
                        android.graphics.RenderEffect.createColorFilterEffect(
                                new android.graphics.ColorMatrixColorFilter(cm), null);
                glassBackdrop.setRenderEffect(
                        android.graphics.RenderEffect.createChainEffect(sat, blur));
            }
        } catch (Throwable ignored) {
        }
    }

    int currentTab() {
        return currentTab;
    }

    void onPageScrolled(float progress) {
        if (barTouching || tabViews.isEmpty()) return;
        if (stage != null && stage.isNavigating()) return;
        int n = tabViews.size();
        float clamped = Math.max(0f, Math.min(n - 1f, progress));
        int page = Math.round(clamped);
        if (page != currentTab) updateTabUi(page);
    }

    private int nearestTab() {
        int n = Math.max(1, tabViews.size());
        return Math.max(0, Math.min(n - 1, Math.round(pillTarget)));
    }

    /** 手指 x 落在第几格：与 pillXForTouch 同一套坐标，直接四舍五入，不依赖动画状态 */
    private int tabIndexForTouch(float touchX) {
        int cell = barCellWidth();
        if (cell <= 0) return nearestTab();
        return Math.round(pillXForTouch(touchX));
    }

    /** 手指是否按在底栏上（页面吸附结束时用它判断该不该抢选中态） */
    boolean isTouching() {
        return barTouching;
    }

    void build(FrameLayout rootFrame) {
        FrameLayout barShell = new FrameLayout(host);
        GradientDrawable glass = new GradientDrawable();
        // 半透明底 + 下层的模糊位图 → 视觉上是液态玻璃；不透明会盖掉模糊
        // surfaceContainer.copy(0.4f)：40% 不透明，剩下的交给下层的模糊位图
        glass.setColor(host.nightMode ? 0x66211F26 : 0x66F3EDF7);   // KernelSU: surfaceContainer.copy(0.4f)
        glass.setCornerRadius(host.dp(32));       // CircleShape 等价：64dp 高的一半
        barShell.setBackground(glass);
        barShell.setPadding(host.dp(4), host.dp(4), host.dp(4), host.dp(4));
        // 胶囊按下只放大到外壳里（1.07），四个方向都不会越界，不依赖父容器不裁剪
        barShell.setClipChildren(false);
        barShell.setClipToPadding(false);
        barShell.setElevation(host.dp(10));       // dropShadow(radius = 10.dp, alpha = 0.1f)
        barShell.setOutlineSpotShadowColor(0x1A000000);
        barShell.setOutlineAmbientShadowColor(0x1A000000);

        tabPill = new GlassPill(host);
        tabPill.setNight(host.nightMode);
        // 宽度 = 一格的宽，布局完成后由 barPillWidth() 修正
        barShell.addView(tabPill, new FrameLayout.LayoutParams(pillWidth(), host.dp(56)));

        tabBar = new LinearLayout(host);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabViews.clear();
        tabIcons.clear();
        tabLabels.clear();
        String[] names = new String[]{host.T("主页"), host.T("组件拦截"), host.T("设置")};
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            LinearLayout tab = new LinearLayout(host);
            tab.setOrientation(LinearLayout.VERTICAL);
            tab.setGravity(Gravity.CENTER);
            TabIcon icon = new TabIcon(host, i);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(host.dp(24), host.dp(24));
            ilp.bottomMargin = host.dp(2);
            tab.addView(icon, ilp);
            TextView label = new TextView(host);
            label.setText(names[i]);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            label.setGravity(Gravity.CENTER);
            tab.addView(label, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tabViews.add(tab);
            tabIcons.add(icon);
            tabLabels.add(label);
            tabBar.addView(tab, new LinearLayout.LayoutParams(0, host.dp(56), 1f));
        }
        barShell.addView(tabBar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, host.dp(56)));

        /* 顶层触摸层：整条底栏由它接管，行为照 DampedDragAnimation 的 pointerInput
         * 按下   → press() 放大到 PRESSED_SCALE，胶囊 spring 吸到手指所在格
         * 拖动   → 只推胶囊格位，页面一动不动
         * 松手   → targetValue.roundToInt() 吸附，这才调 onSelected 切页
         */
        final android.view.View touchLayer = new android.view.View(host);
        touchLayer.setOnTouchListener((v, e) -> {
            final int cell = barCellWidth();
            if (cell <= 0) return false;
            switch (e.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    barDownX = e.getX();
                    barDownY = e.getY();
                    barDragging = false;
                    barTouching = true;
                    pressPill(true);
                    pressHold = false;      // 手指压着，别自动缩回
                    drivePill(pillXForTouch(e.getX()), false);
                    return true;
                case android.view.MotionEvent.ACTION_MOVE:
                    barDragging = true;
                    /* 手指还按着：只推目标，不再走 press 分支。
                     * 之前这里调的是带 press 的 drivePill，于是 pressHold 被反复置真，
                     * ticker 每次 |pillTarget - pillValue| < 0.05 就把 pressTarget 归零，
                     * 表现就是「按住不动时是大的，手指一动就缩、停下又放大」。 */
                    drivePill(pillXForTouch(e.getX()), false);
                    return true;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    /* 用抬手位置算目标格，不读 pillTarget：pillTarget 在按下期间可能被
                     * 上一段页面吸附动画的 onPageSelected 改掉（那一刻正在按着，看不出变化），
                     * 于是松手会「切到当前页」，表现就是胶囊走一段又弹回来、页面没变。 */
                    int tapped = tabIndexForTouch(e.getX());
                    barTouching = false;
                    selectTab(tapped);
                    releasePillAfterSettle();
                    barDragging = false;
                    return true;
            }
            return true;
        });
        // 玻璃高光层：顶部边缘亮、底部几乎透明，液态玻璃的 specular 边
        barShell.addView(new GlossView(host, host.dp(32), Math.max(1, host.dp(1))),
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 固定高度：MATCH_PARENT 会把 WRAP_CONTENT 的底栏容器撑满全屏
        /* 铺满 64dp 外壳：胶囊只有 56dp 高，原来触摸层也只 56dp，
         * 点在胶囊下沿那 8dp 里没有任何反应。 */
        barShell.addView(touchLayer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, host.dp(64)));

        FrameLayout barWrap = new FrameLayout(host);
        barWrap.setClipChildren(false);      // 否则 elevation 阴影被容器裁掉
        barWrap.setClipToPadding(false);
        // 液态玻璃底层：模糊位图，被圆角 outline 裁住，垫在 barShell 下面
        glassBackdrop = new ImageView(host);
        glassBackdrop.setScaleType(ImageView.ScaleType.FIT_XY);
        GradientDrawable glassClip = new GradientDrawable();
        // 深色下必须是暗底：这一层在模糊位图生成前会先露出来（首帧／切主题重建）
        glassClip.setColor(host.nightMode ? 0xFF1C1C1E : 0xFFFFFFFF);
        glassClip.setCornerRadius(host.dp(32));
        glassBackdrop.setBackground(glassClip);
        glassBackdrop.setClipToOutline(true);
        glassBackdrop.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
        barWrap.addView(glassBackdrop, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        FrameLayout.LayoutParams wlp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, host.dp(64));
        wlp.gravity = Gravity.BOTTOM;
        wlp.setMargins(host.dp(26), 0, host.dp(26), host.dp(10) + host.navBarHeight());
        barWrap.addView(barShell, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, host.dp(64)));
        rootFrame.addView(barWrap, wlp);
        updateTabUi(currentTab);
        tabBar.post(() -> {
            barPillWidth();
            pillValue = pillXForIndex(currentTab);
            applyPill();
        });
        barWrap.postDelayed(this::updateGlassBackdrop, 160);
    }
}

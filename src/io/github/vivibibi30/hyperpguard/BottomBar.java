package io.github.vivibibi30.hyperpguard;

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
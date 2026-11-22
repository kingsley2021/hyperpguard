package io.github.vivibibi30.hyperpguard;

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


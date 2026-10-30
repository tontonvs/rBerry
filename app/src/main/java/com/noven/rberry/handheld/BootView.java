/*
 * Copyright 2026 Noven (tontonvs)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.noven.rberry.handheld;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import ru.playsoftware.j2meloader.R;

/**
 * The start-up animation shown in the display: the berry drops in, "RetroBerry" types itself out,
 * then the whole thing fades away. A tap skips it. The berry is drawn from the same shapes as
 * branding/rBerry_logo.svg.
 */
final class BootView extends View {

    private static final long TOTAL_MS = 2100;
    private static final long FADE_START_MS = 1800;
    private static final String WORD_A = "Retro";
    private static final String WORD_B = "Berry";

    // The logo is drawn in its 1024 x 1024 SVG space; this is the part of it that has ink.
    private static final float LOGO_CX = 504f;
    private static final float LOGO_CY = 577.5f;
    private static final float LOGO_SPAN = 690f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path dome = new Path();
    private final Path stalk = new Path();
    private final Shader backShader;
    private final Shader glossShader;
    private final Shader domeShader;
    private final Shader eyeShader;
    private final Shader eyeShader2;
    private final String tagline;

    private long startedAt;
    private boolean finished;
    private Runnable onFinished;

    private final Runnable frame = new Runnable() {
        @Override
        public void run() {
            if (finished) {
                return;
            }
            long ms = SystemClock.uptimeMillis() - startedAt;
            if (ms >= TOTAL_MS) {
                skip();
                return;
            }
            float fade = clamp((ms - FADE_START_MS) / (float) (TOTAL_MS - FADE_START_MS));
            setAlpha(1f - fade);
            invalidate();
            postOnAnimation(this);
        }
    };

    BootView(@NonNull Context context) {
        super(context);
        tagline = context.getString(R.string.handheld_boot_tagline);
        backShader = new LinearGradient(0f, 243f, 0f, 912f,
                new int[]{0xFFF0307A, 0xFFC80A4E, 0xFFA80038}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP);
        glossShader = new LinearGradient(0f, 243f, 0f, 440f, 0x61FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP);
        domeShader = new RadialGradient(511f, 790f, 430f,
                new int[]{0xFFB0003C, 0xFFC20A4B, 0xFFE81E78, 0xFFFF7DB1},
                new float[]{0f, 0.58f, 0.86f, 1f}, Shader.TileMode.CLAMP);
        eyeShader = new LinearGradient(0f, 573f, 0f, 729f, 0xFFFFFFFF, 0xFFF6B7CF, Shader.TileMode.CLAMP);
        eyeShader2 = new LinearGradient(0f, 538f, 0f, 690f, 0xFFFFFFFF, 0xFFF6B7CF, Shader.TileMode.CLAMP);

        dome.moveTo(175f, 660f);
        dome.cubicTo(175f, 520f, 330f, 408f, 511f, 408f);
        dome.cubicTo(692f, 408f, 848f, 520f, 848f, 660f);
        dome.lineTo(848f, 747f);
        dome.arcTo(new RectF(518f, 582f, 848f, 912f), 0f, 90f);
        dome.lineTo(340f, 912f);
        dome.arcTo(new RectF(175f, 582f, 505f, 912f), 90f, 90f);
        dome.close();

        stalk.moveTo(160f, 433f);
        stalk.quadTo(248f, 420f, 290f, 490f);

        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    void setOnFinished(Runnable onFinished) {
        this.onFinished = onFinished;
    }

    /** Ends the animation now. Safe to call more than once. */
    void skip() {
        if (finished) {
            return;
        }
        finished = true;
        removeCallbacks(frame);
        if (onFinished != null) {
            Runnable done = onFinished;
            onFinished = null;
            done.run();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startedAt = SystemClock.uptimeMillis();
        postOnAnimation(frame);
    }

    @Override
    protected void onDetachedFromWindow() {
        // The host went away; just stop, do not call back into a dead screen.
        finished = true;
        removeCallbacks(frame);
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            skip();
        }
        return true;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        long ms = SystemClock.uptimeMillis() - startedAt;
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(null);
        paint.setColor(0xFF1E0B22);
        canvas.drawRect(0f, 0f, w, h, paint);

        float logoSize = Math.min(w, h) * 0.38f;
        float cx = w / 2f;
        float cy = h * 0.40f;
        float p = easeOutBack(clamp((ms - 150) / 500f));
        float scale = 0.55f + 0.45f * p;
        float drop = (1f - p) * -h * 0.12f;
        drawLogo(canvas, cx, cy + drop, logoSize / LOGO_SPAN * scale);

        float size = Math.min(w * 0.11f, h * 0.09f);
        textPaint.setTextSize(size);
        float charW = textPaint.measureText("W");
        float total = charW * (WORD_A.length() + WORD_B.length());
        float x = cx - total / 2f;
        float baseline = cy + logoSize / 2f + size * 1.5f;
        int shown = (int) (clamp((ms - 700) / 700f) * (WORD_A.length() + WORD_B.length()));
        int shownA = Math.min(shown, WORD_A.length());
        textPaint.setColor(0xFFFFFFFF);
        canvas.drawText(WORD_A, 0, shownA, x, baseline, textPaint);
        if (shown > WORD_A.length()) {
            textPaint.setColor(0xFFFF6FA5);
            canvas.drawText(WORD_B, 0, shown - WORD_A.length(), x + charW * WORD_A.length(), baseline, textPaint);
        }
        if (ms < 1500 && ((ms / 220) & 1) == 0) {
            // Blinking block cursor while typing.
            paint.setColor(0xFFFF6FA5);
            float cursorX = x + charW * shown;
            canvas.drawRect(cursorX + charW * 0.1f, baseline - size * 0.8f, cursorX + charW * 0.7f, baseline, paint);
        }

        float tagAlpha = clamp((ms - 1300) / 300f);
        if (tagAlpha > 0f) {
            textPaint.setTextSize(size * 0.42f);
            textPaint.setColor(0xFFFFB070);
            textPaint.setAlpha((int) (255 * tagAlpha));
            float tw = textPaint.measureText(tagline);
            canvas.drawText(tagline, cx - tw / 2f, baseline + size * 0.95f, textPaint);
        }
    }

    private void drawLogo(Canvas canvas, float cx, float cy, float s) {
        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(s, s);
        canvas.translate(-LOGO_CX, -LOGO_CY);

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(backShader);
        rect.set(175f, 243f, 848f, 912f);
        canvas.drawRoundRect(rect, 165f, 165f, paint);

        canvas.save();
        canvas.clipRect(175f, 243f, 848f, 440f);
        paint.setShader(glossShader);
        canvas.drawRoundRect(rect, 165f, 165f, paint);
        canvas.restore();

        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(30f);
        paint.setColor(0xFF141414);
        canvas.drawPath(stalk, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(domeShader);
        canvas.drawPath(dome, paint);

        paint.setShader(eyeShader);
        rect.set(451f - 62f, 651f - 78f, 451f + 62f, 651f + 78f);
        canvas.drawOval(rect, paint);

        canvas.save();
        canvas.rotate(-8f, 627f, 614f);
        paint.setShader(eyeShader2);
        rect.set(627f - 59f, 614f - 76f, 627f + 59f, 614f + 76f);
        canvas.drawOval(rect, paint);
        canvas.restore();

        paint.setShader(null);
        canvas.restore();
    }

    private static float clamp(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }
}

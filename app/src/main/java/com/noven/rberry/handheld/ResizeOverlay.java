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
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import ru.playsoftware.j2meloader.R;

/**
 * Drawn over the display while the player is resizing the picture. A dashed pink frame with
 * handles: drag an edge or corner to resize, drag inside to move. Three big buttons at the bottom:
 * stretch on/off, reset, done.
 */
final class ResizeOverlay extends View {

    interface Listener {
        /** The frame changed (a drag ended, reset, or stretch toggled). Apply it now. */
        void onFrameChanged(@NonNull DisplayFrame frame);

        /** The player pressed DONE. */
        void onFinished(@NonNull DisplayFrame frame);
    }

    private static final int PINK = 0xFFFF6FA5;
    private static final int PLUM = 0xFF2A1030;
    private static final int SCRIM = 0x99140818;

    private static final int NONE = -1;
    private static final int BTN_STRETCH = 0;
    private static final int BTN_RESET = 1;
    private static final int BTN_DONE = 2;

    private final Listener listener;
    private final DisplayFrame frame;
    private final DisplayFrame dragStart = new DisplayFrame();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final RectF[] buttons = {new RectF(), new RectF(), new RectF()};
    private final String stretchOn;
    private final String stretchOff;
    private final String resetLabel;
    private final String doneLabel;
    private final String hint1;
    private final String hint2;
    private final float density;
    private final DashPathEffect dashes;
    private final float grab;

    private int grabbed;
    private int pressedButton = NONE;
    private float downX;
    private float downY;
    private boolean finished;

    ResizeOverlay(@NonNull Context context, @NonNull DisplayFrame start, @NonNull Listener listener) {
        super(context);
        this.listener = listener;
        this.frame = new DisplayFrame(start);
        density = context.getResources().getDisplayMetrics().density;
        grab = 30f * density;
        dashes = new DashPathEffect(new float[]{10f * density, 6f * density}, 0f);
        stretchOn = context.getString(R.string.resize_stretch_on);
        stretchOff = context.getString(R.string.resize_stretch_off);
        resetLabel = context.getString(R.string.resize_reset);
        doneLabel = context.getString(R.string.resize_done);
        hint1 = context.getString(R.string.resize_hint_edges);
        hint2 = context.getString(R.string.resize_hint_move);
        paint.setTypeface(PixelFont.get(context));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float gap = 8f * density;
        float bh = 36f * density;
        float bottom = h - gap;
        buttons[BTN_STRETCH].set(gap, bottom - 2 * bh - gap, w - gap, bottom - bh - gap);
        float half = (w - 3 * gap) / 2f;
        buttons[BTN_RESET].set(gap, bottom - bh, gap + half, bottom);
        buttons[BTN_DONE].set(2 * gap + half, bottom - bh, w - gap, bottom);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float l = frame.left * w;
        float t = frame.top * h;
        float r = frame.right * w;
        float b = frame.bottom * h;

        // Dim everything outside the frame.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(SCRIM);
        canvas.drawRect(0f, 0f, w, t, paint);
        canvas.drawRect(0f, b, w, h, paint);
        canvas.drawRect(0f, t, l, b, paint);
        canvas.drawRect(r, t, w, b, paint);

        // Dashed frame.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f * density);
        paint.setColor(PINK);
        paint.setPathEffect(dashes);
        canvas.drawRect(l, t, r, b, paint);
        paint.setPathEffect(null);

        // Handles: corners and the middle of each edge.
        float hs = 9f * density;
        float mx = (l + r) / 2f;
        float my = (t + b) / 2f;
        handle(canvas, l, t, hs);
        handle(canvas, r, t, hs);
        handle(canvas, l, b, hs);
        handle(canvas, r, b, hs);
        handle(canvas, mx, t, hs * 0.75f);
        handle(canvas, mx, b, hs * 0.75f);
        handle(canvas, l, my, hs * 0.75f);
        handle(canvas, r, my, hs * 0.75f);

        // Hint in the middle of the frame.
        float size = 10f * density;
        paint.setTextSize(size);
        paint.setTextAlign(Paint.Align.CENTER);
        float tw = Math.max(paint.measureText(hint1), paint.measureText(hint2));
        float cx = mx;
        float cy = my - size * 2.2f;
        if (r - l > tw + 24f * density) {
            rect.set(cx - tw / 2f - 8f * density, cy - size * 1.5f, cx + tw / 2f + 8f * density, cy + size * 2.4f);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xCC2A1030);
            canvas.drawRoundRect(rect, 6f * density, 6f * density, paint);
            paint.setColor(0xFFFFFFFF);
            canvas.drawText(hint1, cx, cy, paint);
            paint.setColor(PINK);
            canvas.drawText(hint2, cx, cy + size * 1.7f, paint);
        }

        // Buttons.
        button(canvas, BTN_STRETCH, frame.stretch ? stretchOn : stretchOff, frame.stretch);
        button(canvas, BTN_RESET, resetLabel, false);
        button(canvas, BTN_DONE, doneLabel, true);
    }

    private void handle(Canvas canvas, float x, float y, float half) {
        rect.set(x - half, y - half, x + half, y + half);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFFFFFFF);
        canvas.drawRoundRect(rect, 3f * density, 3f * density, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f * density);
        paint.setColor(PINK);
        canvas.drawRoundRect(rect, 3f * density, 3f * density, paint);
    }

    private void button(Canvas canvas, int index, String label, boolean strong) {
        RectF box = buttons[index];
        boolean down = pressedButton == index;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(strong ? (down ? 0xFFFF9CC0 : PINK) : (down ? 0xFF4B2A55 : PLUM));
        canvas.drawRoundRect(box, 8f * density, 8f * density, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f * density);
        paint.setColor(PINK);
        canvas.drawRoundRect(box, 8f * density, 8f * density, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(12f * density);
        paint.setColor(strong ? PLUM : 0xFFFFFFFF);
        float baseline = box.centerY() - (paint.ascent() + paint.descent()) / 2f;
        canvas.drawText(label, box.centerX(), baseline, paint);
    }

    private int buttonAt(float x, float y) {
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i].contains(x, y)) {
                return i;
            }
        }
        return NONE;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (finished) {
            return true;
        }
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressedButton = buttonAt(x, y);
                grabbed = 0;
                if (pressedButton == NONE) {
                    grabbed = frame.hit(x, y, getWidth(), getHeight(), grab);
                    dragStart.set(frame);
                    downX = x;
                    downY = y;
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (grabbed != 0) {
                    frame.drag(dragStart, grabbed, x - downX, y - downY, getWidth(), getHeight());
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (pressedButton != NONE) {
                    int released = buttonAt(x, y);
                    int pressed = pressedButton;
                    pressedButton = NONE;
                    if (released == pressed) {
                        press(pressed);
                    }
                } else if (grabbed != 0) {
                    frame.drag(dragStart, grabbed, x - downX, y - downY, getWidth(), getHeight());
                    grabbed = 0;
                    listener.onFrameChanged(new DisplayFrame(frame));
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressedButton = NONE;
                if (grabbed != 0) {
                    frame.set(dragStart);
                    grabbed = 0;
                }
                invalidate();
                return true;
            default:
                return true;
        }
    }

    private void press(int button) {
        if (button == BTN_STRETCH) {
            frame.stretch = !frame.stretch;
            listener.onFrameChanged(new DisplayFrame(frame));
        } else if (button == BTN_RESET) {
            frame.reset();
            listener.onFrameChanged(new DisplayFrame(frame));
        } else if (button == BTN_DONE) {
            finish();
        }
    }

    /** Closes the overlay and tells the listener. Safe to call twice. */
    void finish() {
        if (finished) {
            return;
        }
        finished = true;
        listener.onFinished(new DisplayFrame(frame));
    }
}

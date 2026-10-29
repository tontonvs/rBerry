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

package com.noven.rberry.games.snake;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

import ru.playsoftware.j2meloader.R;

/**
 * The Snake screen: draws the board with the coloured rBerry display style and handles swipes.
 *
 * <p>Everything a host needs is public: {@link #turn}, {@link #togglePause} and {@link #pause}.
 * Phase 3 will call these from the on-screen D-pad and PLAY button of the handheld body.</p>
 */
public class SnakeView extends View {

    public enum State { READY, RUNNING, PAUSED, GAME_OVER, WON }

    static final int COLS = 17;
    static final int ROWS = 20;

    private static final String PREFS = "rberry_snake";
    private static final String KEY_BEST = "best";
    private static final long RETRY_LOCKOUT_MS = 600;

    // Display palette: plum screen, electric green snake, berry food, button-palette accents.
    private static final int COLOR_BG = 0xFF1E0B22;
    private static final int COLOR_PANEL = 0xFF2E1238;
    private static final int COLOR_CELL_A = 0xFF24102B;
    private static final int COLOR_CELL_B = 0xFF2A1333;
    private static final int COLOR_HEAD = 0xFF7DFFB0;
    private static final int COLOR_TAIL = 0xFF17A858;
    private static final int COLOR_BERRY = 0xFFFF2E6E;
    private static final int COLOR_BERRY_SHINE = 0xFFFFA6C4;
    private static final int COLOR_LEAF = 0xFF39E07A;
    private static final int COLOR_EYE = 0xFFFFFFFF;
    private static final int COLOR_PUPIL = 0xFF1E0B22;
    private static final int COLOR_SCORE = 0xFFFFD84D;
    private static final int COLOR_BEST = 0xFF62E6FF;
    private static final int COLOR_HINT = 0xFFFFB070;
    private static final int COLOR_LOSE = 0xFFFF5C8A;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_DIM = 0xB3120616;

    private final SnakeEngine engine = new SnakeEngine(COLS, ROWS, new Random());
    private final int[] bodyBuffer = new int[COLS * ROWS];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final SharedPreferences prefs;
    private final float density;
    private final float swipeThreshold;

    private State state = State.READY;
    private int best;
    private boolean newBest;
    private long endedAt;
    private String scoreText = "";
    private String bestText = "";

    private float cell;
    private float gridLeft;
    private float gridTop;
    private float hudBaseline;

    private float anchorX;
    private float anchorY;
    private boolean swiped;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (state != State.RUNNING) {
                return;
            }
            SnakeEngine.Result result = engine.step();
            if (result == SnakeEngine.Result.DIED) {
                finishGame(State.GAME_OVER);
            } else if (result == SnakeEngine.Result.WON) {
                finishGame(State.WON);
            } else if (result == SnakeEngine.Result.ATE) {
                refreshHud();
            }
            invalidate();
            if (state == State.RUNNING) {
                postDelayed(this, engine.tickMillis());
            }
        }
    };

    public SnakeView(Context context) {
        this(context, null);
    }

    public SnakeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        swipeThreshold = 24 * density;
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        best = prefs.getInt(KEY_BEST, 0);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        setFocusable(true);
        refreshHud();
    }

    // ---- public controls -------------------------------------------------------------------

    /** Steers the snake. The first direction pressed on the ready screen also starts the game. */
    public void turn(SnakeEngine.Dir dir) {
        if (state == State.READY) {
            engine.turn(dir);
            begin();
        } else if (state == State.RUNNING) {
            engine.turn(dir);
        }
    }

    /** Start, pause, resume or get ready for a new round, depending on the current state. */
    public void togglePause() {
        switch (state) {
            case RUNNING:
                pause();
                break;
            case READY:
            case PAUSED:
                begin();
                break;
            case GAME_OVER:
            case WON:
                if (SystemClock.uptimeMillis() - endedAt >= RETRY_LOCKOUT_MS) {
                    engine.reset();
                    newBest = false;
                    state = State.READY;
                    refreshHud();
                    invalidate();
                }
                break;
            default:
                break;
        }
    }

    public void pause() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
            removeCallbacks(ticker);
            invalidate();
        }
    }

    public State getState() {
        return state;
    }

    // ---- game flow --------------------------------------------------------------------------

    private void begin() {
        state = State.RUNNING;
        removeCallbacks(ticker);
        postDelayed(ticker, engine.tickMillis());
        invalidate();
    }

    private void finishGame(State end) {
        state = end;
        endedAt = SystemClock.uptimeMillis();
        int score = engine.getScore();
        newBest = score > best;
        if (newBest) {
            best = score;
            prefs.edit().putInt(KEY_BEST, best).apply();
        }
        refreshHud();
    }

    private void refreshHud() {
        Context c = getContext();
        scoreText = c.getString(R.string.snake_hud_score) + " " + engine.getScore();
        bestText = c.getString(R.string.snake_hud_best) + " " + best;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(ticker);
        if (state == State.RUNNING) {
            state = State.PAUSED;
        }
    }

    // ---- input ------------------------------------------------------------------------------

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                anchorX = event.getX();
                anchorY = event.getY();
                swiped = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                trackSwipe(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_UP:
                if (!swiped && state != State.RUNNING) {
                    togglePause();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void trackSwipe(float x, float y) {
        float dx = x - anchorX;
        float dy = y - anchorY;
        if (Math.max(Math.abs(dx), Math.abs(dy)) < swipeThreshold) {
            return;
        }
        SnakeEngine.Dir dir;
        if (Math.abs(dx) > Math.abs(dy)) {
            dir = dx > 0 ? SnakeEngine.Dir.RIGHT : SnakeEngine.Dir.LEFT;
        } else {
            dir = dy > 0 ? SnakeEngine.Dir.DOWN : SnakeEngine.Dir.UP;
        }
        swiped = true;
        // New anchor lets one continuous drag chain several turns.
        anchorX = x;
        anchorY = y;
        turn(dir);
    }

    // ---- layout and drawing -----------------------------------------------------------------

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float hud = 44 * density;
        float margin = 12 * density;
        float availW = w - 2 * margin;
        float availH = h - hud - margin;
        cell = (float) Math.floor(Math.min(availW / COLS, availH / ROWS));
        if (cell < 1f) {
            cell = 1f;
        }
        gridLeft = (w - cell * COLS) / 2f;
        gridTop = hud + Math.max(0f, (availH - cell * ROWS) / 2f);
        hudBaseline = hud * 0.62f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        canvas.drawColor(COLOR_BG);
        drawHud(canvas);
        drawGrid(canvas);
        drawFood(canvas);
        drawSnake(canvas);
        if (state != State.RUNNING) {
            drawOverlay(canvas);
        }
    }

    private void drawHud(Canvas canvas) {
        textPaint.setTextSize(15 * density);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(COLOR_SCORE);
        canvas.drawText(scoreText, gridLeft, hudBaseline, textPaint);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setColor(COLOR_BEST);
        canvas.drawText(bestText, gridLeft + cell * COLS, hudBaseline, textPaint);
    }

    private void drawGrid(Canvas canvas) {
        float gridW = cell * COLS;
        float gridH = cell * ROWS;
        float pad = 4 * density;
        rect.set(gridLeft - pad, gridTop - pad, gridLeft + gridW + pad, gridTop + gridH + pad);
        paint.setColor(COLOR_PANEL);
        canvas.drawRoundRect(rect, 10 * density, 10 * density, paint);
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                paint.setColor(((x + y) & 1) == 0 ? COLOR_CELL_A : COLOR_CELL_B);
                float l = gridLeft + x * cell;
                float t = gridTop + y * cell;
                canvas.drawRect(l, t, l + cell, t + cell, paint);
            }
        }
    }

    private void drawFood(Canvas canvas) {
        int food = engine.getFood();
        if (food < 0) {
            return;
        }
        float cx = gridLeft + (engine.xOf(food) + 0.5f) * cell;
        float cy = gridTop + (engine.yOf(food) + 0.5f) * cell;
        float r = cell * 0.38f;
        paint.setColor(COLOR_BERRY);
        canvas.drawCircle(cx, cy + cell * 0.04f, r, paint);
        paint.setColor(COLOR_BERRY_SHINE);
        canvas.drawCircle(cx - r * 0.35f, cy - r * 0.3f, r * 0.22f, paint);
        paint.setColor(COLOR_LEAF);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(Math.max(1.5f * density, cell * 0.1f));
        canvas.drawLine(cx, cy - r * 0.8f, cx - r * 0.5f, cy - r * 1.35f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawSnake(Canvas canvas) {
        int n = engine.copyBody(bodyBuffer);
        float inset = Math.max(1f, cell * 0.06f);
        float radius = cell * 0.3f;
        for (int i = n - 1; i >= 0; i--) {
            int c = bodyBuffer[i];
            float t = n > 1 ? (float) i / (n - 1) : 0f;
            paint.setColor(lerpColor(COLOR_HEAD, COLOR_TAIL, t));
            float l = gridLeft + engine.xOf(c) * cell;
            float top = gridTop + engine.yOf(c) * cell;
            rect.set(l + inset, top + inset, l + cell - inset, top + cell - inset);
            canvas.drawRoundRect(rect, radius, radius, paint);
        }
        if (n > 0) {
            drawEyes(canvas, bodyBuffer[0]);
        }
    }

    private void drawEyes(Canvas canvas, int headCell) {
        SnakeEngine.Dir dir = engine.getHeading();
        float cx = gridLeft + (engine.xOf(headCell) + 0.5f) * cell;
        float cy = gridTop + (engine.yOf(headCell) + 0.5f) * cell;
        float fx = dir.dx;
        float fy = dir.dy;
        float px = -fy;
        float py = fx;
        for (int side = -1; side <= 1; side += 2) {
            float ex = cx + fx * cell * 0.18f + px * side * cell * 0.2f;
            float ey = cy + fy * cell * 0.18f + py * side * cell * 0.2f;
            paint.setColor(COLOR_EYE);
            canvas.drawCircle(ex, ey, cell * 0.13f, paint);
            paint.setColor(COLOR_PUPIL);
            canvas.drawCircle(ex + fx * cell * 0.04f, ey + fy * cell * 0.04f, cell * 0.065f, paint);
        }
    }

    private void drawOverlay(Canvas canvas) {
        rect.set(gridLeft, gridTop, gridLeft + cell * COLS, gridTop + cell * ROWS);
        paint.setColor(COLOR_DIM);
        canvas.drawRoundRect(rect, 6 * density, 6 * density, paint);
        float cx = rect.centerX();
        float cy = rect.centerY();
        Context c = getContext();
        switch (state) {
            case READY:
                drawLine(canvas, c.getString(R.string.snake_title), cx, cy - 44 * density, 30, COLOR_SCORE);
                drawLine(canvas, c.getString(R.string.snake_hint_swipe), cx, cy, 15, COLOR_TEXT);
                drawLine(canvas, c.getString(R.string.snake_tap_start), cx, cy + 28 * density, 14, COLOR_HINT);
                break;
            case PAUSED:
                drawLine(canvas, c.getString(R.string.snake_paused), cx, cy - 6 * density, 28, COLOR_BEST);
                drawLine(canvas, c.getString(R.string.snake_tap_resume), cx, cy + 28 * density, 14, COLOR_HINT);
                break;
            case GAME_OVER:
            case WON:
                int titleRes = state == State.WON ? R.string.snake_win : R.string.snake_game_over;
                drawLine(canvas, c.getString(titleRes), cx, cy - 52 * density, 28,
                        state == State.WON ? COLOR_SCORE : COLOR_LOSE);
                drawLine(canvas, scoreText, cx, cy - 12 * density, 18, COLOR_TEXT);
                if (newBest) {
                    drawLine(canvas, c.getString(R.string.snake_new_best), cx, cy + 16 * density, 16, COLOR_SCORE);
                }
                drawLine(canvas, c.getString(R.string.snake_tap_retry), cx, cy + 50 * density, 14, COLOR_HINT);
                break;
            default:
                break;
        }
    }

    private void drawLine(Canvas canvas, String text, float cx, float baseline, float sizeDp, int color) {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(sizeDp * density);
        textPaint.setColor(color);
        canvas.drawText(text, cx, baseline, textPaint);
    }

    private static int lerpColor(int from, int to, float t) {
        int a = channel(from, to, t, 24);
        int r = channel(from, to, t, 16);
        int g = channel(from, to, t, 8);
        int b = channel(from, to, t, 0);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int channel(int from, int to, float t, int shift) {
        int f = (from >>> shift) & 0xFF;
        int o = (to >>> shift) & 0xFF;
        return (int) (f + (o - f) * t);
    }
}

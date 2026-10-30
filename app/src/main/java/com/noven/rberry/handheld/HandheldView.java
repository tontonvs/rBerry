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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ru.playsoftware.j2meloader.R;

/**
 * The rBerry handheld console: a light body with a black display frame, a D-pad, four face
 * buttons, PICK and PLAY pills, two shoulder buttons and a speaker grille.
 *
 * <p>The game goes in the display slot ({@link #setScreen}). Button presses come out through
 * {@link KeyListener}, so any game, built-in or the J2ME engine, can sit in the same body. The
 * layout (portrait or landscape) comes from {@link HandheldGeometry}; this class only draws it
 * and tracks fingers. Everything is drawn in code, so there are no image files to load.</p>
 */
public class HandheldView extends ViewGroup {

    /** Receives every button press and release. Called on the UI thread. */
    public interface KeyListener {
        void onKey(@NonNull GameKey key, boolean down);
    }

    // ---- palette (console body is light; the buttons use the rBerry accents) -------------------
    static final int BODY = 0xFFEDEBF1;
    private static final int BEZEL = 0xFF0B0710;
    private static final int SHADOW = 0x26140A1E;
    private static final int PLATE = 0xFFE5E2EA;

    private static final int DPAD = 0xFF2F2338;
    private static final int DPAD_HI = 0xFF3C2D47;
    private static final int DPAD_ON = 0xFF5A416E;
    private static final int DPAD_DIMPLE = 0xFF241A2B;
    private static final int ARROW = 0xFF6A5877;
    private static final int ARROW_ON = 0xFFFF6FA5;

    private static final int FACE_LABEL = 0xFF2A1630;
    /** Index order matches {@link HandheldGeometry#FACE_KEYS}: X cyan, Y green, A orange, B yellow. */
    private static final int[] FACE_COLOR = {0xFF2ED8F0, 0xFF34F07C, 0xFFFFA552, 0xFFFFD83A};
    private static final String[] FACE_TEXT = {"X", "Y", "A", "B"};

    private static final int PILL = 0xFF4B4252;
    private static final int PILL_HI = 0xFF665B6E;
    private static final int PILL_LIP = 0xFF2E2733;
    private static final int CAPTION = 0xFF8A8493;

    private static final int SHOULDER = 0xFFDAD6E0;
    private static final int SHOULDER_ON = 0xFFC7C1D1;
    private static final int SHOULDER_LIP = 0xFFB9B3C4;
    private static final int SHOULDER_TEXT = 0xFF7B7584;

    private static final int GRILLE_DARK = 0xFFB4AFBD;
    private static final int GRILLE_LIGHT = 0xFFFFFFFF;
    private static final int BRAND = 0xFFC80A4E;

    private static final int KEY_COUNT = GameKey.values().length;
    private static final int MAX_POINTERS = 32;

    private final HandheldGeometry geo = new HandheldGeometry();
    private final FrameLayout screenHost;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Path bezelPath = new Path();
    private final Path screenPath = new Path();
    private final Path ringPath = new Path();
    private final Path arrow = new Path();
    private final int[] faceLip = new int[4];
    private final float[] radii = new float[8];

    private final int[] keyCount = new int[KEY_COUNT];
    private final int[] pointerMask = new int[MAX_POINTERS];
    private int touchMask;
    private int externalMask;
    private boolean diagonals;
    private KeyListener listener;
    private BootView bootView;

    private final String pickLabel;
    private final String playLabel;

    public HandheldView(@NonNull Context context) {
        this(context, null);
    }

    public HandheldView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        pickLabel = context.getString(R.string.handheld_pick);
        playLabel = context.getString(R.string.handheld_play);
        screenHost = new FrameLayout(context);
        screenHost.setBackgroundColor(0xFF1E0B22);
        addView(screenHost, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        for (int i = 0; i < 4; i++) {
            faceLip[i] = shade(FACE_COLOR[i], 0.70f);
        }
        text.setTextAlign(Paint.Align.CENTER);
    }

    // ---- public API ---------------------------------------------------------------------------

    /** Puts a game view in the display slot, replacing the previous one. */
    public void setScreen(@NonNull View view) {
        for (int i = screenHost.getChildCount() - 1; i >= 0; i--) {
            View child = screenHost.getChildAt(i);
            if (child != bootView) {
                screenHost.removeViewAt(i);
            }
        }
        screenHost.addView(view, 0, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    public void setKeyListener(@Nullable KeyListener listener) {
        this.listener = listener;
    }

    /**
     * Four-way D-pad (default) or eight-way. J2ME games use the diagonals (keys 1, 3, 7 and 9),
     * simple built-in games like Snake do not.
     */
    public void setDiagonalsEnabled(boolean enabled) {
        diagonals = enabled;
    }

    /** True while the RetroBerry start-up animation is showing. */
    public boolean isBooting() {
        return bootView != null;
    }

    /** Plays the start-up animation in the display, then runs {@code onDone} (may be null). */
    public void showBoot(@Nullable Runnable onDone) {
        if (bootView != null) {
            return;
        }
        BootView boot = new BootView(getContext());
        bootView = boot;
        boot.setOnFinished(() -> {
            screenHost.removeView(boot);
            if (bootView == boot) {
                bootView = null;
            }
            if (onDone != null) {
                onDone.run();
            }
        });
        screenHost.addView(boot, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    /** Shows a button as pressed without sending anything. Used for keyboard and gamepad input. */
    public void showPressed(@NonNull GameKey key, boolean down) {
        int before = externalMask;
        externalMask = down ? (externalMask | key.mask()) : (externalMask & ~key.mask());
        if (before != externalMask) {
            invalidate();
        }
    }

    /**
     * Makes the window match the console: light system bars with dark icons over the body colour.
     * Call after {@code super.onCreate()} in the activity that hosts a handheld.
     */
    public static void applyWindowChrome(@NonNull ComponentActivity activity) {
        Window window = activity.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(BODY));
        EdgeToEdge.enable(activity, SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                SystemBarStyle.light(BODY, BODY));
    }

    // ---- layout ---------------------------------------------------------------------------------

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        int h = MeasureSpec.getSize(heightSpec);
        setMeasuredDimension(w, h);
        geo.layout(w, h, getPaddingLeft(), getPaddingTop(), getPaddingRight(), getPaddingBottom());
        int sw = Math.max(1, Math.round(geo.screen.width()));
        int sh = Math.max(1, Math.round(geo.screen.height()));
        screenHost.measure(MeasureSpec.makeMeasureSpec(sw, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(sh, MeasureSpec.EXACTLY));
        rebuildPaths();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int right, int b) {
        screenHost.layout(Math.round(geo.screen.left), Math.round(geo.screen.top),
                Math.round(geo.screen.right), Math.round(geo.screen.bottom));
    }

    private void rebuildPaths() {
        bezelPath.rewind();
        putRadii(geo.bezelRadius, geo.bezelBigRadius);
        r.set(geo.bezel.left, geo.bezel.top, geo.bezel.right, geo.bezel.bottom);
        bezelPath.addRoundRect(r, radii, Path.Direction.CW);

        screenPath.rewind();
        putRadii(geo.screenRadius, geo.screenBigRadius);
        r.set(geo.screen.left, geo.screen.top, geo.screen.right, geo.screen.bottom);
        screenPath.addRoundRect(r, radii, Path.Direction.CW);

        // Frame colour poured over the corners of the (square) game view, so the display shows
        // the same shape as the bezel, with smooth edges and the big curve at the bottom right.
        ringPath.rewind();
        ringPath.setFillType(Path.FillType.EVEN_ODD);
        ringPath.addPath(bezelPath);
        ringPath.addPath(screenPath);

        float half = geo.dpadSize / 2f;
        float armHalf = geo.dpadArm / 2f;
        arrow.rewind();
        arrow.moveTo(0f, -half * 0.82f);
        arrow.lineTo(-armHalf * 0.42f, -half * 0.54f);
        arrow.lineTo(armHalf * 0.42f, -half * 0.54f);
        arrow.close();
    }

    /** Corner radii in Path order: top left, top right, bottom right (the big curve), bottom left. */
    private void putRadii(float small, float big) {
        radii[0] = small;
        radii[1] = small;
        radii[2] = small;
        radii[3] = small;
        radii[4] = big;
        radii[5] = big;
        radii[6] = small;
        radii[7] = small;
    }

    // ---- drawing ----------------------------------------------------------------------------------

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        canvas.drawColor(BODY);
        int pressed = touchMask | externalMask;
        drawBrand(canvas);
        drawShoulder(canvas, geo.shoulderL, "L", (pressed & GameKey.L.mask()) != 0);
        drawShoulder(canvas, geo.shoulderR, "R", (pressed & GameKey.R.mask()) != 0);
        drawBezel(canvas);
        drawDpad(canvas, pressed);
        drawFaceButtons(canvas, pressed);
        drawPill(canvas, geo.pick, pickLabel, (pressed & GameKey.PICK.mask()) != 0);
        drawPill(canvas, geo.play, playLabel, (pressed & GameKey.PLAY.mask()) != 0);
        drawGrille(canvas);
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(BEZEL);
        canvas.drawPath(ringPath, fill);
    }

    private void drawBrand(Canvas canvas) {
        if (Float.isNaN(geo.brandX)) {
            return;
        }
        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC));
        text.setTextSize(geo.brandSize);
        text.setColor(BRAND);
        canvas.drawText("rBerry", geo.brandX, geo.brandY, text);
    }

    private void drawBezel(Canvas canvas) {
        float lift = geo.content.width() * 0.006f;
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(SHADOW);
        canvas.save();
        canvas.translate(0f, lift);
        canvas.drawPath(bezelPath, fill);
        canvas.restore();
        fill.setColor(BEZEL);
        canvas.drawPath(bezelPath, fill);
    }

    private void drawShoulder(Canvas canvas, HandheldGeometry.Box box, String label, boolean pressed) {
        float lip = box.height() * 0.2f;
        float dy = pressed ? lip * 0.8f : 0f;
        float radius = box.height() / 2f;
        fill.setStyle(Paint.Style.FILL);
        if (!pressed) {
            fill.setColor(SHOULDER_LIP);
            r.set(box.left, box.top + lip, box.right, box.bottom + lip);
            canvas.drawRoundRect(r, radius, radius, fill);
        }
        fill.setColor(pressed ? SHOULDER_ON : SHOULDER);
        r.set(box.left, box.top + dy, box.right, box.bottom + dy);
        canvas.drawRoundRect(r, radius, radius, fill);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextSize(box.height() * 0.62f);
        text.setColor(SHOULDER_TEXT);
        canvas.drawText(label, box.centerX(), box.centerY() + dy + box.height() * 0.22f, text);
    }

    private void drawCross(Canvas canvas, float x, float y, float half, float arm, float radius) {
        r.set(x - half, y - arm, x + half, y + arm);
        canvas.drawRoundRect(r, radius, radius, fill);
        r.set(x - arm, y - half, x + arm, y + half);
        canvas.drawRoundRect(r, radius, radius, fill);
    }

    private void drawDpad(Canvas canvas, int pressed) {
        float x = geo.dpadX;
        float y = geo.dpadY;
        float half = geo.dpadSize / 2f;
        float arm = geo.dpadArm / 2f;
        float radius = geo.dpadArm * 0.28f;
        float lift = geo.dpadArm * 0.10f;
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(SHADOW);
        drawCross(canvas, x, y + lift, half, arm, radius);
        fill.setColor(DPAD);
        drawCross(canvas, x, y, half, arm, radius);
        fill.setColor(DPAD_HI);
        drawCross(canvas, x, y - lift * 0.35f, half * 0.94f, arm * 0.80f, radius * 0.8f);

        fill.setColor(DPAD_ON);
        if ((pressed & GameKey.UP.mask()) != 0) {
            r.set(x - arm, y - half, x + arm, y - arm * 0.2f);
            canvas.drawRoundRect(r, radius, radius, fill);
        }
        if ((pressed & GameKey.DOWN.mask()) != 0) {
            r.set(x - arm, y + arm * 0.2f, x + arm, y + half);
            canvas.drawRoundRect(r, radius, radius, fill);
        }
        if ((pressed & GameKey.LEFT.mask()) != 0) {
            r.set(x - half, y - arm, x - arm * 0.2f, y + arm);
            canvas.drawRoundRect(r, radius, radius, fill);
        }
        if ((pressed & GameKey.RIGHT.mask()) != 0) {
            r.set(x + arm * 0.2f, y - arm, x + half, y + arm);
            canvas.drawRoundRect(r, radius, radius, fill);
        }

        drawArrow(canvas, 0f, (pressed & GameKey.UP.mask()) != 0);
        drawArrow(canvas, 90f, (pressed & GameKey.RIGHT.mask()) != 0);
        drawArrow(canvas, 180f, (pressed & GameKey.DOWN.mask()) != 0);
        drawArrow(canvas, 270f, (pressed & GameKey.LEFT.mask()) != 0);

        fill.setColor(DPAD_DIMPLE);
        canvas.drawCircle(x, y, arm * 0.62f, fill);
        fill.setColor(DPAD_HI);
        canvas.drawCircle(x, y - arm * 0.06f, arm * 0.46f, fill);
    }

    private void drawArrow(Canvas canvas, float degrees, boolean on) {
        fill.setColor(on ? ARROW_ON : ARROW);
        canvas.save();
        canvas.translate(geo.dpadX, geo.dpadY);
        canvas.rotate(degrees);
        canvas.drawPath(arrow, fill);
        canvas.restore();
    }

    private void drawFaceButtons(Canvas canvas, int pressed) {
        // A faint diamond plate under the four buttons.
        float reach = geo.faceOffset + geo.faceHalf * 0.9f;
        float side = reach * 1.4142f;
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(PLATE);
        canvas.save();
        canvas.rotate(45f, geo.faceX, geo.faceY);
        r.set(geo.faceX - side / 2f, geo.faceY - side / 2f, geo.faceX + side / 2f, geo.faceY + side / 2f);
        canvas.drawRoundRect(r, side * 0.14f, side * 0.14f, fill);
        canvas.restore();

        float half = geo.faceHalf;
        float lip = half * 0.22f;
        float radius = half * 0.38f;
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextSize(half * 1.0f);
        for (int i = 0; i < 4; i++) {
            boolean on = (pressed & HandheldGeometry.FACE_KEYS[i].mask()) != 0;
            float cx = geo.faceCenterX(i);
            float cy = geo.faceCenterY(i);
            float dy = on ? lip * 0.8f : 0f;
            if (!on) {
                fill.setColor(faceLip[i]);
                r.set(cx - half, cy - half + lip, cx + half, cy + half + lip);
                canvas.drawRoundRect(r, radius, radius, fill);
            }
            fill.setColor(FACE_COLOR[i]);
            r.set(cx - half, cy - half + dy, cx + half, cy + half + dy);
            canvas.drawRoundRect(r, radius, radius, fill);
            fill.setColor(0x44FFFFFF);
            r.set(cx - half * 0.78f, cy - half * 0.80f + dy, cx + half * 0.78f, cy - half * 0.28f + dy);
            canvas.drawRoundRect(r, radius * 0.7f, radius * 0.7f, fill);
            text.setColor(FACE_LABEL);
            canvas.drawText(FACE_TEXT[i], cx, cy + dy + half * 0.35f, text);
        }
    }

    private void drawPill(Canvas canvas, HandheldGeometry.Box box, String label, boolean pressed) {
        float lip = box.height() * 0.26f;
        float dy = pressed ? lip * 0.8f : 0f;
        float radius = box.height() / 2f;
        fill.setStyle(Paint.Style.FILL);
        if (!pressed) {
            fill.setColor(PILL_LIP);
            r.set(box.left, box.top + lip, box.right, box.bottom + lip);
            canvas.drawRoundRect(r, radius, radius, fill);
        }
        fill.setColor(PILL);
        r.set(box.left, box.top + dy, box.right, box.bottom + dy);
        canvas.drawRoundRect(r, radius, radius, fill);
        fill.setColor(PILL_HI);
        r.set(box.left + radius * 0.6f, box.top + box.height() * 0.14f + dy,
                box.right - radius * 0.6f, box.top + box.height() * 0.40f + dy);
        canvas.drawRoundRect(r, radius * 0.3f, radius * 0.3f, fill);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextSize(geo.pillLabelSize);
        text.setColor(CAPTION);
        text.setLetterSpacing(0.08f);
        canvas.drawText(label, box.centerX(), geo.pillLabelY, text);
        text.setLetterSpacing(0f);
    }

    private void drawGrille(Canvas canvas) {
        HandheldGeometry.Box g = geo.grille;
        if (g.width() <= 0f || g.height() <= 0f) {
            return;
        }
        int slits = 6;
        float slant = Math.min(g.height() * 0.45f, g.width() * 0.3f);
        float step = (g.width() - slant) / (slits - 1);
        float width = Math.max(1.5f, Math.min(g.height() * 0.075f, step * 0.4f));
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeWidth(width);
        for (int i = 0; i < slits; i++) {
            float x = g.left + i * step;
            stroke.setColor(GRILLE_LIGHT);
            canvas.drawLine(x + width * 0.8f, g.top + width * 0.8f, x + slant + width * 0.8f,
                    g.bottom + width * 0.8f, stroke);
            stroke.setColor(GRILLE_DARK);
            canvas.drawLine(x, g.top, x + slant, g.bottom, stroke);
        }
    }

    private static int shade(int color, float factor) {
        int rr = Math.round(Color.red(color) * factor);
        int gg = Math.round(Color.green(color) * factor);
        int bb = Math.round(Color.blue(color) * factor);
        return Color.rgb(rr, gg, bb);
    }

    // ---- touch --------------------------------------------------------------------------------------

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (bootView != null) {
            // Any touch on the body skips the start-up animation.
            if (action == MotionEvent.ACTION_DOWN) {
                bootView.skip();
            }
            return true;
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int index = event.getActionIndex();
                setPointer(event.getPointerId(index), geo.hitMask(event.getX(index), event.getY(index), diagonals));
                return true;
            }
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    setPointer(event.getPointerId(i), geo.hitMask(event.getX(i), event.getY(i), diagonals));
                }
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                setPointer(event.getPointerId(event.getActionIndex()), 0);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                releaseAll();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    /** Updates what one finger is pressing, firing a press or release for every key that changed. */
    private void setPointer(int id, int mask) {
        if (id < 0 || id >= MAX_POINTERS || pointerMask[id] == mask) {
            return;
        }
        int old = pointerMask[id];
        pointerMask[id] = mask;
        int released = old & ~mask;
        int added = mask & ~old;
        boolean newPress = false;
        for (int k = 0; k < KEY_COUNT; k++) {
            int bit = 1 << k;
            if ((released & bit) != 0) {
                if (--keyCount[k] <= 0) {
                    keyCount[k] = 0;
                    touchMask &= ~bit;
                    fire(k, false);
                }
            }
            if ((added & bit) != 0) {
                if (keyCount[k]++ == 0) {
                    touchMask |= bit;
                    fire(k, true);
                    newPress = true;
                }
            }
        }
        if (newPress) {
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
        invalidate();
    }

    private void releaseAll() {
        for (int id = 0; id < MAX_POINTERS; id++) {
            if (pointerMask[id] != 0) {
                setPointer(id, 0);
            }
        }
    }

    private void fire(int keyIndex, boolean down) {
        if (listener != null) {
            listener.onKey(GameKey.values()[keyIndex], down);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        releaseAll();
        super.onDetachedFromWindow();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (!hasWindowFocus) {
            // A pulled-down notification shade can swallow the finger-up; never leave a key stuck.
            releaseAll();
        }
    }
}

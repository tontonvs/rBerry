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

/**
 * Where everything sits on the handheld body, for a given window size. Pure Java with no Android
 * classes, so the layout rules can be unit tested on any JVM.
 *
 * <p>Two layouts share the same parts:</p>
 * <ul>
 *   <li><b>Portrait</b> (taller than wide): display on top, then a thin strip under it with the
 *       L and R buttons and the small "123" button between them, then the D-pad on the left, the
 *       face-button diamond on the right, the PICK and PLAY pills at the bottom centre and the
 *       speaker grille at the bottom left.</li>
 *   <li><b>Landscape</b> (wider than tall): display in the middle, a side column on each side. The
 *       D-pad is in the left column with L below it, the face buttons are in the right column
 *       with R below them and "123" above them. PICK and PLAY sit under the display.</li>
 * </ul>
 *
 * <p>The number pad is not a separate part: it takes the place of the face buttons while it is
 * open, in the same spot in both layouts, so the D-pad stays available.</p>
 *
 * <p>Every size is a fraction of the window, so the body scales to any phone or tablet. When the
 * controls would not fit, the whole control cluster shrinks together instead of overlapping.
 * In compact mode (used while the on-screen keyboard is up) the controls go away and the display
 * takes the whole window.</p>
 */
public final class HandheldGeometry {

    /** A plain float rectangle, so this class does not depend on android.graphics.RectF. */
    public static final class Box {
        public float left;
        public float top;
        public float right;
        public float bottom;

        void set(float l, float t, float r, float b) {
            left = l;
            top = t;
            right = r;
            bottom = b;
        }

        public float width() {
            return right - left;
        }

        public float height() {
            return bottom - top;
        }

        public float centerX() {
            return (left + right) / 2f;
        }

        public float centerY() {
            return (top + bottom) / 2f;
        }

        public boolean contains(float x, float y) {
            return x >= left && x < right && y >= top && y < bottom;
        }

        /** True when the two boxes share any area (touching edges do not count). */
        public boolean overlaps(Box o) {
            return left < o.right && o.left < right && top < o.bottom && o.top < bottom;
        }
    }

    /** Order of the face buttons: top, left, right, bottom. */
    public static final GameKey[] FACE_KEYS = {GameKey.X, GameKey.Y, GameKey.A, GameKey.B};
    private static final float[] FACE_DX = {0f, -1f, 1f, 0f};
    private static final float[] FACE_DY = {-1f, 0f, 0f, 1f};

    public static final int PAD_COLS = 3;
    public static final int PAD_ROWS = 4;
    private static final int PAD_FIRST = GameKey.NUM1.ordinal();

    /** A D-pad touch within this fraction of the pad size from the centre presses nothing. */
    private static final float DEAD_ZONE = 0.10f;
    /** With diagonals on, a touch this close to an axis (about 22.5 degrees) is one direction only. */
    private static final float DIAGONAL_RATIO = 0.38f;
    private static final float OFFSCREEN = -100000f;

    // ---- input ------------------------------------------------------------------------------

    /** Set before {@link #layout}: no controls, the display takes the whole window. */
    public boolean compact;

    // ---- results of layout() ---------------------------------------------------------------

    public boolean landscape;
    /** The whole usable area (the window minus system bars). */
    public final Box content = new Box();

    /** Black frame around the display. The bottom right corner is the big curve. */
    public final Box bezel = new Box();
    /** Where the game view goes, inside the bezel. */
    public final Box screen = new Box();
    public float bezelRadius;
    public float bezelBigRadius;
    public float screenRadius;
    public float screenBigRadius;

    public float dpadX;
    public float dpadY;
    public float dpadSize;
    public float dpadArm;
    private float dpadHitHalf;

    public float faceX;
    public float faceY;
    public float faceOffset;
    public float faceHalf;
    private float faceReach;

    /** What you see. */
    public final Box pick = new Box();
    public final Box play = new Box();
    public final Box shoulderL = new Box();
    public final Box shoulderR = new Box();
    public final Box pad = new Box();
    /** Where a touch counts. Bigger than what you see, and never overlapping each other. */
    public final Box pickHit = new Box();
    public final Box playHit = new Box();
    public final Box shoulderLHit = new Box();
    public final Box shoulderRHit = new Box();
    public final Box padHit = new Box();

    /** The number pad, a 3 x 4 grid that fills this box. */
    public final Box keypad = new Box();

    /** Baseline of the PICK and PLAY captions. */
    public float pillLabelY;
    public float pillLabelSize;
    /** Where the "rBerry" wordmark goes (baseline centre). It takes the place of the "123" button. */
    public float brandX;
    public float brandY;
    public float brandSize;

    public final Box grille = new Box();

    // ---- layout -----------------------------------------------------------------------------

    /**
     * Computes the whole layout.
     *
     * @param w         full view width in pixels
     * @param h         full view height in pixels
     * @param padLeft   system bar insets; the body is laid out inside them
     * @param padTop    see padLeft
     * @param padRight  see padLeft
     * @param padBottom see padLeft
     */
    public void layout(float w, float h, float padLeft, float padTop, float padRight, float padBottom) {
        float cw = Math.max(1f, w - padLeft - padRight);
        float ch = Math.max(1f, h - padTop - padBottom);
        content.set(padLeft, padTop, padLeft + cw, padTop + ch);
        landscape = cw > ch;
        if (compact) {
            layoutCompact(padLeft, padTop, cw, ch);
        } else if (landscape) {
            layoutLandscape(padLeft, padTop, cw, ch);
        } else {
            layoutPortrait(padLeft, padTop, cw, ch);
        }
    }

    private void layoutCompact(float ox, float oy, float w, float h) {
        float m = 0.012f * Math.min(w, h);
        bezel.set(ox + m, oy + m, ox + w - m, oy + h - m);
        setScreenFromBezel(0.012f * Math.min(w, h), 0.02f * Math.min(w, h), 0.08f * Math.min(w, h));
        moveControlsAway();
    }

    private void moveControlsAway() {
        dpadX = OFFSCREEN;
        dpadY = OFFSCREEN;
        dpadSize = 0f;
        dpadArm = 0f;
        dpadHitHalf = 0f;
        faceX = OFFSCREEN;
        faceY = OFFSCREEN;
        faceOffset = 0f;
        faceHalf = 0f;
        faceReach = 0f;
        Box[] boxes = {pick, play, shoulderL, shoulderR, pad, pickHit, playHit, shoulderLHit,
                shoulderRHit, padHit, keypad, grille};
        for (Box b : boxes) {
            b.set(OFFSCREEN, OFFSCREEN, OFFSCREEN, OFFSCREEN);
        }
        pillLabelY = OFFSCREEN;
        pillLabelSize = 0f;
        brandX = OFFSCREEN;
        brandY = OFFSCREEN;
        brandSize = 0f;
    }

    private void layoutPortrait(float ox, float oy, float w, float h) {
        float m = 0.025f * w;
        float topMargin = 0.03f * w;
        float stripH = 0.115f * w;
        float fullBottom = stripH + 0.80f * w;
        float bottomH = Math.min(fullBottom, 0.55f * h);
        // s < 1 only when the window is too short for the full-size controls.
        float s = bottomH / fullBottom;
        float yStrip = oy + h - bottomH;
        float stripS = stripH * s;
        float y0 = yStrip + stripS;

        bezel.set(ox + m, oy + topMargin, ox + w - m, Math.max(yStrip, oy + topMargin + 1f));
        setScreenFromBezel(0.018f * w, 0.03f * w, 0.15f * w);

        // The strip under the display: L, the "123" button and R.
        float tabH = 0.065f * w * s;
        float tabTop = yStrip + (stripS - tabH) / 2f;
        shoulderL.set(ox + 0.06f * w, tabTop, ox + 0.34f * w, tabTop + tabH);
        shoulderR.set(ox + 0.66f * w, tabTop, ox + 0.94f * w, tabTop + tabH);
        pad.set(ox + 0.39f * w, tabTop, ox + 0.61f * w, tabTop + tabH);
        shoulderLHit.set(ox, yStrip, ox + 0.365f * w, yStrip + stripS);
        padHit.set(ox + 0.365f * w, yStrip, ox + 0.635f * w, yStrip + stripS);
        shoulderRHit.set(ox + 0.635f * w, yStrip, ox + w, yStrip + stripS);
        brandX = ox + 0.5f * w;
        brandSize = 0.042f * w * s;
        brandY = yStrip + stripS / 2f + brandSize * 0.35f;

        dpadSize = 0.36f * w * s;
        dpadArm = 0.125f * w * s;
        dpadX = ox + 0.25f * w;
        dpadY = y0 + 0.30f * w * s;
        dpadHitHalf = dpadSize * 0.58f;

        faceX = ox + 0.75f * w;
        faceY = y0 + 0.28f * w * s;
        faceOffset = 0.12f * w * s;
        faceHalf = 0.072f * w * s;
        faceReach = faceOffset + faceHalf * 1.15f;

        keypad.set(ox + 0.50f * w, y0 + 0.01f * w * s, ox + 0.98f * w, y0 + 0.575f * w * s);

        float pw = 0.18f * w * s;
        float ph = 0.05f * w * s;
        float py = y0 + 0.66f * w * s;
        float pickX = ox + 0.385f * w;
        float playX = ox + 0.615f * w;
        pick.set(pickX - pw / 2, py - ph / 2, pickX + pw / 2, py + ph / 2);
        play.set(playX - pw / 2, py - ph / 2, playX + pw / 2, py + ph / 2);
        float hitHalf = 0.11f * w;
        float hitTop = py - 0.07f * w * s;
        float hitBottom = py + 0.085f * w * s;
        pickHit.set(pickX - hitHalf, hitTop, pickX + hitHalf - 0.005f * w, hitBottom);
        playHit.set(playX - hitHalf + 0.005f * w, hitTop, playX + hitHalf, hitBottom);
        pillLabelSize = 0.034f * w * s;
        pillLabelY = py + ph / 2 + pillLabelSize * 1.35f;

        grille.set(ox + 0.06f * w, y0 + 0.58f * w * s, ox + 0.24f * w, y0 + 0.72f * w * s);
    }

    private void layoutLandscape(float ox, float oy, float w, float h) {
        float m = 0.03f * h;
        // Side columns shrink together on very wide windows so the display keeps its room.
        float ls = Math.min(1f, 0.24f * w / (0.54f * h));
        float sw = 0.54f * h * ls;
        float barH = 0.15f * h;

        bezel.set(ox + sw + m, oy + m, ox + w - sw - m, oy + h - barH);
        setScreenFromBezel(0.018f * h, 0.03f * h, 0.15f * h);

        float leftX = ox + sw / 2f;
        float rightX = ox + w - sw / 2f;
        float midY = oy + 0.55f * h;

        // L and R sit at the bottom of their columns, under the thumbs; "123" is above the faces.
        float tabW = 0.36f * h * ls;
        float tabH = 0.07f * h;
        float tabTop = oy + 0.855f * h;
        shoulderL.set(leftX - tabW / 2, tabTop, leftX + tabW / 2, tabTop + tabH);
        shoulderR.set(rightX - tabW / 2, tabTop, rightX + tabW / 2, tabTop + tabH);
        shoulderLHit.set(ox, oy + 0.80f * h, ox + sw, oy + h);
        shoulderRHit.set(ox + w - sw, oy + 0.80f * h, ox + w, oy + h);
        float padW = 0.28f * h * ls;
        float padTop = oy + 0.045f * h;
        pad.set(rightX - padW / 2, padTop, rightX + padW / 2, padTop + tabH);
        padHit.set(ox + w - sw, oy, ox + w, oy + 0.19f * h);
        brandX = rightX;
        brandSize = 0.05f * h * ls;
        brandY = padTop + tabH / 2f + brandSize * 0.35f;

        dpadSize = 0.42f * h * ls;
        dpadArm = 0.145f * h * ls;
        dpadX = leftX;
        dpadY = midY;
        dpadHitHalf = dpadSize * 0.58f;

        faceX = rightX;
        faceY = midY;
        faceOffset = 0.13f * h * ls;
        faceHalf = 0.077f * h * ls;
        faceReach = faceOffset + faceHalf * 1.15f;

        keypad.set(ox + w - sw, oy + 0.20f * h, ox + w, oy + 0.80f * h);

        float cx = bezel.centerX();
        float off = Math.min(0.20f * h, 0.23f * bezel.width());
        float pw = Math.min(0.30f * h, off * 1.5f);
        float ph = 0.055f * h;
        float py = oy + h - barH * 0.5f - 0.012f * h;
        pick.set(cx - off - pw / 2, py - ph / 2, cx - off + pw / 2, py + ph / 2);
        play.set(cx + off - pw / 2, py - ph / 2, cx + off + pw / 2, py + ph / 2);
        float hitHalf = off * 0.85f;
        pickHit.set(cx - off - hitHalf, oy + h - barH, cx - off + hitHalf, oy + h);
        playHit.set(cx + off - hitHalf, oy + h - barH, cx + off + hitHalf, oy + h);
        pillLabelSize = 0.04f * h;
        pillLabelY = py + ph / 2 + pillLabelSize * 1.25f;

        float gx = ox + 0.07f * h * ls;
        grille.set(gx, oy + 0.04f * h, gx + 0.20f * h * ls, oy + 0.14f * h);
    }

    private void setScreenFromBezel(float inset, float smallRadius, float bigRadius) {
        bezelRadius = smallRadius;
        bezelBigRadius = bigRadius;
        screen.set(bezel.left + inset, bezel.top + inset, bezel.right - inset, bezel.bottom - inset);
        screenRadius = Math.max(1f, smallRadius - inset * 0.5f);
        screenBigRadius = Math.max(1f, bigRadius - inset);
    }

    // ---- face buttons and number pad ---------------------------------------------------------

    /** Centre of face button {@code index} (0 top, 1 left, 2 right, 3 bottom). */
    public float faceCenterX(int index) {
        return faceX + FACE_DX[index] * faceOffset;
    }

    public float faceCenterY(int index) {
        return faceY + FACE_DY[index] * faceOffset;
    }

    /** The key on number pad cell {@code index}: 1 2 3 / 4 5 6 / 7 8 9 / * 0 #. */
    public static GameKey keypadKey(int index) {
        return GameKey.values()[PAD_FIRST + index];
    }

    /** Fills {@code out} with number pad cell {@code index} (0 to 11, left to right, top to bottom). */
    public void keypadCell(int index, Box out) {
        float cw = keypad.width() / PAD_COLS;
        float chh = keypad.height() / PAD_ROWS;
        int col = index % PAD_COLS;
        int row = index / PAD_COLS;
        out.set(keypad.left + col * cw, keypad.top + row * chh,
                keypad.left + (col + 1) * cw, keypad.top + (row + 1) * chh);
    }

    // ---- touch -------------------------------------------------------------------------------

    /** Same as {@link #hitMask(float, float, boolean, boolean)} with the number pad closed. */
    public int hitMask(float x, float y, boolean diagonals) {
        return hitMask(x, y, diagonals, false);
    }

    /**
     * Which keys a finger at (x, y) presses, as a bit set of {@link GameKey#mask()} values.
     *
     * @param diagonals when true the D-pad reports two directions near the corners (J2ME games use
     *                  diagonals); when false it is a strict four-way pad
     * @param padOpen   when true the number pad is where the face buttons normally are
     */
    public int hitMask(float x, float y, boolean diagonals, boolean padOpen) {
        if (shoulderLHit.contains(x, y)) {
            return GameKey.L.mask();
        }
        if (shoulderRHit.contains(x, y)) {
            return GameKey.R.mask();
        }
        if (padHit.contains(x, y)) {
            return GameKey.PAD.mask();
        }
        if (pickHit.contains(x, y)) {
            return GameKey.PICK.mask();
        }
        if (playHit.contains(x, y)) {
            return GameKey.PLAY.mask();
        }
        if (Math.abs(x - dpadX) <= dpadHitHalf && Math.abs(y - dpadY) <= dpadHitHalf) {
            return dpadMask(x - dpadX, y - dpadY, diagonals);
        }
        if (padOpen) {
            if (!keypad.contains(x, y)) {
                return 0;
            }
            int col = Math.min(PAD_COLS - 1, (int) ((x - keypad.left) / (keypad.width() / PAD_COLS)));
            int row = Math.min(PAD_ROWS - 1, (int) ((y - keypad.top) / (keypad.height() / PAD_ROWS)));
            return keypadKey(row * PAD_COLS + col).mask();
        }
        int best = -1;
        float bestDist = faceReach * faceReach;
        for (int i = 0; i < 4; i++) {
            float dx = x - faceCenterX(i);
            float dy = y - faceCenterY(i);
            float d = dx * dx + dy * dy;
            if (Math.abs(x - faceX) <= faceReach && Math.abs(y - faceY) <= faceReach && d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best < 0 ? 0 : FACE_KEYS[best].mask();
    }

    private int dpadMask(float dx, float dy, boolean diagonals) {
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < dpadSize * DEAD_ZONE) {
            return 0;
        }
        if (!diagonals) {
            if (Math.abs(dx) > Math.abs(dy)) {
                return dx > 0 ? GameKey.RIGHT.mask() : GameKey.LEFT.mask();
            }
            return dy > 0 ? GameKey.DOWN.mask() : GameKey.UP.mask();
        }
        int mask = 0;
        if (Math.abs(dx) / dist >= DIAGONAL_RATIO) {
            mask |= dx > 0 ? GameKey.RIGHT.mask() : GameKey.LEFT.mask();
        }
        if (Math.abs(dy) / dist >= DIAGONAL_RATIO) {
            mask |= dy > 0 ? GameKey.DOWN.mask() : GameKey.UP.mask();
        }
        return mask;
    }
}

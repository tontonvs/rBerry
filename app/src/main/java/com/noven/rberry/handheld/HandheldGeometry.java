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
 *   <li><b>Portrait</b> (taller than wide): display on top, a thin strip above it holding the two
 *       shoulder buttons, then D-pad on the left, face-button diamond on the right, the PICK and
 *       PLAY pills at the bottom centre and the speaker grille at the bottom left.</li>
 *   <li><b>Landscape</b> (wider than tall): display in the middle, a side column on each
 *       side (shoulder button on top, D-pad on the left, face buttons on the right), PICK and PLAY
 *       under the display and the grille at the bottom left.</li>
 * </ul>
 *
 * <p>Every size is a fraction of the window, so the body scales to any phone or tablet. When the
 * controls would not fit, the whole control cluster shrinks together instead of overlapping.</p>
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

    /** A D-pad touch within this fraction of the pad size from the centre presses nothing. */
    private static final float DEAD_ZONE = 0.10f;
    /** With diagonals on, a touch this close to an axis (about 22.5 degrees) is one direction only. */
    private static final float DIAGONAL_RATIO = 0.38f;

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
    /** Where a touch counts. Bigger than what you see, and never overlapping each other. */
    public final Box pickHit = new Box();
    public final Box playHit = new Box();
    public final Box shoulderLHit = new Box();
    public final Box shoulderRHit = new Box();

    /** Baseline of the PICK and PLAY captions. */
    public float pillLabelY;
    public float pillLabelSize;
    /** Where the "rBerry" wordmark goes (centre), or NaN when there is no room for it. */
    public float brandX = Float.NaN;
    public float brandY = Float.NaN;
    public float brandSize;

    public final Box grille = new Box();

    // ---- layout -----------------------------------------------------------------------------

    /**
     * Computes the whole layout.
     *
     * @param w full view width in pixels
     * @param h full view height in pixels
     * @param padLeft, padTop, padRight, padBottom system bar insets; the body is laid out
     *                                              inside them
     */
    public void layout(float w, float h, float padLeft, float padTop, float padRight, float padBottom) {
        float cw = Math.max(1f, w - padLeft - padRight);
        float ch = Math.max(1f, h - padTop - padBottom);
        content.set(padLeft, padTop, padLeft + cw, padTop + ch);
        landscape = cw > ch;
        brandX = Float.NaN;
        brandY = Float.NaN;
        if (landscape) {
            layoutLandscape(padLeft, padTop, cw, ch);
        } else {
            layoutPortrait(padLeft, padTop, cw, ch);
        }
    }

    private void layoutPortrait(float ox, float oy, float w, float h) {
        float m = 0.025f * w;
        float shoulderStrip = 0.10f * w;
        float controlsH = Math.min(0.80f * w, 0.5f * h);
        // s < 1 only when the window is too short for the full-size controls.
        float s = controlsH / (0.80f * w);
        float y0 = oy + h - controlsH;

        float bezelTop = oy + shoulderStrip;
        bezel.set(ox + m, bezelTop, ox + w - m, Math.max(y0, bezelTop + 1f));
        setScreenFromBezel(0.018f * w, 0.03f * w, 0.15f * w);

        // Shoulder buttons float in the strip above the display.
        float tabH = 0.055f * w;
        float tabTop = oy + 0.022f * w;
        shoulderL.set(ox + 0.06f * w, tabTop, ox + 0.34f * w, tabTop + tabH);
        shoulderR.set(ox + 0.66f * w, tabTop, ox + 0.94f * w, tabTop + tabH);
        shoulderLHit.set(ox, oy, ox + 0.42f * w, bezelTop);
        shoulderRHit.set(ox + 0.58f * w, oy, ox + w, bezelTop);
        brandX = ox + 0.5f * w;
        brandSize = 0.042f * w;
        brandY = tabTop + tabH * 0.5f + brandSize * 0.35f;

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

        float tabW = 0.36f * h * ls;
        float tabH = 0.07f * h;
        float tabTop = oy + 0.03f * h;
        shoulderL.set(leftX - tabW / 2, tabTop, leftX + tabW / 2, tabTop + tabH);
        shoulderR.set(rightX - tabW / 2, tabTop, rightX + tabW / 2, tabTop + tabH);
        shoulderLHit.set(ox, oy, ox + sw, oy + 0.19f * h);
        shoulderRHit.set(ox + w - sw, oy, ox + w, oy + 0.19f * h);

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

        grille.set(ox + 0.04f * h * ls + 0.04f * h, oy + 0.82f * h, ox + 0.04f * h * ls + 0.04f * h + 0.20f * h * ls,
                oy + 0.96f * h);
    }

    private void setScreenFromBezel(float inset, float smallRadius, float bigRadius) {
        bezelRadius = smallRadius;
        bezelBigRadius = bigRadius;
        screen.set(bezel.left + inset, bezel.top + inset, bezel.right - inset, bezel.bottom - inset);
        screenRadius = Math.max(1f, smallRadius - inset * 0.5f);
        screenBigRadius = Math.max(1f, bigRadius - inset);
    }

    // ---- touch -------------------------------------------------------------------------------

    /** Centre of face button {@code index} (0 top, 1 left, 2 right, 3 bottom). */
    public float faceCenterX(int index) {
        return faceX + FACE_DX[index] * faceOffset;
    }

    public float faceCenterY(int index) {
        return faceY + FACE_DY[index] * faceOffset;
    }

    /**
     * Which keys a finger at (x, y) presses, as a bit set of {@link GameKey#mask()} values.
     *
     * @param diagonals when true the D-pad reports two directions near the corners (J2ME games use
     *                  diagonals); when false it is a strict four-way pad
     */
    public int hitMask(float x, float y, boolean diagonals) {
        if (shoulderLHit.contains(x, y)) {
            return GameKey.L.mask();
        }
        if (shoulderRHit.contains(x, y)) {
            return GameKey.R.mask();
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

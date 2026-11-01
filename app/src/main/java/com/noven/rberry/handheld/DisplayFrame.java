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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Where the game picture sits inside the console's display window, as fractions of the window
 * (0 = left/top edge, 1 = right/bottom edge). The default is the whole window. The player can drag
 * it smaller or move it around; this class holds the numbers and the drag rules, so they can be
 * tested without a phone.
 */
public final class DisplayFrame {

    /** The frame never gets thinner than this share of the window. */
    public static final float MIN_SIZE = 0.30f;

    public static final int EDGE_LEFT = 1;
    public static final int EDGE_TOP = 2;
    public static final int EDGE_RIGHT = 4;
    public static final int EDGE_BOTTOM = 8;
    public static final int MOVE = 16;

    public float left;
    public float top = 0f;
    public float right = 1f;
    public float bottom = 1f;
    /** True = stretch the picture to fill the frame; false = keep its shape. */
    public boolean stretch;

    public DisplayFrame() {
        reset();
    }

    public DisplayFrame(@NonNull DisplayFrame other) {
        set(other);
    }

    /** Whole window, picture keeps its shape. */
    public void reset() {
        left = 0f;
        top = 0f;
        right = 1f;
        bottom = 1f;
        stretch = false;
    }

    public void set(@NonNull DisplayFrame other) {
        left = other.left;
        top = other.top;
        right = other.right;
        bottom = other.bottom;
        stretch = other.stretch;
    }

    public boolean coversWindow() {
        return left <= 0.0005f && top <= 0.0005f && right >= 0.9995f && bottom >= 0.9995f;
    }

    public boolean sameAs(@NonNull DisplayFrame o) {
        return Math.abs(left - o.left) < 0.0005f && Math.abs(top - o.top) < 0.0005f
                && Math.abs(right - o.right) < 0.0005f && Math.abs(bottom - o.bottom) < 0.0005f
                && stretch == o.stretch;
    }

    /**
     * Which part of the frame a finger at ({@code x}, {@code y}) grabs, in window pixels:
     * one or two edges (a corner is two), {@link #MOVE} for the inside, 0 for outside.
     * {@code grab} is how far from an edge still counts as the edge.
     */
    public int hit(float x, float y, float width, float height, float grab) {
        float l = left * width;
        float t = top * height;
        float r = right * width;
        float b = bottom * height;
        if (x < l - grab || x > r + grab || y < t - grab || y > b + grab) {
            return 0;
        }
        int mask = 0;
        // A very small frame can have both edges within reach: take the nearer one.
        float dl = Math.abs(x - l);
        float dr = Math.abs(x - r);
        if (dl <= grab || dr <= grab) {
            mask |= dl <= dr ? EDGE_LEFT : EDGE_RIGHT;
        }
        float dt = Math.abs(y - t);
        float db = Math.abs(y - b);
        if (dt <= grab || db <= grab) {
            mask |= dt <= db ? EDGE_TOP : EDGE_BOTTOM;
        }
        return mask != 0 ? mask : MOVE;
    }

    /**
     * Sets this frame to {@code start} dragged by ({@code dx}, {@code dy}) window pixels. Always
     * measured from where the drag began, so the result never drifts.
     */
    public void drag(@NonNull DisplayFrame start, int grabbed, float dx, float dy, float width, float height) {
        set(start);
        if (width <= 0f || height <= 0f || grabbed == 0) {
            return;
        }
        float fx = dx / width;
        float fy = dy / height;
        if (grabbed == MOVE) {
            float w = start.right - start.left;
            float h = start.bottom - start.top;
            left = clamp(start.left + fx, 0f, 1f - w);
            top = clamp(start.top + fy, 0f, 1f - h);
            right = left + w;
            bottom = top + h;
            return;
        }
        if ((grabbed & EDGE_LEFT) != 0) {
            left = clamp(start.left + fx, 0f, start.right - MIN_SIZE);
        }
        if ((grabbed & EDGE_RIGHT) != 0) {
            right = clamp(start.right + fx, start.left + MIN_SIZE, 1f);
        }
        if ((grabbed & EDGE_TOP) != 0) {
            top = clamp(start.top + fy, 0f, start.bottom - MIN_SIZE);
        }
        if ((grabbed & EDGE_BOTTOM) != 0) {
            bottom = clamp(start.bottom + fy, start.top + MIN_SIZE, 1f);
        }
    }

    /** One line for saving: "left,top,right,bottom,stretch". */
    @NonNull
    public String encode() {
        return left + "," + top + "," + right + "," + bottom + "," + (stretch ? 1 : 0);
    }

    /** Reads {@link #encode()} back. Anything broken gives the default whole-window frame. */
    @NonNull
    public static DisplayFrame decode(@Nullable String text) {
        DisplayFrame frame = new DisplayFrame();
        if (text == null) {
            return frame;
        }
        try {
            String[] p = text.split(",");
            if (p.length != 5) {
                return frame;
            }
            float l = Float.parseFloat(p[0]);
            float t = Float.parseFloat(p[1]);
            float r = Float.parseFloat(p[2]);
            float b = Float.parseFloat(p[3]);
            boolean ok = l >= 0f && t >= 0f && r <= 1f && b <= 1f
                    && r - l >= MIN_SIZE - 0.001f && b - t >= MIN_SIZE - 0.001f;
            if (ok) {
                frame.left = l;
                frame.top = t;
                frame.right = r;
                frame.bottom = b;
                frame.stretch = !"0".equals(p[4]);
            }
        } catch (NumberFormatException ignored) {
            frame.reset();
        }
        return frame;
    }

    private static float clamp(float v, float lo, float hi) {
        if (hi < lo) {
            return lo;
        }
        return v < lo ? lo : (v > hi ? hi : v);
    }
}

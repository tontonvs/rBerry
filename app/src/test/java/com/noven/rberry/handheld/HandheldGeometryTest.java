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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.noven.rberry.handheld.HandheldGeometry.Box;

import org.junit.Test;

public class HandheldGeometryTest {

    /** Typical phones and tablets, width x height in pixels. */
    private static final int[][] PORTRAIT = {
            {360, 640}, {360, 780}, {411, 891}, {393, 852}, {320, 480}, {720, 1560}, {600, 960},
            {800, 1280}, {360, 360}, {360, 420}, {1080, 2400}
    };
    private static final int[][] LANDSCAPE = {
            {640, 360}, {780, 360}, {891, 411}, {852, 393}, {480, 320}, {960, 600}, {1280, 800},
            {400, 360}, {440, 360}, {2400, 1080}, {1920, 1200}
    };

    private static HandheldGeometry make(int w, int h) {
        HandheldGeometry g = new HandheldGeometry();
        g.layout(w, h, 0, 0, 0, 0);
        return g;
    }

    private static Box box(float l, float t, float r, float b) {
        Box x = new Box();
        x.left = l;
        x.top = t;
        x.right = r;
        x.bottom = b;
        return x;
    }

    private static Box dpadBox(HandheldGeometry g) {
        float half = g.dpadSize * 0.58f;
        return box(g.dpadX - half, g.dpadY - half, g.dpadX + half, g.dpadY + half);
    }

    private static Box faceBox(HandheldGeometry g) {
        float reach = g.faceOffset + g.faceHalf * 1.15f;
        return box(g.faceX - reach, g.faceY - reach, g.faceX + reach, g.faceY + reach);
    }

    private static boolean inside(Box inner, Box outer) {
        return inner.left >= outer.left - 0.5f && inner.top >= outer.top - 0.5f
                && inner.right <= outer.right + 0.5f && inner.bottom <= outer.bottom + 0.5f;
    }

    private static int[][] all() {
        int[][] out = new int[PORTRAIT.length + LANDSCAPE.length][];
        System.arraycopy(PORTRAIT, 0, out, 0, PORTRAIT.length);
        System.arraycopy(LANDSCAPE, 0, out, PORTRAIT.length, LANDSCAPE.length);
        return out;
    }

    @Test
    public void orientationFollowsWindowShape() {
        for (int[] s : PORTRAIT) {
            assertFalse(s[0] + "x" + s[1], make(s[0], s[1]).landscape);
        }
        for (int[] s : LANDSCAPE) {
            assertTrue(s[0] + "x" + s[1], make(s[0], s[1]).landscape);
        }
    }

    @Test
    public void everyPartStaysInsideTheWindow() {
        for (int[] s : all()) {
            HandheldGeometry g = make(s[0], s[1]);
            String name = s[0] + "x" + s[1];
            assertTrue(name + " bezel", inside(g.bezel, g.content));
            assertTrue(name + " screen", inside(g.screen, g.bezel));
            assertTrue(name + " dpad", inside(dpadBox(g), g.content));
            assertTrue(name + " face", inside(faceBox(g), g.content));
            assertTrue(name + " keypad", inside(g.keypad, g.content));
            assertTrue(name + " pick", inside(g.pickHit, g.content));
            assertTrue(name + " play", inside(g.playHit, g.content));
            assertTrue(name + " L", inside(g.shoulderLHit, g.content));
            assertTrue(name + " R", inside(g.shoulderRHit, g.content));
            assertTrue(name + " pad", inside(g.padHit, g.content));
            assertTrue(name + " pick visual", inside(g.pick, g.pickHit));
            assertTrue(name + " play visual", inside(g.play, g.playHit));
            assertTrue(name + " L visual", inside(g.shoulderL, g.shoulderLHit));
            assertTrue(name + " R visual", inside(g.shoulderR, g.shoulderRHit));
            assertTrue(name + " pad visual", inside(g.pad, g.padHit));
            assertTrue(name + " grille", inside(g.grille, g.content));
            assertTrue(name + " pill label", g.pillLabelY <= g.content.bottom);
            assertTrue(name + " brand", g.brandX > 0 && g.brandY < g.content.bottom);
        }
    }

    @Test
    public void touchAreasNeverOverlap() {
        for (int[] s : all()) {
            HandheldGeometry g = make(s[0], s[1]);
            String name = s[0] + "x" + s[1];
            Box[] areas = {g.shoulderLHit, g.shoulderRHit, g.padHit, g.pickHit, g.playHit, dpadBox(g),
                    faceBox(g), g.bezel};
            String[] names = {"L", "R", "pad", "pick", "play", "dpad", "face", "bezel"};
            for (int i = 0; i < areas.length; i++) {
                for (int j = i + 1; j < areas.length; j++) {
                    assertFalse(name + ": " + names[i] + " overlaps " + names[j], areas[i].overlaps(areas[j]));
                }
            }
            // The number pad replaces the face buttons, so it may share their spot but nothing else.
            Box[] others = {g.shoulderLHit, g.shoulderRHit, g.padHit, g.pickHit, g.playHit, dpadBox(g), g.bezel};
            String[] otherNames = {"L", "R", "pad", "pick", "play", "dpad", "bezel"};
            for (int i = 0; i < others.length; i++) {
                assertFalse(name + ": keypad overlaps " + otherNames[i], g.keypad.overlaps(others[i]));
            }
            assertTrue(name + ": keypad covers the face buttons",
                    g.keypad.left <= faceBox(g).left + 1f && g.keypad.right >= faceBox(g).right - 1f
                            && g.keypad.top <= g.faceY - g.faceOffset && g.keypad.bottom >= g.faceY + g.faceOffset);
        }
    }

    @Test
    public void grilleDoesNotCoverAnyButton() {
        for (int[] s : all()) {
            HandheldGeometry g = make(s[0], s[1]);
            String name = s[0] + "x" + s[1];
            Box[] areas = {dpadBox(g), faceBox(g), g.keypad, g.pickHit, g.playHit, g.shoulderLHit,
                    g.shoulderRHit, g.padHit, g.bezel};
            String[] names = {"dpad", "face", "keypad", "pick", "play", "L", "R", "pad", "bezel"};
            for (int i = 0; i < areas.length; i++) {
                assertFalse(name + " " + names[i], g.grille.overlaps(areas[i]));
            }
        }
    }

    @Test
    public void faceButtonsDoNotTouchEachOther() {
        for (int[] s : all()) {
            HandheldGeometry g = make(s[0], s[1]);
            for (int i = 0; i < 4; i++) {
                for (int j = i + 1; j < 4; j++) {
                    float dx = g.faceCenterX(i) - g.faceCenterX(j);
                    float dy = g.faceCenterY(i) - g.faceCenterY(j);
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    assertTrue(s[0] + "x" + s[1] + " " + i + "," + j, dist >= 2 * g.faceHalf + 1f);
                }
            }
        }
    }

    @Test
    public void everyButtonCentreHitsItsOwnKey() {
        for (int[] s : all()) {
            HandheldGeometry g = make(s[0], s[1]);
            String name = s[0] + "x" + s[1];
            assertEquals(name + " L", GameKey.L.mask(), g.hitMask(g.shoulderL.centerX(), g.shoulderL.centerY(), false));
            assertEquals(name + " R", GameKey.R.mask(), g.hitMask(g.shoulderR.centerX(), g.shoulderR.centerY(), false));
            assertEquals(name + " PAD", GameKey.PAD.mask(), g.hitMask(g.pad.centerX(), g.pad.centerY(), false));
            assertEquals(name + " PICK", GameKey.PICK.mask(), g.hitMask(g.pick.centerX(), g.pick.centerY(), false));
            assertEquals(name + " PLAY", GameKey.PLAY.mask(), g.hitMask(g.play.centerX(), g.play.centerY(), false));
            for (int i = 0; i < 4; i++) {
                assertEquals(name + " face " + i, HandheldGeometry.FACE_KEYS[i].mask(),
                        g.hitMask(g.faceCenterX(i), g.faceCenterY(i), false));
            }
        }
    }

    @Test
    public void numberPadCellsTileTheKeypadAndHitTheRightKeys() {
        GameKey[] expected = {GameKey.NUM1, GameKey.NUM2, GameKey.NUM3, GameKey.NUM4, GameKey.NUM5,
                GameKey.NUM6, GameKey.NUM7, GameKey.NUM8, GameKey.NUM9, GameKey.STAR, GameKey.NUM0,
                GameKey.POUND};
        for (int[] s : all()) {
            HandheldGeometry g = make(s[0], s[1]);
            String name = s[0] + "x" + s[1];
            float area = 0f;
            Box cell = new Box();
            for (int i = 0; i < 12; i++) {
                assertEquals(name + " key " + i, expected[i], HandheldGeometry.keypadKey(i));
                g.keypadCell(i, cell);
                area += cell.width() * cell.height();
                assertTrue(name + " cell inside " + i, inside(cell, g.keypad));
                assertEquals(name + " hit " + i, expected[i].mask(), g.hitMask(cell.centerX(), cell.centerY(), true, true));
            }
            assertTrue(name + " tiles", Math.abs(area - g.keypad.width() * g.keypad.height()) < 1f);
            // With the pad closed, the same spots are face buttons or empty, never digits.
            int digits = 0;
            for (int i = 0; i < 12; i++) {
                g.keypadCell(i, cell);
                digits |= g.hitMask(cell.centerX(), cell.centerY(), false, false) & expected[i].mask();
            }
            assertEquals(name + " closed pad has no digits", 0, digits);
            // The D-pad still works while the pad is open.
            assertEquals(name + " dpad up", GameKey.UP.mask(),
                    g.hitMask(g.dpadX, g.dpadY - g.dpadSize * 0.4f, false, true));
        }
    }

    @Test
    public void faceKeysAreLaidOutAsADiamond() {
        HandheldGeometry g = make(360, 780);
        // X top, Y left, A right, B bottom (index order top, left, right, bottom).
        assertTrue(g.faceCenterY(0) < g.faceY && g.faceCenterX(0) == g.faceX);
        assertTrue(g.faceCenterX(1) < g.faceX && g.faceCenterY(1) == g.faceY);
        assertTrue(g.faceCenterX(2) > g.faceX && g.faceCenterY(2) == g.faceY);
        assertTrue(g.faceCenterY(3) > g.faceY && g.faceCenterX(3) == g.faceX);
        assertEquals(GameKey.X, HandheldGeometry.FACE_KEYS[0]);
        assertEquals(GameKey.Y, HandheldGeometry.FACE_KEYS[1]);
        assertEquals(GameKey.A, HandheldGeometry.FACE_KEYS[2]);
        assertEquals(GameKey.B, HandheldGeometry.FACE_KEYS[3]);
    }

    @Test
    public void shouldersSitBelowTheDisplay() {
        for (int[] s : PORTRAIT) {
            HandheldGeometry g = make(s[0], s[1]);
            String name = s[0] + "x" + s[1];
            // Directly under the display, above the D-pad and face buttons.
            assertTrue(name + " L below display", g.shoulderL.top >= g.bezel.bottom);
            assertTrue(name + " R below display", g.shoulderR.top >= g.bezel.bottom);
            assertTrue(name + " pad below display", g.pad.top >= g.bezel.bottom);
            assertTrue(name + " L above dpad", g.shoulderL.bottom <= g.dpadY - g.dpadSize / 2f);
            assertTrue(name + " R above faces", g.shoulderR.bottom <= g.faceY - g.faceOffset - g.faceHalf);
            // The strip is in the lower part of the window, within thumb reach.
            assertTrue(name + " lower part", g.shoulderL.centerY() > g.content.height() * 0.4f);
            // Nothing is left above the display but a thin margin.
            assertTrue(name + " thin top margin", g.bezel.top <= g.content.top + 0.04f * g.content.width());
        }
        for (int[] s : LANDSCAPE) {
            HandheldGeometry g = make(s[0], s[1]);
            String name = s[0] + "x" + s[1];
            assertTrue(name + " L under dpad", g.shoulderL.top >= g.dpadY + g.dpadSize / 2f);
            assertTrue(name + " R under faces", g.shoulderR.top >= g.faceY + g.faceOffset + g.faceHalf);
        }
    }

    @Test
    public void dpadIsFourWayWhenDiagonalsAreOff() {
        for (int[] s : all()) {
            HandheldGeometry g = make(s[0], s[1]);
            float d = g.dpadSize * 0.4f;
            assertEquals(GameKey.UP.mask(), g.hitMask(g.dpadX, g.dpadY - d, false));
            assertEquals(GameKey.DOWN.mask(), g.hitMask(g.dpadX, g.dpadY + d, false));
            assertEquals(GameKey.LEFT.mask(), g.hitMask(g.dpadX - d, g.dpadY, false));
            assertEquals(GameKey.RIGHT.mask(), g.hitMask(g.dpadX + d, g.dpadY, false));
            // A corner press picks the stronger axis, never two keys.
            int corner = g.hitMask(g.dpadX + d, g.dpadY - d * 0.8f, false);
            assertEquals(GameKey.RIGHT.mask(), corner);
        }
    }

    @Test
    public void dpadDiagonalsReportTwoKeysOnlyNearCorners() {
        HandheldGeometry g = make(360, 780);
        float d = g.dpadSize * 0.4f;
        int upRight = GameKey.UP.mask() | GameKey.RIGHT.mask();
        int downLeft = GameKey.DOWN.mask() | GameKey.LEFT.mask();
        assertEquals(upRight, g.hitMask(g.dpadX + d, g.dpadY - d, true));
        assertEquals(downLeft, g.hitMask(g.dpadX - d, g.dpadY + d, true));
        // Close to an axis (within about 22 degrees) stays a single direction.
        assertEquals(GameKey.RIGHT.mask(), g.hitMask(g.dpadX + d, g.dpadY - d * 0.2f, true));
        assertEquals(GameKey.UP.mask(), g.hitMask(g.dpadX + d * 0.2f, g.dpadY - d, true));
    }

    @Test
    public void dpadCentreAndEmptyBodyPressNothing() {
        HandheldGeometry g = make(360, 780);
        assertEquals(0, g.hitMask(g.dpadX, g.dpadY, false));
        assertEquals(0, g.hitMask(g.dpadX + g.dpadSize * 0.03f, g.dpadY, true));
        // Middle of the display and the empty strip between the two clusters.
        assertEquals(0, g.hitMask(g.screen.centerX(), g.screen.centerY(), false));
        assertEquals(0, g.hitMask(g.content.centerX(), g.dpadY, false));
        // Outside the window.
        assertEquals(0, g.hitMask(-5, -5, false));
        assertEquals(0, g.hitMask(5000, 5000, false));
    }

    @Test
    public void screenIsCentredInLandscape() {
        for (int[] s : LANDSCAPE) {
            HandheldGeometry g = make(s[0], s[1]);
            assertTrue(s[0] + "x" + s[1], Math.abs(g.screen.centerX() - g.content.centerX()) < 1f);
            assertTrue(g.dpadX < g.screen.left);
            assertTrue(g.faceX > g.screen.right);
        }
    }

    @Test
    public void displayIsBigEnoughToPlay() {
        // A 20:9 phone on its side must leave a usable display.
        HandheldGeometry g = make(780, 360);
        assertTrue(g.screen.width() >= 0.45f * 780);
        assertTrue(g.screen.height() >= 0.65f * 360);
        // Portrait: the display is still the biggest part of the body.
        HandheldGeometry p = make(360, 780);
        assertTrue(p.screen.height() >= 0.5f * 780);
        assertTrue(p.screen.width() >= 0.9f * 360);
        // And it keeps a 240x320 game at a comfortable size: at least 1x on a 360 dp wide phone.
        assertTrue(p.screen.width() >= 240f && p.screen.height() >= 320f);
    }

    @Test
    public void screenKeepsTheBigCurveAtTheBottomRight() {
        for (int[] s : all()) {
            HandheldGeometry g = make(s[0], s[1]);
            assertTrue(g.bezelBigRadius > g.bezelRadius * 3f);
            assertTrue(g.screenBigRadius > g.screenRadius * 2f);
            // The curve must fit inside the display.
            assertTrue(g.bezelBigRadius * 2f < Math.min(g.bezel.width(), g.bezel.height()));
        }
    }

    @Test
    public void systemBarInsetsPushTheBodyInward() {
        HandheldGeometry g = new HandheldGeometry();
        g.layout(360, 780, 0, 40, 0, 48);
        assertTrue(g.content.top == 40 && g.content.bottom == 780 - 48);
        assertTrue(inside(g.bezel, g.content));
        assertTrue(inside(g.pickHit, g.content));
        assertTrue(inside(g.shoulderLHit, g.content));
        // Touches on the status bar and navigation bar press nothing.
        assertEquals(0, g.hitMask(60, 10, false));
        assertEquals(0, g.hitMask(g.pick.centerX(), 780 - 10, false));
        // Landscape with a camera cut-out on the left.
        g.layout(780, 360, 48, 0, 0, 0);
        assertTrue(g.content.left == 48);
        assertTrue(inside(dpadBox(g), g.content));
        assertTrue(inside(g.bezel, g.content));
    }

    @Test
    public void compactModeGivesTheDisplayTheWholeWindowAndRemovesTheButtons() {
        for (int[] s : all()) {
            HandheldGeometry g = new HandheldGeometry();
            g.compact = true;
            g.layout(s[0], s[1], 0, 0, 0, 0);
            String name = s[0] + "x" + s[1];
            assertTrue(name + " big display", g.screen.width() >= 0.93f * Math.min(s[0], s[1]) - 1f
                    && g.screen.width() >= 0.9f * s[0] && g.screen.height() >= 0.9f * s[1]);
            assertTrue(name + " inside", inside(g.screen, g.content));
            for (int i = 0; i < 40; i++) {
                float x = s[0] * (i % 8 + 0.5f) / 8f;
                float y = s[1] * (i / 8 + 0.5f) / 5f;
                assertEquals(name + " no keys", 0, g.hitMask(x, y, true, true));
            }
            // Turning compact off again restores the controls.
            g.compact = false;
            g.layout(s[0], s[1], 0, 0, 0, 0);
            assertTrue(name + " controls back", g.dpadSize > 0 && g.pickHit.width() > 0);
        }
    }

    @Test
    public void tinyOrZeroSizedWindowsDoNotBreak() {
        int[][] sizes = {{0, 0}, {1, 1}, {20, 20}, {50, 400}, {400, 50}};
        for (int[] s : sizes) {
            for (int c = 0; c < 2; c++) {
                HandheldGeometry g = new HandheldGeometry();
                g.compact = c == 1;
                g.layout(s[0], s[1], 0, 0, 0, 0);
                assertFalse(Float.isNaN(g.screen.left) || Float.isNaN(g.screen.right));
                assertTrue(g.screen.width() >= 0f && g.screen.height() >= 0f);
                assertFalse(Float.isNaN(g.dpadX) || Float.isNaN(g.faceX));
                g.hitMask(1, 1, true, true);
            }
        }
    }

    @Test
    public void shortWindowsShrinkTheControlsInsteadOfOverlapping() {
        // Split screen: wide-ish but short portrait window.
        HandheldGeometry g = make(360, 420);
        assertTrue(g.dpadSize < 0.36f * 360);
        assertTrue(g.screen.height() > 0);
        assertFalse(dpadBox(g).overlaps(faceBox(g)));
        assertFalse(g.pickHit.overlaps(g.playHit));
    }

    @Test
    public void touchAreasAreBigEnoughForThumbs() {
        // 48 dp at the 2.625 density of a 1080 px wide phone is 126 px. A touch area may be a bit
        // smaller than that if it is wide, but never tiny.
        float minTouch = 48f * 2.625f;
        for (int[] s : new int[][]{{1080, 2400}, {2400, 1080}}) {
            HandheldGeometry g = make(s[0], s[1]);
            String name = s[0] + "x" + s[1];
            assertTrue(name + " L", g.shoulderLHit.height() >= minTouch * 0.8f && g.shoulderLHit.width() >= minTouch);
            assertTrue(name + " R", g.shoulderRHit.height() >= minTouch * 0.8f && g.shoulderRHit.width() >= minTouch);
            assertTrue(name + " pad", g.padHit.height() >= minTouch * 0.8f && g.padHit.width() >= minTouch);
            assertTrue(name + " pick", g.pickHit.height() >= minTouch * 0.8f && g.pickHit.width() >= minTouch);
            assertTrue(name + " play", g.playHit.height() >= minTouch * 0.8f && g.playHit.width() >= minTouch);
            assertTrue(name + " face", 2 * g.faceHalf >= minTouch * 0.8f);
            assertTrue(name + " dpad", g.dpadSize / 3f >= minTouch * 0.8f);
            Box cell = new Box();
            g.keypadCell(0, cell);
            assertTrue(name + " pad key", cell.width() >= minTouch * 0.8f && cell.height() >= minTouch * 0.8f);
        }
    }
}

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

import org.junit.Test;

public class DisplayFrameTest {

    private static final float W = 400f;
    private static final float H = 600f;
    private static final float GRAB = 30f;

    private static DisplayFrame frame(float l, float t, float r, float b) {
        DisplayFrame f = new DisplayFrame();
        f.left = l;
        f.top = t;
        f.right = r;
        f.bottom = b;
        return f;
    }

    @Test
    public void startsAsWholeWindowKeepingShape() {
        DisplayFrame f = new DisplayFrame();
        assertTrue(f.coversWindow());
        assertFalse(f.stretch);
    }

    @Test
    public void grabbingEdgesCornersInsideAndOutside() {
        DisplayFrame f = frame(0.2f, 0.2f, 0.8f, 0.8f); // 80,120 .. 320,480
        assertEquals(DisplayFrame.EDGE_LEFT, f.hit(80f, 300f, W, H, GRAB));
        assertEquals(DisplayFrame.EDGE_RIGHT, f.hit(325f, 300f, W, H, GRAB));
        assertEquals(DisplayFrame.EDGE_TOP, f.hit(200f, 125f, W, H, GRAB));
        assertEquals(DisplayFrame.EDGE_BOTTOM, f.hit(200f, 470f, W, H, GRAB));
        assertEquals(DisplayFrame.EDGE_LEFT | DisplayFrame.EDGE_TOP, f.hit(85f, 125f, W, H, GRAB));
        assertEquals(DisplayFrame.EDGE_RIGHT | DisplayFrame.EDGE_BOTTOM, f.hit(318f, 478f, W, H, GRAB));
        assertEquals(DisplayFrame.MOVE, f.hit(200f, 300f, W, H, GRAB));
        assertEquals(0, f.hit(5f, 5f, W, H, GRAB));
    }

    @Test
    public void wholeWindowFrameStillHasGrabbableEdges() {
        DisplayFrame f = new DisplayFrame();
        assertEquals(DisplayFrame.EDGE_RIGHT, f.hit(395f, 300f, W, H, GRAB));
        assertEquals(DisplayFrame.EDGE_LEFT | DisplayFrame.EDGE_BOTTOM, f.hit(3f, 598f, W, H, GRAB));
        assertEquals(DisplayFrame.MOVE, f.hit(200f, 300f, W, H, GRAB));
    }

    @Test
    public void aTinyFrameTakesTheNearerEdge() {
        DisplayFrame f = frame(0.40f, 0.40f, 0.70f, 0.70f); // 160..280 wide, 240..420
        assertEquals(DisplayFrame.EDGE_LEFT, f.hit(170f, 330f, W, H, 200f) & DisplayFrame.EDGE_LEFT);
        assertEquals(0, f.hit(170f, 330f, W, H, 200f) & DisplayFrame.EDGE_RIGHT);
    }

    @Test
    public void dragRightEdgeResizes() {
        DisplayFrame start = new DisplayFrame();
        DisplayFrame f = new DisplayFrame();
        f.drag(start, DisplayFrame.EDGE_RIGHT, -100f, 0f, W, H);
        assertEquals(0.75f, f.right, 0.001f);
        assertEquals(0f, f.left, 0.001f);
        assertEquals(1f, f.bottom, 0.001f);
    }

    @Test
    public void cannotShrinkBelowMinimumOrPastTheWindow() {
        DisplayFrame start = new DisplayFrame();
        DisplayFrame f = new DisplayFrame();
        f.drag(start, DisplayFrame.EDGE_LEFT | DisplayFrame.EDGE_TOP, 5000f, 5000f, W, H);
        assertEquals(1f - DisplayFrame.MIN_SIZE, f.left, 0.001f);
        assertEquals(1f - DisplayFrame.MIN_SIZE, f.top, 0.001f);
        f.drag(start, DisplayFrame.EDGE_LEFT | DisplayFrame.EDGE_TOP, -5000f, -5000f, W, H);
        assertEquals(0f, f.left, 0.001f);
        assertEquals(0f, f.top, 0.001f);
        DisplayFrame small = frame(0.2f, 0.2f, 0.6f, 0.6f);
        f.drag(small, DisplayFrame.EDGE_RIGHT | DisplayFrame.EDGE_BOTTOM, 9999f, 9999f, W, H);
        assertEquals(1f, f.right, 0.001f);
        assertEquals(1f, f.bottom, 0.001f);
    }

    @Test
    public void movingKeepsSizeAndStaysInsideTheWindow() {
        DisplayFrame start = frame(0.1f, 0.1f, 0.6f, 0.5f);
        DisplayFrame f = new DisplayFrame();
        f.drag(start, DisplayFrame.MOVE, 100f, 150f, W, H);
        assertEquals(0.5f, f.right - f.left, 0.001f);
        assertEquals(0.4f, f.bottom - f.top, 0.001f);
        assertEquals(0.35f, f.left, 0.001f);
        assertEquals(0.35f, f.top, 0.001f);
        f.drag(start, DisplayFrame.MOVE, 99999f, 99999f, W, H);
        assertEquals(1f, f.right, 0.001f);
        assertEquals(1f, f.bottom, 0.001f);
        assertEquals(0.5f, f.right - f.left, 0.001f);
        f.drag(start, DisplayFrame.MOVE, -99999f, -99999f, W, H);
        assertEquals(0f, f.left, 0.001f);
        assertEquals(0f, f.top, 0.001f);
    }

    @Test
    public void dragIsMeasuredFromWhereItStartedSoNothingDrifts() {
        DisplayFrame start = new DisplayFrame();
        DisplayFrame f = new DisplayFrame();
        f.drag(start, DisplayFrame.EDGE_LEFT, 100f, 0f, W, H);
        f.drag(start, DisplayFrame.EDGE_BOTTOM, 0f, -150f, W, H);
        assertEquals(0f, f.left, 0.001f);
        assertEquals(0.75f, f.bottom, 0.001f);
        assertEquals(1f, start.bottom, 0.0001f);
    }

    @Test
    public void saveAndLoadRoundTrip() {
        DisplayFrame f = frame(0.1f, 0.2f, 0.9f, 0.95f);
        f.stretch = true;
        DisplayFrame back = DisplayFrame.decode(f.encode());
        assertTrue(f.sameAs(back));
    }

    @Test
    public void brokenSavedTextGivesTheDefault() {
        String[] bad = {null, "", "1,2,3", "a,b,c,d,e", "0,0,1,1", "-1,0,1,1,0", "0,0,2,1,0",
                "0.5,0.5,0.6,0.6,0", "NaN,0,1,1,0"};
        for (String text : bad) {
            DisplayFrame f = DisplayFrame.decode(text);
            assertTrue("\"" + text + "\"", f.coversWindow() && !f.stretch);
        }
    }

    @Test
    public void resetGoesBackToTheWholeWindow() {
        DisplayFrame f = frame(0.3f, 0.3f, 0.7f, 0.7f);
        f.stretch = true;
        f.reset();
        assertTrue(f.coversWindow());
        assertFalse(f.stretch);
    }
}

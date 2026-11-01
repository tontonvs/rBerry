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

import ru.playsoftware.j2meloader.config.ProfileModel;

public class DisplayModeTest {

    private static ProfileModel messy() {
        ProfileModel p = new ProfileModel();
        p.orientation = 0;
        p.screenWidth = 176;
        p.screenHeight = 208;
        p.screenScaleType = 0;
        p.screenScaleRatio = 250;
        p.screenGravity = 4;
        p.screenFilter = false;
        p.showKeyboard = true;
        return p;
    }

    @Test
    public void portraitIsUprightAndFitted() {
        ProfileModel p = messy();
        DisplayMode.PORTRAIT.apply(p);
        assertEquals(240, p.screenWidth);
        assertEquals(320, p.screenHeight);
        assertEquals(DisplayMode.PROFILE_PORTRAIT, p.orientation);
        assertEquals(1, p.screenScaleType);
        assertEquals(100, p.screenScaleRatio);
        assertEquals(2, p.screenGravity);
        assertTrue(p.screenFilter && p.screenScaleToFit && p.screenKeepAspectRatio);
        assertFalse(p.showKeyboard);
    }

    @Test
    public void landscapeIsSideways() {
        ProfileModel p = messy();
        DisplayMode.LANDSCAPE.apply(p);
        assertEquals(320, p.screenWidth);
        assertEquals(240, p.screenHeight);
        assertEquals(DisplayMode.PROFILE_LANDSCAPE, p.orientation);
    }

    @Test
    public void profilesPickTheirModeAndEverythingElseIsPortrait() {
        ProfileModel p = new ProfileModel();
        for (int o : new int[]{0, 1, 2, 4, -1}) {
            p.orientation = o;
            assertEquals("orientation " + o, DisplayMode.PORTRAIT, DisplayMode.of(p));
        }
        p.orientation = 3;
        assertEquals(DisplayMode.LANDSCAPE, DisplayMode.of(p));
    }

    @Test
    public void applyIsRepeatableAndStable() {
        ProfileModel p = messy();
        for (DisplayMode m : DisplayMode.values()) {
            m.apply(p);
            assertEquals(m, DisplayMode.of(p));
            m.apply(p);
            assertEquals(m, DisplayMode.of(p));
            assertEquals(m.width, p.screenWidth);
        }
    }

    @Test
    public void switchingModesFlipsTheShape() {
        assertEquals(DisplayMode.LANDSCAPE, DisplayMode.PORTRAIT.other());
        assertEquals(DisplayMode.PORTRAIT, DisplayMode.LANDSCAPE.other());
        assertEquals(DisplayMode.LANDSCAPE, DisplayMode.forSize(320, 240));
        assertEquals(DisplayMode.PORTRAIT, DisplayMode.forSize(240, 320));
        assertEquals(DisplayMode.PORTRAIT, DisplayMode.forSize(240, 240));
        assertEquals(DisplayMode.PORTRAIT, DisplayMode.forSize(0, 0));
    }
}

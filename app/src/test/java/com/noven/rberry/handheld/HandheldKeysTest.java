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

import android.view.KeyEvent;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class HandheldKeysTest {

    @Test
    public void everyGameButtonMapsToAKeyExceptPickAndPad() {
        for (GameKey key : GameKey.values()) {
            int code = HandheldKeys.androidKeyCode(key);
            if (key == GameKey.PICK || key == GameKey.PAD) {
                assertEquals(key.name(), 0, code);
            } else {
                assertTrue(key.name(), code != 0);
            }
        }
    }

    @Test
    public void numberPadSendsTheMatchingDigits() {
        int[] digits = {KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4,
                KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8,
                KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_STAR, KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_POUND};
        for (int i = 0; i < 12; i++) {
            assertEquals("cell " + i, digits[i], HandheldKeys.androidKeyCode(HandheldGeometry.keypadKey(i)));
        }
    }

    @Test
    public void controlsMapToTheKeysJ2meGamesExpect() {
        assertEquals(KeyEvent.KEYCODE_DPAD_UP, HandheldKeys.androidKeyCode(GameKey.UP));
        assertEquals(KeyEvent.KEYCODE_DPAD_DOWN, HandheldKeys.androidKeyCode(GameKey.DOWN));
        assertEquals(KeyEvent.KEYCODE_DPAD_LEFT, HandheldKeys.androidKeyCode(GameKey.LEFT));
        assertEquals(KeyEvent.KEYCODE_DPAD_RIGHT, HandheldKeys.androidKeyCode(GameKey.RIGHT));
        assertEquals(KeyEvent.KEYCODE_ENTER, HandheldKeys.androidKeyCode(GameKey.A));
        assertEquals(KeyEvent.KEYCODE_ENTER, HandheldKeys.androidKeyCode(GameKey.PLAY));
        assertEquals(KeyEvent.KEYCODE_DEL, HandheldKeys.androidKeyCode(GameKey.B));
        assertEquals(KeyEvent.KEYCODE_SOFT_LEFT, HandheldKeys.androidKeyCode(GameKey.L));
        assertEquals(KeyEvent.KEYCODE_SOFT_RIGHT, HandheldKeys.androidKeyCode(GameKey.R));
        assertEquals(KeyEvent.KEYCODE_STAR, HandheldKeys.androidKeyCode(GameKey.X));
        assertEquals(KeyEvent.KEYCODE_POUND, HandheldKeys.androidKeyCode(GameKey.Y));
    }

    @Test
    public void digitsAreAllDifferent() {
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 12; i++) {
            assertTrue(seen.add(HandheldKeys.androidKeyCode(HandheldGeometry.keypadKey(i))));
        }
        assertFalse(seen.contains(0));
    }
}

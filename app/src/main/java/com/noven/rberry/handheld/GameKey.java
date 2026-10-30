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
 * Every button on the handheld body. Games only ever see these, so the same body can drive a
 * built-in game today and the J2ME engine later.
 */
public enum GameKey {
    UP, DOWN, LEFT, RIGHT,
    /** Face buttons, laid out as a diamond: X on top, Y left, A right, B bottom. */
    A, B, X, Y,
    /** The two small centre pills (the old SELECT and START). */
    PICK, PLAY,
    /** The two shoulder buttons, used as soft keys by J2ME games. */
    L, R;

    /** One bit per key, so a set of pressed keys fits in an int. */
    public int mask() {
        return 1 << ordinal();
    }
}

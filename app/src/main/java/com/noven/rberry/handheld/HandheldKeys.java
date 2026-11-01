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

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

/**
 * Turns console buttons into ordinary Android key events and hands them to the activity, so the
 * J2ME engine treats them exactly like a keyboard: its own key mapping, game actions and
 * soft keys all keep working. A held button repeats the way a real key does.
 *
 * <p>Mapping: D-pad to the arrow keys, A and PLAY to Enter (fire), B to Delete (clear), X to
 * star, Y to pound, L and R to the left and right soft keys, and the number pad to the digits.
 * PICK and PAD are not keys; the activity handles them.</p>
 */
public final class HandheldKeys {

    /** What the host activity must provide: somewhere to send the key events. */
    public interface Target {
        boolean dispatchKeyEvent(@NonNull KeyEvent event);
    }

    private static final long REPEAT_DELAY_MS = 400;
    private static final long REPEAT_INTERVAL_MS = 80;

    private final Target target;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final long[] downTime = new long[GameKey.values().length];
    private final int[] repeats = new int[GameKey.values().length];
    private final boolean[] held = new boolean[GameKey.values().length];
    private final Runnable[] repeaters = new Runnable[GameKey.values().length];

    public HandheldKeys(@NonNull Target target) {
        this.target = target;
    }

    /** The Android key code for a console button, or 0 when it is not a key. */
    public static int androidKeyCode(@NonNull GameKey key) {
        switch (key) {
            case UP:
                return KeyEvent.KEYCODE_DPAD_UP;
            case DOWN:
                return KeyEvent.KEYCODE_DPAD_DOWN;
            case LEFT:
                return KeyEvent.KEYCODE_DPAD_LEFT;
            case RIGHT:
                return KeyEvent.KEYCODE_DPAD_RIGHT;
            case A:
            case PLAY:
                return KeyEvent.KEYCODE_ENTER;
            case B:
                return KeyEvent.KEYCODE_DEL;
            case X:
            case STAR:
                return KeyEvent.KEYCODE_STAR;
            case Y:
            case POUND:
                return KeyEvent.KEYCODE_POUND;
            case L:
                return KeyEvent.KEYCODE_SOFT_LEFT;
            case R:
                return KeyEvent.KEYCODE_SOFT_RIGHT;
            case NUM0:
                return KeyEvent.KEYCODE_0;
            case NUM1:
                return KeyEvent.KEYCODE_1;
            case NUM2:
                return KeyEvent.KEYCODE_2;
            case NUM3:
                return KeyEvent.KEYCODE_3;
            case NUM4:
                return KeyEvent.KEYCODE_4;
            case NUM5:
                return KeyEvent.KEYCODE_5;
            case NUM6:
                return KeyEvent.KEYCODE_6;
            case NUM7:
                return KeyEvent.KEYCODE_7;
            case NUM8:
                return KeyEvent.KEYCODE_8;
            case NUM9:
                return KeyEvent.KEYCODE_9;
            default:
                return 0;
        }
    }

    /** Presses or releases a console button. Buttons that are not keys are ignored. */
    public void onKey(@NonNull GameKey key, boolean down) {
        int code = androidKeyCode(key);
        if (code == 0) {
            return;
        }
        int i = key.ordinal();
        if (down) {
            if (held[i]) {
                return;
            }
            held[i] = true;
            repeats[i] = 0;
            downTime[i] = SystemClock.uptimeMillis();
            send(i, code, KeyEvent.ACTION_DOWN, 0);
            Runnable repeater = new Runnable() {
                @Override
                public void run() {
                    if (!held[i]) {
                        return;
                    }
                    repeats[i]++;
                    send(i, code, KeyEvent.ACTION_DOWN, repeats[i]);
                    handler.postDelayed(this, REPEAT_INTERVAL_MS);
                }
            };
            repeaters[i] = repeater;
            handler.postDelayed(repeater, REPEAT_DELAY_MS);
        } else if (held[i]) {
            held[i] = false;
            if (repeaters[i] != null) {
                handler.removeCallbacks(repeaters[i]);
                repeaters[i] = null;
            }
            send(i, code, KeyEvent.ACTION_UP, 0);
        }
    }

    /** Releases everything that is held, for example when the game goes to the background. */
    public void releaseAll() {
        for (GameKey key : GameKey.values()) {
            if (held[key.ordinal()]) {
                onKey(key, false);
            }
        }
    }

    private void send(int index, int code, int action, int repeat) {
        long now = SystemClock.uptimeMillis();
        target.dispatchKeyEvent(new KeyEvent(downTime[index], now, action, code, repeat));
    }
}

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
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/** Remembers where the game picture sits in the display, separately for upright and sideways play. */
public final class DisplayPrefs {

    private static final String FILE = "rberry_display";

    private DisplayPrefs() {
    }

    @NonNull
    public static DisplayFrame load(@NonNull Context context, @NonNull DisplayMode mode) {
        try {
            SharedPreferences sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
            return DisplayFrame.decode(sp.getString(key(mode), null));
        } catch (RuntimeException e) {
            return new DisplayFrame();
        }
    }

    public static void save(@NonNull Context context, @NonNull DisplayMode mode, @NonNull DisplayFrame frame) {
        try {
            SharedPreferences sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
            if (frame.coversWindow() && !frame.stretch) {
                sp.edit().remove(key(mode)).apply();
            } else {
                sp.edit().putString(key(mode), frame.encode()).apply();
            }
        } catch (RuntimeException ignored) {
            // Not being able to remember it is not worth stopping a game for.
        }
    }

    private static String key(DisplayMode mode) {
        return "frame_" + mode.name().toLowerCase(java.util.Locale.ROOT);
    }
}

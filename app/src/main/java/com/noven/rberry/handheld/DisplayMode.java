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

import ru.playsoftware.j2meloader.config.ProfileModel;

/**
 * The two display modes every imported game runs in. There is nothing else to set: a game is
 * either upright (240 x 320) or sideways (320 x 240), the console body follows, and the picture
 * always fits the display with its shape kept and centred.
 */
public enum DisplayMode {
    PORTRAIT(240, 320, 2),
    LANDSCAPE(320, 240, 3);

    /** Orientation codes used by the stored game profile: 2 = portrait, 3 = landscape. */
    public static final int PROFILE_PORTRAIT = 2;
    public static final int PROFILE_LANDSCAPE = 3;

    public final int width;
    public final int height;
    public final int profileOrientation;

    DisplayMode(int width, int height, int profileOrientation) {
        this.width = width;
        this.height = height;
        this.profileOrientation = profileOrientation;
    }

    /** The mode a stored profile asks for. Anything that is not landscape runs as portrait. */
    @NonNull
    public static DisplayMode of(@NonNull ProfileModel params) {
        return params.orientation == PROFILE_LANDSCAPE ? LANDSCAPE : PORTRAIT;
    }

    /** The mode for a game that reports its own screen size (landscape if it is wider than tall). */
    @NonNull
    public static DisplayMode forSize(int width, int height) {
        return width > height ? LANDSCAPE : PORTRAIT;
    }

    @NonNull
    public DisplayMode other() {
        return this == PORTRAIT ? LANDSCAPE : PORTRAIT;
    }

    /**
     * Forces the display settings of a profile to this mode. Called every time a game starts, so
     * old settings, a copied template or the advanced screen can never leave a game half-sized.
     * Other settings (graphics, fonts, sound, key mapping) are left as they are.
     */
    public void apply(@NonNull ProfileModel params) {
        params.orientation = profileOrientation;
        params.screenWidth = width;
        params.screenHeight = height;
        // Fit the whole picture, keep its shape, centre it.
        params.screenScaleType = 1;
        params.screenScaleRatio = 100;
        params.screenGravity = 2;
        params.screenScaleToFit = true;
        params.screenKeepAspectRatio = true;
        params.screenFilter = true;
        // The console buttons replace the on-screen keypad that J2ME Loader draws over the game.
        params.showKeyboard = false;
        params.touchInput = true;
    }
}

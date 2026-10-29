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

package com.noven.rberry.home;

import android.app.Activity;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/** One of the original games that ship inside rBerry and show up as a card on the home screen. */
public final class BuiltInGame {
    public final String id;
    @StringRes
    public final int titleRes;
    @DrawableRes
    public final int iconRes;
    public final Class<? extends Activity> activityClass;

    public BuiltInGame(String id, @StringRes int titleRes, @DrawableRes int iconRes,
                       Class<? extends Activity> activityClass) {
        this.id = id;
        this.titleRes = titleRes;
        this.iconRes = iconRes;
        this.activityClass = activityClass;
    }
}

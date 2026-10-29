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

import com.noven.rberry.games.snake.SnakeActivity;

import java.util.Collections;
import java.util.List;

import ru.playsoftware.j2meloader.R;

/** The list of built-in games. To add a game, add one line here and one string and icon. */
public final class BuiltInGames {

    private BuiltInGames() {
    }

    public static List<BuiltInGame> all() {
        return Collections.singletonList(
                new BuiltInGame("snake", R.string.snake_title, R.drawable.ic_game_snake,
                        SnakeActivity.class));
    }
}

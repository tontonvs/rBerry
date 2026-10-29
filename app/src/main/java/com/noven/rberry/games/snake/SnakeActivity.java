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

package com.noven.rberry.games.snake;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.KeyEvent;

import androidx.annotation.Nullable;

import ru.playsoftware.j2meloader.base.BaseActivity;

/** Hosts the built-in Snake game. Phase 3 will move it inside the handheld body. */
public class SnakeActivity extends BaseActivity {

    private SnakeView snakeView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Same plum as the game screen, so the system bar areas do not show a light strip.
        getWindow().setBackgroundDrawable(new ColorDrawable(0xFF1E0B22));
        snakeView = new SnakeView(this);
        setContentView(snakeView);
    }

    @Override
    protected void onPause() {
        snakeView.pause();
        super.onPause();
    }

    /** Keyboard, D-pad and gamepad support for people who play with a controller. */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        SnakeEngine.Dir dir;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_W:
                dir = SnakeEngine.Dir.UP;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_S:
                dir = SnakeEngine.Dir.DOWN;
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_A:
                dir = SnakeEngine.Dir.LEFT;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_D:
                dir = SnakeEngine.Dir.RIGHT;
                break;
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_BUTTON_START:
                snakeView.togglePause();
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
        snakeView.turn(dir);
        return true;
    }
}

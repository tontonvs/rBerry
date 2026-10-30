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

import android.os.Bundle;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.noven.rberry.handheld.GameKey;
import com.noven.rberry.handheld.HandheldView;

import ru.playsoftware.j2meloader.base.BaseActivity;

/**
 * Hosts the built-in Snake game inside the rBerry handheld body. The D-pad steers, PLAY (or A)
 * starts, pauses and resumes. Swiping on the display and a hardware keyboard or gamepad still work.
 */
public class SnakeActivity extends BaseActivity implements HandheldView.KeyListener {

    private HandheldView handheld;
    private SnakeView snakeView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HandheldView.applyWindowChrome(this);
        handheld = new HandheldView(this);
        snakeView = new SnakeView(this);
        handheld.setScreen(snakeView);
        handheld.setKeyListener(this);
        setContentView(handheld);
        if (savedInstanceState == null) {
            handheld.showBoot(null);
        }
    }

    @Override
    protected void onPause() {
        snakeView.pause();
        super.onPause();
    }

    @Override
    public void onKey(@NonNull GameKey key, boolean down) {
        if (!down) {
            return;
        }
        switch (key) {
            case UP:
                snakeView.turn(SnakeEngine.Dir.UP);
                break;
            case DOWN:
                snakeView.turn(SnakeEngine.Dir.DOWN);
                break;
            case LEFT:
                snakeView.turn(SnakeEngine.Dir.LEFT);
                break;
            case RIGHT:
                snakeView.turn(SnakeEngine.Dir.RIGHT);
                break;
            case A:
            case PLAY:
                snakeView.togglePause();
                break;
            case B:
            case PICK:
                snakeView.pause();
                break;
            default:
                break;
        }
    }

    /** Keyboard, D-pad and gamepad support. The console buttons light up to match. */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        GameKey key = keyFor(keyCode);
        if (key == null) {
            return super.onKeyDown(keyCode, event);
        }
        if (event.getRepeatCount() == 0) {
            handheld.showPressed(key, true);
            if (!handheld.isBooting()) {
                onKey(key, true);
            }
        }
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        GameKey key = keyFor(keyCode);
        if (key == null) {
            return super.onKeyUp(keyCode, event);
        }
        handheld.showPressed(key, false);
        return true;
    }

    @Nullable
    private static GameKey keyFor(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_W:
                return GameKey.UP;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_S:
                return GameKey.DOWN;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_A:
                return GameKey.LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_D:
                return GameKey.RIGHT;
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_BUTTON_START:
            case KeyEvent.KEYCODE_BUTTON_A:
                return GameKey.PLAY;
            default:
                return null;
        }
    }
}

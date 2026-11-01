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
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.annotation.NonNull;

/**
 * The little "click" and buzz when a console button is pressed. The click is a tiny tone made in
 * code (no sound file); the buzz is a short pulse of the phone's vibration motor. Each can be
 * switched off, and the choice is remembered.
 */
public final class KeyFeedback {

    private static final String FILE = "rberry_feedback";
    private static final String KEY_SOUND = "sound";
    private static final String KEY_VIBRATE = "vibrate";
    private static final int SAMPLE_RATE = 22050;
    private static final int CLICK_MS = 24;
    private static final long BUZZ_MS = 18;

    private final SharedPreferences prefs;
    private final Vibrator vibrator;
    private boolean sound;
    private boolean vibrate;
    private AudioTrack track;
    private boolean trackFailed;

    @SuppressWarnings("deprecation")
    public KeyFeedback(@NonNull Context context) {
        Context app = context.getApplicationContext();
        prefs = app.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        sound = prefs.getBoolean(KEY_SOUND, true);
        vibrate = prefs.getBoolean(KEY_VIBRATE, true);
        Vibrator v = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                v = vm != null ? vm.getDefaultVibrator() : null;
            } else {
                v = (Vibrator) app.getSystemService(Context.VIBRATOR_SERVICE);
            }
        } catch (RuntimeException ignored) {
            // No vibrator: the buzz is simply skipped.
        }
        vibrator = v;
    }

    public boolean isSoundOn() {
        return sound;
    }

    public boolean isVibrationOn() {
        return vibrate;
    }

    public void setSoundOn(boolean on) {
        sound = on;
        prefs.edit().putBoolean(KEY_SOUND, on).apply();
        if (!on) {
            release();
        }
    }

    public void setVibrationOn(boolean on) {
        vibrate = on;
        prefs.edit().putBoolean(KEY_VIBRATE, on).apply();
    }

    /** Call for every new button press. */
    public void onPress() {
        if (sound) {
            click();
        }
        if (vibrate) {
            buzz();
        }
    }

    /** Frees the sound player. Safe to call more than once; the next click makes a new one. */
    public void release() {
        AudioTrack t = track;
        track = null;
        if (t != null) {
            try {
                t.release();
            } catch (RuntimeException ignored) {
                // Already gone.
            }
        }
    }

    private void buzz() {
        if (vibrator == null) {
            return;
        }
        try {
            if (!vibrator.hasVibrator()) {
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(BUZZ_MS, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrate(BUZZ_MS);
            }
        } catch (RuntimeException ignored) {
            // Some phones refuse; never break a game over a buzz.
        }
    }

    @SuppressWarnings("deprecation")
    private void vibrate(long ms) {
        vibrator.vibrate(ms);
    }

    private void click() {
        if (trackFailed) {
            return;
        }
        try {
            if (track == null) {
                track = makeTrack();
            }
            track.stop();
            track.reloadStaticData();
            track.play();
        } catch (RuntimeException e) {
            trackFailed = true;
            release();
        }
    }

    /** A 24 ms blip that drops in pitch and fades out, like a small plastic key. */
    @NonNull
    static short[] makeClick() {
        int n = SAMPLE_RATE * CLICK_MS / 1000;
        short[] pcm = new short[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double freq = 1700 - 700 * t;
            phase += 2 * Math.PI * freq / SAMPLE_RATE;
            double envelope = Math.pow(1.0 - t, 2.2);
            pcm[i] = (short) Math.round(Math.sin(phase) * envelope * 0.35 * Short.MAX_VALUE);
        }
        return pcm;
    }

    @NonNull
    @SuppressWarnings("deprecation")
    private static AudioTrack makeTrack() {
        short[] pcm = makeClick();
        int bytes = pcm.length * 2;
        AudioTrack t;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            t = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setBufferSizeInBytes(bytes)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build();
        } else {
            t = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bytes, AudioTrack.MODE_STATIC);
        }
        t.write(pcm, 0, pcm.length);
        return t;
    }
}

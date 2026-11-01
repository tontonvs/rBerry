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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KeyFeedbackTest {

    @Test
    public void clickIsShortQuietAndFadesOut() {
        short[] pcm = KeyFeedback.makeClick();
        assertEquals(22050 * 24 / 1000, pcm.length);
        int peak = 0;
        for (short s : pcm) {
            peak = Math.max(peak, Math.abs(s));
        }
        assertTrue("audible", peak > 2000);
        assertTrue("not loud", peak < 0.5 * Short.MAX_VALUE);
        int tail = 0;
        for (int i = pcm.length - 20; i < pcm.length; i++) {
            tail = Math.max(tail, Math.abs(pcm[i]));
        }
        assertTrue("fades out (no pop)", tail < peak / 20);
    }
}

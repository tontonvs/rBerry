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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.noven.rberry.games.snake.SnakeEngine.Dir;
import com.noven.rberry.games.snake.SnakeEngine.Result;

import org.junit.Test;

import java.util.Random;

public class SnakeEngineTest {

    private static SnakeEngine newEngine() {
        return new SnakeEngine(10, 10, new Random(1));
    }

    private static int[] body(SnakeEngine e) {
        int[] out = new int[e.getCols() * e.getRows()];
        int n = e.copyBody(out);
        int[] exact = new int[n];
        System.arraycopy(out, 0, exact, 0, n);
        return exact;
    }

    private static boolean onBody(SnakeEngine e, int cell) {
        for (int c : body(e)) {
            if (c == cell) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void startsWithThreeCellsHeadingRightInTheMiddle() {
        SnakeEngine e = newEngine();
        assertEquals(3, e.getLength());
        assertEquals(Dir.RIGHT, e.getHeading());
        assertTrue(e.isAlive());
        assertFalse(e.isWon());
        assertEquals(0, e.getScore());
        int[] b = body(e);
        assertEquals(e.cellOf(5, 5), b[0]);
        assertEquals(e.cellOf(4, 5), b[1]);
        assertEquals(e.cellOf(3, 5), b[2]);
        assertFalse(onBody(e, e.getFood()));
    }

    @Test
    public void movesOneCellPerStepAndKeepsItsLength() {
        SnakeEngine e = newEngine();
        e.load(Dir.RIGHT, e.cellOf(0, 0), e.cellOf(5, 5), e.cellOf(4, 5), e.cellOf(3, 5));
        assertEquals(Result.MOVED, e.step());
        int[] b = body(e);
        assertEquals(3, b.length);
        assertEquals(e.cellOf(6, 5), b[0]);
        assertEquals(e.cellOf(4, 5), b[2]);
    }

    @Test
    public void ignoresReverseAndRepeatedTurns() {
        SnakeEngine e = newEngine();
        assertFalse(e.turn(Dir.LEFT));
        assertFalse(e.turn(Dir.RIGHT));
        assertEquals(Result.MOVED, e.step());
        assertEquals(Dir.RIGHT, e.getHeading());
    }

    @Test
    public void quickTurnsCannotReverseTheSnakeOntoItself() {
        SnakeEngine e = newEngine();
        assertTrue(e.turn(Dir.UP));
        assertFalse(e.turn(Dir.DOWN));
        assertEquals(Result.MOVED, e.step());
        assertEquals(Dir.UP, e.getHeading());
    }

    @Test
    public void buffersTwoTurnsAndDropsTheThird() {
        SnakeEngine e = newEngine();
        assertTrue(e.turn(Dir.UP));
        assertTrue(e.turn(Dir.LEFT));
        assertFalse(e.turn(Dir.DOWN));
        e.step();
        assertEquals(Dir.UP, e.getHeading());
        e.step();
        assertEquals(Dir.LEFT, e.getHeading());
        assertTrue(e.isAlive());
    }

    @Test
    public void diesWhenHittingTheWallAndStaysDead() {
        SnakeEngine e = newEngine();
        Result r = Result.MOVED;
        int steps = 0;
        while (r == Result.MOVED || r == Result.ATE) {
            r = e.step();
            steps++;
            assertTrue("never reached the wall", steps < 50);
        }
        assertEquals(Result.DIED, r);
        assertFalse(e.isAlive());
        int[] before = body(e);
        assertEquals(Result.DIED, e.step());
        assertEquals(before.length, body(e).length);
        assertEquals(before[0], body(e)[0]);
    }

    @Test
    public void eatingFoodGrowsScoresAndMovesTheFood() {
        SnakeEngine e = newEngine();
        e.load(Dir.RIGHT, e.cellOf(6, 5), e.cellOf(5, 5), e.cellOf(4, 5), e.cellOf(3, 5));
        assertEquals(Result.ATE, e.step());
        assertEquals(4, e.getLength());
        assertEquals(SnakeEngine.POINTS_PER_FOOD, e.getScore());
        assertFalse(onBody(e, e.getFood()));
    }

    @Test
    public void hittingItsOwnBodyIsFatal() {
        SnakeEngine e = newEngine();
        // length 5 heading right: head (5,5)
        e.load(Dir.RIGHT, e.cellOf(0, 0),
                e.cellOf(5, 5), e.cellOf(4, 5), e.cellOf(3, 5), e.cellOf(2, 5), e.cellOf(1, 5));
        e.turn(Dir.UP);
        assertEquals(Result.MOVED, e.step());   // (5,4)
        e.turn(Dir.LEFT);
        assertEquals(Result.MOVED, e.step());   // (4,4)
        e.turn(Dir.DOWN);
        assertEquals(Result.DIED, e.step());    // (4,5) is still body
        assertFalse(e.isAlive());
    }

    @Test
    public void snakeMayStepIntoTheCellItsTailIsLeaving() {
        SnakeEngine e = newEngine();
        // 2x2 loop of length 4 heading left: head (4,4), (5,4), (5,5), tail (4,5).
        // Turning down moves the head into (4,5), the cell the tail is leaving on that tick.
        e.load(Dir.LEFT, e.cellOf(0, 0),
                e.cellOf(4, 4), e.cellOf(5, 4), e.cellOf(5, 5), e.cellOf(4, 5));
        e.turn(Dir.DOWN);
        assertEquals(Result.MOVED, e.step());   // into (4,5), where the tail was
        assertTrue(e.isAlive());
        assertEquals(4, e.getLength());
    }

    @Test
    public void foodNeverSpawnsOnTheSnake() {
        for (int seed = 0; seed < 300; seed++) {
            SnakeEngine e = new SnakeEngine(10, 10, new Random(seed));
            assertFalse("seed " + seed, onBody(e, e.getFood()));
            e.load(Dir.RIGHT, e.cellOf(6, 5), e.cellOf(5, 5), e.cellOf(4, 5), e.cellOf(3, 5));
            assertEquals(Result.ATE, e.step());
            assertFalse("seed " + seed, onBody(e, e.getFood()));
        }
    }

    @Test
    public void winsWhenTheBoardIsFull() {
        SnakeEngine e = new SnakeEngine(4, 4, new Random(1));
        // Path in order: row0 left->right, row1 right->left, row2 left->right, row3 right->left
        // stopping one cell short at (1,3). Head is the last cell of the path.
        int[] headFirst = {
                e.cellOf(1, 3), e.cellOf(2, 3), e.cellOf(3, 3),
                e.cellOf(3, 2), e.cellOf(2, 2), e.cellOf(1, 2), e.cellOf(0, 2),
                e.cellOf(0, 1), e.cellOf(1, 1), e.cellOf(2, 1), e.cellOf(3, 1),
                e.cellOf(3, 0), e.cellOf(2, 0), e.cellOf(1, 0), e.cellOf(0, 0)
        };
        e.load(Dir.LEFT, e.cellOf(0, 3), headFirst);
        assertEquals(Result.WON, e.step());
        assertTrue(e.isWon());
        assertFalse(e.isAlive());
        assertEquals(16, e.getLength());
        assertEquals(Result.WON, e.step());
    }

    @Test
    public void ticksGetFasterButNeverBelowTheFloor() {
        assertEquals(SnakeEngine.START_TICK_MS, SnakeEngine.tickMillisFor(0));
        assertTrue(SnakeEngine.tickMillisFor(5) < SnakeEngine.tickMillisFor(0));
        assertEquals(SnakeEngine.MIN_TICK_MS, SnakeEngine.tickMillisFor(10_000));
    }
}

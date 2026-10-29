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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Random;

/**
 * Rules of Snake with no Android dependencies, so they can be unit tested on any JVM.
 *
 * <p>The board is a grid of {@code cols x rows} cells. A cell is stored as a single int
 * {@code y * cols + x}. Walls are deadly. The snake dies when it hits a wall or itself and wins
 * when it fills the whole board.</p>
 */
public final class SnakeEngine {

    public enum Dir {
        UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

        public final int dx;
        public final int dy;

        Dir(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        public Dir opposite() {
            switch (this) {
                case UP:
                    return DOWN;
                case DOWN:
                    return UP;
                case LEFT:
                    return RIGHT;
                default:
                    return LEFT;
            }
        }
    }

    public enum Result { MOVED, ATE, DIED, WON }

    public static final int START_TICK_MS = 170;
    public static final int MIN_TICK_MS = 75;
    public static final int TICK_STEP_MS = 4;
    public static final int POINTS_PER_FOOD = 10;

    private static final int START_LENGTH = 3;
    private static final int MAX_QUEUED_TURNS = 2;

    private final int cols;
    private final int rows;
    private final Random random;
    private final boolean[] occupied;
    private final int[] freeBuffer;
    private final Deque<Integer> body = new ArrayDeque<>(); // head first
    private final Deque<Dir> turns = new ArrayDeque<>();

    private Dir heading = Dir.RIGHT;
    private int food = -1;
    private int eaten;
    private boolean alive;
    private boolean won;

    public SnakeEngine(int cols, int rows, Random random) {
        if (cols < 4 || rows < 4) {
            throw new IllegalArgumentException("Board must be at least 4x4");
        }
        this.cols = cols;
        this.rows = rows;
        this.random = random;
        this.occupied = new boolean[cols * rows];
        this.freeBuffer = new int[cols * rows];
        reset();
    }

    /** Puts a new three-cell snake in the middle of the board, heading right, with fresh food. */
    public void reset() {
        body.clear();
        turns.clear();
        Arrays.fill(occupied, false);
        heading = Dir.RIGHT;
        eaten = 0;
        alive = true;
        won = false;
        int headX = cols / 2;
        int y = rows / 2;
        for (int i = 0; i < START_LENGTH; i++) {
            int cell = cellOf(headX - i, y);
            body.addLast(cell);
            occupied[cell] = true;
        }
        placeFood();
    }

    /**
     * Queues a turn. Turning back onto yourself and repeating the current direction are ignored,
     * and at most two turns are buffered so fast swipes between two ticks are not lost.
     *
     * @return true if the turn was accepted
     */
    public boolean turn(Dir dir) {
        Dir last = turns.isEmpty() ? heading : turns.peekLast();
        if (dir == last || dir == last.opposite()) {
            return false;
        }
        if (turns.size() >= MAX_QUEUED_TURNS) {
            return false;
        }
        turns.addLast(dir);
        return true;
    }

    /** Advances the game by one tick. */
    public Result step() {
        if (won) {
            return Result.WON;
        }
        if (!alive) {
            return Result.DIED;
        }
        if (!turns.isEmpty()) {
            heading = turns.pollFirst();
        }
        int head = body.peekFirst();
        int nx = head % cols + heading.dx;
        int ny = head / cols + heading.dy;
        if (nx < 0 || nx >= cols || ny < 0 || ny >= rows) {
            alive = false;
            return Result.DIED;
        }
        int next = cellOf(nx, ny);
        boolean grows = next == food;
        int tail = body.peekLast();
        // The tail cell is free again on this tick, unless the snake grows.
        if (occupied[next] && !(next == tail && !grows)) {
            alive = false;
            return Result.DIED;
        }
        if (!grows) {
            body.pollLast();
            occupied[tail] = false;
        }
        body.addFirst(next);
        occupied[next] = true;
        if (grows) {
            eaten++;
            if (body.size() == occupied.length) {
                won = true;
                alive = false;
                food = -1;
                return Result.WON;
            }
            placeFood();
            return Result.ATE;
        }
        return Result.MOVED;
    }

    private void placeFood() {
        int n = 0;
        for (int i = 0; i < occupied.length; i++) {
            if (!occupied[i]) {
                freeBuffer[n++] = i;
            }
        }
        food = n == 0 ? -1 : freeBuffer[random.nextInt(n)];
    }

    /** Copies the body into {@code out}, head first, and returns how many cells were written. */
    public int copyBody(int[] out) {
        int i = 0;
        for (int cell : body) {
            out[i++] = cell;
        }
        return i;
    }

    public static int tickMillisFor(int eaten) {
        return Math.max(MIN_TICK_MS, START_TICK_MS - TICK_STEP_MS * eaten);
    }

    public int tickMillis() {
        return tickMillisFor(eaten);
    }

    public int cellOf(int x, int y) {
        return y * cols + x;
    }

    public int xOf(int cell) {
        return cell % cols;
    }

    public int yOf(int cell) {
        return cell / cols;
    }

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }

    public int getFood() {
        return food;
    }

    public Dir getHeading() {
        return heading;
    }

    public int getLength() {
        return body.size();
    }

    public int getScore() {
        return eaten * POINTS_PER_FOOD;
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean isWon() {
        return won;
    }

    /** Test hook: replaces the whole state. Cells are head first. */
    void load(Dir newHeading, int newFood, int... cellsHeadFirst) {
        body.clear();
        turns.clear();
        Arrays.fill(occupied, false);
        for (int cell : cellsHeadFirst) {
            body.addLast(cell);
            occupied[cell] = true;
        }
        heading = newHeading;
        food = newFood;
        eaten = 0;
        alive = true;
        won = false;
    }
}

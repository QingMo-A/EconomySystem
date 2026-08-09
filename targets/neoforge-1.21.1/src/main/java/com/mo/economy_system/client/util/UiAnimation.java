package com.mo.economy_system.client.util;

public final class UiAnimation {
    public static final int INFINITE = -1;

    public enum LoopMode {
        NONE,
        LOOP,
        PING_PONG
    }

    public enum Easing {
        LINEAR,
        EASE_IN_QUAD,
        EASE_OUT_QUAD,
        EASE_IN_OUT_QUAD,
        EASE_IN_CUBIC,
        EASE_OUT_CUBIC,
        EASE_IN_OUT_CUBIC
    }

    private long durationMs;
    private long delayMs;
    private LoopMode loopMode = LoopMode.NONE;
    private int repeatCount = 1;
    private Easing easing;
    private boolean reversed;
    private boolean running;
    private boolean paused;
    private boolean finished;
    private long startTimeMs;
    private long pausedAtMs;
    private long pausedTotalMs;

    public UiAnimation(long durationMs) {
        this(durationMs, Easing.LINEAR);
    }

    public UiAnimation(long durationMs, Easing easing) {
        this.durationMs = Math.max(0, durationMs);
        this.easing = easing == null ? Easing.LINEAR : easing;
    }

    public UiAnimation setDurationMs(long durationMs) {
        this.durationMs = Math.max(0, durationMs);
        return this;
    }

    public UiAnimation setDelayMs(long delayMs) {
        this.delayMs = Math.max(0, delayMs);
        return this;
    }

    public UiAnimation setLoopMode(LoopMode loopMode) {
        this.loopMode = loopMode == null ? LoopMode.NONE : loopMode;
        return this;
    }

    public UiAnimation setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
        return this;
    }

    public UiAnimation setEasing(Easing easing) {
        this.easing = easing == null ? Easing.LINEAR : easing;
        return this;
    }

    public UiAnimation setReversed(boolean reversed) {
        this.reversed = reversed;
        return this;
    }

    public UiAnimation start() {
        return startAt(System.currentTimeMillis());
    }

    public UiAnimation startAt(long nowMs) {
        running = true;
        paused = false;
        finished = false;
        startTimeMs = nowMs;
        pausedAtMs = 0L;
        pausedTotalMs = 0L;
        return this;
    }

    public UiAnimation restart() {
        return start();
    }

    public UiAnimation stop() {
        running = false;
        paused = false;
        finished = false;
        return this;
    }

    public UiAnimation finish() {
        running = false;
        paused = false;
        finished = true;
        return this;
    }

    public UiAnimation pause() {
        if (running && !paused) {
            paused = true;
            pausedAtMs = System.currentTimeMillis();
        }
        return this;
    }

    public UiAnimation resume() {
        if (paused) {
            long nowMs = System.currentTimeMillis();
            pausedTotalMs += nowMs - pausedAtMs;
            paused = false;
            pausedAtMs = 0L;
        }
        return this;
    }

    public boolean isRunning() {
        return running && !paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isFinished() {
        return finished;
    }

    public float progress() {
        return progressAt(System.currentTimeMillis());
    }

    public float progressAt(long nowMs) {
        return computeLinearProgress(nowMs);
    }

    public float value() {
        return valueAt(System.currentTimeMillis());
    }

    public float valueAt(long nowMs) {
        if (!running && !finished) {
            return reversed ? 1.0f : 0.0f;
        }

        float t = computeLinearProgress(nowMs);
        float eased = applyEasing(t);
        return reversed ? 1.0f - eased : eased;
    }

    private float computeLinearProgress(long nowMs) {
        if (durationMs <= 0L) {
            return 1.0f;
        }

        if (!running) {
            return finished ? 1.0f : 0.0f;
        }

        if (paused) {
            nowMs = pausedAtMs;
        }

        long elapsed = nowMs - startTimeMs - pausedTotalMs - delayMs;
        if (elapsed < 0L) {
            return 0.0f;
        }

        if (loopMode == LoopMode.NONE) {
            if (elapsed >= durationMs) {
                running = false;
                finished = true;
                return 1.0f;
            }
            return (float) elapsed / (float) durationMs;
        }

        if (repeatCount == 0) {
            running = false;
            finished = true;
            return 0.0f;
        }

        long cycleDuration = durationMs;
        if (loopMode == LoopMode.PING_PONG) {
            cycleDuration = durationMs * 2L;
        }

        if (repeatCount > 0) {
            long totalDuration = cycleDuration * (long) repeatCount;
            if (elapsed >= totalDuration) {
                running = false;
                finished = true;
                return loopMode == LoopMode.LOOP ? 1.0f : 0.0f;
            }
        }

        long within = elapsed % cycleDuration;
        if (loopMode == LoopMode.LOOP) {
            return (float) within / (float) durationMs;
        }

        if (within <= durationMs) {
            return (float) within / (float) durationMs;
        }

        return 1.0f - ((float) (within - durationMs) / (float) durationMs);
    }

    private float applyEasing(float t) {
        float clamped = t <= 0.0f ? 0.0f : (t >= 1.0f ? 1.0f : t);
        switch (easing) {
            case EASE_IN_QUAD:
                return clamped * clamped;
            case EASE_OUT_QUAD:
                return clamped * (2.0f - clamped);
            case EASE_IN_OUT_QUAD:
                return clamped < 0.5f
                    ? 2.0f * clamped * clamped
                    : 1.0f - (float) Math.pow(-2.0f * clamped + 2.0f, 2.0f) / 2.0f;
            case EASE_IN_CUBIC:
                return clamped * clamped * clamped;
            case EASE_OUT_CUBIC:
                return 1.0f - (float) Math.pow(1.0f - clamped, 3.0f);
            case EASE_IN_OUT_CUBIC:
                return clamped < 0.5f
                    ? 4.0f * clamped * clamped * clamped
                    : 1.0f - (float) Math.pow(-2.0f * clamped + 2.0f, 3.0f) / 2.0f;
            case LINEAR:
            default:
                return clamped;
        }
    }
}

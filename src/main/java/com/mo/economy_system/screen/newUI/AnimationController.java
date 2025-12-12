package com.mo.economy_system.screen.newUI;

public class AnimationController {

    private boolean enabled = false;
    private long duration = 300;
    private long delayAfterLeave = 500;

    private float progress = 1.0f;
    private boolean animatingIn = false;
    private boolean animatingOut = false;
    private long lastUpdate = System.currentTimeMillis();
    private long leaveTime = -1;

    private int startX, startY, targetX, targetY;

    private boolean forcedHover = false;

    public AnimationController enable(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public AnimationController setDuration(long ms) {
        this.duration = ms;
        return this;
    }

    public AnimationController setLeaveDelay(long ms) {
        this.delayAfterLeave = ms;
        return this;
    }

    public AnimationController setPositions(int startX, int startY, int targetX, int targetY) {
        this.startX = startX;
        this.startY = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        return this;
    }

    public void setForcedHover(boolean forcedHover) {
        this.forcedHover = forcedHover;
    }

    public void update(int mouseX, int mouseY, int hoverX, int hoverY, int hoverW, int hoverH) {
        if (!enabled) return;

        long now = System.currentTimeMillis();
        float delta = now - lastUpdate;
        lastUpdate = now;

        boolean hovering = mouseX >= hoverX && mouseX <= hoverX + hoverW &&
                mouseY >= hoverY && mouseY <= hoverY + hoverH;

        if (forcedHover || hovering) {
            animatingIn = true;
            animatingOut = false;
            leaveTime = -1;
        } else {
            if (leaveTime == -1) {
                leaveTime = now;
            } else if (now - leaveTime >= delayAfterLeave) {
                animatingOut = true;
                animatingIn = false;
            }
        }

        if (animatingIn) {
            progress += delta / duration;
            if (progress >= 1f) {
                progress = 1f;
                animatingIn = false;
            }
        } else if (animatingOut) {
            progress -= delta / duration;
            if (progress <= 0f) {
                progress = 0f;
                animatingOut = false;
            }
        }
    }

    public int getCurrentX() {
        return (int) (startX + (targetX - startX) * progress);
    }

    public int getCurrentY() {
        return (int) (startY + (targetY - startY) * progress);
    }

    public boolean isVisible() {
        return progress > 0.01f;
    }

    public float getProgress() {
        return progress;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}


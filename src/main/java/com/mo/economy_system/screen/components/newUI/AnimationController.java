package com.mo.economy_system.screen.components.newUI;

/**
 * 动画控制器类，用于管理UI组件的平滑动画效果（如淡入、淡出、移动等）。
 * 支持悬停触发动画、延迟消失和强制控制动画状态。
 */
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

    /**
     * 启用或禁用动画控制器。
     *
     * @param enabled 是否启用动画控制器
     * @return 当前动画控制器实例，支持链式调用
     */
    public AnimationController enable(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * 设置动画的持续时间（毫秒）。
     *
     * @param ms 动画持续时间（毫秒）
     * @return 当前动画控制器实例，支持链式调用
     */
    public AnimationController setDuration(long ms) {
        this.duration = ms;
        return this;
    }

    /**
     * 设置鼠标离开后的动画延迟时间（毫秒）。
     *
     * @param ms 延迟时间（毫秒）
     * @return 当前动画控制器实例，支持链式调用
     */
    public AnimationController setLeaveDelay(long ms) {
        this.delayAfterLeave = ms;
        return this;
    }

    /**
     * 设置动画的起始位置和目标位置。
     *
     * @param startX 起始X坐标
     * @param startY 起始Y坐标
     * @param targetX 目标X坐标
     * @param targetY 目标Y坐标
     * @return 当前动画控制器实例，支持链式调用
     */
    public AnimationController setPositions(int startX, int startY, int targetX, int targetY) {
        this.startX = startX;
        this.startY = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        return this;
    }

    /**
     * 设置强制悬停状态，忽略实际鼠标位置。
     *
     * @param forcedHover 是否强制悬停
     */
    public void setForcedHover(boolean forcedHover) {
        this.forcedHover = forcedHover;
    }

    /**
     * 更新动画状态，根据鼠标位置和悬停区域计算当前动画进度。
     *
     * @param mouseX 鼠标当前的X坐标
     * @param mouseY 鼠标当前的Y坐标
     * @param hoverX 悬停区域的X坐标
     * @param hoverY 悬停区域的Y坐标
     * @param hoverW 悬停区域的宽度
     * @param hoverH 悬停区域的高度
     */
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

    /**
     * 获取当前动画位置的X坐标。
     *
     * @return 当前X坐标，根据动画进度在起始X和目标X之间插值
     */
    public int getCurrentX() {
        return (int) (startX + (targetX - startX) * progress);
    }

    /**
     * 获取当前动画位置的Y坐标。
     *
     * @return 当前Y坐标，根据动画进度在起始Y和目标Y之间插值
     */
    public int getCurrentY() {
        return (int) (startY + (targetY - startY) * progress);
    }

    /**
     * 判断动画是否可见（动画进度大于0.01视为可见）。
     *
     * @return 如果动画可见则返回true，否则返回false
     */
    public boolean isVisible() {
        return progress > 0.01f;
    }

    /**
     * 获取当前动画进度（0.0到1.0之间）。
     *
     * @return 动画进度，0.0表示动画开始（或完全消失），1.0表示动画完成（或完全显示）
     */
    public float getProgress() {
        return progress;
    }

    /**
     * 判断动画控制器是否已启用。
     *
     * @return 如果动画控制器已启用则返回true，否则返回false
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置动画控制器的启用状态。
     *
     * @param enabled 是否启用动画控制器
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}


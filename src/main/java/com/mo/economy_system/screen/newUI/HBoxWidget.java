package com.mo.economy_system.screen.newUI;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 水平布局容器组件，用于在水平方向上排列子组件
 */
public class HBoxWidget extends AbstractWidget {

    // 子组件列表
    private final List<AbstractWidget> children = new ArrayList<>();
    // 子组件外边距映射
    private final Map<AbstractWidget, Insets> marginMap = new HashMap<>();
    // 子组件水平扩展标记映射
    private final Map<AbstractWidget, Boolean> hgrowMap = new HashMap<>();
    // 用于填充剩余空间的占位组件
    private AbstractWidget growSpacer = null;
    // 是否自动插入扩展占位组件
    private boolean autoInsertGrowSpacer = false;
    // 子组件之间的间距
    private int spacing = 4;

    // 固定宽度，-1表示自动计算
    private int fixedWidth = -1;
    // 固定高度，-1表示自动计算
    private int fixedHeight = -1;

    // 滚动偏移量
    private int scrollOffset = 0;
    // 是否启用滚动条
    private boolean scrollEnabled = false;
    // 滚动条是否在底部
    private boolean scrollbarAtBottom = true;
    // 内容总宽度
    private int contentWidth = 0;
    // 是否正在拖动滚动条
    private boolean draggingScrollbar = false;
    // 拖动起始X坐标
    private int dragStartX = 0;
    // 拖动起始滚动偏移量
    private int dragScrollOffset = 0;

    // 鼠标是否在悬停区域按下
    private boolean mouseHeldInHoverArea = false;

    // 边框显示设置
    private boolean borderTop = false;
    private boolean borderRight = false;
    private boolean borderBottom = false;
    private boolean borderLeft = false;
    // 边框颜色
    private int borderColor = 0xFFFFFFFF;
    // 边框厚度
    private int borderThickness = 1;

    // 内边距
    private int paddingLeft = 0;
    private int paddingRight = 0;
    private int paddingTop = 0;
    private int paddingBottom = 0;

    // 动画控制器
    private final AnimationController animation = new AnimationController();
    // 悬停触发区域
    private int hoverAreaX, hoverAreaY, hoverAreaW, hoverAreaH;

    // 背景颜色
    private Integer backgroundColor = null;

    private boolean selected = false;

    /**
     * 构造函数
     * @param x 初始X坐标
     * @param y 初始Y坐标
     * @param height 初始高度
     */
    public HBoxWidget(int x, int y, int height) {
        super(x, y, 0, height, null);
    }

    /**
     * 构造函数，默认位置为(0,0)
     * @param height 初始高度
     */
    public HBoxWidget(int height) {
        super(0, 0, 0, height, null);
    }

    /**
     * 设置容器固定宽度
     */
    public HBoxWidget setBoxWidth(int width) {
        this.fixedWidth = width;
        return this;
    }

    /**
     * 设置容器固定高度
     */
    public HBoxWidget setBoxHeight(int height) {
        this.fixedHeight = height;
        return this;
    }

    /**
     * 设置高度为屏幕高度的百分比
     */
    public HBoxWidget setHeightFraction(int screenHeight, float fraction) {
        this.fixedHeight = (int) (screenHeight * fraction);
        return this;
    }

    /**
     * 启用滚动条
     * @param atBottom 滚动条是否在底部
     */
    public HBoxWidget enableScrollbar(boolean atBottom) {
        this.scrollEnabled = true;
        this.scrollbarAtBottom = atBottom;
        return this;
    }

    /**
     * 设置内边距
     */
    public HBoxWidget setPadding(int top, int right, int bottom, int left) {
        this.paddingTop = top;
        this.paddingRight = right;
        this.paddingBottom = bottom;
        this.paddingLeft = left;
        updateLayout();
        return this;
    }

    /**
     * 设置背景颜色
     */
    public HBoxWidget setBackgroundColor(int argb) {
        this.backgroundColor = argb;
        return this;
    }

    /**
     * 设置子组件间距
     */
    public HBoxWidget setSpacing(int spacing) {
        this.spacing = spacing;
        updateLayout();
        return this;
    }

    /**
     * 设置子组件外边距
     */
    public HBoxWidget setMargin(AbstractWidget widget, Insets margin) {
        marginMap.put(widget, margin);
        updateLayout();
        return this;
    }

    /**
     * 添加子组件
     */
    public HBoxWidget addChild(AbstractWidget child) {
        children.add(child);
        updateLayout();
        return this;
    }

    /**
     * 添加多个子组件
     */
    public HBoxWidget addAllChildren(AbstractWidget... childrenToAdd) {
        for (AbstractWidget child : childrenToAdd) {
            children.add(child);
        }
        updateLayout();
        return this;
    }

    /**
     * 设置子组件是否水平扩展
     */
    public HBoxWidget setHGrow(AbstractWidget widget, boolean grow) {
        hgrowMap.put(widget, grow);
        return this;
    }

    /**
     * 启用自动插入扩展占位组件
     */
    public HBoxWidget enableAutoHGrowSpacer(boolean enable) {
        this.autoInsertGrowSpacer = enable;
        updateLayout();
        return this;
    }

    /**
     * 设置显示哪些边框
     */
    public HBoxWidget showBorder(boolean top, boolean right, boolean bottom, boolean left) {
        this.borderTop = top;
        this.borderRight = right;
        this.borderBottom = bottom;
        this.borderLeft = left;
        return this;
    }

    /**
     * 设置边框颜色
     */
    public HBoxWidget setBorderColor(int color) {
        this.borderColor = color;
        return this;
    }

    /**
     * 设置边框厚度
     */
    public HBoxWidget setBorderThickness(int thickness) {
        this.borderThickness = thickness;
        return this;
    }

    /**
     * 设置悬停触发区域
     */
    public HBoxWidget setHoverTriggerArea(int x, int y, int width, int height) {
        this.hoverAreaX = x;
        this.hoverAreaY = y;
        this.hoverAreaW = width;
        this.hoverAreaH = height;
        return this;
    }

    /**
     * 获取动画控制器
     */
    public AnimationController getAnimationController() {
        return animation;
    }

    /**
     * 更新布局
     */
    public void updateLayout() {
        int currentX = this.getX() + paddingLeft;
        int maxHeight = 0;

        // 计算非扩展组件的总宽度
        int totalWidthWithoutGrow = 0;
        int growCount = 0;

        for (AbstractWidget child : children) {
            if (hgrowMap.getOrDefault(child, false)) growCount++;
            else totalWidthWithoutGrow += child.getWidth();
        }

        // 加上间距
        int totalSpacing = spacing * Math.max(0, children.size() - 1);
        totalWidthWithoutGrow += totalSpacing;

        // 计算可用内容宽度
        int availableContentWidth = fixedWidth - paddingLeft - paddingRight;

        // 判断是否需要应用扩展
        boolean shouldApplyGrow = !scrollEnabled || totalWidthWithoutGrow <= availableContentWidth;

        // 自动处理扩展占位组件
        if (autoInsertGrowSpacer) {
            if (!shouldApplyGrow && growSpacer != null) {
                children.remove(growSpacer);
                hgrowMap.remove(growSpacer);
                growSpacer = null;
            } else if (shouldApplyGrow && growSpacer == null && fixedWidth > 0) {
                growSpacer = new AbstractWidget(0, 0, 0, 1, null) {
                    @Override
                    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
                    @Override
                    protected void updateWidgetNarration(NarrationElementOutput output) {}
                };
                children.add(children.size() - 1, growSpacer);
                hgrowMap.put(growSpacer, true);
            }
        }

        // 重新计算扩展组件数量和非扩展组件总宽度
        totalWidthWithoutGrow = 0;
        growCount = 0;
        for (AbstractWidget child : children) {
            if (hgrowMap.getOrDefault(child, false)) growCount++;
            else totalWidthWithoutGrow += child.getWidth();
        }
        totalSpacing = spacing * Math.max(0, children.size() - 1);
        totalWidthWithoutGrow += totalSpacing;

        // 计算每个扩展组件的宽度
        int growWidthEach = growCount > 0 && availableContentWidth > totalWidthWithoutGrow
                ? (availableContentWidth - totalWidthWithoutGrow) / growCount
                : 0;

        // 定位子组件
        currentX = getX() + paddingLeft;
        for (int i = 0; i < children.size(); i++) {
            AbstractWidget child = children.get(i);
            boolean grow = hgrowMap.getOrDefault(child, false);
            Insets margin = marginMap.getOrDefault(child, Insets.NONE);

            int left = margin.left();
            int right = margin.right();
            int top = margin.top();
            int bottom = margin.bottom();

            // 设置子组件宽度和位置
            int w = grow ? growWidthEach : child.getWidth();
            child.setWidth(w);
            child.setX(currentX + left - scrollOffset); // 考虑滚动偏移
            child.setY(this.getY() + paddingTop + top);

            currentX += left + w + right;
            if (i < children.size() - 1) currentX += spacing;

            // 计算最大高度
            maxHeight = Math.max(maxHeight, child.getHeight() + top + bottom);
        }

        // 更新内容总宽度和容器尺寸
        this.contentWidth = currentX - getX() - paddingLeft;
        this.width = fixedWidth >= 0 ? fixedWidth :
                contentWidth + paddingLeft + paddingRight;
        this.height = fixedHeight >= 0 ? fixedHeight : maxHeight + paddingTop + paddingBottom;

        // 确保滚动偏移在合理范围内
        int scrollMax = Math.max(0, contentWidth - (width - paddingLeft - paddingRight));
        if (scrollOffset > scrollMax) scrollOffset = scrollMax;
    }

    /**
     * 渲染边框
     */
    private void renderBorder(GuiGraphics graphics) {
        int x = getX();
        int y = getY();
        int x2 = x + width;
        int y2 = y + height;

        if (borderTop) {
            graphics.fill(x, y, x2, y + borderThickness, borderColor);
        }
        if (borderRight) {
            graphics.fill(x2 - borderThickness, y, x2, y2, borderColor);
        }
        if (borderBottom) {
            graphics.fill(x, y2 - borderThickness, x2, y2, borderColor);
        }
        if (borderLeft) {
            graphics.fill(x, y, x + borderThickness, y2, borderColor);
        }
    }

    /**
     * 渲染组件
     */
    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 检查鼠标是否在悬停区域
        boolean hovering = mouseX >= hoverAreaX && mouseX <= hoverAreaX + hoverAreaW &&
                mouseY >= hoverAreaY && mouseY <= hoverAreaY + hoverAreaH;

        // 更新动画状态
        animation.setForcedHover(draggingScrollbar || mouseHeldInHoverArea);
        animation.update(mouseX, mouseY, hoverAreaX, hoverAreaY, hoverAreaW, hoverAreaH);

        // 应用动画位置
        if (animation.isEnabled()) {
            this.setX(animation.getCurrentX());
            this.setY(animation.getCurrentY());
        } else {
            this.setX(getX());
            this.setY(getY());
        }

        // 更新布局
        updateLayout();

        // 启用剪裁区域
        graphics.enableScissor(
                getX(),
                getY(),
                getX() + width,
                getY() + height
        );

        // 绘制背景
        if (backgroundColor != null) {
            graphics.fill(getX(), getY(), getX() + width, getY() + height, backgroundColor);
        }

        // 绘制边框
        renderBorder(graphics);

        // 渲染子组件
        for (AbstractWidget child : children) {
            if (isWithinScroll(child.getX(), child.getWidth())) {
                child.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        // 渲染滚动条
        if (scrollEnabled && contentWidth > width) {
            int scrollMax = Math.max(1, contentWidth - (width - paddingLeft - paddingRight));
            float scrollRatio = (float) scrollOffset / scrollMax;
            int barX = getX() + paddingLeft;
            int barY = scrollbarAtBottom ? (getY() + height - 6) : getY();
            int barAreaWidth = width - paddingLeft - paddingRight;
            int barWidth = Math.max(10, (int) ((float) barAreaWidth * width / contentWidth));
            int barOffset = (int) (scrollRatio * (barAreaWidth - barWidth));
            graphics.fill(barX + barOffset, barY, barX + barOffset + barWidth, barY + 6, 0x88FFFFFF);
        }

        // 禁用剪裁区域
        graphics.disableScissor();
    }

    /**
     * 检查子组件是否在可见滚动区域内
     */
    private boolean isWithinScroll(int childX, int childWidth) {
        return childX + childWidth > getX() && childX < getX() + width;
    }

    /**
     * 鼠标滚轮事件处理
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 先检查子组件是否需要处理滚动
        for (AbstractWidget child : children) {
            if (child.isMouseOver(mouseX, mouseY)) {
                if (child.mouseScrolled(mouseX, mouseY, delta)) {
                    return true;
                }
            }
        }

        // 处理自己的滚动
        if (scrollEnabled && contentWidth > width) {
            int scrollMax = Math.max(0, contentWidth - (width - paddingLeft - paddingRight));
            scrollOffset -= delta * 10;
            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > scrollMax) scrollOffset = scrollMax;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    /**
     * 鼠标点击事件处理
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 处理滚动条点击
        if (scrollEnabled && contentWidth > width && button == 0) {
            int scrollMax = Math.max(1, contentWidth - (width - paddingLeft - paddingRight));
            float scrollRatio = (float) scrollOffset / scrollMax;
            int barAreaWidth = width - paddingLeft - paddingRight;
            int barWidth = Math.max(10, (int) ((float) barAreaWidth * width / contentWidth));
            int barX = getX() + paddingLeft + (int) (scrollRatio * (barAreaWidth - barWidth));
            int barY = scrollbarAtBottom ? (getY() + height - 6) : getY();
            if (mouseY >= barY && mouseY <= barY + 6 && mouseX >= barX && mouseX <= barX + barWidth) {
                draggingScrollbar = true;
                dragStartX = (int) mouseX;
                dragScrollOffset = scrollOffset;
                return true;
            }
        }

        // 检查子组件点击
        for (AbstractWidget child : children) {
            if (child.isMouseOver(mouseX, mouseY)) {
                if (child.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }

        // 处理悬停区域点击
        if (mouseX >= hoverAreaX && mouseX <= hoverAreaX + hoverAreaW &&
                mouseY >= hoverAreaY && mouseY <= hoverAreaY + hoverAreaH && button == 0) {
            mouseHeldInHoverArea = true;
            return true;
        }

        return false;
    }

    /**
     * 鼠标释放事件处理
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScrollbar = false;
        mouseHeldInHoverArea = false;
        for (AbstractWidget child : children) {
            if (child.mouseReleased(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    /**
     * 鼠标拖动事件处理
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // 处理滚动条拖动
        if (draggingScrollbar && scrollEnabled && contentWidth > width) {
            int scrollMax = Math.max(1, contentWidth - (width - paddingLeft - paddingRight));
            int barAreaWidth = width - paddingLeft - paddingRight;
            int barWidth = Math.max(10, (int) ((float) barAreaWidth * width / contentWidth));
            int dragDelta = (int) mouseX - dragStartX;
            int scrollDelta = (int) ((float) dragDelta * scrollMax / (barAreaWidth - barWidth));
            scrollOffset = dragScrollOffset + scrollDelta;
            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > scrollMax) scrollOffset = scrollMax;
            return true;
        }

        // 检查子组件拖动
        for (AbstractWidget child : children) {
            if (child.isMouseOver(mouseX, mouseY)) {
                if (child.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 无障碍功能叙述更新
     */
    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // 无障碍功能支持
    }

    /**
     * 键盘按键事件处理
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (AbstractWidget child : children) {
            if (child.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    // 以下为getter方法

    public List<AbstractWidget> getChildren() {
        return children;
    }

    public int getSpacing() {
        return spacing;
    }

    public int getFixedWidth() {
        return fixedWidth;
    }

    public int getHoverAreaX() {
        return hoverAreaX;
    }

    public int getHoverAreaY() {
        return hoverAreaY;
    }

    public int getHoverAreaW() {
        return hoverAreaW;
    }

    public int getHoverAreaH() {
        return hoverAreaH;
    }

    public Map<AbstractWidget, Insets> getMarginMap() {
        return marginMap;
    }

    public int getPaddingBottom() {
        return paddingBottom;
    }

    public int getPaddingTop() {
        return paddingTop;
    }

    public int getPaddingRight() {
        return paddingRight;
    }

    public int getPaddingLeft() {
        return paddingLeft;
    }

    /**
     * 清除悬停保持状态
     */
    public void clearHoverHold() {
        this.mouseHeldInHoverArea = false;
        this.draggingScrollbar = false;
    }

    public void clearChildren() {
        this.children.clear();
        this.updateLayout();
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return this.selected;
    }

    /**
     * 外边距记录类
     */
    public record Insets(int top, int right, int bottom, int left) {
        public static final Insets NONE = new Insets(0, 0, 0, 0);
    }
}
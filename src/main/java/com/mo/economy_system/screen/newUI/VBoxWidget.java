package com.mo.economy_system.screen.newUI;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VBoxWidget extends AbstractWidget {

    private final List<AbstractWidget> children = new ArrayList<>();
    private final Map<AbstractWidget, Insets> marginMap = new HashMap<>();
    private final Map<AbstractWidget, Boolean> vgrowMap = new HashMap<>();
    private AbstractWidget growSpacer = null;
    private boolean autoInsertGrowSpacer = false;
    private int spacing = 4;

    private int fixedWidth = -1;
    private int fixedHeight = -1;

    private int scrollOffset = 0;
    private boolean scrollEnabled = false;
    private boolean scrollbarAtRight = true;
    private int contentHeight = 0;
    private boolean draggingScrollbar = false;
    private int dragStartY = 0;
    private int dragScrollOffset = 0;

    private boolean mouseHeldInHoverArea = false;

    private boolean borderTop = false;
    private boolean borderRight = false;
    private boolean borderBottom = false;
    private boolean borderLeft = false;
    private int borderColor = 0xFFFFFFFF;
    private int borderThickness = 1;

    private int paddingLeft = 0;
    private int paddingRight = 0;
    private int paddingTop = 0;
    private int paddingBottom = 0;

    private final AnimationController animation = new AnimationController();
    private int hoverAreaX, hoverAreaY, hoverAreaW, hoverAreaH;

    private Integer backgroundColor = null;

    private boolean selected = false;

    public VBoxWidget(int x, int y, int width) {
        super(x, y, width, 0, null);
    }
    public VBoxWidget(int x, int y, int width, int height) {
        super(x, y, width, height, null);
    }

    public VBoxWidget(int width) {
        super(0, 0, width, 0, null);
    }

    public VBoxWidget setBoxWidth(int width) {
        this.fixedWidth = width;
        return this;
    }

    public VBoxWidget setBoxHeight(int height) {
        this.fixedHeight = height;
        return this;
    }

    public VBoxWidget setWidthFraction(int screenWidth, float fraction) {
        this.fixedWidth = (int) (screenWidth * fraction);
        return this;
    }

    public VBoxWidget enableScrollbar(boolean atRight) {
        this.scrollEnabled = true;
        this.scrollbarAtRight = atRight;
        return this;
    }

    public VBoxWidget setPadding(int top, int right, int bottom, int left) {
        this.paddingTop = top;
        this.paddingRight = right;
        this.paddingBottom = bottom;
        this.paddingLeft = left;
        updateLayout();
        return this;
    }

    public VBoxWidget setBackgroundColor(int argb) {
        this.backgroundColor = argb;
        return this;
    }

    public VBoxWidget setSpacing(int spacing) {
        this.spacing = spacing;
        updateLayout();
        return this;
    }

    public VBoxWidget setMargin(AbstractWidget widget, Insets margin) {
        marginMap.put(widget, margin);
        updateLayout();
        return this;
    }

    public VBoxWidget addChild(AbstractWidget child) {
        children.add(child);
        updateLayout();
        return this;
    }

    public VBoxWidget addAllChildren(AbstractWidget... childrenToAdd) {
        for (AbstractWidget child : childrenToAdd) {
            children.add(child);
        }
        updateLayout();
        return this;
    }

    public VBoxWidget setVGrow(AbstractWidget widget, boolean grow) {
        vgrowMap.put(widget, grow);
        updateLayout();
        return this;
    }

    public VBoxWidget enableAutoVGrowSpacer(boolean enable) {
        this.autoInsertGrowSpacer = enable;
        updateLayout();
        return this;
    }

    public VBoxWidget showBorder(boolean top, boolean right, boolean bottom, boolean left) {
        this.borderTop = top;
        this.borderRight = right;
        this.borderBottom = bottom;
        this.borderLeft = left;
        return this;
    }

    public VBoxWidget setBorderColor(int color) {
        this.borderColor = color;
        return this;
    }

    public VBoxWidget setBorderThickness(int thickness) {
        this.borderThickness = thickness;
        return this;
    }

    public VBoxWidget setHoverTriggerArea(int x, int y, int width, int height) {
        this.hoverAreaX = x;
        this.hoverAreaY = y;
        this.hoverAreaW = width;
        this.hoverAreaH = height;
        return this;
    }

    public AnimationController getAnimationController() {
        return animation;
    }

    public void updateLayout() {
        int currentY = this.getY() + paddingTop;
        int maxWidth = 0;

        int totalHeightWithoutGrow = 0;
        int growCount = 0;

        for (AbstractWidget child : children) {
            if (vgrowMap.getOrDefault(child, false)) growCount++;
            else totalHeightWithoutGrow += child.getHeight();
        }

        int totalSpacing = spacing * Math.max(0, children.size() - 1);
        totalHeightWithoutGrow += totalSpacing;

        int availableContentHeight = fixedHeight - paddingTop - paddingBottom;

        boolean shouldApplyGrow = !scrollEnabled || totalHeightWithoutGrow <= availableContentHeight;

        if (autoInsertGrowSpacer) {
            if (!shouldApplyGrow && growSpacer != null) {
                children.remove(growSpacer);
                vgrowMap.remove(growSpacer);
                growSpacer = null;
            } else if (shouldApplyGrow && growSpacer == null && fixedHeight > 0) {
                growSpacer = new AbstractWidget(0, 0, 1, 0, null) {
                    @Override
                    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
                    @Override
                    protected void updateWidgetNarration(NarrationElementOutput output) {}
                };
                children.add(children.size() - 1, growSpacer);
                vgrowMap.put(growSpacer, true);
            }
        }

        // Recalculate after spacer inserted
        totalHeightWithoutGrow = 0;
        growCount = 0;
        for (AbstractWidget child : children) {
            if (vgrowMap.getOrDefault(child, false)) growCount++;
            else totalHeightWithoutGrow += child.getHeight();
        }
        totalSpacing = spacing * Math.max(0, children.size() - 1);
        totalHeightWithoutGrow += totalSpacing;

        int growHeightEach = growCount > 0 && availableContentHeight > totalHeightWithoutGrow
                ? (availableContentHeight - totalHeightWithoutGrow) / growCount
                : 0;

        currentY = getY() + paddingTop;
        for (int i = 0; i < children.size(); i++) {
            AbstractWidget child = children.get(i);
            boolean grow = vgrowMap.getOrDefault(child, false);
            Insets margin = marginMap.getOrDefault(child, Insets.NONE);

            int top = margin.top();
            int bottom = margin.bottom();
            int left = margin.left();
            int right = margin.right();

            int h = grow ? growHeightEach : child.getHeight();
            child.setHeight(h);
            child.setY(currentY + top - scrollOffset);
            child.setX(this.getX() + paddingLeft + left);

            currentY += top + h + bottom;
            if (i < children.size() - 1) currentY += spacing;

            maxWidth = Math.max(maxWidth, child.getWidth() + left + right);
        }

        this.contentHeight = currentY - getY() - paddingTop;
        this.height = fixedHeight >= 0 ? fixedHeight : contentHeight;
        this.width = fixedWidth >= 0 ? fixedWidth : maxWidth + paddingLeft + paddingRight;

        int scrollMax = Math.max(0, contentHeight - (height - paddingTop - paddingBottom));
        if (scrollOffset > scrollMax) scrollOffset = scrollMax;
    }

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

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovering = mouseX >= hoverAreaX && mouseX <= hoverAreaX + hoverAreaW &&
                mouseY >= hoverAreaY && mouseY <= hoverAreaY + hoverAreaH;

        animation.setForcedHover(draggingScrollbar || mouseHeldInHoverArea);
        animation.update(mouseX, mouseY, hoverAreaX, hoverAreaY, hoverAreaW, hoverAreaH);

        if (animation.isEnabled()) {
            this.setX(animation.getCurrentX());
            this.setY(animation.getCurrentY());
        } else {
            this.setX(getX());
            this.setY(getY());
        }

        updateLayout();

        // 启用剪裁区域
        graphics.enableScissor(
                getX(),
                getY(),
                getX() + width,
                getY() + height
        );

        if (backgroundColor != null) {
            graphics.fill(getX(), getY(), getX() + width, getY() + height, backgroundColor);
        }

        renderBorder(graphics);

        for (AbstractWidget child : children) {
            if (isWithinScroll(child.getY(), child.getHeight())) {
                child.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        if (scrollEnabled && contentHeight > height) {
            int scrollMax = Math.max(1, contentHeight - (height - paddingTop - paddingBottom));
            float scrollRatio = (float) scrollOffset / scrollMax;
            int barX = scrollbarAtRight ? (getX() + width - 6) : getX();
            int barY = getY() + paddingTop;
            int barAreaHeight = height - paddingTop - paddingBottom;
            int barHeight = Math.max(10, (int) ((float) barAreaHeight * height / contentHeight));
            int barOffset = (int) (scrollRatio * (barAreaHeight - barHeight));
            graphics.fill(barX, barY + barOffset, barX + 6, barY + barOffset + barHeight, 0x88FFFFFF);
        }

        // 禁用剪裁区域
        graphics.disableScissor();
    }

    private boolean isWithinScroll(int childY, int childHeight) {
        return childY + childHeight > getY() && childY < getY() + height;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 先检查子控件是否需要处理滚动
        for (AbstractWidget child : children) {
            if (child.isMouseOver(mouseX, mouseY)) {
                if (child.mouseScrolled(mouseX, mouseY, delta)) {
                    return true;
                }
            }
        }

        // 如果子控件不需要处理，再处理自己的滚动
        if (scrollEnabled && contentHeight > height) {
            int scrollMax = Math.max(0, contentHeight - (height - paddingTop - paddingBottom));
            scrollOffset -= delta * 10;
            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > scrollMax) scrollOffset = scrollMax;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 先处理滚动条点击
        if (scrollEnabled && contentHeight > height && button == 0) {
            int scrollMax = Math.max(1, contentHeight - (height - paddingTop - paddingBottom));
            float scrollRatio = (float) scrollOffset / scrollMax;
            int barAreaHeight = height - paddingTop - paddingBottom;
            int barHeight = Math.max(10, (int) ((float) barAreaHeight * height / contentHeight));
            int barY = getY() + paddingTop + (int) (scrollRatio * (barAreaHeight - barHeight));
            int barX = scrollbarAtRight ? (getX() + width - 6) : getX();
            if (mouseX >= barX && mouseX <= barX + 6 && mouseY >= barY && mouseY <= barY + barHeight) {
                draggingScrollbar = true;
                dragStartY = (int) mouseY;
                dragScrollOffset = scrollOffset;
                return true;
            }
        }

        // 检查子控件点击
        for (AbstractWidget child : children) {
            if (child.isMouseOver(mouseX, mouseY)) {
                if (child.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }

        // 处理hover区域点击
        if (mouseX >= hoverAreaX && mouseX <= hoverAreaX + hoverAreaW &&
                mouseY >= hoverAreaY && mouseY <= hoverAreaY + hoverAreaH && button == 0) {
            mouseHeldInHoverArea = true;
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScrollbar = false;
        mouseHeldInHoverArea = false;
        for (AbstractWidget child : children) {
            if (child.mouseReleased(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // 1. 先处理自己的滚动条拖动
        if (draggingScrollbar && scrollEnabled && contentHeight > height) {
            int scrollMax = Math.max(1, contentHeight - (height - paddingTop - paddingBottom));
            int barAreaHeight = height - paddingTop - paddingBottom;
            int barHeight = Math.max(10, (int) ((float) barAreaHeight * height / contentHeight));
            int dragDelta = (int) mouseY - dragStartY;
            int scrollDelta = (int) ((float) dragDelta * scrollMax / (barAreaHeight - barHeight));
            scrollOffset = dragScrollOffset + scrollDelta;
            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > scrollMax) scrollOffset = scrollMax;
            return true;
        }

        // 2. 检查子控件的拖动
        for (AbstractWidget child : children) {
            if (child.isMouseOver(mouseX, mouseY)) {
                if (child.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // Narration support can be implemented here if needed
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (AbstractWidget child : children) {
            if (child.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    public List<AbstractWidget> getChildren() {
        return new ArrayList<>(children);
    }

    public int getSpacing() {
        return spacing;
    }

    public int getFixedWidth() {
        return fixedWidth;
    }

    public int getFixedHeight() {
        return fixedHeight;
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

    public int getPaddingLeft() {
        return paddingLeft;
    }

    public int getPaddingRight() {
        return paddingRight;
    }

    public int getPaddingTop() {
        return paddingTop;
    }

    public int getPaddingBottom() {
        return paddingBottom;
    }

    public void clearHoverHold() {
        this.mouseHeldInHoverArea = false;
        this.draggingScrollbar = false;
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public boolean isScrollEnabled() {
        return scrollEnabled;
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

    public record Insets(int top, int right, int bottom, int left) {
        public static final Insets NONE = new Insets(0, 0, 0, 0);
    }
}
package com.mo.economy_system.screen.components;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;

public class HBox extends ContainerWidget {
    private final int spacing;
    private boolean autoSize = true;

    public HBox(int spacing) {
        super(0, 0);
        this.spacing = spacing;
        this.autoSize = true;
    }

    public HBox(int width, int height, int spacing) {
        super(width, height);
        this.spacing = spacing;
        this.autoSize = false;
    }

    @Override
    public void addChild(AbstractWidget child) {
        super.addChild(child);
        updateLayout();
    }

    @Override
    public void updateLayout() {
        int totalWidth = children.stream().filter(c -> !(c instanceof SpacerWidget))
                .mapToInt(AbstractWidget::getWidth).sum() + spacing * (children.size() - 1);
        int maxHeight = children.stream().mapToInt(AbstractWidget::getHeight).max().orElse(0);

        if (autoSize) {
            this.width = totalWidth + paddingLeft + paddingRight;
            this.height = maxHeight + paddingTop + paddingBottom;
        } else {
            this.width = Math.max(this.width, totalWidth + paddingLeft + paddingRight);
            this.height = Math.max(this.height, maxHeight + paddingTop + paddingBottom);
        }

        int availableWidth = this.width - paddingLeft - paddingRight;
        int expandableSpacers = (int) children.stream().filter(c -> c instanceof SpacerWidget && ((SpacerWidget) c).isExpandHorizontally()).count();
        int remainingSpace = Math.max(0, availableWidth - totalWidth);
        int extraWidthPerSpacer = expandableSpacers > 0 ? remainingSpace / expandableSpacers : 0;

        int currentX = this.getX() + paddingLeft;
        int parentY = this.getY() + paddingTop;

        for (AbstractWidget child : children) {
            int childWidth = Math.min(child.getWidth(), availableWidth);
            int childHeight = Math.min(child.getHeight(), this.height - paddingTop - paddingBottom);

            if (child instanceof SpacerWidget spacer && spacer.isExpandHorizontally()) {
                childWidth += extraWidthPerSpacer;
            }

            child.setWidth(childWidth);
            child.setHeight(childHeight);
            child.setPosition(currentX, parentY + (this.height - paddingTop - paddingBottom - childHeight) / 2);
            currentX += childWidth + spacing;
        }
    }


    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}


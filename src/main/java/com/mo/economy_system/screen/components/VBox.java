package com.mo.economy_system.screen.components;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;

public class VBox extends ContainerWidget {
    private final int spacing;
    private boolean autoSize = true;

    public VBox(int spacing) {
        super(0, 0);
        this.spacing = spacing;
        this.autoSize = true;
    }

    public VBox(int width, int height, int spacing) {
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
        int totalHeight = children.stream().filter(c -> !(c instanceof SpacerWidget))
                .mapToInt(AbstractWidget::getHeight).sum() + spacing * (children.size() - 1);
        int maxWidth = children.stream().mapToInt(AbstractWidget::getWidth).max().orElse(0);

        if (autoSize) {
            this.width = maxWidth + paddingLeft + paddingRight;
            this.height = totalHeight + paddingTop + paddingBottom;
        } else {
            this.width = Math.max(this.width, maxWidth + paddingLeft + paddingRight);
            this.height = Math.max(this.height, totalHeight + paddingTop + paddingBottom);
        }

        int availableHeight = this.height - paddingTop - paddingBottom;
        int expandableSpacers = (int) children.stream().filter(c -> c instanceof SpacerWidget && ((SpacerWidget) c).isExpandVertically()).count();
        int remainingSpace = Math.max(0, availableHeight - totalHeight);
        int extraHeightPerSpacer = expandableSpacers > 0 ? remainingSpace / expandableSpacers : 0;

        int parentX = this.getX() + paddingLeft;
        int currentY = this.getY() + paddingTop;

        for (AbstractWidget child : children) {
            int childWidth = Math.min(child.getWidth(), this.width - paddingLeft - paddingRight);
            int childHeight = Math.min(child.getHeight(), availableHeight);

            if (child instanceof SpacerWidget spacer && spacer.isExpandVertically()) {
                childHeight += extraHeightPerSpacer;
            }

            child.setWidth(childWidth);
            child.setHeight(childHeight);
            child.setPosition(parentX + (this.width - paddingLeft - paddingRight - childWidth) / 2, currentY);
            currentY += childHeight + spacing;
        }
    }


    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}



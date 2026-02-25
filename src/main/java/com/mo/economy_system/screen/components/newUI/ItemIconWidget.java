package com.mo.economy_system.screen.components.newUI;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ItemIconWidget extends AbstractWidget {
    private final ItemStack itemStack;
    private final Font font;
    private boolean showDecorations = true;
    private float scale = 1.0f;
    private List<Component> tooltipLines = List.of();

    public ItemIconWidget(ItemStack itemStack, Font font, int x, int y) {
        super(x, y, 16, 16, Component.empty());
        this.itemStack = itemStack;
        this.font = font;
    }

    public ItemIconWidget setShowDecorations(boolean showDecorations) {
        this.showDecorations = showDecorations;
        return this;
    }

    public ItemIconWidget setScale(float scale) {
        this.scale = scale;
        this.width = (int) (16 * scale);
        this.height = (int) (16 * scale);
        return this;
    }

    public ItemIconWidget setTooltipLines(List<Component> lines) {
        this.tooltipLines = lines;
        return this;
    }

    public List<Component> getTooltipLines() {
        return tooltipLines;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushPose();
        graphics.pose().translate(getX(), getY(), 0);
        graphics.pose().scale(scale, scale, 1.0f);

        graphics.renderItem(itemStack, 0, 0);
        if (showDecorations) {
            graphics.renderItemDecorations(font, itemStack, 0, 0);
        }

        if (isHovered()) {
            // System.out.println("11111111111111111");
            List<Component> tooltip = itemStack.getTooltipLines(
                    Minecraft.getInstance().player,
                    Minecraft.getInstance().options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL
            );

            /*graphics.renderTooltip(
                    Minecraft.getInstance().font,
                    tooltip,
                    Optional.empty(),
                    mouseX,
                    mouseY
            );*/
        }

        graphics.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}
}



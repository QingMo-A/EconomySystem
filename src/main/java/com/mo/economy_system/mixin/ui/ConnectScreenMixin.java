package com.mo.economy_system.mixin.ui;

import com.mo.economy_system.client.util.LoadingTips;
import com.mo.economy_system.client.util.UiBackgroundRenderer;
import com.mo.economy_system.client.util.VirtualCoordinateHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin extends Screen {

    @Unique private static final int ACCENT_GREEN = 0xFF3FBF7F;
    @Unique private static final int BAR_BG = 0x66000000;
    @Unique private static final int BAR_HIGHLIGHT = 0xFF8EF0B8;

    @Unique private final VirtualCoordinateHelper.VirtualSizeResult vs = new VirtualCoordinateHelper.VirtualSizeResult();
    @Unique private String tip = "";
    @Unique private Button economySystem$cancelBtn;

    protected ConnectScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void economySystem$init(CallbackInfo ci) {
        ci.cancel();
        VirtualCoordinateHelper.calculateVirtualSize(this, vs);

        String cancelText = "← 取消连接";
        int textW = this.font.width(cancelText);
        int btnX = (int) (8 * vs.uiScale);
        int btnY = this.height - (int) (50 * vs.uiScale);

        economySystem$cancelBtn = new TextCancelButton(btnX, btnY, textW + 8, 12,
            Component.literal(cancelText), btn -> economySystem$disconnect());
        this.addRenderableWidget(economySystem$cancelBtn);

        if (tip.isEmpty()) {
            tip = LoadingTips.getRandomTip();
        }
    }

    @Unique
    private void economySystem$disconnect() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) mc.getConnection().close();
        if (mc.level != null) mc.level.disconnect();
        mc.disconnect();
        mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void economySystem$render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ci.cancel();

        VirtualCoordinateHelper.calculateVirtualSize(this, vs);
        float scale = vs.uiScale;
        int vw = vs.virtualWidth;
        int vh = vs.virtualHeight;

        UiBackgroundRenderer.renderCyclingBackground(guiGraphics, this.width, this.height);
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0x88000000, 0xCC000000);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1.0f);

        // 左上角提示
        renderTip(guiGraphics);

        // 底部进度条（循环动画）
        int barMargin = 32;
        int barHeight = 6;
        int barX = barMargin;
        int barW = vw - barMargin * 2;
        int barY = vh - 28;

        long now = System.currentTimeMillis();
        int fakeProgress = (int) ((now % 6000) * 100 / 6000);

        String label = "正在连接到服务器... " + fakeProgress + "%";
        int labelW = this.font.width(label);
        guiGraphics.drawString(this.font, label, barX + barW - labelW, barY - 12, 0xFFFFFFFF, true);

        renderRoundedBar(guiGraphics, barX, barY, barW, barHeight, BAR_BG);
        int fillW = barW * fakeProgress / 100;
        if (fillW > 0) {
            renderRoundedBar(guiGraphics, barX, barY, fillW, barHeight, ACCENT_GREEN);
            if (fillW > 2) {
                guiGraphics.fill(barX + 2, barY, barX + fillW - 2, barY + 1, BAR_HIGHLIGHT);
            }
        }

        guiGraphics.pose().popPose();

        // 按钮在屏幕坐标渲染
        if (economySystem$cancelBtn != null) {
            economySystem$cancelBtn.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Unique
    private void renderTip(GuiGraphics guiGraphics) {
        int tipX = 8;
        int tipY = 8;
        guiGraphics.drawString(this.font, "§e💡 提示", tipX, tipY, 0xFFFFFFFF, true);
        guiGraphics.drawString(this.font, "§7" + tip, tipX, tipY + 13, 0xFFAAAAAA, true);
    }

    @Unique
    private static void renderRoundedBar(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        int r = h >= 6 ? h / 3 : 1;
        int ih = Math.max(1, h - 2);
        int left = x + r;
        int right = x + w - r;
        if (right > left) g.fill(left, y, right, y + h, color);
        g.fill(x, y + 1, x + r, y + 1 + ih, color);
        g.fill(x + w - r, y + 1, x + w, y + 1 + ih, color);
    }

    @Unique
    private static class TextCancelButton extends Button {
        TextCancelButton(int x, int y, int w, int h, Component msg, OnPress onPress) {
            super(x, y, w, h, msg, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            boolean hov = isHovered();
            String text = getMessage().getString();
            int color = hov ? 0xFF88FFAA : 0xFFAAAAAA;
            g.drawString(Minecraft.getInstance().font, text, getX(), getY(), color, true);
            if (hov) {
                int tw = Minecraft.getInstance().font.width(text);
                g.fill(getX(), getY() + 10, getX() + tw, getY() + 11, 0xFF3FBF7F);
            }
        }
    }
}

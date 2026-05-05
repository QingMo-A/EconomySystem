package com.mo.economy_system.mixin.ui;

import com.mo.economy_system.client.util.LoadingTips;
import com.mo.economy_system.client.util.UiBackgroundRenderer;
import com.mo.economy_system.client.util.VirtualCoordinateHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin extends Screen {

    @Unique private static final int ACCENT_BLUE = 0xFF0088FF;
    @Unique private static final int BAR_BG = 0x66000000;
    @Unique private static final int BAR_HIGHLIGHT = 0xFF55AAFF;

    @Shadow @Final private StoringChunkProgressListener progressListener;

    @Unique private final VirtualCoordinateHelper.VirtualSizeResult vs = new VirtualCoordinateHelper.VirtualSizeResult();
    @Unique private String tip = "";

    protected LevelLoadingScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void economySystem$render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ci.cancel();

        VirtualCoordinateHelper.calculateVirtualSize(this, vs);
        if (tip.isEmpty()) {
            tip = LoadingTips.getRandomTip();
        }

        UiBackgroundRenderer.renderCyclingBackground(guiGraphics, this.width, this.height);
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0x88000000, 0xCC000000);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(vs.uiScale, vs.uiScale, 1.0f);

        int vw = vs.virtualWidth;
        int vh = vs.virtualHeight;

        // 左上角提示
        renderTip(guiGraphics);

        // 底部进度条
        int barMargin = 32;
        int barHeight = 6;
        int barX = barMargin;
        int barW = vw - barMargin * 2;
        int barY = vh - 28;

        int progress = Mth.clamp(progressListener.getProgress(), 0, 100);

        // 进度条上方右侧文字
        String label = "正在加载世界... " + progress + "%";
        int labelW = this.font.width(label);
        guiGraphics.drawString(this.font, label, barX + barW - labelW, barY - 12, 0xFFFFFFFF, true);

        // 背景条
        renderRoundedBar(guiGraphics, barX, barY, barW, barHeight, BAR_BG);

        // 进度条
        int fillW = barW * progress / 100;
        if (fillW > 0) {
            renderRoundedBar(guiGraphics, barX, barY, fillW, barHeight, ACCENT_BLUE);
            if (fillW > 2) {
                guiGraphics.fill(barX + 2, barY, barX + fillW - 2, barY + 1, BAR_HIGHLIGHT);
            }
        }

        guiGraphics.pose().popPose();
    }

    @Unique
    private void renderTip(GuiGraphics guiGraphics) {
        int tipX = 8;
        int tipY = 8;
        String header = "§e💡 提示";
        guiGraphics.drawString(this.font, header, tipX, tipY, 0xFFFFFFFF, true);
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
}

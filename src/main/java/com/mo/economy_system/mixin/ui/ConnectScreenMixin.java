package com.mo.economy_system.mixin.ui;

import com.mo.economy_system.client.util.VirtualCoordinateHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ConnectScreen Mixin
 * 统一服务器连接界面样式为：背景图 + 绿色玻璃面板 + 动态加载条。
 */
@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin extends Screen {

    // ===== 视觉样式常量 =====
    private static final int ACCENT_GREEN = 0xFF3FBF7F;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_GRAY = 0xFFAAAAAA;
    private static final int GLASS_TOP = 0x663FAF7F;
    private static final int GLASS_BOTTOM = 0x33102218;
    private static final int GLASS_BORDER = 0x5590D9B0;
    private static final int GLASS_SHADOW = 0x33201028;
    private static final int GLASS_HIGHLIGHT = 0x66B8FFD5;
    private static final int PADDING = 12;

    @Unique
    private final VirtualCoordinateHelper.VirtualSizeResult virtualSize = new VirtualCoordinateHelper.VirtualSizeResult();

    // 连接界面背景图（与加载界面保持一致）
    @Unique
    private static final ResourceLocation BACKGROUND_TEXTURE =
        new ResourceLocation("economy_system", "background.png");

    @Unique
    private Button economySystem$disconnectButton;

    protected ConnectScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void economySystem$init(CallbackInfo ci) {
        // 取消原版 init，改用自定义布局
        ci.cancel();
        initCustomScreen();
    }

    /**
     * 初始化按钮并将虚拟坐标转换为实际屏幕坐标。
     */
    @Unique
    private void initCustomScreen() {
        VirtualCoordinateHelper.calculateVirtualSize(this, virtualSize);

        int centerX = virtualSize.virtualWidth / 2;
        int centerY = virtualSize.virtualHeight / 2;

        int boxWidth = 400;
        int boxHeight = 170;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        int buttonWidth = boxWidth - 2 * PADDING;
        int buttonHeight = 24;
        int virtualButtonX = boxX + PADDING;
        int virtualButtonY = boxY + 132;

        int screenButtonX = (int) (virtualButtonX * virtualSize.uiScale);
        int screenButtonY = (int) (virtualButtonY * virtualSize.uiScale);
        int screenButtonWidth = (int) (buttonWidth * virtualSize.uiScale);
        int screenButtonHeight = (int) (buttonHeight * virtualSize.uiScale);

        economySystem$disconnectButton = new CustomButton(
            screenButtonX,
            screenButtonY,
            screenButtonWidth,
            screenButtonHeight,
            Component.literal("取消连接"),
            btn -> economySystem$disconnect()
        );
        this.addRenderableWidget(economySystem$disconnectButton);
    }

    @Unique
    private void economySystem$disconnect() {
        Minecraft mc = Minecraft.getInstance();
        // 主动关闭连接并返回标题界面
        if (mc.getConnection() != null) {
            mc.getConnection().close();
        }
        if (mc.level != null) {
            mc.level.disconnect();
        }
        mc.clearLevel(null);
        mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void economySystem$render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        // 取消原版 render，完全接管绘制
        ci.cancel();
        renderCustomScreen(guiGraphics, mouseX, mouseY, partialTick);
    }

    /**
     * 渲染连接界面主体。
     */
    @Unique
    private void renderCustomScreen(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        VirtualCoordinateHelper.calculateVirtualSize(this, virtualSize);

        // 背景图 + 暗色遮罩
        guiGraphics.blit(BACKGROUND_TEXTURE,
            0, 0, this.width, this.height,
            0, 0, 256, 144, 256, 144);
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0x88000000, 0xCC000000);

        // 切换到虚拟分辨率坐标系，保证不同分辨率布局一致
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(virtualSize.uiScale, virtualSize.uiScale, 1.0f);

        int centerX = virtualSize.virtualWidth / 2;
        int centerY = virtualSize.virtualHeight / 2;

        int boxWidth = 400;
        int boxHeight = 170;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        // 绿色玻璃主面板
        economySystem$renderGlassPanel(guiGraphics, boxX, boxY, boxWidth, boxHeight, 0xAA78C89A);

        // 标题与分隔线
        guiGraphics.drawCenteredString(this.font, "正在连接服务器...", centerX, boxY + 16, TEXT_WHITE);
        guiGraphics.fill(boxX + PADDING, boxY + 34, boxX + boxWidth - PADDING, boxY + 35, ACCENT_GREEN);

        // 动态连接状态文字（点动画）
        long time = System.currentTimeMillis();
        int dots = (int) ((time / 500) % 4);
        String loadingDots = ".".repeat(dots);
        guiGraphics.drawCenteredString(this.font, "正在建立连接" + loadingDots, centerX, boxY + 70, TEXT_GRAY);

        // 动态进度条动画（时间驱动）
        int progressBarY = boxY + 108;
        int progressBarWidth = boxWidth - 2 * PADDING;
        int progressBarX = boxX + PADDING;
        int progressBarHeight = 6;

        economySystem$renderRoundedBar(guiGraphics, progressBarX, progressBarY, progressBarWidth, progressBarHeight, 0x66000000);
        int progressWidth = (int) ((time % 2000) / 2000.0f * progressBarWidth);
        economySystem$renderRoundedBar(guiGraphics, progressBarX, progressBarY, progressWidth, progressBarHeight, ACCENT_GREEN);
        if (progressWidth > 2) {
            guiGraphics.fill(progressBarX + 2, progressBarY, progressBarX + progressWidth - 2, progressBarY + 1, 0xFF8EF0B8);
        }

        // 恢复屏幕坐标系
        guiGraphics.pose().popPose();

        // 按钮使用屏幕坐标渲染
        if (economySystem$disconnectButton != null) {
            economySystem$disconnectButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    /**
     * 绘制玻璃风格面板。
     */
    @Unique
    private void economySystem$renderGlassPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int tint) {
        guiGraphics.fillGradient(x, y, x + width, y + height, GLASS_TOP, GLASS_BOTTOM);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, economySystem$withAlpha(tint, 0x12));
        guiGraphics.fill(x, y, x + width, y + 1, GLASS_BORDER);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, GLASS_SHADOW);
        guiGraphics.fill(x, y, x + 1, y + height, GLASS_BORDER);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, GLASS_SHADOW);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + 2, GLASS_HIGHLIGHT);
        guiGraphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, GLASS_SHADOW);
        economySystem$renderGlassNoise(guiGraphics, x, y, width, height);
    }

    /**
     * 玻璃表面细微噪点，避免纯色过于平。
     */
    @Unique
    private void economySystem$renderGlassNoise(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        if (width < 20 || height < 20) {
            return;
        }
        int maxX = x + width - 6;
        int maxY = y + height - 6;
        for (int i = 0; i < 6; i++) {
            int nx = x + 6 + (i * 23 + x) % (maxX - x);
            int ny = y + 6 + (i * 17 + y) % (maxY - y);
            guiGraphics.fill(nx, ny, nx + 1, ny + 1, 0x22FFFFFF);
        }
    }

    /**
     * 替换颜色透明度通道。
     */
    @Unique
    private int economySystem$withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    /**
     * 绘制圆角风格进度条（用于背景和前景）。
     */
    @Unique
    private void economySystem$renderRoundedBar(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int radius = height >= 6 ? 2 : 1;
        int innerHeight = Math.max(1, height - 2);
        int left = x + radius;
        int right = x + width - radius;
        if (right > left) {
            guiGraphics.fill(left, y, right, y + height, color);
        }
        guiGraphics.fill(x, y + 1, x + radius, y + 1 + innerHeight, color);
        guiGraphics.fill(x + width - radius, y + 1, x + width, y + 1 + innerHeight, color);
    }

    /**
     * 自定义按钮，保持与玻璃主题一致的绿色样式。
     */
    @Unique
    private static class CustomButton extends Button {
        public CustomButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHovered() || isFocused();

            int topColor;
            int bottomColor;
            int borderColor;
            if (hovered) {
                topColor = 0xCC2E7A58;
                bottomColor = 0xCC1C4A35;
                borderColor = 0xFF89E2B5;
            } else {
                topColor = 0xCC255F46;
                bottomColor = 0xCC173A2B;
                borderColor = 0xCC66B78E;
            }

            int x = getX();
            int y = getY();
            int w = width;
            int h = height;

            guiGraphics.fill(x + 2, y, x + w - 2, y + h, topColor);
            guiGraphics.fill(x + 2, y + h, x + w - 2, y + h + 1, bottomColor);

            guiGraphics.fill(x + 1, y, x + 2, y + h, borderColor);
            guiGraphics.fill(x + w - 2, y, x + w - 1, y + h, borderColor);
            guiGraphics.fill(x + 2, y, x + w - 2, y + 1, borderColor);
            guiGraphics.fill(x + 2, y + h - 1, x + w - 2, y + h, borderColor);

            guiGraphics.fill(x, y, x + 1, y + 1, borderColor);
            guiGraphics.fill(x + w - 1, y, x + w, y + 1, borderColor);
            guiGraphics.fill(x, y + h - 1, x + 1, y + h, borderColor);
            guiGraphics.fill(x + w - 1, y + h - 1, x + w, y + h, borderColor);

            String displayText = getMessage().getString();
            int textX = x + w / 2 - Minecraft.getInstance().font.width(displayText) / 2;
            int textY = y + (h - 8) / 2;
            guiGraphics.drawString(Minecraft.getInstance().font, displayText, textX, textY, 0xFFFFFF, false);
        }
    }
}
package com.mo.economy_system.mixin.ui;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.client.util.UiBackgroundRenderer;
import com.mo.economy_system.client.util.VirtualCoordinateHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * DisconnectedScreen Mixin
 * 使用虚拟坐标系统（640×360）的自定义断开连接界面
 */
@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {

    // ==================== 虚拟基准尺寸 ====================
    private static final int BASE_WIDTH = 640;
    private static final int BASE_HEIGHT = 360;

    // ==================== 颜色定义 - 暗红色调 ====================
    private static final int GLASS_TOP = 0x66D16868;
    private static final int GLASS_BOTTOM = 0x33201010;
    private static final int GLASS_BORDER = 0x55FF9C9C;
    private static final int GLASS_SHADOW = 0x33280000;
    private static final int GLASS_HIGHLIGHT = 0x66FFD2D2;
    private static final int ACCENT_RED = 0xFFCC0000;

    // ==================== 虚拟坐标系统变量 ====================
    @Unique
    private final VirtualCoordinateHelper.VirtualSizeResult virtualSize = new VirtualCoordinateHelper.VirtualSizeResult();

    @Unique
    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("economy_system", "background.png");

    @Unique
    private Button economySystem$returnButton;

    @Shadow
    private Component reason;

    @Shadow
    private Screen parent;

    protected DisconnectedScreenMixin(Component title) {
        super(title);
    }

    /**
     * 检查是否是复活点数耗尽
     */
    @Unique
    private boolean isPermaDeathDisconnect() {
        if (this.reason == null) return false;
        String msg = this.reason.getString();
        return msg.contains("复活点数耗尽") || msg.contains("细胞分裂");
    }

    /**
     * 检查是否是封禁
     */
    @Unique
    private boolean isBanDisconnect() {
        if (this.reason == null) return false;
        String msg = this.reason.getString();
        return msg.contains("banned") || msg.contains("封禁") || msg.contains("banned.expiration");
    }

    /**
     * 注入 init() 方法
     */
    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void economySystem$init(CallbackInfo ci) {
        ci.cancel();
        initCustomScreen();
    }

    /**
     * 初始化自定义屏幕
     */
    @Unique
    private void initCustomScreen() {
        // 计算虚拟尺寸
        VirtualCoordinateHelper.calculateVirtualSize(this, virtualSize);

        // 虚拟坐标下的按钮位置
        int centerX = virtualSize.virtualWidth / 2;
        int centerY = virtualSize.virtualHeight / 2;

        int boxWidth = 420;
        int boxHeight = 200;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        int buttonWidth = 380;
        int buttonHeight = 24;
        // 按钮在框内水平居中
        int virtualButtonX = boxX + (boxWidth - buttonWidth) / 2;
        int virtualButtonY = boxY + boxHeight - 40;
        // 转换虚拟坐标到屏幕坐标
        int screenButtonX = (int) (virtualButtonX * virtualSize.uiScale);
        int screenButtonY = (int) (virtualButtonY * virtualSize.uiScale);
        int screenButtonWidth = (int) (buttonWidth * virtualSize.uiScale);
        int screenButtonHeight = (int) (buttonHeight * virtualSize.uiScale);

        // 确定按钮文字和目标屏幕
        Component buttonText;
        Button.OnPress onPress;
        if (parent instanceof ConnectScreen) {
            buttonText = Component.literal("§c返回服务器列表");
            onPress = btn -> Minecraft.getInstance().setScreen(new TitleScreen());
        } else {
            buttonText = Component.literal("§c返回标题界面");
            onPress = btn -> economySystem$returnToTitleScreen();
        }

        economySystem$returnButton = new CustomButton(
                screenButtonX, screenButtonY,
                screenButtonWidth, screenButtonHeight,
                buttonText,
                onPress,
                virtualSize.uiScale
        );
        this.addRenderableWidget(economySystem$returnButton);
    }

    /**
     * 返回标题界面
     */
    @Unique
    private void economySystem$returnToTitleScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            mc.level.disconnect();
        }
        mc.disconnect();
        mc.setScreen(new TitleScreen());
    }

    /**
     * 注入 render() 方法
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void economySystem$render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ci.cancel();
        renderCustomScreen(guiGraphics, mouseX, mouseY, partialTick);
    }

    /**
     * 渲染自定义屏幕
     */
    @Unique
    private void renderCustomScreen(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 每帧重新计算虚拟尺寸（支持窗口大小变化）
        VirtualCoordinateHelper.calculateVirtualSize(this, virtualSize);

        // ========== 背景（使用屏幕坐标） ==========
        UiBackgroundRenderer.renderCover(guiGraphics, BACKGROUND_TEXTURE, this.width, this.height);
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0x88000000, 0xCC000000);

        // ========== 应用虚拟坐标缩放 ==========
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(virtualSize.uiScale, virtualSize.uiScale, 1.0f);

        // ========== 虚拟坐标下的布局 ==========
        int centerX = virtualSize.virtualWidth / 2;
        int centerY = virtualSize.virtualHeight / 2;

        int boxWidth = 420;
        int boxHeight = 200;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        // 主面板
        renderGlassPanel(guiGraphics, boxX, boxY, boxWidth, boxHeight, 0xAAC25E5E);

        // 标题区域
        PoseStack poseStack = guiGraphics.pose();

        // 骷髅图标
        String skullIcon = "☠";
        poseStack.pushPose();
        poseStack.scale(3.0f, 3.0f, 1.0f);
        int skullX = (int) ((boxX + 25) / 3.0f);
        int skullY = (int) ((boxY + 20) / 3.0f);
        guiGraphics.drawString(this.font, skullIcon, skullX, skullY, ACCENT_RED, false);
        poseStack.popPose();

        // 标题
        poseStack.pushPose();
        poseStack.scale(2.2f, 2.2f, 1.0f);
        String titleText = isPermaDeathDisconnect() ? "§c§l布豪，您趋势了！" : "§c§l布豪，连接失败！";
        int titleX = (int) ((centerX + 15) / 2.2f - font.width(titleText) / 2.0f);
        int titleY = (int) ((boxY + 28) / 2.2f);
        guiGraphics.drawString(this.font, titleText, titleX, titleY, 0xFFFFFFFF, false);
        poseStack.popPose();

        // 右上角：DreamingFish
        String domainText = "§b§lDreaming§d§lFish";
        int domainX = boxX + boxWidth - 12 - font.width(domainText);
        int domainY = boxY + 15;
        guiGraphics.drawString(this.font, domainText, domainX, domainY, 0xFFFFFFFF, false);

        // 分隔线
        int lineY = boxY + 55;
        guiGraphics.fill(boxX + 12, lineY, boxX + boxWidth - 12, lineY + 2, ACCENT_RED);
        guiGraphics.fill(boxX + 12, lineY + 3, boxX + boxWidth - 12, lineY + 4, 0xAA660000);

        // 消息内容
        if (isPermaDeathDisconnect()) {
            renderPermaDeathMessage(guiGraphics, centerX, boxY);
        } else {
            renderDisconnectMessage(guiGraphics, centerX, boxY);
        }

        // 恢复矩阵状态
        guiGraphics.pose().popPose();

        // ========== 渲染按钮（使用屏幕坐标） ==========
        if (economySystem$returnButton != null) {
            economySystem$returnButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    /**
     * 渲染复活点数耗尽消息
     */
    @Unique
    private void renderPermaDeathMessage(GuiGraphics guiGraphics, int centerX, int boxY) {
        String[] messageLines = {
                "§c很不幸，您的细胞分裂点数已经耗尽...",
                "§7您已无法通过常规手段重生",
                "§7请等待其他幸存者来拯救您...",
        };

        int messageStartY = boxY + 75;
        for (int i = 0; i < messageLines.length; i++) {
            String line = messageLines[i];
            guiGraphics.drawCenteredString(this.font, line, centerX, messageStartY + i * 18, 0xFFFFFFFF);
        }
    }

    /**
     * 渲染普通断开连接消息
     */
    @Unique
    private void renderDisconnectMessage(GuiGraphics guiGraphics, int centerX, int boxY) {
        String messageText = this.reason.getString();

        if (isBanDisconnect()) {
            String expiration = extractBanExpiration(messageText);
            String formattedExpiration = formatExpirationTime(expiration);

            String[] banLines = {
                    "§c您已被服务器封禁",
                    "§7原因: §f" + extractBanReason(messageText),
                    "§7解封时间: " + formattedExpiration,
                    "§7如有疑问请联系服务器管理员"
            };

            int messageStartY = boxY + 75;
            for (int i = 0; i < banLines.length; i++) {
                guiGraphics.drawCenteredString(this.font, banLines[i], centerX, messageStartY + i * 18, 0xFFFFFFFF);
            }
        } else {
            int maxWidth = 380;
            int messageStartY = boxY + 80;

            if (font.width(messageText) > maxWidth) {
                String[] words = messageText.split(" ");
                StringBuilder currentLine = new StringBuilder();
                int currentY = messageStartY;

                for (String word : words) {
                    String testLine = currentLine.length() > 0
                            ? currentLine + " " + word
                            : word;

                    if (font.width(testLine) > maxWidth && currentLine.length() > 0) {
                        guiGraphics.drawCenteredString(this.font, currentLine.toString(), centerX, currentY, 0xFFFFFFFF);
                        currentLine = new StringBuilder(word);
                        currentY += 18;
                    } else {
                        currentLine = new StringBuilder(testLine);
                    }
                }
                if (currentLine.length() > 0) {
                    guiGraphics.drawCenteredString(this.font, currentLine.toString(), centerX, currentY, 0xFFFFFFFF);
                }
            } else {
                guiGraphics.drawCenteredString(this.font, messageText, centerX, messageStartY, 0xFFFFFFFF);
            }
        }
    }

    /**
     * 从封禁消息中提取原因
     */
    @Unique
    private String extractBanReason(String fullMessage) {
        EconomySystem.LOGGER.info("封禁消息原始内容: [{}]", fullMessage);

        // 中文格式："原因：{原因}"
        if (fullMessage.contains("原因：")) {
            int reasonIndex = fullMessage.indexOf("原因：") + 3;
            int endIndex = fullMessage.length();

            if (fullMessage.contains("解封时间")) {
                endIndex = fullMessage.indexOf("解封时间");
            } else if (fullMessage.contains("\n")) {
                int newlineIndex = fullMessage.indexOf("\n", reasonIndex);
                if (newlineIndex != -1) endIndex = newlineIndex;
            }

            String reason = fullMessage.substring(reasonIndex, endIndex).trim();
            EconomySystem.LOGGER.info("提取到的原因: [{}]", reason);
            if (!reason.isEmpty()) return reason;
        }

        // 英文格式: "Reason: {reason}"
        if (fullMessage.contains("Reason:")) {
            int reasonIndex = fullMessage.indexOf("Reason:") + 7;
            int endIndex = fullMessage.length();

            if (fullMessage.contains("Your ban will be removed on")) {
                endIndex = fullMessage.indexOf("Your ban will be removed on");
            } else if (fullMessage.contains("\n")) {
                int newlineIndex = fullMessage.indexOf("\n", reasonIndex);
                if (newlineIndex != -1) endIndex = newlineIndex;
            }

            String reason = fullMessage.substring(reasonIndex, endIndex).trim();
            if (!reason.isEmpty()) return reason;
        }

        // 兼容旧版中文冒号
        if (fullMessage.contains("原因:")) {
            int reasonIndex = fullMessage.indexOf("原因:") + 3;
            int endIndex = fullMessage.length();

            if (fullMessage.contains("\n")) {
                int newlineIndex = fullMessage.indexOf("\n", reasonIndex);
                if (newlineIndex != -1) endIndex = newlineIndex;
            }

            String reason = fullMessage.substring(reasonIndex, endIndex).trim();
            if (!reason.isEmpty()) return reason;
        }

        return "违反服务器规则";
    }

    /**
     * 从封禁消息中提取解封时间
     */
    @Unique
    private String extractBanExpiration(String fullMessage) {
        if (fullMessage.contains("Your ban will be removed on")) {
            int index = fullMessage.indexOf("Your ban will be removed on") + 27;
            String expiration = fullMessage.substring(index).trim();
            return expiration.isEmpty() ? "未知" : expiration;
        }
        if (fullMessage.contains("解封时间:")) {
            int index = fullMessage.indexOf("解封时间:") + 5;
            String expiration = fullMessage.substring(index).trim();
            return expiration.isEmpty() ? "未知" : expiration;
        }
        if (fullMessage.contains("解封于")) {
            int index = fullMessage.indexOf("解封于") + 3;
            String expiration = fullMessage.substring(index).trim();
            return expiration.isEmpty() ? "未知" : expiration;
        }
        return null;
    }

    /**
     * 格式化封禁时间
     */
    @Unique
    private String formatExpirationTime(String expiration) {
        if (expiration == null) return "§c永久封禁";
        if (expiration.equalsIgnoreCase("forever") || expiration.contains("永久")) return "§c永久封禁";
        return "§e" + expiration;
    }

    /**
     * 绘制圆角矩形
     */
    @Unique
    private void renderGlassPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int tint) {
        guiGraphics.fillGradient(x, y, x + width, y + height, GLASS_TOP, GLASS_BOTTOM);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, withAlpha(tint, 0x12));
        guiGraphics.fill(x, y, x + width, y + 1, GLASS_BORDER);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, GLASS_SHADOW);
        guiGraphics.fill(x, y, x + 1, y + height, GLASS_BORDER);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, GLASS_SHADOW);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + 2, GLASS_HIGHLIGHT);
        guiGraphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, GLASS_SHADOW);
        renderGlassNoise(guiGraphics, x, y, width, height);
    }

    @Unique
    private void renderGlassNoise(GuiGraphics guiGraphics, int x, int y, int width, int height) {
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

    @Unique
    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    /**
     * 自定义按钮（使用屏幕坐标）
     */
    @Unique
    private static class CustomButton extends Button {
        private final float virtualScale;

        public CustomButton(int x, int y, int width, int height, Component message, OnPress onPress, float virtualScale) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.virtualScale = virtualScale;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHovered() || isFocused();

            int topColor, bottomColor, borderColor;
            if (hovered) {
                topColor = 0xCC660000;
                bottomColor = 0xCC330000;
                borderColor = 0xFFCC0000;
            } else {
                topColor = 0xCC440000;
                bottomColor = 0xCC220000;
                borderColor = 0xCC880000;
            }

            int x = getX();
            int y = getY();
            int w = width;
            int h = height;

            // 渐变背景
            guiGraphics.fill(x + 2, y, x + w - 2, y + h, topColor);
            guiGraphics.fill(x + 2, y + h, x + w - 2, y + h + 1, bottomColor);

            // 边框
            guiGraphics.fill(x + 1, y, x + 2, y + h, borderColor);
            guiGraphics.fill(x + w - 2, y, x + w - 1, y + h, borderColor);
            guiGraphics.fill(x + 2, y, x + w - 2, y + 1, borderColor);
            guiGraphics.fill(x + 2, y + h - 1, x + w - 2, y + h, borderColor);

            // 角落装饰
            guiGraphics.fill(x, y, x + 1, y + 1, borderColor);
            guiGraphics.fill(x + w - 1, y, x + w, y + 1, borderColor);
            guiGraphics.fill(x, y + h - 1, x + 1, y + h, borderColor);
            guiGraphics.fill(x + w - 1, y + h - 1, x + w, y + h, borderColor);

            // 文字
            String displayText = getMessage().getString();
            int textX = x + w / 2 - Minecraft.getInstance().font.width(displayText) / 2;
            int textY = y + (h - 8) / 2;
            guiGraphics.drawString(Minecraft.getInstance().font, displayText, textX, textY, 0xFFFFFF, false);
        }
    }
}

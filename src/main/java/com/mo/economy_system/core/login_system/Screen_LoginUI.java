package com.mo.economy_system.core.login_system;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mo.economy_system.EconomySystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Screen_LoginUI extends Screen {
    private static final int BG_COLOR = 0xB0202020;      // 半透明黑色背景
    private static final int LINE_COLOR = 0xFFDD00;       // 分隔线颜色
    private static final int PADDING = 8;                 // 内边距
    private static final int SPACING = 6;                // 元素间距

    // 性能优化：缓存RGB颜色值
    private static int CACHED_DYNAMIC_BORDER_COLOR = 0xFFDDAA55;
    private static long LAST_BORDER_COLOR_UPDATE = 0;
    private static final long BORDER_COLOR_UPDATE_INTERVAL = 100; // 100ms更新一次颜色

    private EditBox passwordField;
    private EditBox confirmPasswordField;
    private Component statusMessage = Component.literal("");
    private int messageColor = 0xFFFF5555;
    private boolean isSubmitting = false;  // 防止重复提交

    private final boolean requireRegistration;

    public Screen_LoginUI(boolean requireRegistration) {
        super(Component.literal("登录界面"));
        this.requireRegistration = requireRegistration;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int boxWidth = 400;
        int boxHeight = 200;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        int fieldWidth = 300;
        int fieldHeight = 20;
        int startY = boxY + 95;

        // 密码输入框
        this.passwordField = new EditBox(this.font, centerX - fieldWidth / 2, startY, fieldWidth, fieldHeight, Component.literal("密码"));
        this.passwordField.setHint(Component.literal(requireRegistration ? "请设置您的密码" : "请输入您的密码"));
        this.passwordField.setMaxLength(32);
        this.passwordField.setBordered(false);
        this.passwordField.setTextColor(0xFFFFFF);
        this.passwordField.setResponder(value -> {
            if (value.length() > 0 && value.endsWith("\n")) {
                // 回车键
                passwordField.setValue(value.substring(0, value.length() - 1));
                onSubmit();
            }
        });
        this.addRenderableWidget(this.passwordField);

        // 确认密码输入框（仅在注册阶段显示）
        this.confirmPasswordField = new EditBox(this.font, centerX - fieldWidth / 2, startY + fieldHeight + SPACING, fieldWidth, fieldHeight, Component.literal("确认密码"));
        this.confirmPasswordField.setHint(Component.literal("请再次确认您的密码"));
        this.confirmPasswordField.setMaxLength(32);
        this.confirmPasswordField.setBordered(false);
        this.confirmPasswordField.setTextColor(0xFFFFFF);
        this.confirmPasswordField.setResponder(value -> {
            if (value.length() > 0 && value.endsWith("\n")) {
                // 回车键
                confirmPasswordField.setValue(value.substring(0, value.length() - 1));
                onSubmit();
            }
        });
        this.confirmPasswordField.setVisible(requireRegistration);
        this.addRenderableWidget(this.confirmPasswordField);

        updatePromptMessage();
    }

    private void updatePromptMessage() {
        if (requireRegistration) {
            statusMessage = Component.literal("§e欢迎萌新鱼友来到梦鱼服！首次进入服务器请注册账号");
            messageColor = 0xFFFFFF00;
        } else {
            statusMessage = Component.literal("§e欢迎回来！请输入密码登录");
            messageColor = 0xFFFFFF00;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 半透明背景
        guiGraphics.fill(0, 0, this.width, this.height, 0xD0000000);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int boxWidth = 400;
        int boxHeight = requireRegistration ? 220 : 180;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        // 获取动态RGB边框颜色
        int dynamicBorderColor = getDynamicBorderColor();

        // 主面板背景发光效果
        int glowColor = 0x40000000 | (dynamicBorderColor & 0x00FFFFFF);
        guiGraphics.fill(RenderType.gui(), boxX - 2, boxY - 2, boxX + boxWidth + 2, boxY + boxHeight + 2, glowColor);

        // 主面板背景
        guiGraphics.fill(RenderType.gui(), boxX, boxY, boxX + boxWidth, boxY + boxHeight, BG_COLOR);

        // 边框（使用动态RGB颜色）
        guiGraphics.fill(RenderType.gui(), boxX, boxY, boxX + boxWidth, boxY + 1, dynamicBorderColor);
        guiGraphics.fill(RenderType.gui(), boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, dynamicBorderColor);
        guiGraphics.fill(RenderType.gui(), boxX, boxY, boxX + 1, boxY + boxHeight, dynamicBorderColor);
        guiGraphics.fill(RenderType.gui(), boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, dynamicBorderColor);

        PoseStack poseStack = guiGraphics.pose();

        // 第一行：左边玩家昵称（大字号，靠左，黄色）
        poseStack.pushPose();
        poseStack.scale(1.8f, 1.8f, 1.0f);
        String playerName = minecraft.player != null ? minecraft.player.getName().getString() : "Player";
        String nameText = "§e§l" + playerName;  // 黄色
        int nameX = (int)((boxX + PADDING) / 1.8f);
        int nameY = (int)((boxY + 25) / 1.8f);
        guiGraphics.drawString(minecraft.font, nameText, nameX, nameY, 0xFFFFFF, false);
        poseStack.popPose();

        // 第一行：右边"欢迎游玩梦鱼服"（靠右）
        String welcomeText = "§6欢迎游玩§d梦鱼服";
        int welcomeX = boxX + boxWidth - PADDING - minecraft.font.width(welcomeText);
        int welcomeY = boxY + 25;
        guiGraphics.drawString(minecraft.font, welcomeText, welcomeX, welcomeY, 0xFFAAAAAA);

        // 昵称下面的长线
        int lineY = boxY + 45;
        guiGraphics.fill(RenderType.gui(), boxX + PADDING, lineY, boxX + boxWidth - PADDING, lineY + 1, LINE_COLOR);

        // 提示消息
        guiGraphics.drawCenteredString(minecraft.font, statusMessage, centerX, boxY + 60, messageColor);

        // 昵称下划线（在输入框上方）
        int underlineY = boxY + 75;
        guiGraphics.fill(RenderType.gui(), boxX + PADDING + 20, underlineY, boxX + boxWidth - PADDING - 20, underlineY + 1, 0xB0DDDDDD);

        // 密码输入框背景（不带"密码:"标签）
        if (!confirmPasswordField.isVisible()) {
            renderInputBackground(guiGraphics, passwordField);
            // 输入框下方的提示文字
            guiGraphics.drawCenteredString(minecraft.font, "§7设置完密码后按下 [Enter] 确认", centerX, passwordField.getY() + 30, 0xFFAAAAAA);
        } else {
            renderInputBackground(guiGraphics, passwordField);
            renderInputBackground(guiGraphics, confirmPasswordField);
            // 输入框下方的提示文字（只有一行，在两个框下方居中）
            guiGraphics.drawCenteredString(minecraft.font, "§7设置完密码后按下 [Enter] 确认", centerX, confirmPasswordField.getY() + 30, 0xFFAAAAAA);
        }

        // 底部：DreamingFish.net（dreaming蓝色，fish紫色，.net金色）
        String domainText = "§b§lDreaming§d§lFish§6§l.net";
        poseStack.pushPose();
        poseStack.scale(1.2f, 1.2f, 1.0f);
        int domainX = (int)((centerX) / 1.2f - minecraft.font.width(domainText) / 2.0f);
        int domainY = (int)((boxY + boxHeight - 20) / 1.2f);
        guiGraphics.drawString(minecraft.font, domainText, domainX, domainY, 0xFFFFFF, false);
        poseStack.popPose();

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderInputBackground(GuiGraphics guiGraphics, EditBox editBox) {
        // 输入框背景
        guiGraphics.fill(RenderType.gui(), editBox.getX() - 2, editBox.getY() - 2,
                       editBox.getX() + editBox.getWidth() + 2, editBox.getY() + editBox.getHeight() + 2,
                       0xB0000000);
        // 输入框边框
        guiGraphics.fill(RenderType.gui(), editBox.getX() - 2, editBox.getY() - 2,
                       editBox.getX() + editBox.getWidth() + 2, editBox.getY() - 1,
                       editBox.isFocused() ? 0xFF4A90E2 : 0xFF404040);
        guiGraphics.fill(RenderType.gui(), editBox.getX() - 2, editBox.getY() + editBox.getHeight() + 1,
                       editBox.getX() + editBox.getWidth() + 2, editBox.getY() + editBox.getHeight() + 2,
                       editBox.isFocused() ? 0xFF4A90E2 : 0xFF404040);
        guiGraphics.fill(RenderType.gui(), editBox.getX() - 2, editBox.getY() - 2,
                       editBox.getX() - 1, editBox.getY() + editBox.getHeight() + 2,
                       editBox.isFocused() ? 0xFF4A90E2 : 0xFF404040);
        guiGraphics.fill(RenderType.gui(), editBox.getX() + editBox.getWidth() + 1, editBox.getY() - 2,
                       editBox.getX() + editBox.getWidth() + 2, editBox.getY() + editBox.getHeight() + 2,
                       editBox.isFocused() ? 0xFF4A90E2 : 0xFF404040);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC键
            // 不允许关闭，提示玩家必须登录
            return true;
        }
        if (keyCode == 257 || keyCode == 335) { // Enter键
            onSubmit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (passwordField.isFocused()) {
            return passwordField.charTyped(codePoint, modifiers);
        }
        if (confirmPasswordField.isFocused() && confirmPasswordField.isVisible()) {
            return confirmPasswordField.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        // 不允许关闭登录界面，玩家必须登录
    }

    private void onSubmit() {
        // 防止重复提交
        if (isSubmitting) {
            return;
        }

        String password = passwordField.getValue().trim();

        if (password.isEmpty()) {
            statusMessage = Component.literal("§c请输入密码！");
            messageColor = 0xFFFF5555;
            return;
        }

        if (password.length() < 4) {
            statusMessage = Component.literal("§c密码长度至少需要4个字符！");
            messageColor = 0xFFFF5555;
            return;
        }

        if (requireRegistration) {
            // 注册模式：检查两个密码框
            String confirmPassword = confirmPasswordField.getValue().trim();

            if (confirmPassword.isEmpty()) {
                statusMessage = Component.literal("§c请确认密码！");
                messageColor = 0xFFFF5555;
                return;
            }

            if (!password.equals(confirmPassword)) {
                statusMessage = Component.literal("§c两次输入的密码不一致！");
                messageColor = 0xFFFF5555;
                return;
            }

            // 密码一致，执行注册
            isSubmitting = true;
            statusMessage = Component.literal("§a正在注册...");
            messageColor = 0xFF55FF55;
            ClientLoginHandler.sendRegisterRequest(password);
        } else {
            // 已注册，直接登录
            isSubmitting = true;
            statusMessage = Component.literal("§a正在登录...");
            messageColor = 0xFF55FF55;
            ClientLoginHandler.sendLoginRequest(password);
        }
    }

    public void setStatusMessage(String message, boolean isError) {
        statusMessage = Component.literal(message);
        messageColor = isError ? 0xFFFF5555 : 0xFF55FF55;
        if (isError) {
            isSubmitting = false;  // 登录/注册失败，允许重新提交
        }
    }

    public void switchToLoginMode() {
        // 不再需要，因为注册后会自动登录
    }

    /**
     * 获取动态RGB变色的边框颜色（基于系统时间循环，使用缓存优化性能）
     */
    private static int getDynamicBorderColor() {
        long currentTime = System.currentTimeMillis();

        // 每100ms更新一次颜色，避免每帧计算
        if (currentTime - LAST_BORDER_COLOR_UPDATE > BORDER_COLOR_UPDATE_INTERVAL) {
            int red = (int) (Math.sin(currentTime * 0.001) * 100 + 155);
            int green = (int) (Math.sin(currentTime * 0.001 + 2) * 100 + 155);
            int blue = (int) (Math.sin(currentTime * 0.001 + 4) * 100 + 155);
            CACHED_DYNAMIC_BORDER_COLOR = 0xFF000000 | (red << 16) | (green << 8) | blue;
            LAST_BORDER_COLOR_UPDATE = currentTime;
        }

        return CACHED_DYNAMIC_BORDER_COLOR;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

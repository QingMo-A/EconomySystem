package com.mo.economy_system.core.login_system;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Screen_LoginUI extends Screen {
    private static final int TERMINAL_BACKGROUND = 0xD9050A10;
    private static final int TERMINAL_SHELL = 0xEE0B1118;
    private static final int TERMINAL_HEADER = 0xF0131B24;
    private static final int TERMINAL_CONTENT = 0xF01B2530;
    private static final int TERMINAL_CARD = 0xFF24313E;
    private static final int TERMINAL_CARD_HOVER = 0xFF2C3B4A;
    private static final int TERMINAL_BORDER = 0xFF7AA8C7;
    private static final int TERMINAL_BORDER_DARK = 0xFF344555;
    private static final int TERMINAL_TEXT = 0xFFE8EDF2;
    private static final int TERMINAL_MUTED_TEXT = 0xFFA7B2BE;
    private static final int TERMINAL_GREEN = 0xFF50D890;
    private static final int TERMINAL_RED = 0xFFFF6677;
    private static final int TERMINAL_GOLD = 0xFFFFC857;
    private static final int SPACING = 8;

    private EditBox passwordField;
    private EditBox confirmPasswordField;
    private Component statusMessage = Component.literal("");
    private int messageColor = TERMINAL_RED;
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

        int boxWidth = Math.min(680, Math.max(360, (int) (this.width * 0.78f)));
        int boxHeight = requireRegistration ? 330 : 290;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        int fieldWidth = Math.min(430, boxWidth - 120);
        int fieldHeight = 18;
        int fieldX = centerX - fieldWidth / 2;
        int startY = boxY + 164;

        // 密码输入框
        this.passwordField = new EditBox(this.font, fieldX + 8, startY + 7, fieldWidth - 16, fieldHeight, Component.literal("密码"));
        this.passwordField.setHint(Component.literal(requireRegistration ? "请设置您的密码" : "请输入您的密码"));
        this.passwordField.setMaxLength(32);
        this.passwordField.setBordered(false);
        this.passwordField.setTextColor(TERMINAL_TEXT);
        this.passwordField.setTextColorUneditable(TERMINAL_MUTED_TEXT);
        this.passwordField.setResponder(value -> {
            if (value.length() > 0 && value.endsWith("\n")) {
                // 回车键
                passwordField.setValue(value.substring(0, value.length() - 1));
                onSubmit();
            }
        });
        this.addRenderableWidget(this.passwordField);

        // 确认密码输入框（仅在注册阶段显示）
        this.confirmPasswordField = new EditBox(this.font, fieldX + 8, startY + 55, fieldWidth - 16, fieldHeight, Component.literal("确认密码"));
        this.confirmPasswordField.setHint(Component.literal("请再次确认您的密码"));
        this.confirmPasswordField.setMaxLength(32);
        this.confirmPasswordField.setBordered(false);
        this.confirmPasswordField.setTextColor(TERMINAL_TEXT);
        this.confirmPasswordField.setTextColorUneditable(TERMINAL_MUTED_TEXT);
        this.confirmPasswordField.setResponder(value -> {
            if (value.length() > 0 && value.endsWith("\n")) {
                // 回车键
                confirmPasswordField.setValue(value.substring(0, value.length() - 1));
                onSubmit();
            }
        });
        this.confirmPasswordField.setVisible(requireRegistration);
        this.addRenderableWidget(this.confirmPasswordField);
        this.setInitialFocus(this.passwordField);

        updatePromptMessage();
    }

    private void updatePromptMessage() {
        if (requireRegistration) {
            statusMessage = Component.literal("首次接入梦屿网络，请设置终端访问密码");
            messageColor = TERMINAL_GOLD;
        } else {
            statusMessage = Component.literal("身份缓存已找到，请输入终端访问密码");
            messageColor = TERMINAL_GOLD;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int boxWidth = Math.min(680, Math.max(360, (int) (this.width * 0.78f)));
        int boxHeight = requireRegistration ? 330 : 290;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;
        int headerHeight = 34;

        guiGraphics.fill(RenderType.gui(), 0, 0, this.width, this.height, TERMINAL_BACKGROUND);
        drawSoftRect(guiGraphics, boxX + 5, boxY + 6, boxWidth, boxHeight, 3, 0x66000000, 0x00000000);
        drawSoftRect(guiGraphics, boxX, boxY, boxWidth, boxHeight, 3, TERMINAL_SHELL, 0x66526372);
        drawSoftRect(guiGraphics, boxX + 6, boxY + 6, boxWidth - 12, headerHeight, 2, TERMINAL_HEADER, 0x224A5A68);
        drawSoftRect(guiGraphics, boxX + 6, boxY + headerHeight, boxWidth - 12, boxHeight - headerHeight - 6, 2, TERMINAL_CONTENT, 0x22384755);

        drawBrandTitle(guiGraphics, boxX + 18, boxY + 16);

        String modeText = requireRegistration ? "REGISTER" : "LOGIN";
        int modeWidth = minecraft.font.width(modeText) + 18;
        drawSoftRect(guiGraphics, boxX + boxWidth - modeWidth - 18, boxY + 13, modeWidth, 16, 2,
                requireRegistration ? 0x3339A6FF : 0x3350D890, requireRegistration ? 0xFF4FC3F7 : TERMINAL_GREEN);
        guiGraphics.drawString(minecraft.font, modeText, boxX + boxWidth - modeWidth - 9, boxY + 17,
                requireRegistration ? 0xFF4FC3F7 : TERMINAL_GREEN, false);

        drawSoftRect(guiGraphics, boxX + 26, boxY + 66, boxWidth - 52, 62, 2, TERMINAL_CARD, TERMINAL_BORDER_DARK);
        guiGraphics.fill(RenderType.gui(), boxX + 26, boxY + 66, boxX + 31, boxY + 128, TERMINAL_GOLD);
        guiGraphics.drawString(minecraft.font, "PLAYER", boxX + 44, boxY + 80, TERMINAL_MUTED_TEXT, false);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(1.35f, 1.35f, 1.0f);
        String playerName = minecraft.player != null ? minecraft.player.getName().getString() : "Player";
        guiGraphics.drawString(minecraft.font, playerName, (int) ((boxX + 44) / 1.35f), (int) ((boxY + 100) / 1.35f), TERMINAL_TEXT, false);
        guiGraphics.pose().popPose();

        int fieldWidth = Math.min(430, boxWidth - 120);
        int fieldX = centerX - fieldWidth / 2;
        int fieldY = boxY + 164;

        drawInputLabel(guiGraphics, fieldX, fieldY, "PASSWORD");
        if (!confirmPasswordField.isVisible()) {
            renderInputBackground(guiGraphics, fieldX, fieldY, fieldWidth, 32, passwordField.isFocused());
            guiGraphics.drawCenteredString(minecraft.font, statusMessage, centerX, fieldY + 48, messageColor);
            guiGraphics.drawCenteredString(minecraft.font, "按下 Enter 确认身份", centerX, fieldY + 68, TERMINAL_MUTED_TEXT);
        } else {
            renderInputBackground(guiGraphics, fieldX, fieldY, fieldWidth, 32, passwordField.isFocused());
            int confirmY = fieldY + 48;
            drawInputLabel(guiGraphics, fieldX, confirmY, "CONFIRM");
            renderInputBackground(guiGraphics, fieldX, confirmY, fieldWidth, 32, confirmPasswordField.isFocused());
            guiGraphics.drawCenteredString(minecraft.font, statusMessage, centerX, confirmY + 48, messageColor);
            guiGraphics.drawCenteredString(minecraft.font, "设置完成后按下 Enter 写入身份凭据", centerX, confirmY + 68, TERMINAL_MUTED_TEXT);
        }

        drawTerminalFooter(guiGraphics, centerX, boxY + boxHeight - 23);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderInputBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, boolean focused) {
        int borderColor = focused ? TERMINAL_BORDER : TERMINAL_BORDER_DARK;
        int bgColor = focused ? TERMINAL_CARD_HOVER : TERMINAL_CARD;
        drawSoftRect(guiGraphics, x, y, width, height, 2, bgColor, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y, x + 4, y + height, borderColor);
    }

    private void drawInputLabel(GuiGraphics guiGraphics, int x, int y, String label) {
        guiGraphics.drawString(minecraft.font, label, x + 6, y - 12, TERMINAL_MUTED_TEXT, false);
    }

    private void drawBrandTitle(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.drawString(minecraft.font, "Dreaming", x, y, 0xFFB58BFF, false);
        int fishX = x + minecraft.font.width("Dreaming");
        guiGraphics.drawString(minecraft.font, "Fish", fishX, y, 0xFF4FC3F7, false);
        guiGraphics.drawString(minecraft.font, " Terminal", fishX + minecraft.font.width("Fish"), y, TERMINAL_GOLD, false);
    }

    private void drawTerminalFooter(GuiGraphics guiGraphics, int centerX, int y) {
        String left = "Dreaming";
        String mid = "Fish";
        String right = ".net";
        int totalWidth = minecraft.font.width(left + mid + right);
        int x = centerX - totalWidth / 2;
        guiGraphics.drawString(minecraft.font, left, x, y, 0xFFB58BFF, false);
        x += minecraft.font.width(left);
        guiGraphics.drawString(minecraft.font, mid, x, y, 0xFF4FC3F7, false);
        x += minecraft.font.width(mid);
        guiGraphics.drawString(minecraft.font, right, x, y, TERMINAL_GOLD, false);
    }

    private void drawSoftRect(GuiGraphics guiGraphics, int x, int y, int width, int height, int radius, int fillColor, int borderColor) {
        guiGraphics.fill(RenderType.gui(), x + radius, y, x + width - radius, y + height, fillColor);
        guiGraphics.fill(RenderType.gui(), x, y + radius, x + width, y + height - radius, fillColor);
        guiGraphics.fill(RenderType.gui(), x + 1, y + 1, x + width - 1, y + 2, borderColor);
        guiGraphics.fill(RenderType.gui(), x + 1, y + height - 2, x + width - 1, y + height - 1, borderColor);
        guiGraphics.fill(RenderType.gui(), x + 1, y + 1, x + 2, y + height - 1, borderColor);
        guiGraphics.fill(RenderType.gui(), x + width - 2, y + 1, x + width - 1, y + height - 1, borderColor);
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
            messageColor = TERMINAL_RED;
            return;
        }

        if (password.length() < 4) {
            statusMessage = Component.literal("§c密码长度至少需要4个字符！");
            messageColor = TERMINAL_RED;
            return;
        }

        if (requireRegistration) {
            // 注册模式：检查两个密码框
            String confirmPassword = confirmPasswordField.getValue().trim();

            if (confirmPassword.isEmpty()) {
                statusMessage = Component.literal("§c请确认密码！");
                messageColor = TERMINAL_RED;
                return;
            }

            if (!password.equals(confirmPassword)) {
                statusMessage = Component.literal("§c两次输入的密码不一致！");
                messageColor = TERMINAL_RED;
                return;
            }

            // 密码一致，执行注册
            isSubmitting = true;
            statusMessage = Component.literal("§a正在注册...");
            messageColor = TERMINAL_GREEN;
            ClientLoginHandler.sendRegisterRequest(password);
        } else {
            // 已注册，直接登录
            isSubmitting = true;
            statusMessage = Component.literal("§a正在登录...");
            messageColor = TERMINAL_GREEN;
            ClientLoginHandler.sendLoginRequest(password);
        }
    }

    public void setStatusMessage(String message, boolean isError) {
        statusMessage = Component.literal(message);
        messageColor = isError ? TERMINAL_RED : TERMINAL_GREEN;
        if (isError) {
            isSubmitting = false;  // 登录/注册失败，允许重新提交
        }
    }

    public void switchToLoginMode() {
        // 不再需要，因为注册后会自动登录
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

package com.mo.economy_system.core.playerattributes_system.death;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.playerattribute_system.death_system.Packet_RevivalRequest;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/**
 * 复活护符 GUI - 金色神圣风格
 * 输入被封禁玩家名称进行复活
 *
 * 使用虚拟坐标系统（参考 ServerScreenUI 和 DeathScreenMixin）
 * 虚拟基准尺寸：640×400（对应 2560×1600 四缩放）
 */
public class Screen_RevivalCharm extends Screen {

    // ==================== 虚拟基准尺寸 ====================
    private static final int BASE_WIDTH = 640;
    private static final int BASE_HEIGHT = 400;

    // ==================== 面板尺寸（虚拟坐标）====================
    private static final int PANEL_WIDTH = 460;
    private static final int PANEL_HEIGHT = 210; // 整体面板高度保持不变

    // ==================== 边距（虚拟坐标）====================
    private static final int PADDING = 12;
    private static final int MARGIN_LARGE = 24;

    // ==================== 元素尺寸（虚拟坐标）====================
    // 布局规则：按钮宽度×2 + 间距 = 输入框宽度，且 间距 = 按钮底部到面板底部的距离
    private static final int INPUT_WIDTH = 320;
    private static final int INPUT_HEIGHT = 26;
    private static final int BUTTON_SPACING = 8;
    private static final int BUTTON_WIDTH = (INPUT_WIDTH - BUTTON_SPACING) / 2;  // 156
    private static final int BUTTON_HEIGHT = 22;

    private static final int ICON_SIZE = 28;

    // ==================== Y 坐标位置（相对于面板）- 仅修改输入框Y坐标，轻微上挪 ====================
    private static final int Y_ICON = 16;
    private static final int Y_TITLE = 18;
    private static final int Y_SEPARATOR = 48;
    private static final int Y_HINT_LINE_1 = 58;
    private static final int Y_HINT_LINE_2 = 72;
    private static final int Y_WARNING = 118;
    private static final int Y_INPUT = 134;          // 核心修改：136→134，输入框轻微上挪2个虚拟单位
    private static final int Y_BUTTON = 168;

    // ==================== 颜色定义 ====================
    private static final int BG_OUTER = 0xDD0A0A00;
    private static final int BG_INNER = 0xEE1A1A05;
    private static final int BORDER_DARK = 0xFF3D3D00;
    private static final int BORDER_GLOW = 0xFFFFCC00;
    private static final int ACCENT_GOLD = 0xFFD4AF37;

    // ==================== 虚拟坐标系统变量 ====================
    private float uiScale;
    private int virtualWidth;
    private int virtualHeight;
    private int panelX;
    private int panelY;
    private int centerX;
    private int centerY;

    private Button confirmButton;
    private Button cancelButton;
    private String playerName = "";

    private static final Minecraft mc = Minecraft.getInstance();

    public Screen_RevivalCharm() {
        super(Component.literal("复活护符"));
    }

    @Override
    protected void init() {
        calculateVirtualSize();
        createButtons();
    }

    private void calculateVirtualSize() {
        float scaleX = (float) this.width / BASE_WIDTH;
        float scaleY = (float) this.height / BASE_HEIGHT;
        uiScale = Math.min(scaleX, scaleY);

        virtualWidth = (int) (this.width / uiScale);
        virtualHeight = (int) (this.height / uiScale);

        centerX = virtualWidth / 2;
        centerY = virtualHeight / 2;

        panelX = centerX - PANEL_WIDTH / 2;
        panelY = centerY - PANEL_HEIGHT / 2;
    }

    private void createButtons() {
        int btnY = v2s(panelY + Y_BUTTON);
        int btnW = s2s(BUTTON_WIDTH);
        int btnH = s2s(BUTTON_HEIGHT);

        int panelCenterScreen = v2s(centerX);
        int spacingScreen = s2s(BUTTON_SPACING);

        int btnConfirmX = panelCenterScreen - btnW - spacingScreen / 2;
        int btnCancelX = panelCenterScreen + spacingScreen / 2;

        confirmButton = new CustomButton(
                btnConfirmX, btnY, btnW, btnH,
                Component.literal("§e§l复活"),
                true,
                btn -> confirmRevival()
        );
        this.addRenderableWidget(confirmButton);

        cancelButton = new CustomButton(
                btnCancelX, btnY, btnW, btnH,
                Component.literal("§7取消"),
                false,
                btn -> onClose()
        );
        this.addRenderableWidget(cancelButton);
    }

    private int v2s(int v) {
        return (int) (v * uiScale);
    }

    private int s2s(int v) {
        return (int) (v * uiScale);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        calculateVirtualSize();
        updateButtonPositions();

        // 背景
        guiGraphics.fillGradient(0, 0, this.width, this.height, BG_OUTER, BG_INNER);

        // 应用虚拟坐标缩放
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(uiScale, uiScale, 1.0f);

        renderPanel(guiGraphics);
        renderContent(guiGraphics);

        guiGraphics.pose().popPose();

        // 渲染按钮
        if (confirmButton != null) {
            confirmButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        if (cancelButton != null) {
            cancelButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderPanel(GuiGraphics guiGraphics) {
        int outerBorder = 4;
        int innerBorder = 2;

        renderRoundedBox(guiGraphics,
                panelX - outerBorder, panelY - outerBorder,
                panelX + PANEL_WIDTH + outerBorder, panelY + PANEL_HEIGHT + outerBorder,
                BORDER_DARK);

        renderRoundedBox(guiGraphics,
                panelX - innerBorder, panelY - innerBorder,
                panelX + PANEL_WIDTH + innerBorder, panelY + PANEL_HEIGHT + innerBorder,
                BORDER_GLOW);

        guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, BG_INNER);
    }

    private void renderContent(GuiGraphics guiGraphics) {
        PoseStack poseStack = guiGraphics.pose();

        // ========== 不死图腾图标 ==========
        poseStack.pushPose();
        poseStack.translate(panelX + MARGIN_LARGE - 2, panelY + Y_ICON, 0);
        poseStack.scale(ICON_SIZE / 16.0f, ICON_SIZE / 16.0f, 1.0f);
        guiGraphics.renderItem(new net.minecraft.world.item.ItemStack(Items.TOTEM_OF_UNDYING), 0, 0);
        poseStack.popPose();

        // ========== 标题：复活护符 ==========
        poseStack.pushPose();
        float titleScale = 2.0f;
        poseStack.scale(titleScale, titleScale, 1.0f);
        String titleText = "§e§l复活护符";
        int titleX = (int) ((centerX + 6) / titleScale - mc.font.width(titleText) / 2.0f);
        int titleY = (int) ((panelY + Y_TITLE) / titleScale);
        guiGraphics.drawString(mc.font, titleText, titleX, titleY, 0xFFFFFFFF, false);
        poseStack.popPose();

        // ========== 右上角品牌名 ==========
        String brandText = "§b§lDreaming§d§lFish";
        int brandX = panelX + PANEL_WIDTH - PADDING - mc.font.width(brandText);
        int brandY = panelY + PADDING;
        guiGraphics.drawString(mc.font, brandText, brandX, brandY, 0xFFFFFFFF, false);

        // ========== 分隔线 ==========
        int sepY = panelY + Y_SEPARATOR;
        guiGraphics.fill(panelX + PADDING, sepY, panelX + PANEL_WIDTH - PADDING, sepY + 2, ACCENT_GOLD);
        guiGraphics.fill(panelX + PADDING, sepY + 4, panelX + PANEL_WIDTH - PADDING, sepY + 5, 0xAA666600);

        // ========== 提示文字（左对齐）==========
        String hintLine1 = "§7输入被封禁玩家的名称";
        String hintLine2 = "§7用复活护符拯救他们的灵魂";

        poseStack.pushPose();
        float hintScale = 1.1f;
        poseStack.scale(hintScale, hintScale, 1.0f);
        int hintX = (int) ((panelX + PADDING + 4) / hintScale);
        int hintY1 = (int) ((panelY + Y_HINT_LINE_1) / hintScale);
        int hintY2 = (int) ((panelY + Y_HINT_LINE_2) / hintScale);
        guiGraphics.drawString(mc.font, hintLine1, hintX, hintY1, 0xFFFFFFFF, false);
        guiGraphics.drawString(mc.font, hintLine2, hintX, hintY2, 0xFFFFFFFF, false);
        poseStack.popPose();

        // ========== 警告文字（红色）==========
        poseStack.pushPose();
        float warnScale = 0.95f;
        poseStack.scale(warnScale, warnScale, 1.0f);
        String warnText = "§c成功复活该玩家后，您的复活点数会扣除一半，被复活者感染情况与您相同";
        int warnX = (int) ((centerX) / warnScale - mc.font.width(warnText) / 2.0f);
        int warnY = (int) ((panelY + Y_WARNING) / warnScale);
        guiGraphics.drawString(mc.font, warnText, warnX, warnY, 0xFFFFFFFF, false);
        poseStack.popPose();

        // ========== 输入框 ==========
        int inputX = centerX - INPUT_WIDTH / 2;
        int inputY = panelY + Y_INPUT;

        // 输入框外发光效果
        guiGraphics.fill(inputX - 3, inputY - 3, inputX + INPUT_WIDTH + 3, inputY + INPUT_HEIGHT + 3, 0x40FFCC00);
        // 输入框边框
        renderRoundedBox(guiGraphics,
                inputX - 2, inputY - 2,
                inputX + INPUT_WIDTH + 2, inputY + INPUT_HEIGHT + 2,
                BORDER_GLOW);
        // 输入框背景
        guiGraphics.fill(inputX, inputY, inputX + INPUT_WIDTH, inputY + INPUT_HEIGHT, 0xDD000000);

        // 输入文字
        String displayText = playerName.isEmpty() ? "§7输入玩家名称..." : "§e" + playerName;
        if (playerName.isEmpty() && (System.currentTimeMillis() / 500) % 2 == 0) {
            displayText = "§8输入玩家名称...";
        }
        poseStack.pushPose();
        float inputScale = 1.05f;
        poseStack.scale(inputScale, inputScale, 1.0f);
        int inputTextY = (int) ((inputY + INPUT_HEIGHT / 2 - 4) / inputScale);
        guiGraphics.drawCenteredString(mc.font, displayText, (int) (centerX / inputScale), inputTextY, 0xFFFFFF);
        poseStack.popPose();

        // 光标
        if (!playerName.isEmpty() && (System.currentTimeMillis() / 500) % 2 == 0) {
            int textWidth = mc.font.width(playerName);
            int cursorX = centerX + textWidth / 2 + 3;
            guiGraphics.fill(cursorX, inputY + 5, cursorX + 2, inputY + INPUT_HEIGHT - 5, 0xFFFFD700);
        }

        // ========== 装饰性角落（输入框两侧）==========
        int cornerSize = 6;
        int cornerY = inputY + INPUT_HEIGHT / 2 - cornerSize / 2;
        // 左侧金色装饰
        guiGraphics.fill(inputX - 8, cornerY, inputX - 6, cornerY + cornerSize, BORDER_GLOW);
        // 右侧金色装饰
        guiGraphics.fill(inputX + INPUT_WIDTH + 6, cornerY, inputX + INPUT_WIDTH + 8, cornerY + cornerSize, BORDER_GLOW);
    }

    private void updateButtonPositions() {
        int btnY = v2s(panelY + Y_BUTTON);
        int btnW = s2s(BUTTON_WIDTH);
        int btnH = s2s(BUTTON_HEIGHT);

        int panelCenterScreen = v2s(centerX);
        int spacingScreen = s2s(BUTTON_SPACING);

        int btnConfirmX = panelCenterScreen - btnW - spacingScreen / 2;
        int btnCancelX = panelCenterScreen + spacingScreen / 2;

        if (confirmButton != null) {
            confirmButton.setX(btnConfirmX);
            confirmButton.setY(btnY);
            confirmButton.setWidth(btnW);
            confirmButton.setHeight(btnH);
        }
        if (cancelButton != null) {
            cancelButton.setX(btnCancelX);
            cancelButton.setY(btnY);
            cancelButton.setWidth(btnW);
            cancelButton.setHeight(btnH);
        }
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (playerName.length() < 16) {
            playerName += codePoint;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            onClose();
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            confirmRevival();
            return true;
        }
        if (keyCode == 261) {
            playerName = "";
            return true;
        }
        if (keyCode == 259) {
            if (!playerName.isEmpty()) {
                playerName = playerName.substring(0, playerName.length() - 1);
            }
            return true;
        }
        return false;
    }

    private void confirmRevival() {
        String name = playerName.trim();
        if (!name.isEmpty()) {
            EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_RevivalRequest(name));
            this.minecraft.setScreen(null);
        } else {
            this.minecraft.player.sendSystemMessage(Component.literal("§c请输入玩家名称！"));
        }
    }

    private void renderRoundedBox(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        guiGraphics.fill(x1 + 1, y1, x2 - 1, y2, color);
        guiGraphics.fill(x1, y1 + 1, x2, y2 - 1, color);
    }

    private static class CustomButton extends Button {
        private final boolean isPrimary;

        public CustomButton(int x, int y, int width, int height, Component message,
                            boolean isPrimary, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.isPrimary = isPrimary;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHovered();

            int topColor, bottomColor, borderColor, glowColor;
            if (isPrimary) {
                // 金色主题（复活按钮）
                if (hovered) {
                    topColor = 0xFFDDAA00;
                    bottomColor = 0xCC886600;
                    borderColor = 0xFFFFEE00;
                    glowColor = 0x30FFCC00;
                } else {
                    topColor = 0xFFCC9900;
                    bottomColor = 0xCC774400;
                    borderColor = 0xFFCC8800;
                    glowColor = 0x20CC8800;
                }
            } else {
                // 灰色主题（取消按钮）
                if (hovered) {
                    topColor = 0xFF777777;
                    bottomColor = 0xCC444444;
                    borderColor = 0xFF999999;
                    glowColor = 0x20555555;
                } else {
                    topColor = 0xFF666666;
                    bottomColor = 0xCC333333;
                    borderColor = 0xCC666666;
                    glowColor = 0x10333333;
                }
            }

            int x = getX(), y = getY(), w = width, h = height;

            // 外发光（仅主按钮悬停时）
            if (isPrimary && hovered) {
                guiGraphics.fill(x - 2, y - 2, x + w + 2, y + h + 2, glowColor);
            }

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

            // 主按钮高光效果
            if (isPrimary) {
                guiGraphics.fill(x + 4, y + 2, x + w - 4, y + 3, 0x40FFFFFF);
            }

            // 文字
            String text = getMessage().getString();
            int textX = x + w / 2 - Minecraft.getInstance().font.width(text) / 2;
            int textY = y + (h - 8) / 2;
            guiGraphics.drawString(mc.font, text, textX, textY, 0xFFFFFF, false);
        }
    }
}
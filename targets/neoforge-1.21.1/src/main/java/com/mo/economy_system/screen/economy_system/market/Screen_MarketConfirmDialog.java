package com.mo.economy_system.screen.economy_system.market;

import com.mo.economy_system.core.economy_system.market.DemandOrder;
import com.mo.economy_system.core.economy_system.market.SalesOrder;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.common.network.RemoveDemandOrderMessage;
import com.mo.economy_system.common.network.PurchaseSalesOrderMessage;
import com.mo.economy_system.common.network.RemoveSalesOrderMessage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

/**
 * 市场操作确认弹窗 - 绿色神圣风格（参考重生锦鲤界面）
 */
public class Screen_MarketConfirmDialog extends Screen {

    // ==================== 虚拟基准尺寸 ====================
    private static final int BASE_WIDTH = 640;
    private static final int BASE_HEIGHT = 400;

    // ==================== 面板尺寸 ====================
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 200;

    // ==================== 边距 ====================
    private static final int PADDING = 12;
    private static final int MARGIN_LARGE = 20;

    // ==================== 按钮尺寸 ====================
    private static final int BUTTON_WIDTH = 140;
    private static final int BUTTON_HEIGHT = 24;
    private static final int BUTTON_SPACING = 12;

    private static final int ICON_SIZE = 32;

    // ==================== Y 坐标位置 ====================
    private static final int Y_ICON = 16;
    private static final int Y_TITLE = 20;
    private static final int Y_SEPARATOR = 55;
    private static final int Y_INFO_START = 68;
    private static final int Y_WARNING = 138;
    private static final int Y_BUTTON = 160;

    // ==================== 颜色定义 =====================
    // 背景颜色（下架用红色背景）
    private static final int BG_INNER_NORMAL = 0xEE0A2A05;
    private static final int BG_INNER_REMOVE = 0xEE2A0505;

    // 绿色主题（购买）
    private static final int BUY_BORDER_DARK = 0xFF1A3D00;
    private static final int BUY_BORDER_GLOW = 0xFF4CAF50;
    private static final int BUY_ACCENT = 0xFF66BB6A;

    // 红色主题（下架）
    private static final int REMOVE_BORDER_DARK = 0xFF3D0000;
    private static final int REMOVE_BORDER_GLOW = 0xFFFF5252;
    private static final int REMOVE_ACCENT = 0xFFFF6B6B;

    // 黄色主题（取消求单）
    private static final int CANCEL_BORDER_DARK = 0xFF3D3A00;
    private static final int CANCEL_BORDER_GLOW = 0xFFFFB300;
    private static final int CANCEL_ACCENT = 0xFFFFC107;

    // ==================== 虚拟坐标系统变量 ====================
    private float uiScale;
    private int virtualWidth;
    private int virtualHeight;
    private int panelX;
    private int panelY;
    private int centerX;
    private int centerY;

    private final ConfirmType confirmType;
    private final SalesOrder salesOrder;
    private final DemandOrder demandOrder;
    private final Screen_Market parentScreen;

    private Button confirmButton;
    private Button cancelButton;

    private static final Minecraft mc = Minecraft.getInstance();

    public enum ConfirmType {
        BUY_SALES,      // 购买卖单
        REMOVE_SALES,   // 下架卖单
        REMOVE_DEMAND   // 取消求单
    }

    public Screen_MarketConfirmDialog(ConfirmType confirmType, SalesOrder salesOrder, Screen_Market parentScreen) {
        super(Component.literal("确认操作"));
        this.confirmType = confirmType;
        this.salesOrder = salesOrder;
        this.demandOrder = null;
        this.parentScreen = parentScreen;
    }

    public Screen_MarketConfirmDialog(ConfirmType confirmType, DemandOrder demandOrder, Screen_Market parentScreen) {
        super(Component.literal("确认操作"));
        this.confirmType = confirmType;
        this.demandOrder = demandOrder;
        this.salesOrder = null;
        this.parentScreen = parentScreen;
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

    private int v2s(int v) {
        return (int) (v * uiScale);
    }

    private int s2s(int v) {
        return (int) (v * uiScale);
    }

    private void createButtons() {
        int btnY = v2s(panelY + Y_BUTTON);
        int btnW = s2s(BUTTON_WIDTH);
        int btnH = s2s(BUTTON_HEIGHT);

        int panelCenterScreen = v2s(centerX);
        int spacingScreen = s2s(BUTTON_SPACING);

        int btnConfirmX = panelCenterScreen - btnW - spacingScreen / 2;
        int btnCancelX = panelCenterScreen + spacingScreen / 2;

        // 根据类型选择确认按钮的颜色主题
        ButtonColorTheme confirmTheme;
        String confirmText;
        switch (confirmType) {
            case BUY_SALES -> {
                confirmTheme = ButtonColorTheme.GREEN;
                confirmText = "§a§l确认";
                break;
            }
            case REMOVE_SALES -> {
                confirmTheme = ButtonColorTheme.RED;
                confirmText = "§c§l确认";
                break;
            }
            default -> {
                confirmTheme = ButtonColorTheme.YELLOW;
                confirmText = "§e§l确认";
            }
        }

        confirmButton = new CustomButton(
                btnConfirmX, btnY, btnW, btnH,
                Component.literal(confirmText),
                confirmTheme,
                btn -> onConfirm()
        );
        this.addRenderableWidget(confirmButton);

        cancelButton = new CustomButton(
                btnCancelX, btnY, btnW, btnH,
                Component.literal("§7取消"),
                ButtonColorTheme.GRAY,
                btn -> onCancel()
        );
        this.addRenderableWidget(cancelButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        calculateVirtualSize();
        updateButtonPositions();

        // 半透明背景
        guiGraphics.fill(0, 0, this.width, this.height, 0xB0000000);

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

        // 根据类型选择颜色
        int borderDark, borderGlow, bgColor;
        switch (confirmType) {
            case BUY_SALES -> {
                borderDark = BUY_BORDER_DARK;
                borderGlow = BUY_BORDER_GLOW;
                bgColor = BG_INNER_NORMAL;
                break;
            }
            case REMOVE_SALES -> {
                borderDark = REMOVE_BORDER_DARK;
                borderGlow = REMOVE_BORDER_GLOW;
                bgColor = BG_INNER_REMOVE;
                break;
            }
            default -> {
                borderDark = CANCEL_BORDER_DARK;
                borderGlow = CANCEL_BORDER_GLOW;
                bgColor = BG_INNER_NORMAL;
            }
        }

        // 外层边框
        renderRoundedBox(guiGraphics,
                panelX - outerBorder, panelY - outerBorder,
                panelX + PANEL_WIDTH + outerBorder, panelY + PANEL_HEIGHT + outerBorder,
                borderDark);

        // 内层边框（发光）
        renderRoundedBox(guiGraphics,
                panelX - innerBorder, panelY - innerBorder,
                panelX + PANEL_WIDTH + innerBorder, panelY + PANEL_HEIGHT + innerBorder,
                borderGlow);

        // 面板背景
        guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, bgColor);
    }

    private void renderContent(GuiGraphics guiGraphics) {
        PoseStack poseStack = guiGraphics.pose();

        // ========== 物品图标 ==========
        poseStack.pushPose();
        poseStack.translate(panelX + MARGIN_LARGE, panelY + Y_ICON, 0);
        poseStack.scale(ICON_SIZE / 16.0f, ICON_SIZE / 16.0f, 1.0f);
        guiGraphics.renderItem(getItemStack(), 0, 0);
        poseStack.popPose();

        // ========== 标题 ==========
        poseStack.pushPose();
        float titleScale = 1.8f;
        poseStack.scale(titleScale, titleScale, 1.0f);
        String titleText = getConfirmTitle();
        int titleX = (int) ((centerX + 10) / titleScale - mc.font.width(titleText) / 2.0f);
        int titleY = (int) ((panelY + Y_TITLE) / titleScale);
        guiGraphics.drawString(mc.font, titleText, titleX, titleY, 0xFFFFFFFF, false);
        poseStack.popPose();

        // ========== 右上角品牌名 ==========
        String brandText = "§b§lEconomy§d§lSystem";
        int brandX = panelX + PANEL_WIDTH - PADDING - mc.font.width(brandText);
        int brandY = panelY + PADDING;
        guiGraphics.drawString(mc.font, brandText, brandX, brandY, 0xFFFFFFFF, false);

        // ========== 分隔线 ==========
        int sepY = panelY + Y_SEPARATOR;
        int accentColor, shadowColor;
        switch (confirmType) {
            case BUY_SALES -> {
                accentColor = BUY_ACCENT;
                shadowColor = 0xAA336633;
                break;
            }
            case REMOVE_SALES -> {
                accentColor = REMOVE_ACCENT;
                shadowColor = 0xAA663333;
                break;
            }
            default -> {
                accentColor = CANCEL_ACCENT;
                shadowColor = 0xAA664400;
            }
        }
        guiGraphics.fill(panelX + PADDING, sepY, panelX + PANEL_WIDTH - PADDING, sepY + 2, accentColor);
        guiGraphics.fill(panelX + PADDING, sepY + 4, panelX + PANEL_WIDTH - PADDING, sepY + 5, shadowColor);

        // ========== 信息文字 ==========
        String[] infoLines = getInfoLines();
        int infoY = panelY + Y_INFO_START;
        for (String line : infoLines) {
            int lineWidth = mc.font.width(line);
            int lineX = centerX - lineWidth / 2;
            guiGraphics.drawString(mc.font, line, lineX, infoY, 0xFFFFFFFF, false);
            infoY += 14;
        }

        // ========== 警告/提示文字 ==========
        poseStack.pushPose();
        float warnScale = 0.95f;
        poseStack.scale(warnScale, warnScale, 1.0f);
        String warnText = "§c" + getWarningText();
        int warnX = (int) ((centerX) / warnScale - mc.font.width(warnText) / 2.0f);
        int warnY = (int) ((panelY + Y_WARNING) / warnScale);
        guiGraphics.drawString(mc.font, warnText, warnX, warnY, 0xFFFFFFFF, false);
        poseStack.popPose();
    }

    private String getConfirmTitle() {
        return switch (confirmType) {
            case BUY_SALES -> "§a§l确认购买";
            case REMOVE_SALES -> "§e§l确认下架";
            case REMOVE_DEMAND -> "§e§l确认取消";
        };
    }

    private ItemStack getItemStack() {
        if (salesOrder != null) {
            return salesOrder.getItemStack();
        }
        return demandOrder != null ? demandOrder.getItemStack() : new net.minecraft.world.item.ItemStack(Items.AIR);
    }

    private String[] getInfoLines() {
        ItemStack itemStack = getItemStack();
        String itemName = itemStack.getHoverName().getString();
        int price = salesOrder != null ? salesOrder.getBasePrice() : demandOrder.getBasePrice();
        String sellerName = salesOrder != null ? salesOrder.getSellerName() : demandOrder.getSellerName();

        return new String[]{
            "§f物品: §e" + itemName,
            "§7价格: §a" + price + " 金币",
            "§7卖家: §b" + sellerName
        };
    }

    private String getWarningText() {
        return switch (confirmType) {
            case BUY_SALES -> "确定要购买此物品吗？购买后将扣除金币";
            case REMOVE_SALES -> "下架后物品将直接返还到物品栏";
            case REMOVE_DEMAND -> "取消后金币将返还到账户";
        };
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

    private void onConfirm() {
        switch (confirmType) {
            case BUY_SALES -> {
                if (salesOrder != null) {
                    EconomySystem_NetworkManager.sendToServer(new PurchaseSalesOrderMessage(salesOrder.getTradeID()));
                }
            }
            case REMOVE_SALES -> {
                if (salesOrder != null) {
                    EconomySystem_NetworkManager.sendToServer(new RemoveSalesOrderMessage(salesOrder.getTradeID()));
                }
            }
            case REMOVE_DEMAND -> {
                if (demandOrder != null) {
                    EconomySystem_NetworkManager.sendToServer(new RemoveDemandOrderMessage(demandOrder.getTradeID()));
                }
            }
        }
        Minecraft.getInstance().setScreen(parentScreen);
    }

    private void onCancel() {
        Minecraft.getInstance().setScreen(parentScreen);
    }

    private void renderRoundedBox(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        guiGraphics.fill(x1 + 1, y1, x2 - 1, y2, color);
        guiGraphics.fill(x1, y1 + 1, x2, y2 - 1, color);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            onCancel();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * 按钮颜色主题
     */
    private enum ButtonColorTheme {
        GREEN,   // 购买
        RED,     // 下架
        YELLOW,  // 取消求单
        GRAY     // 取消按钮
    }

    /**
     * 自定义按钮 - 支持多种颜色主题
     */
    private static class CustomButton extends Button {
        private final ButtonColorTheme theme;

        public CustomButton(int x, int y, int width, int height, Component message,
                            ButtonColorTheme theme, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.theme = theme;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHovered();

            int topColor, bottomColor, borderColor, glowColor;
            switch (theme) {
                case GREEN -> {
                    // 绿色主题（购买）
                    if (hovered) {
                        topColor = 0xFF66DD66;
                        bottomColor = 0xCC448844;
                        borderColor = 0xFF88FF88;
                        glowColor = 0x3044FF44;
                    } else {
                        topColor = 0xFF55CC55;
                        bottomColor = 0xCC337733;
                        borderColor = 0xFF4CAF50;
                        glowColor = 0x20338833;
                    }
                    break;
                }
                case RED -> {
                    // 红色主题（下架）
                    if (hovered) {
                        topColor = 0xFF884444;
                        bottomColor = 0xCC552222;
                        borderColor = 0xFFAA6666;
                        glowColor = 0x30662222;
                    } else {
                        topColor = 0xFF663333;
                        bottomColor = 0xCC441111;
                        borderColor = 0xFFCC4444;
                        glowColor = 0x20551111;
                    }
                    break;
                }
                case YELLOW -> {
                    // 黄色主题（取消求单）
                    if (hovered) {
                        topColor = 0xFFDDDD66;
                        bottomColor = 0xCC884433;
                        borderColor = 0xFFFFCC88;
                        glowColor = 0x30AA7733;
                    } else {
                        topColor = 0xFFCCAA55;
                        bottomColor = 0xCC773333;
                        borderColor = 0xFFFFB300;
                        glowColor = 0x20886622;
                    }
                    break;
                }
                default -> {
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
            }

            int x = getX(), y = getY(), w = width, h = height;

            // 外发光（悬停时）
            if (hovered && theme != ButtonColorTheme.GRAY) {
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

            // 高光效果（非灰色）
            if (theme != ButtonColorTheme.GRAY) {
                guiGraphics.fill(x + 4, y + 2, x + w - 4, y + 3, 0x40FFFFFF);
            }

            // 文字
            String text = getMessage().getString();
            int textX = x + w / 2 - Minecraft.getInstance().font.width(text) / 2;
            int textY = y + (h - 8) / 2;
            guiGraphics.drawString(mc.font, text, textX, textY, 0xFFFFFF, false);
        }
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {}
}

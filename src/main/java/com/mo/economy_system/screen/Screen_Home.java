package com.mo.economy_system.screen;

import com.mo.economy_system.client.util.UiAnimation;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.economy_system.Packet_BalanceRequest;
import com.mo.economy_system.screen.components.CardRenderer;
import com.mo.economy_system.screen.components.UiButtonRenderer;
import com.mo.economy_system.screen.components.UiButtonStyle;
import com.mo.economy_system.screen.economy_system.deliver_box.Screen_DeliveryBox;
import com.mo.economy_system.screen.economy_system.market.Screen_Market;
import com.mo.economy_system.screen.economy_system.shop.Screen_Shop;
import com.mo.economy_system.screen.territory_system.Screen_Territory;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 经济系统主页屏幕 - 现代化卡片风�?
 *
 * 布局�?
 * - 左侧：导航卡片按钮组
 * - 右侧：余额卡�?+ 交易信息卡片（并排） + 富豪榜卡�?
 * - 左下角：版本信息
 */
public class Screen_Home extends Screen {

    // ==================== 布局常量 ====================
    private static final int BASE_WIDTH = 640;
    private static final int BASE_HEIGHT = 360;
    private static final float LEFT_PANEL_PERCENT = 0.25f;
    private static final int CARD_SPACING = 8;
    private static final int PANEL_PADDING = 12;
    private static final int NAV_CARD_HEIGHT = 28;
    private static final int TOP_ROW_HEIGHT = 70;
    private static final int PANEL_ANIMATION_OFFSET = 50;
    private static final int LEADERBOARD_VISIBLE_ROWS = 10;
    private static final int BACKGROUND_COLOR = 0x400A0A14;

    // ==================== 导航卡片配置 ====================
    private static final String[] NAV_ICONS = {"🛒", "📈", "📦", "🏰", "ℹ️"};
    private static final String[] NAV_NAME_KEYS = {
        Util_MessageKeys.HOME_SHOP_BUTTON_KEY,
        Util_MessageKeys.HOME_MARKET_BUTTON_KEY,
        Util_MessageKeys.HOME_DELIVERY_BOX_BUTTON_KEY,
        Util_MessageKeys.HOME_TERRITORY_BUTTON_KEY,
        Util_MessageKeys.HOME_ABOUT_BUTTON_KEY
    };
    private static final int[] NAV_COLORS = {
        CardRenderer.THEME_SHOP,
        CardRenderer.THEME_MARKET,
        CardRenderer.THEME_DELIVERY,
        CardRenderer.THEME_TERRITORY,
        CardRenderer.THEME_ABOUT
    };

    // ==================== 数据 ====================
    private int balance = -1;
    private List<Map.Entry<String, Integer>> accounts;
    private String playerName;
    private int sellOrderCount = 0;
    private int buyOrderCount = 0;

    // ==================== 动画 ====================
    private static final long ANIMATION_DURATION = 500;
    private final UiAnimation openAnimation = new UiAnimation(ANIMATION_DURATION, UiAnimation.Easing.EASE_OUT_CUBIC);
    private boolean skipAnimation = false;

    // ==================== 虚拟坐标系统 ====================
    private float uiScale;
    private int virtualWidth;
    private int virtualHeight;
    private int leftPanelWidth;
    private int rightPanelStartX;
    private int rightPanelWidth;

    // ==================== 导航卡片区域（虚拟坐标） ====================
    private final int[] navCardX1 = new int[NAV_ICONS.length];
    private final int[] navCardY1 = new int[NAV_ICONS.length];
    private final int[] navCardX2 = new int[NAV_ICONS.length];
    private final int[] navCardY2 = new int[NAV_ICONS.length];

    // ==================== 交易信息卡片点击区域 ====================
    private int tradeCardX1, tradeCardY1, tradeCardX2, tradeCardY2;

    // ==================== 富豪榜滚�?====================
    private int leaderboardScrollOffset = 0;
    private final UiButtonStyle[] navButtonStyles = new UiButtonStyle[NAV_COLORS.length];

    public Screen_Home() {
        super(Component.translatable(Util_MessageKeys.HOME_TITLE_KEY));
        EconomySystem_NetworkManager.sendToServer(new Packet_BalanceRequest());
        for (int i = 0; i < NAV_COLORS.length; i++) {
            navButtonStyles[i] = UiButtonStyle.accent(NAV_COLORS[i]);
        }
    }

    @Override
    protected void init() {
        super.init();
        if (skipAnimation) {
            openAnimation.finish();
        } else {
            openAnimation.start();
        }
        if (this.minecraft != null && this.minecraft.player != null) {
            this.playerName = this.minecraft.player.getName().getString();
        }
    }

    private void calculateVirtualSize() {
        float scaleX = (float) this.width / BASE_WIDTH;
        float scaleY = (float) this.height / BASE_HEIGHT;
        uiScale = Math.min(scaleX, scaleY);
        virtualWidth = (int) (this.width / uiScale);
        virtualHeight = (int) (this.height / uiScale);
        leftPanelWidth = (int) (virtualWidth * LEFT_PANEL_PERCENT);
        rightPanelStartX = leftPanelWidth + PANEL_PADDING;
        rightPanelWidth = virtualWidth - rightPanelStartX - PANEL_PADDING;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 先绘制全屏背景（在缩放之前，确保填满整个屏幕�?
        renderFullScreenBackground(guiGraphics);

        calculateVirtualSize();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(uiScale, uiScale, 1.0f);
        renderPanels(guiGraphics, mouseX, mouseY);
        guiGraphics.pose().popPose();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderFullScreenBackground(GuiGraphics guiGraphics) {
        // 使用更淡的背景色
        guiGraphics.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);
    }

    private void renderPanels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        float animProgress = openAnimation.value();
        float virtualMouseX = mouseX / uiScale;
        float virtualMouseY = mouseY / uiScale;
        int leftOffsetX = (int) ((1.0f - animProgress) * -PANEL_ANIMATION_OFFSET);
        int rightOffsetX = (int) ((1.0f - animProgress) * PANEL_ANIMATION_OFFSET);

        // 左侧导航面板
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftOffsetX, 0, 0);
        renderNavPanel(guiGraphics, virtualMouseX, virtualMouseY);
        guiGraphics.pose().popPose();

        // 右侧内容面板
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(rightOffsetX, 0, 0);
        renderContentPanel(guiGraphics, virtualMouseX, virtualMouseY);
        guiGraphics.pose().popPose();

        // 左下角版本信�?
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftOffsetX, 0, 0);
        int versionY = virtualHeight - PANEL_PADDING;
        CardRenderer.drawVersionInfo(guiGraphics, font, PANEL_PADDING, versionY, leftPanelWidth);
        guiGraphics.pose().popPose();
    }

    private void renderNavPanel(GuiGraphics guiGraphics, float mouseX, float mouseY) {
        int cardWidth = leftPanelWidth - PANEL_PADDING * 2;
        int cardHeight = NAV_CARD_HEIGHT;
        int startY = PANEL_PADDING;

        for (int i = 0; i < NAV_ICONS.length; i++) {
            int cardX = PANEL_PADDING;
            int cardY = startY + i * (cardHeight + CARD_SPACING);
            navCardX1[i] = cardX;
            navCardY1[i] = cardY;
            navCardX2[i] = cardX + cardWidth;
            navCardY2[i] = cardY + cardHeight;
            boolean isHovered = (mouseX >= cardX && mouseX <= cardX + cardWidth &&
                                mouseY >= cardY && mouseY <= cardY + cardHeight);
            UiButtonRenderer.drawStripedButton(guiGraphics, font, cardX, cardY, cardWidth, cardHeight,
                Component.translatable(NAV_NAME_KEYS[i]).getString(), NAV_ICONS[i], navButtonStyles[i], isHovered);
        }
    }

    private void renderContentPanel(GuiGraphics guiGraphics, float mouseX, float mouseY) {
        int startY = PANEL_PADDING;
        int topRowHeight = TOP_ROW_HEIGHT;

        // 计算玩家排名
        int playerRank = 0;
        if (accounts != null && playerName != null) {
            for (int i = 0; i < accounts.size(); i++) {
                if (accounts.get(i).getKey().equals(playerName)) {
                    playerRank = i + 1;
                    break;
                }
            }
        }

        // 顶部一排两个卡片：余额 + 交易信息
        int halfWidth = (rightPanelWidth - CARD_SPACING) / 2;

        // 左侧：余额卡�?
        int balanceCardX = rightPanelStartX;
        int balanceCardY = startY;
        CardRenderer.drawBalanceCard(guiGraphics, font,
            balanceCardX, balanceCardY, halfWidth, topRowHeight,
            balance >= 0 ? balance : 0, playerRank);

        // 右侧：交易信息卡�?
        int tradeCardX = balanceCardX + halfWidth + CARD_SPACING;
        tradeCardX1 = tradeCardX;
        tradeCardY1 = startY;
        tradeCardX2 = tradeCardX + halfWidth;
        tradeCardY2 = startY + topRowHeight;
        boolean isTradeHovered = (mouseX >= tradeCardX1 && mouseX <= tradeCardX2 &&
                                  mouseY >= tradeCardY1 && mouseY <= tradeCardY2);
        CardRenderer.drawTradeInfoCard(guiGraphics, font,
            tradeCardX, tradeCardY1, halfWidth, topRowHeight,
            sellOrderCount, buyOrderCount, isTradeHovered);

        // ==================== 富豪榜卡�?====================
        int leaderboardCardY = startY + topRowHeight + CARD_SPACING;
        int leaderboardCardHeight = virtualHeight - leaderboardCardY - PANEL_PADDING;

        CardRenderer.drawLeaderboardCard(guiGraphics, font,
            rightPanelStartX, leaderboardCardY, rightPanelWidth, leaderboardCardHeight,
            accounts, playerName, balance, leaderboardScrollOffset);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float virtualMouseX = (float) mouseX / uiScale;
        float virtualMouseY = (float) mouseY / uiScale;

        // 检查导航卡片点�?
        for (int i = 0; i < NAV_ICONS.length; i++) {
            if (virtualMouseX >= navCardX1[i] && virtualMouseX <= navCardX2[i] &&
                virtualMouseY >= navCardY1[i] && virtualMouseY <= navCardY2[i]) {
                handleNavClick(i);
                return true;
            }
        }

        // 检查交易信息卡片点�?
        if (virtualMouseX >= tradeCardX1 && virtualMouseX <= tradeCardX2 &&
            virtualMouseY >= tradeCardY1 && virtualMouseY <= tradeCardY2) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new Screen_Market());
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleNavClick(int index) {
        if (this.minecraft == null) return;
        switch (index) {
            case 0 -> this.minecraft.setScreen(new Screen_Shop());
            case 1 -> this.minecraft.setScreen(new Screen_Market());
            case 2 -> this.minecraft.setScreen(new Screen_DeliveryBox());
            case 3 -> this.minecraft.setScreen(new Screen_Territory());
            case 4 -> this.minecraft.setScreen(new Screen_About());
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (accounts != null && !accounts.isEmpty()) {
            int maxScroll = Math.max(0, accounts.size() - LEADERBOARD_VISIBLE_ROWS);
            int newOffset = leaderboardScrollOffset - (int) Math.signum(scrollY);
            leaderboardScrollOffset = Mth.clamp(newOffset, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public void updateBalance(int balance, List<Map.Entry<String, Integer>> accounts) {
        this.balance = balance;
        this.accounts = accounts;
    }

    /**
     * 更新交易信息（卖单和求购数量�?
     */
    public void updateTradeInfo(int sellCount, int buyCount) {
        this.sellOrderCount = sellCount;
        this.buyOrderCount = buyCount;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}

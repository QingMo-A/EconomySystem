package com.mo.economy_system.screen.economy_system.deliver_box;

import com.mo.economy_system.core.economy_system.delivery_box.DeliveryItem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.common.network.DeliveryBoxClaimMessage;
import com.mo.economy_system.common.network.DeliveryBoxDataRequestMessage;
import com.mo.economy_system.target.neoforge1211.client.NeoForge1211HomeScreen;
import com.mo.economy_system.screen.components.CardRenderer;
import com.mo.economy_system.screen.components.UiButtonRenderer;
import com.mo.economy_system.screen.components.UiButtonStyle;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 收货箱屏幕 - 卡片网格风格
 *
 * 布局：
 * - 左上角：搜索框
 * - 左下角：收货箱标题
 * - 右下角：ESC返回提示
 * - 中间：物品卡片网格
 * - 底部：翻页控制
 */
public class Screen_DeliveryBox extends Screen {

    private static final AtomicLong REQUEST_IDS = new AtomicLong();

    // ==================== 布局常量 ====================
    private static final int BASE_WIDTH = 640;
    private static final int BASE_HEIGHT = 360;
    private static final int CARD_SPACING = 8;
    private static final int PANEL_PADDING = 12;

    // ==================== 卡片配置 ====================
    private static final int CARD_WIDTH = 200;
    private static final int CARD_HEIGHT = 70;
    private static final int TOTAL_CARD_HEIGHT = CARD_HEIGHT + CARD_SPACING;
    private static final int CARD_PADDING = 8;
    private static final int ICON_SIZE = 32;
    private static final int CLAIM_BTN_WIDTH = 60;
    private static final int CLAIM_BTN_HEIGHT = 18;

    // ==================== 数据 ====================
    private boolean dataLoaded = false;
    private List<DeliveryItem> allItems = new ArrayList<>();
    private List<DeliveryItem> filteredItems = new ArrayList<>();

    // ==================== 分页 ====================
    private int currentPage = 0;
    private int rows = 2;
    private int columns = -1;
    private int itemsPerPage = -1;

    // ==================== 搜索 ====================
    private EditBox searchBox;

    // ==================== 虚拟坐标系统 ====================
    private float uiScale;
    private int virtualWidth;
    private int virtualHeight;

    // ==================== 卡片点击区域 ====================
    private final List<ItemCardArea> cardAreas = new ArrayList<>();
    private final List<IconArea> iconAreas = new ArrayList<>();

    // ==================== 翻页按钮区域 ====================
    private int prevBtnX1, prevBtnY1, prevBtnX2, prevBtnY2;
    private int nextBtnX1, nextBtnY1, nextBtnX2, nextBtnY2;

    // ==================== 按钮样式 ====================
    private final UiButtonStyle pageButtonStyle = createPageButtonStyle(CardRenderer.THEME_SHOP);
    private final UiButtonStyle pageButtonDisabledStyle = createDisabledPageButtonStyle();
    private final UiButtonStyle claimButtonStyle = createActionButtonStyle(CardRenderer.THEME_DELIVERY);

    // ==================== 玩家信息 ====================
    private UUID playerUUID;
    private String playerName;
    private long latestRequestId;

    private record ItemCardArea(int x, int y, int width, int height, int itemIndex) {}
    private record IconArea(int x, int y, int width, int height, ItemStack itemStack) {}

    public Screen_DeliveryBox() {
        super(Component.translatable(Util_MessageKeys.DELIVERY_BOX_TITLE_KEY));
        latestRequestId = REQUEST_IDS.incrementAndGet();
        EconomySystem_NetworkManager.sendToServer(new DeliveryBoxDataRequestMessage(latestRequestId));
    }

    public void updateDeliveryItems(List<DeliveryItem> items) {
        this.dataLoaded = true;
        this.allItems = items;
        this.filteredItems = new ArrayList<>(items);
    }

    public void updateDeliveryItems(long requestId, List<DeliveryItem> items) {
        if (requestId < latestRequestId) return;
        latestRequestId = requestId;
        updateDeliveryItems(items);
    }

    @Override
    protected void init() {
        super.init();
        calculateVirtualSize();

        if (this.minecraft != null && this.minecraft.player != null) {
            this.playerUUID = this.minecraft.player.getUUID();
            this.playerName = this.minecraft.player.getName().getString();
        }

        // 创建搜索框
        int searchBoxWidth = 200;
        int searchBoxHeight = 20;
        int searchBoxX = PANEL_PADDING;
        int searchBoxY = 20;

        this.searchBox = new EditBox(this.font, searchBoxX, searchBoxY, searchBoxWidth, searchBoxHeight, Component.literal("搜索收货箱..."));
        this.searchBox.setMaxLength(50);
        this.searchBox.setHint(Component.literal("搜索收货箱..."));
        this.searchBox.setResponder(this::onSearchChanged);
        this.searchBox.setFocused(false);
        this.addRenderableWidget(this.searchBox);
        updateSearchBoxLayout();
    }

    private void calculateVirtualSize() {
        float scaleX = (float) this.width / BASE_WIDTH;
        float scaleY = (float) this.height / BASE_HEIGHT;
        uiScale = Math.min(scaleX, scaleY);
        virtualWidth = (int) (this.width / uiScale);
        virtualHeight = (int) (this.height / uiScale);
    }

    private void updateSearchBoxLayout() {
        if (searchBox == null) {
            return;
        }

        int boxX = Math.round(PANEL_PADDING * uiScale);
        int boxY = Math.round(20 * uiScale);
        int boxWidth = Math.round(200 * uiScale);
        int boxHeight = Math.round(20 * uiScale);

        searchBox.setX(boxX);
        searchBox.setY(boxY);
        searchBox.setWidth(boxWidth);
        searchBox.setHeight(boxHeight);
    }

    private void onSearchChanged(String text) {
        applySearch(text);
    }

    private void applySearch(String searchText) {
        if (searchText.isEmpty()) {
            filteredItems = new ArrayList<>(allItems);
        } else {
            String lowerSearch = searchText.toLowerCase();
            filteredItems = allItems.stream()
                .filter(item -> itemMatchesSearch(item, lowerSearch))
                .collect(Collectors.toList());
        }
        currentPage = 0;
    }

    private boolean itemMatchesSearch(DeliveryItem item, String search) {
        return item.getItemID().toLowerCase().contains(search) ||
               item.getSource().toLowerCase().contains(search) ||
               item.getItemStack().getHoverName().getString().toLowerCase().contains(search);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 绘制全屏背景
        renderFullScreenBackground(guiGraphics);

        calculateVirtualSize();
        updateSearchBoxLayout();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(uiScale, uiScale, 1.0f);

        float virtualMouseX = mouseX / uiScale;
        float virtualMouseY = mouseY / uiScale;

        // 绘制左下角标题
        drawTitle(guiGraphics);

        // 绘制右下角ESC提示
        drawEscHint(guiGraphics);

        // 绘制搜索框背景
        guiGraphics.pose().popPose();
        renderSearchBoxBackground(guiGraphics);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(uiScale, uiScale, 1.0f);

        // 绘制物品卡片网格
        renderItemCards(guiGraphics, virtualMouseX, virtualMouseY);

        // 绘制翻页控制
        renderPageControls(guiGraphics, virtualMouseX, virtualMouseY);

        guiGraphics.pose().popPose();

        // 渲染物品tooltip
        renderItemTooltips(guiGraphics, mouseX, mouseY);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderFullScreenBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xB0000000);
    }

    private void renderSearchBoxBackground(GuiGraphics guiGraphics) {
        if (searchBox != null) {
            int boxX = searchBox.getX();
            int boxY = searchBox.getY();
            int boxWidth = searchBox.getWidth();
            int boxHeight = searchBox.getHeight();

            int bgColor = 0xE04A5568;
            int borderColor = 0xFFFFB74D;

            guiGraphics.fill(boxX - 4, boxY - 2, boxX + boxWidth + 4, boxY + boxHeight + 2, bgColor);
            guiGraphics.fill(boxX - 4, boxY - 2, boxX + boxWidth + 4, boxY - 1, borderColor);
            guiGraphics.fill(boxX - 4, boxY + boxHeight + 1, boxX + boxWidth + 4, boxY + boxHeight + 2, borderColor);
            guiGraphics.fill(boxX - 4, boxY - 2, boxX - 3, boxY + boxHeight + 2, borderColor);
            guiGraphics.fill(boxX + boxWidth + 3, boxY - 2, boxX + boxWidth + 4, boxY + boxHeight + 2, borderColor);
        }
    }

    private void drawTitle(GuiGraphics guiGraphics) {
        int x = PANEL_PADDING;
        int y = virtualHeight - PANEL_PADDING - font.lineHeight;
        CardRenderer.drawVersionInfo(guiGraphics, font, x, y + font.lineHeight, 140, CardRenderer.UiIcon.DELIVERY, "收货箱");
    }

    private void drawEscHint(GuiGraphics guiGraphics) {
        String hint = "按 ESC 返回";
        int hintWidth = font.width(hint);
        int x = virtualWidth - PANEL_PADDING - hintWidth;
        int y = virtualHeight - PANEL_PADDING - font.lineHeight;
        guiGraphics.drawString(font, hint, x, y, 0x90FFFFFF);
    }

    private void renderItemCards(GuiGraphics guiGraphics, float mouseX, float mouseY) {
        cardAreas.clear();
        iconAreas.clear();

        if (!dataLoaded) {
            String loadingText = "加载中...";
            int textWidth = font.width(loadingText);
            int textX = (virtualWidth - textWidth) / 2;
            int textY = virtualHeight / 2;
            guiGraphics.drawString(font, loadingText, textX, textY, 0x80FFFFFF);
            return;
        }

        if (filteredItems.isEmpty()) {
            String emptyText = "收货箱为空";
            int textWidth = font.width(emptyText);
            int textX = (virtualWidth - textWidth) / 2;
            int textY = virtualHeight / 2;
            guiGraphics.drawString(font, emptyText, textX, textY, 0x80FFFFFF);
            return;
        }

        // 计算列数
        columns = Math.max(1, (virtualWidth - PANEL_PADDING * 2 + CARD_SPACING) / (CARD_WIDTH + CARD_SPACING));
        itemsPerPage = rows * columns;

        // 计算分页
        int totalPages = (int) Math.ceil((double) filteredItems.size() / itemsPerPage);
        currentPage = Math.min(currentPage, Math.max(0, totalPages - 1));

        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, filteredItems.size());

        // 网格配置
        int gridStartX = PANEL_PADDING;
        int gridStartY = 55;

        for (int i = startIndex; i < endIndex; i++) {
            int indexInPage = i - startIndex;
            int col = indexInPage % columns;
            int row = indexInPage / columns;

            int cardX = gridStartX + col * (CARD_WIDTH + CARD_SPACING);
            int cardY = gridStartY + row * TOTAL_CARD_HEIGHT;

            DeliveryItem item = filteredItems.get(i);
            ItemStack itemStack = item.getItemStack();

            boolean isHovered = (mouseX >= cardX && mouseX <= cardX + CARD_WIDTH &&
                                mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT);

            // 绘制物品卡片
            drawItemCard(guiGraphics, font, cardX, cardY, CARD_WIDTH, CARD_HEIGHT,
                itemStack, item.getSource(), isHovered, mouseX, mouseY);

            // 存储卡片区域
            cardAreas.add(new ItemCardArea(cardX, cardY, CARD_WIDTH, CARD_HEIGHT, i));

            // 存储物品图标区域（用于tooltip）
            int iconX = cardX + CARD_PADDING;
            int iconY = cardY + (CARD_HEIGHT - ICON_SIZE) / 2;
            int actualIconSize = ICON_SIZE;
            iconAreas.add(new IconArea(iconX, iconY, actualIconSize, actualIconSize, itemStack));
        }
    }

    private void drawItemCard(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height,
                              ItemStack itemStack, String source, boolean isHovered, float mouseX, float mouseY) {
        CardRenderer.drawCard(guiGraphics, x, y, width, height, CardRenderer.THEME_SHOP, isHovered);

        int iconX = x + CARD_PADDING;
        int iconY = y + (height - ICON_SIZE) / 2;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(2.0f, 2.0f, 1.0f);
        guiGraphics.renderItem(itemStack, iconX / 2, iconY / 2);
        guiGraphics.pose().popPose();

        int infoX = iconX + ICON_SIZE + 8;
        int infoY = y + 8;
        int infoOffset = infoX - x;
        int maxTextWidth = width - infoOffset - CARD_PADDING - CLAIM_BTN_WIDTH - 6;

        String itemName = itemStack.getHoverName().getString();
        int count = itemStack.getCount();
        String nameText = count > 1 ? itemName + " x" + count : itemName;
        String truncatedName = CardRenderer.truncateText(font, nameText, maxTextWidth);
        guiGraphics.drawString(font, truncatedName, infoX, infoY, CardRenderer.TEXT_TITLE);

        infoY += font.lineHeight + 2;
        String sourceText = CardRenderer.truncateText(font, "来自: " + source, maxTextWidth);
        guiGraphics.drawString(font, sourceText, infoX, infoY, CardRenderer.TEXT_DESC);

        int btnX = x + width - CARD_PADDING - CLAIM_BTN_WIDTH;
        int btnY = y + height - CARD_PADDING - CLAIM_BTN_HEIGHT;
        boolean btnHovered = mouseX >= btnX && mouseX <= btnX + CLAIM_BTN_WIDTH &&
                             mouseY >= btnY && mouseY <= btnY + CLAIM_BTN_HEIGHT;
        UiButtonRenderer.drawStripedButton(guiGraphics, font, btnX, btnY, CLAIM_BTN_WIDTH, CLAIM_BTN_HEIGHT,
            "领取", "", claimButtonStyle, btnHovered, UiButtonRenderer.TextAlign.CENTER, false);
    }

    private void renderItemTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        float virtualMouseX = (float) mouseX / uiScale;
        float virtualMouseY = (float) mouseY / uiScale;

        for (IconArea iconArea : iconAreas) {
            if (virtualMouseX >= iconArea.x() && virtualMouseX <= iconArea.x() + iconArea.width() &&
                virtualMouseY >= iconArea.y() && virtualMouseY <= iconArea.y() + iconArea.height()) {

                ItemStack itemStack = iconArea.itemStack();
                if (itemStack != null && !itemStack.isEmpty() && this.minecraft != null && this.minecraft.player != null) {
                    List<Component> tooltipLines = itemStack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(this.minecraft.player.level()), this.minecraft.player, this.minecraft.options.advancedItemTooltips ?
                            net.minecraft.world.item.TooltipFlag.ADVANCED : net.minecraft.world.item.TooltipFlag.NORMAL);

                    guiGraphics.renderTooltip(this.font, tooltipLines, Optional.empty(), mouseX, mouseY);
                }
                break;
            }
        }
    }

    private void renderPageControls(GuiGraphics guiGraphics, float mouseX, float mouseY) {
        int totalPages = getTotalPages();
        if (totalPages <= 1) return;

        String pageText = (currentPage + 1) + " / " + totalPages;
        int pageTextWidth = font.width(pageText);
        int pageTextX = virtualWidth / 2 - pageTextWidth / 2;
        int pageTextY = virtualHeight - 35;
        guiGraphics.drawString(font, pageText, pageTextX, pageTextY, 0xFFFFFFFF);

        int btnWidth = 50;
        int btnHeight = 24;
        int btnY = virtualHeight - 40;
        int prevBtnX = pageTextX - btnWidth - 12;

        prevBtnX1 = prevBtnX;
        prevBtnY1 = btnY;
        prevBtnX2 = prevBtnX + btnWidth;
        prevBtnY2 = btnY + btnHeight;

        boolean prevHovered = (mouseX >= prevBtnX1 && mouseX <= prevBtnX2 && mouseY >= prevBtnY1 && mouseY <= prevBtnY2);
        drawPageButton(guiGraphics, prevBtnX, btnY, btnWidth, btnHeight, "<", prevHovered, currentPage > 0);

        int nextBtnX = pageTextX + pageTextWidth + 12;

        nextBtnX1 = nextBtnX;
        nextBtnY1 = btnY;
        nextBtnX2 = nextBtnX + btnWidth;
        nextBtnY2 = btnY + btnHeight;

        boolean nextHovered = (mouseX >= nextBtnX1 && mouseX <= nextBtnX2 && mouseY >= nextBtnY1 && mouseY <= nextBtnY2);
        drawPageButton(guiGraphics, nextBtnX, btnY, btnWidth, btnHeight, ">", nextHovered, currentPage < totalPages - 1);
    }

    private void drawPageButton(GuiGraphics guiGraphics, int x, int y, int width, int height, String text, boolean isHovered, boolean isEnabled) {
        UiButtonStyle style = isEnabled ? pageButtonStyle : pageButtonDisabledStyle;
        UiButtonRenderer.drawStripedButton(guiGraphics, font, x, y, width, height,
            text, "", style, isEnabled && isHovered, UiButtonRenderer.TextAlign.CENTER, false);
    }

    private int getTotalPages() {
        return (int) Math.ceil((double) filteredItems.size() / itemsPerPage);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float virtualMouseX = (float) mouseX / uiScale;
        float virtualMouseY = (float) mouseY / uiScale;

        // 检查领取按钮点击
        for (ItemCardArea cardArea : cardAreas) {
            if (virtualMouseX >= cardArea.x() && virtualMouseX <= cardArea.x() + cardArea.width() &&
                virtualMouseY >= cardArea.y() && virtualMouseY <= cardArea.y() + cardArea.height()) {

                // 检查是否点击了右侧领取按钮区域
                int claimBtnX = cardArea.x() + CARD_WIDTH - CARD_PADDING - CLAIM_BTN_WIDTH;
                int claimBtnY = cardArea.y() + CARD_HEIGHT - CARD_PADDING - CLAIM_BTN_HEIGHT;
                if (virtualMouseX >= claimBtnX && virtualMouseX <= claimBtnX + CLAIM_BTN_WIDTH &&
                    virtualMouseY >= claimBtnY && virtualMouseY <= claimBtnY + CLAIM_BTN_HEIGHT) {

                    DeliveryItem item = filteredItems.get(cardArea.itemIndex());
                    latestRequestId = REQUEST_IDS.incrementAndGet();
                    EconomySystem_NetworkManager.sendToServer(
                        new DeliveryBoxClaimMessage(item.getDataID(), latestRequestId));
                    return true;
                }
            }
        }

        // 检查翻页按钮
        if (virtualMouseX >= prevBtnX1 && virtualMouseX <= prevBtnX2 &&
            virtualMouseY >= prevBtnY1 && virtualMouseY <= prevBtnY2) {
            if (currentPage > 0) currentPage--;
            return true;
        }

        if (virtualMouseX >= nextBtnX1 && virtualMouseX <= nextBtnX2 &&
            virtualMouseY >= nextBtnY1 && virtualMouseY <= nextBtnY2) {
            if (currentPage < getTotalPages() - 1) currentPage++;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int totalPages = getTotalPages();
        if (totalPages > 1) {
            int newPage = currentPage - (int) Math.signum(scrollY);
            currentPage = Math.max(0, Math.min(totalPages - 1, newPage));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new NeoForge1211HomeScreen());
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private UiButtonStyle createPageButtonStyle(int accentColor) {
        return UiButtonStyle.accent(accentColor)
            .setPadding(6)
            .setStripeWidth(3)
            .setGlowHeight(4)
            .setBgAlpha(0x55)
            .setBgAlphaHover(0x70)
            .setBorderAlpha(0x25)
            .setBorderAlphaHover(0x40)
            .setTextShadow(false);
    }

    private UiButtonStyle createActionButtonStyle(int accentColor) {
        return UiButtonStyle.accent(accentColor)
            .setPadding(6)
            .setStripeWidth(3)
            .setGlowHeight(4)
            .setBgAlpha(0x55)
            .setBgAlphaHover(0x70)
            .setBorderAlpha(0x25)
            .setBorderAlphaHover(0x40)
            .setTextShadow(false);
    }

    private UiButtonStyle createDisabledPageButtonStyle() {
        return UiButtonStyle.accent(0xFF6F7F8C)
            .setTextColor(0xFFB0BBC6)
            .setBgAlpha(0x30)
            .setBgAlphaHover(0x30)
            .setStripeAlpha(0x50)
            .setStripeAlphaHover(0x50)
            .setGlowHeight(0)
            .setBorderAlpha(0x20)
            .setBorderAlphaHover(0x20)
            .setTextShadow(false);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {}
}

package com.mo.economy_system.screen.economy_system.shop;

import com.mo.economy_system.core.economy_system.shop.ShopItem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.economy_system.Packet_ShopDataRequest;
import com.mo.economy_system.screen.Screen_Home;
import com.mo.economy_system.client.util.UiAnimation;
import com.mo.economy_system.screen.components.CardRenderer;
import com.mo.economy_system.screen.components.UiButtonRenderer;
import com.mo.economy_system.screen.components.UiButtonStyle;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 商店屏幕 - 卡片网格风格
 *
 * 布局：
 * - 左上角：搜索框
 * - 左下角：经济系统标题
 * - 右下角：ESC返回提示
 * - 中间：商品卡片（两行）
 * - 底部：翻页控制
 */
public class Screen_Shop extends Screen {

    // ==================== 布局常量 ====================
    private static final int BASE_WIDTH = 640;
    private static final int BASE_HEIGHT = 360;
    private static final int CARD_SPACING = 8;
    private static final int PANEL_PADDING = 12;
    private static final int SEARCH_BOX_WIDTH = 200;
    private static final int SEARCH_BOX_HEIGHT = 20;
    private static final int SEARCH_BOX_TOP = 20;
    private static final int SEARCH_BOX_ANIMATION_OFFSET = 30;
    private static final int GRID_START_Y = 55;
    private static final int PAGE_BUTTON_WIDTH = 50;
    private static final int PAGE_BUTTON_HEIGHT = 24;
    private static final int PANEL_ANIMATION_OFFSET = 40;

    // ==================== 商品卡片配置 ====================
    private static final int CARD_WIDTH = 100;
    private static final int CARD_HEIGHT = 80;
    private static final int CARD_PADDING = 6;
    private static final int ICON_SIZE = 32;

    // ==================== 数据 ====================
    private List<ShopItem> items = new ArrayList<>();
    private List<ShopItem> filteredItems = new ArrayList<>();
    private boolean dataLoaded = false;

    // ==================== 分页 ====================
    private int currentPage = 0;
    private int rows = 3; // 3行
    private int columns = -1; // 根据虚拟坐标计算
    private int itemsPerPage = -1; // 动态计算

    // ==================== 搜索 ====================
    private EditBox searchBox;

    // ==================== 虚拟坐标系统 ====================
    private float uiScale;
    private int virtualWidth;
    private int virtualHeight;

    // ==================== 商品卡片点击区域 ====================
    private final List<ItemCardArea> cardAreas = new ArrayList<>();

    // ==================== 翻页按钮区域 ====================
    private int prevBtnX1, prevBtnY1, prevBtnX2, prevBtnY2;
    private int nextBtnX1, nextBtnY1, nextBtnX2, nextBtnY2;

    // ==================== 动画 ====================
    private static final long ANIMATION_DURATION = 420;
    private final UiAnimation openAnimation = new UiAnimation(ANIMATION_DURATION, UiAnimation.Easing.EASE_OUT_CUBIC);
    private boolean skipAnimation = false;

    // ==================== 按钮样式 ====================
    private final UiButtonStyle pageButtonStyle = createPageButtonStyle(CardRenderer.THEME_MARKET);
    private final UiButtonStyle pageButtonDisabledStyle = createDisabledPageButtonStyle();

    private record ItemCardArea(int x, int y, int width, int height, int itemIndex) {}

    public Screen_Shop() {
        super(Component.translatable(Util_MessageKeys.SHOP_TITLE_KEY));
        EconomySystem_NetworkManager.sendToServer(new Packet_ShopDataRequest());
    }

    public void updateShopItems(List<ShopItem> items) {
        this.items = items;
        this.filteredItems = new ArrayList<>(items);
        this.dataLoaded = true;
    }

    @Override
    protected void init() {
        super.init();
        if (skipAnimation) {
            openAnimation.finish();
        } else {
            openAnimation.start();
        }
        calculateVirtualSize();

        // 创建搜索框（左上角）
        int searchBoxWidth = SEARCH_BOX_WIDTH;
        int searchBoxHeight = SEARCH_BOX_HEIGHT;
        int searchBoxX = PANEL_PADDING;
        int searchBoxY = SEARCH_BOX_TOP;
        Component searchHint = Component.translatable(Util_MessageKeys.SHOP_SEARCH_HINT_TEXT_KEY);
        this.searchBox = new EditBox(this.font, searchBoxX, searchBoxY, searchBoxWidth, searchBoxHeight, searchHint);
        this.searchBox.setMaxLength(50);
        this.searchBox.setHint(searchHint);
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
        int boxY = Math.round(SEARCH_BOX_TOP * uiScale) - getSearchBoxOffsetY();
        int boxWidth = Math.round(SEARCH_BOX_WIDTH * uiScale);
        int boxHeight = Math.round(SEARCH_BOX_HEIGHT * uiScale);

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
            filteredItems = new ArrayList<>(items);
        } else {
            String lowerSearch = searchText.toLowerCase();
            filteredItems = items.stream()
                .filter(item -> itemMatchesSearch(item, lowerSearch))
                .collect(ArrayList::new, (list, item) -> list.add(item), (list1, list2) -> {});
        }
        currentPage = 0;
    }

    private boolean itemMatchesSearch(ShopItem item, String searchText) {
        return item.getItemId().toLowerCase().contains(searchText) ||
                item.getDescription().toLowerCase().contains(searchText) ||
                getPreviewStack(item).getHoverName().getString().toLowerCase().contains(searchText);
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
        float animProgress = openAnimation.value();
        int panelOffsetY = (int) ((1.0f - animProgress) * PANEL_ANIMATION_OFFSET);

        // 绘制左下角标题（经济系统）
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, panelOffsetY, 0);
        drawTitle(guiGraphics);
        guiGraphics.pose().popPose();

        // 绘制右下角ESC提示
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, panelOffsetY, 0);
        drawEscHint(guiGraphics);
        guiGraphics.pose().popPose();

        // 绘制搜索框背景（需要恢复坐标系统）
        guiGraphics.pose().popPose();
        renderSearchBoxBackground(guiGraphics);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(uiScale, uiScale, 1.0f);

        // 绘制商品卡片网格
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, panelOffsetY, 0);
        renderShopItems(guiGraphics, virtualMouseX, virtualMouseY - panelOffsetY);
        guiGraphics.pose().popPose();

        // 绘制翻页控制
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, panelOffsetY, 0);
        renderPageControls(guiGraphics, virtualMouseX, virtualMouseY - panelOffsetY);
        guiGraphics.pose().popPose();

        guiGraphics.pose().popPose();

        // 渲染Tooltip（在恢复坐标后，使用原始鼠标坐标）
        renderTooltip(guiGraphics, mouseX, mouseY);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    /**
     * 渲染物品Tooltip
     */
    private void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        float virtualMouseX = mouseX / uiScale;
        float virtualMouseY = mouseY / uiScale - getContentOffsetY();

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        for (ItemCardArea cardArea : cardAreas) {
            if (virtualMouseX >= cardArea.x() && virtualMouseX <= cardArea.x() + cardArea.width() &&
                virtualMouseY >= cardArea.y() && virtualMouseY <= cardArea.y() + cardArea.height()) {

                ShopItem item = filteredItems.get(cardArea.itemIndex());
                ItemStack itemStack = getPreviewStack(item);

                List<Component> tooltipLines = itemStack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(player.level()), player,
                        Minecraft.getInstance().options.advancedItemTooltips ?
                                net.minecraft.world.item.TooltipFlag.ADVANCED : net.minecraft.world.item.TooltipFlag.NORMAL
                );

                // 添加分隔线
                tooltipLines.add(Component.literal("-=-=-=-=-=-").withStyle(ChatFormatting.DARK_GRAY));

                // 添加价格信息
                int priceDifference = item.getCurrentPrice() - item.getLastPrice();
                String priceChangeText;
                if (priceDifference > 0) {
                    priceChangeText = "+" + priceDifference;
                } else {
                    priceChangeText = String.valueOf(priceDifference);
                }

                ChatFormatting changeColor;
                if (priceDifference > 0) {
                    changeColor = ChatFormatting.RED;
                } else if (priceDifference < 0) {
                    changeColor = ChatFormatting.GREEN;
                } else {
                    changeColor = ChatFormatting.GRAY;
                }
                tooltipLines.add(Component.translatable(Util_MessageKeys.SHOP_ITEM_CHANGE_PRICE_KEY, priceChangeText)
                        .withStyle(changeColor));
                tooltipLines.add(Component.translatable(Util_MessageKeys.SHOP_ITEM_BASIC_PRICE_KEY, item.getBasePrice())
                        .withStyle(ChatFormatting.GRAY));
                tooltipLines.add(Component.translatable(Util_MessageKeys.SHOP_ITEM_CURRENT_PRICE_KEY, item.getCurrentPrice())
                        .withStyle(ChatFormatting.YELLOW));
                tooltipLines.add(Component.translatable(Util_MessageKeys.SHOP_ITEM_FLUCTUATION_FACTOR_KEY, item.getFluctuationFactor())
                        .withStyle(ChatFormatting.GRAY));
                tooltipLines.add(Component.translatable(Util_MessageKeys.SHOP_ITEM_ID_KEY, item.getItemId())
                        .withStyle(ChatFormatting.DARK_GRAY));

                guiGraphics.renderTooltip(this.font, tooltipLines, Optional.empty(), mouseX, mouseY);
                break;
            }
        }
    }

    private void renderFullScreenBackground(GuiGraphics guiGraphics) {
        // 淡黑色半透明背景
        guiGraphics.fill(0, 0, this.width, this.height, 0xB0000000);
    }

    private void renderSearchBoxBackground(GuiGraphics guiGraphics) {
        if (searchBox != null) {
            int boxX = searchBox.getX();
            int boxY = searchBox.getY();
            int boxWidth = searchBox.getWidth();
            int boxHeight = searchBox.getHeight();

            // 搜索框背景（蓝色主题）
            int bgColor = 0xE04A5568;
            int borderColor = 0xFF4FC3F7;

            guiGraphics.fill(boxX - 4, boxY - 2, boxX + boxWidth + 4, boxY + boxHeight + 2, bgColor);
            guiGraphics.fill(boxX - 4, boxY - 2, boxX + boxWidth + 4, boxY - 1, borderColor);
            guiGraphics.fill(boxX - 4, boxY + boxHeight + 1, boxX + boxWidth + 4, boxY + boxHeight + 2, borderColor);
            guiGraphics.fill(boxX - 4, boxY - 2, boxX - 3, boxY + boxHeight + 2, borderColor);
            guiGraphics.fill(boxX + boxWidth + 3, boxY - 2, boxX + boxWidth + 4, boxY + boxHeight + 2, borderColor);
        }
    }

    private void drawTitle(GuiGraphics guiGraphics) {
        // 左下角显示"商店"
        int x = PANEL_PADDING;
        int y = virtualHeight - PANEL_PADDING - font.lineHeight;

        CardRenderer.drawVersionInfo(guiGraphics, font, x, y + font.lineHeight, 120,
            CardRenderer.UiIcon.SHOP, Component.translatable(Util_MessageKeys.SHOP_TITLE_KEY).getString());
    }

    private void drawEscHint(GuiGraphics guiGraphics) {
        String hint = Component.translatable(Util_MessageKeys.SHOP_ESC_HINT_TEXT_KEY).getString();
        int hintWidth = font.width(hint);
        int x = virtualWidth - PANEL_PADDING - hintWidth;
        int y = virtualHeight - PANEL_PADDING - font.lineHeight;

        guiGraphics.drawString(font, hint, x, y, 0x90FFFFFF);
    }

    private void renderShopItems(GuiGraphics guiGraphics, float mouseX, float mouseY) {
        cardAreas.clear();

        if (filteredItems.isEmpty()) {
            String emptyText = getEmptyStateText();
            int textWidth = font.width(emptyText);
            int textX = (virtualWidth - textWidth) / 2;
            int textY = virtualHeight / 2;
            guiGraphics.drawString(font, emptyText, textX, textY, 0x80FFFFFF);
            return;
        }

        // 计算列数（根据虚拟宽度和卡片宽度）
        int contentWidth = virtualWidth - PANEL_PADDING * 2;
        columns = Math.max(1, (contentWidth + CARD_SPACING) / (CARD_WIDTH + CARD_SPACING));
        itemsPerPage = rows * columns;

        // 计算分页
        int totalPages = (int) Math.ceil((double) filteredItems.size() / itemsPerPage);
        currentPage = Math.min(currentPage, Math.max(0, totalPages - 1));

        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, filteredItems.size());

        // 网格配置（动态列数 x 3行，从左到右排列）
        int totalGridWidth = columns * CARD_WIDTH + (columns - 1) * CARD_SPACING;
        int gridStartX = PANEL_PADDING + Math.max(0, (contentWidth - totalGridWidth) / 2);  // 左对齐
        int gridStartY = GRID_START_Y;  // 往上移

        for (int i = startIndex; i < endIndex; i++) {
            int indexInPage = i - startIndex;
            int col = indexInPage % columns;  // 从左到右：0, 1, 2, ...
            int row = indexInPage / columns;  // 行：0, 1, 2

            int cardX = gridStartX + col * (CARD_WIDTH + CARD_SPACING);
            int cardY = gridStartY + row * (CARD_HEIGHT + CARD_SPACING);

            ShopItem item = filteredItems.get(i);
            ItemStack itemStack = getPreviewStack(item);
            String itemName = itemStack.getHoverName().getString();
            int price = item.getCurrentPrice();

            boolean isHovered = (mouseX >= cardX && mouseX <= cardX + CARD_WIDTH &&
                                mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT);

            // 绘制商品卡片
            drawShopItemCard(guiGraphics, font, cardX, cardY, CARD_WIDTH, CARD_HEIGHT,
                itemStack, itemName, price, isHovered);

            // 存储卡片区域（用于点击和Tooltip）
            cardAreas.add(new ItemCardArea(cardX, cardY, CARD_WIDTH, CARD_HEIGHT, i));
        }
    }

    private void renderPageControls(GuiGraphics guiGraphics, float mouseX, float mouseY) {
        int totalPages = getTotalPages();
        if (totalPages <= 1) return;

        // 页码显示
        String pageText = (currentPage + 1) + " / " + totalPages;
        int pageTextWidth = font.width(pageText);
        int pageTextX = virtualWidth / 2 - pageTextWidth / 2;
        int pageTextY = virtualHeight - 35;
        guiGraphics.drawString(font, pageText, pageTextX, pageTextY, 0xFFFFFFFF);

        // 上一页按钮
        int btnWidth = PAGE_BUTTON_WIDTH;
        int btnHeight = PAGE_BUTTON_HEIGHT;
        int btnY = virtualHeight - 40;
        int prevBtnX = pageTextX - btnWidth - 12;

        prevBtnX1 = prevBtnX;
        prevBtnY1 = btnY;
        prevBtnX2 = prevBtnX + btnWidth;
        prevBtnY2 = btnY + btnHeight;

        boolean prevHovered = (mouseX >= prevBtnX1 && mouseX <= prevBtnX2 && mouseY >= prevBtnY1 && mouseY <= prevBtnY2);
        drawPageButton(guiGraphics, prevBtnX, btnY, btnWidth, btnHeight, CardRenderer.UiIcon.ARROW_LEFT, prevHovered, currentPage > 0);

        // 下一页按钮
        int nextBtnX = pageTextX + pageTextWidth + 12;

        nextBtnX1 = nextBtnX;
        nextBtnY1 = btnY;
        nextBtnX2 = nextBtnX + btnWidth;
        nextBtnY2 = btnY + btnHeight;

        boolean nextHovered = (mouseX >= nextBtnX1 && mouseX <= nextBtnX2 && mouseY >= nextBtnY1 && mouseY <= nextBtnY2);
        drawPageButton(guiGraphics, nextBtnX, btnY, btnWidth, btnHeight, CardRenderer.UiIcon.ARROW_RIGHT, nextHovered, currentPage < totalPages - 1);
    }

    private void drawPageButton(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                CardRenderer.UiIcon icon, boolean isHovered, boolean isEnabled) {
        UiButtonStyle style = isEnabled ? pageButtonStyle : pageButtonDisabledStyle;
        UiButtonRenderer.drawStripedButton(guiGraphics, font, x, y, width, height,
            "", "", style, isEnabled && isHovered, UiButtonRenderer.TextAlign.CENTER, false);
        int iconSize = 12;
        int iconX = x + (width - iconSize) / 2;
        int iconY = y + (height - iconSize) / 2;
        CardRenderer.drawUiIconSized(guiGraphics, icon, iconX, iconY, iconSize);
    }

    private int getTotalPages() {
        int safeItemsPerPage = Math.max(1, itemsPerPage);
        return (int) Math.ceil((double) filteredItems.size() / safeItemsPerPage);
    }

    private String getEmptyStateText() {
        if (!dataLoaded) {
            return Component.translatable(Util_MessageKeys.SHOP_LOADING_SHOP_DATA_TEXT_KEY).getString();
        }
        if (!items.isEmpty()) {
            return Component.translatable(Util_MessageKeys.SHOP_NO_MATCHING_ITEMS_TEXT_KEY).getString();
        }
        return Component.translatable(Util_MessageKeys.SHOP_NO_ITEMS_AVAILABLE_TEXT_KEY).getString();
    }

    private ItemStack getPreviewStack(ShopItem item) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? item.getItemStack() : item.getItemStack(player.level().registryAccess());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float virtualMouseX = (float) mouseX / uiScale;
        float virtualMouseY = (float) mouseY / uiScale - getContentOffsetY();

        // 检查商品卡片点击（点击卡片直接跳转购买）
        for (ItemCardArea cardArea : cardAreas) {
            if (virtualMouseX >= cardArea.x() && virtualMouseX <= cardArea.x() + cardArea.width() &&
                virtualMouseY >= cardArea.y() && virtualMouseY <= cardArea.y() + cardArea.height()) {
                ShopItem item = filteredItems.get(cardArea.itemIndex());
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new Screen_BuyItem(item));
                }
                return true;
            }
        }

        // 检查上一页按钮
        if (virtualMouseX >= prevBtnX1 && virtualMouseX <= prevBtnX2 &&
            virtualMouseY >= prevBtnY1 && virtualMouseY <= prevBtnY2) {
            if (currentPage > 0) {
                currentPage--;
            }
            return true;
        }

        // 检查下一页按钮
        if (virtualMouseX >= nextBtnX1 && virtualMouseX <= nextBtnX2 &&
            virtualMouseY >= nextBtnY1 && virtualMouseY <= nextBtnY2) {
            if (currentPage < getTotalPages() - 1) {
                currentPage++;
            }
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

    private int getContentOffsetY() {
        float animProgress = openAnimation.value();
        return (int) ((1.0f - animProgress) * PANEL_ANIMATION_OFFSET);
    }

    private int getSearchBoxOffsetY() {
        float animProgress = openAnimation.value();
        return Math.round((1.0f - animProgress) * SEARCH_BOX_ANIMATION_OFFSET * uiScale);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC键返回主页
        if (keyCode == 256) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new Screen_Home());
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawShopItemCard(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height,
                                  ItemStack itemStack, String itemName, int price, boolean isHovered) {
        CardRenderer.drawCard(guiGraphics, x, y, width, height, CardRenderer.THEME_SHOP, isHovered);

        int textX = x + CARD_PADDING;
        int headerY = y + 6;
        String name = CardRenderer.truncateText(font, itemName, width - CARD_PADDING * 2);
        guiGraphics.drawString(font, name, textX, headerY, CardRenderer.TEXT_TITLE);

        String priceText = "￥" + CardRenderer.formatNumber(price);
        int priceWidth = font.width(priceText);
        guiGraphics.drawString(font, priceText, x + width - CARD_PADDING - priceWidth, headerY, CardRenderer.THEME_BALANCE);

        int iconX = x + (width - ICON_SIZE) / 2;
        int iconY = y + 26;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(2.0f, 2.0f, 1.0f);
        guiGraphics.renderItem(itemStack, iconX / 2, iconY / 2);
        guiGraphics.pose().popPose();
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


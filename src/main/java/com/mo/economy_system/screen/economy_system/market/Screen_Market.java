package com.mo.economy_system.screen.economy_system.market;

import com.mo.economy_system.core.economy_system.market.DemandOrder;
import com.mo.economy_system.core.economy_system.market.MarketItem;
import com.mo.economy_system.core.economy_system.market.SalesOrder;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.economy_system.Packet_MarketDataRequest;
import com.mo.economy_system.network.packets.economy_system.demand_order.Packet_ConfirmDemandOrder;
import com.mo.economy_system.network.packets.economy_system.demand_order.Packet_DeliverDemandOrder;
import com.mo.economy_system.network.packets.economy_system.demand_order.Packet_RemoveDemandOrder;
import com.mo.economy_system.network.packets.economy_system.sales_order.Packet_PurchaseSalesOrder;
import com.mo.economy_system.network.packets.economy_system.sales_order.Packet_RemoveSalesOrder;
import com.mo.economy_system.screen.Screen_Home;
import com.mo.economy_system.screen.components.CardRenderer;
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
import java.util.stream.Collectors;

/**
 * 市场屏幕 - 卡片网格风格
 *
 * 布局：
 * - 左上角：搜索框
 * - 左下角：市场标题
 * - 右下角：ESC返回提示
 * - 中间：订单卡片网格
 * - 底部：翻页控制
 */
public class Screen_Market extends Screen {

    // ==================== 布局常量 ====================
    private static final int BASE_WIDTH = 640;
    private static final int BASE_HEIGHT = 360;
    private static final int CARD_SPACING = 8;
    private static final int PANEL_PADDING = 12;

    // ==================== 订单卡片配置 ====================
    private static final int CARD_WIDTH = 200;
    private static final int CARD_HEIGHT = 80;
    private static final int TOTAL_CARD_HEIGHT = CARD_HEIGHT + CARD_SPACING;

    // ==================== 数据 ====================
    private List<MarketItem> allItems = new ArrayList<>();
    private List<MarketItem> filteredItems = new ArrayList<>();

    // ==================== 分页 ====================
    private int currentPage = 0;
    private int rows = 3;
    private int columns = -1;
    private int itemsPerPage = -1;

    // ==================== 搜索与过滤 ====================
    private EditBox searchBox;
    private int filterIndex = 0; // 0:全部, 1:我的, 2:卖单, 3:求单

    // ==================== 虚拟坐标系统 ====================
    private float uiScale;
    private int virtualWidth;
    private int virtualHeight;

    // ==================== 卡片点击区域 ====================
    private final List<OrderCardArea> cardAreas = new ArrayList<>();
    private final List<OrderCardArea2> cardAreas2 = new ArrayList<>();

    // ==================== 物品图标区域（用于tooltip） ====================
    private final List<ItemIconArea> itemIconAreas = new ArrayList<>();

    // ==================== 翻页按钮区域 ====================
    private int prevBtnX1, prevBtnY1, prevBtnX2, prevBtnY2;
    private int nextBtnX1, nextBtnY1, nextBtnX2, nextBtnY2;

    // ==================== 上架/求购按钮区域 ====================
    private int listBtnX1, listBtnY1, listBtnX2, listBtnY2;
    private int requestBtnX1, requestBtnY1, requestBtnX2, requestBtnY2;

    // ==================== 玩家信息 ====================
    private UUID playerUUID;
    private String playerName;

    private record OrderCardArea(int x, int y, int width, int height, int itemIndex, String actionType) {}
    private record OrderCardArea2(int x, int y, int width, int height, int itemIndex, String actionType) {}
    private record ItemIconArea(int x, int y, int width, int height, ItemStack itemStack) {}

    public Screen_Market() {
        super(Component.translatable(Util_MessageKeys.MARKET_TITLE_KEY));
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_MarketDataRequest());
    }

    public void updateMarketItems(List<MarketItem> items) {
        this.allItems = items;
        this.filteredItems = new ArrayList<>(items);
    }

    @Override
    protected void init() {
        super.init();
        calculateVirtualSize();

        if (this.minecraft != null && this.minecraft.player != null) {
            this.playerUUID = this.minecraft.player.getUUID();
            this.playerName = this.minecraft.player.getName().getString();
        }

        // 创建搜索框（左上角，给右侧按钮留空间）
        int searchBoxWidth = 200;
        int searchBoxX = PANEL_PADDING;
        int searchBoxY = 20;

        this.searchBox = new EditBox(this.font, searchBoxX, searchBoxY, searchBoxWidth, 20, Component.translatable("搜索市场..."));
        this.searchBox.setMaxLength(50);
        this.searchBox.setHint(Component.literal("搜索市场..."));
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
        applyFilters();
    }

    private void applyFilters() {
        filteredItems = allItems.stream()
                .filter(item -> {
                    // 应用搜索过滤
                    if (searchBox != null && !searchBox.getValue().isEmpty()) {
                        String search = searchBox.getValue().toLowerCase();
                        if (!itemMatchesSearch(item, search)) {
                            return false;
                        }
                    }

                    // 应用类型过滤
                    return switch (filterIndex) {
                        case 1 -> item.getSellerName().equals(playerName); // 我的订单
                        case 2 -> item instanceof SalesOrder; // 卖单
                        case 3 -> item instanceof DemandOrder; // 求单
                        default -> true;
                    };
                })
                .collect(Collectors.toList());
        currentPage = 0;
    }

    private boolean itemMatchesSearch(MarketItem item, String search) {
        return item.getItemID().toLowerCase().contains(search) ||
                item.getSellerName().toLowerCase().contains(search) ||
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

        // 绘制右上角按钮
        drawTopButtons(guiGraphics);

        // 绘制右下角ESC提示
        drawEscHint(guiGraphics);

        // 绘制搜索框背景
        guiGraphics.pose().popPose();
        renderSearchBoxBackground(guiGraphics);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(uiScale, uiScale, 1.0f);

        // 绘制订单卡片网格
        renderOrderCards(guiGraphics, virtualMouseX, virtualMouseY);

        // 绘制翻页控制
        renderPageControls(guiGraphics, virtualMouseX, virtualMouseY);

        guiGraphics.pose().popPose();

        // 渲染物品tooltip（在虚拟坐标系统外，使用实际屏幕坐标）
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
            int borderColor = 0xFF4FC3F7;

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

        // 过滤器按钮
        String[] filters = {"全部", "我的", "卖单", "求单"};
        int filterX = x;
        for (int i = 0; i < filters.length; i++) {
            String filterText = filters[i];
            boolean isSelected = filterIndex == i;
            int textWidth = font.width(filterText);
            int color = isSelected ? 0xFFFFFFFF : 0x80FFFFFF;

            guiGraphics.drawString(font, filterText, filterX, y, color);
            if (isSelected) {
                guiGraphics.fill(filterX, y + font.lineHeight + 2, filterX + textWidth, y + font.lineHeight + 3, 0xFF4FC3F7);
            }
            filterX += textWidth + 20;
        }
    }

    private void drawTopButtons(GuiGraphics guiGraphics) {
        // 上架和求购按钮（右上角）
        int btnY = 20;
        int btnHeight = 20;
        int btnSpacing = 8;
        int btnWidth = 60;

        // 上架按钮（右边）
        int listBtnX = virtualWidth - PANEL_PADDING - btnWidth;
        guiGraphics.fill(listBtnX, btnY, listBtnX + btnWidth, btnY + btnHeight, 0xC04CAF50);
        guiGraphics.fill(listBtnX, btnY, listBtnX + btnWidth, btnY + 1, 0xFF6BCF6B);
        guiGraphics.fill(listBtnX, btnY + btnHeight - 1, listBtnX + btnWidth, btnY + btnHeight, 0xFF6BCF6B);
        guiGraphics.fill(listBtnX, btnY, listBtnX + 1, btnY + btnHeight, 0xFF6BCF6B);
        guiGraphics.fill(listBtnX + btnWidth - 1, btnY, listBtnX + btnWidth, btnY + btnHeight, 0xFF6BCF6B);
        String listText = "上架";
        int listTextWidth = font.width(listText);
        guiGraphics.drawString(font, listText, listBtnX + (btnWidth - listTextWidth) / 2, btnY + (btnHeight - font.lineHeight) / 2, 0xFFFFFFFF);
        listBtnX1 = listBtnX;
        listBtnY1 = btnY;
        listBtnX2 = listBtnX + btnWidth;
        listBtnY2 = btnY + btnHeight;

        // 求购按钮（左边）
        int requestBtnX = listBtnX - btnSpacing - btnWidth;
        guiGraphics.fill(requestBtnX, btnY, requestBtnX + btnWidth, btnY + btnHeight, 0xC0FF9800);
        guiGraphics.fill(requestBtnX, btnY, requestBtnX + btnWidth, btnY + 1, 0xFFFFB74D);
        guiGraphics.fill(requestBtnX, btnY + btnHeight - 1, requestBtnX + btnWidth, btnY + btnHeight, 0xFFFFB74D);
        guiGraphics.fill(requestBtnX, btnY, requestBtnX + 1, btnY + btnHeight, 0xFFFFB74D);
        guiGraphics.fill(requestBtnX + btnWidth - 1, btnY, requestBtnX + btnWidth, btnY + btnHeight, 0xFFFFB74D);
        String requestText = "求购";
        int requestTextWidth = font.width(requestText);
        guiGraphics.drawString(font, requestText, requestBtnX + (btnWidth - requestTextWidth) / 2, btnY + (btnHeight - font.lineHeight) / 2, 0xFFFFFFFF);
        requestBtnX1 = requestBtnX;
        requestBtnY1 = btnY;
        requestBtnX2 = requestBtnX + btnWidth;
        requestBtnY2 = btnY + btnHeight;
    }

    private void drawEscHint(GuiGraphics guiGraphics) {
        String hint = "按 ESC 返回";
        int hintWidth = font.width(hint);
        int x = virtualWidth - PANEL_PADDING - hintWidth;
        int y = virtualHeight - PANEL_PADDING - font.lineHeight;
        guiGraphics.drawString(font, hint, x, y, 0x90FFFFFF);
    }

    private void renderOrderCards(GuiGraphics guiGraphics, float mouseX, float mouseY) {
        cardAreas.clear();
        cardAreas2.clear();
        itemIconAreas.clear();

        if (filteredItems.isEmpty()) {
            String emptyText = "暂无订单";
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

            MarketItem item = filteredItems.get(i);
            ItemStack itemStack = item.getItemStack();

            boolean isSalesOrder = item instanceof SalesOrder;
            // 只用自己的UUID判断，不包含管理员权限
            boolean isOwnOrder = item.getSellerID().equals(playerUUID);
            // 管理员标识
            boolean isAdmin = this.minecraft != null && this.minecraft.player != null && this.minecraft.player.hasPermissions(2);
            boolean isHovered = (mouseX >= cardX && mouseX <= cardX + CARD_WIDTH &&
                                mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT);

            // 绘制订单卡片
            drawOrderCard(guiGraphics, font, cardX, cardY, CARD_WIDTH, CARD_HEIGHT,
                itemStack, item.getSellerName(), item.getBasePrice(), isSalesOrder, isOwnOrder, isAdmin, isHovered);

            // 存储物品图标区域（用于tooltip）
            // 图标位置与drawOrderCard中一致：居中，32x32基础大小，2倍缩放后64x64
            int iconSize = 32;
            int iconX = cardX + (CARD_WIDTH - iconSize) / 2;
            int iconY = cardY + 22;
            int actualIconSize = 64; // 2倍缩放后的实际大小
            itemIconAreas.add(new ItemIconArea(iconX, iconY, actualIconSize, actualIconSize, itemStack));

            // 存储卡片和操作按钮区域
            String actionType = getActionType(item, isOwnOrder);
            int actionBtnWidth = 50;
            int actionBtnHeight = 16;
            int actionBtnX = cardX + CARD_WIDTH - 6 - actionBtnWidth;
            int actionBtnY = cardY + CARD_HEIGHT - 6 - actionBtnHeight;
            cardAreas.add(new OrderCardArea(actionBtnX, actionBtnY, actionBtnWidth, actionBtnHeight, i, actionType));

            // 管理员专属：添加额外的强制下架按钮（左侧按钮）
            if (isAdmin && item instanceof SalesOrder && !item.getSellerID().equals(playerUUID)) {
                int removeBtnWidth = 50;  // 和普通按钮一样大
                int removeBtnHeight = 16;
                int removeBtnX = cardX + CARD_WIDTH - 6 - actionBtnWidth - removeBtnWidth - 4;
                int removeBtnY = actionBtnY;
                cardAreas2.add(new OrderCardArea2(removeBtnX, removeBtnY, removeBtnWidth, removeBtnHeight, i, "remove"));
            }
        }
    }

    private String getActionType(MarketItem item, boolean isOwnOrder) {
        if (item instanceof SalesOrder) {
            // 只用自己的订单判断，管理员也有独立的强制下架按钮
            return isOwnOrder ? "remove" : "buy";
        } else if (item instanceof DemandOrder demandOrder) {
            if (isOwnOrder) {
                return demandOrder.isDelivered() ? "confirm" : "cancel";
            }
            return demandOrder.isDelivered() ? "none" : "deliver";
        }
        return "none";
    }

    private void drawOrderCard(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height,
                              ItemStack itemStack, String sellerName, int price, boolean isSalesOrder, boolean isOwnOrder, boolean isAdmin, boolean isHovered) {
        // 卡片背景
        int cardBg = isHovered ? 0xC02A3A4A : 0xA01A2A3A;
        guiGraphics.fill(x, y, x + width, y + height, cardBg);

        // 边框
        int borderColor = isHovered ? 0xFF4A8ACF : 0xFF3A6A9F;
        if (isOwnOrder) {
            borderColor = isHovered ? 0xFF4CAF50 : 0xFF3A8A3F;
        }
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + 1, y + height, borderColor);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);

        // 顶部装饰条
        int topColor = isSalesOrder ? 0xFF4FC3F7 : 0xFFFF9800;
        if (isOwnOrder) topColor = 0xFF4CAF50;
        guiGraphics.fill(x, y, x + width, y + 3, topColor);

        int padding = 6;

        // 订单类型标签（左上角）
        String typeLabel = isSalesOrder ? "📦 卖单" : "📋 求单";
        if (isOwnOrder) typeLabel = "📌 我的";
        guiGraphics.drawString(font, typeLabel, x + padding, y + padding, topColor);

        // 物品名称（类型标签下方，左上角）
        String itemName = itemStack.getHoverName().getString();
        String displayName = CardRenderer.truncateText(font, itemName, width - padding * 2 - 60); // 留出右侧价格空间
        guiGraphics.drawString(font, displayName, x + padding, y + padding + font.lineHeight + 2, 0xFFFFFFFF);

        // 价格（右上角）
        String priceText = "💰 " + formatNumber(price);
        int priceWidth = font.width(priceText);
        guiGraphics.drawString(font, priceText, x + width - padding - priceWidth, y + padding, 0xFFFFD700);

        // 物品图标（居中）
        int iconSize = 32;
        int iconX = x + (width - iconSize) / 2;
        int iconY = y + 22;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(2.0f, 2.0f, 1.0f);
        guiGraphics.renderItem(itemStack, iconX / 2, iconY / 2);
        guiGraphics.pose().popPose();

        // 卖家名称（底部左侧）
        String sellerText = "卖家: " + sellerName;
        String truncatedSeller = CardRenderer.truncateText(font, sellerText, width - padding * 2 - 55); // 留出按钮空间
        guiGraphics.drawString(font, truncatedSeller, x + padding, y + height - 12, 0x80CCCCCC);

        // 操作按钮背景
        int btnWidth = 50;
        int btnHeight = 16;
        int btnX = x + width - padding - btnWidth;
        int btnY = y + height - padding - btnHeight;

        int btnBg = 0xC03A7ABF;
        int btnBorder = 0xFF4A8ACF;
        guiGraphics.fill(btnX, btnY, btnX + btnWidth, btnY + btnHeight, btnBg);
        guiGraphics.fill(btnX, btnY, btnX + btnWidth, btnY + 1, btnBorder);
        guiGraphics.fill(btnX, btnY + btnHeight - 1, btnX + btnWidth, btnY + btnHeight, btnBorder);
        guiGraphics.fill(btnX, btnY, btnX + 1, btnY + btnHeight, btnBorder);
        guiGraphics.fill(btnX + btnWidth - 1, btnY, btnX + btnWidth, btnY + btnHeight, btnBorder);

        // 按钮文字
        String btnText = isSalesOrder ? (isOwnOrder ? "下架" : "购买") : (isOwnOrder ? "取消" : "交付");
        int btnTextWidth = font.width(btnText);
        guiGraphics.drawString(font, btnText, btnX + (btnWidth - btnTextWidth) / 2, btnY + (btnHeight - font.lineHeight) / 2, 0xFFFFFFFF);

        // 管理员专属：左侧额外的强制下架按钮（仅对卖单且非自己订单）
        if (isAdmin && isSalesOrder && !isOwnOrder) {
            int removeBtnWidth = 50;  // 和普通按钮一样大
            int removeBtnHeight = 16;
            int removeBtnX = btnX - removeBtnWidth - 4;
            int removeBtnY = btnY;

            // 强制下架按钮背景（红色）
            int removeBg = 0xC0AA3333;
            int removeBorder = 0xFFCC4444;
            guiGraphics.fill(removeBtnX, removeBtnY, removeBtnX + removeBtnWidth, removeBtnY + removeBtnHeight, removeBg);
            guiGraphics.fill(removeBtnX, removeBtnY, removeBtnX + removeBtnWidth, removeBtnY + 1, removeBorder);
            guiGraphics.fill(removeBtnX, removeBtnY + removeBtnHeight - 1, removeBtnX + removeBtnWidth, removeBtnY + removeBtnHeight, removeBorder);
            guiGraphics.fill(removeBtnX, removeBtnY, removeBtnX + 1, removeBtnY + removeBtnHeight, removeBorder);
            guiGraphics.fill(removeBtnX + removeBtnWidth - 1, removeBtnY, removeBtnX + removeBtnWidth, removeBtnY + removeBtnHeight, removeBorder);

            // 强制下架按钮文字
            String removeText = "强制下架";
            int removeTextWidth = font.width(removeText);
            guiGraphics.drawString(font, removeText, removeBtnX + (removeBtnWidth - removeTextWidth) / 2, removeBtnY + (removeBtnHeight - font.lineHeight) / 2, 0xFFFFFFFF);
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
        int bgColor = isEnabled ? (isHovered ? 0xD04A8ACF : 0xB03A7ABF) : 0x602A2A3A;
        int borderColor = isEnabled ? (isHovered ? 0xFF6AB8FF : 0xFF4A8ACF) : 0xFF3A3A4A;
        int textColor = isEnabled ? 0xFFFFFFFF : 0x60808080;

        guiGraphics.fill(x, y, x + width, y + height, bgColor);
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + 1, y + height, borderColor);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);

        if (isEnabled) {
            guiGraphics.fill(x + 2, y + 1, x + width - 2, y + 2, 0x60FFFFFF);
        }

        int textWidth = font.width(text);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - font.lineHeight) / 2;
        guiGraphics.drawString(font, text, textX, textY, textColor);
    }

    /**
     * 渲染物品tooltip
     * 检测鼠标是否在物品图标区域上，如果是则显示tooltip
     */
    private void renderItemTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        float virtualMouseX = (float) mouseX / uiScale;
        float virtualMouseY = (float) mouseY / uiScale;

        for (ItemIconArea iconArea : itemIconAreas) {
            if (virtualMouseX >= iconArea.x() && virtualMouseX <= iconArea.x() + iconArea.width() &&
                virtualMouseY >= iconArea.y() && virtualMouseY <= iconArea.y() + iconArea.height()) {

                ItemStack itemStack = iconArea.itemStack();
                if (itemStack != null && !itemStack.isEmpty() && this.minecraft != null && this.minecraft.player != null) {
                    // 获取tooltip行
                    List<Component> tooltipLines = itemStack.getTooltipLines(
                        this.minecraft.player,
                        this.minecraft.options.advancedItemTooltips ?
                            net.minecraft.world.item.TooltipFlag.ADVANCED : net.minecraft.world.item.TooltipFlag.NORMAL
                    );

                    // 渲染tooltip，跟随鼠标位置
                    guiGraphics.renderTooltip(this.font, tooltipLines, Optional.empty(), mouseX, mouseY);
                }
                break; // 只显示第一个匹配的tooltip
            }
        }
    }

    private int getTotalPages() {
        return (int) Math.ceil((double) filteredItems.size() / itemsPerPage);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float virtualMouseX = (float) mouseX / uiScale;
        float virtualMouseY = (float) mouseY / uiScale;

        // 检查过滤器点击
        String[] filters = {"全部", "我的", "卖单", "求单"};
        int filterX = PANEL_PADDING;
        int filterY = virtualHeight - PANEL_PADDING - font.lineHeight;
        for (int i = 0; i < filters.length; i++) {
            int textWidth = font.width(filters[i]);
            // 点击区域：文字和下划线
            if (virtualMouseX >= filterX && virtualMouseX <= filterX + textWidth &&
                virtualMouseY >= filterY - 2 && virtualMouseY <= filterY + font.lineHeight + 5) {
                filterIndex = i;
                applyFilters();
                return true;
            }
            filterX += textWidth + 20;
        }

        // 检查上架按钮点击
        if (virtualMouseX >= listBtnX1 && virtualMouseX <= listBtnX2 &&
            virtualMouseY >= listBtnY1 && virtualMouseY <= listBtnY2) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new Screen_CreateSalesOrder(this.minecraft.player));
            }
            return true;
        }

        // 检查求购按钮点击
        if (virtualMouseX >= requestBtnX1 && virtualMouseX <= requestBtnX2 &&
            virtualMouseY >= requestBtnY1 && virtualMouseY <= requestBtnY2) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new Screen_CreateDemandOrder(this.minecraft.player));
            }
            return true;
        }

        // 检查操作按钮点击
        for (OrderCardArea cardArea : cardAreas) {
            if (virtualMouseX >= cardArea.x() && virtualMouseX <= cardArea.x() + cardArea.width() &&
                virtualMouseY >= cardArea.y() && virtualMouseY <= cardArea.y() + cardArea.height()) {

                MarketItem item = filteredItems.get(cardArea.itemIndex());
                handleOrderAction(item, cardArea.actionType());
                return true;
            }
        }

        // 检查管理员专属下架按钮点击
        for (OrderCardArea2 cardArea : cardAreas2) {
            if (virtualMouseX >= cardArea.x() && virtualMouseX <= cardArea.x() + cardArea.width() &&
                virtualMouseY >= cardArea.y() && virtualMouseY <= cardArea.y() + cardArea.height()) {

                MarketItem item = filteredItems.get(cardArea.itemIndex());
                handleOrderAction(item, cardArea.actionType());
                return true;
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

    private void handleOrderAction(MarketItem item, String actionType) {
        switch (actionType) {
            case "buy" -> {
                if (item instanceof SalesOrder salesOrder) {
                    // 显示购买确认弹窗
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new Screen_MarketConfirmDialog(
                            Screen_MarketConfirmDialog.ConfirmType.BUY_SALES, salesOrder, this));
                    }
                }
            }
            case "remove" -> {
                if (item instanceof SalesOrder salesOrder) {
                    // 显示下架确认弹窗
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new Screen_MarketConfirmDialog(
                            Screen_MarketConfirmDialog.ConfirmType.REMOVE_SALES, salesOrder, this));
                    }
                }
            }
            case "deliver" -> {
                if (item instanceof DemandOrder demandOrder) {
                    EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_DeliverDemandOrder(demandOrder.getTradeID()));
                    refresh();
                }
            }
            case "cancel" -> {
                if (item instanceof DemandOrder demandOrder) {
                    // 显示取消确认弹窗
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new Screen_MarketConfirmDialog(
                            Screen_MarketConfirmDialog.ConfirmType.REMOVE_DEMAND, demandOrder, this));
                    }
                }
            }
            case "confirm" -> {
                if (item instanceof DemandOrder demandOrder) {
                    EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_ConfirmDemandOrder(demandOrder.getTradeID()));
                    refresh();
                }
            }
        }
    }

    public void refresh() {
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_MarketDataRequest());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int totalPages = getTotalPages();
        if (totalPages > 1) {
            int newPage = currentPage - (int) Math.signum(delta);
            currentPage = Math.max(0, Math.min(totalPages - 1, newPage));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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

    private static String formatNumber(int num) {
        if (num >= 10000) {
            return String.format("%.1fk", num / 1000.0);
        }
        return String.valueOf(num);
    }
}

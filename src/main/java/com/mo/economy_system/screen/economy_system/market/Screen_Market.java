package com.mo.economy_system.screen.economy_system.market;

import com.mo.economy_system.client.util.UiAnimation;
import com.mo.economy_system.common.client.ClientMarketState;
import com.mo.economy_system.common.network.ConfirmDemandOrderMessage;
import com.mo.economy_system.common.network.DeliverDemandOrderMessage;
import com.mo.economy_system.common.network.MarketDataRequestMessage;
import com.mo.economy_system.common.network.MarketDataRequestPurpose;
import com.mo.economy_system.common.network.MarketOrderFilter;
import com.mo.economy_system.core.economy_system.market.DemandOrder;
import com.mo.economy_system.core.economy_system.market.MarketItem;
import com.mo.economy_system.core.economy_system.market.SalesOrder;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.screen.Screen_Home;
import com.mo.economy_system.screen.components.CardRenderer;
import com.mo.economy_system.screen.components.UiButtonRenderer;
import com.mo.economy_system.screen.components.UiButtonStyle;
import com.mo.economy_system.utils.Util_MessageKeys;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 市场屏幕 - 卡片网格风格
 *
 * <p>布局： - 左上角：搜索框 - 左下角：市场标题 - 右下角：ESC返回提示 - 中间：订单卡片网格 - 底部：翻页控制
 */
public class Screen_Market extends Screen {

  // ==================== 布局常量 ====================
  private static final int BASE_WIDTH = 640;
  private static final int BASE_HEIGHT = 360;
  private static final int CARD_SPACING = 8;
  private static final int PANEL_PADDING = 12;
  private static final int SEARCH_BOX_WIDTH = 200;
  private static final int SEARCH_BOX_HEIGHT = 20;
  private static final int SEARCH_BOX_TOP = 20;
  private static final int SEARCH_BOX_ANIMATION_OFFSET = 30;
  private static final int TOP_BUTTON_ANIMATION_OFFSET = 30;
  private static final int PANEL_ANIMATION_OFFSET = 40;

  // ==================== 订单卡片配置 ====================
  private static final int CARD_WIDTH = 200;
  private static final int CARD_HEIGHT = 80;
  private static final int TOTAL_CARD_HEIGHT = CARD_HEIGHT + CARD_SPACING;
  private static final int CARD_PADDING = 8;
  private static final int ACTION_BTN_WIDTH = 62;
  private static final int ACTION_BTN_HEIGHT = 18;
  private static final int ADMIN_BTN_WIDTH = 72;
  private static final int ACTION_BTN_GAP = 6;
  private static final int ICON_SIZE = 32;
  private static final int ICON_OFFSET_Y = 26;
  private static final int COLOR_DANGER = 0xFFE05D5D;
  private static final DateTimeFormatter EXPIRATION_FORMATTER =
      DateTimeFormatter.ofPattern("MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

  // ==================== 数据 ====================
  private List<MarketItem> filteredItems = new ArrayList<>();

  // ==================== 分页 ====================
  private int currentPage = 0;
  private int rows = 3;
  private int columns = -1;
  private static final int PAGE_SIZE =
      com.mo.economy_system.common.network.EconomyNetworkLimits.MAX_MARKET_PAGE_SIZE;
  private final int itemsPerPage = PAGE_SIZE;
  private boolean initialRequestSent;
  private int totalMatched;
  private int serverOffset;
  private long searchDueMillis = -1;

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
  private final List<OrderInfoArea> orderInfoAreas = new ArrayList<>();

  // ==================== 翻页按钮区域 ====================
  private int prevBtnX1, prevBtnY1, prevBtnX2, prevBtnY2;
  private int nextBtnX1, nextBtnY1, nextBtnX2, nextBtnY2;

  // ==================== 动画 ====================
  private static final long ANIMATION_DURATION = 420;
  private final UiAnimation openAnimation =
      new UiAnimation(ANIMATION_DURATION, UiAnimation.Easing.EASE_OUT_CUBIC);
  private boolean skipAnimation = false;

  // ==================== 上架/求购按钮区域 ====================
  private int listBtnX1, listBtnY1, listBtnX2, listBtnY2;
  private int requestBtnX1, requestBtnY1, requestBtnX2, requestBtnY2;

  // ==================== 玩家信息 ====================
  private UUID playerUUID;
  private String playerName;

  // ==================== 按钮样式 ====================
  private final UiButtonStyle topListStyle;
  private final UiButtonStyle topRequestStyle;
  private final UiButtonStyle actionBuyStyle;
  private final UiButtonStyle actionRemoveStyle;
  private final UiButtonStyle actionDeliverStyle;
  private final UiButtonStyle actionConfirmStyle;
  private final UiButtonStyle actionCancelStyle;
  private final UiButtonStyle actionDisabledStyle;
  private final UiButtonStyle pageButtonStyle = createPageButtonStyle(CardRenderer.THEME_MARKET);
  private final UiButtonStyle pageButtonDisabledStyle = createDisabledPageButtonStyle();

  private record OrderCardArea(
      int x, int y, int width, int height, int itemIndex, String actionType) {}

  private record OrderCardArea2(
      int x, int y, int width, int height, int itemIndex, String actionType) {}

  private record ItemIconArea(int x, int y, int width, int height, ItemStack itemStack) {}

  private record OrderInfoArea(int x, int y, int width, int height, MarketItem item) {}

  public Screen_Market() {
    super(Component.translatable(Util_MessageKeys.MARKET_TITLE_KEY));
    topListStyle = createTopButtonStyle(CardRenderer.THEME_DELIVERY);
    topRequestStyle = createTopButtonStyle(CardRenderer.THEME_SHOP);
    actionBuyStyle = createActionButtonStyle(CardRenderer.THEME_MARKET);
    actionRemoveStyle = createActionButtonStyle(COLOR_DANGER);
    actionDeliverStyle = createActionButtonStyle(CardRenderer.THEME_SHOP);
    actionConfirmStyle = createActionButtonStyle(CardRenderer.THEME_DELIVERY);
    actionCancelStyle = createActionButtonStyle(COLOR_DANGER);
    actionDisabledStyle =
        createActionButtonStyle(0xFF6F7F8C)
            .setTextColor(0xFFB0BBC6)
            .setBgAlpha(0x30)
            .setBgAlphaHover(0x30)
            .setStripeAlpha(0x50)
            .setStripeAlphaHover(0x50)
            .setGlowHeight(0)
            .setBorderAlpha(0x20)
            .setBorderAlphaHover(0x20);
  }

  private void requestMarketPage() {
    requestMarketPage(serverOffset);
  }

  private void requestMarketPage(int offset) {
    long requestId = ClientMarketState.nextPageRequestId();
    MarketOrderFilter filter =
        switch (filterIndex) {
          case 1 -> MarketOrderFilter.MINE;
          case 2 -> MarketOrderFilter.SALES;
          case 3 -> MarketOrderFilter.DEMAND;
          default -> MarketOrderFilter.ALL;
        };
    String query = searchBox == null ? "" : searchBox.getValue();
    EconomySystem_NetworkManager.sendToServer(
        new MarketDataRequestMessage(
            requestId,
            MarketDataRequestPurpose.PAGE,
            Math.max(0, offset),
            Math.max(1, itemsPerPage),
            filter,
            query));
  }

  public void updateMarketItems(List<MarketItem> items) {
    this.filteredItems = new ArrayList<>(items);
    var state = ClientMarketState.snapshot();
    this.totalMatched = state.totalMatched();
    this.serverOffset = state.offset();
    this.currentPage = itemsPerPage > 0 ? serverOffset / itemsPerPage : 0;
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

    if (this.minecraft != null && this.minecraft.player != null) {
      this.playerUUID = this.minecraft.player.getUUID();
      this.playerName = this.minecraft.player.getName().getString();
    }

    // 创建搜索框（左上角，给右侧按钮留空间）
    int searchBoxWidth = SEARCH_BOX_WIDTH;
    int searchBoxX = PANEL_PADDING;
    int searchBoxY = SEARCH_BOX_TOP;

    this.searchBox =
        new EditBox(
            this.font,
            searchBoxX,
            searchBoxY,
            searchBoxWidth,
            SEARCH_BOX_HEIGHT,
            Component.translatable("搜索市场..."));
    this.searchBox.setMaxLength(50);
    this.searchBox.setHint(Component.translatable(Util_MessageKeys.MARKET_SEARCH_HINT));
    this.searchBox.setResponder(this::onSearchChanged);
    this.searchBox.setFocused(false);
    this.addRenderableWidget(this.searchBox);
    updateSearchBoxLayout();
    if (!initialRequestSent) {
      initialRequestSent = true;
      requestMarketPage(0);
    }
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
    searchDueMillis = System.currentTimeMillis() + 250L;
  }

  @Override
  public void tick() {
    super.tick();
    if (searchDueMillis >= 0 && System.currentTimeMillis() >= searchDueMillis) {
      searchDueMillis = -1;
      serverOffset = 0;
      requestMarketPage(0);
    }
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
    int topButtonsOffsetY = (int) ((1.0f - animProgress) * TOP_BUTTON_ANIMATION_OFFSET);

    // 绘制左下角标题
    guiGraphics.pose().pushPose();
    guiGraphics.pose().translate(0, panelOffsetY, 0);
    drawTitle(guiGraphics);
    guiGraphics.pose().popPose();

    // 绘制右上角按钮
    guiGraphics.pose().pushPose();
    guiGraphics.pose().translate(0, -topButtonsOffsetY, 0);
    drawTopButtons(guiGraphics, virtualMouseX, virtualMouseY + topButtonsOffsetY);
    guiGraphics.pose().popPose();

    // 绘制右下角ESC提示
    guiGraphics.pose().pushPose();
    guiGraphics.pose().translate(0, panelOffsetY, 0);
    drawEscHint(guiGraphics);
    guiGraphics.pose().popPose();

    // 绘制搜索框背景
    guiGraphics.pose().popPose();
    renderSearchBoxBackground(guiGraphics);
    guiGraphics.pose().pushPose();
    guiGraphics.pose().scale(uiScale, uiScale, 1.0f);

    // 绘制订单卡片网格
    guiGraphics.pose().pushPose();
    guiGraphics.pose().translate(0, panelOffsetY, 0);
    renderOrderCards(guiGraphics, virtualMouseX, virtualMouseY - panelOffsetY);
    guiGraphics.pose().popPose();

    // 绘制翻页控制
    guiGraphics.pose().pushPose();
    guiGraphics.pose().translate(0, panelOffsetY, 0);
    renderPageControls(guiGraphics, virtualMouseX, virtualMouseY - panelOffsetY);
    guiGraphics.pose().popPose();

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
      guiGraphics.fill(
          boxX - 4, boxY + boxHeight + 1, boxX + boxWidth + 4, boxY + boxHeight + 2, borderColor);
      guiGraphics.fill(boxX - 4, boxY - 2, boxX - 3, boxY + boxHeight + 2, borderColor);
      guiGraphics.fill(
          boxX + boxWidth + 3, boxY - 2, boxX + boxWidth + 4, boxY + boxHeight + 2, borderColor);
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
        guiGraphics.fill(
            filterX,
            y + font.lineHeight + 2,
            filterX + textWidth,
            y + font.lineHeight + 3,
            0xFF4FC3F7);
      }
      filterX += textWidth + 20;
    }
  }

  private void drawTopButtons(GuiGraphics guiGraphics, float mouseX, float mouseY) {
    // 上架和求购按钮（右上角）
    int btnY = 18;
    int btnHeight = 24;
    int btnSpacing = 10;
    int btnWidth = 84;

    // 上架按钮（右边）
    int listBtnX = virtualWidth - PANEL_PADDING - btnWidth;
    boolean listHovered =
        (mouseX >= listBtnX
            && mouseX <= listBtnX + btnWidth
            && mouseY >= btnY
            && mouseY <= btnY + btnHeight);
    drawStripedButton(
        guiGraphics, listBtnX, btnY, btnWidth, btnHeight, "上架", topListStyle, listHovered);
    listBtnX1 = listBtnX;
    listBtnY1 = btnY;
    listBtnX2 = listBtnX + btnWidth;
    listBtnY2 = btnY + btnHeight;

    // 求购按钮（左边）
    int requestBtnX = listBtnX - btnSpacing - btnWidth;
    boolean requestHovered =
        (mouseX >= requestBtnX
            && mouseX <= requestBtnX + btnWidth
            && mouseY >= btnY
            && mouseY <= btnY + btnHeight);
    drawStripedButton(
        guiGraphics, requestBtnX, btnY, btnWidth, btnHeight, "求购", topRequestStyle, requestHovered);
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
    orderInfoAreas.clear();

    if (filteredItems.isEmpty()) {
      String emptyText = "暂无订单";
      int textWidth = font.width(emptyText);
      int textX = (virtualWidth - textWidth) / 2;
      int textY = virtualHeight / 2;
      guiGraphics.drawString(font, emptyText, textX, textY, 0x80FFFFFF);
      return;
    }

    // 计算列数
    columns =
        Math.max(
            1, (virtualWidth - PANEL_PADDING * 2 + CARD_SPACING) / (CARD_WIDTH + CARD_SPACING));
    int startIndex = 0;
    int endIndex = Math.min(itemsPerPage, filteredItems.size());

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
      boolean isAdmin =
          this.minecraft != null
              && this.minecraft.player != null
              && this.minecraft.player.hasPermissions(2);
      boolean isHovered =
          (mouseX >= cardX
              && mouseX <= cardX + CARD_WIDTH
              && mouseY >= cardY
              && mouseY <= cardY + CARD_HEIGHT);
      String actionType = getActionType(item, isOwnOrder);

      // 绘制订单卡片
      drawOrderCard(
          guiGraphics,
          font,
          cardX,
          cardY,
          CARD_WIDTH,
          CARD_HEIGHT,
          itemStack,
          item.getSellerName(),
          item.getBasePrice(),
          isSalesOrder,
          isOwnOrder,
          isAdmin,
          isHovered,
          actionType,
          mouseX,
          mouseY);

      // 存储物品图标区域（用于tooltip）
      // 图标位置与drawOrderCard中一致：居中，16x16基础大小，2倍缩放后约32x32
      int iconX = cardX + (CARD_WIDTH - ICON_SIZE) / 2;
      int iconY = cardY + ICON_OFFSET_Y;
      int actualIconSize = ICON_SIZE; // 2倍缩放后的实际大小为32
      itemIconAreas.add(new ItemIconArea(iconX, iconY, actualIconSize, actualIconSize, itemStack));
      orderInfoAreas.add(new OrderInfoArea(cardX, cardY, CARD_WIDTH, CARD_HEIGHT, item));

      // 存储卡片和操作按钮区域
      int actionBtnX = cardX + CARD_WIDTH - CARD_PADDING - ACTION_BTN_WIDTH;
      int actionBtnY = cardY + CARD_HEIGHT - CARD_PADDING - ACTION_BTN_HEIGHT;
      if (!"none".equals(actionType)) {
        cardAreas.add(
            new OrderCardArea(
                actionBtnX, actionBtnY, ACTION_BTN_WIDTH, ACTION_BTN_HEIGHT, i, actionType));
      }

      // 管理员专属：添加额外的强制下架按钮（左侧按钮）
      if (isAdmin && item instanceof SalesOrder && !item.getSellerID().equals(playerUUID)) {
        int removeBtnWidth = ADMIN_BTN_WIDTH;
        int removeBtnHeight = ACTION_BTN_HEIGHT;
        int removeBtnX =
            cardX + CARD_WIDTH - CARD_PADDING - ACTION_BTN_WIDTH - removeBtnWidth - ACTION_BTN_GAP;
        int removeBtnY = actionBtnY;
        cardAreas2.add(
            new OrderCardArea2(
                removeBtnX, removeBtnY, removeBtnWidth, removeBtnHeight, i, "remove"));
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

  private void drawOrderCard(
      GuiGraphics guiGraphics,
      Font font,
      int x,
      int y,
      int width,
      int height,
      ItemStack itemStack,
      String sellerName,
      int price,
      boolean isSalesOrder,
      boolean isOwnOrder,
      boolean isAdmin,
      boolean isHovered,
      String actionType,
      float mouseX,
      float mouseY) {
    int themeColor =
        isOwnOrder
            ? CardRenderer.THEME_DELIVERY
            : (isSalesOrder ? CardRenderer.THEME_MARKET : CardRenderer.THEME_SHOP);
    CardRenderer.drawCard(guiGraphics, x, y, width, height, themeColor, isHovered);

    int headerY = y + 6;
    String typeLabel = isOwnOrder ? "我的" : (isSalesOrder ? "卖单" : "求单");
    guiGraphics.drawString(font, typeLabel, x + CARD_PADDING, headerY, themeColor);

    String priceText = "￥" + formatNumber(price);
    int priceWidth = font.width(priceText);
    guiGraphics.drawString(
        font,
        priceText,
        x + width - CARD_PADDING - priceWidth,
        headerY,
        CardRenderer.THEME_BALANCE);

    String itemName = itemStack.getHoverName().getString();
    String countText = " x" + itemStack.getCount();
    String displayName =
        CardRenderer.truncateText(font, itemName + countText, width - CARD_PADDING * 2);
    guiGraphics.drawString(
        font,
        displayName,
        x + CARD_PADDING,
        headerY + font.lineHeight + 2,
        CardRenderer.TEXT_TITLE);

    int iconX = x + (width - ICON_SIZE) / 2;
    int iconY = y + ICON_OFFSET_Y;
    guiGraphics.pose().pushPose();
    guiGraphics.pose().scale(2.0f, 2.0f, 1.0f);
    guiGraphics.renderItem(itemStack, iconX / 2, iconY / 2);
    guiGraphics.pose().popPose();

    String sellerText =
        Component.translatable(
                    isSalesOrder
                        ? Util_MessageKeys.MARKET_SELLER
                        : Util_MessageKeys.MARKET_REQUESTER)
                .getString()
            + ": "
            + sellerName;
    int maxSellerWidth = width - CARD_PADDING * 2 - ACTION_BTN_WIDTH - ACTION_BTN_GAP;
    String truncatedSeller = CardRenderer.truncateText(font, sellerText, maxSellerWidth);
    guiGraphics.drawString(
        font, truncatedSeller, x + CARD_PADDING, y + height - 12, CardRenderer.TEXT_DESC);

    int btnX = x + width - CARD_PADDING - ACTION_BTN_WIDTH;
    int btnY = y + height - CARD_PADDING - ACTION_BTN_HEIGHT;
    boolean actionHovered =
        (mouseX >= btnX
            && mouseX <= btnX + ACTION_BTN_WIDTH
            && mouseY >= btnY
            && mouseY <= btnY + ACTION_BTN_HEIGHT);

    if (!"none".equals(actionType)) {
      drawStripedButton(
          guiGraphics,
          btnX,
          btnY,
          ACTION_BTN_WIDTH,
          ACTION_BTN_HEIGHT,
          getActionText(actionType),
          getActionStyle(actionType),
          actionHovered);
    } else {
      drawStripedButton(
          guiGraphics,
          btnX,
          btnY,
          ACTION_BTN_WIDTH,
          ACTION_BTN_HEIGHT,
          "已完成",
          actionDisabledStyle,
          false);
    }

    if (isAdmin && isSalesOrder && !isOwnOrder) {
      int removeBtnX = btnX - ADMIN_BTN_WIDTH - ACTION_BTN_GAP;
      int removeBtnY = btnY;
      boolean removeHovered =
          (mouseX >= removeBtnX
              && mouseX <= removeBtnX + ADMIN_BTN_WIDTH
              && mouseY >= removeBtnY
              && mouseY <= removeBtnY + ACTION_BTN_HEIGHT);
      drawStripedButton(
          guiGraphics,
          removeBtnX,
          removeBtnY,
          ADMIN_BTN_WIDTH,
          ACTION_BTN_HEIGHT,
          "强制下架",
          actionRemoveStyle,
          removeHovered);
    }
  }

  private void drawStripedButton(
      GuiGraphics guiGraphics,
      int x,
      int y,
      int width,
      int height,
      String text,
      UiButtonStyle style,
      boolean hovered) {
    UiButtonRenderer.drawStripedButton(
        guiGraphics,
        this.font,
        x,
        y,
        width,
        height,
        text,
        "",
        style,
        hovered,
        UiButtonRenderer.TextAlign.CENTER,
        false);
  }

  private UiButtonStyle getActionStyle(String actionType) {
    return switch (actionType) {
      case "buy" -> actionBuyStyle;
      case "remove" -> actionRemoveStyle;
      case "deliver" -> actionDeliverStyle;
      case "confirm" -> actionConfirmStyle;
      case "cancel" -> actionCancelStyle;
      default -> actionDisabledStyle;
    };
  }

  private String getActionText(String actionType) {
    return switch (actionType) {
      case "buy" -> "购买";
      case "remove" -> "下架";
      case "deliver" -> "交付";
      case "confirm" -> "确认";
      case "cancel" -> "取消";
      default -> "操作";
    };
  }

  private UiButtonStyle createTopButtonStyle(int accentColor) {
    return UiButtonStyle.accent(accentColor)
        .setPadding(10)
        .setStripeWidth(4)
        .setGlowHeight(7)
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

    boolean prevHovered =
        (mouseX >= prevBtnX1 && mouseX <= prevBtnX2 && mouseY >= prevBtnY1 && mouseY <= prevBtnY2);
    drawPageButton(
        guiGraphics, prevBtnX, btnY, btnWidth, btnHeight, "<", prevHovered, currentPage > 0);

    int nextBtnX = pageTextX + pageTextWidth + 12;

    nextBtnX1 = nextBtnX;
    nextBtnY1 = btnY;
    nextBtnX2 = nextBtnX + btnWidth;
    nextBtnY2 = btnY + btnHeight;

    boolean nextHovered =
        (mouseX >= nextBtnX1 && mouseX <= nextBtnX2 && mouseY >= nextBtnY1 && mouseY <= nextBtnY2);
    drawPageButton(
        guiGraphics,
        nextBtnX,
        btnY,
        btnWidth,
        btnHeight,
        ">",
        nextHovered,
        currentPage < totalPages - 1);
  }

  private void drawPageButton(
      GuiGraphics guiGraphics,
      int x,
      int y,
      int width,
      int height,
      String text,
      boolean isHovered,
      boolean isEnabled) {
    UiButtonStyle style = isEnabled ? pageButtonStyle : pageButtonDisabledStyle;
    UiButtonRenderer.drawStripedButton(
        guiGraphics,
        font,
        x,
        y,
        width,
        height,
        text,
        "",
        style,
        isEnabled && isHovered,
        UiButtonRenderer.TextAlign.CENTER,
        false);
  }

  /** 渲染物品tooltip 检测鼠标是否在物品图标区域上，如果是则显示tooltip */
  private void renderItemTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    float virtualMouseX = (float) mouseX / uiScale;
    float virtualMouseY = (float) mouseY / uiScale - getContentOffsetY();

    for (ItemIconArea iconArea : itemIconAreas) {
      if (virtualMouseX >= iconArea.x()
          && virtualMouseX <= iconArea.x() + iconArea.width()
          && virtualMouseY >= iconArea.y()
          && virtualMouseY <= iconArea.y() + iconArea.height()) {

        ItemStack itemStack = iconArea.itemStack();
        if (itemStack != null
            && !itemStack.isEmpty()
            && this.minecraft != null
            && this.minecraft.player != null) {
          // 获取tooltip行
          List<Component> tooltipLines =
              new ArrayList<>(
                  itemStack.getTooltipLines(
                      net.minecraft.world.item.Item.TooltipContext.of(
                          this.minecraft.player.level()),
                      this.minecraft.player,
                      this.minecraft.options.advancedItemTooltips
                          ? net.minecraft.world.item.TooltipFlag.ADVANCED
                          : net.minecraft.world.item.TooltipFlag.NORMAL));
          appendOrderInfoTooltip(tooltipLines, findOrderForIcon(iconArea));

          // 渲染tooltip，跟随鼠标位置
          guiGraphics.renderTooltip(this.font, tooltipLines, Optional.empty(), mouseX, mouseY);
        }
        return; // 只显示第一个匹配的tooltip
      }
    }

    for (OrderInfoArea infoArea : orderInfoAreas) {
      if (virtualMouseX >= infoArea.x()
          && virtualMouseX <= infoArea.x() + infoArea.width()
          && virtualMouseY >= infoArea.y()
          && virtualMouseY <= infoArea.y() + infoArea.height()) {
        List<Component> tooltipLines = new ArrayList<>();
        appendOrderInfoTooltip(tooltipLines, infoArea.item());
        guiGraphics.renderTooltip(this.font, tooltipLines, Optional.empty(), mouseX, mouseY);
        return;
      }
    }
  }

  private MarketItem findOrderForIcon(ItemIconArea iconArea) {
    for (OrderInfoArea infoArea : orderInfoAreas) {
      if (iconArea.x() >= infoArea.x()
          && iconArea.x() < infoArea.x() + infoArea.width()
          && iconArea.y() >= infoArea.y()
          && iconArea.y() < infoArea.y() + infoArea.height()) {
        return infoArea.item();
      }
    }
    return null;
  }

  private void appendOrderInfoTooltip(List<Component> tooltipLines, MarketItem item) {
    if (item == null) {
      return;
    }
    tooltipLines.add(Component.literal("物品ID: " + item.getItemID()).withStyle(ChatFormatting.GRAY));
    tooltipLines.add(
        Component.literal("商品ID: " + item.getTradeID()).withStyle(ChatFormatting.DARK_GRAY));
    tooltipLines.add(
        Component.literal(
                "过期时间: "
                    + EXPIRATION_FORMATTER.format(Instant.ofEpochMilli(item.getExpirationTime())))
            .withStyle(ChatFormatting.GOLD));
    long remainingMillis = Math.max(0L, item.getExpirationTime() - System.currentTimeMillis());
    tooltipLines.add(
        Component.literal("剩余时间: " + formatDuration(remainingMillis))
            .withStyle(ChatFormatting.YELLOW));
  }

  private String formatDuration(long millis) {
    long totalSeconds = millis / 1000L;
    long days = totalSeconds / 86400L;
    long hours = (totalSeconds % 86400L) / 3600L;
    long minutes = (totalSeconds % 3600L) / 60L;
    if (days > 0) {
      return days + "天 " + hours + "小时";
    }
    if (hours > 0) {
      return hours + "小时 " + minutes + "分钟";
    }
    return minutes + "分钟";
  }

  private int getTotalPages() {
    return itemsPerPage <= 0 ? 0 : (int) Math.ceil((double) totalMatched / itemsPerPage);
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    float virtualMouseX = (float) mouseX / uiScale;
    float virtualMouseY = (float) mouseY / uiScale - getContentOffsetY();
    float virtualMouseYTop = (float) mouseY / uiScale + getTopButtonsOffsetY();

    // 检查过滤器点击
    String[] filters = {"全部", "我的", "卖单", "求单"};
    int filterX = PANEL_PADDING;
    int filterY = virtualHeight - PANEL_PADDING - font.lineHeight;
    for (int i = 0; i < filters.length; i++) {
      int textWidth = font.width(filters[i]);
      // 点击区域：文字和下划线
      if (virtualMouseX >= filterX
          && virtualMouseX <= filterX + textWidth
          && virtualMouseY >= filterY - 2
          && virtualMouseY <= filterY + font.lineHeight + 5) {
        filterIndex = i;
        serverOffset = 0;
        requestMarketPage(0);
        return true;
      }
      filterX += textWidth + 20;
    }

    // 检查上架按钮点击
    if (virtualMouseX >= listBtnX1
        && virtualMouseX <= listBtnX2
        && virtualMouseYTop >= listBtnY1
        && virtualMouseYTop <= listBtnY2) {
      if (this.minecraft != null) {
        this.minecraft.setScreen(new Screen_CreateSalesOrder(this.minecraft.player));
      }
      return true;
    }

    // 检查求购按钮点击
    if (virtualMouseX >= requestBtnX1
        && virtualMouseX <= requestBtnX2
        && virtualMouseYTop >= requestBtnY1
        && virtualMouseYTop <= requestBtnY2) {
      if (this.minecraft != null) {
        this.minecraft.setScreen(new Screen_CreateDemandOrder(this.minecraft.player));
      }
      return true;
    }

    // 检查操作按钮点击
    for (OrderCardArea cardArea : cardAreas) {
      if (virtualMouseX >= cardArea.x()
          && virtualMouseX <= cardArea.x() + cardArea.width()
          && virtualMouseY >= cardArea.y()
          && virtualMouseY <= cardArea.y() + cardArea.height()) {

        MarketItem item = filteredItems.get(cardArea.itemIndex());
        handleOrderAction(item, cardArea.actionType());
        return true;
      }
    }

    // 检查管理员专属下架按钮点击
    for (OrderCardArea2 cardArea : cardAreas2) {
      if (virtualMouseX >= cardArea.x()
          && virtualMouseX <= cardArea.x() + cardArea.width()
          && virtualMouseY >= cardArea.y()
          && virtualMouseY <= cardArea.y() + cardArea.height()) {

        MarketItem item = filteredItems.get(cardArea.itemIndex());
        handleOrderAction(item, cardArea.actionType());
        return true;
      }
    }

    // 检查翻页按钮
    if (virtualMouseX >= prevBtnX1
        && virtualMouseX <= prevBtnX2
        && virtualMouseY >= prevBtnY1
        && virtualMouseY <= prevBtnY2) {
      if (currentPage > 0) requestMarketPage(Math.max(0, serverOffset - itemsPerPage));
      return true;
    }

    if (virtualMouseX >= nextBtnX1
        && virtualMouseX <= nextBtnX2
        && virtualMouseY >= nextBtnY1
        && virtualMouseY <= nextBtnY2) {
      if (currentPage < getTotalPages() - 1) requestMarketPage(serverOffset + itemsPerPage);
      return true;
    }

    return super.mouseClicked(mouseX, mouseY, button);
  }

  private int getContentOffsetY() {
    float animProgress = openAnimation.value();
    return (int) ((1.0f - animProgress) * PANEL_ANIMATION_OFFSET);
  }

  private int getTopButtonsOffsetY() {
    float animProgress = openAnimation.value();
    return (int) ((1.0f - animProgress) * TOP_BUTTON_ANIMATION_OFFSET);
  }

  private int getSearchBoxOffsetY() {
    float animProgress = openAnimation.value();
    return Math.round((1.0f - animProgress) * SEARCH_BOX_ANIMATION_OFFSET * uiScale);
  }

  private void handleOrderAction(MarketItem item, String actionType) {
    switch (actionType) {
      case "buy" -> {
        if (item instanceof SalesOrder salesOrder) {
          // 显示购买确认弹窗
          if (this.minecraft != null) {
            this.minecraft.setScreen(
                new Screen_MarketConfirmDialog(
                    Screen_MarketConfirmDialog.ConfirmType.BUY_SALES, salesOrder, this));
          }
        }
      }
      case "remove" -> {
        if (item instanceof SalesOrder salesOrder) {
          // 显示下架确认弹窗
          if (this.minecraft != null) {
            this.minecraft.setScreen(
                new Screen_MarketConfirmDialog(
                    Screen_MarketConfirmDialog.ConfirmType.REMOVE_SALES, salesOrder, this));
          }
        }
      }
      case "deliver" -> {
        if (item instanceof DemandOrder demandOrder) {
          EconomySystem_NetworkManager.sendToServer(
              new DeliverDemandOrderMessage(demandOrder.getTradeID()));
        }
      }
      case "cancel" -> {
        if (item instanceof DemandOrder demandOrder) {
          // 显示取消确认弹窗
          if (this.minecraft != null) {
            this.minecraft.setScreen(
                new Screen_MarketConfirmDialog(
                    Screen_MarketConfirmDialog.ConfirmType.REMOVE_DEMAND, demandOrder, this));
          }
        }
      }
      case "confirm" -> {
        if (item instanceof DemandOrder demandOrder) {
          EconomySystem_NetworkManager.sendToServer(
              new ConfirmDemandOrderMessage(demandOrder.getTradeID()));
        }
      }
    }
  }

  public void refresh() {
    requestMarketPage();
  }

  public void requestFallbackPage(int totalMatched) {
    int last = totalMatched == 0 ? 0 : ((totalMatched - 1) / PAGE_SIZE) * PAGE_SIZE;
    if (last != serverOffset) requestMarketPage(last);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    int totalPages = getTotalPages();
    if (totalPages > 1) {
      int newPage = currentPage - (int) Math.signum(scrollY);
      newPage = Math.max(0, Math.min(totalPages - 1, newPage));
      if (newPage != currentPage) requestMarketPage(newPage * itemsPerPage);
      return true;
    }
    return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
  public void renderBackground(
      @NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {}
}

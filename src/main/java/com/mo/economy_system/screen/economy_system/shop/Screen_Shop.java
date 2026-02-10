package com.mo.economy_system.screen.economy_system.shop;

import com.mo.economy_system.core.economy_system.shop.ShopItem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.economy_system.Packet_ShopDataRequest;
import com.mo.economy_system.screen.Screen_Home;
import com.mo.economy_system.screen.components.CardRenderer;
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

    // ==================== 商品卡片配置 ====================
    private static final int CARD_WIDTH = 100;
    private static final int CARD_HEIGHT = 80;

    // ==================== 数据 ====================
    private List<ShopItem> items = new ArrayList<>();
    private List<ShopItem> filteredItems = new ArrayList<>();

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

    private record ItemCardArea(int x, int y, int width, int height, int itemIndex) {}

    public Screen_Shop() {
        super(Component.translatable(Util_MessageKeys.SHOP_TITLE_KEY));
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_ShopDataRequest());
    }

    public void updateShopItems(List<ShopItem> items) {
        this.items = items;
        this.filteredItems = new ArrayList<>(items);
    }

    @Override
    protected void init() {
        super.init();
        calculateVirtualSize();

        // 创建搜索框（左上角）
        int searchBoxWidth = 200;
        int searchBoxHeight = 20;
        int searchBoxX = PANEL_PADDING;
        int searchBoxY = 20;

        this.searchBox = new EditBox(this.font, searchBoxX, searchBoxY, searchBoxWidth, searchBoxHeight, Component.translatable("搜索商品..."));
        this.searchBox.setMaxLength(50);
        this.searchBox.setHint(Component.literal("搜索商品..."));
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
                item.getItemStack().getHoverName().getString().toLowerCase().contains(searchText);
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

        // 绘制左下角标题（经济系统）
        drawTitle(guiGraphics);

        // 绘制右下角ESC提示
        drawEscHint(guiGraphics);

        // 绘制搜索框背景（需要恢复坐标系统）
        guiGraphics.pose().popPose();
        renderSearchBoxBackground(guiGraphics);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(uiScale, uiScale, 1.0f);

        // 绘制商品卡片网格
        renderShopItems(guiGraphics, virtualMouseX, virtualMouseY);

        // 绘制翻页控制
        renderPageControls(guiGraphics, virtualMouseX, virtualMouseY);

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
        float virtualMouseY = mouseY / uiScale;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        for (ItemCardArea cardArea : cardAreas) {
            if (virtualMouseX >= cardArea.x() && virtualMouseX <= cardArea.x() + cardArea.width() &&
                virtualMouseY >= cardArea.y() && virtualMouseY <= cardArea.y() + cardArea.height()) {

                ShopItem item = filteredItems.get(cardArea.itemIndex());
                ItemStack itemStack = item.getItemStack();

                List<Component> tooltipLines = itemStack.getTooltipLines(
                        player,
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

                tooltipLines.add(Component.translatable(Util_MessageKeys.SHOP_ITEM_CHANGE_PRICE_KEY, priceChangeText)
                        .withStyle(priceDifference > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
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

        CardRenderer.drawVersionInfo(guiGraphics, font, x, y + font.lineHeight, 120, "🛒 商店");
    }

    private void drawEscHint(GuiGraphics guiGraphics) {
        String hint = "按 ESC 返回";
        int hintWidth = font.width(hint);
        int x = virtualWidth - PANEL_PADDING - hintWidth;
        int y = virtualHeight - PANEL_PADDING - font.lineHeight;

        guiGraphics.drawString(font, hint, x, y, 0x90FFFFFF);
    }

    private void renderShopItems(GuiGraphics guiGraphics, float mouseX, float mouseY) {
        cardAreas.clear();

        if (filteredItems.isEmpty()) {
            String loadingText = "加载中...";
            int textWidth = font.width(loadingText);
            int textX = (virtualWidth - textWidth) / 2;
            int textY = virtualHeight / 2;
            guiGraphics.drawString(font, loadingText, textX, textY, 0x80FFFFFF);
            return;
        }

        // 计算列数（根据虚拟宽度和卡片宽度）
        columns = Math.max(1, (virtualWidth - PANEL_PADDING * 2 + CARD_SPACING) / (CARD_WIDTH + CARD_SPACING));
        itemsPerPage = rows * columns;

        // 计算分页
        int totalPages = (int) Math.ceil((double) filteredItems.size() / itemsPerPage);
        currentPage = Math.min(currentPage, Math.max(0, totalPages - 1));

        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, filteredItems.size());

        // 网格配置（动态列数 x 3行，从左到右排列）
        int totalGridWidth = columns * CARD_WIDTH + (columns - 1) * CARD_SPACING;
        int gridStartX = PANEL_PADDING;  // 左对齐
        int gridStartY = 55;  // 往上移

        for (int i = startIndex; i < endIndex; i++) {
            int indexInPage = i - startIndex;
            int col = indexInPage % columns;  // 从左到右：0, 1, 2, ...
            int row = indexInPage / columns;  // 行：0, 1, 2

            int cardX = gridStartX + col * (CARD_WIDTH + CARD_SPACING);
            int cardY = gridStartY + row * (CARD_HEIGHT + CARD_SPACING);

            ShopItem item = filteredItems.get(i);
            ItemStack itemStack = item.getItemStack();
            String itemName = itemStack.getHoverName().getString();
            int price = item.getCurrentPrice();

            boolean isHovered = (mouseX >= cardX && mouseX <= cardX + CARD_WIDTH &&
                                mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT);

            // 绘制商品卡片（不显示价格变化）
            CardRenderer.drawShopItemCard(guiGraphics, font, cardX, cardY, CARD_WIDTH, CARD_HEIGHT,
                itemStack, itemName, price, 0, isHovered);

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

        // 下一页按钮
        int nextBtnX = pageTextX + pageTextWidth + 12;

        nextBtnX1 = nextBtnX;
        nextBtnY1 = btnY;
        nextBtnX2 = nextBtnX + btnWidth;
        nextBtnY2 = btnY + btnHeight;

        boolean nextHovered = (mouseX >= nextBtnX1 && mouseX <= nextBtnX2 && mouseY >= nextBtnY1 && mouseY <= nextBtnY2);
        drawPageButton(guiGraphics, nextBtnX, btnY, btnWidth, btnHeight, ">", nextHovered, currentPage < totalPages - 1);
    }

    private void drawPageButton(GuiGraphics guiGraphics, int x, int y, int width, int height, String text, boolean isHovered, boolean isEnabled) {
        // 按钮背景（渐变蓝色）
        int bgColor = isEnabled ? (isHovered ? 0xD04A8ACF : 0xB03A7ABF) : 0x602A2A3A;
        int borderColor = isEnabled ? (isHovered ? 0xFF6AB8FF : 0xFF4A8ACF) : 0xFF3A3A4A;
        int textColor = isEnabled ? 0xFFFFFFFF : 0x60808080;

        // 背景
        guiGraphics.fill(x, y, x + width, y + height, bgColor);

        // 边框
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + 1, y + height, borderColor);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);

        // 顶部高光条
        if (isEnabled) {
            guiGraphics.fill(x + 2, y + 1, x + width - 2, y + 2, 0x60FFFFFF);
        }

        // 文字
        int textWidth = font.width(text);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - font.lineHeight) / 2;
        guiGraphics.drawString(font, text, textX, textY, textColor);
    }

    private int getTotalPages() {
        return (int) Math.ceil((double) filteredItems.size() / itemsPerPage);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float virtualMouseX = (float) mouseX / uiScale;
        float virtualMouseY = (float) mouseY / uiScale;

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
}

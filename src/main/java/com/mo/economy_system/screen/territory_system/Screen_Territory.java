package com.mo.economy_system.screen.territory_system;

import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.territory_system.Packet_TeleportToTerritory;
import com.mo.economy_system.network.packets.territory_system.Packet_TerritoryDataRequest;
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
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 领地系统屏幕 - 卡片网格风格
 *
 * 布局：
 * - 左上角：搜索框
 * - 左下角：领地系统标题
 * - 右下角：ESC返回提示
 * - 中间：领地卡片网格
 * - 底部：翻页控制
 */
public class Screen_Territory extends Screen {

    // ==================== 布局常量 ====================
    private static final int BASE_WIDTH = 640;
    private static final int BASE_HEIGHT = 360;
    private static final int CARD_SPACING = 8;
    private static final int PANEL_PADDING = 12;

    // ==================== 领地卡片配置 ====================
    private static final int CARD_WIDTH = 200;
    private static final int CARD_HEIGHT = 120;
    private static final int TOTAL_CARD_HEIGHT = CARD_HEIGHT + CARD_SPACING;

    // ==================== 数据 ====================
    private boolean dataLoaded = false;
    private List<Territory> ownedTerritories = new ArrayList<>();
    private List<Territory> authorizedTerritories = new ArrayList<>();
    private List<Territory> allTerritories = new ArrayList<>();
    private List<Territory> filteredTerritories = new ArrayList<>();

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
    private final List<TerritoryCardArea> cardAreas = new ArrayList<>();

    // ==================== 按钮点击区域 ====================
    private final List<ActionBtnArea> buttonAreas = new ArrayList<>();

    // ==================== 翻页按钮区域 ====================
    private int prevBtnX1, prevBtnY1, prevBtnX2, prevBtnY2;
    private int nextBtnX1, nextBtnY1, nextBtnX2, nextBtnY2;

    private record TerritoryCardArea(int x, int y, int width, int height, int territoryIndex) {}
    private record ActionBtnArea(int x, int y, int width, int height, int territoryIndex, String actionType) {}

    public Screen_Territory() {
        super(Component.translatable(Util_MessageKeys.TERRITORY_TITLE_KEY));
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_TerritoryDataRequest());
    }

    public void updateTerritoryData(List<Territory> owned, List<Territory> authorized) {
        this.dataLoaded = true;
        this.ownedTerritories.clear();
        this.authorizedTerritories.clear();
        this.ownedTerritories.addAll(owned);
        this.authorizedTerritories.addAll(authorized);

        allTerritories = new ArrayList<>();
        allTerritories.addAll(ownedTerritories);
        // 添加有权限但不重复的领地
        for (Territory t : authorizedTerritories) {
            if (!allTerritories.contains(t)) {
                allTerritories.add(t);
            }
        }
        filteredTerritories = new ArrayList<>(allTerritories);
    }

    @Override
    protected void init() {
        super.init();
        calculateVirtualSize();

        // 创建搜索框（左上角）
        int searchBoxWidth = 200;
        int searchBoxX = PANEL_PADDING;
        int searchBoxY = 20;

        this.searchBox = new EditBox(this.font, searchBoxX, searchBoxY, searchBoxWidth, 20, Component.translatable("搜索领地..."));
        this.searchBox.setMaxLength(50);
        this.searchBox.setHint(Component.literal("搜索领地..."));
        this.searchBox.setResponder(this::onSearchChanged);
        this.searchBox.setFocused(false);
        this.addRenderableWidget(this.searchBox);
    }

    private void calculateVirtualSize() {
        float scaleX = (float) this.width / BASE_WIDTH;
        float scaleY = (float) this.height / BASE_HEIGHT;
        uiScale = Math.min(scaleX, scaleY);
        virtualWidth = (int) (this.width / uiScale);
        virtualHeight = (int) (this.height / uiScale);
    }

    private void onSearchChanged(String text) {
        applySearch(text);
    }

    private void applySearch(String searchText) {
        if (searchText.isEmpty()) {
            filteredTerritories = new ArrayList<>(allTerritories);
        } else {
            String lowerSearch = searchText.toLowerCase();
            filteredTerritories = allTerritories.stream()
                .filter(t -> t.getName().toLowerCase().contains(lowerSearch) ||
                           t.getOwnerName().toLowerCase().contains(lowerSearch))
                .collect(ArrayList::new, (list, t) -> list.add(t), (list1, list2) -> {});
        }
        currentPage = 0;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 绘制全屏背景
        renderFullScreenBackground(guiGraphics);

        calculateVirtualSize();

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

        // 绘制领地卡片网格
        renderTerritoryCards(guiGraphics, virtualMouseX, virtualMouseY);

        // 绘制翻页控制
        renderPageControls(guiGraphics, virtualMouseX, virtualMouseY);

        guiGraphics.pose().popPose();

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
            int borderColor = 0xFF9B59B6;

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
        CardRenderer.drawVersionInfo(guiGraphics, font, x, y + font.lineHeight, 140, "🏰 领地系统");
    }

    private void drawEscHint(GuiGraphics guiGraphics) {
        String hint = "按 ESC 返回";
        int hintWidth = font.width(hint);
        int x = virtualWidth - PANEL_PADDING - hintWidth;
        int y = virtualHeight - PANEL_PADDING - font.lineHeight;
        guiGraphics.drawString(font, hint, x, y, 0x90FFFFFF);
    }

    private void renderTerritoryCards(GuiGraphics guiGraphics, float mouseX, float mouseY) {
        cardAreas.clear();
        buttonAreas.clear();

        // 未加载数据时显示加载中
        if (!dataLoaded) {
            String loadingText = "加载中...";
            int textWidth = font.width(loadingText);
            int textX = (virtualWidth - textWidth) / 2;
            int textY = virtualHeight / 2;
            guiGraphics.drawString(font, loadingText, textX, textY, 0x80FFFFFF);
            return;
        }

        // 数据已加载但没有领地时显示提示
        if (filteredTerritories.isEmpty()) {
            String emptyText = "暂无领地";
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
        int totalPages = (int) Math.ceil((double) filteredTerritories.size() / itemsPerPage);
        currentPage = Math.min(currentPage, Math.max(0, totalPages - 1));

        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, filteredTerritories.size());

        // 网格配置
        int gridStartX = PANEL_PADDING;
        int gridStartY = 55;

        // 按钮配置
        int buttonHeight = 14;
        int buttonSpacing = 3;
        int buttonBottomMargin = 5;
        int cardPadding = 6;

        for (int i = startIndex; i < endIndex; i++) {
            int indexInPage = i - startIndex;
            int col = indexInPage % columns;
            int row = indexInPage / columns;

            int cardX = gridStartX + col * (CARD_WIDTH + CARD_SPACING);
            int cardY = gridStartY + row * TOTAL_CARD_HEIGHT;

            Territory territory = filteredTerritories.get(i);
            boolean isOwned = ownedTerritories.contains(territory);
            CardRenderer.TerritoryType type = getTerritoryType(territory);

            // 检查卡片悬停
            boolean cardHovered = (mouseX >= cardX && mouseX <= cardX + CARD_WIDTH &&
                                  mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT);

            // 计算按钮位置
            int buttonY = cardY + CARD_HEIGHT - buttonBottomMargin - buttonHeight;
            boolean teleportHovered = false;
            boolean manageHovered = false;

            if (isOwned) {
                // 两个按钮
                int totalButtonWidth = CARD_WIDTH - cardPadding * 2;
                int singleButtonWidth = (totalButtonWidth - buttonSpacing) / 2;

                int teleportX = cardX + cardPadding;
                int manageX = teleportX + singleButtonWidth + buttonSpacing;

                // 检查按钮悬停
                teleportHovered = (mouseX >= teleportX && mouseX <= teleportX + singleButtonWidth &&
                                  mouseY >= buttonY && mouseY <= buttonY + buttonHeight);
                manageHovered = (mouseX >= manageX && mouseX <= manageX + singleButtonWidth &&
                                mouseY >= buttonY && mouseY <= buttonY + buttonHeight);
            } else {
                // 单按钮
                int buttonWidth = CARD_WIDTH - cardPadding * 2;
                int teleportX = cardX + cardPadding;

                teleportHovered = (mouseX >= teleportX && mouseX <= teleportX + buttonWidth &&
                                  mouseY >= buttonY && mouseY <= buttonY + buttonHeight);
            }

            // 绘制领地卡片（按钮集成在内部）
            // 准备详细信息
            String ownerName = territory.getOwnerName();
            String territoryId = territory.getTerritoryID().toString();
            String coordinateRange = String.format("[%d,%d,%d]→[%d,%d,%d]",
                territory.getPos1().getX(), territory.getPos1().getY(), territory.getPos1().getZ(),
                territory.getPos2().getX(), territory.getPos2().getY(), territory.getPos2().getZ());

            int[] buttonAreas = CardRenderer.drawTerritoryCard(
                guiGraphics, font, cardX, cardY, CARD_WIDTH, CARD_HEIGHT,
                territory.getName(), type, isOwned, cardHovered,
                teleportHovered, manageHovered, isOwned,
                ownerName, territoryId, coordinateRange
            );

            // 存储卡片区域
            this.cardAreas.add(new TerritoryCardArea(cardX, cardY, CARD_WIDTH, CARD_HEIGHT, i));

            // 存储按钮区域（返回值格式：[传送X1,Y1,X2,Y2, 管理X1,Y1,X2,Y2]）
            if (isOwned) {
                // 传送按钮
                this.buttonAreas.add(new ActionBtnArea(buttonAreas[0], buttonAreas[1],
                    buttonAreas[2] - buttonAreas[0], buttonAreas[3] - buttonAreas[1], i, "teleport"));
                // 管理按钮
                this.buttonAreas.add(new ActionBtnArea(buttonAreas[4], buttonAreas[5],
                    buttonAreas[6] - buttonAreas[4], buttonAreas[7] - buttonAreas[5], i, "manage"));
            } else {
                // 只有传送按钮
                this.buttonAreas.add(new ActionBtnArea(buttonAreas[0], buttonAreas[1],
                    buttonAreas[2] - buttonAreas[0], buttonAreas[3] - buttonAreas[1], i, "teleport"));
            }
        }
    }

    private CardRenderer.TerritoryType getTerritoryType(Territory territory) {
        if (ownedTerritories.contains(territory)) {
            // 通过比较 location 来判断维度
            String dim = territory.getDimension().location().toString();
            if (dim.contains("overworld") || dim.contains("主世界")) {
                return CardRenderer.TerritoryType.OVERWORLD;
            } else if (dim.contains("the_nether") || dim.contains("下界")) {
                return CardRenderer.TerritoryType.NETHER;
            } else if (dim.contains("the_end") || dim.contains("末地")) {
                return CardRenderer.TerritoryType.END;
            } else {
                return CardRenderer.TerritoryType.OVERWORLD;
            }
        } else {
            return CardRenderer.TerritoryType.AUTHORIZED;
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

    private int getTotalPages() {
        return (int) Math.ceil((double) filteredTerritories.size() / itemsPerPage);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float virtualMouseX = (float) mouseX / uiScale;
        float virtualMouseY = (float) mouseY / uiScale;

        // 检查操作按钮点击
        for (ActionBtnArea btnArea : buttonAreas) {
            if (virtualMouseX >= btnArea.x() && virtualMouseX <= btnArea.x() + btnArea.width() &&
                virtualMouseY >= btnArea.y() && virtualMouseY <= btnArea.y() + btnArea.height()) {

                Territory territory = filteredTerritories.get(btnArea.territoryIndex());

                if ("teleport".equals(btnArea.actionType())) {
                    EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_TeleportToTerritory(territory.getTerritoryID()));
                } else if ("manage".equals(btnArea.actionType())) {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new Screen_ManageTerritory(territory));
                    }
                }
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
}

package com.mo.economy_system.screen.territory_system;

import net.minecraft.core.registries.BuiltInRegistries;

import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryBuff;
import com.mo.economy_system.core.territory_system.TerritoryBuffConfig;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.territory_system.Packet_SingleTerritoryDataRequest;
import com.mo.economy_system.network.packets.territory_system.Packet_UnlockTerritoryBuff;
import com.mo.economy_system.network.packets.territory_system.Packet_UpgradeTerritoryBuff;
import com.mo.economy_system.screen.components.CardRenderer;
import com.mo.economy_system.screen.components.UiButtonRenderer;
import com.mo.economy_system.screen.components.UiButtonStyle;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 领地增益界面 - 现代卡片风格
 */
public class Screen_TerritoryBuff extends Screen {

    // ==================== 布局常量 ====================
    private static final int BASE_WIDTH = 640;
    private static final int BASE_HEIGHT = 360;
    private static final int CARD_SPACING = 8;
    private static final int PANEL_PADDING = 12;
    private static final int GRID_START_Y = 55;
    private static final int PAGE_HINT_HEIGHT = 45;

    // ==================== 卡片配置 ====================
    private static final int CARD_WIDTH = 200;
    private static final int CARD_HEIGHT = 88;
    private static final int TOTAL_CARD_HEIGHT = CARD_HEIGHT + CARD_SPACING;
    private static final int CARD_PADDING = 8;
    private static final int ACTION_BTN_WIDTH = 70;
    private static final int ACTION_BTN_HEIGHT = 18;
    private static final int ICON_SIZE = 32;

    // ==================== 数据 ====================
    private Territory territory;
    private List<TerritoryBuff> allBuffs = new ArrayList<>();
    private List<TerritoryBuff> filteredBuffs = new ArrayList<>();

    // ==================== 分页 ====================
    private int currentPage = 0;
    private int rows = 3;
    private int columns = -1;
    private int itemsPerPage = -1;

    // ==================== 搜索 ====================
    private EditBox searchBox;

    // ==================== 虚拟坐标系统 ====================
    private float uiScale;
    private int virtualWidth;
    private int virtualHeight;

    // ==================== 点击区域 ====================
    private final List<BuffActionArea> actionAreas = new ArrayList<>();
    private final List<BuffIconArea> iconAreas = new ArrayList<>();
    private final List<BuffCostArea> costAreas = new ArrayList<>();

    // ==================== 翻页按钮区域 ====================
    private int prevBtnX1, prevBtnY1, prevBtnX2, prevBtnY2;
    private int nextBtnX1, nextBtnY1, nextBtnX2, nextBtnY2;

    // ==================== 玩家信息 ====================
    private LocalPlayer player;

    // ==================== 按钮样式 ====================
    private final UiButtonStyle actionUnlockStyle = createActionButtonStyle(CardRenderer.THEME_SHOP);
    private final UiButtonStyle actionUpgradeStyle = createActionButtonStyle(CardRenderer.THEME_TERRITORY);
    private final UiButtonStyle actionMaxStyle = createDisabledStyle();
    private final UiButtonStyle pageButtonStyle = createPageButtonStyle(CardRenderer.THEME_TERRITORY);
    private final UiButtonStyle pageButtonDisabledStyle = createDisabledPageButtonStyle();

    private record BuffActionArea(int x, int y, int width, int height, int buffIndex, String actionType) {}
    private record BuffIconArea(int x, int y, int width, int height, TerritoryBuff buff) {}
    private record BuffCostArea(int x, int y, int width, int height, TerritoryBuff buff) {}

    protected Screen_TerritoryBuff(Territory territory) {
        super(Component.translatable(Util_MessageKeys.TERRITORY_BUFF_TITLE_KEY));
        this.territory = territory;
        if (territory != null) {
            this.allBuffs = new ArrayList<>(territory.getTerritoryBuffs());
            this.filteredBuffs = new ArrayList<>(allBuffs);
        }
    }

    public void updateTerritory(Territory territory) {
        this.territory = territory;
        if (territory != null) {
            this.allBuffs = new ArrayList<>(territory.getTerritoryBuffs());
        } else {
            this.allBuffs = new ArrayList<>();
        }
        String currentSearch = searchBox != null ? searchBox.getValue() : "";
        applySearch(currentSearch);
    }

    @Override
    protected void init() {
        String existingValue = searchBox != null ? searchBox.getValue() : "";
        super.init();
        calculateVirtualSize();

        if (this.minecraft != null) {
            this.player = this.minecraft.player;
        }

        int searchBoxWidth = 200;
        int searchBoxX = PANEL_PADDING;
        int searchBoxY = 20;

        this.searchBox = new EditBox(this.font, searchBoxX, searchBoxY, searchBoxWidth, 20,
            Component.translatable(Util_MessageKeys.TERRITORY_BUFF_TITLE_KEY));
        this.searchBox.setMaxLength(50);
        this.searchBox.setHint(Component.literal("搜索增益..."));
        this.searchBox.setResponder(this::onSearchChanged);
        this.searchBox.setFocused(false);
        this.addRenderableWidget(this.searchBox);
        updateSearchBoxLayout();

        if (!existingValue.isEmpty()) {
            this.searchBox.setValue(existingValue);
            applySearch(existingValue);
        } else {
            applySearch("");
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
        if (searchText == null || searchText.isEmpty()) {
            filteredBuffs = new ArrayList<>(allBuffs);
        } else {
            String lowerSearch = searchText.toLowerCase();
            filteredBuffs = allBuffs.stream()
                .filter(buff -> {
                    String name = buff.getDisplayText() == null ? "" : buff.getDisplayText();
                    String id = buff.getId() == null ? "" : buff.getId();
                    String effect = buff.getEffectId() == null ? "" : buff.getEffectId();
                    return name.toLowerCase().contains(lowerSearch)
                        || id.toLowerCase().contains(lowerSearch)
                        || effect.toLowerCase().contains(lowerSearch);
                })
                .collect(ArrayList::new, (list, b) -> list.add(b), (list1, list2) -> {});
        }
        currentPage = 0;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderFullScreenBackground(guiGraphics);

        calculateVirtualSize();
        updateSearchBoxLayout();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(uiScale, uiScale, 1.0f);

        float virtualMouseX = mouseX / uiScale;
        float virtualMouseY = mouseY / uiScale;

        drawTitle(guiGraphics);
        drawEscHint(guiGraphics);

        guiGraphics.pose().popPose();

        renderSearchBoxBackground(guiGraphics);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(uiScale, uiScale, 1.0f);

        renderBuffCards(guiGraphics, virtualMouseX, virtualMouseY);
        renderPageControls(guiGraphics, virtualMouseX, virtualMouseY);

        guiGraphics.pose().popPose();

        renderBuffTooltips(guiGraphics, mouseX, mouseY);
        renderCostTooltips(guiGraphics, mouseX, mouseY);

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
            int borderColor = CardRenderer.THEME_TERRITORY;

            guiGraphics.fill(boxX - 4, boxY - 2, boxX + boxWidth + 4, boxY + boxHeight + 2, bgColor);
            guiGraphics.fill(boxX - 4, boxY - 2, boxX + boxWidth + 4, boxY - 1, borderColor);
            guiGraphics.fill(boxX - 4, boxY + boxHeight + 1, boxX + boxWidth + 4, boxY + boxHeight + 2, borderColor);
            guiGraphics.fill(boxX - 4, boxY - 2, boxX - 3, boxY + boxHeight + 2, borderColor);
            guiGraphics.fill(boxX + boxWidth + 3, boxY - 2, boxX + boxWidth + 4, boxY + boxHeight + 2, borderColor);
        }
    }

    private void drawTitle(GuiGraphics guiGraphics) {
        int y = virtualHeight - PANEL_PADDING - font.lineHeight;
        String title = "🏰 领地增益";
        String name = territory != null ? territory.getName() : "";
        String text = name.isEmpty() ? title : (title + " · " + name);
        CardRenderer.drawVersionInfo(guiGraphics, font, PANEL_PADDING, y + font.lineHeight, 240, text);
    }

    private void drawEscHint(GuiGraphics guiGraphics) {
        String hint = "按 ESC 返回";
        int hintWidth = font.width(hint);
        int x = virtualWidth - PANEL_PADDING - hintWidth;
        int y = virtualHeight - PANEL_PADDING - font.lineHeight;
        guiGraphics.drawString(font, hint, x, y, 0x90FFFFFF);
    }

    private void renderBuffCards(GuiGraphics guiGraphics, float mouseX, float mouseY) {
        actionAreas.clear();
        iconAreas.clear();
        costAreas.clear();

        if (filteredBuffs.isEmpty()) {
            String emptyText = Component.translatable(Util_MessageKeys.TERRITORY_BUFF_TEXT_NO_BUFFS_TEXT_KEY).getString();
            int textWidth = font.width(emptyText);
            int textX = (virtualWidth - textWidth) / 2;
            int textY = virtualHeight / 2;
            guiGraphics.drawString(font, emptyText, textX, textY, 0x80FFFFFF);
            return;
        }

        columns = Math.max(1, (virtualWidth - PANEL_PADDING * 2 + CARD_SPACING) / (CARD_WIDTH + CARD_SPACING));
        int availableHeight = virtualHeight - GRID_START_Y - PAGE_HINT_HEIGHT;
        rows = Math.max(1, (availableHeight + CARD_SPACING) / (CARD_HEIGHT + CARD_SPACING));
        itemsPerPage = rows * columns;

        int totalPages = getTotalPages();
        currentPage = Math.min(currentPage, Math.max(0, totalPages - 1));

        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, filteredBuffs.size());

        int gridStartX = PANEL_PADDING;

        for (int i = startIndex; i < endIndex; i++) {
            int indexInPage = i - startIndex;
            int col = indexInPage % columns;
            int row = indexInPage / columns;

            int cardX = gridStartX + col * (CARD_WIDTH + CARD_SPACING);
            int cardY = GRID_START_Y + row * TOTAL_CARD_HEIGHT;

            TerritoryBuff buff = filteredBuffs.get(i);
            boolean isHovered = mouseX >= cardX && mouseX <= cardX + CARD_WIDTH &&
                                mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT;

            int themeColor = buff.isUnlocked() ? CardRenderer.THEME_TERRITORY : 0xFF6F7F8C;
            CardRenderer.drawCard(guiGraphics, cardX, cardY, CARD_WIDTH, CARD_HEIGHT, themeColor, isHovered);

            int iconX = cardX + CARD_PADDING;
            int iconY = cardY + (CARD_HEIGHT - ICON_SIZE) / 2;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(2.0f, 2.0f, 1.0f);
            guiGraphics.renderItem(Items.GLASS_BOTTLE.getDefaultInstance(), iconX / 2, iconY / 2);
            guiGraphics.pose().popPose();
            iconAreas.add(new BuffIconArea(iconX, iconY, ICON_SIZE, ICON_SIZE, buff));

            int textX = iconX + ICON_SIZE + 8;
            int textY = cardY + 6;
            int rightReserve = ACTION_BTN_WIDTH + 10;
            int maxTextWidth = CARD_WIDTH - (textX - cardX) - CARD_PADDING - rightReserve;
            if (maxTextWidth < 20) {
                maxTextWidth = CARD_WIDTH - (textX - cardX) - CARD_PADDING;
            }

            String buffName = buff.getDisplayText() == null ? "" : buff.getDisplayText();
            String displayName = CardRenderer.truncateText(font, buffName, maxTextWidth);
            guiGraphics.drawString(font, displayName, textX, textY, CardRenderer.TEXT_TITLE);

            String levelText = "Lv " + buff.getLevel() + "/" + buff.getMaxLevel();
            int levelWidth = font.width(levelText);
            guiGraphics.drawString(font, levelText, cardX + CARD_WIDTH - CARD_PADDING - levelWidth, textY, 0xB0FFFFFF);

            String statusText = buff.isUnlocked() ?
                Component.translatable(Util_MessageKeys.TERRITORY_BUFF_TEXT_BE_UNLOCKED_TEXT_KEY).getString() :
                Component.translatable(Util_MessageKeys.TERRITORY_BUFF_TEXT_BE_LOCKED_TEXT_KEY).getString();
            int statusColor = buff.isUnlocked() ? 0xFF9BE7A7 : 0xFFB0BBC6;
            guiGraphics.drawString(font, statusText, textX, textY + font.lineHeight + 2, statusColor);

            String costLabel = Component.translatable(Util_MessageKeys.TERRITORY_BUFF_COST_LABEL_KEY).getString();
            String displayCost = CardRenderer.truncateText(font, costLabel, maxTextWidth);
            int costY = textY + font.lineHeight * 2 + 4;
            guiGraphics.drawString(font, displayCost, textX, costY, 0xA0FFFFFF);
            costAreas.add(new BuffCostArea(textX, costY, font.width(displayCost), font.lineHeight, buff));

            int actionX = cardX + CARD_PADDING + (CARD_WIDTH - CARD_PADDING * 2 - ACTION_BTN_WIDTH);
            int actionY = cardY + CARD_HEIGHT - CARD_PADDING - ACTION_BTN_HEIGHT;
            boolean actionHovered = mouseX >= actionX && mouseX <= actionX + ACTION_BTN_WIDTH &&
                                    mouseY >= actionY && mouseY <= actionY + ACTION_BTN_HEIGHT;

            String actionType = getActionType(buff);
            drawStripedButton(guiGraphics, actionX, actionY, ACTION_BTN_WIDTH, ACTION_BTN_HEIGHT,
                getActionText(actionType), getActionStyle(actionType), actionHovered);

            actionAreas.add(new BuffActionArea(actionX, actionY, ACTION_BTN_WIDTH, ACTION_BTN_HEIGHT, i, actionType));
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

        boolean prevHovered = mouseX >= prevBtnX1 && mouseX <= prevBtnX2 &&
                              mouseY >= prevBtnY1 && mouseY <= prevBtnY2;
        drawPageButton(guiGraphics, prevBtnX, btnY, btnWidth, btnHeight, "<", prevHovered, currentPage > 0);

        int nextBtnX = pageTextX + pageTextWidth + 12;

        nextBtnX1 = nextBtnX;
        nextBtnY1 = btnY;
        nextBtnX2 = nextBtnX + btnWidth;
        nextBtnY2 = btnY + btnHeight;

        boolean nextHovered = mouseX >= nextBtnX1 && mouseX <= nextBtnX2 &&
                              mouseY >= nextBtnY1 && mouseY <= nextBtnY2;
        drawPageButton(guiGraphics, nextBtnX, btnY, btnWidth, btnHeight, ">", nextHovered, currentPage < totalPages - 1);
    }

    private void drawPageButton(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                String text, boolean isHovered, boolean isEnabled) {
        UiButtonStyle style = isEnabled ? pageButtonStyle : pageButtonDisabledStyle;
        UiButtonRenderer.drawStripedButton(guiGraphics, font, x, y, width, height,
            text, "", style, isEnabled && isHovered, UiButtonRenderer.TextAlign.CENTER, false);
    }

    private void renderBuffTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        float virtualMouseX = (float) mouseX / uiScale;
        float virtualMouseY = (float) mouseY / uiScale;

        for (BuffIconArea iconArea : iconAreas) {
            if (virtualMouseX >= iconArea.x() && virtualMouseX <= iconArea.x() + iconArea.width() &&
                virtualMouseY >= iconArea.y() && virtualMouseY <= iconArea.y() + iconArea.height()) {

                TerritoryBuff buff = iconArea.buff();
                List<Component> tooltipLines = new ArrayList<>();
                String buffId = buff.getId() == null ? "" : buff.getId();
                String buffName = buff.getDisplayText() == null ? "" : buff.getDisplayText();
                String effectId = buff.getEffectId() == null ? "" : buff.getEffectId();
                tooltipLines.add(Component.translatable(Util_MessageKeys.TERRITORY_BUFF_TOOLTIP_BUFF_ID_TEXT_KEY, buffId));
                tooltipLines.add(Component.translatable(Util_MessageKeys.TERRITORY_BUFF_TOOLTIP_BUFF_NAME_TEXT_KEY, buffName));
                tooltipLines.add(Component.translatable(Util_MessageKeys.TERRITORY_BUFF_TOOLTIP_BUFF_CURRENT_LEVEL_TEXT_KEY, buff.getLevel()));
                tooltipLines.add(Component.translatable(Util_MessageKeys.TERRITORY_BUFF_TOOLTIP_BUFF_MAX_LEVEL_TEXT_KEY, buff.getMaxLevel()));
                tooltipLines.add(Component.translatable(Util_MessageKeys.TERRITORY_BUFF_TOOLTIP_BUFF_EFFECT_ID_TEXT_KEY, effectId));
                tooltipLines.add(Component.translatable(Util_MessageKeys.TERRITORY_BUFF_TOOLTIP_BUFF_UNLOCK_STATE_KEY, buff.isUnlocked()));

                guiGraphics.renderTooltip(this.font, tooltipLines, Optional.empty(), mouseX, mouseY);
                break;
            }
        }
    }

    private void renderCostTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        float virtualMouseX = (float) mouseX / uiScale;
        float virtualMouseY = (float) mouseY / uiScale;

        for (BuffCostArea costArea : costAreas) {
            if (virtualMouseX >= costArea.x() && virtualMouseX <= costArea.x() + costArea.width() &&
                virtualMouseY >= costArea.y() && virtualMouseY <= costArea.y() + costArea.height()) {
                List<Component> lines = buildCostTooltipLines(costArea.buff());
                guiGraphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
                break;
            }
        }
    }

    private int getTotalPages() {
        if (filteredBuffs.isEmpty() || itemsPerPage <= 0) return 1;
        return (int) Math.ceil((double) filteredBuffs.size() / itemsPerPage);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float virtualMouseX = (float) mouseX / uiScale;
        float virtualMouseY = (float) mouseY / uiScale;

        for (BuffActionArea actionArea : actionAreas) {
            if (virtualMouseX >= actionArea.x() && virtualMouseX <= actionArea.x() + actionArea.width() &&
                virtualMouseY >= actionArea.y() && virtualMouseY <= actionArea.y() + actionArea.height()) {
                TerritoryBuff buff = filteredBuffs.get(actionArea.buffIndex());
                handleBuffAction(buff, actionArea.actionType());
                return true;
            }
        }

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
        if (keyCode == 256 && this.shouldCloseOnEsc()) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new Screen_ManageTerritory(territory));
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void handleBuffAction(TerritoryBuff buff, String actionType) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        this.player = this.minecraft.player;

        switch (actionType) {
            case "unlock" -> {
                if (canAffordUpgrade(buff, player)) {
                    EconomySystem_NetworkManager.sendToServer(
                        new Packet_UnlockTerritoryBuff(territory.getTerritoryID(), buff.getId()));
                    EconomySystem_NetworkManager.sendToServer(
                        new Packet_SingleTerritoryDataRequest(territory.getTerritoryID()));
                }
            }
            case "upgrade" -> {
                if (canAffordUpgrade(buff, player)) {
                    EconomySystem_NetworkManager.sendToServer(
                        new Packet_UpgradeTerritoryBuff(territory.getTerritoryID(), buff.getId()));
                    EconomySystem_NetworkManager.sendToServer(
                        new Packet_SingleTerritoryDataRequest(territory.getTerritoryID()));
                }
            }
            case "max" -> this.player.sendSystemMessage(
                Component.translatable(Util_MessageKeys.TERRITORY_BUFF_MESSAGE_BUFF_MAX_LEVEL_KEY));
        }
    }

    private String getActionType(TerritoryBuff buff) {
        if (!buff.isUnlocked()) return "unlock";
        if (buff.getLevel() < buff.getMaxLevel()) return "upgrade";
        return "max";
    }

    private String getActionText(String actionType) {
        return switch (actionType) {
            case "unlock" -> Component.translatable(Util_MessageKeys.TERRITORY_BUFF_BUTTON_UNLOCK_KEY).getString();
            case "upgrade" -> Component.translatable(Util_MessageKeys.TERRITORY_BUFF_BUTTON_UPGRADE_KEY).getString();
            case "max" -> Component.translatable(Util_MessageKeys.TERRITORY_BUFF_BUTTON_MAX_KEY).getString();
            default -> "";
        };
    }

    private UiButtonStyle getActionStyle(String actionType) {
        return switch (actionType) {
            case "unlock" -> actionUnlockStyle;
            case "upgrade" -> actionUpgradeStyle;
            case "max" -> actionMaxStyle;
            default -> actionMaxStyle;
        };
    }

    private void drawStripedButton(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                   String text, UiButtonStyle style, boolean hovered) {
        UiButtonRenderer.drawStripedButton(guiGraphics, this.font, x, y, width, height,
            text, "", style, hovered, UiButtonRenderer.TextAlign.CENTER, false);
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

    private UiButtonStyle createDisabledStyle() {
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

    private UiButtonStyle createDisabledPageButtonStyle() {
        return createDisabledStyle();
    }

    private List<Component> buildCostTooltipLines(TerritoryBuff buff) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(Util_MessageKeys.TERRITORY_BUFF_COST_LABEL_KEY));
        List<TerritoryBuffConfig.BuffUpgradeCost> costs = buff.getUpgradeCost();
        if (costs == null || costs.isEmpty()) {
            lines.add(Component.literal("无"));
            return lines;
        }
        Map<String, Integer> itemCounts = new LinkedHashMap<>();
        int xpTotal = 0;
        int coinTotal = 0;

        for (TerritoryBuffConfig.BuffUpgradeCost cost : costs) {
            if (cost == null) {
                continue;
            }
            if (cost.items != null) {
                for (TerritoryBuffConfig.BuffUpgradeCost.ItemRequirement itemCost : cost.items) {
                    if (itemCost == null || itemCost.item == null || itemCost.item.isEmpty() || itemCost.count <= 0) {
                        continue;
                    }
                    String name = resolveItemName(itemCost.item);
                    if (name.isEmpty()) {
                        continue;
                    }
                    itemCounts.merge(name, itemCost.count, Integer::sum);
                }
            }
            if (cost.xp > 0) {
                xpTotal += cost.xp;
            }
            if (cost.df_coin > 0) {
                coinTotal += cost.df_coin;
            }
        }

        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
            lines.add(Component.literal("· " + entry.getKey() + " x" + entry.getValue()));
        }
        if (xpTotal > 0) {
            lines.add(Component.literal("· 经验等级 " + xpTotal));
        }
        if (coinTotal > 0) {
            lines.add(Component.literal("· 梦鱼币 " + coinTotal));
        }
        if (lines.size() == 1) {
            lines.add(Component.literal("无"));
        }
        return lines;
    }

    private String resolveItemName(String itemId) {
        ResourceLocation location = ResourceLocation.tryParse(itemId);
        if (location == null) {
            return itemId;
        }
        var item = BuiltInRegistries.ITEM.get(location);
        if (item == null || item == Items.AIR) {
            return itemId;
        }
        return new ItemStack(item).getHoverName().getString();
    }

    private boolean canAffordUpgrade(TerritoryBuff buff, LocalPlayer player) {
        if (player == null) {
            return false;
        }
        List<TerritoryBuffConfig.BuffUpgradeCost> costs = buff.getUpgradeCost();
        if (costs == null) {
            return true;
        }
        for (TerritoryBuffConfig.BuffUpgradeCost cost : costs) {
            if (cost == null || cost.items == null) {
                continue;
            }
            for (TerritoryBuffConfig.BuffUpgradeCost.ItemRequirement itemCost : cost.items) {
                if (!itemCost.item.isEmpty() && !playerHasItem(player, itemCost.item, itemCost.count)) {
                    player.sendSystemMessage(Component.translatable(
                        Util_MessageKeys.TERRITORY_BUFF_MESSAGE_REQUIREMENT_ITEM_FAIL_KEY));
                    return false;
                }
            }

            if (cost.xp > 0 && player.experienceLevel < cost.xp) {
                player.sendSystemMessage(Component.translatable(
                    Util_MessageKeys.TERRITORY_BUFF_MESSAGE_REQUIREMENT_XP_LEVEL_FAIL_KEY));
                return false;
            }
        }
        return true;
    }

    private boolean playerHasItem(LocalPlayer player, String itemID, int requiredCount) {
        int count = 0;
        for (var stack : player.getInventory().items) {
            if (stack == null || stack.isEmpty()) continue;
            var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (key != null && key.toString().equals(itemID)) {
                count += stack.getCount();
                if (count >= requiredCount) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {}
}

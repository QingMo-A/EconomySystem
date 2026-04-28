package com.mo.economy_system.screen.territory_system;

import com.mo.economy_system.core.territory_system.PlayerInfo;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.territory_system.Packet_ModifyMode;
import com.mo.economy_system.network.packets.territory_system.Packet_RemovePlayer;
import com.mo.economy_system.network.packets.territory_system.Packet_RemoveTerritory;
import com.mo.economy_system.screen.components.CardRenderer;
import com.mo.economy_system.screen.components.UiButtonRenderer;
import com.mo.economy_system.screen.components.UiButtonStyle;
import com.mo.economy_system.utils.Util_MessageKeys;
import com.mo.economy_system.utils.Util_Skull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Screen_ManageTerritory extends Screen {
    // ==================== 布局常量 ====================
    private static final int BASE_WIDTH = 640;
    private static final int BASE_HEIGHT = 360;
    private static final int PANEL_PADDING = 12;
    private static final int CARD_SPACING = 8;
    private static final int LIST_START_Y = 55;
    private static final int PAGE_HINT_HEIGHT = 45;

    // ==================== 列表卡片配置 ====================
    private static final int PLAYER_CARD_HEIGHT = 48;
    private static final int PLAYER_ICON_SIZE = 32;
    private static final int ACTION_PANEL_WIDTH = 180;
    private static final int ACTION_BTN_HEIGHT = 22;
    private static final int ACTION_BTN_SPACING = 6;
    private static final int KICK_BTN_WIDTH = 66;

    // ==================== 数据 ====================
    private final Territory territory;
    private final List<PlayerInfo> authorizedPlayers;

    // ==================== 分页 ====================
    private int currentPage = 0;
    private int rows = 1;
    private int itemsPerPage = 1;

    // ==================== 虚拟坐标系统 ====================
    private float uiScale;
    private int virtualWidth;
    private int virtualHeight;

    // ==================== 点击区域 ====================
    private final List<ButtonArea> actionButtons = new ArrayList<>();
    private final List<ButtonArea> kickButtons = new ArrayList<>();

    // ==================== 翻页按钮区域 ====================
    private int prevBtnX1, prevBtnY1, prevBtnX2, prevBtnY2;
    private int nextBtnX1, nextBtnY1, nextBtnX2, nextBtnY2;

    // ==================== 按钮样式 ====================
    private final UiButtonStyle actionPrimaryStyle;
    private final UiButtonStyle actionWarnStyle;
    private final UiButtonStyle actionNeutralStyle;
    private final UiButtonStyle actionDangerStyle;
    private final UiButtonStyle kickStyle;

    private record ButtonArea(int x, int y, int width, int height, Runnable onClick) {}
    private record ActionEntry(String key, UiButtonStyle style, Runnable onClick) {}

    private final List<ActionEntry> actionEntries = new ArrayList<>();

    public Screen_ManageTerritory(Territory territory) {
        super(Component.literal("管理领地: " + territory.getName()));
        this.territory = territory;
        this.authorizedPlayers = new ArrayList<>(territory.getAuthorizedPlayers());
        this.actionPrimaryStyle = createActionStyle(CardRenderer.THEME_TERRITORY);
        this.actionWarnStyle = createActionStyle(CardRenderer.THEME_SHOP);
        this.actionNeutralStyle = createActionStyle(CardRenderer.THEME_MARKET);
        this.actionDangerStyle = createActionStyle(0xFFE05D5D);
        this.kickStyle = createActionStyle(0xFFE05D5D).setStripeWidth(4);
        initActionEntries();
    }

    @Override
    protected void init() {
        super.init();
        calculateVirtualSize();
    }

    private void calculateVirtualSize() {
        float scaleX = (float) this.width / BASE_WIDTH;
        float scaleY = (float) this.height / BASE_HEIGHT;
        uiScale = Math.min(scaleX, scaleY);
        virtualWidth = (int) (this.width / uiScale);
        virtualHeight = (int) (this.height / uiScale);
    }

    private int getRightPanelWidth() {
        int minLeftWidth = 240;
        int rightWidth = ACTION_PANEL_WIDTH;
        int leftWidth = virtualWidth - PANEL_PADDING * 3 - rightWidth;
        if (leftWidth < minLeftWidth) {
            rightWidth = Math.max(140, virtualWidth - PANEL_PADDING * 3 - minLeftWidth);
        }
        return Math.max(140, rightWidth);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderFullScreenBackground(guiGraphics);

        calculateVirtualSize();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(uiScale, uiScale, 1.0f);

        float virtualMouseX = mouseX / uiScale;
        float virtualMouseY = mouseY / uiScale;

        drawTitle(guiGraphics);
        drawEscHint(guiGraphics);
        renderActionPanel(guiGraphics, virtualMouseX, virtualMouseY);
        renderPlayerList(guiGraphics, virtualMouseX, virtualMouseY);
        renderPageControls(guiGraphics, virtualMouseX, virtualMouseY);

        guiGraphics.pose().popPose();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderFullScreenBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xB0000000);
    }

    private void drawTitle(GuiGraphics guiGraphics) {
        int y = virtualHeight - PANEL_PADDING - font.lineHeight;
        String title = "🏰 领地管理";
        String name = territory.getName();
        String titleText = title + " · " + name;
        CardRenderer.drawVersionInfo(guiGraphics, font, PANEL_PADDING, y + font.lineHeight, 240, titleText);
    }

    private void drawEscHint(GuiGraphics guiGraphics) {
        String hint = "按 ESC 返回";
        int hintWidth = font.width(hint);
        int x = virtualWidth - PANEL_PADDING - hintWidth;
        int y = virtualHeight - PANEL_PADDING - font.lineHeight;
        guiGraphics.drawString(font, hint, x, y, 0x90FFFFFF);
    }

    private void renderActionPanel(GuiGraphics guiGraphics, float mouseX, float mouseY) {
        actionButtons.clear();

        int rightPanelWidth = getRightPanelWidth();
        int leftPanelWidth = virtualWidth - PANEL_PADDING * 3 - rightPanelWidth;
        int panelX = PANEL_PADDING + leftPanelWidth + PANEL_PADDING;
        int panelY = LIST_START_Y;
        int panelInnerX = panelX + 6;
        int panelWidth = rightPanelWidth;

        int contentHeight = actionEntries.size() * ACTION_BTN_HEIGHT + (actionEntries.size() - 1) * ACTION_BTN_SPACING;
        int headerHeight = font.lineHeight + 14;
        int panelHeight = Math.max(120, headerHeight + contentHeight + 8);

        CardRenderer.drawCard(guiGraphics, panelX, panelY, panelWidth, panelHeight, CardRenderer.THEME_TERRITORY, false);

        String header = "管理操作";
        guiGraphics.drawString(font, header, panelX + 8, panelY + 6, CardRenderer.TEXT_TITLE);
        String sub = CardRenderer.truncateText(font, territory.getName(), panelWidth - 16);
        guiGraphics.drawString(font, sub, panelX + 8, panelY + 6 + font.lineHeight, 0x90FFFFFF);

        int btnY = panelY + headerHeight;
        int btnWidth = panelWidth - 12;

        for (ActionEntry entry : actionEntries) {
            boolean hovered = (mouseX >= panelInnerX && mouseX <= panelInnerX + btnWidth &&
                               mouseY >= btnY && mouseY <= btnY + ACTION_BTN_HEIGHT);
            String text = Component.translatable(entry.key()).getString();
            drawStripedButton(guiGraphics, panelInnerX, btnY, btnWidth, ACTION_BTN_HEIGHT, text, entry.style(), hovered);
            actionButtons.add(new ButtonArea(panelInnerX, btnY, btnWidth, ACTION_BTN_HEIGHT, entry.onClick()));
            btnY += ACTION_BTN_HEIGHT + ACTION_BTN_SPACING;
        }
    }

    private void renderPlayerList(GuiGraphics guiGraphics, float mouseX, float mouseY) {
        kickButtons.clear();

        int rightPanelWidth = getRightPanelWidth();
        int listX = PANEL_PADDING;
        int listWidth = virtualWidth - PANEL_PADDING * 3 - rightPanelWidth;
        int listY = LIST_START_Y;

        String header = "授权成员 (" + authorizedPlayers.size() + ")";
        String displayHeader = CardRenderer.truncateText(font, header, listWidth);
        guiGraphics.drawString(font, displayHeader, listX, listY - font.lineHeight - 3, 0xB0FFFFFF);

        if (authorizedPlayers.isEmpty()) {
            String emptyText = "暂无成员";
            int textWidth = font.width(emptyText);
            int textX = listX + (listWidth - textWidth) / 2;
            int textY = listY + (virtualHeight - listY - PAGE_HINT_HEIGHT) / 2;
            guiGraphics.drawString(font, emptyText, textX, textY, 0x80FFFFFF);
            return;
        }

        int availableHeight = virtualHeight - listY - PAGE_HINT_HEIGHT;
        rows = Math.max(1, (availableHeight + CARD_SPACING) / (PLAYER_CARD_HEIGHT + CARD_SPACING));
        itemsPerPage = rows;

        int totalPages = getTotalPages();
        currentPage = Math.min(currentPage, Math.max(0, totalPages - 1));

        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, authorizedPlayers.size());

        for (int i = startIndex; i < endIndex; i++) {
            PlayerInfo playerInfo = authorizedPlayers.get(i);
            int row = i - startIndex;
            int cardY = listY + row * (PLAYER_CARD_HEIGHT + CARD_SPACING);

            boolean cardHovered = (mouseX >= listX && mouseX <= listX + listWidth &&
                                   mouseY >= cardY && mouseY <= cardY + PLAYER_CARD_HEIGHT);
            CardRenderer.drawCard(guiGraphics, listX, cardY, listWidth, PLAYER_CARD_HEIGHT, CardRenderer.THEME_TERRITORY, cardHovered);

            ItemStack head = Util_Skull.createPlayerHead(playerInfo.getUuid(), playerInfo.getName());
            int iconX = listX + 8;
            int iconY = cardY + (PLAYER_CARD_HEIGHT - PLAYER_ICON_SIZE) / 2;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(2.0f, 2.0f, 1.0f);
            guiGraphics.renderItem(head, iconX / 2, iconY / 2);
            guiGraphics.pose().popPose();

            int textX = listX + 8 + PLAYER_ICON_SIZE + 8;
            int textY = cardY + 7;
            guiGraphics.drawString(font, playerInfo.getName(), textX, textY, 0xFFFFFFFF);

            String uuidText = CardRenderer.truncateText(font, playerInfo.getUuid().toString(),
                listWidth - textX - KICK_BTN_WIDTH - 20);
            guiGraphics.drawString(font, uuidText, textX, textY + font.lineHeight + 2, 0x90FFFFFF);

            int kickX = listX + listWidth - KICK_BTN_WIDTH - 8;
            int kickY = cardY + (PLAYER_CARD_HEIGHT - ACTION_BTN_HEIGHT) / 2;
            boolean kickHovered = (mouseX >= kickX && mouseX <= kickX + KICK_BTN_WIDTH &&
                                   mouseY >= kickY && mouseY <= kickY + ACTION_BTN_HEIGHT);
            drawStripedButton(guiGraphics, kickX, kickY, KICK_BTN_WIDTH, ACTION_BTN_HEIGHT,
                Component.translatable(Util_MessageKeys.TERRITORY_MANAGEMENT_KICK_PLAYER).getString(), kickStyle, kickHovered);

            UUID targetUuid = playerInfo.getUuid();
            kickButtons.add(new ButtonArea(kickX, kickY, KICK_BTN_WIDTH, ACTION_BTN_HEIGHT, () -> {
                EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_RemovePlayer(territory.getTerritoryID(), targetUuid));
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new Screen_Territory());
                }
            }));
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
        if (authorizedPlayers.isEmpty()) return 1;
        return (int) Math.ceil((double) authorizedPlayers.size() / itemsPerPage);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float virtualMouseX = (float) mouseX / uiScale;
        float virtualMouseY = (float) mouseY / uiScale;

        for (ButtonArea actionArea : actionButtons) {
            if (virtualMouseX >= actionArea.x() && virtualMouseX <= actionArea.x() + actionArea.width() &&
                virtualMouseY >= actionArea.y() && virtualMouseY <= actionArea.y() + actionArea.height()) {
                actionArea.onClick().run();
                return true;
            }
        }

        for (ButtonArea kickArea : kickButtons) {
            if (virtualMouseX >= kickArea.x() && virtualMouseX <= kickArea.x() + kickArea.width() &&
                virtualMouseY >= kickArea.y() && virtualMouseY <= kickArea.y() + kickArea.height()) {
                kickArea.onClick().run();
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
                this.minecraft.setScreen(new Screen_Territory());
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawStripedButton(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                   String text, UiButtonStyle style, boolean hovered) {
        UiButtonRenderer.drawStripedButton(guiGraphics, this.font, x, y, width, height,
            text, "", style, hovered, UiButtonRenderer.TextAlign.CENTER, false);
    }

    private UiButtonStyle createActionStyle(int accentColor) {
        return UiButtonStyle.accent(accentColor)
            .setPadding(8)
            .setStripeWidth(4)
            .setGlowHeight(6)
            .setBgAlpha(0x55)
            .setBgAlphaHover(0x70)
            .setBorderAlpha(0x25)
            .setBorderAlphaHover(0x40)
            .setTextShadow(false);
    }

    private void initActionEntries() {
        actionEntries.add(new ActionEntry(Util_MessageKeys.TERRITORY_MANAGEMENT_COPY_ID, actionNeutralStyle, () -> {
            if (this.minecraft == null || this.minecraft.player == null) {
                return;
            }
            GLFW.glfwSetClipboardString(Minecraft.getInstance().getWindow().getWindow(), territory.getTerritoryID().toString());
            this.minecraft.player.sendSystemMessage(Component.translatable(Util_MessageKeys.TERRITORY_MANAGEMENT_COPY_SUCCESS));
        }));

        actionEntries.add(new ActionEntry(Util_MessageKeys.TERRITORY_MANAGEMENT_RESIZE_TERRITORY, actionPrimaryStyle, () -> {
            EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_ModifyMode(territory.getTerritoryID()));
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
        }));

        actionEntries.add(new ActionEntry(Util_MessageKeys.TERRITORY_MANAGEMENT_INVITE_PLAYER, actionPrimaryStyle, () ->
            Minecraft.getInstance().setScreen(new Screen_InvitePlayer(territory))));

        actionEntries.add(new ActionEntry(Util_MessageKeys.TERRITORY_MANAGEMENT_BUFF, actionWarnStyle, () ->
            Minecraft.getInstance().setScreen(new Screen_TerritoryBuff(territory))));

        actionEntries.add(new ActionEntry(Util_MessageKeys.TERRITORY_MANAGEMENT_PERMISSIONS, actionNeutralStyle, () -> {
        }));

        actionEntries.add(new ActionEntry(Util_MessageKeys.TERRITORY_MANAGEMENT_TRANSFER_OWNERSHIP, actionNeutralStyle, () -> {
        }));

        actionEntries.add(new ActionEntry(Util_MessageKeys.TERRITORY_MANAGEMENT_DELETE_TERRITORY, actionDangerStyle, () -> {
            EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_RemoveTerritory(territory.getTerritoryID()));
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
        }));
    }
}


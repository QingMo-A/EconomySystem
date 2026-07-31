package com.mo.economy_system.screen.territory_system;

import com.mo.economy_system.core.territory_system.PlayerInfo;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryPermissionAction;
import com.mo.economy_system.core.territory_system.TerritoryPermissionLevel;
import com.mo.economy_system.common.network.ServerPlayerListRequestMessage;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.territory_system.Packet_TransferTerritoryOwnership;
import com.mo.economy_system.network.packets.territory_system.Packet_UpdateTerritoryRule;
import com.mo.economy_system.screen.components.CardRenderer;
import com.mo.economy_system.screen.components.HighLevelTextField;
import com.mo.economy_system.screen.components.UiButtonRenderer;
import com.mo.economy_system.screen.components.UiButtonStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Screen_TerritoryPlayerAction extends Screen {
    public enum Mode {
        PERMISSION,
        TRANSFER
    }

    private static final int BASE_WIDTH = 640;
    private static final int BASE_HEIGHT = 360;
    private static final int PANEL_PADDING = 12;
    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 210;
    private static final int INPUT_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 22;
    private static final int BUTTON_SPACING = 8;

    private final Territory territory;
    private final Mode mode;
    private final List<Map.Entry<UUID, String>> players = new ArrayList<>();
    private final List<String> names = new ArrayList<>();

    private HighLevelTextField playerNameField;
    private float uiScale;
    private int virtualWidth;
    private int virtualHeight;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int inputX;
    private int inputY;
    private int inputWidth;
    private int inputHeight;
    private int primaryBtnX1, primaryBtnY1, primaryBtnX2, primaryBtnY2;
    private int secondaryBtnX1, secondaryBtnY1, secondaryBtnX2, secondaryBtnY2;
    private int backBtnX1, backBtnY1, backBtnX2, backBtnY2;

    private final UiButtonStyle primaryStyle = createButtonStyle(CardRenderer.THEME_TERRITORY);
    private final UiButtonStyle secondaryStyle = createButtonStyle(CardRenderer.THEME_MARKET);
    private final UiButtonStyle backStyle = createButtonStyle(CardRenderer.THEME_ABOUT);
    private final List<RuleButtonArea> ruleButtons = new ArrayList<>();

    private record RuleButtonArea(int x, int y, int width, int height, TerritoryPermissionAction action) {}

    public Screen_TerritoryPlayerAction(Territory territory, Mode mode) {
        super(Component.literal(mode == Mode.TRANSFER ? "转让领地" : "领地权限"));
        this.territory = territory;
        this.mode = mode;
        if (mode == Mode.TRANSFER) {
            EconomySystem_NetworkManager.sendToServer(ServerPlayerListRequestMessage.INSTANCE);
        }
    }

    @Override
    protected void init() {
        String existingValue = playerNameField != null ? playerNameField.getValue() : "";
        super.init();
        calculateVirtualSize();
        updateLayout();
        playerNameField = null;
        if (mode == Mode.TRANSFER) {
            initInputField();
        }
        if (!existingValue.isEmpty() && playerNameField != null) {
            playerNameField.setValue(existingValue);
        }
    }

    private void calculateVirtualSize() {
        float scaleX = (float) this.width / BASE_WIDTH;
        float scaleY = (float) this.height / BASE_HEIGHT;
        uiScale = Math.min(scaleX, scaleY);
        virtualWidth = (int) (this.width / uiScale);
        virtualHeight = (int) (this.height / uiScale);
    }

    private void updateLayout() {
        panelWidth = Math.min(PANEL_WIDTH, virtualWidth - PANEL_PADDING * 2);
        panelHeight = Math.min(PANEL_HEIGHT, virtualHeight - PANEL_PADDING * 2);
        panelX = (virtualWidth - panelWidth) / 2;
        panelY = (virtualHeight - panelHeight) / 2;

        inputWidth = Math.min(250, panelWidth - 24);
        inputHeight = INPUT_HEIGHT;
        inputX = panelX + (panelWidth - inputWidth) / 2;
        inputY = panelY + 90;

        if (mode == Mode.PERMISSION) {
            primaryBtnX1 = primaryBtnY1 = primaryBtnX2 = primaryBtnY2 = -1;
            secondaryBtnX1 = secondaryBtnY1 = secondaryBtnX2 = secondaryBtnY2 = -1;
            backBtnX1 = panelX + (panelWidth - 104) / 2;
            backBtnY1 = panelY + panelHeight - 34;
            backBtnX2 = backBtnX1 + 104;
            backBtnY2 = backBtnY1 + BUTTON_HEIGHT;
            return;
        }

        int buttonCount = mode == Mode.PERMISSION ? 3 : 2;
        int buttonWidth = Math.max(76, Math.min(104, (panelWidth - PANEL_PADDING * 2 - BUTTON_SPACING * (buttonCount - 1)) / buttonCount));
        int totalWidth = buttonWidth * buttonCount + BUTTON_SPACING * (buttonCount - 1);
        int buttonX = panelX + (panelWidth - totalWidth) / 2;
        int buttonY = panelY + panelHeight - 38;

        primaryBtnX1 = buttonX;
        primaryBtnY1 = buttonY;
        primaryBtnX2 = primaryBtnX1 + buttonWidth;
        primaryBtnY2 = buttonY + BUTTON_HEIGHT;

        secondaryBtnX1 = primaryBtnX2 + BUTTON_SPACING;
        secondaryBtnY1 = buttonY;
        secondaryBtnX2 = secondaryBtnX1 + buttonWidth;
        secondaryBtnY2 = buttonY + BUTTON_HEIGHT;

        backBtnX1 = (mode == Mode.PERMISSION ? secondaryBtnX2 : primaryBtnX2) + BUTTON_SPACING;
        backBtnY1 = buttonY;
        backBtnX2 = backBtnX1 + buttonWidth;
        backBtnY2 = buttonY + BUTTON_HEIGHT;
    }

    private void initInputField() {
        int boxX = Math.round(inputX * uiScale);
        int boxY = Math.round(inputY * uiScale);
        int boxWidth = Math.round(inputWidth * uiScale);
        int boxHeight = Math.round(inputHeight * uiScale);
        playerNameField = new HighLevelTextField(this.font, boxX, boxY, boxWidth, boxHeight, Component.literal("输入玩家名称"));
        playerNameField.setHint(Component.literal("输入玩家名称"));
        playerNameField.setSuggestions(names);
        this.addRenderableWidget(playerNameField);
    }

    public void update(List<Map.Entry<UUID, String>> accounts) {
        players.clear();
        players.addAll(accounts);
        names.clear();
        for (Map.Entry<UUID, String> entry : accounts) {
            if (!entry.getKey().equals(territory.getOwnerUUID())) {
                names.add(entry.getValue());
            }
        }
        if (playerNameField != null) {
            playerNameField.setSuggestions(names);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderFullScreenBackground(guiGraphics);
        calculateVirtualSize();
        updateLayout();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(uiScale, uiScale, 1.0f);
        float virtualMouseX = mouseX / uiScale;
        float virtualMouseY = mouseY / uiScale;
        renderPanel(guiGraphics, virtualMouseX, virtualMouseY);
        drawEscHint(guiGraphics);
        guiGraphics.pose().popPose();

        if (mode == Mode.TRANSFER) {
            renderInputBackground(guiGraphics);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderFullScreenBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xB0000000);
    }

    private void renderPanel(GuiGraphics guiGraphics, float mouseX, float mouseY) {
        CardRenderer.drawCard(guiGraphics, panelX, panelY, panelWidth, panelHeight, CardRenderer.THEME_TERRITORY, false);
        int textX = panelX + PANEL_PADDING;
        int titleY = panelY + 8;
        String title = mode == Mode.TRANSFER ? "转让领地" : "领地权限";
        guiGraphics.drawString(font, title, textX, titleY, CardRenderer.TEXT_TITLE);
        guiGraphics.drawString(font, "领地: " + CardRenderer.truncateText(font, territory.getName(), panelWidth - 60),
            textX, titleY + font.lineHeight + 2, 0x90FFFFFF);

        String hint = mode == Mode.TRANSFER
            ? "选择新领主。转让后你会变为授权成员。"
            : "每项规则可设置为仅领主、所有成员或所有人。";
        guiGraphics.drawString(font, CardRenderer.truncateText(font, hint, panelWidth - PANEL_PADDING * 2),
            textX, titleY + font.lineHeight * 2 + 8, CardRenderer.TEXT_DESC);

        if (mode == Mode.PERMISSION) {
            renderPermissionRules(guiGraphics, mouseX, mouseY);
            boolean backHovered = isInside(mouseX, mouseY, backBtnX1, backBtnY1, backBtnX2, backBtnY2);
            drawStripedButton(guiGraphics, backBtnX1, backBtnY1, backBtnX2 - backBtnX1, BUTTON_HEIGHT,
                "返回", backStyle, backHovered);
            return;
        }

        String label = "玩家名称";
        int labelWidth = font.width(label);
        guiGraphics.drawString(font, label, inputX + (inputWidth - labelWidth) / 2,
            inputY - font.lineHeight - 2, CardRenderer.TEXT_DESC);

        boolean primaryHovered = isInside(mouseX, mouseY, primaryBtnX1, primaryBtnY1, primaryBtnX2, primaryBtnY2);
        boolean secondaryHovered = isInside(mouseX, mouseY, secondaryBtnX1, secondaryBtnY1, secondaryBtnX2, secondaryBtnY2);
        boolean backHovered = isInside(mouseX, mouseY, backBtnX1, backBtnY1, backBtnX2, backBtnY2);

        drawStripedButton(guiGraphics, primaryBtnX1, primaryBtnY1, primaryBtnX2 - primaryBtnX1, BUTTON_HEIGHT,
            mode == Mode.TRANSFER ? "转让" : "添加", primaryStyle, primaryHovered);
        if (mode == Mode.PERMISSION) {
            drawStripedButton(guiGraphics, secondaryBtnX1, secondaryBtnY1, secondaryBtnX2 - secondaryBtnX1, BUTTON_HEIGHT,
                "移除", secondaryStyle, secondaryHovered);
        }
        drawStripedButton(guiGraphics, backBtnX1, backBtnY1, backBtnX2 - backBtnX1, BUTTON_HEIGHT,
            "返回", backStyle, backHovered);
    }

    private void renderPermissionRules(GuiGraphics guiGraphics, float mouseX, float mouseY) {
        ruleButtons.clear();
        int rowX = panelX + PANEL_PADDING;
        int rowY = panelY + 66;
        int rowWidth = panelWidth - PANEL_PADDING * 2;
        int rowHeight = 21;
        int buttonWidth = 76;
        for (TerritoryPermissionAction action : TerritoryPermissionAction.values()) {
            TerritoryPermissionLevel level = territory.getPermissionLevel(action);
            boolean hovered = isInside(mouseX, mouseY, rowX, rowY, rowX + rowWidth, rowY + rowHeight);
            CardRenderer.drawCard(guiGraphics, rowX, rowY, rowWidth, rowHeight, CardRenderer.THEME_TERRITORY, hovered);
            guiGraphics.drawString(font, action.getDisplayName(), rowX + 8, rowY + 6, CardRenderer.TEXT_TITLE);

            int btnX = rowX + rowWidth - buttonWidth - 6;
            int btnY = rowY + 3;
            boolean btnHovered = isInside(mouseX, mouseY, btnX, btnY, btnX + buttonWidth, btnY + 16);
            drawStripedButton(guiGraphics, btnX, btnY, buttonWidth, 16, level.getDisplayName(), secondaryStyle, btnHovered);
            ruleButtons.add(new RuleButtonArea(btnX, btnY, buttonWidth, 16, action));
            rowY += rowHeight + 5;
        }
    }

    private void renderInputBackground(GuiGraphics guiGraphics) {
        if (playerNameField == null) {
            return;
        }
        int boxX = playerNameField.getX();
        int boxY = playerNameField.getY();
        int boxWidth = playerNameField.getWidth();
        int boxHeight = playerNameField.getHeight();
        int bgColor = 0xE04A5568;
        int borderColor = CardRenderer.THEME_TERRITORY;
        guiGraphics.fill(boxX - 4, boxY - 2, boxX + boxWidth + 4, boxY + boxHeight + 2, bgColor);
        guiGraphics.fill(boxX - 4, boxY - 2, boxX + boxWidth + 4, boxY - 1, borderColor);
        guiGraphics.fill(boxX - 4, boxY + boxHeight + 1, boxX + boxWidth + 4, boxY + boxHeight + 2, borderColor);
        guiGraphics.fill(boxX - 4, boxY - 2, boxX - 3, boxY + boxHeight + 2, borderColor);
        guiGraphics.fill(boxX + boxWidth + 3, boxY - 2, boxX + boxWidth + 4, boxY + boxHeight + 2, borderColor);
    }

    private void drawEscHint(GuiGraphics guiGraphics) {
        String hint = "按 ESC 返回";
        int hintWidth = font.width(hint);
        guiGraphics.drawString(font, hint, virtualWidth - PANEL_PADDING - hintWidth,
            virtualHeight - PANEL_PADDING - font.lineHeight, 0x90FFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float virtualMouseX = (float) mouseX / uiScale;
        float virtualMouseY = (float) mouseY / uiScale;
        if (isInside(virtualMouseX, virtualMouseY, primaryBtnX1, primaryBtnY1, primaryBtnX2, primaryBtnY2)) {
            if (mode == Mode.PERMISSION) {
                return true;
            }
            submitTransfer();
            return true;
        }
        if (mode == Mode.PERMISSION) {
            for (RuleButtonArea area : ruleButtons) {
                if (isInside(virtualMouseX, virtualMouseY, area.x(), area.y(), area.x() + area.width(), area.y() + area.height())) {
                    cycleRule(area.action());
                    return true;
                }
            }
        }
        if (isInside(virtualMouseX, virtualMouseY, backBtnX1, backBtnY1, backBtnX2, backBtnY2)) {
            backToManage();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void submitTransfer() {
        if (this.minecraft == null || this.minecraft.player == null || playerNameField == null) {
            return;
        }
        String name = playerNameField.getValue().trim();
        Map.Entry<UUID, String> target = findPlayer(name);
        if (target == null) {
            this.minecraft.player.sendSystemMessage(Component.literal("请选择有效玩家。"));
            return;
        }
        EconomySystem_NetworkManager.sendToServer(new Packet_TransferTerritoryOwnership(territory.getTerritoryID(), target.getKey(), target.getValue()));
        this.minecraft.setScreen(new Screen_Territory());
    }

    private void cycleRule(TerritoryPermissionAction action) {
        TerritoryPermissionLevel next = territory.getPermissionLevel(action).next();
        territory.setPermissionLevel(action, next);
        EconomySystem_NetworkManager.sendToServer(new Packet_UpdateTerritoryRule(territory.getTerritoryID(), action, next));
    }

    private Map.Entry<UUID, String> findPlayer(String name) {
        for (Map.Entry<UUID, String> entry : players) {
            if (entry.getValue().equalsIgnoreCase(name)) {
                return entry;
            }
        }
        for (PlayerInfo playerInfo : territory.getAuthorizedPlayers()) {
            if (playerInfo.getName().equalsIgnoreCase(name)) {
                return Map.entry(playerInfo.getUuid(), playerInfo.getName());
            }
        }
        return null;
    }

    private boolean isInside(float mouseX, float mouseY, int x1, int y1, int x2, int y2) {
        return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
    }

    private void backToManage() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new Screen_ManageTerritory(territory));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && this.shouldCloseOnEsc()) {
            backToManage();
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

    private static UiButtonStyle createButtonStyle(int accentColor) {
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

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {}
}

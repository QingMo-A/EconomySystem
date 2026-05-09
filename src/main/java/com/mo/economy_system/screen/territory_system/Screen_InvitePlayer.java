package com.mo.economy_system.screen.territory_system;

import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.Packet_ServerPlayerListRequest;
import com.mo.economy_system.network.packets.territory_system.Packet_InvitePlayer;
import com.mo.economy_system.screen.components.CardRenderer;
import com.mo.economy_system.screen.components.HighLevelTextField;
import com.mo.economy_system.screen.components.UiButtonRenderer;
import com.mo.economy_system.screen.components.UiButtonStyle;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Screen_InvitePlayer extends Screen {
    // ==================== 布局常量 ====================
    private static final int BASE_WIDTH = 640;
    private static final int BASE_HEIGHT = 360;
    private static final int PANEL_PADDING = 12;
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 190;
    private static final int INPUT_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 22;
    private static final int BUTTON_WIDTH = 110;
    private static final int BUTTON_SPACING = 8;

    private final Territory territory;
    private final List<UUID> uuids = new ArrayList<>();
    private final List<String> names = new ArrayList<>();

    private HighLevelTextField playerNameField;

    // ==================== 虚拟坐标系统 ====================
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
    private int inviteBtnX1, inviteBtnY1, inviteBtnX2, inviteBtnY2;
    private int backBtnX1, backBtnY1, backBtnX2, backBtnY2;

    private final UiButtonStyle inviteStyle;
    private final UiButtonStyle backStyle;

    public Screen_InvitePlayer(Territory territory) {
        super(Component.translatable(Util_MessageKeys.INVITE_TITLE_KEY));
        this.territory = territory;
        EconomySystem_NetworkManager.sendToServer(new Packet_ServerPlayerListRequest());
        inviteStyle = createButtonStyle(CardRenderer.THEME_TERRITORY);
        backStyle = createButtonStyle(CardRenderer.THEME_ABOUT);
    }

    @Override
    protected void init() {
        String existingValue = playerNameField != null ? playerNameField.getValue() : "";
        super.init();
        calculateVirtualSize();
        updateLayout();
        playerNameField = null;
        initInputField();
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

        inputWidth = Math.min(240, panelWidth - 24);
        inputHeight = INPUT_HEIGHT;
        inputX = panelX + (panelWidth - inputWidth) / 2;
        inputY = panelY + 86;

        int maxButtonWidth = (panelWidth - PANEL_PADDING * 2 - BUTTON_SPACING) / 2;
        int buttonWidth = Math.min(BUTTON_WIDTH, Math.max(90, maxButtonWidth));
        int buttonY = panelY + panelHeight - 38;
        int totalWidth = buttonWidth * 2 + BUTTON_SPACING;
        int buttonStartX = panelX + (panelWidth - totalWidth) / 2;

        inviteBtnX1 = buttonStartX;
        inviteBtnY1 = buttonY;
        inviteBtnX2 = inviteBtnX1 + buttonWidth;
        inviteBtnY2 = inviteBtnY1 + BUTTON_HEIGHT;

        backBtnX1 = inviteBtnX2 + BUTTON_SPACING;
        backBtnY1 = buttonY;
        backBtnX2 = backBtnX1 + buttonWidth;
        backBtnY2 = backBtnY1 + BUTTON_HEIGHT;
    }

    private void initInputField() {
        int boxX = Math.round(inputX * uiScale);
        int boxY = Math.round(inputY * uiScale);
        int boxWidth = Math.round(inputWidth * uiScale);
        int boxHeight = Math.round(inputHeight * uiScale);

        if (playerNameField == null) {
            playerNameField = new HighLevelTextField(this.font, boxX, boxY, boxWidth, boxHeight, Component.literal("输入玩家名称"));
            playerNameField.setHint(Component.literal("输入玩家名称"));
            playerNameField.setSuggestions(names);
            this.addRenderableWidget(playerNameField);
        } else {
            playerNameField.setX(boxX);
            playerNameField.setY(boxY);
            playerNameField.setWidth(boxWidth);
            playerNameField.setHeight(boxHeight);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderFullScreenBackground(guiGraphics);

        calculateVirtualSize();
        updateLayout();
        initInputField();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(uiScale, uiScale, 1.0f);

        float virtualMouseX = mouseX / uiScale;
        float virtualMouseY = mouseY / uiScale;

        renderPanel(guiGraphics, virtualMouseX, virtualMouseY);
        drawEscHint(guiGraphics);

        guiGraphics.pose().popPose();

        renderInputBackground(guiGraphics);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderFullScreenBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xB0000000);
    }

    private void renderPanel(GuiGraphics guiGraphics, float mouseX, float mouseY) {
        CardRenderer.drawCard(guiGraphics, panelX, panelY, panelWidth, panelHeight, CardRenderer.THEME_TERRITORY, false);

        int textX = panelX + PANEL_PADDING;
        int titleY = panelY + 8;
        String titleText = Component.translatable(Util_MessageKeys.INVITE_TITLE_KEY).getString();
        guiGraphics.drawString(font, titleText, textX, titleY, CardRenderer.TEXT_TITLE);

        String subText = "领地: " + territory.getName();
        String subDisplay = CardRenderer.truncateText(font, subText, panelWidth - PANEL_PADDING * 2);
        guiGraphics.drawString(font, subDisplay, textX, titleY + font.lineHeight + 2, 0x90FFFFFF);

        String label = "玩家名称";
        int labelWidth = font.width(label);
        int labelX = inputX + (inputWidth - labelWidth) / 2;
        guiGraphics.drawString(font, label, labelX, inputY - font.lineHeight - 2, CardRenderer.TEXT_DESC);

        boolean inviteHovered = mouseX >= inviteBtnX1 && mouseX <= inviteBtnX2 &&
                                mouseY >= inviteBtnY1 && mouseY <= inviteBtnY2;
        boolean backHovered = mouseX >= backBtnX1 && mouseX <= backBtnX2 &&
                              mouseY >= backBtnY1 && mouseY <= backBtnY2;

        drawStripedButton(guiGraphics, inviteBtnX1, inviteBtnY1, inviteBtnX2 - inviteBtnX1, BUTTON_HEIGHT,
            Component.translatable(Util_MessageKeys.INVITE_INVITE_BUTTON_KEY).getString(), inviteStyle, inviteHovered);
        drawStripedButton(guiGraphics, backBtnX1, backBtnY1, backBtnX2 - backBtnX1, BUTTON_HEIGHT,
            Component.translatable(Util_MessageKeys.INVITE_BACK_BUTTON).getString(), backStyle, backHovered);
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
        int x = virtualWidth - PANEL_PADDING - hintWidth;
        int y = virtualHeight - PANEL_PADDING - font.lineHeight;
        guiGraphics.drawString(font, hint, x, y, 0x90FFFFFF);
    }

    public void update(List<Map.Entry<UUID, String>> accounts) {
        names.clear();
        uuids.clear();
        for (Map.Entry<UUID, String> entry : accounts) {
            uuids.add(entry.getKey());
            names.add(entry.getValue());
        }
        if (playerNameField != null) {
            playerNameField.setSuggestions(names);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float virtualMouseX = (float) mouseX / uiScale;
        float virtualMouseY = (float) mouseY / uiScale;

        if (virtualMouseX >= inviteBtnX1 && virtualMouseX <= inviteBtnX2 &&
            virtualMouseY >= inviteBtnY1 && virtualMouseY <= inviteBtnY2) {
            onInvite();
            return true;
        }

        if (virtualMouseX >= backBtnX1 && virtualMouseX <= backBtnX2 &&
            virtualMouseY >= backBtnY1 && virtualMouseY <= backBtnY2) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new Screen_ManageTerritory(territory));
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void onInvite() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        String playerName = playerNameField == null ? "" : playerNameField.getValue();
        if (!playerName.isEmpty()) {
            EconomySystem_NetworkManager.sendToServer(
                new Packet_InvitePlayer(territory.getTerritoryID(), playerName));
            this.minecraft.setScreen(null);
        } else {
            this.minecraft.player.sendSystemMessage(Component.translatable(Util_MessageKeys.INVITE_NO_NAME_KEY));
        }
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

    private void drawStripedButton(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                   String text, UiButtonStyle style, boolean hovered) {
        UiButtonRenderer.drawStripedButton(guiGraphics, this.font, x, y, width, height,
            text, "", style, hovered, UiButtonRenderer.TextAlign.CENTER, false);
    }

    private UiButtonStyle createButtonStyle(int accentColor) {
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




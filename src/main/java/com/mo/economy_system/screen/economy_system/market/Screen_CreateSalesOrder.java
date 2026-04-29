package com.mo.economy_system.screen.economy_system.market;

import com.mo.economy_system.core.economy_system.market.SalesOrder;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.economy_system.sales_order.Packet_CreateSalesOrder;
import com.mo.economy_system.screen.components.CardRenderer;
import com.mo.economy_system.screen.components.UiButtonRenderer;
import com.mo.economy_system.screen.components.UiButtonStyle;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public class Screen_CreateSalesOrder extends Screen {

    private static final int BASE_WIDTH = 640;
    private static final int BASE_HEIGHT = 360;
    private static final int PANEL_PADDING = 12;
    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 210;
    private static final int ITEM_CARD_HEIGHT = 58;
    private static final int INPUT_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 22;
    private static final int BUTTON_WIDTH = 110;
    private static final int ICON_SIZE = 32;

    private final Player player;
    private EditBox priceInput;

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
    private int listBtnX1, listBtnY1, listBtnX2, listBtnY2;

    private final UiButtonStyle listStyle;
    private final UiButtonStyle disabledStyle;

    public Screen_CreateSalesOrder(Player player) {
        super(Component.translatable(Util_MessageKeys.LIST_TITLE_KEY));
        this.player = player;
        this.listStyle = createButtonStyle(CardRenderer.THEME_MARKET);
        this.disabledStyle = createButtonStyle(0xFF6F7F8C)
            .setTextColor(0xFFB0BBC6)
            .setBgAlpha(0x30)
            .setBgAlphaHover(0x30)
            .setStripeAlpha(0x50)
            .setStripeAlphaHover(0x50)
            .setGlowHeight(0)
            .setBorderAlpha(0x20)
            .setBorderAlphaHover(0x20);
    }

    @Override
    protected void init() {
        String existingValue = priceInput != null ? priceInput.getValue() : "";
        super.init();
        calculateVirtualSize();
        updateLayout();
        priceInput = null;
        initPriceInput();
        if (!existingValue.isEmpty() && priceInput != null) {
            priceInput.setValue(existingValue);
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

        inputWidth = Math.min(160, panelWidth - PANEL_PADDING * 2);
        inputHeight = INPUT_HEIGHT;
        inputX = panelX + panelWidth - PANEL_PADDING - inputWidth;
        inputY = panelY + 90;

        int buttonWidth = Math.min(BUTTON_WIDTH, Math.max(90, panelWidth - PANEL_PADDING * 2));
        listBtnX1 = panelX + (panelWidth - buttonWidth) / 2;
        listBtnY1 = panelY + panelHeight - 34;
        listBtnX2 = listBtnX1 + buttonWidth;
        listBtnY2 = listBtnY1 + BUTTON_HEIGHT;
    }

    private void initPriceInput() {
        int boxX = Math.round(inputX * uiScale);
        int boxY = Math.round(inputY * uiScale);
        int boxWidth = Math.round(inputWidth * uiScale);
        int boxHeight = Math.round(inputHeight * uiScale);

        if (priceInput == null) {
            priceInput = new EditBox(this.font, boxX, boxY, boxWidth, boxHeight,
                Component.translatable(Util_MessageKeys.LIST_PRICE_TEXT_KEY));
            priceInput.setHint(Component.translatable(Util_MessageKeys.LIST_HINT_TEXT_KEY));
            priceInput.setMaxLength(9);
            this.addRenderableWidget(priceInput);
        } else {
            priceInput.setX(boxX);
            priceInput.setY(boxY);
            priceInput.setWidth(boxWidth);
            priceInput.setHeight(boxHeight);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderFullScreenBackground(guiGraphics);

        calculateVirtualSize();
        updateLayout();
        initPriceInput();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(uiScale, uiScale, 1.0f);

        float virtualMouseX = mouseX / uiScale;
        float virtualMouseY = mouseY / uiScale;

        drawTitle(guiGraphics);
        drawEscHint(guiGraphics);
        renderPanel(guiGraphics, virtualMouseX, virtualMouseY);

        guiGraphics.pose().popPose();

        renderInputBackground(guiGraphics);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderFullScreenBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xB0000000);
    }

    private void drawTitle(GuiGraphics guiGraphics) {
        int y = PANEL_PADDING + font.lineHeight + 10;
        String title = "📦 " + Component.translatable(Util_MessageKeys.LIST_TITLE_KEY).getString();
        CardRenderer.drawVersionInfo(guiGraphics, font, PANEL_PADDING, y, 220, title);
    }

    private void drawEscHint(GuiGraphics guiGraphics) {
        String hint = "按 ESC 返回";
        int hintWidth = font.width(hint);
        int x = virtualWidth - PANEL_PADDING - hintWidth;
        int y = PANEL_PADDING + 6;
        guiGraphics.drawString(font, hint, x, y, 0x90FFFFFF);
    }

    private void renderPanel(GuiGraphics guiGraphics, float mouseX, float mouseY) {
        CardRenderer.drawCard(guiGraphics, panelX, panelY, panelWidth, panelHeight, CardRenderer.THEME_MARKET, false);

        int textX = panelX + PANEL_PADDING;
        int textY = panelY + 8;

        String titleText = Component.translatable(Util_MessageKeys.LIST_TITLE_KEY).getString();
        guiGraphics.drawString(font, titleText, textX, textY, CardRenderer.TEXT_TITLE);
        textY += font.lineHeight + 4;

        int itemCardX = panelX + PANEL_PADDING;
        int itemCardY = textY;
        int itemCardWidth = panelWidth - PANEL_PADDING * 2;
        CardRenderer.drawCard(guiGraphics, itemCardX, itemCardY, itemCardWidth, ITEM_CARD_HEIGHT, CardRenderer.THEME_MARKET, false);

        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            String emptyText = Component.translatable(Util_MessageKeys.LIST_NO_ITEM_IN_HAND_TEXT_KEY).getString();
            int textWidth = font.width(emptyText);
            int textCenterX = itemCardX + (itemCardWidth - textWidth) / 2;
            int textCenterY = itemCardY + (ITEM_CARD_HEIGHT - font.lineHeight) / 2;
            guiGraphics.drawString(font, emptyText, textCenterX, textCenterY, 0x80FFFFFF);
        } else {
            int iconX = itemCardX + 8;
            int iconY = itemCardY + (ITEM_CARD_HEIGHT - ICON_SIZE) / 2;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(2.0f, 2.0f, 1.0f);
            guiGraphics.renderItem(heldItem, iconX / 2, iconY / 2);
            guiGraphics.pose().popPose();

            int nameX = iconX + ICON_SIZE + 8;
            int nameY = itemCardY + 8;
            String itemName = CardRenderer.truncateText(font, heldItem.getHoverName().getString(),
                itemCardWidth - nameX + itemCardX - 10);
            guiGraphics.drawString(font, itemName, nameX, nameY, 0xFFFFFFFF);

            String countText = "x" + heldItem.getCount();
            guiGraphics.drawString(font, countText, nameX, nameY + font.lineHeight + 2, 0x90FFFFFF);
        }

        int labelY = itemCardY + ITEM_CARD_HEIGHT + 18;
        String priceLabel = Component.translatable(Util_MessageKeys.LIST_PRICE_TEXT_KEY).getString();
        guiGraphics.drawString(font, priceLabel, textX, labelY, CardRenderer.TEXT_DESC);

        boolean listHovered = mouseX >= listBtnX1 && mouseX <= listBtnX2 &&
                              mouseY >= listBtnY1 && mouseY <= listBtnY2;
        boolean enabled = !heldItem.isEmpty();
        UiButtonStyle style = enabled ? listStyle : disabledStyle;
        drawStripedButton(guiGraphics, listBtnX1, listBtnY1, listBtnX2 - listBtnX1, BUTTON_HEIGHT,
            Component.translatable(Util_MessageKeys.LIST_LIST_BUTTON_KEY).getString(), style, enabled && listHovered);
    }

    private void renderInputBackground(GuiGraphics guiGraphics) {
        if (priceInput == null) {
            return;
        }
        int boxX = priceInput.getX();
        int boxY = priceInput.getY();
        int boxWidth = priceInput.getWidth();
        int boxHeight = priceInput.getHeight();

        int bgColor = 0xE04A5568;
        int borderColor = CardRenderer.THEME_MARKET;

        guiGraphics.fill(boxX - 4, boxY - 2, boxX + boxWidth + 4, boxY + boxHeight + 2, bgColor);
        guiGraphics.fill(boxX - 4, boxY - 2, boxX + boxWidth + 4, boxY - 1, borderColor);
        guiGraphics.fill(boxX - 4, boxY + boxHeight + 1, boxX + boxWidth + 4, boxY + boxHeight + 2, borderColor);
        guiGraphics.fill(boxX - 4, boxY - 2, boxX - 3, boxY + boxHeight + 2, borderColor);
        guiGraphics.fill(boxX + boxWidth + 3, boxY - 2, boxX + boxWidth + 4, boxY + boxHeight + 2, borderColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float virtualMouseX = (float) mouseX / uiScale;
        float virtualMouseY = (float) mouseY / uiScale;

        if (virtualMouseX >= listBtnX1 && virtualMouseX <= listBtnX2 &&
            virtualMouseY >= listBtnY1 && virtualMouseY <= listBtnY2) {
            listItem();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void listItem() {
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            this.player.sendSystemMessage(Component.translatable(Util_MessageKeys.LIST_NO_ITEM_IN_HAND_MESSAGE_KEY));
            return;
        }

        String priceText = priceInput == null ? "" : priceInput.getValue();
        Optional<Integer> price = parsePrice(priceText);

        if (price.isEmpty() || price.get() <= 0) {
            this.player.sendSystemMessage(Component.translatable(Util_MessageKeys.LIST_INVALID_PRICE_MESSAGE_KEY));
            return;
        }

        SalesOrder salesOrder = new SalesOrder(
            UUID.randomUUID(),
            heldItem.getItem().getDescriptionId(),
            heldItem.copy(),
            price.get(),
            player.getName().getString(),
            player.getUUID(),
            System.currentTimeMillis()
        );

        EconomySystem_NetworkManager.sendToServer(new Packet_CreateSalesOrder(salesOrder));
        if (this.minecraft != null) {
            this.minecraft.setScreen(new Screen_Market());
        }
    }

    private Optional<Integer> parsePrice(String priceText) {
        try {
            return Optional.of(Integer.parseInt(priceText));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && this.shouldCloseOnEsc()) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new Screen_Market());
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
}


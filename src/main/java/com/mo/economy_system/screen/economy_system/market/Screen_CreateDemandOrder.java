package com.mo.economy_system.screen.economy_system.market;

import com.mo.economy_system.core.economy_system.market.DemandOrder;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.economy_system.demand_order.Packet_CreateDemandOrder;
import com.mo.economy_system.screen.components.CardRenderer;
import com.mo.economy_system.screen.components.HighLevelTextField;
import com.mo.economy_system.screen.components.UiButtonRenderer;
import com.mo.economy_system.screen.components.UiButtonStyle;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

public class Screen_CreateDemandOrder extends Screen {

    private static final int BASE_WIDTH = 640;
    private static final int BASE_HEIGHT = 360;
    private static final int PANEL_PADDING = 12;
    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 238;
    private static final int INPUT_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 22;
    private static final int BUTTON_WIDTH = 110;
    private static final int PREVIEW_HEIGHT = 58;
    private static final int ICON_SIZE = 32;

    private final Player player;
    private HighLevelTextField itemIdInput;
    private EditBox countInput;
    private EditBox priceInput;

    private float uiScale;
    private int virtualWidth;
    private int virtualHeight;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int itemIdX;
    private int itemIdY;
    private int itemIdWidth;
    private int countX;
    private int countY;
    private int countWidth;
    private int priceX;
    private int priceY;
    private int priceWidth;
    private int requestBtnX1;
    private int requestBtnY1;
    private int requestBtnX2;
    private int requestBtnY2;

    private final UiButtonStyle requestStyle;
    private final UiButtonStyle disabledStyle;

    public Screen_CreateDemandOrder(Player player) {
        super(Component.translatable(Util_MessageKeys.REQUEST_TITLE_KEY));
        this.player = player;
        this.requestStyle = createButtonStyle(CardRenderer.THEME_SHOP);
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
        String itemId = itemIdInput == null ? "" : itemIdInput.getValue();
        String count = countInput == null ? "" : countInput.getValue();
        String price = priceInput == null ? "" : priceInput.getValue();

        super.init();
        calculateVirtualSize();
        updateLayout();
        initInputs();

        itemIdInput.setValue(itemId);
        countInput.setValue(count);
        priceInput.setValue(price);
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

        int labelWidth = 72;
        int fieldWidth = Math.max(120, panelWidth - PANEL_PADDING * 2 - labelWidth);
        int fieldX = panelX + PANEL_PADDING + labelWidth;

        itemIdX = fieldX;
        itemIdY = panelY + 84;
        itemIdWidth = fieldWidth;

        countX = fieldX;
        countY = itemIdY + 28;
        countWidth = Math.min(120, fieldWidth);

        priceX = fieldX;
        priceY = countY + 28;
        priceWidth = Math.min(140, fieldWidth);

        int buttonWidth = Math.min(BUTTON_WIDTH, Math.max(90, panelWidth - PANEL_PADDING * 2));
        requestBtnX1 = panelX + (panelWidth - buttonWidth) / 2;
        requestBtnY1 = panelY + panelHeight - 34;
        requestBtnX2 = requestBtnX1 + buttonWidth;
        requestBtnY2 = requestBtnY1 + BUTTON_HEIGHT;
    }

    private void initInputs() {
        countInput = new EditBox(this.font, scaleX(countX), scaleY(countY), scaleX(countWidth), scaleY(INPUT_HEIGHT),
                Component.translatable(Util_MessageKeys.REQUEST_ITEM_COUNT_HINT_TEXT_KEY));
        countInput.setHint(Component.translatable(Util_MessageKeys.REQUEST_ITEM_COUNT_HINT_TEXT_KEY));
        countInput.setMaxLength(6);
        addRenderableWidget(countInput);

        priceInput = new EditBox(this.font, scaleX(priceX), scaleY(priceY), scaleX(priceWidth), scaleY(INPUT_HEIGHT),
                Component.translatable(Util_MessageKeys.REQUEST_PRICE_HINT_TEXT_KEY));
        priceInput.setHint(Component.translatable(Util_MessageKeys.REQUEST_PRICE_HINT_TEXT_KEY));
        priceInput.setMaxLength(9);
        addRenderableWidget(priceInput);

        itemIdInput = new HighLevelTextField(this.font, scaleX(itemIdX), scaleY(itemIdY), scaleX(itemIdWidth), scaleY(INPUT_HEIGHT),
                Component.translatable(Util_MessageKeys.REQUEST_ITEM_ID_HINT_TEXT_KEY));
        itemIdInput.setHint(Component.translatable(Util_MessageKeys.REQUEST_ITEM_ID_HINT_TEXT_KEY));
        itemIdInput.setMaxLength(128);
        itemIdInput.setSuggestions(buildItemIdSuggestions());
        addRenderableWidget(itemIdInput);
    }

    private List<String> buildItemIdSuggestions() {
        return BuiltInRegistries.ITEM.keySet().stream()
                .map(ResourceLocation::toString)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderFullScreenBackground(guiGraphics);

        calculateVirtualSize();
        updateLayout();
        updateInputBounds();
        renderInputBackgrounds(guiGraphics);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(uiScale, uiScale, 1.0f);

        float virtualMouseX = mouseX / uiScale;
        float virtualMouseY = mouseY / uiScale;

        drawTitle(guiGraphics);
        drawEscHint(guiGraphics);
        renderPanel(guiGraphics, virtualMouseX, virtualMouseY);

        guiGraphics.pose().popPose();

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void updateInputBounds() {
        if (itemIdInput != null) {
            itemIdInput.setX(scaleX(itemIdX));
            itemIdInput.setY(scaleY(itemIdY));
            itemIdInput.setWidth(scaleX(itemIdWidth));
            itemIdInput.setHeight(scaleY(INPUT_HEIGHT));
        }
        if (countInput != null) {
            countInput.setX(scaleX(countX));
            countInput.setY(scaleY(countY));
            countInput.setWidth(scaleX(countWidth));
            countInput.setHeight(scaleY(INPUT_HEIGHT));
        }
        if (priceInput != null) {
            priceInput.setX(scaleX(priceX));
            priceInput.setY(scaleY(priceY));
            priceInput.setWidth(scaleX(priceWidth));
            priceInput.setHeight(scaleY(INPUT_HEIGHT));
        }
    }

    private int scaleX(int x) {
        return Math.round(x * uiScale);
    }

    private int scaleY(int y) {
        return Math.round(y * uiScale);
    }

    private void renderFullScreenBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xB0000000);
    }

    private void drawTitle(GuiGraphics guiGraphics) {
        int y = PANEL_PADDING + font.lineHeight + 10;
        CardRenderer.drawVersionInfo(guiGraphics, font, PANEL_PADDING, y, 220,
                Component.translatable(Util_MessageKeys.REQUEST_TITLE_KEY).getString());
    }

    private void drawEscHint(GuiGraphics guiGraphics) {
        String hint = "按 ESC 返回";
        int hintWidth = font.width(hint);
        guiGraphics.drawString(font, hint, virtualWidth - PANEL_PADDING - hintWidth, PANEL_PADDING + 6, 0x90FFFFFF);
    }

    private void renderPanel(GuiGraphics guiGraphics, float mouseX, float mouseY) {
        CardRenderer.drawCard(guiGraphics, panelX, panelY, panelWidth, panelHeight, CardRenderer.THEME_SHOP, false);

        int textX = panelX + PANEL_PADDING;
        int textY = panelY + 8;
        guiGraphics.drawString(font, Component.translatable(Util_MessageKeys.REQUEST_TITLE_KEY), textX, textY, CardRenderer.TEXT_TITLE);

        renderPreview(guiGraphics, panelX + PANEL_PADDING, panelY + 24, panelWidth - PANEL_PADDING * 2);

        drawLabel(guiGraphics, Util_MessageKeys.REQUEST_ITEM_ID_TEXT_KEY, panelX + PANEL_PADDING, itemIdY + 6);
        drawLabel(guiGraphics, Util_MessageKeys.REQUEST_ITEM_COUNT_TEXT_KEY, panelX + PANEL_PADDING, countY + 6);
        drawLabel(guiGraphics, Util_MessageKeys.REQUEST_PRICE_TEXT_KEY, panelX + PANEL_PADDING, priceY + 6);

        Validation validation = validateInputs();
        if (validation.message() != null) {
            guiGraphics.drawString(font, validation.message().copy().withStyle(ChatFormatting.RED),
                    panelX + PANEL_PADDING, priceY + INPUT_HEIGHT + 12, 0xFFFFFFFF);
        }

        boolean hovered = mouseX >= requestBtnX1 && mouseX <= requestBtnX2 && mouseY >= requestBtnY1 && mouseY <= requestBtnY2;
        UiButtonStyle style = validation.valid() ? requestStyle : disabledStyle;
        UiButtonRenderer.drawStripedButton(guiGraphics, font, requestBtnX1, requestBtnY1, requestBtnX2 - requestBtnX1, BUTTON_HEIGHT,
                Component.translatable(Util_MessageKeys.REQUEST_REQUEST_BUTTON_KEY).getString(), "", style,
                validation.valid() && hovered, UiButtonRenderer.TextAlign.CENTER, false);
    }

    private void renderPreview(GuiGraphics guiGraphics, int x, int y, int width) {
        CardRenderer.drawCard(guiGraphics, x, y, width, PREVIEW_HEIGHT, CardRenderer.THEME_SHOP, false);
        ItemStack preview = getRequestedStack();
        if (preview.isEmpty()) {
            String text = Component.translatable(Util_MessageKeys.REQUEST_UNKNOWN_ITEM_ID_KEY).getString();
            int textWidth = font.width(text);
            guiGraphics.drawString(font, text, x + (width - textWidth) / 2, y + (PREVIEW_HEIGHT - font.lineHeight) / 2, 0x80FFFFFF);
            return;
        }

        int iconX = x + 8;
        int iconY = y + (PREVIEW_HEIGHT - ICON_SIZE) / 2;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(2.0f, 2.0f, 1.0f);
        guiGraphics.renderItem(preview, iconX / 2, iconY / 2);
        guiGraphics.pose().popPose();

        int nameX = iconX + ICON_SIZE + 8;
        String name = CardRenderer.truncateText(font, preview.getHoverName().getString(), width - ICON_SIZE - 28);
        guiGraphics.drawString(font, name, nameX, y + 10, 0xFFFFFFFF);
        guiGraphics.drawString(font, BuiltInRegistries.ITEM.getKey(preview.getItem()).toString(), nameX, y + 24, 0x90FFFFFF);
    }

    private void drawLabel(GuiGraphics guiGraphics, String key, int x, int y) {
        guiGraphics.drawString(font, Component.translatable(key), x, y, CardRenderer.TEXT_DESC);
    }

    private void renderInputBackgrounds(GuiGraphics guiGraphics) {
        drawInputBackground(guiGraphics, itemIdInput, CardRenderer.THEME_SHOP);
        drawInputBackground(guiGraphics, countInput, CardRenderer.THEME_SHOP);
        drawInputBackground(guiGraphics, priceInput, CardRenderer.THEME_SHOP);
    }

    private void drawInputBackground(GuiGraphics guiGraphics, EditBox input, int borderColor) {
        if (input == null) {
            return;
        }
        int x = input.getX();
        int y = input.getY();
        int width = input.getWidth();
        int height = input.getHeight();
        guiGraphics.fill(x - 4, y - 2, x + width + 4, y + height + 2, 0xE04A5568);
        guiGraphics.fill(x - 4, y - 2, x + width + 4, y - 1, borderColor);
        guiGraphics.fill(x - 4, y + height + 1, x + width + 4, y + height + 2, borderColor);
        guiGraphics.fill(x - 4, y - 2, x - 3, y + height + 2, borderColor);
        guiGraphics.fill(x + width + 3, y - 2, x + width + 4, y + height + 2, borderColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float virtualMouseX = (float) mouseX / uiScale;
        float virtualMouseY = (float) mouseY / uiScale;

        if (virtualMouseX >= requestBtnX1 && virtualMouseX <= requestBtnX2 &&
                virtualMouseY >= requestBtnY1 && virtualMouseY <= requestBtnY2) {
            requestItem();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void requestItem() {
        Validation validation = validateInputs();
        if (!validation.valid()) {
            if (validation.message() != null) {
                player.sendSystemMessage(validation.message());
            }
            return;
        }

        ItemStack stack = validation.stack().copy();
        stack.setCount(validation.count());

        DemandOrder marketItem = new DemandOrder(
                UUID.randomUUID(),
                stack.getItem().getDescriptionId(),
                stack,
                validation.price(),
                player.getName().getString(),
                player.getUUID(),
                System.currentTimeMillis(),
                false
        );

        EconomySystem_NetworkManager.sendToServer(new Packet_CreateDemandOrder(marketItem));
        if (this.minecraft != null) {
            this.minecraft.setScreen(new Screen_Market());
        }
    }

    private Validation validateInputs() {
        ItemStack stack = getRequestedStack();
        if (stack.isEmpty()) {
            return Validation.invalid(Component.translatable(Util_MessageKeys.REQUEST_UNKNOWN_ITEM_ID_KEY));
        }

        OptionalInt count = parsePositiveInt(countInput == null ? "" : countInput.getValue());
        if (count.isEmpty()) {
            return Validation.invalid(Component.translatable(Util_MessageKeys.REQUEST_INVALID_ITEM_COUNT_KEY));
        }
        if (count.getAsInt() > stack.getMaxStackSize()) {
            return Validation.invalid(Component.translatable(Util_MessageKeys.REQUEST_EXCESSIVE_ITEM_COUNT_KEY));
        }

        OptionalInt price = parsePositiveInt(priceInput == null ? "" : priceInput.getValue());
        if (price.isEmpty()) {
            return Validation.invalid(Component.translatable(Util_MessageKeys.REQUEST_INVALID_PRICE_KEY));
        }

        return new Validation(true, null, stack, count.getAsInt(), price.getAsInt());
    }

    private OptionalInt parsePositiveInt(String text) {
        try {
            int value = Integer.parseInt(text.trim());
            return value > 0 ? OptionalInt.of(value) : OptionalInt.empty();
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    private ItemStack getRequestedStack() {
        if (itemIdInput == null) {
            return ItemStack.EMPTY;
        }
        String itemID = itemIdInput.getValue().trim();
        if (itemID.isEmpty()) {
            return ItemStack.EMPTY;
        }
        try {
            ResourceLocation location = ResourceLocation.parse(itemID);
            if (!BuiltInRegistries.ITEM.containsKey(location)) {
                return ItemStack.EMPTY;
            }
            Item item = BuiltInRegistries.ITEM.get(location);
            return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
        } catch (Exception e) {
            return ItemStack.EMPTY;
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

    private record Validation(boolean valid, Component message, ItemStack stack, int count, int price) {
        private static Validation invalid(Component message) {
            return new Validation(false, message, ItemStack.EMPTY, 0, 0);
        }
    }
}

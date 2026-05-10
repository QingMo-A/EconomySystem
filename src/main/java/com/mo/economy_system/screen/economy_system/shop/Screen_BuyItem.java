package com.mo.economy_system.screen.economy_system.shop;

import com.mo.economy_system.core.economy_system.shop.ShopItem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.economy_system.Packet_ShopBuyItem;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class Screen_BuyItem extends Screen {

    private static final int BASE_WIDTH = 640;
    private static final int BASE_HEIGHT = 360;
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 160;
    private static final int PANEL_PADDING = 12;

    private static final int INPUT_WIDTH = 140;
    private static final int INPUT_MIN_WIDTH = 90;
    private static final int INPUT_HEIGHT = 20;
    private static final int LABEL_INPUT_GAP = 8;
    private static final int BUTTON_WIDTH = 96;
    private static final int BUTTON_HEIGHT = 24;

    private static final int BG_COLOR = 0xB0000000;
    private static final int PANEL_BG = 0xB01A2A3A;
    private static final int PANEL_BORDER = 0xFF4A8ACF;

    private final ShopItem shopItem;
    private final ItemStack itemStack;

    private EditBox countInput;
    private int countLabelX;
    private int countLabelY;
    private int countInputXVirtual;
    private int countInputYVirtual;
    private int countInputWidthVirtual;

    private float uiScale;
    private int virtualWidth;
    private int virtualHeight;

    private int buyBtnX1;
    private int buyBtnY1;
    private int buyBtnX2;
    private int buyBtnY2;

    public Screen_BuyItem(ShopItem shopItem) {
        super(Component.translatable(Util_MessageKeys.SHOP_BUY_TITLE_KEY));
        this.shopItem = shopItem;
        this.itemStack = Minecraft.getInstance().level == null
                ? shopItem.getItemStack()
                : shopItem.getItemStack(Minecraft.getInstance().level.registryAccess());
    }

    @Override
    protected void init() {
        super.init();
        calculateVirtualSize();

        this.countInput = new EditBox(this.font, 0, 0, 10, INPUT_HEIGHT,
                Component.translatable(Util_MessageKeys.SHOP_BUY_HINT_TEXT_KEY));
        this.countInput.setHint(Component.translatable(Util_MessageKeys.SHOP_BUY_HINT_TEXT_KEY));
        this.countInput.setMaxLength(8);
        // this.countInput.setValue("1");
        this.addRenderableWidget(this.countInput);

        updateInputLayout();
    }

    private void calculateVirtualSize() {
        float scaleX = (float) this.width / BASE_WIDTH;
        float scaleY = (float) this.height / BASE_HEIGHT;
        uiScale = Math.min(scaleX, scaleY);
        virtualWidth = (int) (this.width / uiScale);
        virtualHeight = (int) (this.height / uiScale);
    }

    private int panelX() {
        return (virtualWidth - PANEL_WIDTH) / 2;
    }

    private int panelY() {
        return (virtualHeight - PANEL_HEIGHT) / 2;
    }

    private void updateInputLayout() {
        if (countInput == null) {
            return;
        }

        Component countLabel = Component.translatable(Util_MessageKeys.SHOP_BUY_COUNT_TEXT_KEY);
        int labelWidth = this.font.width(countLabel);
        int panelX = panelX();
        int panelY = panelY();
        int rowY = panelY + 100;
        int contentMaxWidth = PANEL_WIDTH - PANEL_PADDING * 2;

        int preferredInlineWidth = labelWidth + LABEL_INPUT_GAP + INPUT_WIDTH;

        if (preferredInlineWidth <= contentMaxWidth) {
            int groupX = panelX + (PANEL_WIDTH - preferredInlineWidth) / 2;
            countLabelX = groupX;
            countLabelY = rowY;
            countInputXVirtual = groupX + labelWidth + LABEL_INPUT_GAP;
            countInputYVirtual = rowY - 2;
            countInputWidthVirtual = INPUT_WIDTH;
        } else {
            countLabelX = panelX + (PANEL_WIDTH - labelWidth) / 2;
            countLabelY = panelY + 88;
            countInputWidthVirtual = Math.max(INPUT_MIN_WIDTH, Math.min(INPUT_WIDTH, contentMaxWidth));
            countInputXVirtual = panelX + (PANEL_WIDTH - countInputWidthVirtual) / 2;
            countInputYVirtual = countLabelY + this.font.lineHeight + 4;
        }

        countInput.setX(Math.round(countInputXVirtual * uiScale));
        countInput.setY(Math.round(countInputYVirtual * uiScale));
        countInput.setWidth(Math.round(countInputWidthVirtual * uiScale));
        countInput.setHeight(Math.round(INPUT_HEIGHT * uiScale));
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderFullScreenBackground(guiGraphics);

        calculateVirtualSize();
        updateInputLayout();

        float virtualMouseX = mouseX / uiScale;
        float virtualMouseY = mouseY / uiScale;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(uiScale, uiScale, 1.0f);

        renderMainPanel(guiGraphics, virtualMouseX, virtualMouseY);
        drawEscHint(guiGraphics);

        guiGraphics.pose().popPose();

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderFullScreenBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, this.width, this.height, BG_COLOR);
    }

    private void renderMainPanel(GuiGraphics guiGraphics, float mouseX, float mouseY) {
        int panelX = panelX();
        int panelY = panelY();

        guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, PANEL_BG);
        guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 1, PANEL_BORDER);
        guiGraphics.fill(panelX, panelY + PANEL_HEIGHT - 1, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, PANEL_BORDER);
        guiGraphics.fill(panelX, panelY, panelX + 1, panelY + PANEL_HEIGHT, PANEL_BORDER);
        guiGraphics.fill(panelX + PANEL_WIDTH - 1, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, PANEL_BORDER);

        String itemName = itemStack.isEmpty()
                ? Component.translatable(Util_MessageKeys.SHOP_BUY_NO_ITEM_TEXT_KEY).getString()
                : itemStack.getHoverName().getString();

        int nameWidth = this.font.width(itemName);
        int nameX = panelX + (PANEL_WIDTH - nameWidth) / 2;
        int nameY = panelY + 16;
        guiGraphics.drawString(this.font, itemName, nameX, nameY, 0xFFFFFFFF);

        if (!itemStack.isEmpty()) {
            int iconX = panelX + (PANEL_WIDTH - 16) / 2;
            int iconY = panelY + 34;
            guiGraphics.renderItem(itemStack, iconX, iconY);
        }

        Component priceText = Component.translatable(Util_MessageKeys.SHOP_ITEM_CURRENT_PRICE_KEY, shopItem.getCurrentPrice())
                .withStyle(ChatFormatting.YELLOW);
        int priceWidth = this.font.width(priceText);
        int priceY = panelY + 56;
        guiGraphics.drawString(this.font, priceText, panelX + (PANEL_WIDTH - priceWidth) / 2, priceY, 0xFFFFFFFF);

        int countValue = parseCountValue();
        int totalPrice = countValue > 0 ? shopItem.getCurrentPrice() * countValue : 0;
        Component totalText = Component.translatable(Util_MessageKeys.SHOP_BUY_TOTAL_PRICE_TEXT_KEY, totalPrice)
                .withStyle(ChatFormatting.GOLD);
        int totalWidth = this.font.width(totalText);
        int totalY = priceY + this.font.lineHeight + 4;
        guiGraphics.drawString(this.font, totalText, panelX + (PANEL_WIDTH - totalWidth) / 2, totalY, 0xFFFFFFFF);

        Player player = Minecraft.getInstance().player;
        if (player != null && countValue > 0) {
            int missingSlots = calculateMissingSlots(player, countValue);
            if (missingSlots > 0) {
                Component inventoryHintText = Component.translatable(
                        Util_MessageKeys.SHOP_BUY_INVENTORY_INSUFFICIENT_TEXT_KEY, missingSlots
                ).withStyle(ChatFormatting.RED);
                int hintWidth = this.font.width(inventoryHintText);
                int hintY = totalY + this.font.lineHeight + 3;
                guiGraphics.drawString(this.font, inventoryHintText, panelX + (PANEL_WIDTH - hintWidth) / 2, hintY, 0xFFFFFFFF);
            }
        }

        Component countLabel = Component.translatable(Util_MessageKeys.SHOP_BUY_COUNT_TEXT_KEY);
        guiGraphics.drawString(this.font, countLabel, countLabelX, countLabelY, 0xD0FFFFFF);

        int btnX = panelX + (PANEL_WIDTH - BUTTON_WIDTH) / 2;
        int btnY = panelY + PANEL_HEIGHT - PANEL_PADDING - BUTTON_HEIGHT;

        buyBtnX1 = btnX;
        buyBtnY1 = btnY;
        buyBtnX2 = btnX + BUTTON_WIDTH;
        buyBtnY2 = btnY + BUTTON_HEIGHT;

        boolean hovered = mouseX >= buyBtnX1 && mouseX <= buyBtnX2 && mouseY >= buyBtnY1 && mouseY <= buyBtnY2;
        drawBuyButton(guiGraphics, btnX, btnY, hovered);
    }

    private void drawBuyButton(GuiGraphics guiGraphics, int x, int y, boolean hovered) {
        int bg = hovered ? 0xD04A8ACF : 0xB03A7ABF;
        int border = hovered ? 0xFF6AB8FF : 0xFF4A8ACF;

        guiGraphics.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, bg);
        guiGraphics.fill(x, y, x + BUTTON_WIDTH, y + 1, border);
        guiGraphics.fill(x, y + BUTTON_HEIGHT - 1, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, border);
        guiGraphics.fill(x, y, x + 1, y + BUTTON_HEIGHT, border);
        guiGraphics.fill(x + BUTTON_WIDTH - 1, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, border);

        String text = Component.translatable(Util_MessageKeys.SHOP_BUY_BUY_BUTTON_KEY).getString();
        int textWidth = this.font.width(text);
        int textX = x + (BUTTON_WIDTH - textWidth) / 2;
        int textY = y + (BUTTON_HEIGHT - this.font.lineHeight) / 2;
        guiGraphics.drawString(this.font, text, textX, textY, 0xFFFFFFFF);
    }

    private void drawEscHint(GuiGraphics guiGraphics) {
        String hint = Component.translatable(Util_MessageKeys.SHOP_ESC_HINT_TEXT_KEY).getString();
        int hintWidth = this.font.width(hint);
        int x = virtualWidth - PANEL_PADDING - hintWidth;
        int y = virtualHeight - PANEL_PADDING - this.font.lineHeight;
        guiGraphics.drawString(this.font, hint, x, y, 0x90FFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float virtualMouseX = (float) mouseX / uiScale;
        float virtualMouseY = (float) mouseY / uiScale;

        if (virtualMouseX >= buyBtnX1 && virtualMouseX <= buyBtnX2 &&
                virtualMouseY >= buyBtnY1 && virtualMouseY <= buyBtnY2) {
            buyItem();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void buyItem() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        if (itemStack.isEmpty()) {
            player.sendSystemMessage(Component.translatable(Util_MessageKeys.SHOP_BUY_NO_ITEM_MESSAGE_KEY));
            return;
        }

        String countText = countInput == null ? "" : countInput.getValue().trim();
        int count;
        try {
            count = Integer.parseInt(countText);
        } catch (NumberFormatException e) {
            player.sendSystemMessage(Component.translatable(Util_MessageKeys.SHOP_BUY_INVALID_COUNT_MESSAGE_KEY));
            return;
        }

        if (count <= 0) {
            player.sendSystemMessage(Component.translatable(Util_MessageKeys.SHOP_BUY_INVALID_COUNT_MESSAGE_KEY));
            return;
        }

        if (!hasEnoughInventorySpace(player, count)) {
            player.sendSystemMessage(Component.translatable(Util_MessageKeys.SHOP_BUY_FAILED_INVENTORY_FULL_MESSAGE_KEY));
            return;
        }

        EconomySystem_NetworkManager.sendToServer(
                new Packet_ShopBuyItem(shopItem.getShopItemId(), count)
        );

        if (this.minecraft != null) {
            this.minecraft.setScreen(new Screen_Shop());
        }
    }

    private int parseCountValue() {
        if (countInput == null) {
            return -1;
        }
        try {
            return Integer.parseInt(countInput.getValue().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean hasEnoughInventorySpace(Player player, int quantity) {
        return calculateMissingSlots(player, quantity) <= 0;
    }

    private int calculateMissingSlots(Player player, int quantity) {
        int maxStackSize = itemStack.getMaxStackSize();
        int remaining = quantity;

        if (maxStackSize > 1) {
            for (ItemStack stack : player.getInventory().items) {
                if (ItemStack.isSameItemSameComponents(stack, itemStack) && stack.getCount() < stack.getMaxStackSize()) {
                    int availableSpace = stack.getMaxStackSize() - stack.getCount();
                    int transfer = Math.min(availableSpace, remaining);
                    remaining -= transfer;
                    if (remaining == 0) {
                        return 0;
                    }
                }
            }
        }

        if (remaining <= 0) {
            return 0;
        }

        int requiredSlots = maxStackSize == 1
                ? remaining
                : (remaining + maxStackSize - 1) / maxStackSize;

        int freeSlots = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) {
                freeSlots++;
            }
        }

        return Math.max(0, requiredSlots - freeSlots);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new Screen_Shop());
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {}
}

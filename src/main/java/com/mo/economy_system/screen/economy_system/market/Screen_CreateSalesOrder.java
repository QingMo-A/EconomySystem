package com.mo.economy_system.screen.economy_system.market;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.economy_system.sales_order.Packet_CreateSalesOrder;
import com.mo.economy_system.screen.components.CardRenderer;
import com.mo.economy_system.screen.components.UiButtonRenderer;
import com.mo.economy_system.screen.components.UiButtonStyle;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Screen_CreateSalesOrder extends Screen {

    private static final int BASE_WIDTH = 640;
    private static final int BASE_HEIGHT = 360;
    private static final int PADDING = 12;
    private static final int SLOT_SIZE = 24;
    private static final int LEFT_WIDTH = 292;
    private static final int RIGHT_WIDTH = 300;
    private static final int INPUT_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 22;
    private static final int ICON_SIZE = 32;

    private final Player player;
    private final List<SlotArea> slotAreas = new ArrayList<>();
    private EditBox priceInput;
    private EditBox countInput;
    private int selectedSlot = -1;
    private int selectedCount = 1;

    private float uiScale;
    private int virtualWidth;
    private int virtualHeight;
    private int leftX, leftY, leftH;
    private int rightX, rightY, rightH;
    private int priceInputX, priceInputY, countInputX, countInputY;
    private int decBtnX1, decBtnY1, decBtnX2, decBtnY2;
    private int incBtnX1, incBtnY1, incBtnX2, incBtnY2;
    private int allBtnX1, allBtnY1, allBtnX2, allBtnY2;
    private int listBtnX1, listBtnY1, listBtnX2, listBtnY2;

    private final UiButtonStyle listStyle;
    private final UiButtonStyle adjustStyle;
    private final UiButtonStyle disabledStyle;

    private record SlotArea(int x, int y, int slot, ItemStack stack) {}

    public Screen_CreateSalesOrder(Player player) {
        super(Component.translatable(Util_MessageKeys.LIST_TITLE_KEY));
        this.player = player;
        this.listStyle = createButtonStyle(CardRenderer.THEME_MARKET);
        this.adjustStyle = createButtonStyle(CardRenderer.THEME_DELIVERY).setPadding(5);
        this.disabledStyle = createButtonStyle(0xFF6F7F8C)
                .setTextColor(0xFFB0BBC6)
                .setBgAlpha(0x30)
                .setBgAlphaHover(0x30)
                .setStripeAlpha(0x50)
                .setStripeAlphaHover(0x50)
                .setGlowHeight(0)
                .setBorderAlpha(0x20)
                .setBorderAlphaHover(0x20);
        selectInitialSlot();
    }

    @Override
    protected void init() {
        String oldPrice = priceInput == null ? "" : priceInput.getValue();
        String oldCount = countInput == null ? String.valueOf(selectedCount) : countInput.getValue();
        super.init();
        calculateVirtualSize();
        updateLayout();
        initInputs(oldPrice, oldCount);
    }

    private void selectInitialSlot() {
        Inventory inventory = player.getInventory();
        int mainHandSlot = inventory.selected;
        if (isValidFilledSlot(mainHandSlot)) {
            selectedSlot = mainHandSlot;
            selectedCount = 1;
            return;
        }
        for (int i = 0; i < inventory.items.size(); i++) {
            if (isValidFilledSlot(i)) {
                selectedSlot = i;
                selectedCount = 1;
                return;
            }
        }
    }

    private boolean isValidFilledSlot(int slot) {
        return slot >= 0 && slot < player.getInventory().items.size() && !player.getInventory().items.get(slot).isEmpty();
    }

    private void calculateVirtualSize() {
        float scaleX = (float) this.width / BASE_WIDTH;
        float scaleY = (float) this.height / BASE_HEIGHT;
        uiScale = Math.min(scaleX, scaleY);
        virtualWidth = (int) (this.width / uiScale);
        virtualHeight = (int) (this.height / uiScale);
    }

    private void updateLayout() {
        int contentWidth = Math.min(LEFT_WIDTH + RIGHT_WIDTH + PADDING, virtualWidth - PADDING * 2);
        leftX = (virtualWidth - contentWidth) / 2;
        leftY = 52;
        leftH = Math.min(252, virtualHeight - 88);
        rightX = leftX + LEFT_WIDTH + PADDING;
        rightY = leftY;
        rightH = leftH;

        if (rightX + RIGHT_WIDTH > virtualWidth - PADDING) {
            rightX = leftX;
            rightY = leftY + leftH + PADDING;
        }

        priceInputX = rightX + 104;
        priceInputY = rightY + 112;
        countInputX = rightX + 104;
        countInputY = rightY + 82;
    }

    private void initInputs(String oldPrice, String oldCount) {
        priceInput = new EditBox(this.font, Math.round(priceInputX * uiScale), Math.round(priceInputY * uiScale),
                Math.round(150 * uiScale), Math.round(INPUT_HEIGHT * uiScale), Component.translatable(Util_MessageKeys.LIST_PRICE_TEXT_KEY));
        priceInput.setHint(Component.literal("价格"));
        priceInput.setMaxLength(9);
        priceInput.setValue(oldPrice);
        addRenderableWidget(priceInput);

        countInput = new EditBox(this.font, Math.round(countInputX * uiScale), Math.round(countInputY * uiScale),
                Math.round(58 * uiScale), Math.round(INPUT_HEIGHT * uiScale), Component.literal("数量"));
        countInput.setHint(Component.literal("数量"));
        countInput.setMaxLength(4);
        countInput.setValue(oldCount == null || oldCount.isBlank() ? String.valueOf(selectedCount) : oldCount);
        countInput.setResponder(value -> syncCountFromInput());
        addRenderableWidget(countInput);
    }

    private void updateInputsLayout() {
        if (priceInput != null) {
            priceInput.setX(Math.round(priceInputX * uiScale));
            priceInput.setY(Math.round(priceInputY * uiScale));
            priceInput.setWidth(Math.round(150 * uiScale));
            priceInput.setHeight(Math.round(INPUT_HEIGHT * uiScale));
        }
        if (countInput != null) {
            countInput.setX(Math.round(countInputX * uiScale));
            countInput.setY(Math.round(countInputY * uiScale));
            countInput.setWidth(Math.round(58 * uiScale));
            countInput.setHeight(Math.round(INPUT_HEIGHT * uiScale));
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xB0000000);
        calculateVirtualSize();
        updateLayout();
        updateInputsLayout();
        clampSelectedCount();

        float vx = mouseX / uiScale;
        float vy = mouseY / uiScale;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(uiScale, uiScale, 1.0f);
        drawHeader(guiGraphics);
        renderInventoryPanel(guiGraphics, vx, vy);
        renderSelectedPanel(guiGraphics, vx, vy);
        guiGraphics.pose().popPose();

        renderInputBackground(guiGraphics, priceInput, CardRenderer.THEME_MARKET);
        renderInputBackground(guiGraphics, countInput, CardRenderer.THEME_DELIVERY);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderSlotTooltip(guiGraphics, mouseX, mouseY);
    }

    private void drawHeader(GuiGraphics guiGraphics) {
        CardRenderer.drawVersionInfo(guiGraphics, font, PADDING, PADDING + 12, 240, "上架商品");
        String hint = "按 ESC 返回";
        guiGraphics.drawString(font, hint, virtualWidth - PADDING - font.width(hint), PADDING + 6, 0x90FFFFFF);
    }

    private void renderInventoryPanel(GuiGraphics guiGraphics, float mouseX, float mouseY) {
        CardRenderer.drawCard(guiGraphics, leftX, leftY, LEFT_WIDTH, leftH, CardRenderer.THEME_MARKET, false);
        guiGraphics.drawString(font, "选择背包物品", leftX + PADDING, leftY + 10, CardRenderer.TEXT_TITLE);

        slotAreas.clear();
        int gridColumns = 9;
        int availableWidth = LEFT_WIDTH - PADDING * 2;
        int slotGap = Math.max(4, (availableWidth - gridColumns * SLOT_SIZE) / Math.max(1, gridColumns - 1));
        int gridWidth = gridColumns * SLOT_SIZE + (gridColumns - 1) * slotGap;
        int gridX = leftX + (LEFT_WIDTH - gridWidth) / 2;
        int gridY = leftY + 34;
        Inventory inventory = player.getInventory();
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = row == 3 ? col : 9 + row * 9 + col;
                ItemStack stack = inventory.items.get(slot);
                int x = gridX + col * (SLOT_SIZE + slotGap);
                int y = gridY + row * (SLOT_SIZE + slotGap);
                boolean filled = !stack.isEmpty();
                boolean selected = slot == selectedSlot;
                boolean hovered = mouseX >= x && mouseX <= x + SLOT_SIZE && mouseY >= y && mouseY <= y + SLOT_SIZE;
                int bg = selected ? 0x804FC3F7 : hovered ? 0x604A5568 : 0x404A5568;
                guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, bg);
                guiGraphics.fill(x, y, x + SLOT_SIZE, y + 1, selected ? CardRenderer.THEME_MARKET : 0x554FC3F7);
                if (filled) {
                    guiGraphics.renderItem(stack, x + 4, y + 4);
                    String count = String.valueOf(stack.getCount());
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(0, 0, 200);
                    guiGraphics.drawString(font, count, x + SLOT_SIZE - font.width(count) - 2, y + SLOT_SIZE - 9, 0xFFFFFFFF);
                    guiGraphics.pose().popPose();
                    slotAreas.add(new SlotArea(x, y, slot, stack.copy()));
                }
            }
        }
    }

    private void renderSelectedPanel(GuiGraphics guiGraphics, float mouseX, float mouseY) {
        CardRenderer.drawCard(guiGraphics, rightX, rightY, RIGHT_WIDTH, rightH, CardRenderer.THEME_MARKET, false);
        guiGraphics.drawString(font, "上架设置", rightX + PADDING, rightY + 10, CardRenderer.TEXT_TITLE);

        ItemStack selected = getSelectedStack();
        if (selected.isEmpty()) {
            String empty = "请选择一个要上架的物品";
            guiGraphics.drawString(font, empty, rightX + (RIGHT_WIDTH - font.width(empty)) / 2, rightY + 74, 0x80FFFFFF);
            drawListButton(guiGraphics, mouseX, mouseY, false);
            return;
        }

        int iconX = rightX + PADDING;
        int iconY = rightY + 36;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(2.0f, 2.0f, 1.0f);
        guiGraphics.renderItem(selected, iconX / 2, iconY / 2);
        guiGraphics.pose().popPose();

        int ownedCount = countMatchingItems(selected);
        String name = CardRenderer.truncateText(font, selected.getHoverName().getString(), RIGHT_WIDTH - 64);
        guiGraphics.drawString(font, name, iconX + ICON_SIZE + 8, iconY + 2, 0xFFFFFFFF);
        guiGraphics.drawString(font, "拥有: " + ownedCount, iconX + ICON_SIZE + 8, iconY + 16, 0xB8FFFFFF);

        guiGraphics.drawString(font, "上架数量", rightX + PADDING, countInputY + 6, CardRenderer.TEXT_DESC);
        renderAdjustButtons(guiGraphics, mouseX, mouseY, ownedCount);

        guiGraphics.drawString(font, "出售价格", rightX + PADDING, priceInputY + 6, CardRenderer.TEXT_DESC);

        int price = parsePrice().orElse(0);
        int tax = price > 0 ? calculateTax(price) : 0;
        int income = Math.max(0, price - tax);
        int infoY = rightY + 142;
        guiGraphics.drawString(font, "商品税: " + (tax > 0 ? tax + " 梦鱼币" : "-"), rightX + PADDING, infoY, tax > 0 ? 0xFFFFD166 : 0x80FFFFFF);
        guiGraphics.drawString(font, "预计到账: " + (price > 0 ? income + " 梦鱼币" : "-"), rightX + PADDING, infoY + 14, 0xFF7CFFB2);
        guiGraphics.drawString(font, "价格单位: 梦鱼币", rightX + PADDING, infoY + 28, CardRenderer.TEXT_DESC);

        drawListButton(guiGraphics, mouseX, mouseY, price > 0 && selectedCount > 0 && selectedCount <= ownedCount);
    }

    private void renderAdjustButtons(GuiGraphics guiGraphics, float mouseX, float mouseY, int ownedCount) {
        int y = countInputY - 1;
        int size = BUTTON_HEIGHT;
        decBtnX1 = countInputX + 66;
        decBtnY1 = y;
        decBtnX2 = decBtnX1 + 30;
        decBtnY2 = y + size;
        incBtnX1 = decBtnX2 + 4;
        incBtnY1 = y;
        incBtnX2 = incBtnX1 + 30;
        incBtnY2 = y + size;
        allBtnX1 = incBtnX2 + 4;
        allBtnY1 = y;
        allBtnX2 = allBtnX1 + 48;
        allBtnY2 = y + size;

        drawStripedButton(guiGraphics, decBtnX1, decBtnY1, 30, size, "-1", adjustStyle, isHover(mouseX, mouseY, decBtnX1, decBtnY1, decBtnX2, decBtnY2) && selectedCount > 1);
        drawStripedButton(guiGraphics, incBtnX1, incBtnY1, 30, size, "+1", adjustStyle, isHover(mouseX, mouseY, incBtnX1, incBtnY1, incBtnX2, incBtnY2) && selectedCount < ownedCount);
        drawStripedButton(guiGraphics, allBtnX1, allBtnY1, 48, size, "全部", adjustStyle, isHover(mouseX, mouseY, allBtnX1, allBtnY1, allBtnX2, allBtnY2));
    }

    private void drawListButton(GuiGraphics guiGraphics, float mouseX, float mouseY, boolean enabled) {
        listBtnX1 = rightX + (RIGHT_WIDTH - 120) / 2;
        listBtnY1 = rightY + rightH - 36;
        listBtnX2 = listBtnX1 + 120;
        listBtnY2 = listBtnY1 + BUTTON_HEIGHT;
        drawStripedButton(guiGraphics, listBtnX1, listBtnY1, 120, BUTTON_HEIGHT,
                Component.translatable(Util_MessageKeys.LIST_LIST_BUTTON_KEY).getString(),
                enabled ? listStyle : disabledStyle,
                enabled && isHover(mouseX, mouseY, listBtnX1, listBtnY1, listBtnX2, listBtnY2));
    }

    private ItemStack getSelectedStack() {
        if (!isValidFilledSlot(selectedSlot)) {
            return ItemStack.EMPTY;
        }
        return player.getInventory().items.get(selectedSlot);
    }

    private void syncCountFromInput() {
        if (countInput == null) {
            return;
        }
        try {
            selectedCount = Integer.parseInt(countInput.getValue().trim());
        } catch (NumberFormatException ignored) {
            selectedCount = 1;
        }
        clampSelectedCount();
    }

    private void clampSelectedCount() {
        ItemStack selected = getSelectedStack();
        int max = selected.isEmpty() ? 1 : countMatchingItems(selected);
        selectedCount = Math.max(1, Math.min(selectedCount, max));
    }

    private int countMatchingItems(ItemStack template) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, template)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private Optional<Integer> parsePrice() {
        if (priceInput == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(priceInput.getValue().trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private int calculateTax(int price) {
        return Math.max(1, (int) Math.min(Integer.MAX_VALUE, Math.ceil(price * 0.1D)));
    }

    private void renderInputBackground(GuiGraphics guiGraphics, EditBox input, int color) {
        if (input == null) {
            return;
        }
        guiGraphics.fill(input.getX() - 4, input.getY() - 2, input.getX() + input.getWidth() + 4, input.getY() + input.getHeight() + 2, 0xE04A5568);
        guiGraphics.fill(input.getX() - 4, input.getY() - 2, input.getX() + input.getWidth() + 4, input.getY() - 1, color);
        guiGraphics.fill(input.getX() - 4, input.getY() + input.getHeight() + 1, input.getX() + input.getWidth() + 4, input.getY() + input.getHeight() + 2, color);
        guiGraphics.fill(input.getX() - 4, input.getY() - 2, input.getX() - 3, input.getY() + input.getHeight() + 2, color);
        guiGraphics.fill(input.getX() + input.getWidth() + 3, input.getY() - 2, input.getX() + input.getWidth() + 4, input.getY() + input.getHeight() + 2, color);
    }

    private void renderSlotTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        float vx = mouseX / uiScale;
        float vy = mouseY / uiScale;
        for (SlotArea area : slotAreas) {
            if (isHover(vx, vy, area.x(), area.y(), area.x() + SLOT_SIZE, area.y() + SLOT_SIZE)) {
                List<Component> lines = new ArrayList<>(area.stack().getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(player.level()), player,
                        minecraft != null && minecraft.options.advancedItemTooltips ? net.minecraft.world.item.TooltipFlag.ADVANCED : net.minecraft.world.item.TooltipFlag.NORMAL));
                lines.add(Component.literal("点击选择此物品").withStyle(ChatFormatting.GRAY));
                guiGraphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float vx = (float) mouseX / uiScale;
        float vy = (float) mouseY / uiScale;
        for (SlotArea area : slotAreas) {
            if (isHover(vx, vy, area.x(), area.y(), area.x() + SLOT_SIZE, area.y() + SLOT_SIZE)) {
                selectedSlot = area.slot();
                selectedCount = 1;
                updateCountInput();
                return true;
            }
        }

        if (isHover(vx, vy, decBtnX1, decBtnY1, decBtnX2, decBtnY2)) {
            selectedCount--;
            clampSelectedCount();
            updateCountInput();
            return true;
        }
        if (isHover(vx, vy, incBtnX1, incBtnY1, incBtnX2, incBtnY2)) {
            selectedCount++;
            clampSelectedCount();
            updateCountInput();
            return true;
        }
        if (isHover(vx, vy, allBtnX1, allBtnY1, allBtnX2, allBtnY2)) {
            ItemStack selected = getSelectedStack();
            selectedCount = selected.isEmpty() ? 1 : countMatchingItems(selected);
            updateCountInput();
            return true;
        }
        if (isHover(vx, vy, listBtnX1, listBtnY1, listBtnX2, listBtnY2)) {
            listItem();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void updateCountInput() {
        if (countInput != null) {
            countInput.setValue(String.valueOf(selectedCount));
        }
    }

    private void listItem() {
        ItemStack selected = getSelectedStack();
        Optional<Integer> price = parsePrice();
        if (selected.isEmpty()) {
            player.sendSystemMessage(Component.translatable(Util_MessageKeys.LIST_NO_ITEM_IN_HAND_MESSAGE_KEY));
            return;
        }
        if (price.isEmpty() || price.get() <= 0) {
            player.sendSystemMessage(Component.translatable(Util_MessageKeys.LIST_INVALID_PRICE_MESSAGE_KEY));
            return;
        }
        clampSelectedCount();
        if (selectedCount <= 0 || selectedCount > countMatchingItems(selected)) {
            player.sendSystemMessage(Component.translatable(Util_MessageKeys.LIST_INSUFFICIENT_ITEM_MESSAGE_KEY));
            return;
        }
        EconomySystem_NetworkManager.sendToServer(new Packet_CreateSalesOrder(selectedSlot, selectedCount, price.get()));
        if (minecraft != null) {
            minecraft.setScreen(new Screen_Market());
        }
    }

    private boolean isHover(float mouseX, float mouseY, int x1, int y1, int x2, int y2) {
        return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
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

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {}
}

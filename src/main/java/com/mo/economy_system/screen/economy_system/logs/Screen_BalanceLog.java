package com.mo.economy_system.screen.economy_system.logs;

import com.mo.economy_system.common.network.BalanceLogRequestMessage;
import com.mo.economy_system.core.economy_system.BalanceLogEntry;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.screen.Screen_Home;
import com.mo.economy_system.screen.components.CardRenderer;
import com.mo.economy_system.screen.components.UiButtonRenderer;
import com.mo.economy_system.screen.components.UiButtonStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Screen_BalanceLog extends Screen {
    private static final int BASE_WIDTH = 640;
    private static final int BASE_HEIGHT = 360;
    private static final int PADDING = 12;
    private static final int TAB_HEIGHT = 22;
    private static final int ROW_HEIGHT = 24;
    private static final int PAGE_SIZE = 50;
    private static final String[] TABS = {"全部", "指令", "红包", "领地", "市场", "转账", "税费", "系统"};
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final List<BalanceLogEntry> logs = new ArrayList<>();
    private int selectedTab = 0;
    private int scrollOffset = 0;
    private int pageOffset = 0;
    private int pageLimit = PAGE_SIZE;
    private int totalLogs = 0;
    private int prevBtnX1, prevBtnY1, prevBtnX2, prevBtnY2;
    private int nextBtnX1, nextBtnY1, nextBtnX2, nextBtnY2;
    private float uiScale;
    private int virtualWidth;
    private int virtualHeight;

    public Screen_BalanceLog() {
        super(Component.literal("货币日志"));
        requestPage(0);
    }

    public void updateLogs(List<BalanceLogEntry> entries) {
        updateLogs(entries, TABS[selectedTab], pageOffset, pageLimit, entries.size());
    }

    public void updateLogs(List<BalanceLogEntry> entries, String category, int offset, int limit, int total) {
        logs.clear();
        logs.addAll(entries);
        pageOffset = Math.max(0, offset);
        pageLimit = Math.max(1, limit);
        totalLogs = Math.max(0, total);
        for (int i = 0; i < TABS.length; i++) {
            if (TABS[i].equals(category)) {
                selectedTab = i;
                break;
            }
        }
        scrollOffset = 0;
    }

    private void requestPage(int offset) {
        EconomySystem_NetworkManager.sendToServer(
                new BalanceLogRequestMessage(TABS[selectedTab], offset, PAGE_SIZE)
        );
    }

    private void calculateVirtualSize() {
        float scaleX = (float) width / BASE_WIDTH;
        float scaleY = (float) height / BASE_HEIGHT;
        uiScale = Math.min(scaleX, scaleY);
        virtualWidth = (int) (width / uiScale);
        virtualHeight = (int) (height / uiScale);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, width, height, 0xB0000000);
        calculateVirtualSize();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(uiScale, uiScale, 1.0f);
        float virtualMouseX = mouseX / uiScale;
        float virtualMouseY = mouseY / uiScale;

        renderPage(guiGraphics, virtualMouseX, virtualMouseY);
        guiGraphics.pose().popPose();

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderPage(GuiGraphics guiGraphics, float mouseX, float mouseY) {
        int x = PADDING;
        int y = PADDING;
        int w = virtualWidth - PADDING * 2;
        int h = virtualHeight - PADDING * 2;
        CardRenderer.drawCard(guiGraphics, x, y, w, h, CardRenderer.THEME_BALANCE, false);

        guiGraphics.drawString(font, "货币日志", x + 10, y + 8, CardRenderer.TEXT_TITLE);
        String hint = "按 ESC 返回";
        guiGraphics.drawString(font, hint, x + w - 10 - font.width(hint), y + 8, 0x90FFFFFF);

        renderTabs(guiGraphics, mouseX, mouseY, x + 10, y + 28, w - 20);
        renderRows(guiGraphics, x + 10, y + 58, w - 20, h - 96);
        renderPageControls(guiGraphics, mouseX, mouseY, x + 10, y + h - 30, w - 20);
    }

    private void renderTabs(GuiGraphics guiGraphics, float mouseX, float mouseY, int x, int y, int width) {
        int tabWidth = Math.max(52, width / TABS.length);
        for (int i = 0; i < TABS.length; i++) {
            int tabX = x + i * tabWidth;
            boolean active = selectedTab == i;
            boolean hovered = mouseX >= tabX && mouseX <= tabX + tabWidth - 4 && mouseY >= y && mouseY <= y + TAB_HEIGHT;
            UiButtonStyle style = UiButtonStyle.accent(active ? CardRenderer.THEME_BALANCE : 0xFF6F7F8C)
                    .setPadding(6)
                    .setStripeWidth(3)
                    .setGlowHeight(active ? 5 : 0)
                    .setTextShadow(false);
            UiButtonRenderer.drawStripedButton(guiGraphics, font, tabX, y, tabWidth - 4, TAB_HEIGHT, TABS[i], "", style, hovered, UiButtonRenderer.TextAlign.CENTER, false);
        }
    }

    private void renderRows(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        List<BalanceLogEntry> filtered = logs;
        int visibleRows = Math.max(1, height / ROW_HEIGHT);
        scrollOffset = Mth.clamp(scrollOffset, 0, Math.max(0, filtered.size() - visibleRows));

        if (filtered.isEmpty()) {
            String text = "暂无日志";
            guiGraphics.drawString(font, text, x + (width - font.width(text)) / 2, y + 30, 0x80FFFFFF);
            return;
        }

        for (int i = 0; i < visibleRows && i + scrollOffset < filtered.size(); i++) {
            BalanceLogEntry entry = filtered.get(i + scrollOffset);
            int rowY = y + i * ROW_HEIGHT;
            int bg = i % 2 == 0 ? 0x301A2633 : 0x201A2633;
            guiGraphics.fill(x, rowY, x + width, rowY + ROW_HEIGHT - 2, bg);

            String time = TIME_FORMATTER.format(Instant.ofEpochMilli(entry.timeMillis()));
            String delta = (entry.delta() > 0 ? "+" : "") + entry.delta();
            int deltaColor = entry.delta() >= 0 ? 0xFF7CFFB2 : 0xFFFF8A8A;

            guiGraphics.drawString(font, time, x + 6, rowY + 4, 0xFFB8C7D9);
            guiGraphics.drawString(font, entry.category(), x + 82, rowY + 4, 0xFFFFD166);
            guiGraphics.drawString(font, delta, x + 132, rowY + 4, deltaColor);
            guiGraphics.drawString(font, entry.beforeBalance() + " -> " + entry.afterBalance(), x + 188, rowY + 4, 0xFFE8EEF6);

            String reason = CardRenderer.truncateText(font, entry.reason(), Math.max(80, width - 330));
            guiGraphics.drawString(font, reason, x + 320, rowY + 4, 0xFFB8C7D9);
        }
    }

    private void renderPageControls(GuiGraphics guiGraphics, float mouseX, float mouseY, int x, int y, int width) {
        int btnWidth = 58;
        int btnHeight = 20;
        prevBtnX1 = x;
        prevBtnY1 = y;
        prevBtnX2 = x + btnWidth;
        prevBtnY2 = y + btnHeight;
        nextBtnX1 = x + width - btnWidth;
        nextBtnY1 = y;
        nextBtnX2 = x + width;
        nextBtnY2 = y + btnHeight;

        boolean hasPrev = pageOffset > 0;
        boolean hasNext = pageOffset + pageLimit < totalLogs;
        UiButtonStyle prevStyle = UiButtonStyle.accent(hasPrev ? CardRenderer.THEME_BALANCE : 0xFF6F7F8C).setTextShadow(false);
        UiButtonStyle nextStyle = UiButtonStyle.accent(hasNext ? CardRenderer.THEME_BALANCE : 0xFF6F7F8C).setTextShadow(false);
        UiButtonRenderer.drawStripedButton(guiGraphics, font, prevBtnX1, prevBtnY1, btnWidth, btnHeight, "上一页", "", prevStyle,
                hasPrev && mouseX >= prevBtnX1 && mouseX <= prevBtnX2 && mouseY >= prevBtnY1 && mouseY <= prevBtnY2,
                UiButtonRenderer.TextAlign.CENTER, false);
        UiButtonRenderer.drawStripedButton(guiGraphics, font, nextBtnX1, nextBtnY1, btnWidth, btnHeight, "下一页", "", nextStyle,
                hasNext && mouseX >= nextBtnX1 && mouseX <= nextBtnX2 && mouseY >= nextBtnY1 && mouseY <= nextBtnY2,
                UiButtonRenderer.TextAlign.CENTER, false);

        int currentPage = totalLogs == 0 ? 0 : pageOffset / pageLimit + 1;
        int totalPages = totalLogs == 0 ? 0 : (totalLogs + pageLimit - 1) / pageLimit;
        String pageText = currentPage + " / " + totalPages + "  共 " + totalLogs + " 条";
        guiGraphics.drawString(font, pageText, x + (width - font.width(pageText)) / 2, y + 6, 0xB8FFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float vx = (float) mouseX / uiScale;
        float vy = (float) mouseY / uiScale;
        int x = PADDING + 10;
        int y = PADDING + 28;
        int width = virtualWidth - PADDING * 2 - 20;
        int tabWidth = Math.max(52, width / TABS.length);
        for (int i = 0; i < TABS.length; i++) {
            int tabX = x + i * tabWidth;
            if (vx >= tabX && vx <= tabX + tabWidth - 4 && vy >= y && vy <= y + TAB_HEIGHT) {
                selectedTab = i;
                scrollOffset = 0;
                requestPage(0);
                return true;
            }
        }
        if (vx >= prevBtnX1 && vx <= prevBtnX2 && vy >= prevBtnY1 && vy <= prevBtnY2 && pageOffset > 0) {
            requestPage(Math.max(0, pageOffset - pageLimit));
            return true;
        }
        if (vx >= nextBtnX1 && vx <= nextBtnX2 && vy >= nextBtnY1 && vy <= nextBtnY2 && pageOffset + pageLimit < totalLogs) {
            requestPage(pageOffset + pageLimit);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int visibleRows = Math.max(1, (virtualHeight - PADDING * 2 - 96) / ROW_HEIGHT);
        int maxScroll = Math.max(0, logs.size() - visibleRows);
        scrollOffset = Mth.clamp(scrollOffset - (int) Math.signum(scrollY), 0, maxScroll);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (minecraft != null) {
                minecraft.setScreen(new Screen_Home());
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
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }
}

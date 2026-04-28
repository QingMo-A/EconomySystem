package com.mo.economy_system.screen.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HighLevelTextField extends EditBox {
    private final List<String> suggestions = new ArrayList<>();
    private List<String> currentMatches = new ArrayList<>();
    protected final Font font;
    private int selectedSuggestion = -1;

    // 添加一个起始显示索引
    private int startIndex = 0;
    private static final int MAX_DISPLAY_ITEMS = 5;

    public HighLevelTextField(Font font, int x, int y, int width, int height) {
        super(font, x, y, width, height, Component.empty());
        this.font = font;
        // 监听文本变化
        this.setResponder(text -> updateSuggestions(text));
    }

    public HighLevelTextField(Font font, int x, int y, int width, int height, Component component) {
        super(font, x, y, width, height, component);
        this.font = font;
        // 监听文本变化
        this.setResponder(text -> updateSuggestions(text));
    }

    // 设置补全数据源
    public void setSuggestions(List<String> suggestions) {
        this.suggestions.clear();
        this.suggestions.addAll(suggestions);
    }

    private void updateSuggestions(String input) {
        currentMatches.clear();
        if (!input.isEmpty()) {
            currentMatches = suggestions.stream()
                    .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                    .collect(Collectors.toList());
        }
        selectedSuggestion = -1; // 重置选择
        startIndex = 0; // 每次更新时将显示从头开始
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_TAB && !currentMatches.isEmpty()) {
            // 使用当前选中的建议项来填充文本框
            if (selectedSuggestion != -1) {
                this.setValue(currentMatches.get(selectedSuggestion));
            } else {
                this.setValue(currentMatches.get(0)); // 如果没有选中任何项，则默认填充第一个
            }
            this.setHighlightPos(this.getValue().length());
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_DOWN) {
            // 方向键选择下一个建议项
            if (selectedSuggestion < currentMatches.size() - 1) {
                selectedSuggestion++;
                // 自动滚动
                if (selectedSuggestion >= startIndex + MAX_DISPLAY_ITEMS) {
                    startIndex++;
                }
            }
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_UP) {
            // 方向键选择上一个建议项
            if (selectedSuggestion > 0) {
                selectedSuggestion--;
                // 自动滚动
                if (selectedSuggestion < startIndex) {
                    startIndex--;
                }
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(gui, mouseX, mouseY, partialTick);

        if (isFocused() && !currentMatches.isEmpty()) {
            int listY = this.getY() + this.height;
            int maxItems = MAX_DISPLAY_ITEMS; // 最多显示5条

            // 计算显示区域的高度，如果匹配项更多，则展示更多项
            int itemsToShow = Math.min(maxItems, currentMatches.size() - startIndex);
            int listHeight = 10 * itemsToShow + 2;

            // 设置透明背景色，0xAARRGGBB 格式，A（Alpha）为透明度，FF 是不透明，80 是半透明
            int backgroundColor = 0x80202020; // 半透明的背景色（50%透明）

            // 绘制背景
            gui.fill(this.getX(), listY,
                    this.getX() + this.width,
                    listY + listHeight,
                    backgroundColor);

            // 绘制建议条目
            for (int i = 0; i < itemsToShow; i++) {
                String s = currentMatches.get(startIndex + i);
                int color = (i == selectedSuggestion - startIndex) ? 0xFF00FF00 : 0xFFFFFFFF;
                gui.drawString(this.font, s, this.getX() + 4, listY + 2 + 10 * i, color);
            }
        }
    }
}

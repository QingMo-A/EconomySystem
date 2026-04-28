package com.mo.economy_system.screen.components.newUI;

import net.minecraft.client.gui.components.AbstractWidget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PageManager {
    private final Map<String, PageContainer> pageCache = new HashMap<>();
    private final Map<String, Runnable> pageBuilders = new HashMap<>();
    private PageContainer currentPage;

    public void registerPage(String pageId, Runnable builder) {
        pageBuilders.put(pageId, builder);
    }

    public void switchToPage(String pageId) {
        // 获取或创建页面
        PageContainer page = pageCache.get(pageId);
        if (page == null) {
            page = new PageContainer(pageId);
            pageCache.put(pageId, page);
            pageBuilders.get(pageId).run(); // 构建页面内容
        }

        // 切换页面
        if (currentPage != null) {
            currentPage.setVisible(false);
        }
        currentPage = page;
        currentPage.setVisible(true);
    }

    // 页面容器内部类
    private static class PageContainer {
        private final String pageId;
        private final List<AbstractWidget> widgets = new ArrayList<>();
        private boolean visible;

        public PageContainer(String pageId) {
            this.pageId = pageId;
        }

        public void addWidget(AbstractWidget widget) {
            widgets.add(widget);
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
            for (AbstractWidget widget : widgets) {
                widget.visible = visible;
            }
        }

        public void clear() {
            widgets.clear();
        }
    }
}

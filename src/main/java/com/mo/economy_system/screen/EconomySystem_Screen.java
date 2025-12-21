package com.mo.economy_system.screen;

import com.mo.economy_system.core.economy_system.delivery_box.DeliveryItem;
import com.mo.economy_system.core.economy_system.market.MarketItem;
import com.mo.economy_system.core.economy_system.shop.ShopItem;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.screen.components.ItemIconAnimation;
import com.mo.economy_system.screen.components.TextAnimation;
import com.mo.economy_system.screen.newUI.ItemIconWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EconomySystem_Screen extends Screen {
    protected List<MarketItem> marketItems = new ArrayList<>();
    protected List<MarketItem> marketItemsSnapshot = new ArrayList<>();

    protected List<ShopItem> shopItems = new ArrayList<>(); // 商品列表
    protected List<ShopItem> shopItemsSnapshot = new ArrayList<>();

    protected final List<ItemIconWidget> itemWidgets = new ArrayList<>();

    protected List<DeliveryItem> deliveryItems = new ArrayList<>(); // 物品列表
    protected List<DeliveryItem> deliveryItemsSnapshot = new ArrayList<>();

    protected List<Territory> allTerritories = new ArrayList<>(); // 拥有的领地
    protected List<Territory> territorys = new ArrayList<>(); // 商品列表
    protected List<Territory> ownedTerritories = new ArrayList<>(); // 拥有的领地
    protected List<Territory> authorizedTerritories = new ArrayList<>(); // 有权限的领地

    protected final String PAGE_ID_HOME = "HOME";
    protected final String PAGE_ID_STORE = "STORE";
    protected final String PAGE_ID_MARKET = "MARKET";
    protected final String PAGE_ID_DELIVERY_BOX = "DELIVERY_BOX";
    protected final String PAGE_ID_TERRITORIES = "TERRITORIES";
    protected final String PAGE_ID_ABOUT = "ABOUT";

    protected final String PAGE_NAME_HOME = "title.home";
    protected final String PAGE_NAME_STORE = "title.store";
    protected final String PAGE_NAME_MARKET = "title.market";
    protected final String PAGE_NAME_DELIVERY_BOX = "title.delivery_box";
    protected final String PAGE_NAME_TERRITORIES = "title.territories";
    protected final String PAGE_NAME_ABOUT = "title.about";

    protected String currentPage = PAGE_ID_HOME;
    protected String currentTitle = PAGE_NAME_HOME;

    protected int balance = -1;
    protected List<Map.Entry<String, Integer>> accounts;

    protected int currentPageNumber = 0; // 当前页码
    protected static int TOP_MARGIN; // 距离底部的最小空白高度
    protected static final int BOTTOM_MARGIN = 60; // 距离底部的最小空白高度
    protected int thingsPerPage; // 动态计算的每页东西
    protected final int THING_SPACING = 35; // 动态调整的垂直间距
    protected List<RunnableWithGraphics> renderCache = new ArrayList<>();
    protected int PAGE_BUTTON_WIDTH = 40;
    protected int PAGE_BUTTON_HEIGHT = 20;
    protected int startIndex;
    protected int endIndex;
    protected int startX;
    protected int startY;

    protected int flag = 0;

    protected EconomySystem_Screen(Component title) {
        super(title);
    }

    // 初始化渲染缓存
    protected void initializeRenderCache() {}

    // 初始化坐标
    protected void initPosition() {}

    // 初始化部分
    protected void initPart() {}

    // 根据鼠标位置渲染描述
    protected void detectMouseHoverAndRenderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {}

    protected void addPageAnimatedButtons() {

    }

    protected void addPageButtons() {

    }

    @Override
    public boolean isPauseScreen() {
        return false; // 界面打开时不暂停游戏
    }

    // 检测鼠标位置
    protected boolean isMouseOver(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @FunctionalInterface
    protected interface RunnableWithGraphics {
        void run(GuiGraphics guiGraphics);
    }

    // 渲染动画文本
    protected void renderAnimatedText(GuiGraphics guiGraphics, Component text, TextAnimation animation) {
        // 计算当前属性
        int x = animation.getCurrentX();
        int y = animation.getCurrentY();
        float alpha = animation.getCurrentAlpha();

        // 设置透明度（ARGB格式：0xAARRGGBB）
        int color = 0xFFFFFF | ((int) (alpha * 255) << 24);

        // 文字绘制
        guiGraphics.drawString(
                minecraft.font,
                text,
                x,
                y,
                color,
                true // 启用阴影
        );
    }

    // 渲染自定义颜色的动画文本
    protected void renderAnimatedText(GuiGraphics guiGraphics, Component text, TextAnimation animation, int customHexColor) {
        // 计算当前属性
        int x = animation.getCurrentX();
        int y = animation.getCurrentY();
        float alpha = animation.getCurrentAlpha();

        // 解析并应用自定义颜色
        int rgb = customHexColor & 0x00FFFFFF; // 提取RGB部分（忽略原始透明度）
        int alphaChannel = (int) (alpha * 255) << 24; // 将动画透明度转为ARGB的Alpha通道
        int finalColor = alphaChannel | rgb; // 合并颜色和透明度

        // 文字绘制
        guiGraphics.drawString(
                minecraft.font,
                text,
                x,
                y,
                finalColor,
                true // 启用阴影
        );
    }

    // 渲染动画图标
    protected void renderAnimatedItem(GuiGraphics guiGraphics, ItemStack itemStack, ItemIconAnimation animation) {
        if (animation == null) return;

        // 更新动画状态
        boolean isCompleted = animation.update();

        // 应用动画参数
        int x = animation.getCurrentX();
        int y = animation.getCurrentY();
        float alpha = animation.getCurrentAlpha();
        float scale = animation.getCurrentScale();

        // 设置透明度（需支持透明渲染）
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1f);
        guiGraphics.setColor(1f, 1f, 1f, alpha);
        guiGraphics.renderItem(itemStack, 0, 0);
        guiGraphics.setColor(1f, 1f, 1f, 1f); // 重置颜色
        guiGraphics.pose().popPose();
    }

    protected boolean isInHoverArea(double mouseX, double mouseY, AbstractWidget widget) {
        return mouseX >= widget.getX() && mouseX <= widget.getX() + widget.getWidth() &&
                mouseY >= widget.getY() && mouseY <= widget.getY() + widget.getHeight();
    }
}

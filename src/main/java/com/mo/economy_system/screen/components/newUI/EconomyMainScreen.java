package com.mo.economy_system.screen.components.newUI;

import com.mo.economy_system.core.economy_system.market.MarketItem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.economy_system.Packet_BalanceRequest;
import com.mo.economy_system.network.packets.economy_system.Packet_MarketDataRequest;
import com.mo.economy_system.network.packets.economy_system.Packet_ShopDataRequest;
import com.mo.economy_system.screen.EconomySystem_Screen;
import com.mo.economy_system.utils.Util_Message;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EconomyMainScreen extends EconomySystem_Screen {
    // UI组件
//    private TitleBarWidget titleBar;
//    private SidebarWidget sidebar;
//    private StatusBarWidget statusBar;
//    private PageContainer mainContent;

    private HBoxWidget titleBar;
    private VBoxWidget sideBar;
    private HBoxWidget actionBar;
    private VBoxWidget mainPane;

    private LocalPlayer player;

    // 页面管理器
    private final PageManager pageManager = new PageManager();

    // 页面ID
    public static final String PAGE_HOME = "home";
    public static final String PAGE_MARKET = "market";
    public static final String PAGE_STORE = "store";
    public static final String PAGE_DELIVERY = "delivery";
    public static final String PAGE_TERRITORY = "territory";
    public static final String PAGE_ABOUT = "about";

    // 当前状态
    protected String currentPageId = PAGE_HOME;
    protected int playerBalance = -1;

    public EconomyMainScreen() {
        super(Component.translatable("title.economy_system"));
    }

    @Override
    protected void init() {
        super.init();

        if (this.minecraft != null) {
            if (this.minecraft.player != null) {
                this.player = this.minecraft.player;
            }
        }

        // 初始化UI框架（只初始化一次）
        if (titleBar == null) {
            initializeUIFramework();
        }

        // 切换页面
        switchPage(currentPageId);

        // 更新数据
        // refreshCurrentPageData();
    }

    private void initializeUIFramework() {
        int width = this.width;
        int height = this.height;

        // 标题栏（顶部）
        titleBar = new HBoxWidget(0, 0, 20)
                .setSpacing(10)
                .setPadding(6, 10, 6, 10)
                .setBorderColor(0x55FFFFFF)
                .showBorder(true, true, false, true)
                .setBorderThickness(2)
                .setBackgroundColor(0x50FFFFFF)
                .setBoxWidth(this.width)
                .setBoxHeight(30)
                .enableScrollbar(true);

        LabelWidget label = new LabelWidget(font, Component.translatable(this.currentTitle), 0, 0, 0xFFFFFF, true)
                .setScale(1.5f);
        titleBar.setMargin(label, new HBoxWidget.Insets(3, 0, 0, 0));

        AbstractWidget growSpacer = new AbstractWidget(0, 0, 0, 1, null) {
            @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
            @Override protected void updateWidgetNarration(NarrationElementOutput output) {}
        };
        titleBar.setHGrow(growSpacer, true);

        titleBar.addAllChildren(
                label,
                growSpacer
        );

        this.addRenderableWidget(titleBar);

        // 操作栏(底部)
        actionBar = new HBoxWidget(0, this.height - 30, 30)
                .setSpacing(10)
                .setPadding(6, 10, 6, 10)
                .setBorderColor(0x55FFFFFF)
                .showBorder(true, true, true, true)
                .setBorderThickness(2)
                .setBackgroundColor(0x50FFFFFF)
                .setBoxWidth(this.width)
                .setBoxHeight(30)
                .enableScrollbar(true);

        LabelWidget label2 = new LabelWidget(font, Component.literal(String.valueOf(balance)), 0, 0, 0xFFFFFF, true)
                .setScale(1.5f);
        actionBar.setMargin(label2, new HBoxWidget.Insets(3, 0, 0, 0));

        AbstractWidget growSpacer2 = new AbstractWidget(0, 0, 0, 1, null) {
            @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
            @Override protected void updateWidgetNarration(NarrationElementOutput output) {}
        };
        actionBar.setHGrow(growSpacer2, true);

        actionBar.addAllChildren(
                label2,
                growSpacer2
        );

        this.addRenderableWidget(actionBar);

        // 侧边栏（左侧）
        sideBar = new VBoxWidget(0, titleBar.getY() + titleBar.getHeight(), 70, getSideBarHeight(height))
                .setSpacing(10)
                .setPadding(10, 5, 10, 5)
                .setBorderColor(0x55FFFFFF)
                .showBorder(true, false, false, true)
                .setBorderThickness(2)
                .setBackgroundColor(0x50FFFFFF)
                .enableScrollbar(true);

        LabelWidget label3 = new LabelWidget(font, Component.literal("v1.0"), 0, 0, 0xFFFFFF, true);
        HBoxWidget h = new HBoxWidget(30)
                .setSpacing(10)
                .setPadding(6, 10, 6, 10)
                .setBorderColor(0x55AAAAAA)
                .showBorder(true, true, true, true)
                .setBorderThickness(2)
                .setBoxWidth(60)
                .setBackgroundColor(0x50AAAAAA)
                .enableScrollbar(true);
        h.setMargin(label3, new HBoxWidget.Insets(1, 0, 0, 6));
        h.addChild(label3);

        AbstractWidget spacer = new AbstractWidget(0, 0, 0, 1, null) {
            @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
            @Override protected void updateWidgetNarration(NarrationElementOutput output) {}
        };
        sideBar.setVGrow(spacer, true);

        sideBar.addAllChildren(
                new Button.Builder(Component.literal("Home"), b -> {
                    switchPage(PAGE_ID_HOME);
                })
                        .tooltip(Tooltip.create(Component.literal("Open Home Page")))
                        .size(60, 20).build(),
                new Button.Builder(Component.literal("Store"), b -> {
                    switchPage(PAGE_ID_STORE);
                })
                        .tooltip(Tooltip.create(Component.literal("Open Store Page")))
                        .size(60, 20).build(),
                new Button.Builder(Component.literal("Market"), b -> {
                    switchPage(PAGE_ID_MARKET);
                })
                        .tooltip(Tooltip.create(Component.literal("Open Market Page")))
                        .size(60, 20).build(),
                new Button.Builder(Component.literal("Delivery Box"), b -> {
                    switchPage(PAGE_ID_DELIVERY_BOX);
                })
                        .tooltip(Tooltip.create(Component.literal("Open Delivery Box Page")))
                        .size(60, 20).build(),
                new Button.Builder(Component.literal("Territories"), b -> {
                    switchPage(PAGE_ID_TERRITORIES);
                })
                        .tooltip(Tooltip.create(Component.literal("Open Territories Page")))
                        .size(60, 20).build(),
                new Button.Builder(Component.literal("About"), b -> {
                    switchPage(PAGE_ID_ABOUT);
                })
                        .tooltip(Tooltip.create(Component.literal("Open About Page")))
                        .size(60, 20).build(),
                spacer,
                h
        );

        this.addRenderableWidget(sideBar);

        // 主内容区域
        // mainPane = new PageContainer(70, 30, width - 70, height - 60);
        mainPane = new VBoxWidget(sideBar.getWidth(), titleBar.getY() + titleBar.getHeight(),
                this.width - sideBar.getWidth())
                .setSpacing(10)
                .setPadding(10, 10, 10, 10)
                .setBorderColor(0x55FFFFFF)
                .showBorder(true, true, false, true)
                .setBorderThickness(2)
                .setBackgroundColor(0x30FFFFFF)
                .enableScrollbar(true);

        addRenderableWidget(mainPane);

        // 注册页面构建器
        // registerPages();
    }

    /*private void registerPages() {
        // 首页
        pageManager.registerPage(PAGE_HOME, this::buildHomePage);

        // 市场页面（懒加载）
        pageManager.registerPage(PAGE_MARKET, () -> {
            MarketPageBuilder builder = new MarketPageBuilder(mainContent);
            builder.setMarketItems(marketItems);
            builder.build();
        });

        // 商店页面（懒加载）
        pageManager.registerPage(PAGE_STORE, () -> {
            StorePageBuilder builder = new StorePageBuilder(mainContent);
            builder.setShopItems(shopItems);
            builder.build();
        });

        // 其他页面...
    }*/

    private void switchPage(String pageId) {
        if (currentPageId.equals(pageId)) return;

        // 保存当前页面状态（如果需要）
        saveCurrentPageState();

        // 切换页面
        currentPageId = pageId;
        // titleBar.setTitle(getCurrentTitle());

        // 使用页面管理器切换
        pageManager.switchToPage(pageId);

        // 发送数据请求
        requestPageData(pageId);
    }

    private void saveCurrentPageState() {
        // 保存当前页面的滚动位置、筛选条件等
        // 例如：marketPage.saveState();
    }

    private void requestPageData(String pageId) {
        // 根据页面请求数据
        switch (pageId) {
            case PAGE_HOME:
                EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_BalanceRequest());
                break;
            case PAGE_MARKET:
                EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_MarketDataRequest());
                break;
            case PAGE_STORE:
                EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_ShopDataRequest());
                break;
            // ... 其他页面
        }
    }

    private String getCurrentTitle() {
        return switch (currentPageId) {
            case PAGE_HOME -> "title.home";
            case PAGE_MARKET -> "title.market";
            case PAGE_STORE -> "title.store";
            case PAGE_DELIVERY -> "title.delivery_box";
            case PAGE_TERRITORY -> "title.territories";
            case PAGE_ABOUT -> "title.about";
            default -> "title.home";
        };
    }

    // 数据更新方法
    public void updateMarketItems(List<MarketItem> items) {
        this.marketItems = new ArrayList<>(items);

        // 如果当前是市场页面，更新显示
        if (PAGE_MARKET.equals(currentPageId)) {
            // pageManager.refreshPage(PAGE_MARKET);
        }
    }

    public void updateBalance(int balance, List<Map.Entry<String, Integer>> accounts) {
        this.playerBalance = balance;
        if (actionBar != null) {
            this.balance = balance;
            this.accounts = accounts;
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /*
    * UI构造方法
    */

    private int getSideBarHeight(int height) {
        if (titleBar != null && actionBar != null) {
            int sideBarHeight = this.height - titleBar.getHeight() - actionBar.getHeight();
            Util_Message.sendDebugMessage("屏幕高度为: " + this.height);
            Util_Message.sendDebugMessage("titleBar高度为: " + titleBar.getHeight());
            Util_Message.sendDebugMessage("actionBar高度为: " + actionBar.getHeight());
            Util_Message.sendDebugMessage("SideBar高度为: " + sideBarHeight);
            return sideBarHeight;
        }
        return 0;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
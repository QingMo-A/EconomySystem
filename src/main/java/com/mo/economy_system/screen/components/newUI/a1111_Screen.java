package com.mo.economy_system.screen.components.newUI;

import com.mo.economy_system.core.economy_system.delivery_box.DeliveryItem;
import com.mo.economy_system.core.economy_system.market.MarketItem;
import com.mo.economy_system.core.economy_system.market.SalesOrder;
import com.mo.economy_system.core.economy_system.shop.ShopItem;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.economy_system.Packet_BalanceRequest;
import com.mo.economy_system.network.packets.economy_system.Packet_DeliveryBoxDataRequest;
import com.mo.economy_system.network.packets.economy_system.Packet_MarketDataRequest;
import com.mo.economy_system.network.packets.economy_system.Packet_ShopDataRequest;
import com.mo.economy_system.network.packets.territory_system.Packet_TerritoryDataRequest;
import com.mo.economy_system.screen.EconomySystem_Screen;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

public class a1111_Screen extends EconomySystem_Screen {

    private LocalPlayer player;

    private List<MarketItem> marketItems = new ArrayList<>();
    private List<MarketItem> marketItemsSnapshot = new ArrayList<>();

    private List<ShopItem> shopItems = new ArrayList<>(); // 商品列表
    private List<ShopItem> shopItemsSnapshot = new ArrayList<>();

    private final List<ItemIconWidget> itemWidgets = new ArrayList<>();

    private List<DeliveryItem> deliveryItems = new ArrayList<>(); // 物品列表
    private List<DeliveryItem> deliveryItemsSnapshot = new ArrayList<>();

    private List<Territory> allTerritories = new ArrayList<>(); // 拥有的领地
    private List<Territory> territorys = new ArrayList<>(); // 商品列表
    private List<Territory> ownedTerritories = new ArrayList<>(); // 拥有的领地
    private List<Territory> authorizedTerritories = new ArrayList<>(); // 有权限的领地

    private HBoxWidget titleBar;
    private VBoxWidget sideBar;
    private HBoxWidget actionBar;
    private VBoxWidget mainPane;

    private int lastTopY = -1;
    private int lastVBoxHeight = -1;
    private int lastSidebarX = -1;
    private int lastSidebarY = -1;

    public static final String PAGE_ID_HOME = "HOME";
    public static final String PAGE_ID_STORE = "STORE";
    public static final String PAGE_ID_MARKET = "MARKET";
    public static final String PAGE_ID_DELIVERY_BOX = "DELIVERY_BOX";
    public static final String PAGE_ID_TERRITORIES = "TERRITORIES";
    public static final String PAGE_ID_ABOUT = "ABOUT";

    private final String PAGE_NAME_HOME = "title.home";
    private final String PAGE_NAME_STORE = "title.store";
    private final String PAGE_NAME_MARKET = "title.market";
    private final String PAGE_NAME_DELIVERY_BOX = "title.delivery_box";
    private final String PAGE_NAME_TERRITORIES = "title.territories";
    private final String PAGE_NAME_ABOUT = "title.about";

    private String currentPage = PAGE_ID_HOME;
    private String currentTitle = PAGE_NAME_HOME;

    private int balance = -1;
    private List<Map.Entry<String, Integer>> accounts;

    public a1111_Screen() {
        super(Component.literal("Test UI Screen"));
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_BalanceRequest());
        // EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_MarketDataRequest());
    }

    public a1111_Screen(String startPage) {
        super(Component.literal("Test UI Screen"));
        setStartPage(startPage);
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_BalanceRequest());
        if (PAGE_ID_STORE.equals(startPage)) {
            EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_ShopDataRequest());
        } else if (PAGE_ID_MARKET.equals(startPage)) {
            EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_MarketDataRequest());
        }
    }

    @Override
    protected void init() {

        if (this.minecraft != null) {
            if (this.minecraft.player != null) {
                this.player = this.minecraft.player;
            }
        }

        // 清除现有按钮
        this.clearWidgets();
        
        initializeRenderCache();

    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (titleBar != null && !isInHoverArea(mouseX, mouseY, titleBar)) {
            titleBar.clearHoverHold();
        }
        if (sideBar != null && !isInHoverArea(mouseX, mouseY, sideBar)) {
            sideBar.clearHoverHold();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    protected boolean isInHoverArea(double mouseX, double mouseY, AbstractWidget widget) {
        return mouseX >= widget.getX() && mouseX <= widget.getX() + widget.getWidth() &&
                mouseY >= widget.getY() && mouseY <= widget.getY() + widget.getHeight();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        // 计算 VBox 位置和高度
        int topY = titleBar.getY() + titleBar.getHeight();
        int bottomY = actionBar.getY();
        int vboxHeight = bottomY - topY;
        int currentSidebarX = sideBar.getX();  // 动画变化的 X
        int currentSidebarY = sideBar.getY();  // 动画变化的 Y


        // 仅当值变化时更新（避免 setX 被 reset）
        if (topY != lastTopY || vboxHeight != lastVBoxHeight ||
                currentSidebarX != mainPane.getX() || currentSidebarY != sideBar.getY()) {

            sideBar.setY(topY);
            sideBar.setBoxHeight(vboxHeight);

            // 更新 mainPane 的位置和大小
            mainPane.setX(sideBar.getX() + sideBar.getWidth());
            mainPane.setY(topY);
            mainPane.setBoxWidth(this.width - sideBar.getX() - sideBar.getWidth());
            mainPane.setBoxHeight(vboxHeight);

            for (AbstractWidget widget : mainPane.getChildren()) {
                if (widget instanceof HBoxWidget itemRow) {
                    itemRow.setBoxWidth(mainPane.getWidth() - mainPane.getPaddingRight() - mainPane.getPaddingLeft());
                }
            }


            int paddingLeft = sideBar.getX() + sideBar.getWidth()  == 0 ? mainPane.getPaddingLeft() : 0;
            int paddingTop = titleBar.getY() + titleBar.getHeight() == 0 ? mainPane.getPaddingTop() : 0;

            sideBar.setHoverTriggerArea(0, topY, sideBar.getX() + sideBar.getWidth() + paddingLeft, vboxHeight);
            // 动态更新 hbox 的 hover 区域
            titleBar.setHoverTriggerArea(0, 0, titleBar.getWidth(), titleBar.getY() + titleBar.getHeight() + paddingTop);

            // 更新动画 Y 区域（X 不变）
            sideBar.getAnimationController()
                    .enable(true)
                    .setDuration(600)
                    .setLeaveDelay(700)
                    .setPositions(-70, topY, 0, topY);

            lastTopY = topY;
            lastVBoxHeight = vboxHeight;
            lastSidebarX = currentSidebarX;
            lastSidebarY = currentSidebarY;

        }

        // 检查哪个ItemIconWidget被悬停
        for (ItemIconWidget widget : itemWidgets) {
            if (widget.isHovered()) {
                graphics.renderTooltip(this.font, widget.getTooltipLines(), Optional.empty(), mouseX, mouseY);
                break;
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void initializeRenderCache() {
        renderCache.clear();
        itemWidgets.clear();

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

        titleBar.setHoverTriggerArea(0, 0, this.width, 30);
        titleBar.getAnimationController()
                .enable(true)
                .setDuration(600)
                .setLeaveDelay(700)
                .setPositions(0, -30, 0, 0);

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

        // 提前初始化 hBox2，因为后面需要用它计算 vbox 高度
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

        // 初始化 VBox（Y 和高度后续在 render 中动态更新）
        sideBar = new VBoxWidget(0, titleBar.getY() + titleBar.getHeight(), 70)
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

        mainPane = new VBoxWidget(sideBar.getWidth(), titleBar.getY() + titleBar.getHeight(),
                this.width - sideBar.getWidth())
                .setSpacing(10)
                .setPadding(10, 10, 10, 10)
                .setBorderColor(0x55FFFFFF)
                .showBorder(true, true, false, true)
                .setBorderThickness(2)
                .setBackgroundColor(0x30FFFFFF)
                .enableScrollbar(true);

        if (this.currentPage.equals(PAGE_ID_HOME)) {

        } else if (this.currentPage.equals(PAGE_ID_STORE)) {
            buildShopLayout();

        } else if (this.currentPage.equals(PAGE_ID_MARKET)) {
            buildMarketLayout();

        } else if (this.currentPage.equals(PAGE_ID_DELIVERY_BOX)) {

        } else if (this.currentPage.equals(PAGE_ID_TERRITORIES)) {

        } else if (this.currentPage.equals(PAGE_ID_ABOUT)) {

        }

        this.addRenderableWidget(mainPane);

        super.initializeRenderCache();
    }

    public void updateBalance(int balance, List<Map.Entry<String, Integer>> accounts) {
        this.balance = balance;
        this.accounts = accounts;
        this.init();
    }

    public void updateMarketItems() {
        List<MarketItem> items = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            items.add(new SalesOrder(
                    UUID.randomUUID(),
                    Items.DIAMOND_AXE.getDescriptionId(),
                    Items.DIAMOND_AXE.getDefaultInstance(),
                    5,
                    player.getName().getString(),
                    player.getUUID(),
                    System.currentTimeMillis()
            ));
        }
        this.marketItems = new ArrayList<>(items); // 初始化过滤后的列表
        this.marketItemsSnapshot = new ArrayList<>(items); // 初始化过滤后的列表
        this.init(); // 每次更新市场物品后重新初始化界面
    }

    public void updateMarketItems(List<MarketItem> items) {
        this.marketItems = new ArrayList<>(items); // 初始化过滤后的列表
        this.marketItemsSnapshot = new ArrayList<>(items); // 初始化过滤后的列表
        this.init(); // 每次更新市场物品后重新初始化界面
    }

    public void updateShopItems(List<ShopItem> items) {
        this.shopItems = new ArrayList<>(items);
        this.shopItemsSnapshot = new ArrayList<>(items);
        this.init(); // 每次更新商店物品后重新初始化界面
    }

    public void updateDeliveryBoxItems(List<DeliveryItem> deliveryItems) {
        this.deliveryItems = new ArrayList<>(deliveryItems);
        this.deliveryItemsSnapshot = new ArrayList<>(deliveryItems); // 初始化过滤后的列表
        this.init(); // 每次更新物品后重新初始化界面
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void switchPage(String pageID) {
        if (!(this.currentPage.equals(pageID))) {
            switch (pageID){
                case PAGE_ID_HOME:
                    buildHomePage();
                    break;
                case PAGE_ID_STORE:
                    buildStorePage();
                    break;
                case PAGE_ID_MARKET:
                    buildMarketPage();
                    break;
                case PAGE_ID_DELIVERY_BOX:
                    buildDeliveryBoxPage();
                    break;
                case PAGE_ID_TERRITORIES:
                    buildTerritoriesPage();
                    break;
                case PAGE_ID_ABOUT:
                    buildAboutPage();break;
                default:
                    break;
            }
        }
    }

    private void buildHomePage() {
        System.out.println("点击了按钮 Home");
        this.currentTitle = PAGE_NAME_HOME;
        this.currentPage = PAGE_ID_HOME;

        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_BalanceRequest());
    }

    private void buildStorePage() {
        System.out.println("点击了按钮 Store");
        this.currentTitle = PAGE_NAME_STORE;
        this.currentPage = PAGE_ID_STORE;

        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_BalanceRequest());
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_ShopDataRequest());
    }

    private void buildMarketPage() {
        System.out.println("点击了按钮 Market");
        this.currentTitle = PAGE_NAME_MARKET;
        this.currentPage = PAGE_ID_MARKET;

        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_BalanceRequest());
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_MarketDataRequest());

        // updateMarketItems();
    }

    private void buildDeliveryBoxPage() {
        System.out.println("点击了按钮 DeliveryBox");
        this.currentTitle = PAGE_NAME_DELIVERY_BOX;
        this.currentPage = PAGE_ID_DELIVERY_BOX;

        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_BalanceRequest());
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_DeliveryBoxDataRequest());
    }

    private void buildTerritoriesPage() {
        System.out.println("点击了按钮 Territories");
        this.currentTitle = PAGE_NAME_TERRITORIES;
        this.currentPage = PAGE_ID_TERRITORIES;

        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_BalanceRequest());
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_TerritoryDataRequest());
    }

    private void buildAboutPage() {
        System.out.println("点击了按钮 About");
        this.currentTitle = PAGE_NAME_ABOUT;
        this.currentPage = PAGE_ID_ABOUT;

        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_BalanceRequest());
    }

    private HBoxWidget buildItemRow(ItemStack stack, int rowHeight, int rowWidth, List<Component> tooltipLines) {
        HBoxWidget itemRow = new HBoxWidget(0, 0, 0)
                .setSpacing(7)
                .setPadding(5, 10, 5, 10)
                .setBorderColor(0x22FFFFFF)
                .setBoxWidth(rowWidth)
                .setBoxHeight(rowHeight)
                .showBorder(true, false, true, false)
                .setBorderThickness(1);

        ItemIconWidget icon = new ItemIconWidget(stack, font, 0, 0)
                .setScale(1.3f)
                .setShowDecorations(true)
                .setTooltipLines(tooltipLines);
        itemRow.addChild(icon);

        VBoxWidget infoBox = new VBoxWidget(0, 0, 200)
                .setSpacing(2)
                .setBoxHeight(rowHeight)
                .setPadding(2, 2, 2, 2);

        LabelWidget nameLabel = new LabelWidget(font, stack.getHoverName(), 0, 0, 0xFFFFFF, true)
                .setScale(1.0f);
        infoBox.addChild(nameLabel);

        return itemRow;
    }

    private void buildMarketLayout() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        HBoxWidget toolbar = buildToolbar(Component.literal("玩家市场"), marketItems.size(), 0xFF66CCFF);
        EditBox searchBox = buildSearchBox(Component.literal("搜索市场物品"));
        toolbar.addChild(searchBox);
        toolbar.setHGrow(searchBox, true);
        toolbar.addChild(new Button.Builder(Component.literal("最新"), b -> {})
                .size(50, 18)
                .tooltip(Tooltip.create(Component.literal("按上架时间排序")))
                .build());
        toolbar.addChild(new Button.Builder(Component.literal("价格↑"), b -> {})
                .size(50, 18)
                .tooltip(Tooltip.create(Component.literal("按价格升序")))
                .build());
        toolbar.addChild(new Button.Builder(Component.literal("价格↓"), b -> {})
                .size(50, 18)
                .tooltip(Tooltip.create(Component.literal("按价格降序")))
                .build());
        mainPane.addChild(toolbar);

        HBoxWidget contentRow = new HBoxWidget(0, 0, 0)
                .setSpacing(10)
                .setBoxWidth(mainPane.getWidth() - mainPane.getPaddingLeft() - mainPane.getPaddingRight());

        VBoxWidget filterPanel = buildFilterPanel("市场筛选", List.of(
                Component.literal("全部"),
                Component.literal("材料"),
                Component.literal("装备"),
                Component.literal("方块"),
                Component.literal("消耗品")
        ));

        VBoxWidget listPanel = new VBoxWidget(0, 0, 0)
                .setSpacing(8)
                .setPadding(6, 6, 6, 6)
                .setBorderColor(0x33FFFFFF)
                .showBorder(true, true, true, true)
                .setBorderThickness(1)
                .setBackgroundColor(0x12000000);

        if (marketItems.isEmpty()) {
            listPanel.addChild(new LabelWidget(font, Component.literal("当前市场暂无物品"), 0, 0, 0xAAAAAA, true));
        } else {
            for (MarketItem item : marketItems) {
                ItemStack stack = item.getItemStack();
                HBoxWidget card = buildMarketCard(player, item, stack);
                listPanel.addChild(card);
            }
        }

        contentRow.addChild(filterPanel);
        contentRow.addChild(listPanel);
        contentRow.setHGrow(listPanel, true);
        mainPane.addChild(contentRow);
    }

    private void buildShopLayout() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        HBoxWidget toolbar = buildToolbar(Component.literal("系统商店"), shopItems.size(), 0xFFFFD27D);
        EditBox searchBox = buildSearchBox(Component.literal("搜索商店物品"));
        toolbar.addChild(searchBox);
        toolbar.setHGrow(searchBox, true);
        toolbar.addChild(new Button.Builder(Component.literal("热销"), b -> {})
                .size(50, 18)
                .tooltip(Tooltip.create(Component.literal("按热度排序")))
                .build());
        toolbar.addChild(new Button.Builder(Component.literal("折扣"), b -> {})
                .size(50, 18)
                .tooltip(Tooltip.create(Component.literal("查看折扣商品")))
                .build());
        mainPane.addChild(toolbar);

        HBoxWidget contentRow = new HBoxWidget(0, 0, 0)
                .setSpacing(10)
                .setBoxWidth(mainPane.getWidth() - mainPane.getPaddingLeft() - mainPane.getPaddingRight());

        VBoxWidget filterPanel = buildFilterPanel("商店分类", List.of(
                Component.literal("全部"),
                Component.literal("方块"),
                Component.literal("工具"),
                Component.literal("附魔"),
                Component.literal("特殊")
        ));

        VBoxWidget listPanel = new VBoxWidget(0, 0, 0)
                .setSpacing(8)
                .setPadding(6, 6, 6, 6)
                .setBorderColor(0x33FFFFFF)
                .showBorder(true, true, true, true)
                .setBorderThickness(1)
                .setBackgroundColor(0x12000000);

        if (shopItems.isEmpty()) {
            listPanel.addChild(new LabelWidget(font, Component.literal("当前商店暂无商品"), 0, 0, 0xAAAAAA, true));
        } else {
            for (ShopItem item : shopItems) {
                ItemStack stack = item.getItemStack();
                HBoxWidget card = buildShopCard(player, item, stack);
                listPanel.addChild(card);
            }
        }

        contentRow.addChild(filterPanel);
        contentRow.addChild(listPanel);
        contentRow.setHGrow(listPanel, true);
        mainPane.addChild(contentRow);
    }

    private HBoxWidget buildToolbar(Component title, int totalCount, int accentColor) {
        HBoxWidget toolbar = new HBoxWidget(0, 0, 24)
                .setSpacing(8)
                .setPadding(6, 10, 6, 10)
                .setBorderColor(0x55FFFFFF)
                .showBorder(true, true, true, true)
                .setBorderThickness(1)
                .setBackgroundColor(0x20FFFFFF)
                .setBoxWidth(mainPane.getWidth() - mainPane.getPaddingLeft() - mainPane.getPaddingRight())
                .setBoxHeight(26);

        LabelWidget titleLabel = new LabelWidget(font, title, 0, 0, accentColor, true)
                .setScale(1.1f);
        LabelWidget countLabel = new LabelWidget(font,
                Component.literal("共 " + totalCount + " 项"),
                0,
                0,
                0xAAAAAA,
                true
        ).setScale(0.9f);

        toolbar.addAllChildren(titleLabel, countLabel);
        return toolbar;
    }

    private VBoxWidget buildFilterPanel(String title, List<Component> entries) {
        VBoxWidget filterPanel = new VBoxWidget(0, 0, 180)
                .setSpacing(6)
                .setPadding(8, 8, 8, 8)
                .setBorderColor(0x44FFFFFF)
                .showBorder(true, true, true, true)
                .setBorderThickness(1)
                .setBackgroundColor(0x1A000000);

        LabelWidget titleLabel = new LabelWidget(font, Component.literal(title), 0, 0, 0xFFFFFF, true)
                .setScale(1.0f);
        filterPanel.addChild(titleLabel);

        for (Component entry : entries) {
            filterPanel.addChild(new Button.Builder(entry, b -> {})
                    .size(150, 18)
                    .tooltip(Tooltip.create(entry))
                    .build());
        }

        return filterPanel;
    }

    private EditBox buildSearchBox(Component hint) {
        EditBox searchBox = new EditBox(this.font, 0, 0, 140, 18, hint);
        searchBox.setHint(hint);
        searchBox.setMaxLength(50);
        return searchBox;
    }

    private HBoxWidget buildMarketCard(LocalPlayer player, MarketItem item, ItemStack stack) {
        HBoxWidget card = new HBoxWidget(0, 0, 0)
                .setSpacing(8)
                .setPadding(6, 10, 6, 10)
                .setBorderColor(0x22FFFFFF)
                .showBorder(true, true, true, true)
                .setBorderThickness(1)
                .setBackgroundColor(0x11000000)
                .setBoxHeight(40);

        ItemIconWidget icon = new ItemIconWidget(stack, font, 0, 0)
                .setScale(1.3f)
                .setShowDecorations(true)
                .setTooltipLines(buildItemTooltip(player, item));
        itemWidgets.add(icon);

        VBoxWidget infoBox = new VBoxWidget(0, 0, 200)
                .setSpacing(2)
                .setPadding(2, 2, 2, 2);
        infoBox.addChild(new LabelWidget(font, stack.getHoverName(), 0, 0, 0xFFFFFF, true));
        infoBox.addChild(new LabelWidget(
                font,
                Component.literal("卖家: " + item.getSellerName()),
                0,
                0,
                0xAAAAAA,
                true
        ).setScale(0.85f));

        LabelWidget priceLabel = new LabelWidget(
                font,
                Component.literal("￥" + item.getBasePrice()),
                0,
                0,
                0xFFB5F0FF,
                true
        ).setScale(1.0f);

        Button actionButton = new Button.Builder(Component.literal("查看"), b -> {})
                .size(40, 18)
                .tooltip(Tooltip.create(Component.literal("查看交易详情")))
                .build();

        card.addAllChildren(icon, infoBox, priceLabel, actionButton);
        card.setHGrow(infoBox, true);
        return card;
    }

    private HBoxWidget buildShopCard(LocalPlayer player, ShopItem item, ItemStack stack) {
        HBoxWidget card = new HBoxWidget(0, 0, 0)
                .setSpacing(8)
                .setPadding(6, 10, 6, 10)
                .setBorderColor(0x22FFFFFF)
                .showBorder(true, true, true, true)
                .setBorderThickness(1)
                .setBackgroundColor(0x11000000)
                .setBoxHeight(40);

        ItemIconWidget icon = new ItemIconWidget(stack, font, 0, 0)
                .setScale(1.3f)
                .setShowDecorations(true)
                .setTooltipLines(List.of(buildItemTooltip(player, item)));
        itemWidgets.add(icon);

        VBoxWidget infoBox = new VBoxWidget(0, 0, 200)
                .setSpacing(2)
                .setPadding(2, 2, 2, 2);
        infoBox.addChild(new LabelWidget(font, stack.getHoverName(), 0, 0, 0xFFFFFF, true));
        infoBox.addChild(new LabelWidget(
                font,
                Component.literal("浮动: " + item.getFluctuationFactor()),
                0,
                0,
                0xAAAAAA,
                true
        ).setScale(0.85f));

        LabelWidget priceLabel = new LabelWidget(
                font,
                Component.literal("￥" + item.getCurrentPrice()),
                0,
                0,
                0xFFFFD27D,
                true
        ).setScale(1.0f);

        Button actionButton = new Button.Builder(Component.literal("购买"), b -> {})
                .size(40, 18)
                .tooltip(Tooltip.create(Component.literal("购买商品")))
                .build();

        card.addAllChildren(icon, infoBox, priceLabel, actionButton);
        card.setHGrow(infoBox, true);
        return card;
    }

    private void setStartPage(String pageId) {
        if (pageId == null || pageId.isBlank()) {
            return;
        }
        this.currentPage = pageId;
        this.currentTitle = switch (pageId) {
            case PAGE_ID_STORE -> PAGE_NAME_STORE;
            case PAGE_ID_MARKET -> PAGE_NAME_MARKET;
            case PAGE_ID_DELIVERY_BOX -> PAGE_NAME_DELIVERY_BOX;
            case PAGE_ID_TERRITORIES -> PAGE_NAME_TERRITORIES;
            case PAGE_ID_ABOUT -> PAGE_NAME_ABOUT;
            default -> PAGE_NAME_HOME;
        };
    }

    // 工具提示构建方法
    private static List<Component> buildItemTooltip(Player player, MarketItem item) {
        List<Component> tooltipLines = new ArrayList<>(item.getItemStack().getTooltipLines(
                player,
                Minecraft.getInstance().options.advancedItemTooltips ?
                        TooltipFlag.ADVANCED : TooltipFlag.NORMAL
        ));

        // 添加分隔线
        tooltipLines.add(Component.literal("━━━━━━━━━━━━━━━━━━━━").withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.BOLD));

        // 卖家信息（键带符号，值在下一行缩进）
        // 卖家名称
        MutableComponent sellerKeyLine = Component.literal("» ").withStyle(ChatFormatting.GRAY)
                .append(Component.translatable(Util_MessageKeys.MARKET_SELLER_NAME_KEY)
                        .withStyle(ChatFormatting.GRAY));
        tooltipLines.add(sellerKeyLine);

        MutableComponent sellerValueLine = Component.literal("  ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(item.getSellerName())
                        .withStyle(ChatFormatting.WHITE));
        tooltipLines.add(sellerValueLine);

        // 卖家UUID
        MutableComponent sellerIDKeyLine = Component.literal("» ").withStyle(ChatFormatting.GRAY)
                .append(Component.translatable(Util_MessageKeys.MARKET_SELLER_UUID_KEY)
                        .withStyle(ChatFormatting.GRAY));
        tooltipLines.add(sellerIDKeyLine);

        MutableComponent sellerIDValueLine = Component.literal("  ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(item.getSellerID()))
                        .withStyle(ChatFormatting.WHITE));
        tooltipLines.add(sellerIDValueLine);

        // 交易ID
        MutableComponent tradeIDKeyLine = Component.literal("» ").withStyle(ChatFormatting.GRAY)
                .append(Component.translatable(Util_MessageKeys.MARKET_TRADE_ID_KEY)
                        .withStyle(ChatFormatting.GRAY));
        tooltipLines.add(tradeIDKeyLine);

        MutableComponent tradeIDValueLine = Component.literal("  ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(item.getTradeID()))
                        .withStyle(ChatFormatting.WHITE));
        tooltipLines.add(tradeIDValueLine);

        // 物品ID
        MutableComponent itemIDKeyLine = Component.literal("» ").withStyle(ChatFormatting.GRAY)
                .append(Component.translatable(Util_MessageKeys.MARKET_ITEM_ID_KEY)
                        .withStyle(ChatFormatting.GRAY));
        tooltipLines.add(itemIDKeyLine);

        MutableComponent itemIDValueLine = Component.literal("  ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(item.getItemID())
                        .withStyle(ChatFormatting.WHITE));
        tooltipLines.add(itemIDValueLine);

        // 添加空行和时间戳（注意：只有这里添加空行）
        tooltipLines.add(Component.empty());
        tooltipLines.add(Component.literal(formatTimestamp(item.getListingTime())).withStyle(ChatFormatting.GOLD));

        tooltipLines.add(Component.literal("━━━━━━━━━━━━━━━━━━━━").withStyle(ChatFormatting.DARK_GRAY));

        // 将多行合并为一个组件
        //return joinComponents(lines);
        return  tooltipLines;

    }

    private static Component buildItemTooltip(Player player, ShopItem item) {
        List<Component> lines = new ArrayList<>(item.getItemStack().getTooltipLines(
                player,
                Minecraft.getInstance().options.advancedItemTooltips ?
                        TooltipFlag.ADVANCED : TooltipFlag.NORMAL
        ));

        int priceDifference = item.getCurrentPrice() - item.getLastPrice();
        String priceChangeText;

        if (priceDifference > 0) {
            priceChangeText = "+" + priceDifference; // 正数显示 "+ xxx"
        } else {
            priceChangeText = String.valueOf(priceDifference); // 负数直接显示 "- xxx"
        }

        // 添加分隔线
        lines.add(Component.literal("----------------").withStyle(ChatFormatting.DARK_GRAY));

        // 添加市场信息
        lines.add(Component.translatable(Util_MessageKeys.SHOP_ITEM_CHANGE_PRICE_KEY, priceChangeText));
        lines.add(Component.translatable(Util_MessageKeys.SHOP_ITEM_BASIC_PRICE_KEY, item.getBasePrice()));
        lines.add(Component.translatable(Util_MessageKeys.SHOP_ITEM_CURRENT_PRICE_KEY, item.getCurrentPrice()));
        lines.add(Component.translatable(Util_MessageKeys.SHOP_ITEM_FLUCTUATION_FACTOR_KEY, item.getFluctuationFactor()));
        lines.add(Component.translatable(Util_MessageKeys.SHOP_ITEM_ID_KEY, item.getItemId()));

        // 将多行合并为一个组件
        return joinComponents(lines);
    }

    // 合并多个Component为一个
    private static Component joinComponents(List<Component> components) {
        if (components.isEmpty()) return Component.empty();

        Component result = components.get(0);
        for (int i = 1; i < components.size(); i++) {
            result = result.copy().append("\n").append(components.get(i));
        }
        return result;
    }

    // 格式化时间戳
    private static String formatTimestamp(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
    }
}

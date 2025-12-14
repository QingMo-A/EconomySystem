package com.mo.economy_system.screen.economy_system.market;

import com.mo.economy_system.network.packets.economy_system.*;
import com.mo.economy_system.network.packets.economy_system.demand_order.Packet_ConfirmDemandOrder;
import com.mo.economy_system.network.packets.economy_system.demand_order.Packet_DeliverDemandOrder;
import com.mo.economy_system.network.packets.economy_system.demand_order.Packet_RemoveDemandOrder;
import com.mo.economy_system.network.packets.economy_system.sales_order.Packet_PurchaseSalesOrder;
import com.mo.economy_system.network.packets.economy_system.sales_order.Packet_RemoveSalesOrder;
import com.mo.economy_system.screen.EconomySystem_Screen;
import com.mo.economy_system.screen.Screen_Home;
import com.mo.economy_system.core.economy_system.market.DemandOrder;
import com.mo.economy_system.core.economy_system.market.MarketItem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.core.economy_system.market.SalesOrder;
import com.mo.economy_system.screen.components.AnimatedButton;
import com.mo.economy_system.screen.components.AnimatedHighLevelTextField;
import com.mo.economy_system.screen.components.ItemIconAnimation;
import com.mo.economy_system.screen.components.TextAnimation;
import com.mo.economy_system.screen.newUI.HBoxWidget;
import com.mo.economy_system.screen.newUI.ItemIconWidget;
import com.mo.economy_system.screen.newUI.LabelWidget;
import com.mo.economy_system.screen.newUI.VBoxWidget;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class Screen_Market extends EconomySystem_Screen {
    private List<MarketItem> items = new ArrayList<>(); // 根据搜索过滤后的商品列表
    private List<MarketItem> itemsSnapshot = new ArrayList<>();
    private final List<ItemIconWidget> itemWidgets = new ArrayList<>();
    // 计数器变量，初始为 0
    private int displayTypeIndex = 0;
    // 定义按钮显示的文本数组
    private final String[] DISPLAY_TYPE_KEYS = {
            Util_MessageKeys.MARKET_SWITCH_DISPLAY_TYPE_0_BUTTON_KEY,
            Util_MessageKeys.MARKET_SWITCH_DISPLAY_TYPE_1_BUTTON_KEY,
            Util_MessageKeys.MARKET_SWITCH_DISPLAY_TYPE_2_BUTTON_KEY,
            Util_MessageKeys.MARKET_SWITCH_DISPLAY_TYPE_3_BUTTON_KEY,
            Util_MessageKeys.MARKET_SWITCH_DISPLAY_TYPE_4_BUTTON_KEY
    };

    private TextAnimation pageAnimation;
    private TextAnimation noItem;

    private AnimatedHighLevelTextField searchBox; // 搜索框

    private UUID playerUUID;
    private String playerName;

    // 构造方法
    public Screen_Market() {
        super(Component.translatable(Util_MessageKeys.MARKET_TITLE_KEY));
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_MarketDataRequest());
    }

    // 初始化
    @Override
    protected void init() {
        super.init();

        this.currentPage = 0;

        initPart();


        if (this.minecraft != null && this.minecraft.player != null) {
            this.playerUUID = this.minecraft.player.getUUID();
            this.playerName = this.minecraft.player.getName().getString();
        }
    }

    @Override
    protected void initPart() {

        initPosition();

        // 清除现有按钮
        this.clearWidgets();

        if (flag == 1) {

            // 添加搜索框
            this.searchBox = new AnimatedHighLevelTextField(
                    this.font,
                    Math.max((this.width / 2) - 300, 60),
                    -20,
                    200,
                    20,
                    1000,
                    Component.translatable("search.market")
            );
            this.addRenderableWidget(searchBox);

            // 设置搜索框的键盘监听器
            this.searchBox.setFocused(false); // 默认不聚焦
            this.searchBox.setMaxLength(50); // 限制输入长度
            this.searchBox.setHint(Component.translatable(Util_MessageKeys.MARKET_HINT_TEXT_KEY)); // 提示文本
            this.searchBox.setResponder(text -> applySearch());
            this.searchBox.startMoveAnimation(Math.max((this.width / 2) - 300, 60), 20);

            // 添加动态翻页按钮
            addPageAnimatedButtons();

            addSwitchDisplayTypeAnimatedButton();

            addListItemAnimatedButton();

            addRequestItemAnimatedButton();

        } else if (flag >= 2) {

            // 添加搜索框
            this.searchBox = new AnimatedHighLevelTextField(
                    this.font,
                    Math.max((this.width / 2) - 300, 60),
                    20,
                    200,
                    20,
                    1000,
                    Component.translatable("search.market")
            );
            this.addRenderableWidget(searchBox);

            // 设置搜索框的键盘监听器
            this.searchBox.setFocused(false); // 默认不聚焦
            this.searchBox.setMaxLength(50); // 限制输入长度
            this.searchBox.setHint(Component.translatable(Util_MessageKeys.MARKET_HINT_TEXT_KEY)); // 提示文本
            this.searchBox.setResponder(text -> applySearch());
            this.searchBox.startMoveAnimation(Math.max((this.width / 2) - 300, 60), 20);

            // 添加静态翻页按钮
            addPageButtons();

            // 添加切换显示类型的按钮
            addSwitchDisplayTypeButton();

            // 添加上架按钮
            addListItemButton();

            // 添加求购按钮
            addRequestItemButton();
        }

        flag ++;

        initializeRenderCache();

        super.initPart();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {

        this.flag = 1;

        super.resize(minecraft, width, height);
    }

    // 渲染(一帧一更新)
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);

        // 执行渲染缓存中的任务
        for (RunnableWithGraphics task : renderCache) {
            task.run(guiGraphics);
        }

        // 如果有商品，进行鼠标悬停检测并显示 Tooltip
        if (!items.isEmpty()) {
            detectMouseHoverAndRenderTooltip(guiGraphics, mouseX, mouseY);
        }

        // 检查哪个ItemIconWidget被悬停
        for (ItemIconWidget widget : itemWidgets) {
            if (widget.isHovered()) {
                guiGraphics.renderTooltip(this.font, widget.getTooltipLines(), Optional.empty(), mouseX, mouseY);
                break;
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void initializeRenderCache() {
        renderCache.clear(); // 清空旧的缓存

        pageAnimation = new TextAnimation(
                this.width / 2 - this.font.width((currentPage + 1) + " / " + getTotalPages()) / 2,
                this.height + 33,
                this.width / 2 - this.font.width((currentPage + 1) + " / " + getTotalPages()) / 2,
                this.height - 33,
                0f,
                1f,
                1000
        );

        renderCache.add((guiGraphics) -> {
            renderAnimatedText(
                    guiGraphics,
                    Component.literal((currentPage + 1) + " / " + getTotalPages()),
                    pageAnimation
            );
        });

        if (items.isEmpty()) {

            int textWidth = this.font.width(Component.translatable(Util_MessageKeys.MARKET_NO_ITEMS_TEXT_KEY));
            int xPosition = (this.width - textWidth) / 2;

            noItem = new TextAnimation(
                    xPosition,
                    this.height / 2 - 10,
                    xPosition,
                    this.height / 2 - 10,
                    0f,
                    1f,
                    2000
            );

            // 如果没有商品，添加无商品提示的渲染任务
            renderCache.add((guiGraphics) -> {

                renderAnimatedText(
                        guiGraphics,
                        Component.translatable(Util_MessageKeys.MARKET_NO_ITEMS_TEXT_KEY),
                        noItem
                );
            });
            return;
        }

        int y = startY;

        for (int i = startIndex; i < endIndex; i++) {
            MarketItem item = items.get(i);
            ItemStack itemStack = item.getItemStack();

            final int currentY = y; // 使用最终变量供 Lambda 表达式使用

            if (i == 1) {
                int rowHeight = 30;
                int rowWidth = 120; // 获取 mainPane 当前宽度

                HBoxWidget itemRow = new HBoxWidget(0, 0, 0)
                        .setSpacing(7)
                        .setPadding(5, 10, 5, 10)
                        .setBorderColor(0x22FFFFFF)
                        .setBoxWidth(rowWidth)
                        .setBoxHeight(rowHeight)
                        .showBorder(true, false, true, false)
                        .setBorderThickness(1);

                // 图标
                ItemIconWidget icon = new ItemIconWidget(itemStack, font, 0, 0)
                        .setScale(1.3f)
                        .setShowDecorations(true);
                // 构建工具提示
                List<Component> tooltipLines = buildItemTooltip(this.minecraft.player, item);
                // Tooltip tooltip = Tooltip.create(buildItemTooltip(player, item));
                icon.setTooltipLines(tooltipLines);
                itemWidgets.add(icon);
                // icon.setCustomTooltipLines(tooltipLines);

                // 名称 + 价格 VBox
                VBoxWidget infoBox = new VBoxWidget(0, 0, 200) // 宽 200，高和 row 高一致
                        .setSpacing(2)
                        .setBoxHeight(rowHeight)
                        .setPadding(2, 2, 2, 2);

                LabelWidget nameLabel = new LabelWidget(font, itemStack.getHoverName(), 0, 0, 0xFFFFFF, true)
                        .setScale(1.0f);
                LabelWidget priceLabel = new LabelWidget(font, Component.literal("价格: " + item.getBasePrice()), 0, 0, 0xAAAAAA, true)
                        .setScale(0.9f);

                infoBox.addAllChildren(nameLabel, priceLabel);
                itemRow.addAllChildren(icon, infoBox);
                this.addRenderableWidget(itemRow);
            }

            ItemIconAnimation icon;
            TextAnimation name;
            TextAnimation price;

            icon = new ItemIconAnimation(
                    startX,
                    currentY,
                    startX,
                    currentY,
                    0f,
                    1f,
                    0.8f,
                    1f,
                    1000
            );

            name = new TextAnimation(
                    startX + 20,
                    currentY + 5,
                    startX + 20,
                    currentY + 5,
                    0f,
                    1f,
                    1000
            );

            price = new TextAnimation(
                    startX,
                    currentY + 18,
                    startX,
                    currentY + 18,
                    0f,
                    1f,
                    1000
            );

            // 渲染物品图标, 价格与描述
            renderCache.add((guiGraphics) -> {
                renderAnimatedItem(
                        guiGraphics,
                        itemStack,
                        icon
                );
                renderAnimatedText(
                        guiGraphics,
                        Component.translatable(Util_MessageKeys.MARKET_ITEM_NAME_AND_COUNT_KEY, itemStack.getHoverName().getString(), itemStack.getCount()),
                        name,
                        0xFFFFFF
                );
                renderAnimatedText(
                        guiGraphics,
                        Component.translatable(Util_MessageKeys.MARKET_ITEM_PRICE_KEY, item.getBasePrice()),
                        price,
                        0xAAAAAA
                );
            });

            // 添加购买或下架按钮（确保在初始化时添加按钮）
            addActionButton(item, this.width - startX, currentY, playerUUID);

            y += THING_SPACING; // 调整下一件商品的位置
        }
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

        tooltipLines.add(Component.literal("━━━━━━━━━━━━━━━━━━━━").withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.BOLD));

        // 将多行合并为一个组件
        //return joinComponents(lines);
        return  tooltipLines;

    }

    @Override
    protected void detectMouseHoverAndRenderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        initPosition();
        int y = startY;

        for (int i = startIndex; i < endIndex; i++) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return;

            MarketItem item = items.get(i);

            if (isMouseOver(mouseX, mouseY, startX, y, 16, 16)) {
                List<Component> tooltipLines = item.getItemStack().getTooltipLines(
                        player,
                        Minecraft.getInstance().options.advancedItemTooltips ?
                                TooltipFlag.ADVANCED : TooltipFlag.NORMAL
                );
                tooltipLines.add(Component.literal("-=-=-=-=-=-").withStyle(ChatFormatting.DARK_GRAY));

                tooltipLines.add(Component.translatable(Util_MessageKeys.MARKET_SELLER_NAME_KEY, item.getSellerName()));
                tooltipLines.add(Component.translatable(Util_MessageKeys.MARKET_SELLER_UUID_KEY, item.getSellerID()));
                tooltipLines.add(Component.translatable(Util_MessageKeys.MARKET_TRADE_ID_KEY, item.getTradeID()));
                tooltipLines.add(Component.translatable(Util_MessageKeys.MARKET_ITEM_ID_KEY, item.getItemID()));
                tooltipLines.add(Component.literal(""));
                tooltipLines.add(Component.literal(formatTimestamp(item.getListingTime())));

                guiGraphics.renderTooltip(this.font, tooltipLines, Optional.empty(), mouseX, mouseY);
            }

            y += THING_SPACING;
        }
    }

    // 添加初始化动态分页按钮
    @Override
    protected void addPageAnimatedButtons() {
        initPosition();
        int buttonY = this.height - 40;

        this.addRenderableWidget(
                new AnimatedButton(
                        startX,
                        this.height,
                        startX,
                        buttonY,
                        PAGE_BUTTON_WIDTH,
                        PAGE_BUTTON_HEIGHT,
                        Component.literal("<"),
                        1000,
                        button -> {
                            if (currentPage > 0) {
                                currentPage--;
                                this.initPart(); // 刷新页面
                            }
                        }
                )
        );

        this.addRenderableWidget(
                new AnimatedButton(
                        this.width - startX - PAGE_BUTTON_WIDTH,
                        this.height,
                        this.width - startX - PAGE_BUTTON_WIDTH,
                        buttonY,
                        PAGE_BUTTON_WIDTH,
                        PAGE_BUTTON_HEIGHT,
                        Component.literal(">"),
                        1000,
                        button -> {
                            if (currentPage < getTotalPages() - 1) {
                                currentPage++;
                                this.initPart(); // 刷新页面
                            }
                        }
                )
        );
    }

    // 添加后续静态分页按钮
    @Override
    protected void addPageButtons() {
        initPosition();
        int buttonY = this.height - 40;

        // 上一页按钮
        this.addRenderableWidget(
                Button.builder(Component.literal("<"), button -> {
                            if (currentPage > 0) {
                                currentPage--;
                                this.initPart(); // 刷新页面
                            }
                        })
                        .pos(startX, buttonY)
                        .size(PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT)
                        .build()
        );

        // 下一页按钮
        this.addRenderableWidget(
                Button.builder(Component.literal(">"), button -> {
                            if (currentPage < getTotalPages() - 1) {
                                currentPage++;
                                this.initPart(); // 刷新页面
                            }
                        })
                        .pos(this.width - startX - PAGE_BUTTON_WIDTH, buttonY)
                        .size(PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT)
                        .build()
        );
    }

    // 添加求购按钮
    private void addRequestItemAnimatedButton() {
        int buttonWidth = 70;
        int buttonHeight = 20;

        this.addRenderableWidget(
                new AnimatedButton(
                        this.width - Math.max((this.width / 2) - 300, 60) - buttonWidth,
                        -20,
                        this.width - Math.max((this.width / 2) - 300, 60) - buttonWidth,
                        20,
                        buttonWidth,
                        buttonHeight,
                        Component.translatable(Util_MessageKeys.MARKET_REQUEST_BUTTON_KEY),
                        1000,
                        button -> {
                            // 打开上架界面
                            this.minecraft.setScreen(new Screen_CreateDemandOrder(minecraft.player));
                        }
                )
        );
    }

    private void addRequestItemButton() {
        int buttonWidth = 70;
        int buttonHeight = 20;

        this.addRenderableWidget(
                Button.builder(Component.translatable(Util_MessageKeys.MARKET_REQUEST_BUTTON_KEY), button -> {
                            // 打开上架界面
                            this.minecraft.setScreen(new Screen_CreateDemandOrder(minecraft.player));
                        })
                        .pos(this.width - Math.max((this.width / 2) - 300, 60) - buttonWidth, 20)
                        .size(buttonWidth, buttonHeight)
                        .build()
        );
    }

    // 添加上架按钮
    private void addListItemAnimatedButton() {
        int buttonWidth = 70;
        int buttonHeight = 20;

        this.addRenderableWidget(
                new AnimatedButton(
                        this.width - Math.max((this.width / 2) - 300, 60) - (2 * buttonWidth + 10),
                        -20,
                        this.width - Math.max((this.width / 2) - 300, 60) - (2 * buttonWidth + 10),
                        20,
                        buttonWidth,
                        buttonHeight,
                        Component.translatable(Util_MessageKeys.MARKET_LIST_BUTTON_KEY),
                        1000,
                        button -> {
                            // 打开上架界面
                            this.minecraft.setScreen(new Screen_CreateSalesOrder(minecraft.player));
                        }
                )
        );
    }

    private void addListItemButton() {
        int buttonWidth = 70;
        int buttonHeight = 20;

        this.addRenderableWidget(
                Button.builder(Component.translatable(Util_MessageKeys.MARKET_LIST_BUTTON_KEY), button -> {
                            // 打开上架界面
                            this.minecraft.setScreen(new Screen_CreateSalesOrder(minecraft.player));
                        })
                        .pos(this.width - Math.max((this.width / 2) - 300, 60) - (2 * buttonWidth + 10), 20)
                        .size(buttonWidth, buttonHeight)
                        .build()
        );
    }

    // 添加切换显示类型按钮
    private void addSwitchDisplayTypeAnimatedButton() {
        int buttonWidth = 100;
        int buttonHeight = 20;

        // 添加到界面
        this.addRenderableWidget(
                new AnimatedButton(
                        this.width - Math.max((this.width / 2) - 300, 60) - (2 * 70 + 20) - buttonWidth,
                        -20,
                        this.width - Math.max((this.width / 2) - 300, 60) - (2 * 70 + 20) - buttonWidth,
                        20,
                        buttonWidth,
                        buttonHeight,
                        Component.translatable(DISPLAY_TYPE_KEYS[displayTypeIndex]),
                        1000,
                        button -> {
                            // 执行切换逻辑
                            displayTypeIndex = (displayTypeIndex + 1) % DISPLAY_TYPE_KEYS.length; // 循环切换文本索引
                            button.setMessage(Component.translatable(DISPLAY_TYPE_KEYS[displayTypeIndex])); // 更新按钮显示文本

                            // 执行与当前显示类型相关的操作
                            handleDisplayTypeAction(displayTypeIndex);
                        })
        );
    }

    private void addSwitchDisplayTypeButton() {
        int buttonWidth = 100;
        int buttonHeight = 20;

        // 创建按钮
        Button switchDisplayButton = Button.builder(
                        Component.translatable(DISPLAY_TYPE_KEYS[displayTypeIndex]), // 初始文本
                        button -> {
                            // 执行切换逻辑
                            displayTypeIndex = (displayTypeIndex + 1) % DISPLAY_TYPE_KEYS.length; // 循环切换文本索引
                            button.setMessage(Component.translatable(DISPLAY_TYPE_KEYS[displayTypeIndex])); // 更新按钮显示文本

                            // 执行与当前显示类型相关的操作
                            handleDisplayTypeAction(displayTypeIndex);
                        }
                )
                .pos(this.width - Math.max((this.width / 2) - 300, 60) - (2 * 70 + 20) - buttonWidth, 20)
                .size(buttonWidth, buttonHeight)
                .build();

        // 添加到界面
        this.addRenderableWidget(switchDisplayButton);
    }

    // 处理与显示类型相关的操作
    private void handleDisplayTypeAction(int displayTypeIndex) {
        this.displayTypeIndex = displayTypeIndex; // 保存过滤条件
        applyFilters(); // 调用联合过滤逻辑
    }

    // 添加购买按钮
    private void addItemButtons() {
        initPosition();

        int y = startY;
        UUID playerUUID = this.minecraft.player.getUUID();

        for (int i = startIndex; i < endIndex; i++) {

            MarketItem item = items.get(i);

            // 添加购买或下架按钮
            this.addActionButton(item, this.width - startX, y, playerUUID);

            y += THING_SPACING;
        }
    }

    // 重构后的主方法，根据订单类型与玩家权限添加相应按钮
    private void addActionButton(MarketItem item, int buttonX, int buttonY, UUID playerUUID) {
        // 判断玩家是否拥有OP权限
        boolean isOP = this.minecraft.player.hasPermissions(2);

        if (item instanceof SalesOrder salesOrder) {
            // 出货单：卖家只显示下架按钮，买家显示购买按钮；OP买家还可以看到下架按钮
            if (salesOrder.getSellerID().equals(playerUUID)) {
                addButton(Util_MessageKeys.MARKET_REMOVE_BUTTON_KEY, buttonX - 60, buttonY, 60, 20,
                        () -> {
                            EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_RemoveSalesOrder(salesOrder.getTradeID()));
                            refresh();
                        });
            } else {
                if (isOP) {
                    addButton(Util_MessageKeys.MARKET_REMOVE_BUTTON_KEY, buttonX - 60, buttonY, 60, 20,
                            () -> {
                                EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_RemoveSalesOrder(salesOrder.getTradeID()));
                                refresh();
                            });
                    addButton(Util_MessageKeys.MARKET_BUY_BUTTON_KEY, buttonX - 130, buttonY, 60, 20,
                            () -> {
                                EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_PurchaseSalesOrder(salesOrder.getTradeID()));
                                refresh();
                            });
                } else {
                    addButton(Util_MessageKeys.MARKET_BUY_BUTTON_KEY, buttonX - 60, buttonY, 60, 20,
                            () -> {
                                EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_PurchaseSalesOrder(salesOrder.getTradeID()));
                                refresh();
                            });
                }
            }
        } else if (item instanceof DemandOrder demandOrder) {
            // 订购单：区分卖家与买家的按钮显示，并根据交付状态及权限做进一步区分
            if (demandOrder.getSellerID().equals(playerUUID)) {
                if (demandOrder.isDelivered()) {
                    addButton(Util_MessageKeys.REQUEST_CLAIM_BUTTON_KEY, buttonX - 60, buttonY, 60, 20,
                            () -> {
                                EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_ConfirmDemandOrder(demandOrder.getTradeID()));
                                refresh();
                            });
                } else {
                    addButton(Util_MessageKeys.REQUEST_CANCEL_KEY, buttonX - 60, buttonY, 60, 20,
                            () -> {
                                EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_RemoveDemandOrder(demandOrder.getTradeID()));
                                refresh();
                            });
                }
            } else {
                if (isOP) {
                    if (demandOrder.isDelivered()) {
                        addButton(Util_MessageKeys.REQUEST_CLAIM_BUTTON_KEY, buttonX - 60, buttonY, 60, 20,
                                () -> {
                                    EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_ConfirmDemandOrder(demandOrder.getTradeID()));
                                    refresh();
                                });
                        addDisabledButton(Util_MessageKeys.REQUEST_DELIVERED_STATUS_KEY, buttonX - 130, buttonY, 60, 20,
                                () -> {
                                    EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_DeliverDemandOrder(demandOrder.getTradeID()));
                                    refresh();
                                });
                    } else {
                        addButton(Util_MessageKeys.REQUEST_CANCEL_KEY, buttonX - 60, buttonY, 60, 20,
                                () -> {
                                    EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_RemoveDemandOrder(demandOrder.getTradeID()));
                                    refresh();
                                });
                        addButton(Util_MessageKeys.REQUEST_DELIVER_BUTTON_KEY, buttonX - 130, buttonY, 60, 20,
                                () -> {
                                    EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_DeliverDemandOrder(demandOrder.getTradeID()));
                                    refresh();
                                });
                    }
                } else {
                    if (demandOrder.isDelivered()) {
                        addDisabledButton(Util_MessageKeys.REQUEST_DELIVERED_STATUS_KEY, buttonX - 60, buttonY, 60, 20,
                                () -> {
                                    EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_PurchaseSalesOrder(demandOrder.getTradeID()));
                                    refresh();
                                });
                    } else {
                        addButton(Util_MessageKeys.REQUEST_DELIVER_BUTTON_KEY, buttonX - 60, buttonY, 60, 20,
                                () -> {
                                    EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_DeliverDemandOrder(demandOrder.getTradeID()));
                                    refresh();
                                });
                    }
                }
            }
        }
        // 其它类型暂不处理
    }

    // 辅助方法：创建并添加一个按钮
    private void addButton(String translationKey, int posX, int posY, int width, int height, Runnable onClick) {
        this.addRenderableWidget(
                new AnimatedButton(
                        this.width + width,
                        posY,
                        posX,
                        posY,
                        width,
                        height,
                        Component.translatable(translationKey),
                        1000,
                        button -> onClick.run()
                )
        );
    }

    // 辅助方法：创建一个按钮并将其设为不可用（disabled）
    private void addDisabledButton(String translationKey, int posX, int posY, int width, int height, Runnable onClick) {
        AnimatedButton button = new AnimatedButton(
                this.width + width,
                posY,
                posX,
                posY,
                width,
                height,
                Component.translatable(translationKey),
                1000,
                btn -> onClick.run()
        );
        button.active = false;
        this.addRenderableWidget(button);
    }

    // 辅助方法：发送刷新界面
    private void refresh() {
        refreshItemButtons();
        requestMarketUpdate();
    }

    private void requestMarketUpdate() {
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_MarketDataRequest());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox.isFocused() && keyCode == 257) { // 检测回车键（keyCode 257）
            applySearch();
            return true; // 防止事件进一步传播
        } else if (keyCode == 256 && this.shouldCloseOnEsc()) {
            Minecraft.getInstance().setScreen(new Screen_Home());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void applySearch() {
        applyFilters(); // 调用联合过滤逻辑
    }

    private void applyFilters() {
        new Thread(() -> {
            List<MarketItem> result = itemsSnapshot;

            // 1. 应用过滤条件
            switch (displayTypeIndex) {
                case 1: // 仅显示自己的订单
                    result = result.stream()
                            .filter(item -> item.getSellerName().toLowerCase().contains(playerName.toLowerCase()))
                            .collect(Collectors.toList());
                    break;
                case 2: // 仅显示非自己的订单
                    result = result.stream()
                            .filter(item -> !item.getSellerName().toLowerCase().contains(playerName.toLowerCase()))
                            .collect(Collectors.toList());
                    break;
                case 3: // 仅显示出货单
                    result = result.stream()
                            .filter(SalesOrder.class::isInstance)
                            .collect(Collectors.toList());
                    break;
                case 4: // 仅显示订购单
                    result = result.stream()
                            .filter(DemandOrder.class::isInstance)
                            .collect(Collectors.toList());
                    break;
                // case 0: 无过滤条件
            }

            // 2. 应用搜索条件
            if (searchBox != null && !searchBox.getValue().isEmpty()) {
                result = result.stream()
                        .filter(item -> itemMatchesSearch(item, searchBox.getValue()))
                        .collect(Collectors.toList());
            }

            // 3. 更新UI
            List<MarketItem> finalResult = result;
            this.minecraft.execute(() -> {
                this.items = finalResult;
                this.currentPage = 0;
                refreshItemButtons();
                initializeRenderCache(); // 重新初始化渲染缓存
            });
        }).start();
    }

    private boolean itemMatchesSearch(MarketItem item, String searchText) {
        return item.getItemID().toLowerCase().contains(searchText.toLowerCase()) ||
                item.getSellerName().toLowerCase().contains(searchText.toLowerCase()) ||
                item.getItemStack().getHoverName().getString().toLowerCase().contains(searchText.toLowerCase());
    }

    // 刷新购买按钮
    private void refreshItemButtons() {
        clearItemButtons(); // 清除旧的商品按钮
        addItemButtons();   // 添加新的商品按钮
    }

    // 移除购买按钮
    private void clearItemButtons() {
        // 遍历所有已渲染的控件并移除与商品相关的按钮
        this.renderables.removeIf(widget -> widget instanceof Button && isItemButton((Button) widget));
        this.children().removeIf(widget -> widget instanceof Button && isItemButton((Button) widget));
    }

    // 判断是否为购买按钮
    private boolean isItemButton(Button button) {
        // 判断按钮是否为 "Buy" 或 "Remove" 按钮
        Component buttonMessage = button.getMessage();
        return buttonMessage.equals(Component.translatable(Util_MessageKeys.MARKET_BUY_BUTTON_KEY)) ||
                buttonMessage.equals(Component.translatable(Util_MessageKeys.MARKET_REMOVE_BUTTON_KEY)) ||
                buttonMessage.equals(Component.translatable(Util_MessageKeys.REQUEST_DELIVER_BUTTON_KEY)) ||
                buttonMessage.equals(Component.translatable(Util_MessageKeys.REQUEST_DELIVERED_STATUS_KEY)) ||
                buttonMessage.equals(Component.translatable(Util_MessageKeys.REQUEST_CLAIM_BUTTON_KEY)) ||
                buttonMessage.equals(Component.translatable(Util_MessageKeys.REQUEST_CANCEL_KEY));
    }

    public void updateMarketItems(List<MarketItem> items) {
        this.items = new ArrayList<>(items); // 初始化过滤后的列表
        this.itemsSnapshot = new ArrayList<>(items); // 初始化过滤后的列表
        this.init(); // 每次更新市场物品后重新初始化界面
    }

    // 动态计算总页数
    private int getTotalPages() {
        return (int) Math.ceil((double) this.items.size() / thingsPerPage);
    }

    @Override
    protected void initPosition(){
        TOP_MARGIN = this.height - 100;
        thingsPerPage = Math.max(1, TOP_MARGIN / THING_SPACING);

        startIndex = currentPage * thingsPerPage;
        endIndex = Math.min(startIndex + thingsPerPage, items.size());

        startX = Math.max((this.width / 2) - 300, 60);
        startY = Math.max((this.height - 450) / 4, 55);
    }

    // 格式化时间戳
    private static String formatTimestamp(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
    }
}
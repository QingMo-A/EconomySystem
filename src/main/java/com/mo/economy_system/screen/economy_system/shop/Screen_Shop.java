package com.mo.economy_system.screen.economy_system.shop;

import com.mo.economy_system.screen.EconomySystem_Screen;
import com.mo.economy_system.screen.Screen_Home;
import com.mo.economy_system.core.economy_system.shop.ShopItem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.economy_system.Packet_ShopDataRequest;
import com.mo.economy_system.screen.components.AnimatedButton;
import com.mo.economy_system.screen.components.AnimatedHighLevelTextField;
import com.mo.economy_system.screen.components.ItemIconAnimation;
import com.mo.economy_system.screen.components.TextAnimation;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Screen_Shop extends EconomySystem_Screen {

    private List<ShopItem> items = new ArrayList<>(); // 商品列表
    private List<ShopItem> itemsSnapshot = new ArrayList<>();

    private TextAnimation pageAnimation;
    private TextAnimation noItem;

    private AnimatedHighLevelTextField searchBox; // 搜索框

    public Screen_Shop() {
        super(Component.translatable(Util_MessageKeys.SHOP_TITLE_KEY));
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_ShopDataRequest());
    }

    public void updateShopItems(List<ShopItem> items) {
        // System.out.println("更新商店数据: " + items.size() + " 个");
        this.items = items;
        this.itemsSnapshot = new ArrayList<>(items);
        this.init(); // 每次更新商店物品后重新初始化界面
    }

    @Override
    protected void init() {
        super.init();

        this.currentPageNumber = 0;

        initPart();
    }

    @Override
    protected void initPart() {
        initPosition();

        // 清除现有按钮
        clearWidgets();

        if (flag == 1) {

            // 添加搜索框
            this.searchBox = new AnimatedHighLevelTextField(
                    this.font,
                    Math.max((this.width / 2) - 300, 60),
                    -20,
                    200,
                    20,
                    1000,
                    Component.translatable("search.shop")
            );
            this.addRenderableWidget(searchBox);

            // 设置搜索框的键盘监听器
            this.searchBox.setFocused(false); // 默认不聚焦
            this.searchBox.setMaxLength(50); // 限制输入长度
            this.searchBox.setHint(Component.translatable(Util_MessageKeys.SHOP_HINT_TEXT_KEY)); // 提示文本
            this.searchBox.setResponder(text -> applySearch());
            this.searchBox.startMoveAnimation(Math.max((this.width / 2) - 300, 60), 20);

            // 添加动态翻页按钮
            addPageAnimatedButtons();

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
            this.searchBox.setHint(Component.translatable(Util_MessageKeys.SHOP_HINT_TEXT_KEY)); // 提示文本
            this.searchBox.setResponder(text -> applySearch());
            this.searchBox.startMoveAnimation(Math.max((this.width / 2) - 300, 60), 20);

            // 添加静态翻页按钮
            addPageButtons();
        }

        // 动态添加商品购买按钮
        addItemButtons();

        flag ++;

        // 初始化渲染缓存（在所有按钮添加后调用）
        initializeRenderCache();

        super.initPart();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {

        this.flag = 1;

        super.resize(minecraft, width, height);
    }

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

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void initializeRenderCache() {
        renderCache.clear(); // 清空旧的缓存

        pageAnimation = new TextAnimation(
                this.width / 2 - this.font.width((currentPageNumber + 1) + " / " + getTotalPages()) / 2,
                this.height + 33,
                this.width / 2 - this.font.width((currentPageNumber + 1) + " / " + getTotalPages()) / 2,
                this.height - 33,
                0f,
                1f,
                1000
        );

        renderCache.add((guiGraphics) -> {
            renderAnimatedText(
                    guiGraphics,
                    Component.literal((currentPageNumber + 1) + " / " + getTotalPages()),
                    pageAnimation
            );
        });

        if (items.isEmpty()) {

            // 动态计算文字居中的位置
            int textWidth = this.font.width(Component.translatable(Util_MessageKeys.SHOP_LOADING_SHOP_DATA_TEXT_KEY));
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
                        Component.translatable(Util_MessageKeys.SHOP_LOADING_SHOP_DATA_TEXT_KEY),
                        noItem
                );

            });
            return;

        }

        int y = startY;

        for (int i = startIndex; i < endIndex; i++) {
            ShopItem item = items.get(i);
            ItemStack itemStack = item.getItemStack();

            final int currentY = y; // 使用最终变量供 Lambda 表达式使用

            ItemIconAnimation icon;
            TextAnimation price;
            TextAnimation description;

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

            price = new TextAnimation(
                    startX + 20,
                    currentY + 5,
                    startX + 20,
                    currentY + 5,
                    0f,
                    1f,
                    1000
            );

            description = new TextAnimation(
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
                        Component.translatable(Util_MessageKeys.SHOP_ITEM_PRICE_KEY, item.getCurrentPrice()),
                        price,
                        0xFFFFFF
                );
                renderAnimatedText(
                        guiGraphics,
                        Component.literal(item.getDescription()),
                        description,
                        0xAAAAAA
                );
            });

            y += THING_SPACING;
        }
    }

    @Override
    protected void detectMouseHoverAndRenderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        initPosition();

        int y = startY;

        for (int i = startIndex; i < endIndex; i++) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return;

            ShopItem item = items.get(i);

            int priceDifference = item.getCurrentPrice() - item.getLastPrice();
            String priceChangeText;

            if (priceDifference > 0) {
                priceChangeText = "+" + priceDifference; // 正数显示 "+ xxx"
            } else {
                priceChangeText = String.valueOf(priceDifference); // 负数直接显示 "- xxx"
            }

            if (isMouseOver(mouseX, mouseY, startX, y, 16, 16)) {
                List<Component> tooltipLines = item.getItemStack().getTooltipLines(
                        player,
                        Minecraft.getInstance().options.advancedItemTooltips ?
                                TooltipFlag.ADVANCED : TooltipFlag.NORMAL
                );
                tooltipLines.add(Component.literal("-=-=-=-=-=-").withStyle(ChatFormatting.DARK_GRAY));

                tooltipLines.add(Component.translatable(Util_MessageKeys.SHOP_ITEM_CHANGE_PRICE_KEY, priceChangeText));
                tooltipLines.add(Component.translatable(Util_MessageKeys.SHOP_ITEM_BASIC_PRICE_KEY, item.getBasePrice()));
                tooltipLines.add(Component.translatable(Util_MessageKeys.SHOP_ITEM_CURRENT_PRICE_KEY, item.getCurrentPrice()));
                tooltipLines.add(Component.translatable(Util_MessageKeys.SHOP_ITEM_FLUCTUATION_FACTOR_KEY, item.getFluctuationFactor()));
                tooltipLines.add(Component.translatable(Util_MessageKeys.SHOP_ITEM_ID_KEY, item.getItemId()));

                guiGraphics.renderTooltip(this.font, tooltipLines, Optional.empty(), mouseX, mouseY);
            }

            y += THING_SPACING;
        }
    }

    // 动态为每个商品添加按钮
    private void addItemButtons() {
        initPosition();

        int y = startY;
        for (int i = startIndex; i < endIndex; i++) {
            ShopItem item = items.get(i);

            // 添加 购买 按钮
            this.addRenderableWidget(
                    new AnimatedButton(
                            this.width + 60,
                            y,
                            this.width - startX - 60,
                            y,
                            60, 20,
                            Component.translatable(Util_MessageKeys.SHOP_BUY_BUTTON_KEY),
                            1000,
                            button -> {
                                this.minecraft.setScreen(new Screen_BuyItem(item));
                            })
            );

            y += THING_SPACING;
        }
    }

    // 添加初始化动态分页按钮
    @Override
    protected void addPageAnimatedButtons() {
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
                            if (currentPageNumber > 0) {
                                currentPageNumber--;
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
                            if (currentPageNumber < getTotalPages() - 1) {
                                currentPageNumber++;
                                this.initPart(); // 刷新页面
                            }
                        }
                )
        );
    }

    // 添加后续静态分页按钮
    @Override
    protected void addPageButtons() {
        int buttonY = this.height - 40;

        // 上一页按钮
        this.addRenderableWidget(
                Button.builder(Component.literal("<"), button -> {
                            if (currentPageNumber > 0) {
                                currentPageNumber--;
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
                            if (currentPageNumber < getTotalPages() - 1) {
                                currentPageNumber++;
                                this.initPart(); // 刷新页面
                            }
                        })
                        .pos(this.width - startX - PAGE_BUTTON_WIDTH, buttonY)
                        .size(PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT)
                        .build()
        );
    }

    // 动态计算总页数
    private int getTotalPages() {
        return (int) Math.ceil((double) items.size() / thingsPerPage);
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
            List<ShopItem> result = itemsSnapshot;

            // 2. 应用搜索条件
            if (searchBox != null && !searchBox.getValue().isEmpty()) {
                result = result.stream()
                        .filter(item -> itemMatchesSearch(item, searchBox.getValue()))
                        .collect(Collectors.toList());
            }

            // 3. 更新UI
            List<ShopItem> finalResult = result;
            this.minecraft.execute(() -> {
                this.items = finalResult;
                this.currentPageNumber = 0;
                refreshItemButtons();
                initializeRenderCache(); // 重新初始化渲染缓存
            });
        }).start();
    }

    private boolean itemMatchesSearch(ShopItem item, String searchText) {
        return item.getItemId().toLowerCase().contains(searchText.toLowerCase()) ||
                item.getDescription().toLowerCase().contains(searchText.toLowerCase()) ||
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
        Component buttonMessage = button.getMessage();
        return buttonMessage.equals(Component.translatable(Util_MessageKeys.SHOP_BUY_BUTTON_KEY));
    }

    @Override
    protected void initPosition(){
        TOP_MARGIN = this.height - 100;
        thingsPerPage = Math.max(1, TOP_MARGIN / THING_SPACING);

        startIndex = currentPageNumber * thingsPerPage;
        endIndex = Math.min(startIndex + thingsPerPage, items.size());

        startX = Math.max((this.width / 2) - 300, 60);
        startY = Math.max((this.height - 450) / 4, 55);
    }
}

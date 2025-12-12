package com.mo.economy_system.screen.economy_system.deliver_box;

import com.mo.economy_system.core.economy_system.delivery_box.DeliveryItem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.economy_system.Packet_DeliveryBoxClaimItem;
import com.mo.economy_system.network.packets.economy_system.Packet_DeliveryBoxDataRequest;
import com.mo.economy_system.screen.EconomySystem_Screen;
import com.mo.economy_system.screen.Screen_Home;
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
import java.util.UUID;
import java.util.stream.Collectors;

public class Screen_DeliveryBox extends EconomySystem_Screen {
    private List<DeliveryItem> items = new ArrayList<>(); // 物品列表
    private List<DeliveryItem> filteredItems = new ArrayList<>(); // 根据搜索过滤后的物品列表
    private List<DeliveryItem> itemsSnapshot = new ArrayList<>();

    private TextAnimation pageAnimation;
    private TextAnimation noItem;

    private AnimatedHighLevelTextField searchBox; // 搜索框

    private UUID playerUUID;
    private String playerName;

    public Screen_DeliveryBox() {
        super(Component.translatable(Util_MessageKeys.DELIVERY_BOX_TITLE_KEY));
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_DeliveryBoxDataRequest());
    }

    @Override
    protected void init() {
        super.init();

        this.currentPage = 0;

        initPart();
    }

    @Override
    protected void initPart() {
        initPosition();

        if (this.minecraft != null && this.minecraft.player != null) {
            this.playerUUID = this.minecraft.player.getUUID();
            this.playerName = this.minecraft.player.getName().getString();
        }

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
                    Component.translatable("search.delivery_box")
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
                    Component.translatable("search.delivery_box")
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
        if (!filteredItems.isEmpty()) {
            detectMouseHoverAndRenderTooltip(guiGraphics, mouseX, mouseY);
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

        if (filteredItems.isEmpty()) {

            // 动态计算文字居中的位置
            int textWidth = this.font.width(Component.translatable(Util_MessageKeys.DELIVERY_BOX_NO_ITEMS_TEXT_KEY));
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
                        Component.translatable(Util_MessageKeys.DELIVERY_BOX_NO_ITEMS_TEXT_KEY),
                        noItem
                );
            });
            return;

        }

        int y = startY;

        for (int i = startIndex; i < endIndex; i++) {
            DeliveryItem item = filteredItems.get(i);
            ItemStack itemStack = item.getItemStack();

            final int currentY = y; // 使用最终变量供 Lambda 表达式使用

            ItemIconAnimation icon;
            TextAnimation name;
            TextAnimation source;

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

            source = new TextAnimation(
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
                        Component.translatable(Util_MessageKeys.DELIVERY_BOX_ITEM_NAME_AND_COUNT_KEY, itemStack.getHoverName().getString(), itemStack.getCount()),
                        name,
                        0xFFFFFF
                );
                renderAnimatedText(
                        guiGraphics,
                        Component.translatable(Util_MessageKeys.DELIVERY_BOX_SOURCE_KEY, item.getSource()),
                        source,
                        0xAAAAAA
                );
            });

            addItemButtons();

            y += THING_SPACING; // 调整下一件商品的位置
        }

        super.initializeRenderCache();
    }

    @Override
    protected void detectMouseHoverAndRenderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        initPosition();
        int y = startY;

        for (int i = startIndex; i < endIndex; i++) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return;

            DeliveryItem item = filteredItems.get(i);

            if (isMouseOver(mouseX, mouseY, startX, y, 16, 16)) {
                List<Component> tooltipLines = item.getItemStack().getTooltipLines(
                        player,
                        Minecraft.getInstance().options.advancedItemTooltips ?
                                TooltipFlag.ADVANCED : TooltipFlag.NORMAL
                );
                tooltipLines.add(Component.literal("-=-=-=-=-=-").withStyle(ChatFormatting.DARK_GRAY));

                tooltipLines.add(Component.translatable(Util_MessageKeys.DELIVERY_BOX_DATA_ID_KEY, item.getDataID()));
                    tooltipLines.add(Component.translatable(Util_MessageKeys.DELIVERY_BOX_ITEM_ID_KEY, item.getItemID()));
                tooltipLines.add(Component.translatable(Util_MessageKeys.DELIVERY_BOX_SOURCE_KEY, item.getSource()));

                guiGraphics.renderTooltip(this.font, tooltipLines, Optional.empty(), mouseX, mouseY);
            }

            y += THING_SPACING;
        }
    }

    // 添加购买按钮
    private void addItemButtons() {
        initPosition();

        int y = startY;
        for (int i = startIndex; i < endIndex; i++) {
            DeliveryItem item = filteredItems.get(i);

            this.addRenderableWidget(
                    new AnimatedButton(
                            this.width + 60,
                            y,
                            this.width - startX - 60,
                            y,
                            60, 20,
                            Component.translatable(Util_MessageKeys.DELIVERY_BOX_CLAIM_BUTTON_KEY),
                            1000,
                            button -> {
                                EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_DeliveryBoxClaimItem(item.getDataID()));
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

    // 添加翻页按钮
    @Override
    protected void addPageButtons() {
        initPosition();
        int buttonY = this.height - 40;

        // 上一页按钮
        this.addRenderableWidget(
                Button.builder(Component.literal("<"), button -> {
                            if (currentPage > 0) {
                                currentPage--;
                                this.init(); // 刷新页面
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
                                this.init(); // 刷新页面
                            }
                        })
                        .pos(this.width - startX - PAGE_BUTTON_WIDTH, buttonY)
                        .size(PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT)
                        .build()
        );
    }

    public void updateDeliveryBoxItems(List<DeliveryItem> deliveryItems) {
        this.items = deliveryItems;
        this.filteredItems = new ArrayList<>(items); // 初始化过滤后的列表
        this.itemsSnapshot = new ArrayList<>(items); // 初始化过滤后的列表
        this.init(); // 每次更新物品后重新初始化界面
    }

    // 动态计算总页数
    private int getTotalPages() {
        return (int) Math.ceil((double) this.filteredItems.size() / thingsPerPage);
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
            List<DeliveryItem> result = itemsSnapshot;

            // 2. 应用搜索条件
            if (searchBox != null && !searchBox.getValue().isEmpty()) {
                result = result.stream()
                        .filter(item -> itemMatchesSearch(item, searchBox.getValue()))
                        .collect(Collectors.toList());
            }

            // 3. 更新UI
            List<DeliveryItem> finalResult = result;
            this.minecraft.execute(() -> {
                this.filteredItems = finalResult;
                this.currentPage = 0;
                refreshItemButtons();
                initializeRenderCache(); // 重新初始化渲染缓存
            });
        }).start();
    }

    private boolean itemMatchesSearch(DeliveryItem item, String searchText) {
        return item.getItemID().toLowerCase().contains(searchText.toLowerCase()) ||
                item.getSource().toLowerCase().contains(searchText.toLowerCase()) ||
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
        return buttonMessage.equals(Component.translatable(Util_MessageKeys.DELIVERY_BOX_CLAIM_BUTTON_KEY));
    }

    @Override
    protected void initPosition(){
        TOP_MARGIN = this.height - 100;
        thingsPerPage = Math.max(1, TOP_MARGIN / THING_SPACING);

        startIndex = currentPage * thingsPerPage;
        endIndex = Math.min(startIndex + thingsPerPage, filteredItems.size());

        startX = Math.max((this.width / 2) - 300, 60);
        startY = Math.max((this.height - 450) / 4, 55);
    }
}

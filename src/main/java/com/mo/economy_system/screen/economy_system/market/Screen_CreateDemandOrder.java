package com.mo.economy_system.screen.economy_system.market;

import com.mo.economy_system.network.packets.economy_system.demand_order.Packet_CreateDemandOrder;
import com.mo.economy_system.core.economy_system.market.DemandOrder;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.screen.EconomySystem_Screen;
import com.mo.economy_system.screen.components.AnimatedButton;
import com.mo.economy_system.screen.components.AnimatedHighLevelTextField;
import com.mo.economy_system.screen.components.ItemIconAnimation;
import com.mo.economy_system.screen.components.TextAnimation;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;
import java.util.UUID;

public class Screen_CreateDemandOrder extends EconomySystem_Screen {

    private final Player player;
    private AnimatedHighLevelTextField priceInput; // 输入框用于设置价格
    private AnimatedHighLevelTextField itemIDInput; // 输入框用于设置物品ID
    private AnimatedHighLevelTextField itemCountInput; // 输入框用于设置物品数量

    private ItemStack itemStackToRequest = ItemStack.EMPTY; // 当前请求的物品
    private Component errorMessage = null; // 错误消息
    private boolean canRequest = false;

    private ItemIconAnimation icon;
    private TextAnimation name;
    private TextAnimation error;
    private TextAnimation itemID;
    private TextAnimation count;
    private TextAnimation price;

    public Screen_CreateDemandOrder(Player player) {
        super(Component.translatable(Util_MessageKeys.REQUEST_TITLE_KEY));
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();

        initPart();

    }

    @Override
    protected void initPart() {

        // 清除所有组件
        this.clearWidgets();

        if (flag == 0) {

            this.itemIDInput = new AnimatedHighLevelTextField(
                    this.font,
                    this.width / 2 - 75,
                    this.height + 20,
                    150,
                    20,
                    1000,
                    Component.literal("Enter Item ID")
            );
            this.addRenderableWidget(itemIDInput);

            // 设置搜索框的键盘监听器
            this.itemIDInput.setFocused(false); // 默认不聚焦
            // this.countInput.setMaxLength(50); // 限制输入长度
            this.itemIDInput.setHint(Component.translatable(Util_MessageKeys.REQUEST_ITEM_ID_HINT_TEXT_KEY)); // 提示文本
            this.itemIDInput.setResponder(text -> checkItemID());
            this.itemIDInput.startMoveAnimation(this.width / 2 - 75, this.height / 2 - 45);

            this.itemCountInput = new AnimatedHighLevelTextField(
                    this.font,
                    this.width / 2 - 75,
                    this.height + 20,
                    150,
                    20,
                    1000,
                    Component.literal("Enter Item Count")
            );
            this.addRenderableWidget(itemCountInput);

            // 设置搜索框的键盘监听器
            this.itemCountInput.setFocused(false); // 默认不聚焦
            // this.countInput.setMaxLength(50); // 限制输入长度
            this.itemCountInput.setHint(Component.translatable(Util_MessageKeys.REQUEST_ITEM_COUNT_HINT_TEXT_KEY)); // 提示文本
            this.itemCountInput.setResponder(text -> checkItemID());
            this.itemCountInput.startMoveAnimation(this.width / 2 - 75, this.height / 2 - 20);

            this.priceInput = new AnimatedHighLevelTextField(
                    this.font,
                    this.width / 2 - 75,
                    this.height + 20,
                    150,
                    20,
                    1000,
                    Component.literal("Enter Price")
            );
            this.addRenderableWidget(priceInput);

            // 设置搜索框的键盘监听器
            this.priceInput.setFocused(false); // 默认不聚焦
            // this.countInput.setMaxLength(50); // 限制输入长度
            this.priceInput.setHint(Component.translatable(Util_MessageKeys.REQUEST_PRICE_HINT_TEXT_KEY)); // 提示文本
            this.priceInput.setResponder(text -> checkItemID());
            this.priceInput.startMoveAnimation(this.width / 2 - 75, this.height / 2 + 5);

            // 添加求购按钮
            this.addRenderableWidget(
                    new AnimatedButton(
                            this.width / 2 - 50,
                            this.height + 20,
                            this.width / 2 - 50,
                            this.height / 2 + 35,
                            100,
                            20,
                            Component.translatable(Util_MessageKeys.REQUEST_REQUEST_BUTTON_KEY),
                            1000,
                            button -> {
                                requestItem();
                            }
                    )
            );

        } else if (flag >= 1) {

            this.itemIDInput = new AnimatedHighLevelTextField(
                    this.font,
                    this.width / 2 - 75,
                    this.height / 2 - 45,
                    150,
                    20,
                    1000,
                    Component.literal("Enter Item ID")
            );
            this.addRenderableWidget(itemIDInput);

            // 设置搜索框的键盘监听器
            this.itemIDInput.setFocused(false); // 默认不聚焦
            // this.countInput.setMaxLength(50); // 限制输入长度
            this.itemIDInput.setHint(Component.translatable(Util_MessageKeys.REQUEST_ITEM_ID_HINT_TEXT_KEY)); // 提示文本
            this.itemIDInput.setResponder(text -> checkItemID());
            this.itemIDInput.startMoveAnimation(this.width / 2 - 75, this.height / 2 - 45);

            this.itemCountInput = new AnimatedHighLevelTextField(
                    this.font,
                    this.width / 2 - 75,
                    this.height / 2 - 20,
                    150,
                    20,
                    1000,
                    Component.literal("Enter Item Count")
            );
            this.addRenderableWidget(itemCountInput);

            // 设置搜索框的键盘监听器
            this.itemCountInput.setFocused(false); // 默认不聚焦
            // this.countInput.setMaxLength(50); // 限制输入长度
            this.itemCountInput.setHint(Component.translatable(Util_MessageKeys.REQUEST_ITEM_COUNT_HINT_TEXT_KEY)); // 提示文本
            this.itemCountInput.setResponder(text -> checkItemID());
            this.itemCountInput.startMoveAnimation(this.width / 2 - 75, this.height / 2 - 20);

            this.priceInput = new AnimatedHighLevelTextField(
                    this.font,
                    this.width / 2 - 75,
                    this.height / 2 + 5,
                    150,
                    20,
                    1000,
                    Component.literal("Enter Price")
            );
            this.addRenderableWidget(priceInput);

            // 设置搜索框的键盘监听器
            this.priceInput.setFocused(false); // 默认不聚焦
            // this.countInput.setMaxLength(50); // 限制输入长度
            this.priceInput.setHint(Component.translatable(Util_MessageKeys.REQUEST_PRICE_HINT_TEXT_KEY)); // 提示文本
            this.priceInput.setResponder(text -> checkItemID());
            this.priceInput.startMoveAnimation(this.width / 2 - 75, this.height / 2 + 5);

            // 添加求购按钮
            this.addRenderableWidget(
                    new AnimatedButton(
                            this.width / 2 - 50,
                            this.height / 2 + 35,
                            this.width / 2 - 50,
                            this.height / 2 + 35,
                            100,
                            20,
                            Component.translatable(Util_MessageKeys.REQUEST_REQUEST_BUTTON_KEY),
                            1000,
                            button -> {
                                requestItem();
                            }
                    )
            );

        }

        flag ++;

        initializeRenderCache();

        super.initPart();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {

        this.flag = 0;

        super.resize(minecraft, width, height);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);

        // 执行渲染缓存中的任务
        for (RunnableWithGraphics task : renderCache) {
            task.run(guiGraphics);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void initializeRenderCache() {
        renderCache.clear();

        // 渲染物品信息或错误消息
        if (itemStackToRequest != ItemStack.EMPTY) {

            int textWidth = this.font.width(itemStackToRequest.getHoverName().getString());
            int totalWidth = textWidth + 16 + 2; // 物品图标宽度为16，间距为2
            int xPosition = (this.width - totalWidth) / 2;

            icon = new ItemIconAnimation(
                    xPosition,
                    this.height / 2 - 65,
                    xPosition,
                    this.height / 2 - 65,
                    0f,
                    1f,
                    0.8f,
                    1f,
                    1000
            );

            name = new TextAnimation(
                    xPosition + 16 + 2,
                    this.height / 2 - 56,
                    xPosition + 16 + 2,
                    this.height / 2 - 56,
                    0f,
                    1f,
                    1000
            );

            renderCache.add((guiGraphics) -> {

                renderAnimatedItem(
                        guiGraphics,
                        itemStackToRequest,
                        icon
                );

                renderAnimatedText(
                        guiGraphics,
                        Component.literal(itemStackToRequest.getHoverName().getString()),
                        name,
                        0xFFFFFF
                );

            });

        } else if (errorMessage != null) {

            int textWidth = this.font.width(errorMessage);
            int xPosition = (this.width - textWidth) / 2;

            error = new TextAnimation(
                    xPosition,
                    this.height / 2 - 65,
                    xPosition,
                    this.height / 2 - 65,
                    0f,
                    1f,
                    1000
            );

            renderCache.add((guiGraphics) -> {

                renderAnimatedText(
                        guiGraphics,
                        errorMessage,
                        error,
                        0xAAAAAA
                );

            });

        }

        itemID = new TextAnimation(
                this.width / 2 - 78 - 45,
                this.height / 2 - 40,
                this.width / 2 - 78 - 45,
                this.height / 2 - 40,
                0f,
                1f,
                1000
        );

        count = new TextAnimation(
                this.width / 2 - 78 - 45,
                this.height / 2 - 15,
                this.width / 2 - 78 - 45,
                this.height / 2 - 15,
                0f,
                1f,
                1000
        );

        price = new TextAnimation(
                this.width / 2 - 78 - 45,
                this.height / 2 + 10,
                this.width / 2 - 78 - 45,
                this.height / 2 + 10,
                0f,
                1f,
                1000
        );

        renderCache.add((guiGraphics) -> {

            renderAnimatedText(
                    guiGraphics,
                    Component.translatable(Util_MessageKeys.REQUEST_ITEM_ID_TEXT_KEY),
                    itemID,
                    0xFFFFFF
            );

            renderAnimatedText(
                    guiGraphics,
                    Component.translatable(Util_MessageKeys.REQUEST_ITEM_COUNT_TEXT_KEY),
                    count,
                    0xFFFFFF
            );

            renderAnimatedText(
                    guiGraphics,
                    Component.translatable(Util_MessageKeys.REQUEST_PRICE_TEXT_KEY),
                    price,
                    0xFFFFFF
            );
        });

        super.initializeRenderCache();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.itemIDInput.isFocused() && keyCode == 257) { // 检测回车键（keyCode 257）
            checkItemID();
            return true; // 防止事件进一步传播
        }

        if (this.itemCountInput.isFocused() && keyCode == 257) { // 检测回车键（keyCode 257）
            checkItemID();
            return true; // 防止事件进一步传播
        }

        if (this.priceInput.isFocused() && keyCode == 257) { // 检测回车键（keyCode 257）
            checkItemID();
            return true; // 防止事件进一步传播
        }

        if (keyCode == 256 && this.shouldCloseOnEsc()) {
            Minecraft.getInstance().setScreen(new Screen_Market());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void checkItemID() {
        String itemID = this.itemIDInput.getValue();
        String itemCount = this.itemCountInput.getValue();
        String itemPrice = this.priceInput.getValue();
        // 重置状态
        itemStackToRequest = ItemStack.EMPTY;
        errorMessage = null;

        // 验证物品ID
        if (itemID.isEmpty() || getItemStack(itemID).is(Items.AIR)) {
            errorMessage = Component.translatable(Util_MessageKeys.REQUEST_UNKNOWN_ITEM_ID_KEY);
        } else {
            // 验证物品数量
            if (itemCount.isEmpty() || Integer.parseInt(itemCount) <= 0) {
                errorMessage = Component.translatable(Util_MessageKeys.REQUEST_INVALID_ITEM_COUNT_KEY);
            } else {

                itemStackToRequest = getItemStack(itemID);
                int count = Integer.parseInt(itemCount);
                System.out.println(itemStackToRequest.getItem().getMaxStackSize());

                // 验证物品数量是否超过最大堆叠数
                if (count > itemStackToRequest.getItem().getMaxStackSize()) {
                    errorMessage = Component.translatable(Util_MessageKeys.REQUEST_EXCESSIVE_ITEM_COUNT_KEY);
                    itemStackToRequest = ItemStack.EMPTY; // 清空物品
                } else {
                    if (itemPrice.isEmpty() || Integer.parseInt(itemPrice) <= 0) {
                        errorMessage = Component.translatable(Util_MessageKeys.REQUEST_INVALID_PRICE_KEY);
                        itemStackToRequest = ItemStack.EMPTY; // 清空物品
                    } else {
                        canRequest = true;
                    }
                }
            }
        }
        initializeRenderCache();
    }

    @Override
    public boolean isPauseScreen() {
        return false; // 界面打开时不暂停游戏
    }

    private void requestItem() {
        if (canRequest) {
            String priceText = priceInput.getValue();
            Optional<Integer> price = parsePrice(priceText);

            if (price.isEmpty() || price.get() <= 0) {
                this.player.sendSystemMessage(Component.translatable(Util_MessageKeys.LIST_INVALID_PRICE_MESSAGE_KEY));
                return;
            }

            // 创建 MarketItem 对象时，自动生成唯一商品 ID
            DemandOrder marketItem = new DemandOrder(
                    UUID.randomUUID(),
                    itemStackToRequest.getItem().getDescriptionId(), // 物品描述 ID
                    itemStackToRequest.copy(), // 复制物品
                    price.get(),
                    player.getName().getString(),
                    player.getUUID(),
                    System.currentTimeMillis(),
                    false
            );

            // 发送上架请求到服务端
            EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_CreateDemandOrder(marketItem));

            this.minecraft.setScreen(new Screen_Market()); // 关闭界面
        }
    }


    private Optional<Integer> parsePrice(String priceText) {
        try {
            return Optional.of(Integer.parseInt(priceText));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private ItemStack getItemStack(String itemID) {
        try {
            // 验证 itemID 是否只包含有效字符 [a-z0-9/._-]
            if (!itemID.matches("^[a-z0-9/._-:]+$")) {
                return ItemStack.EMPTY;
            }
            Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(itemID));
            if (item != null) {
                return new ItemStack(item);
            }
        } catch (Exception e) {
            // 忽略异常，返回空堆
        }
        return ItemStack.EMPTY; // 如果物品 ID 无效，返回空堆
    }
}

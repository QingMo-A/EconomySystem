package com.mo.economy_system.screen.economy_system.market;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.economy_system.sales_order.Packet_CreateSalesOrder;
import com.mo.economy_system.core.economy_system.market.SalesOrder;
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
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

public class Screen_CreateSalesOrder extends EconomySystem_Screen {

    private final Player player;
    private AnimatedHighLevelTextField priceInput; // 输入框用于设置价格

    private TextAnimation noItem;
    private ItemIconAnimation icon;
    private TextAnimation name;
    private TextAnimation count;
    private TextAnimation price;

    public Screen_CreateSalesOrder(Player player) {
        super(Component.translatable(Util_MessageKeys.LIST_TITLE_KEY));
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

            // 添加价格输入框
            priceInput = new AnimatedHighLevelTextField(
                    this.font,
                    this.width / 2 - 75,
                    this.height + 2,
                    150,
                    20,
                    1000,
                    Component.literal("Enter Price"));
            this.addRenderableWidget(priceInput);

            // 设置搜索框的键盘监听器
            this.priceInput.setFocused(false); // 默认不聚焦
            this.priceInput.setHint(Component.translatable(Util_MessageKeys.LIST_HINT_TEXT_KEY)); // 提示文本
            // this.priceInput.setResponder(text -> pricePrediction());
            this.priceInput.startMoveAnimation(this.width / 2 - 75, this.height / 2 - 20);

            this.addRenderableWidget(
                    new AnimatedButton(
                            this.width / 2 - 50,
                            this.height + 20,
                            this.width / 2 - 50,
                            this.height / 2 + 10,
                            100,
                            20,
                            Component.translatable(Util_MessageKeys.LIST_LIST_BUTTON_KEY),
                            1000,
                            button -> {
                                listItem();
                            }
                    )
            );

        } else if (flag >= 1) {

            // 添加价格输入框
            priceInput = new AnimatedHighLevelTextField(
                    this.font,
                    this.width / 2 - 75,
                    this.height / 2 - 20,
                    150,
                    20,
                    1000,
                    Component.literal("Enter Price"));
            this.addRenderableWidget(priceInput);

            // 设置搜索框的键盘监听器
            this.priceInput.setFocused(false); // 默认不聚焦
            this.priceInput.setHint(Component.translatable(Util_MessageKeys.LIST_HINT_TEXT_KEY)); // 提示文本
            // this.priceInput.setResponder(text -> pricePrediction());
            this.priceInput.startMoveAnimation(this.width / 2 - 75, this.height / 2 - 20);

            // 添加上架按钮
            this.addRenderableWidget(
                    Button.builder(Component.translatable(Util_MessageKeys.LIST_LIST_BUTTON_KEY), button -> listItem())
                            .pos(this.width / 2 - 50, this.height / 2 + 10)
                            .size(100, 20)
                            .build()
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

        // 获取玩家手中的物品
        ItemStack heldItem = player.getMainHandItem();

        // 渲染手中物品信息
        if (!heldItem.isEmpty()) {
            int textWidth = this.font.width(heldItem.getHoverName().getString());

            // 计算整体宽度（物品图标宽度为16，间距为2）
            int totalWidth = textWidth + 16 + 2;

            // 计算整体的居中位置
            int xPosition = (this.width - totalWidth) / 2;

            icon = new ItemIconAnimation(
                    xPosition,
                    this.height / 2 - 40,
                    xPosition,
                    this.height / 2 - 40,
                    0f,
                    1f,
                    0.8f,
                    1f,
                    1000
            );

            name = new TextAnimation(
                    xPosition + 16 + 2,
                    this.height / 2 - 36,
                    xPosition + 16 + 2,
                    this.height / 2 - 36,
                    0f,
                    1f,
                    1000
            );

            renderCache.add((guiGraphics) -> {

                renderAnimatedItem(
                        guiGraphics,
                        heldItem,
                        icon
                );

                renderAnimatedText(
                        guiGraphics,
                        Component.literal(heldItem.getHoverName().getString()),
                        name,
                        0xFFFFFF
                );

            });

        } else {
            // 动态计算文字居中的位置
            int textWidth = this.font.width(Component.translatable(Util_MessageKeys.LIST_NO_ITEM_IN_HAND_TEXT_KEY));
            int xPosition = (this.width - textWidth) / 2;

            noItem = new TextAnimation(
                    xPosition,
                    this.height / 2 - 40,
                    xPosition,
                    this.height / 2 - 40,
                    0f,
                    1f,
                    2000
            );

            renderCache.add((guiGraphics) -> {

                renderAnimatedText(
                        guiGraphics,
                        Component.translatable(Util_MessageKeys.LIST_NO_ITEM_IN_HAND_TEXT_KEY),
                        noItem,
                        0xAAAAAA
                );

            });
        }

        int textWidth = this.font.width(Component.translatable(Util_MessageKeys.LIST_PRICE_TEXT_KEY));

        price = new TextAnimation(
                this.width / 2 - 78 - textWidth,
                this.height / 2 - 15,
                this.width / 2 - 78 - textWidth,
                this.height / 2 - 15,
                0f,
                1f,
                1000
        );

        renderCache.add((guiGraphics) -> {

            renderAnimatedText(
                    guiGraphics,
                    Component.translatable(Util_MessageKeys.LIST_PRICE_TEXT_KEY),
                    price,
                    0xFFFFFF
            );

        });

        super.initializeRenderCache();
    }

    private void listItem() {
        ItemStack heldItem = player.getMainHandItem(); // 获取玩家手中的物品

        if (heldItem.isEmpty()) {
            this.player.sendSystemMessage(Component.translatable(Util_MessageKeys.LIST_NO_ITEM_IN_HAND_MESSAGE_KEY));
            return;
        }

        String priceText = priceInput.getValue();
        Optional<Integer> price = parsePrice(priceText);

        if (price.isEmpty() || price.get() <= 0) {
            this.player.sendSystemMessage(Component.translatable(Util_MessageKeys.LIST_INVALID_PRICE_MESSAGE_KEY));
            return;
        }

        // 创建 MarketItem 对象时，自动生成唯一商品 ID
        SalesOrder salesOrder = new SalesOrder(
                UUID.randomUUID(),
                heldItem.getItem().getDescriptionId(), // 物品描述 ID
                heldItem.copy(), // 复制物品
                price.get(),
                player.getName().getString(),
                player.getUUID(),
                System.currentTimeMillis()
        );

        // 发送上架请求到服务端
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_CreateSalesOrder(salesOrder));

        this.minecraft.setScreen(new Screen_Market());
    }


    private Optional<Integer> parsePrice(String priceText) {
        try {
            return Optional.of(Integer.parseInt(priceText));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && this.shouldCloseOnEsc()) {
            Minecraft.getInstance().setScreen(new Screen_Market());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}

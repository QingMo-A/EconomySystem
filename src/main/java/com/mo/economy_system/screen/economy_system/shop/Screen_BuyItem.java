package com.mo.economy_system.screen.economy_system.shop;

import com.mo.economy_system.core.economy_system.shop.ShopItem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.economy_system.Packet_ShopBuyItem;
import com.mo.economy_system.screen.EconomySystem_Screen;
import com.mo.economy_system.screen.components.AnimatedButton;
import com.mo.economy_system.screen.components.AnimatedHighLevelTextField;
import com.mo.economy_system.screen.components.ItemIconAnimation;
import com.mo.economy_system.screen.components.TextAnimation;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class Screen_BuyItem extends EconomySystem_Screen {

    private Player player;
    private ShopItem shopItem;
    private ItemStack itemStack;
    private AnimatedHighLevelTextField countInput; // 输入框用于设置价格

    private TextAnimation noItem;
    private ItemIconAnimation icon;
    private TextAnimation name;
    private TextAnimation count;
    private TextAnimation price;

    protected Screen_BuyItem(ShopItem shopItem) {
        super(Component.translatable(Util_MessageKeys.SHOP_BUY_TITLE_KEY));
        this.shopItem = shopItem;
        this.itemStack = shopItem.getItemStack();
    }

    @Override
    protected void init() {
        super.init();

        if (this.minecraft != null && this.minecraft.player != null) {
            this.player = this.minecraft.player;
        }

        initPart();

    }

    @Override
    protected void initPart() {

        // 清除所有组件
        this.clearWidgets();

        if (flag == 0) {
            this.countInput = new AnimatedHighLevelTextField(
                    this.font,
                    this.width / 2 - 75,
                    this.height + 20,
                    150,
                    20,
                    1000,
                    Component.literal("Enter Count")
            );
            this.addRenderableWidget(countInput);

            // 设置搜索框的键盘监听器
            this.countInput.setFocused(false); // 默认不聚焦
            this.countInput.setMaxLength(50); // 限制输入长度
            this.countInput.setHint(Component.translatable(Util_MessageKeys.SHOP_BUY_HINT_TEXT_KEY));
            // this.countInput.setResponder(text -> pricePrediction());
            this.countInput.startMoveAnimation(this.width / 2 - 75, this.height / 2 - 20);

            this.addRenderableWidget(
                    new AnimatedButton(
                            this.width / 2 - 50,
                            this.height + 20,
                            this.width / 2 - 50,
                            this.height / 2 + 10,
                            100,
                            20,
                            Component.translatable(Util_MessageKeys.SHOP_BUY_BUY_BUTTON_KEY),
                            1000,
                            button -> {
                                buyItem();
                            }
                    )
            );

        } else if (flag >= 1){
            this.countInput = new AnimatedHighLevelTextField(
                    this.font,
                    this.width / 2 - 75,
                    this.height / 2 - 20,
                    150,
                    20,
                    1000,
                    Component.literal("Enter Count")
            );
            this.addRenderableWidget(countInput);

            // 设置搜索框的键盘监听器
            this.countInput.setFocused(false); // 默认不聚焦
            this.countInput.setMaxLength(50); // 限制输入长度
            this.countInput.setHint(Component.translatable(Util_MessageKeys.SHOP_BUY_HINT_TEXT_KEY));
            this.countInput.setResponder(text -> pricePrediction());
            this.countInput.startMoveAnimation(this.width / 2 - 75, this.height / 2 - 20);

            this.addRenderableWidget(
                    Button.builder(Component.translatable(Util_MessageKeys.SHOP_BUY_BUY_BUTTON_KEY), button -> buyItem())
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

        // 渲染物品信息
        if (!itemStack.isEmpty()) {
            int textWidth = this.font.width(itemStack.getHoverName().getString());

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
                        itemStack,
                        icon
                );

                renderAnimatedText(
                        guiGraphics,
                        Component.literal(itemStack.getHoverName().getString()),
                        name,
                        0xFFFFFF
                );

            });

        } else {

            // 动态计算文字居中的位置
            int textWidth = this.font.width(Component.translatable(Util_MessageKeys.SHOP_BUY_NO_ITEM_TEXT_KEY));
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
                        Component.translatable(Util_MessageKeys.SHOP_BUY_NO_ITEM_TEXT_KEY),
                        noItem,
                        0xAAAAAA
                );

            });

        }

        int textWidth = this.font.width(Component.translatable(Util_MessageKeys.SHOP_BUY_COUNT_TEXT_KEY));

        count = new TextAnimation(
                this.width / 2 - 78 - textWidth,
                this.height / 2 - 15,
                this.width / 2 - 78 - textWidth,
                this.height / 2 - 15,
                0f,
                1f,
                1000
        );

        /*price = new TextAnimation(
                this.width
        );*/

        renderCache.add((guiGraphics) -> {

            renderAnimatedText(
                    guiGraphics,
                    Component.translatable(Util_MessageKeys.SHOP_BUY_COUNT_TEXT_KEY),
                    count,
                    0xFFFFFF
            );

        });

    }

    private void pricePrediction() {

    }

    private void buyItem() {
        if (itemStack.isEmpty()) {
            this.player.sendSystemMessage(Component.translatable(Util_MessageKeys.SHOP_BUY_NO_ITEM_MESSAGE_KEY));
            return;
        }

        String countText = countInput.getValue();
        Optional<Integer> count = parsePrice(countText);

        if (count.isEmpty() || count.get() <= 0) {
            this.player.sendSystemMessage(Component.translatable(Util_MessageKeys.SHOP_BUY_INVALID_COUNT_MESSAGE_KEY));
            return;
        }

        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_ShopBuyItem(shopItem.getItemId(), shopItem.getNbt(), shopItem.getCurrentPrice(), count.get()));

        this.minecraft.setScreen(new Screen_Shop());
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
            Minecraft.getInstance().setScreen(new Screen_Shop());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}

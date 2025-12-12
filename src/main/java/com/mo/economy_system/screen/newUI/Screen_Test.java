package com.mo.economy_system.screen.newUI;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Screen_Test extends Screen {
    private static final Component TITLE = Component.literal("Layout Demo Screen");
    private static final Logger LOGGER = LogManager.getLogger(); // Logging system
    private HBoxWidget hBox;

    public Screen_Test() {
        super(TITLE);
    }

    @Override
    protected void init() {
        hBox = new HBoxWidget(30, 30, 20)
                .setSpacing(10)
                .setPadding(6, 10, 6, 10)
                .setBorderColor(0x55FFFFFF)
                .showBorder(true, true, true, true)
                .setBorderThickness(2)
                .setBoxWidth(300)
                .setBackgroundColor(0x50FFFFFF)
                .enableScrollbar(true); // 底部滚动条

        ItemStack apple = Items.APPLE.getDefaultInstance();
        apple.setCount(5);
        ItemIconWidget icon = new ItemIconWidget(apple, font, 0, 0)
                .setScale(1.3f)
                .setShowDecorations(true);

        LabelWidget name = new LabelWidget(font, Component.literal("name"), 0, 0, 0xFFFFFF, false);
        LabelWidget price = new LabelWidget(font, Component.literal("price"), 0, 0, 0xFFFAAF, false);

        hBox.addAllChildren(
                icon
        );

        addRenderableWidget(hBox);
    }
}

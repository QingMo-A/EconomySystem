package com.mo.economy_system.screen.newUI;

import com.mo.economy_system.screen.components.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Screen_Test extends Screen_Father {
    private static final Component TITLE = Component.literal("Layout Demo Screen");
    private static final Logger LOGGER = LogManager.getLogger(); // Logging system

    public Screen_Test() {
        super(TITLE);
    }

    @Override
    protected void init() {
        super.init();

        /*// =============== Example 1: Basic HBox ===============
        HBox hbox1 = new HBox(20, 50, 5);

        // 使用 lambda，但不直接引用变量
        Button button1 = new Button.Builder(Component.literal("Button 1"), b -> {
            LOGGER.info("Button 1 clicked at position: ({}, {})", b.getX(), b.getY());
        }).size(60, 20).build();

        Button button2 = new Button.Builder(Component.literal("Button 2"), b -> {
            LOGGER.info("Button 2 clicked at position: ({}, {})", b.getX(), b.getY());
        }).size(70, 20).build();

        hbox1.addChild(button1);
        hbox1.addChild(button2);

        // Set border
        hbox1.setBorderStyle(new BorderStyle().color(0xFFFF0000).thickness(2).showEdges(true, true, true, true));

        this.addContainerWidget(hbox1); // Automatically register via Screen_Father


        // =============== Example 2: Nested VBox ===============
        VBox mainVBox = new VBox(width / 2 - 100, 100, 8);
        HBox nestedHBox = new HBox(0, 0, 10);

        Button nestedButton1 = new Button.Builder(Component.literal("Nested 1"), b -> {
            LOGGER.info("Nested HBox Button 1 clicked at position: ({}, {})", b.getX(), b.getY());
        }).size(80, 20).build();

        Button nestedButton2 = new Button.Builder(Component.literal("Nested 2"), b -> {
            LOGGER.info("Nested HBox Button 2 clicked at position: ({}, {})", b.getX(), b.getY());
        }).size(80, 20).build();

        nestedHBox.addChild(nestedButton1);
        nestedHBox.addChild(nestedButton2);

        // Set border
        nestedHBox.setBorderStyle(new BorderStyle().color(0xFF0000FF).thickness(3).showEdges(true, true, true, true));
        mainVBox.addChild(nestedHBox);

        Button verticalButton = new Button.Builder(Component.literal("Vertical Button"), b -> {
            LOGGER.info("Vertical Button clicked at position: ({}, {})", b.getX(), b.getY());
        }).size(160, 30).build();

        mainVBox.addChild(verticalButton);

        // Automatically register the main VBox
        this.addContainerWidget(mainVBox);


        // =============== Example 3: Toggle Border Button ===============
        Button toggleBorderBtn = new Button.Builder(Component.literal("Toggle Border"), b -> {
            BorderStyle style = mainVBox.getBorderStyle();
            style.showEdges(!style.isShowTop(), !style.isShowBottom(), !style.isShowLeft(), !style.isShowRight());
            LOGGER.info("Toggle Border Button clicked at position: ({}, {}), New border state: top={}, bottom={}, left={}, right={}",
                    b.getX(), b.getY(),
                    style.isShowTop(), style.isShowBottom(), style.isShowLeft(), style.isShowRight());
        }).pos(20, height - 30).size(100, 20).build();

        this.addRenderableWidget(toggleBorderBtn);*/
        // 父容器：HBox（自动尺寸）
        /*HBox parentHBox = new HBox(10);
        parentHBox.setPosition(50, 50);

        Button nestedButton1 = new Button.Builder(Component.literal("Nested 1"), b -> {
            LOGGER.info("Nested HBox Button 1 clicked at position: ({}, {})", b.getX(), b.getY());
        }).size(80, 20).build();

        Button nestedButton2 = new Button.Builder(Component.literal("Nested 2"), b -> {
            LOGGER.info("Nested HBox Button 2 clicked at position: ({}, {})", b.getX(), b.getY());
        }).size(80, 20).build();

        // 子容器1：VBox（手动尺寸）
        VBox childVBox = new VBox(200, 150, 5);
        childVBox.setBorderStyle(new BorderStyle().color(0xFF0000FF).thickness(3).showEdges(true, true, true, true));
        childVBox.addChild(nestedButton1); // 自动定位
        childVBox.addChild(nestedButton2);

        Button button1 = new Button.Builder(Component.literal("Button 1"), b -> {
            LOGGER.info("Button 1 clicked at position: ({}, {})", b.getX(), b.getY());
        }).size(60, 20).build();

        Button button2 = new Button.Builder(Component.literal("Button 2"), b -> {
            LOGGER.info("Button 2 clicked at position: ({}, {})", b.getX(), b.getY());
        }).size(70, 20).build();

        // 子容器2：手动定位的 HBox
        HBox manualHBox = new HBox(15);
        manualHBox.setBorderStyle(new BorderStyle().color(0xFFAAAAFF).thickness(3).showEdges(true, true, true, true));
        manualHBox.setPosition(20, 30); // 相对于父 HBox 的坐标
        manualHBox.addChild(button1);
        manualHBox.addChild(button2);

        parentHBox.addChild(childVBox);
        parentHBox.addChild(manualHBox);

        this.addRenderableWidget(parentHBox);*/
        int actionBarWidth = this.width;
        int actionBarHeight = this.height / 9;

        int sideBarWidth = this.width / 7;
        int sidebarHeight = this.height - actionBarHeight;

        int titleWidth = this.width - sideBarWidth;
        int titleHeight = actionBarHeight;

        HBox actionBar = new HBox(actionBarWidth, actionBarHeight, 10);
        actionBar.setBorderStyle(new BorderStyle().color(0xFFFFFFFF).thickness(2).showEdges(true, true, true, true));
        actionBar.setPosition(0, this.height - actionBarHeight);

        VBox sideBar = new VBox(sideBarWidth, sidebarHeight, 10);
        sideBar.setPadding(10, 20, 10, 10);
        sideBar.setBorderStyle(new BorderStyle().color(0xFFFFFFFF).thickness(2).showEdges(true, false, true, true));
        sideBar.setPosition(0, 0);

        HBox titleText = new HBox(10);
        titleText.setPadding(0, 0, 0, 3);
        titleText.setBorderStyle(new BorderStyle().color(0xFFFFFFFF).thickness(1).showEdges(false, true, false, false));
        // 创建文本组件
        TextWidget label = new TextWidget(font, Component.literal("Test Title"), 0, 0, 0xFFFFFF);
        titleText.addChild(label);
        sideBar.addChild(titleText);

        Button button1 = new Button.Builder(Component.literal("Button 1"), b -> {
            LOGGER.info("Button 1 clicked at position: ({}, {})", b.getX(), b.getY());
        }).size(70, 20).build();

        Button button2 = new Button.Builder(Component.literal("Button 2"), b -> {
            LOGGER.info("Button 2 clicked at position: ({}, {})", b.getX(), b.getY());
        }).size(70, 20).build();
        sideBar.addChild(button1);
        sideBar.addChild(button2);

        // 添加一个可扩展的空白区域
        SpacerWidget spacer = new SpacerWidget(false, true);
        sideBar.addChild(spacer);

        HBox titleBar = new HBox(this.width - sideBar.getWidth(), titleHeight, 10);
        titleBar.setBorderStyle(new BorderStyle().color(0xFFFFFFFF).thickness(2).showEdges(true, true, false, true));
        titleBar.setPosition(sideBar.getWidth(), 0);

        this.addRenderableWidget(actionBar);
        this.addRenderableWidget(sideBar);
        this.addRenderableWidget(titleBar);
    }
}

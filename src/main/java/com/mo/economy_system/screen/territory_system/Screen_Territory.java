package com.mo.economy_system.screen.territory_system;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.territory_system.Packet_TeleportToTerritory;
import com.mo.economy_system.network.packets.territory_system.Packet_TerritoryDataRequest;
import com.mo.economy_system.screen.EconomySystem_Screen;
import com.mo.economy_system.screen.Screen_Home;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.screen.components.AnimatedButton;
import com.mo.economy_system.screen.components.AnimatedHighLevelTextField;
import com.mo.economy_system.screen.components.ItemIconAnimation;
import com.mo.economy_system.screen.components.TextAnimation;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.stream.Collectors;

public class Screen_Territory extends EconomySystem_Screen {

    private List<Territory> allTerritories = new ArrayList<>(); // 拥有的领地
    private List<Territory> territorys = new ArrayList<>(); // 商品列表
    private List<Territory> ownedTerritories = new ArrayList<>(); // 拥有的领地
    private List<Territory> authorizedTerritories = new ArrayList<>(); // 有权限的领地

    private TextAnimation pageAnimation;
    private TextAnimation noTerritory;

    private AnimatedHighLevelTextField searchBox; // 搜索框

    public Screen_Territory() {
        super(Component.translatable(Util_MessageKeys.TERRITORY_TITLE_KEY));
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_TerritoryDataRequest());
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
                    Component.translatable("search.territory")
            );
            this.addRenderableWidget(searchBox);

            // 设置搜索框的键盘监听器
            this.searchBox.setFocused(false); // 默认不聚焦
            this.searchBox.setMaxLength(50); // 限制输入长度
            this.searchBox.setHint(Component.translatable(Util_MessageKeys.TERRITORY_HINT_TEXT_KEY)); // 提示文本
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
                    Component.translatable("search.territory")
            );
            this.addRenderableWidget(searchBox);

            // 设置搜索框的键盘监听器
            this.searchBox.setFocused(false); // 默认不聚焦
            this.searchBox.setMaxLength(50); // 限制输入长度
            this.searchBox.setHint(Component.translatable(Util_MessageKeys.TERRITORY_HINT_TEXT_KEY)); // 提示文本
            this.searchBox.setResponder(text -> applySearch());
            this.searchBox.startMoveAnimation(Math.max((this.width / 2) - 300, 60), 20);

            // 添加分页按钮
            addPageButtons();

        }

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
        if (!allTerritories.isEmpty()) {
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

        if (ownedTerritories.isEmpty() && authorizedTerritories.isEmpty()) {

            // 动态计算文字居中的位置
            int textWidth = this.font.width(Component.translatable(Util_MessageKeys.TERRITORY_NO_TERRITORIES_TEXT_KEY));
            int xPosition = (this.width - textWidth) / 2;

            noTerritory = new TextAnimation(
                    xPosition,
                    this.height / 2 - 10,
                    xPosition,
                    this.height / 2 - 10,
                    0f,
                    1f,
                    2000
            );
            // 没有领地时，显示提示信息
            renderCache.add((guiGraphics) -> {

                renderAnimatedText(
                        guiGraphics,
                        Component.translatable(Util_MessageKeys.SHOP_LOADING_SHOP_DATA_TEXT_KEY),
                        noTerritory
                );

            });
            return;
        }

        initPosition();

        int y = startY;

        // 渲染领地列表
        for (int i = startIndex; i < endIndex; i++) {
            Territory territory = allTerritories.get(i);

            final int currentY = y; // 使用最终变量供 Lambda 表达式使用

            ItemIconAnimation icon;
            TextAnimation name;
            TextAnimation area;

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

            area = new TextAnimation(
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
                if (ownedTerritories.contains(territory)) {
                    if (territory.getDimension().equals(Level.OVERWORLD)) {
                        renderAnimatedItem(
                                guiGraphics,
                                Items.GRASS_BLOCK.getDefaultInstance(),
                                icon
                        );
                    } else if (territory.getDimension().equals(Level.NETHER)) {
                        renderAnimatedItem(
                                guiGraphics,
                                Items.NETHERRACK.getDefaultInstance(),
                                icon
                        );
                    } else if (territory.getDimension().equals(Level.END)) {
                        renderAnimatedItem(
                                guiGraphics,
                                Items.END_STONE.getDefaultInstance(),
                                icon
                        );
                    } else {
                        renderAnimatedItem(
                                guiGraphics,
                                Items.BEDROCK.getDefaultInstance(),
                                icon
                        );
                    }

                } else {
                    renderAnimatedItem(
                            guiGraphics,
                            Items.OAK_DOOR.getDefaultInstance(),
                            icon
                    );
                }

                renderAnimatedText(
                        guiGraphics,
                        Component.translatable(Util_MessageKeys.TERRITORY_TERRITORY_NAME_TEXT_KEY, territory.getName()),
                        name,
                        0xFFFFFF
                );
                renderAnimatedText(
                        guiGraphics,
                        Component.translatable(Util_MessageKeys.TERRITORY_TERRITORY_AREA_TEXT_KEY,
                                "范围: " +
                                        territory.getPos1().getX() + " " + territory.getPos1().getY() + " " + territory.getPos1().getZ()
                                        + " -> " +
                                        territory.getPos2().getX() + " " + territory.getPos2().getY() + " " + territory.getPos2().getZ()),
                        area,
                        0xAAAAAA
                );
            });

            // 如果是拥有的领地，显示“传送”和"管理"按钮
            if (ownedTerritories.contains(territory)) {
                this.addRenderableWidget(
                        new AnimatedButton(
                                this.width + 60,
                                currentY,
                                this.width - startX - 60 - 80,
                                currentY,
                                60, 20,
                                Component.translatable(Util_MessageKeys.TERRITORY_TELEPORT_BUTTON_KEY),
                                1000,
                                button -> {
                                    EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_TeleportToTerritory(territory.getTerritoryID()));
                                })
                );
                this.addRenderableWidget(
                        new AnimatedButton(
                                this.width + 60,
                                currentY,
                                this.width - startX - 60,
                                currentY,
                                60, 20,
                                Component.translatable(Util_MessageKeys.TERRITORY_MANAGE_BUTTON_KEY),
                                1000,
                                button -> {
                                    Minecraft.getInstance().setScreen(new Screen_ManageTerritory(territory));
                                })
                );
            } else {
                // 如果是有权限的领地，显示“传送”按钮
                this.addRenderableWidget(
                        new AnimatedButton(
                                this.width + 60,
                                currentY,
                                this.width - startX - 60,
                                currentY,
                                60, 20,
                                Component.translatable(Util_MessageKeys.TERRITORY_TELEPORT_BUTTON_KEY),
                                1000,
                                button -> {
                                    EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_TeleportToTerritory(territory.getTerritoryID()));
                                })
                );
            }

            y += THING_SPACING;
        }
    }

    @Override
    protected void detectMouseHoverAndRenderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int y = startY;

        for (int i = startIndex; i < endIndex; i++) {
            Territory territory = allTerritories.get(i);

            if (isMouseOver(mouseX, mouseY, startX, y, 16, 16)) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.translatable(Util_MessageKeys.TERRITORY_TERRITORY_NAME_KEY, territory.getName()));
                tooltip.add(Component.translatable(Util_MessageKeys.TERRITORY_TERRITORY_UUID_KEY, territory.getTerritoryID()));
                tooltip.add(Component.translatable(Util_MessageKeys.TERRITORY_TERRITORY_OWNER_NAME_KEY, territory.getOwnerName()));
                tooltip.add(Component.translatable(Util_MessageKeys.TERRITORY_TERRITORY_OWNER_UUID_KEY, territory.getOwnerUUID()));
                if (territory.getBackpoint() != null) {
                    tooltip.add(Component.translatable(Util_MessageKeys.TERRITORY_TERRITORY_BACK_POINT_KEY, territory.getBackpoint().getX(), territory.getBackpoint().getY(), territory.getBackpoint().getZ()));
                } else {
                    tooltip.add(Component.translatable(Util_MessageKeys.TERRITORY_TERRITORY_BACK_POINT_KEY, "null", "null", "null"));
                }

                guiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
            }

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

    @Override
    protected void addPageButtons() {
        int buttonY = this.height - 40;

        // 上一页按钮
        this.addRenderableWidget(
                Button.builder(Component.literal("<"), button -> {
                            if (currentPage > 0) {
                                currentPage--;
                                this.initPart(); // 刷新页面
                            }
                        }).pos(startX, buttonY)
                        .size(PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT)
                        .build()
        );

        // 下一页按钮
        this.addRenderableWidget(
                Button.builder(Component.literal(">"), button -> {
                            if ((currentPage + 1) * thingsPerPage < ownedTerritories.size() + authorizedTerritories.size()) {
                                currentPage++;
                                this.initPart(); // 刷新页面
                            }
                        }).pos(this.width - startX - PAGE_BUTTON_WIDTH, buttonY)
                        .size(PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT)
                        .build()
        );
    }

    public void updateTerritoryData(List<Territory> owned, List<Territory> authorized) {
        this.ownedTerritories.clear(); // 清空旧的拥有领地
        this.authorizedTerritories.clear(); // 清空旧的有权限领地
        this.ownedTerritories.addAll(owned); // 更新拥有的领地
        this.authorizedTerritories.addAll(authorized); // 更新有权限的领地

        allTerritories = new ArrayList<>();
        territorys = new ArrayList<>();
        allTerritories.addAll(ownedTerritories); // 首先添加拥有的领地
        allTerritories.addAll(authorizedTerritories); // 再添加有权限但不重复的领地
        territorys.addAll(allTerritories);
        
        initPosition();
        this.init(); // 刷新界面
    }


    // 动态计算总页数
    private int getTotalPages() {
        return (int) Math.ceil((double) this.allTerritories.size() / thingsPerPage);
    }

    private void applySearch() {
        applyFilters(); // 调用联合过滤逻辑
    }

    private void applyFilters() {
        new Thread(() -> {
            List<Territory> result = territorys;

            // 2. 应用搜索条件
            if (searchBox != null && !searchBox.getValue().isEmpty()) {
                result = result.stream()
                        .filter(item -> territoryMatchesSearch(item, searchBox.getValue()))
                        .collect(Collectors.toList());
            }

            // 3. 更新UI
            List<Territory> finalResult = result;
            this.minecraft.execute(() -> {
                this.allTerritories = finalResult;
                this.currentPage = 0;
                refreshItemButtons();
                initializeRenderCache(); // 重新初始化渲染缓存
            });
        }).start();
    }

    private boolean territoryMatchesSearch(Territory territory, String searchText) {
        return territory.getName().toLowerCase().contains(searchText.toLowerCase()) ||
                territory.getOwnerName().toLowerCase().contains(searchText.toLowerCase());
    }

    private void refreshItemButtons() {
        clearItemButtons(); // 清除旧的商品按钮
        addTerritoryButtons();   // 添加新的商品按钮
    }

    // 移除购买按钮
    private void clearItemButtons() {
        // 遍历所有已渲染的控件并移除与商品相关的按钮
        this.renderables.removeIf(widget -> widget instanceof Button && isItemButton((Button) widget));
        this.children().removeIf(widget -> widget instanceof Button && isItemButton((Button) widget));
    }

    private void addTerritoryButtons() {
        initPosition();

        int y = startY;

        for (int i = startIndex; i < endIndex; i++) {
            System.out.println(i);
            Territory territory = allTerritories.get(i);

            // 添加购买或下架按钮
            this.addActionButton(territory, y);

            y += THING_SPACING;
        }
    }

    private void addActionButton(Territory territory, int y) {

        final int currentY = y; // 使用最终变量供 Lambda 表达式使用

        if (ownedTerritories.contains(territory)) {
            this.addRenderableWidget(
                    new AnimatedButton(
                            this.width + 60,
                            currentY,
                            this.width - startX - 60 - 80,
                            currentY,
                            60, 20,
                            Component.translatable(Util_MessageKeys.TERRITORY_TELEPORT_BUTTON_KEY),
                            1000,
                            button -> {
                                EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_TeleportToTerritory(territory.getTerritoryID()));
                            })
            );
            this.addRenderableWidget(
                    new AnimatedButton(
                            this.width + 60,
                            currentY,
                            this.width - startX - 60,
                            currentY,
                            60, 20,
                            Component.translatable(Util_MessageKeys.TERRITORY_MANAGE_BUTTON_KEY),
                            1000,
                            button -> {
                                Minecraft.getInstance().setScreen(new Screen_ManageTerritory(territory));
                            })
            );
        } else {
            // 如果是有权限的领地，显示“传送”按钮
            this.addRenderableWidget(
                    new AnimatedButton(
                            this.width + 60,
                            currentY,
                            this.width - startX - 60,
                            currentY,
                            60, 20,
                            Component.translatable(Util_MessageKeys.TERRITORY_TELEPORT_BUTTON_KEY),
                            1000,
                            button -> {
                                EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_TeleportToTerritory(territory.getTerritoryID()));
                            })
            );
        }
    }

    private boolean isItemButton(Button button) {
        Component buttonMessage = button.getMessage();
        return buttonMessage.equals(Component.translatable(Util_MessageKeys.TERRITORY_TELEPORT_BUTTON_KEY)) ||
                buttonMessage.equals(Component.translatable(Util_MessageKeys.TERRITORY_MANAGE_BUTTON_KEY));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && this.shouldCloseOnEsc()) {
            Minecraft.getInstance().setScreen(new Screen_Home());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void initPosition(){
        TOP_MARGIN = this.height - 100;
        thingsPerPage = Math.max(1, TOP_MARGIN / THING_SPACING);

        startIndex = currentPage * thingsPerPage;
        endIndex = Math.min(startIndex + thingsPerPage, allTerritories.size());

        startX = Math.max((this.width / 2) - 300, 60);
        startY = Math.max((this.height - 450) / 4, 55);
    }
}

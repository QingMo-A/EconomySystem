package com.mo.economy_system.screen.territory_system;

import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryBuff;
import com.mo.economy_system.core.territory_system.TerritoryBuffConfig;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.territory_system.Packet_SingleTerritoryDataRequest;
import com.mo.economy_system.network.packets.territory_system.Packet_UnlockTerritoryBuff;
import com.mo.economy_system.network.packets.territory_system.Packet_UpgradeTerritoryBuff;
import com.mo.economy_system.screen.EconomySystem_Screen;
import com.mo.economy_system.screen.components.AnimatedButton;
import com.mo.economy_system.screen.components.AnimatedHighLevelTextField;
import com.mo.economy_system.screen.components.ItemIconAnimation;
import com.mo.economy_system.screen.components.TextAnimation;
import com.mo.economy_system.utils.Util_Message;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Screen_TerritoryBuff extends EconomySystem_Screen {
    private Territory territory;
    private List<TerritoryBuff> buffs;
    private List<TerritoryBuff> buffsSnapshot;


    private TextAnimation pageAnimation;
    private TextAnimation noBuff;

    private AnimatedHighLevelTextField searchBox; // 搜索框

    private UUID playerUUID;
    private String playerName;
    private LocalPlayer player;

    protected Screen_TerritoryBuff(Territory territory) {
        super(Component.translatable(Util_MessageKeys.TERRITORY_BUFF_TITLE_KEY));

        this.territory = territory;
        this.buffs = territory.getTerritoryBuffs();
        this.buffsSnapshot = territory.getTerritoryBuffs();

        for (TerritoryBuff buff : buffs) {
            Util_Message.sendDebugMessage(buff.getDisplayText());
        }
    }

    public void updateTerritory(Territory territory) {
        this.territory = territory;

        this.buffs = territory.getTerritoryBuffs();
        this.buffsSnapshot = territory.getTerritoryBuffs();

        init();
    }

    @Override
    protected void init() {
        super.init();

        this.currentPageNumber = 0;

        initPart();

        if (this.minecraft != null && this.minecraft.player != null) {
            this.playerUUID = this.minecraft.player.getUUID();
            this.playerName = this.minecraft.player.getName().getString();
            this.player = this.minecraft.player;
        }
    }

    @Override
    protected void initPart() {

        initPosition();

        clearWidgets();

        if (flag == 1) {

            // 添加动态翻页按钮
            addPageAnimatedButtons();

        } else if (flag >= 2) {

            // 添加静态翻页按钮
            addPageButtons();

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

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);

        // 执行渲染缓存中的任务
        for (RunnableWithGraphics task : renderCache) {
            task.run(guiGraphics);
        }

        // 进行鼠标悬停检测并显示 Tooltip
        if (!buffs.isEmpty()) {
            detectMouseHoverAndRenderTooltip(guiGraphics, mouseX, mouseY);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void initializeRenderCache() {
        renderCache.clear();

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

        if (buffs.isEmpty()) {

            int textWidth = this.font.width(Component.translatable(Util_MessageKeys.TERRITORY_BUFF_TEXT_NO_BUFFS_TEXT_KEY));
            int xPosition = (this.width - textWidth) / 2;

            noBuff = new TextAnimation(
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
                        Component.translatable(Util_MessageKeys.TERRITORY_BUFF_TEXT_NO_BUFFS_TEXT_KEY),
                        noBuff
                );
            });
            return;
        }

        int y = startY;

        for (int i = startIndex; i < endIndex; i++) {
            TerritoryBuff buff = buffs.get(i);

            final int currentY = y;

            ItemIconAnimation icon;
            TextAnimation name;
            TextAnimation desc;

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

            desc = new TextAnimation(
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
                        Items.GLASS_BOTTLE.getDefaultInstance(),
                        icon
                );
                renderAnimatedText(
                        guiGraphics,
                        Component.literal(buff.getDisplayText()),
                        name,
                        0xFFFFFF
                );
                renderAnimatedText(
                        guiGraphics,
                        Component.translatable(buff.isUnlocked() ? Util_MessageKeys.TERRITORY_BUFF_TEXT_BE_UNLOCKED_TEXT_KEY : Util_MessageKeys.TERRITORY_BUFF_TEXT_BE_LOCKED_TEXT_KEY),
                        desc,
                        0xAAAAAA
                );
            });

            addActionButton(buff, this.width - startX, currentY, playerUUID);

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

            TerritoryBuff buff = buffs.get(i);

            if (isMouseOver(mouseX, mouseY, startX, y, 16, 16)) {
                List<Component> tooltipLines = new ArrayList<>();

                tooltipLines.add(Component.translatable(Util_MessageKeys.TERRITORY_BUFF_TOOLTIP_BUFF_ID_TEXT_KEY, buff.getId()));
                tooltipLines.add(Component.translatable(Util_MessageKeys.TERRITORY_BUFF_TOOLTIP_BUFF_NAME_TEXT_KEY, buff.getDisplayText()));
                tooltipLines.add(Component.translatable(Util_MessageKeys.TERRITORY_BUFF_TOOLTIP_BUFF_CURRENT_LEVEL_TEXT_KEY, buff.getLevel()));
                tooltipLines.add(Component.translatable(Util_MessageKeys.TERRITORY_BUFF_TOOLTIP_BUFF_MAX_LEVEL_TEXT_KEY, buff.getMaxLevel()));
                tooltipLines.add(Component.translatable(Util_MessageKeys.TERRITORY_BUFF_TOOLTIP_BUFF_EFFECT_ID_TEXT_KEY, buff.getEffectId()));
                tooltipLines.add(Component.translatable(Util_MessageKeys.TERRITORY_BUFF_TOOLTIP_BUFF_UNLOCK_STATE_KEY, buff.isUnlocked()));

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
        initPosition();
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

    private void addActionButton(TerritoryBuff buff, int buttonX, int buttonY, UUID playerUUID) {
        // 如果没解锁
        if (!(buff.isUnlocked())) {
            addButton(Util_MessageKeys.TERRITORY_BUFF_BUTTON_UNLOCK_KEY, buttonX - 60, buttonY, 60, 20,
                    () -> {
                        if (canAffordUpgrade(buff, player)) {
                            EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_UnlockTerritoryBuff(territory.getTerritoryID(), buff.getId()));
                            EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_SingleTerritoryDataRequest(territory.getTerritoryID()));
                        }
                    });
        } else {
            // 如果没满级
            if (buff.getLevel() < buff.getMaxLevel()) {
                addButton(Util_MessageKeys.TERRITORY_BUFF_BUTTON_UPGRADE_KEY, buttonX - 60, buttonY, 60, 20,
                        () -> {
                            if (canAffordUpgrade(buff, player)) {
                                EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_UpgradeTerritoryBuff(territory.getTerritoryID(), buff.getId()));
                                EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_SingleTerritoryDataRequest(territory.getTerritoryID()));
                            }
                        });
            } else if (buff.getLevel() >= buff.getMaxLevel()) { // 如果满级了
                addButton(Util_MessageKeys.TERRITORY_BUFF_BUTTON_MAX_KEY, buttonX - 60, buttonY, 60, 20,
                        () -> {
                            this.player.sendSystemMessage(Component.translatable(Util_MessageKeys.TERRITORY_BUFF_MESSAGE_BUFF_MAX_LEVEL_KEY));
                        });
            }
        }
    }

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

    // 动态计算总页数
    private int getTotalPages() {
        return (int) Math.ceil((double) buffs.size() / thingsPerPage);
    }

    @Override
    protected void initPosition(){
        TOP_MARGIN = this.height - 100;
        thingsPerPage = Math.max(1, TOP_MARGIN / THING_SPACING);

        startIndex = currentPageNumber * thingsPerPage;
        endIndex = Math.min(startIndex + thingsPerPage, buffs.size());

        startX = Math.max((this.width / 2) - 300, 60);
        startY = Math.max((this.height - 450) / 4, 55);
    }

    private boolean canAffordUpgrade(TerritoryBuff buff, LocalPlayer player) {
        for (TerritoryBuffConfig.BuffUpgradeCost cost : buff.getUpgradeCost()) {
            // **遍历多种物品**
            for (TerritoryBuffConfig.BuffUpgradeCost.ItemRequirement itemCost : cost.items) {
                if (!itemCost.item.isEmpty() && !playerHasItem(player, itemCost.item, itemCost.count)) {
                    player.sendSystemMessage(Component.translatable(Util_MessageKeys.TERRITORY_BUFF_MESSAGE_REQUIREMENT_ITEM_FAIL_KEY));
                    return false; // 物品不足
                }
            }

            // **检查经验**
            if (cost.xp > 0 && player.experienceLevel < cost.xp) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.TERRITORY_BUFF_MESSAGE_REQUIREMENT_XP_LEVEL_FAIL_KEY));
                return false; // 经验值不足
            }
        }
        return true; // 资源充足，可以解锁或升级
    }

    private boolean playerHasItem(LocalPlayer player, String itemID, int requiredCount) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (ForgeRegistries.ITEMS.getKey(stack.getItem()).toString().equals(itemID)) {
                count += stack.getCount();
                if (count >= requiredCount) {
                    return true; // 物品数量足够
                }
            }
        }
        return false; // 物品不足
    }
}

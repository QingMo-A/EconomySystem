package com.mo.economy_system.screen.territory_system;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.territory_system.Packet_ModifyMode;
import com.mo.economy_system.network.packets.territory_system.Packet_RemoveTerritory;
import com.mo.economy_system.network.packets.territory_system.Packet_RemovePlayer;
import com.mo.economy_system.network.packets.territory_system.Packet_TerritoryDataRequest;
import com.mo.economy_system.core.territory_system.PlayerInfo;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.screen.EconomySystem_Screen;
import com.mo.economy_system.screen.components.AnimatedButton;
import com.mo.economy_system.screen.components.ItemIconAnimation;
import com.mo.economy_system.screen.components.TextAnimation;
import com.mo.economy_system.utils.Util_MessageKeys;
import com.mo.economy_system.utils.Util_Skull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Screen_ManageTerritory extends EconomySystem_Screen {
    private final Territory territory;

    private UUID playerUUID;
    private List<PlayerInfo> authorizedPlayers;

    private TextAnimation noMember;

    public Screen_ManageTerritory(Territory territory) {
        super(Component.literal("管理领地: " + territory.getName()));
        this.territory = territory;
        // 显示有权限玩家列表
        authorizedPlayers = territory.getAuthorizedPlayers().stream().toList();
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

        } else if (flag >= 2) {

        }

        flag ++;

        if (this.minecraft != null && this.minecraft.player != null) {
            this.playerUUID = this.minecraft.player.getUUID();
        }

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

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void initializeRenderCache() {
        renderCache.clear(); // 清空旧的缓存

        int y = startY;

        addActionButton
                (
                        this.width + 100,
                        startY,
                        this.width - startX - 120,
                        startY,
                        120,
                        20,
                        Util_MessageKeys.TERRITORY_MANAGEMENT_COPY_ID,
                        1000,
                        button -> {
                            GLFW.glfwSetClipboardString(Minecraft.getInstance().getWindow().getWindow(),
                                    territory.getTerritoryID().toString());
                            this.minecraft.player.sendSystemMessage(Component.translatable(Util_MessageKeys.TERRITORY_MANAGEMENT_COPY_SUCCESS));
                        }
                );

        addActionButton
                (
                        this.width + 100,
                        startY + 30,
                        this.width - startX - 120,
                        startY + 30,
                        120,
                        20,
                        Util_MessageKeys.TERRITORY_MANAGEMENT_RESIZE_TERRITORY,
                        1000,
                        button -> {
                            EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_ModifyMode(territory.getTerritoryID()));
                            this.minecraft.setScreen(null);
                        }
                );

        addActionButton
                (
                        this.width + 100,
                        startY + 60,
                        this.width - startX - 120,
                        startY + 60,
                        120,
                        20,
                        Util_MessageKeys.TERRITORY_MANAGEMENT_INVITE_PLAYER,
                        1000,
                        button -> {
                            Minecraft.getInstance().setScreen(new Screen_InvitePlayer(territory));
                        }
                );

        addActionButton
                (
                        this.width + 100,
                        startY + 90,
                        this.width - startX - 120,
                        startY + 90,
                        120,
                        20,
                        Util_MessageKeys.TERRITORY_MANAGEMENT_BUFF,
                        1000,
                        button -> {

                        }
                );

        addActionButton
                (
                        this.width + 100,
                        startY + 120,
                        this.width - startX - 120,
                        startY + 120,
                        120,
                        20,
                        Util_MessageKeys.TERRITORY_MANAGEMENT_PERMISSIONS,
                        1000,
                        button -> {

                        }
                );

        addActionButton
                (
                        this.width + 100,
                        startY + 150,
                        this.width - startX - 120,
                        startY + 150,
                        120,
                        20,
                        Util_MessageKeys.TERRITORY_MANAGEMENT_TRANSFER_OWNERSHIP,
                        1000,
                        button -> {

                        }
                );

        addActionButton
                (
                        this.width + 100,
                        startY + 180,
                        this.width - startX - 120,
                        startY + 180,
                        120,
                        20,
                        Util_MessageKeys.TERRITORY_MANAGEMENT_DELETE_TERRITORY,
                        1000,
                        button -> {
                            EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_RemoveTerritory(territory.getTerritoryID()));
                            this.minecraft.setScreen(null); // 关闭界面
                        }
                );

        if (authorizedPlayers.isEmpty()) {

            int textWidth = this.font.width(Component.translatable(Util_MessageKeys.TERRITORY_TERRITORY_NO_AUTHORIZED_PLAYER_KEY));
            int xPosition = (this.width / 4) - (textWidth / 2);

            noMember = new TextAnimation(
                    xPosition,
                    startY,
                    xPosition,
                    startY,
                    0f,
                    1f,
                    2000
            );

            renderCache.add((guiGraphics) -> {


                renderAnimatedText(
                        guiGraphics,
                        Component.translatable(Util_MessageKeys.TERRITORY_TERRITORY_NO_AUTHORIZED_PLAYER_KEY),
                        noMember
                );
            });
            return;
        }

        for (int i = startIndex; i < endIndex; i++) {
            PlayerInfo playerInfo = authorizedPlayers.get(i);

            final int currentY = y; // 使用最终变量供 Lambda 表达式使用

            ItemIconAnimation icon;
            TextAnimation name;
            TextAnimation uuid;

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

            uuid = new TextAnimation(
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
                        Util_Skull.createPlayerHead(playerUUID, playerInfo.getName()),
                        icon
                );
                renderAnimatedText(
                        guiGraphics,
                        Component.literal(playerInfo.getName()),
                        name,
                        0xFFFFFF
                );
                renderAnimatedText(
                        guiGraphics,
                        Component.literal(playerInfo.getUuid().toString()),
                        uuid,
                        0xAAAAAA
                );
            });

            addKickButton(playerInfo.getUuid(), (this.width / 2) - startX, currentY);

            y += THING_SPACING;
        }
    }

    private void addKickButton(UUID playerUUID, int buttonX, int buttonY) {
        this.addRenderableWidget(
                new AnimatedButton(
                        -60,
                        buttonY,
                        buttonX,
                        buttonY,
                        60,
                        20,
                        Component.translatable(Util_MessageKeys.TERRITORY_MANAGEMENT_KICK_PLAYER),
                        1000,
                        button -> {
                            EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_RemovePlayer(territory.getTerritoryID(), playerUUID));
                            this.minecraft.setScreen(new Screen_Territory());
                        }
                )
        );
    }

    private void addActionButton(int startX, int startY, int targetX, int targetY, int width, int height, String key, int duration, Button.OnPress action) {
        this.addRenderableWidget(
                new AnimatedButton(
                        startX,
                        startY,
                        targetX,
                        targetY,
                        width,
                        height,
                        Component.translatable(key),
                        duration,
                        action
                )
        );
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && this.shouldCloseOnEsc()) {
            Minecraft.getInstance().setScreen(new Screen_Territory());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void initPosition() {
        TOP_MARGIN = this.height - 100;
        thingsPerPage = Math.max(1, TOP_MARGIN / THING_SPACING);

        startIndex = currentPage * thingsPerPage;
        endIndex = Math.min(startIndex + thingsPerPage, authorizedPlayers.size());

        startX = Math.max((this.width / 2) - 450, 60);
        startY = Math.max((this.height - 450) / 4, 55);
    }
}

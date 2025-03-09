package com.mo.economy_system.screen.territory_system;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.Packet_ServerPlayerListRequest;
import com.mo.economy_system.network.packets.economy_system.Packet_BalanceRequest;
import com.mo.economy_system.network.packets.territory_system.Packet_InvitePlayer;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.screen.EconomySystem_Screen;
import com.mo.economy_system.screen.components.AnimatedButton;
import com.mo.economy_system.screen.components.AnimatedHighLevelTextField;
import com.mo.economy_system.screen.components.HighLevelTextField;
import com.mo.economy_system.utils.Util_MessageKeys;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

public class Screen_InvitePlayer extends EconomySystem_Screen {
    private List<Map.Entry<UUID, String>> accounts; // 新增字段
    private List<UUID> UUID = new ArrayList<>();
    private List<String> name = new ArrayList<>();
    private final Territory territory;
    private AnimatedHighLevelTextField playerNameField;

    public Screen_InvitePlayer(Territory territory) {
        super(Component.translatable(Util_MessageKeys.INVITE_TITLE_KEY));
        this.territory = territory;
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_ServerPlayerListRequest());
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 玩家名称输入框
        this.playerNameField = new AnimatedHighLevelTextField(
                this.font,
                centerX - 100,
                this.height + 20,
                200,
                20,
                1000,
                Component.literal("输入玩家名称"));
        this.playerNameField.setHint(Component.literal("输入玩家名称")); // 提示文本
        playerNameField.setSuggestions(this.name);
        this.playerNameField.startMoveAnimation(centerX - 100, centerY - 60);
        this.addRenderableWidget(playerNameField);

        // 发送邀请按钮
        this.addRenderableWidget(
                new AnimatedButton(
                        centerX - 50,
                        this.height + 20,
                        centerX - 50,
                        centerY + 60,
                        100,
                        20,
                        Component.translatable(Util_MessageKeys.INVITE_INVITE_BUTTON_KEY),
                        1000,
                        button -> {
                            String playerName = playerNameField.getValue();
                            if (!playerName.isEmpty()) {
                                EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_InvitePlayer(territory.getTerritoryID(), playerName));
                                this.minecraft.setScreen(null); // 关闭当前界面
                            } else {
                                this.minecraft.player.sendSystemMessage(Component.translatable(Util_MessageKeys.INVITE_NO_NAME_KEY));
                            }
                        }
                )
        );

        // 返回按钮
        this.addRenderableWidget(
                new AnimatedButton(
                        centerX - 50,
                        this.height + 20,
                        centerX - 50,
                        centerY + 90,
                        100,
                        20,
                        Component.translatable(Util_MessageKeys.INVITE_BACK_BUTTON),
                        1000,
                        button -> {
                            this.minecraft.setScreen(new Screen_ManageTerritory(territory));
                        }
                )
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        this.playerNameField.render(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    public void update(List<Map.Entry<UUID, String>> accounts) {
        this.accounts = accounts;

        // 遍历 list
        for (Map.Entry<UUID, String> entry : this.accounts) {
            UUID uuid = entry.getKey();  // 获取玩家 UUID
            String name = entry.getValue(); // 获取玩家名称

            this.name.add(name);
            this.UUID.add(uuid);
        }

        init();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && this.shouldCloseOnEsc()) {
            Minecraft.getInstance().setScreen(new Screen_ManageTerritory(territory));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}

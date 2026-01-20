package com.mo.economy_system.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.death.DeathScreenDataStorage;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.playerattribute_system.death_system.Packet_KeepInventoryRequest;
import com.mo.economy_system.network.packets.playerattribute_system.death_system.Packet_NormalRespawnRequest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 死亡界面 Mixin
 * 为原版死亡界面添加自定义复活选项
 */
@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin extends Screen {

    @Unique
    private static final int BG_COLOR = 0xB0202020;
    @Unique
    private static final int LINE_COLOR = 0xFFDD00;
    @Unique
    private static final int PADDING = 8;

    @Unique
    private static int CACHED_DYNAMIC_BORDER_COLOR = 0xFFDDAA55;
    @Unique
    private static long LAST_BORDER_COLOR_UPDATE = 0;
    @Unique
    private static final long BORDER_COLOR_UPDATE_INTERVAL = 100;

    @Unique
    private Button economySystem$normalRespawnButton;
    @Unique
    private Button economySystem$keepInventoryButton;
    @Unique
    private Button economySystem$titleScreenButton;

    // Shadow the constructor parameter from DeathScreen
    @Shadow
    private Component causeOfDeath;

    protected DeathScreenMixin(Component title) {
        super(title);
    }

    /**
     * 注入到 init() 方法，添加自定义按钮
     * 始终使用自定义逻辑，不再检查数据是否为空
     */
    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void economySystem$init(CallbackInfo ci) {
        // 获取数据（如果没有数据，返回默认值）
        DeathScreenDataStorage.DeathScreenData data = DeathScreenDataStorage.getData();

        // 始终使用自定义界面
        ci.cancel();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int boxWidth = 400;
        int boxHeight = 200;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        int buttonWidth = 340;
        int buttonHeight = 22;
        int buttonY = boxY + 100;
        int buttonSpacing = 6;

        String respawnType = data.isInfected() ? "感染者" : "幸存者";

        // 正常复活按钮
        economySystem$normalRespawnButton = new CustomButton(
                centerX - buttonWidth / 2, buttonY,
                buttonWidth, buttonHeight,
                Component.literal("§e作为" + respawnType + "重生 §7(-" + String.format("%.1f", data.normalCost()) + "§7)"),
                btn -> economySystem$sendNormalRespawn()
        );
        this.addRenderableWidget(economySystem$normalRespawnButton);

        // 保留物品复活按钮
        economySystem$keepInventoryButton = new CustomButton(
                centerX - buttonWidth / 2, buttonY + buttonHeight + buttonSpacing,
                buttonWidth, buttonHeight,
                Component.literal("§6作为" + respawnType + "保留物品栏重生 §7(-" + String.format("%.1f", data.keepInventoryCost()) + "§7)"),
                btn -> economySystem$sendKeepInventory()
        );
        this.addRenderableWidget(economySystem$keepInventoryButton);

        // 返回标题界面按钮
        economySystem$titleScreenButton = new CustomButton(
                centerX - buttonWidth / 2, buttonY + (buttonHeight + buttonSpacing) * 2,
                buttonWidth, buttonHeight,
                Component.literal("§c返回标题界面"),
                btn -> economySystem$returnToTitleScreen()
        );
        this.addRenderableWidget(economySystem$titleScreenButton);

        // 检查点数是否足够，禁用相应按钮
        boolean canRespawn = data.respawnPoint() >= data.normalCost();
        boolean canKeepInventory = data.respawnPoint() >= data.keepInventoryCost();

        economySystem$normalRespawnButton.active = canRespawn;
        economySystem$keepInventoryButton.active = canKeepInventory;

        EconomySystem.LOGGER.info("死亡界面自定义按钮已添加");
    }

    /**
     * 返回标题界面
     */
    @Unique
    private void economySystem$returnToTitleScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            mc.level.disconnect();
        }
        mc.clearLevel(null);
        mc.setScreen(null);
    }

    /**
     * 发送正常复活请求
     */
    @Unique
    private void economySystem$sendNormalRespawn() {
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_NormalRespawnRequest());
    }

    /**
     * 发送保留物品复活请求
     */
    @Unique
    private void economySystem$sendKeepInventory() {
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_KeepInventoryRequest());
    }

    /**
     * 重新初始化按钮（当数据包延迟到达时调用）
     */
    @Unique
    private void reinitButtons() {
        DeathScreenDataStorage.DeathScreenData data = DeathScreenDataStorage.getData();

        // 移除旧按钮
        if (economySystem$normalRespawnButton != null) {
            this.removeWidget(economySystem$normalRespawnButton);
        }
        if (economySystem$keepInventoryButton != null) {
            this.removeWidget(economySystem$keepInventoryButton);
        }
        if (economySystem$titleScreenButton != null) {
            this.removeWidget(economySystem$titleScreenButton);
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int boxWidth = 400;
        int boxHeight = 200;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        int buttonWidth = 340;
        int buttonHeight = 22;
        int buttonY = boxY + 100;
        int buttonSpacing = 6;

        String respawnType = data.isInfected() ? "感染者" : "幸存者";

        // 正常复活按钮
        economySystem$normalRespawnButton = new CustomButton(
                centerX - buttonWidth / 2, buttonY,
                buttonWidth, buttonHeight,
                Component.literal("§e作为" + respawnType + "重生 §7(-" + String.format("%.1f", data.normalCost()) + "§7)"),
                btn -> economySystem$sendNormalRespawn()
        );
        this.addRenderableWidget(economySystem$normalRespawnButton);

        // 保留物品复活按钮
        economySystem$keepInventoryButton = new CustomButton(
                centerX - buttonWidth / 2, buttonY + buttonHeight + buttonSpacing,
                buttonWidth, buttonHeight,
                Component.literal("§6作为" + respawnType + "保留物品栏重生 §7(-" + String.format("%.1f", data.keepInventoryCost()) + "§7)"),
                btn -> economySystem$sendKeepInventory()
        );
        this.addRenderableWidget(economySystem$keepInventoryButton);

        // 返回标题界面按钮
        economySystem$titleScreenButton = new CustomButton(
                centerX - buttonWidth / 2, buttonY + (buttonHeight + buttonSpacing) * 2,
                buttonWidth, buttonHeight,
                Component.literal("§c返回标题界面"),
                btn -> economySystem$returnToTitleScreen()
        );
        this.addRenderableWidget(economySystem$titleScreenButton);

        // 检查点数是否足够，禁用相应按钮
        boolean canRespawn = data.respawnPoint() >= data.normalCost();
        boolean canKeepInventory = data.respawnPoint() >= data.keepInventoryCost();

        economySystem$normalRespawnButton.active = canRespawn;
        economySystem$keepInventoryButton.active = canKeepInventory;

        EconomySystem.LOGGER.info("死亡界面按钮已刷新");
    }

    /**
     * 注入到 render() 方法，使用 GuiGraphics (Forge 1.20.1)
     * 始终使用自定义渲染，不再检查数据是否为空
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = true)
    private void economySystem$renderForge(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        // 检查是否需要重新初始化按钮（数据包延迟到达的情况）
        if (DeathScreenDataStorage.needsReinit()) {
            DeathScreenDataStorage.setNeedsReinit(false);
            reinitButtons();
        }

        // 获取数据（如果没有数据，返回默认值）
        DeathScreenDataStorage.DeathScreenData data = DeathScreenDataStorage.getData();

        // 始终取消原版渲染，使用自定义渲染
        ci.cancel();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int boxWidth = 400;
        int boxHeight = 200;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        int dynamicBorderColor = getDynamicBorderColor();

        // 渐变背景
        guiGraphics.fillGradient(0, 0, this.width, this.height, 1615855616, -1602211792);

        // 主面板背景
        guiGraphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, BG_COLOR);

        // 边框
        guiGraphics.fill(boxX, boxY, boxX + boxWidth, boxY + 1, dynamicBorderColor);
        guiGraphics.fill(boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, dynamicBorderColor);
        guiGraphics.fill(boxX, boxY, boxX + 1, boxY + boxHeight, dynamicBorderColor);
        guiGraphics.fill(boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, dynamicBorderColor);

        // 标题：你死了！（大字号，居中，红色）
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.scale(1.5f, 1.5f, 1.0f);
        String nameText = "§c§l你死了！";
        int nameX = (int) ((centerX) / 1.5f - font.width(nameText) / 2.0f);
        int nameY = (int) ((boxY + 22) / 1.5f);
        guiGraphics.drawString(this.font, nameText, nameX, nameY, 0xFFFFFF, false);
        poseStack.popPose();

        // 右上角：玩家ID（黄色）
        String playerIdText = Minecraft.getInstance().player != null ?
                "§e§l" + Minecraft.getInstance().player.getName().getString() : "§e§lPlayer";
        int playerIdX = boxX + boxWidth - PADDING - font.width(playerIdText);
        int playerIdY = boxY + 22;
        guiGraphics.drawString(this.font, playerIdText, playerIdX, playerIdY, 0xFFFFFF, false);

        // 名字下面的长线
        int lineY = boxY + 40;
        guiGraphics.fill(boxX + PADDING, lineY, boxX + boxWidth - PADDING, lineY + 1, LINE_COLOR);

        // 复活点数显示
        String pointText = "§6§l复活点数: §e§l" + String.format("%.1f", data.respawnPoint());
        poseStack.pushPose();
        poseStack.scale(1.2f, 1.2f, 1.0f);
        int pointX = (int) ((centerX) / 1.2f - font.width(pointText) / 2.0f);
        int pointY = (int) ((boxY + 60) / 1.2f);
        guiGraphics.drawString(this.font, pointText, pointX, pointY, 0xFFFFFF, false);
        poseStack.popPose();

        // 死亡原因显示（红色）
        String deathReason = data.deathMessage().getString();
        // 处理过长的死亡消息，截断到合适的长度
        int maxDeathReasonWidth = boxWidth - 10;
        if (font.width(deathReason) > maxDeathReasonWidth) {
            deathReason = font.plainSubstrByWidth(deathReason, maxDeathReasonWidth - font.width("...")) + "...";
        }
        guiGraphics.drawCenteredString(this.font, deathReason, centerX, boxY + 85, 0xFF5555);

        // 警告信息
        if (data.respawnPoint() < data.normalCost()) {
            String warningText = "§c§l复活点数不足！将被封禁！";
            guiGraphics.drawCenteredString(this.font, warningText, centerX, boxY + 170, 0xFFFFFFFF);
        } else {
            String tipText = "§7请选择复活方式";
            guiGraphics.drawCenteredString(this.font, tipText, centerX, boxY + 170, 0xFFAAAAAA);
        }

        // 底部：DreamingFish.net
        String domainText = "§b§lDreaming§d§lFish§6§l.net";
        int domainX = centerX - font.width(domainText) / 2;
        int domainY = boxY + boxHeight - 18;
        guiGraphics.drawString(this.font, domainText, domainX, domainY, 0xFFFFFF, false);

        // 渲染按钮（因为取消了原版 render，需要手动渲染）
        if (economySystem$normalRespawnButton != null) {
            economySystem$normalRespawnButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        if (economySystem$keepInventoryButton != null) {
            economySystem$keepInventoryButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        if (economySystem$titleScreenButton != null) {
            economySystem$titleScreenButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Unique
    private static int getDynamicBorderColor() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - LAST_BORDER_COLOR_UPDATE > BORDER_COLOR_UPDATE_INTERVAL) {
            int red = (int) (Math.sin(currentTime * 0.001) * 100 + 155);
            int green = (int) (Math.sin(currentTime * 0.001 + 2) * 100 + 155);
            int blue = (int) (Math.sin(currentTime * 0.001 + 4) * 100 + 155);
            CACHED_DYNAMIC_BORDER_COLOR = 0xFF000000 | (red << 16) | (green << 8) | blue;
            LAST_BORDER_COLOR_UPDATE = currentTime;
        }
        return CACHED_DYNAMIC_BORDER_COLOR;
    }

    /**
     * 自定义黑色风格按钮
     */
    @Unique
    private static class CustomButton extends Button {
        private static final int BUTTON_BG = 0xB0000000;
        private static final int BUTTON_BG_HOVER = 0xC0303030;
        private static final int BUTTON_BORDER = 0xFF444444;
        private static final int BUTTON_BORDER_HOVER = 0xFFDDAA55;

        public CustomButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHovered() && this.active;
            boolean active = this.active;

            int bgColor = !active ? 0x80303030 : (hovered ? BUTTON_BG_HOVER : BUTTON_BG);
            int borderColor = !active ? 0x80555555 : (hovered ? BUTTON_BORDER_HOVER : BUTTON_BORDER);

            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + 1, borderColor);
            guiGraphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, borderColor);
            guiGraphics.fill(getX(), getY(), getX() + 1, getY() + height, borderColor);
            guiGraphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, borderColor);

            int textColor = active ? 0xFFFFFF : 0x888888;
            String displayText = getMessage().getString();
            int textX = getX() + width / 2 - Minecraft.getInstance().font.width(displayText) / 2;
            int textY = getY() + (height - 8) / 2;
            guiGraphics.drawString(Minecraft.getInstance().font, displayText, textX, textY, textColor, false);
        }
    }
}

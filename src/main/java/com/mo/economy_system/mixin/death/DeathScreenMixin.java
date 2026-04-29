package com.mo.economy_system.mixin.death;

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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 死亡界面 Mixin
 * 压迫感风格的死亡界面
 */
@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin extends Screen {

    // 颜色定义 - 暗红色调，营造压迫感
    @Unique
    private static final int BG_OUTER = 0xDD0A0000;           // 深红色外背景
    @Unique
    private static final int BG_INNER = 0xEE1A0505;           // 暗红色内背景
    @Unique
    private static final int BORDER_DARK = 0xFF3D0000;        // 深红边框
    @Unique
    private static final int BORDER_GLOW = 0xFFFF1A1A;        // 发光边框
    @Unique
    private static final int ACCENT_RED = 0xFFCC0000;         // 强调红
    @Unique
    private static final int TEXT_DARK = 0xFF333333;          // 深色文字
    @Unique
    private static final int TEXT_WARNING = 0xFFFF3333;        // 警告红

    @Unique
    private static final int PADDING = 12;

    @Unique
    private Button economySystem$normalRespawnButton;
    @Unique
    private Button economySystem$keepInventoryButton;
    @Unique
    private Button economySystem$titleScreenButton;

    @Unique
    private boolean economySystem$showDeathPos = false;  // 是否显示死亡位置
    @Unique
    private int economySystem$posButtonX;
    @Unique
    private int economySystem$posButtonY;
    @Unique
    private int economySystem$posButtonWidth = 140;
    @Unique
    private int economySystem$posButtonHeight = 16;

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

        int boxWidth = 420;
        int boxHeight = 235;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        int buttonWidth = 380;
        int buttonHeight = 24;
        int buttonStartY = boxY + 124;
        int buttonSpacing = 6;

        String respawnType = data.isInfected() ? "感染者" : "幸存者";

        // 正常复活按钮
        economySystem$normalRespawnButton = new CustomButton(
                centerX - buttonWidth / 2, buttonStartY,
                buttonWidth, buttonHeight,
                Component.literal("§e作为" + respawnType + "重生 §8(-" + String.format("%.1f", data.normalCost()) + "§8)"),
                false, data.normalCost(), data.respawnPoint(),
                btn -> economySystem$sendNormalRespawn()
        );
        this.addRenderableWidget(economySystem$normalRespawnButton);

        // 保留物品复活按钮
        economySystem$keepInventoryButton = new CustomButton(
                centerX - buttonWidth / 2, buttonStartY + buttonHeight + buttonSpacing,
                buttonWidth, buttonHeight,
                Component.literal("§6作为" + respawnType + "保留物品栏重生 §8(-" + String.format("%.1f", data.keepInventoryCost()) + "§8)"),
                true, data.keepInventoryCost(), data.respawnPoint(),
                btn -> economySystem$sendKeepInventory()
        );
        this.addRenderableWidget(economySystem$keepInventoryButton);

        // 返回标题界面按钮
        economySystem$titleScreenButton = new CustomButton(
                centerX - buttonWidth / 2, buttonStartY + (buttonHeight + buttonSpacing) * 2,
                buttonWidth, buttonHeight,
                Component.literal("§c返回标题界面"),
                false, 0, data.respawnPoint(),
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
        mc.disconnect();
        mc.setScreen(null);
    }

    /**
     * 发送正常复活请求
     */
    @Unique
    private void economySystem$sendNormalRespawn() {
        EconomySystem_NetworkManager.sendToServer(new Packet_NormalRespawnRequest());
    }

    /**
     * 发送保留物品复活请求
     */
    @Unique
    private void economySystem$sendKeepInventory() {
        EconomySystem_NetworkManager.sendToServer(new Packet_KeepInventoryRequest());
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

        int boxWidth = 420;
        int boxHeight = 235;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        int buttonWidth = 380;
        int buttonHeight = 24;
        int buttonStartY = boxY + 124;
        int buttonSpacing = 6;

        String respawnType = data.isInfected() ? "感染者" : "幸存者";

        // 正常复活按钮
        economySystem$normalRespawnButton = new CustomButton(
                centerX - buttonWidth / 2, buttonStartY,
                buttonWidth, buttonHeight,
                Component.literal("§e作为" + respawnType + "重生 §8(-" + String.format("%.1f", data.normalCost()) + "§8)"),
                false, data.normalCost(), data.respawnPoint(),
                btn -> economySystem$sendNormalRespawn()
        );
        this.addRenderableWidget(economySystem$normalRespawnButton);

        // 保留物品复活按钮
        economySystem$keepInventoryButton = new CustomButton(
                centerX - buttonWidth / 2, buttonStartY + buttonHeight + buttonSpacing,
                buttonWidth, buttonHeight,
                Component.literal("§6作为" + respawnType + "保留物品栏重生 §8(-" + String.format("%.1f", data.keepInventoryCost()) + "§8)"),
                true, data.keepInventoryCost(), data.respawnPoint(),
                btn -> economySystem$sendKeepInventory()
        );
        this.addRenderableWidget(economySystem$keepInventoryButton);

        // 返回标题界面按钮
        economySystem$titleScreenButton = new CustomButton(
                centerX - buttonWidth / 2, buttonStartY + (buttonHeight + buttonSpacing) * 2,
                buttonWidth, buttonHeight,
                Component.literal("§c返回标题界面"),
                false, 0, data.respawnPoint(),
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
     * 注入到 render() 方法，使用压迫感风格渲染
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

        int boxWidth = 420;
        int boxHeight = 235;
        int boxX = centerX - boxWidth / 2;
        int boxY = centerY - boxHeight / 2;

        // ========== 背景 ==========
        // 深红色渐变背景
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xDD0A0000, 0xDD150505);

        // ========== 主面板 ==========
        // 外边框（深红色）
        renderRoundedBox(guiGraphics, boxX - 4, boxY - 4, boxX + boxWidth + 4, boxY + boxHeight + 4, BORDER_DARK);
        // 内边框（发光红色）
        renderRoundedBox(guiGraphics, boxX - 2, boxY - 2, boxX + boxWidth + 2, boxY + boxHeight + 2, BORDER_GLOW);
        // 主背景（暗红色）
        guiGraphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, BG_INNER);

        // ========== 标题区域 ==========
        PoseStack poseStack = guiGraphics.pose();

        // 骷髅图标（用文字代替）
        String skullIcon = "☠";
        poseStack.pushPose();
        poseStack.scale(3.0f, 3.0f, 1.0f);
        int skullX = (int) ((boxX + 25) / 3.0f);
        int skullY = (int) ((boxY + 20) / 3.0f);
        guiGraphics.drawString(this.font, skullIcon, skullX, skullY, ACCENT_RED, false);
        poseStack.popPose();

        // 标题：你死了！（超大，血红色，无阴影）
        poseStack.pushPose();
        poseStack.scale(2.2f, 2.2f, 1.0f);
        String titleText = "§c§l布豪，您趋势了！";
        int titleX = (int) ((centerX + 15) / 2.2f - font.width(titleText) / 2.0f);
        int titleY = (int) ((boxY + 28) / 2.2f);
        guiGraphics.drawString(this.font, titleText, titleX, titleY, 0xFFFFFFFF, false);
        poseStack.popPose();

        // 右上角：DreamingFish
        String domainText = "§b§lDreaming§d§lFish";
        int domainX = boxX + boxWidth - PADDING - font.width(domainText);
        int domainY = boxY + 15;
        guiGraphics.drawString(this.font, domainText, domainX, domainY, 0xFFFFFFFF, false);

        // ========== 分隔线（带血红色调）==========
        int lineY = boxY + 55;
        guiGraphics.fill(boxX + PADDING, lineY, boxX + boxWidth - PADDING, lineY + 2, ACCENT_RED);
        // 底部阴影线
        guiGraphics.fill(boxX + PADDING, lineY + 3, boxX + boxWidth - PADDING, lineY + 4, 0xAA660000);

        // ========== 躯体分裂次数显示 ==========
        String splitLabel = "细胞剩余分裂点数";
        String splitValue = String.format("%.0f", data.respawnPoint());

        // 标签
        guiGraphics.drawString(this.font, "§7" + splitLabel, boxX + 30, boxY + 70, 0xFF888888, false);

        // 数值（大字号，根据数量变化颜色）
        int splitColor = data.respawnPoint() < data.normalCost() ? TEXT_WARNING : 0xFFD4AF37;
        poseStack.pushPose();
        poseStack.scale(1.8f, 1.8f, 1.0f);
        int splitValueX = (int) ((boxX + 30) / 1.8f);
        int splitValueY = (int) ((boxY + 85) / 1.8f);
        guiGraphics.drawString(this.font, "§l" + splitValue, splitValueX, splitValueY, splitColor, false);
        poseStack.popPose();

        // 警告文字（多行，右对齐）
        String[] warningLines = {
                "§7人类的赞歌就是勇气的赞歌...",
                "§7人类的伟大就是勇气的伟大...",
                "§7但重生需要代价..."
        };
        for (int i = 0; i < warningLines.length; i++) {
            String line = warningLines[i];
            int lineX = boxX + boxWidth - PADDING - font.width(line);
            guiGraphics.drawString(this.font, line, lineX, boxY + 70 + i * 12, 0xFF888888, false);
        }


        // ========== 死亡原因（居中显示，在按钮上方）==========
        String deathReason = data.deathMessage().getString();
        int maxDeathReasonWidth = boxWidth - 40;
        if (font.width(deathReason) > maxDeathReasonWidth) {
            deathReason = font.plainSubstrByWidth(deathReason, maxDeathReasonWidth - font.width("...")) + "...";
        }
        poseStack.pushPose();
        poseStack.scale(1.15f, 1.15f, 1.0f);
        int deathReasonScaledY = (int) ((boxY + 107) / 1.15f);
        guiGraphics.drawCenteredString(this.font, "§c" + deathReason, (int) (centerX / 1.15f), deathReasonScaledY, 0xFFFFFFFF);
        poseStack.popPose();

        // ========== 死亡位置提示文字（按钮下方）==========
        String posClickText = economySystem$showDeathPos ? "§7点击此处隐藏您的死亡位置" : "§7点击此处查看您当前死亡位置";
        int posClickTextWidth = font.width(posClickText);
        int posClickY = boxY + 222;  // 按钮底部和下边框中间
        economySystem$posButtonX = centerX - posClickTextWidth / 2;
        economySystem$posButtonY = posClickY;
        economySystem$posButtonWidth = posClickTextWidth;
        economySystem$posButtonHeight = 12;

        // 鼠标悬停时文字变亮
        boolean hovering = mouseX >= economySystem$posButtonX && mouseX < economySystem$posButtonX + economySystem$posButtonWidth &&
                           mouseY >= economySystem$posButtonY && mouseY < economySystem$posButtonY + economySystem$posButtonHeight;
        int textColor = hovering ? 0xFFAAAAFF : 0xFF888888;
        guiGraphics.drawCenteredString(this.font, posClickText, centerX, posClickY, textColor);

        // ========== 死亡位置显示 ==========
        if (economySystem$showDeathPos) {
            String posText = String.format("§c死亡位置: §7X: §c%d §7Y: §c%d §7Z: §c%d",
                    (int)data.deathX(), (int)data.deathY(), (int)data.deathZ());
            String dimText = "§7维度: §c" + formatDimension(data.dimension());

            int posTextX = centerX - font.width(posText) / 2;
            int dimTextX = centerX - font.width(dimText) / 2;

            // 背景（在主框框下方弹出）
            int infoBoxWidth = Math.max(font.width(posText), font.width(dimText)) + 20;
            int infoBoxX = centerX - infoBoxWidth / 2;
            int infoBoxHeight = 30;
            int infoBoxY = boxY + boxHeight + 8;  // 主框框下方8像素

            guiGraphics.fill(infoBoxX, infoBoxY, infoBoxX + infoBoxWidth, infoBoxY + infoBoxHeight, 0xCC000000);
            guiGraphics.fill(infoBoxX, infoBoxY, infoBoxX + infoBoxWidth, infoBoxY + 1, 0xFF330000);
            guiGraphics.fill(infoBoxX, infoBoxY + infoBoxHeight - 1, infoBoxX + infoBoxWidth, infoBoxY + infoBoxHeight, 0xFF330000);
            guiGraphics.fill(infoBoxX, infoBoxY, infoBoxX + 1, infoBoxY + infoBoxHeight, 0xFF330000);
            guiGraphics.fill(infoBoxX + infoBoxWidth - 1, infoBoxY, infoBoxX + infoBoxWidth, infoBoxY + infoBoxHeight, 0xFF330000);

            guiGraphics.drawString(this.font, posText, posTextX, infoBoxY + 8, 0xFFFFFFFF, false);
            guiGraphics.drawString(this.font, dimText, dimTextX, infoBoxY + 20, 0xFFFFFFFF, false);
        }

        // ========== 渲染按钮 ==========
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

    /**
     * 注入到 mouseClicked() 方法，处理死亡位置按钮点击
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = true)
    private void economySystem$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        // 检查是否点击了死亡位置按钮
        if (mouseX >= economySystem$posButtonX && mouseX < economySystem$posButtonX + economySystem$posButtonWidth &&
            mouseY >= economySystem$posButtonY && mouseY < economySystem$posButtonY + economySystem$posButtonHeight) {
            // 切换显示状态
            economySystem$showDeathPos = !economySystem$showDeathPos;
            cir.setReturnValue(true);
        }
    }

    /**
     * 绘制圆角矩形
     */
    @Unique
    private void renderRoundedBox(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        guiGraphics.fill(x1 + 1, y1, x2 - 1, y2, color);
        guiGraphics.fill(x1, y1 + 1, x2, y2 - 1, color);
    }

    /**
     * 格式化维度名称
     */
    @Unique
    private String formatDimension(String dimension) {
        return switch (dimension) {
            case "minecraft:overworld" -> "主世界";
            case "minecraft:the_nether" -> "下界";
            case "minecraft:the_end" -> "末地";
            default -> dimension;
        };
    }

    /**
     * 压迫感风格的自定义按钮
     */
    @Unique
    private static class CustomButton extends Button {
        private final boolean isKeepInventory;
        private final float cost;
        private final float currentPoints;

        public CustomButton(int x, int y, int width, int height, Component message,
                           boolean isKeepInventory, float cost, float currentPoints, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.isKeepInventory = isKeepInventory;
            this.cost = cost;
            this.currentPoints = currentPoints;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHovered();
            boolean canAfford = this.cost <= 0 || currentPoints >= cost;

            // 根据按钮类型选择颜色主题
            int topColor, bottomColor, borderColor;
            if (!this.active || !canAfford) {
                // 禁用状态
                topColor = 0xCC333333;
                bottomColor = 0xCC222222;
                borderColor = 0xCC555555;
            } else if (isKeepInventory) {
                // 保留物品按钮（金色主题）
                if (hovered) {
                    topColor = 0xCCAA6600;
                    bottomColor = 0xCC663300;
                    borderColor = 0xFFFFCC00;
                } else {
                    topColor = 0xCC884400;
                    bottomColor = 0xCC442200;
                    borderColor = 0xCCAA8800;
                }
            } else {
                // 正常复活按钮（绿色主题）
                if (hovered) {
                    topColor = 0xCC006600;
                    bottomColor = 0xCC003300;
                    borderColor = 0xFF00FF00;
                } else {
                    topColor = 0xCC004400;
                    bottomColor = 0xCC002200;
                    borderColor = 0xCC008800;
                }
            }

            int x = getX();
            int y = getY();
            int w = width;
            int h = height;

            // 渐变背景
            guiGraphics.fill(x + 2, y, x + w - 2, y + h, topColor);
            guiGraphics.fill(x + 2, y + h, x + w - 2, y + h + 1, bottomColor);

            // 边框
            guiGraphics.fill(x + 1, y, x + 2, y + h, borderColor);
            guiGraphics.fill(x + w - 2, y, x + w - 1, y + h, borderColor);
            guiGraphics.fill(x + 2, y, x + w - 2, y + 1, borderColor);
            guiGraphics.fill(x + 2, y + h - 1, x + w - 2, y + h, borderColor);

            // 角落装饰
            guiGraphics.fill(x, y, x + 1, y + 1, borderColor);
            guiGraphics.fill(x + w - 1, y, x + w, y + 1, borderColor);
            guiGraphics.fill(x, y + h - 1, x + 1, y + h, borderColor);
            guiGraphics.fill(x + w - 1, y + h - 1, x + w, y + h, borderColor);

            // 文字
            String displayText = getMessage().getString();
            int textColor = canAfford ? 0xFFFFFF : 0x666666;
            int textX = x + w / 2 - Minecraft.getInstance().font.width(displayText) / 2;
            int textY = y + (h - 8) / 2;

            // 主文字（无阴影）
            guiGraphics.drawString(Minecraft.getInstance().font, displayText, textX, textY, textColor, false);
        }
    }
}

package com.mo.economy_system.screen.npc_system;

import com.mo.economy_system.client.util.VirtualCoordinateHelper;
import com.mo.economy_system.core.npc_system.NpcDialogueViewData;
import com.mo.economy_system.core.npc_system.NpcInteractionType;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.npc_system.Packet_NpcInteractionRequest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class Screen_NpcDialogue extends Screen {
    private static final Minecraft MC = Minecraft.getInstance();

    private static final String TITLE_FALLBACK = "NPC对话";
    private static final String LABEL_RELATION = "关系";
    private static final String LABEL_FAVORABILITY = "好感度";
    private static final String LABEL_PROFESSION = "职业";
    private static final String LABEL_STAGE = "剧情阶段";
    private static final String LABEL_THOUGHT = "想法";
    private static final String LABEL_WANTED_ITEM = "想要物品";
    private static final String LABEL_DIALOGUE = "对话";
    private static final String LABEL_ACTION = "交互";
    private static final String BUTTON_DIALOGUE = "交谈";
    private static final String BUTTON_GIFT = "赠予手中物品";
    private static final String BUTTON_FOLLOW = "跟随";
    private static final String BUTTON_SET_HOME = "定为家";
    private static final String TEXT_LOCKED = "未解锁";
    private static final String TEXT_EMPTY_DIALOGUE = "他暂时没有更多想说的话。";
    private static final String TEXT_NO_THOUGHT = "暂时没有特别在意的事。";
    private static final String TEXT_CLOSE_HINT = "ESC 关闭";

    private static final int COLOR_STAGE_BLACK = 0xF8000000;
    private static final int COLOR_TITLE = 0xFFFFD88A;
    private static final int COLOR_TEXT = 0xFFEFE6D0;
    private static final int COLOR_MUTED = 0xFFB8AA91;
    private static final int COLOR_GOOD = 0xFF9DE08F;
    private static final int COLOR_ACCENT = 0xFFCDAA64;
    private static final int COLOR_OPTION = 0xFFEAD9B4;
    private static final int COLOR_OPTION_HOVER = 0xFFFFD878;
    private static final int COLOR_OPTION_LOCKED = 0xFF777064;
    private static final int COLOR_DIVIDER = 0x44D0B16F;

    private final NpcDialogueViewData data;
    private final VirtualCoordinateHelper.VirtualSizeResult virtualSize = new VirtualCoordinateHelper.VirtualSizeResult();
    private final List<TextActionArea> textActionAreas = new ArrayList<>();

    private int virtualWidth;
    private int virtualHeight;
    private float uiScale;
    private int stageTop;
    private int solidTop;
    private int modelCenterX;
    private int modelFootY;
    private int infoX;
    private int dialogueX;
    private int dialogueWidth;
    private int actionX;
    private int actionWidth;
    private int contentTop;
    private int dialogueIndex;
    private long openTime;

    public Screen_NpcDialogue(NpcDialogueViewData data) {
        super(Component.literal(TITLE_FALLBACK));
        this.data = data;
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        calculateVirtualLayout();
        rebuildTextActions();
    }

    private void calculateVirtualLayout() {
        VirtualCoordinateHelper.calculateVirtualSize(this, virtualSize);
        uiScale = virtualSize.uiScale;
        virtualWidth = virtualSize.virtualWidth;
        virtualHeight = virtualSize.virtualHeight;

        stageTop = Math.max(170, virtualHeight / 2 - 8);
        solidTop = stageTop + 28;
        modelCenterX = 82;
        modelFootY = virtualHeight - 24;
        infoX = 142;
        actionWidth = 128;
        actionX = virtualWidth - actionWidth - 24;
        dialogueX = Math.max(236, virtualWidth / 3 + 18);
        dialogueWidth = Math.max(150, actionX - dialogueX - 26);
        contentTop = solidTop + 16;
    }

    private void rebuildTextActions() {
        textActionAreas.clear();
        List<TextAction> actions = new ArrayList<>();
        actions.add(new TextAction(BUTTON_DIALOGUE, NpcInteractionType.DIALOGUE, true));
        actions.add(new TextAction(BUTTON_GIFT, NpcInteractionType.GIFT_ITEM, true));
        actions.add(new TextAction(BUTTON_FOLLOW, NpcInteractionType.FOLLOW, isActionAvailable(NpcInteractionType.FOLLOW)));
        actions.add(new TextAction(BUTTON_SET_HOME, NpcInteractionType.SET_HOME, isActionAvailable(NpcInteractionType.SET_HOME)));

        int y = contentTop + 24;
        for (TextAction action : actions) {
            String label = action.enabled ? action.label : action.label + " / " + TEXT_LOCKED;
            textActionAreas.add(new TextActionArea(actionX, y, MC.font.width(label), MC.font.lineHeight + 5, label, action.type, action.enabled));
            y += 18;
        }
    }

    private boolean isActionAvailable(NpcInteractionType type) {
        return data.getAvailableActions().contains(type.name());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        calculateVirtualLayout();
        rebuildTextActions();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(uiScale, uiScale, 1.0f);
        float virtualMouseX = mouseX / uiScale;
        float virtualMouseY = mouseY / uiScale;

        renderStage(guiGraphics);
        renderNpcModel(guiGraphics, virtualMouseX, virtualMouseY);
        renderInfoColumn(guiGraphics);
        renderDialogueColumn(guiGraphics);
        renderActionColumn(guiGraphics, virtualMouseX, virtualMouseY);
        renderFooter(guiGraphics);

        guiGraphics.pose().popPose();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderStage(GuiGraphics guiGraphics) {
        renderQuickFade(guiGraphics);
        guiGraphics.fill(0, solidTop, virtualWidth, virtualHeight, COLOR_STAGE_BLACK);
    }

    private void renderQuickFade(GuiGraphics guiGraphics) {
        int fadeTop = stageTop;
        for (int y = fadeTop; y < solidTop; y += 3) {
            float ratio = (float) (y - fadeTop) / Math.max(1, solidTop - fadeTop);
            int alpha = (int) (ratio * 248.0f);
            int color = (alpha << 24);
            guiGraphics.fill(0, y, virtualWidth, Math.min(solidTop, y + 3), color);
        }
    }

    private void renderNpcModel(GuiGraphics guiGraphics, float mouseX, float mouseY) {
        LivingEntity entity = getDialogueEntity();
        if (entity == null) {
            return;
        }

        int modelSize = Math.max(54, Math.min(112, virtualHeight / 4));
        float relativeMouseX = mouseX - modelCenterX;
        float relativeMouseY = mouseY - modelFootY + 70;
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics,
                modelCenterX - modelSize,
                modelFootY - modelSize * 2,
                modelCenterX + modelSize,
                modelFootY,
                modelSize,
                0.0625F,
                -relativeMouseX * 0.35f,
                -relativeMouseY * 0.18f,
                entity
        );
    }

    private void renderInfoColumn(GuiGraphics guiGraphics) {
        int y = contentTop - 6;
        guiGraphics.drawString(MC.font, data.getNpcName(), infoX, y, COLOR_TITLE, false);
        y += 18;

        drawInfoLine(guiGraphics, LABEL_RELATION, data.getRelationName(), infoX, y, COLOR_GOOD);
        y += 13;
        drawInfoLine(guiGraphics, LABEL_FAVORABILITY, String.valueOf(data.getFavorability()), infoX, y, COLOR_GOOD);
        y += 13;
        drawInfoLine(guiGraphics, LABEL_PROFESSION, data.getNpcProfession(), infoX, y, COLOR_MUTED);
        y += 13;
        drawInfoLine(guiGraphics, LABEL_STAGE, String.valueOf(data.getStoryStageId()), infoX, y, COLOR_MUTED);
    }

    private void drawInfoLine(GuiGraphics guiGraphics, String label, String value, int x, int y, int valueColor) {
        guiGraphics.drawString(MC.font, label, x, y, COLOR_ACCENT, false);
        guiGraphics.drawString(MC.font, value, x + 48, y, valueColor, false);
    }

    private void renderDialogueColumn(GuiGraphics guiGraphics) {
        drawColumnHeader(guiGraphics, LABEL_DIALOGUE, dialogueX, contentTop - 16, dialogueWidth);

        List<String> dialogues = data.getDialogues();
        String dialogue = dialogues.isEmpty() ? TEXT_EMPTY_DIALOGUE : dialogues.get(Mth.positiveModulo(dialogueIndex, dialogues.size()));
        drawTypewriterWrapped(guiGraphics, dialogue, dialogueX, contentTop + 4, dialogueWidth, COLOR_TEXT, 4);

        String intro = data.getNpcIntroduction();
        if (!intro.isEmpty()) {
            drawWrapped(guiGraphics, intro, dialogueX, contentTop + 54, dialogueWidth, COLOR_MUTED, 2);
        }

        int thoughtY = virtualHeight - 52;
        guiGraphics.drawString(MC.font, LABEL_THOUGHT, dialogueX, thoughtY, COLOR_TITLE, false);
        String thought = data.getThoughtText().isEmpty() ? TEXT_NO_THOUGHT : data.getThoughtText();
        drawWrapped(guiGraphics, thought, dialogueX + 34, thoughtY, dialogueWidth - 34, COLOR_MUTED, 1);
        if (!data.getWantedItemId().isEmpty()) {
            guiGraphics.drawString(MC.font, LABEL_WANTED_ITEM + ": " + data.getWantedItemId(), dialogueX + 34, thoughtY + 12, COLOR_MUTED, false);
        }
    }

    private void renderActionColumn(GuiGraphics guiGraphics, float mouseX, float mouseY) {
        drawColumnHeader(guiGraphics, LABEL_ACTION, actionX, contentTop - 16, actionWidth);
        for (TextActionArea area : textActionAreas) {
            boolean hovered = area.contains((int) mouseX, (int) mouseY);
            int color = area.enabled ? (hovered ? COLOR_OPTION_HOVER : COLOR_OPTION) : COLOR_OPTION_LOCKED;
            String prefix = hovered && area.enabled ? "> " : "  ";
            guiGraphics.drawString(MC.font, prefix + area.label, area.x, area.y, color, false);
        }
    }

    private void drawColumnHeader(GuiGraphics guiGraphics, String title, int x, int y, int width) {
        guiGraphics.drawString(MC.font, title, x, y, COLOR_ACCENT, false);
        guiGraphics.fill(x, y + 12, x + width, y + 13, COLOR_DIVIDER);
    }

    private void renderFooter(GuiGraphics guiGraphics) {
        guiGraphics.drawString(MC.font, TEXT_CLOSE_HINT, 20, virtualHeight - 22, COLOR_MUTED, false);
    }

    private void drawWrapped(GuiGraphics guiGraphics, String text, int x, int y, int width, int color, int maxLines) {
        var lines = MC.font.getSplitter().splitLines(text, width, Style.EMPTY);
        int count = Math.min(maxLines, lines.size());
        for (int i = 0; i < count; i++) {
            guiGraphics.drawString(MC.font, lines.get(i).getString(), x, y + i * 12, color, false);
        }
    }

    private void drawTypewriterWrapped(GuiGraphics guiGraphics, String text, int x, int y, int width, int color, int maxLines) {
        int visibleCharacters = Math.min(text.length(), (int) ((System.currentTimeMillis() - openTime) / 18L));
        drawWrapped(guiGraphics, text.substring(0, visibleCharacters), x, y, width, color, maxLines);
    }

    private LivingEntity getDialogueEntity() {
        if (MC.level == null || data.getEntityId() < 0) {
            return null;
        }
        Entity entity = MC.level.getEntity(data.getEntityId());
        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity;
        }
        return null;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int virtualMouseX = (int) (mouseX / uiScale);
            int virtualMouseY = (int) (mouseY / uiScale);
            for (TextActionArea area : textActionAreas) {
                if (area.contains(virtualMouseX, virtualMouseY)) {
                    if (area.enabled) {
                        if (area.type == NpcInteractionType.DIALOGUE) {
                            dialogueIndex++;
                            openTime = System.currentTimeMillis();
                        }
                        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_NpcInteractionRequest(data.getNpcId(), data.getEntityId(), area.type));
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record TextAction(String label, NpcInteractionType type, boolean enabled) {
    }

    private record TextActionArea(int x, int y, int width, int height, String label, NpcInteractionType type, boolean enabled) {
        private boolean contains(int mouseX, int mouseY) {
            return mouseX >= x - 4 && mouseX <= x + width + 16 && mouseY >= y - 3 && mouseY <= y + height + 3;
        }
    }
}

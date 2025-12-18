package com.mo.economy_system.core.task_system.taskui;

import com.mo.economy_system.core.task_system.TaskPlayerData;
import com.mo.economy_system.core.task_system.TaskServerData;
import com.mo.economy_system.network.packets.task_system.Packet_SyncFullTaskData;
import com.mo.economy_system.server.serverui.ServerInformationDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics; // 必须导入，解决guiGraphics解析问题
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component; // 注意：用Component而非MutableComponent
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class TaskUI_Screen extends Screen {
    //样式参数
    private static final int BACKGROUND_ALPHA = 128; //背景透明度
    private static final float UI_WIDTH_PERCENT = 0.75F; //宽度百分比
    private static final float UI_HEIGHT_PERCENT = 0.7F; //高度百分比
    private static final int BORDER_COLOR = 0xFFFFFFFF; // 白色边框（ARGB：0xFF=全透，FFFFFF=白色）
    private static final int BG_COLOR = (BACKGROUND_ALPHA << 24) | 0x000000; //半透明黑背景

    //按钮参数
    private static final int BTN_WIDTH = 60;
    private static final int BTN_HEIGHT = 18;
    private static final int BTN_GAP = 2;
    private static final int LOGO_BTN_WIDTH = 80;

    // 屏幕坐标
    private int screenWidth;
    private int screenHeight;
    private int screenUIWidth;
    private int screenUIHeight;
    private int uiX;
    private int uiY;

    // 按钮对象
    private Button serverTaskBtn;
    private Button playerTaskBtn;
    //Logo按钮
    private Button logoBtn;
    //子界面 0=无，1=服务器任务，2=个人任务
    private int showSubScreen = 0;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TaskUI_Screen() {
        super(Component.literal("任务界面"));
    }

    @Override
    protected void init() {
        super.init();
        // 计算黑框坐标
        this.screenWidth = this.width;
        this.screenHeight = this.height;
        this.screenUIWidth = (int) (screenWidth * UI_WIDTH_PERCENT);
        this.screenUIHeight = (int) (screenHeight * UI_HEIGHT_PERCENT);
        this.uiX = (screenWidth - screenUIWidth) / 2;
        this.uiY = (screenHeight - screenUIHeight) / 2;

        // 按钮坐标（黑框上方10像素，水平居中）
        int totalBtnWidth = BTN_WIDTH * 2 + BTN_GAP;
        int btnStartX = LOGO_BTN_WIDTH + 2 + uiX;
        int btnY = uiY - BTN_HEIGHT - 2;

        //Logo按钮
        int logoBtnX = uiX;
        Button.Builder logoBtnBuilder = Button.builder(Component.literal("§bDreaming§dFish"), new Button.OnPress() {
                    @Override
                    public void onPress(Button btn) {
                        //不可交互，点击无响应
                    }
                })
                .pos(logoBtnX, btnY) //Logo按钮位置）
                .size(LOGO_BTN_WIDTH, BTN_HEIGHT); // Logo按钮尺寸

        //创建Logo按钮，不可交互，无悬浮高亮
        logoBtn = logoBtnBuilder.build((builder) -> {
            return new Button(builder) {
                // 标记按钮不可交互
                @Override
                public boolean isActive() {
                    return false;
                }

                @Override
                public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                    //背景色固定为半透黑，无悬浮高亮
                    int bgColor = (128 << 24) | 0x000000;
                    //绘制Logo按钮背景
                    guiGraphics.fill(RenderType.gui(), this.getX(), this.getY(),
                            this.getX() + this.getWidth(), this.getY() + this.getHeight(), bgColor);
                    //绘制白色边框
                    int borderColor = 0xFFFFFFFF;
                    guiGraphics.fill(RenderType.gui(), this.getX(), this.getY(),
                            this.getX() + this.getWidth(), this.getY() + 1, borderColor);
                    guiGraphics.fill(RenderType.gui(), this.getX(), this.getY() + this.getHeight() - 1,
                            this.getX() + this.getWidth(), this.getY() + this.getHeight(), borderColor);
                    guiGraphics.fill(RenderType.gui(), this.getX(), this.getY(),
                            this.getX() + 1, this.getY() + this.getHeight(), borderColor);
                    guiGraphics.fill(RenderType.gui(), this.getX() + this.getWidth() - 1, this.getY(),
                            this.getX() + this.getWidth(), this.getY() + this.getHeight(), borderColor);
                    //绘制Logo文字
                    guiGraphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
                            this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFF);
                }
            };
        });

        //服务器按钮
        Button.Builder serverBtnBuilder = Button.builder(Component.literal("服务器任务"), new Button.OnPress() {
                    @Override
                    public void onPress(Button btn) {
                        showSubScreen = 1; // 切换到服务器任务子界面（不跳转Screen）
                    }
                }).pos(btnStartX, btnY)
                .size(BTN_WIDTH, BTN_HEIGHT);

        //创建自定义按钮（重写renderWidget）
        serverTaskBtn = serverBtnBuilder.build((builder) -> {
            // 创建Button子类，重写渲染方法
            return new Button(builder) {
                @Override
                public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                    // 背景色（半透黑，悬浮高亮）
                    int bgColor = (128 << 24) | 0x000000;
                    if (this.isHoveredOrFocused()) {
                        bgColor = (180 << 24) | 0x111111;
                    }
                    // 绘制按钮背景
                    guiGraphics.fill(RenderType.gui(), this.getX(), this.getY(),
                            this.getX() + this.getWidth(), this.getY() + this.getHeight(), bgColor);
                    // 绘制白色边框
                    int borderColor = 0xFFFFFFFF;
                    // 上边框
                    guiGraphics.fill(RenderType.gui(), this.getX(), this.getY(),
                            this.getX() + this.getWidth(), this.getY() + 1, borderColor);
                    // 下边框
                    guiGraphics.fill(RenderType.gui(), this.getX(), this.getY() + this.getHeight() - 1,
                            this.getX() + this.getWidth(), this.getY() + this.getHeight(), borderColor);
                    // 左边框
                    guiGraphics.fill(RenderType.gui(), this.getX(), this.getY(),
                            this.getX() + 1, this.getY() + this.getHeight(), borderColor);
                    // 右边框
                    guiGraphics.fill(RenderType.gui(), this.getX() + this.getWidth() - 1, this.getY(),
                            this.getX() + this.getWidth(), this.getY() + this.getHeight(), borderColor);
                    // 绘制居中文字（选中时高亮）
                    int textColor = 0xFFFFFF; // 默认白色
                    if (showSubScreen == 1) { // 服务器任务按钮被选中
                        textColor = 0xFFD700;
                    }

                    guiGraphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
                            this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, textColor);
                }
            };
        });

        //个人任务
        Button.Builder playerBtnBuilder = Button.builder(Component.literal("个人任务"), new Button.OnPress() {
                    @Override
                    public void onPress(Button btn) {
                        showSubScreen = 2; // 切换到个人任务子界面（不跳转Screen）
                    }
                })
                .pos(btnStartX + BTN_WIDTH + BTN_GAP, btnY)
                .size(BTN_WIDTH, BTN_HEIGHT);


        playerTaskBtn = playerBtnBuilder.build((builder) -> {
            return new Button(builder) {
                @Override
                public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                    int bgColor = (128 << 24) | 0x000000;
                    if (this.isHoveredOrFocused()) {
                        bgColor = (180 << 24) | 0x111111;
                    }
                    guiGraphics.fill(RenderType.gui(), this.getX(), this.getY(),
                            this.getX() + this.getWidth(), this.getY() + this.getHeight(), bgColor);

                    int borderColor = 0xFFFFFFFF;
                    guiGraphics.fill(RenderType.gui(), this.getX(), this.getY(),
                            this.getX() + this.getWidth(), this.getY() + 1, borderColor);
                    guiGraphics.fill(RenderType.gui(), this.getX(), this.getY() + this.getHeight() - 1,
                            this.getX() + this.getWidth(), this.getY() + this.getHeight(), borderColor);
                    guiGraphics.fill(RenderType.gui(), this.getX(), this.getY(),
                            this.getX() + 1, this.getY() + this.getHeight(), borderColor);
                    guiGraphics.fill(RenderType.gui(), this.getX() + this.getWidth() - 1, this.getY(),
                            this.getX() + this.getWidth(), this.getY() + this.getHeight(), borderColor);

                    int textColor = 0xFFFFFF;
                    if (showSubScreen == 2) { // 个人任务按钮被选中
                        textColor = 0xFFD700;
                    }

                    guiGraphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
                            this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, textColor);
                }
            };
        });

        // 添加按钮到屏幕
        this.addRenderableWidget(logoBtn);
        this.addRenderableWidget(serverTaskBtn);
        this.addRenderableWidget(playerTaskBtn);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染主界面背景和边框
        guiGraphics.fill(RenderType.gui(), uiX, uiY, uiX + screenUIWidth, uiY + screenUIHeight, BG_COLOR);
        // 上边框
        guiGraphics.fill(RenderType.gui(), uiX, uiY, uiX + screenUIWidth, uiY + 1, BORDER_COLOR);
        // 下边框
        guiGraphics.fill(RenderType.gui(), uiX, uiY + screenUIHeight - 1, uiX + screenUIWidth, uiY + screenUIHeight, BORDER_COLOR);
        // 左边框
        guiGraphics.fill(RenderType.gui(), uiX, uiY, uiX + 1, uiY + screenUIHeight, BORDER_COLOR);
        // 右边框
        guiGraphics.fill(RenderType.gui(), uiX + screenUIWidth - 1, uiY, uiX + screenUIWidth, uiY + screenUIHeight, BORDER_COLOR);

        // 2. 渲染按钮（父类方法会渲染已添加的所有按钮）
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 3. 根据子界面标识渲染对应内容（替代原ServerTaskScreen和PlayerTaskScreen）
        switch (showSubScreen) {
            case 1:
                renderServerTaskContent(guiGraphics); // 渲染服务器任务列表及详情
                break;
            case 2:
                renderPlayerTaskContent(guiGraphics); // 渲染个人任务列表及详情
                break;
            default:
                // 初始状态提示文字
                guiGraphics.drawString(this.font, "请选择任务类型", uiX + 20, uiY + 20, 0xFFFFFF);
        }
    }

    private int selectedServerTaskId = -1; // 选中的服务器任务ID，-1表示未选中
    private int selectedPlayerTaskId = -1;
    private void renderPlayerTaskContent(GuiGraphics guiGraphics) {
        // 计算两栏布局参数
        int MARGIN = 10;
        int listWidth = (int) (screenUIWidth * 0.3f);
        int detailWidth = screenUIWidth - listWidth - MARGIN * 2;
        int listX = uiX + MARGIN;
        int detailX = listX + listWidth + MARGIN;
        int contentY = uiY + MARGIN;
        int line_height = 20;
        int listContentY = contentY + line_height;

        // 获取个人任务数据
        Map<Integer, TaskPlayerData> playerTasks = Packet_SyncFullTaskData.ClientTaskCache.getClientPlayerTaskCache();

        if (playerTasks.isEmpty()) {
            guiGraphics.drawString(this.font, "暂无个人任务", listX, listContentY, 0xAAAAAA);
        } else {
            // 排序任务ID
            List<Integer> taskIds = new ArrayList<>(playerTasks.keySet());
            Collections.sort(taskIds);

            // 绘制任务列表
            for (int i = 0; i < taskIds.size(); i++) {
                int taskId = taskIds.get(i);
                TaskPlayerData task = playerTasks.get(taskId);
                int currentY = listContentY + i * line_height;

                // 绘制选中状态背景
                if (selectedPlayerTaskId == taskId) {
                    guiGraphics.fill(RenderType.gui(),
                            listX, currentY,
                            listX + listWidth, currentY + line_height - 2,
                            0x5050FFFF); // 蓝色半透背景
                }

                // 绘制序号和标题（已完成显示绿色）
                String taskText = String.format("%d. %s", taskId, task.getTaskName());
                int textColor = task.isClientPlayerFinished() ? 0x00FF00 : 0xFFFFFF;
                guiGraphics.drawString(this.font, taskText, listX + 2, currentY, textColor);
            }
        }

        // 绘制右侧详情区域
        guiGraphics.drawString(this.font, "任务详情", detailX, contentY, 0xFFFFFF);
        int detailContentY = contentY + line_height * 2;

        if (selectedPlayerTaskId == -1) {
            guiGraphics.drawString(this.font, "请从左侧选择任务", detailX, detailContentY, 0xAAAAAA);
        } else {
            TaskPlayerData selectedTask = playerTasks.get(selectedPlayerTaskId);
            if (selectedTask != null) {
                // 大字显示标题（缩放1.5倍）
                guiGraphics.pose().pushPose();
                guiGraphics.pose().scale(1.5f, 1.5f, 1.5f);
                guiGraphics.drawString(this.font, selectedTask.getTaskName(),
                        (int) (detailX / 1.5f), (int) (detailContentY / 1.5f), 0xFFFF00);
                guiGraphics.pose().popPose();

                // 显示任务内容
                int contentStartY = detailContentY + 30;
                String[] contentLines = selectedTask.getTaskContent().split("\n");
                for (int i = 0; i < contentLines.length; i++) {
                    guiGraphics.drawString(this.font, contentLines[i],
                            detailX, contentStartY + i * line_height, 0xFFFFFF);
                }

                // 显示任务时间信息
                long startTime = selectedTask.getTaskStartTime();
                long endTime = selectedTask.getTaskEndTime();
                String timeText = String.format("有效期: %s - %s",
                        formatTime(startTime), formatTime(endTime));
                guiGraphics.drawString(this.font, timeText,
                        detailX, contentStartY + contentLines.length * line_height + 10, 0xAAAAAA);

                // 显示完成状态
                String statusText = selectedTask.isClientPlayerFinished() ? "你已完成此任务" : "你尚未完成此任务";
                guiGraphics.drawString(this.font, statusText,
                        detailX, contentStartY + (contentLines.length + 1) * line_height + 10,
                        selectedTask.isClientPlayerFinished() ? 0x00FF00 : 0xFF0000);
            } else {
                guiGraphics.drawString(this.font, "任务数据不存在", detailX, detailContentY, 0xFF0000);
            }
        }
    }
    // 渲染服务器任务内容（复用原ServerTaskScreen的逻辑，调整为子内容区域）
    private void renderServerTaskContent(GuiGraphics guiGraphics) {
        //计算两栏布局坐标（基于主UI区域）
        int margin = 10; // 内边距
        int listWidth = (int) (screenUIWidth * 0.3f); // 左侧列表占30%宽度
        int detailWidth = screenUIWidth - listWidth - margin * 2; // 右侧详情区域宽度
        int listX = uiX + margin; // 左侧列表X坐标
        int detailX = listX + listWidth + margin; // 右侧详情X坐标
        int contentY = uiY + margin; // 内容区域起始Y坐标
        int lineHeight = 20; // 每行高度
        int listContentY = contentY + lineHeight; // 列表内容起始Y

        //遍历客户端缓存的服务器任务，渲染列表
        Map<Integer, TaskServerData> serverTasks = Packet_SyncFullTaskData.ClientTaskCache.getClientServerTaskCache();
        if (serverTasks.isEmpty()) {
            guiGraphics.drawString(this.font, "暂无服务器任务", listX, listContentY, 0xAAAAAA);
        } else {
            // 按任务ID排序（简单处理）
            List<Integer> taskIds = new ArrayList<>(serverTasks.keySet());
            Collections.sort(taskIds);

            for (int i = 0; i < taskIds.size(); i++) {
                int taskId = taskIds.get(i);
                TaskServerData task = serverTasks.get(taskId);
                int currentY = listContentY + i * lineHeight;

                // 绘制选中状态背景
                if (selectedServerTaskId == taskId) {
                    guiGraphics.fill(RenderType.gui(),
                            listX, currentY,
                            listX + listWidth, currentY + lineHeight - 2,
                            0x5050FFFF); // 选中项蓝色半透背景
                }

                // 绘制序号和任务标题
                String taskText = String.format("%d. %s", taskId, task.getTaskName());
                guiGraphics.drawString(this.font, taskText, listX + 2, currentY,
                        task.isClientPlayerFinished() ? 0x00FF00 : 0xFFFFFF); // 已完成任务绿色
            }
        }

        //绘制右侧详情区域
        guiGraphics.drawString(this.font, "任务详情", detailX, contentY, 0xFFFFFF);
        int detailContentY = contentY + lineHeight * 2; // 详情内容起始Y（预留标题空间）

        if (selectedServerTaskId == -1) {
            // 未选中任务时显示提示
            guiGraphics.drawString(this.font, "请从左侧选择任务", detailX, detailContentY, 0xAAAAAA);
        } else {
            //显示选中任务的详情
            TaskServerData selectedTask = serverTasks.get(selectedServerTaskId);
            if (selectedTask != null) {
                //标题（大字显示，通过缩放实现）
                guiGraphics.pose().pushPose();
                guiGraphics.pose().scale(1.5f, 1.5f, 1.5f); // 放大1.5倍
                guiGraphics.drawString(this.font, selectedTask.getTaskName(),
                        (int)(detailX / 1.5f), (int)(detailContentY / 1.5f), 0xFFFF00);
                guiGraphics.pose().popPose();

                // 内容
                int contentStartY = detailContentY + 30; // 标题下方留出空间
                String[] contentLines = selectedTask.getTaskContent().split("\n"); // 支持手动换行
                for (int i = 0; i < contentLines.length; i++) {
                    guiGraphics.drawString(this.font, contentLines[i],
                            detailX, contentStartY + i * lineHeight, 0xFFFFFF);
                }

                // 显示完成进度
                String progressText = String.format("完成进度: %.1f%%", selectedTask.getTaskCompletePercentage());
                guiGraphics.drawString(this.font, progressText,
                        detailX, contentStartY + contentLines.length * lineHeight + 10, 0x00FFFF);

                // 显示当前玩家完成状态
                String statusText = selectedTask.isClientPlayerFinished() ? "你已完成此任务" : "你尚未完成此任务";
                guiGraphics.drawString(this.font, statusText,
                        detailX, contentStartY + (contentLines.length + 1) * lineHeight + 10,
                        selectedTask.isClientPlayerFinished() ? 0x00FF00 : 0xFF0000);
            } else {
                guiGraphics.drawString(this.font, "任务数据不存在", detailX, detailContentY, 0xFF0000);
            }
        }
    }

    private String formatTime(long timestamp) {
        try {
            // 将毫秒时间戳转换为本地时间
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(timestamp),
                    ZoneId.systemDefault()
            );
            return TIME_FORMATTER.format(dateTime);
        } catch (Exception e) {
            // 异常时返回原始时间戳
            return String.valueOf(timestamp);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 只有在显示服务器任务界面时处理点击
        if (showSubScreen == 1) {
            int margin = 10;
            int listWidth = (int) (screenUIWidth * 0.3f);
            int listX = uiX + margin;
            int listY = uiY + margin + 20; // 跳过列表标题行
            int lineHeight = 20;

            // 检查点击是否在左侧列表区域
            if (mouseX >= listX && mouseX <= listX + listWidth
                    && mouseY >= listY && mouseY <= uiY + screenUIHeight - margin) {

                // 计算点击的是第几行
                int row = (int) ((mouseY - listY) / lineHeight);
                Map<Integer, TaskServerData> serverTasks = Packet_SyncFullTaskData.ClientTaskCache.getClientServerTaskCache();
                List<Integer> taskIds = new ArrayList<>(serverTasks.keySet());
                Collections.sort(taskIds);

                // 更新选中的任务ID
                if (row >= 0 && row < taskIds.size()) {
                    selectedServerTaskId = taskIds.get(row);
                    return true;
                } else {
                    selectedServerTaskId = -1; // 点击空白区域取消选中
                }
            }
        }
        else if (showSubScreen == 2) {
            int MARGIN = 10;
            int listWidth = (int) (screenUIWidth * 0.3f);
            int listX = uiX + MARGIN;
            int listY = uiY + MARGIN + 20; // 跳过列表标题行
            int line_height = 20;

            // 检查点击是否在左侧列表区域
            if (mouseX >= listX && mouseX <= listX + listWidth
                    && mouseY >= listY && mouseY <= uiY + screenUIHeight - MARGIN) {

                // 计算点击的是第几行
                int row = (int) ((mouseY - listY) / line_height);
                Map<Integer, TaskPlayerData> playerTasks = Packet_SyncFullTaskData.ClientTaskCache.getClientPlayerTaskCache();
                List<Integer> taskIds = new ArrayList<>(playerTasks.keySet());
                Collections.sort(taskIds);

                // 更新选中的任务ID
                if (row >= 0 && row < taskIds.size()) {
                    selectedPlayerTaskId = taskIds.get(row);
                    return true;
                } else {
                    selectedPlayerTaskId = -1; // 点击空白区域取消选中
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ESC关闭界面
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC键
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null); // 关闭当前界面
        TaskUI.setShowUI(false); // 更新UI显示状态
        if (ServerInformationDisplay.isShowUI() == false) {
            ServerInformationDisplay.toggleUI();
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false; // 打开界面时不暂停游戏
    }
}
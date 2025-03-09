package com.mo.economy_system.screen;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.screen.components.AnimatedButton;
import com.mo.economy_system.screen.components.TextAnimation;
import com.mo.economy_system.utils.Util_MessageKeys;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;

public class Screen_About extends EconomySystem_Screen {

    private static final String MOD_NAME = "Economy System";
    private static final String AUTHOR_NAME = "QingMo";
    private static final String GITHUB_URL = "https://github.com/QingMo-A/EconoeySystem"; // 替换为你的 GitHub 链接

    private TextAnimation titleA;
    private TextAnimation modName;
    private TextAnimation authorName;
    private TextAnimation githubURL;

    private static final ResourceLocation VX_TEXTURE = new ResourceLocation(EconomySystem.MODID, "textures/gui/vx.png");
    private static final ResourceLocation ZFB_TEXTURE = new ResourceLocation(EconomySystem.MODID, "textures/gui/zfb.png");

    // 背景图像的资源路径（可选，如果需要自定义背景）
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("economy_system", "textures/gui/about_screen_background.png");

    public Screen_About() {
        super(Component.translatable(Util_MessageKeys.ABOUT_TITLE_KEY));
    }

    @Override
    protected void init() {
        super.init();

        initPosition();

        // 添加一个返回按钮
        this.addRenderableWidget(
                new AnimatedButton(
                        this.width / 2 - 50,
                        this.height + 20,
                        this.width / 2 - 50,
                        this.height - 40,
                        100,
                        20,
                        Component.translatable(Util_MessageKeys.ABOUT_BACK_BUTTON_KEY),
                        1000,
                        button -> {
                            this.minecraft.setScreen(new Screen_Home()); // 返回主菜单
                        }
                )
        );

        this.initializeRenderCache();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 渲染背景
        this.renderBackground(guiGraphics);

        // 执行渲染缓存中的任务
        for (EconomySystem_Screen.RunnableWithGraphics task : renderCache) {
            task.run(guiGraphics);
        }

        // 如果有自定义背景图像，可以启用以下代码（确保资源文件路径正确）
        /*
        guiGraphics.blit(BACKGROUND_TEXTURE, 0, 0, 0, 0, this.width, this.height, 256, 256);
        */

        // 渲染图片
        renderImage(guiGraphics);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void initializeRenderCache() {
        renderCache.clear(); // 清空旧的缓存

        titleA = new TextAnimation(
                this.width / 2 - this.font.width(this.title.getString()) / 2,
                20,
                this.width / 2 - this.font.width(this.title.getString()) / 2,
                20,
                0f,
                1f,
                1000
        );

        modName = new TextAnimation(
                this.width / 2 - this.font.width(Component.translatable(Util_MessageKeys.ABOUT_MOD_NAME_KEY)) / 2,
                50,
                this.width / 2 - this.font.width(Component.translatable(Util_MessageKeys.ABOUT_MOD_NAME_KEY)) / 2,
                50,
                0f,
                1f,
                1000
        );

        authorName = new TextAnimation(
                this.width / 2 - this.font.width(Component.translatable(Util_MessageKeys.ABOUT_AUTHOR_NAME_KEY, AUTHOR_NAME)) / 2,
                70,
                this.width / 2 - this.font.width(Component.translatable(Util_MessageKeys.ABOUT_AUTHOR_NAME_KEY, AUTHOR_NAME)) / 2,
                70,
                0f,
                1f,
                1000
        );

        // 渲染 GitHub 链接
        Component githubLink = Component.translatable(Util_MessageKeys.ABOUT_GITHUB_URL_KEY, GITHUB_URL)
                .withStyle(style -> style
                        .withColor(0x55FF55) // 绿色
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, GITHUB_URL))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(Util_MessageKeys.ABOUT_TEXT_SHOW_KEY)))
                );
        githubURL = new TextAnimation(
                this.width / 2 - this.font.width(githubLink) / 2,
                90,
                this.width / 2 - this.font.width(githubLink) / 2,
                90,
                0f,
                1f,
                1000
        );

        renderCache.add((guiGraphics) -> {
            renderAnimatedText(
                    guiGraphics,
                    Component.literal(this.title.getString()),
                    titleA
            );

            renderAnimatedText(
                    guiGraphics,
                    Component.translatable(Util_MessageKeys.ABOUT_MOD_NAME_KEY),
                    modName,
                    0x8B658B
            );

            renderAnimatedText(
                    guiGraphics,
                    Component.translatable(Util_MessageKeys.ABOUT_AUTHOR_NAME_KEY, AUTHOR_NAME),
                    authorName
            );

            renderAnimatedText(
                    guiGraphics,
                    githubLink,
                    githubURL
            );
        });

        super.initializeRenderCache();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 检测 GitHub 链接的点击事件
        if (mouseX >= this.width / 2 - 100 && mouseX <= this.width / 2 + 100 && mouseY >= 85 && mouseY <= 105) {
            Minecraft.getInstance().keyboardHandler.setClipboard(GITHUB_URL);
            Minecraft.getInstance().getChatListener().handleSystemMessage(
                    Component.translatable(Util_MessageKeys.ABOUT_COPY_URL).withStyle(style -> style.withColor(0x00FF00)),
                    false
            );
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderImage(GuiGraphics guiGraphics) {
        float maxScale = 0.4f;
        int screenWidth = this.width;
        int screenHeight = this.height;

        // 计算图片尺寸（不超过屏幕的 80%）
        int imageSize = (int) (Math.min(screenWidth, screenHeight) * maxScale);
        imageSize = Math.min(imageSize, Math.min(screenWidth, screenHeight));

        //===== 微信（左下对齐） =====
        int vxX = 0; // 左侧紧贴屏幕边缘
        int vxY = screenHeight - imageSize; // 底部紧贴屏幕边缘

        // 检查高度不足时（例如窗口太矮）
        if (vxY < 0) {
            vxY = 0; // 如果高度不够，强制顶部对齐
            imageSize = screenHeight; // 图片高度占满屏幕（可能变形，需谨慎）
        }

        // 渲染微信二维码
        guiGraphics.blit(
                VX_TEXTURE,
                vxX, vxY,                // 左下角坐标
                imageSize, imageSize,    // 目标宽高
                0, 0,                    // 纹理起始UV坐标
                256, 256,                // 纹理裁剪区域（假设原图256x256）
                256, 256                 // 纹理实际尺寸
        );

        //===== 支付宝（右下对齐） =====
        int zfbX = screenWidth - imageSize; // 右侧紧贴屏幕边缘
        int zfbY = screenHeight - imageSize; // 底部紧贴屏幕边缘

        // 检查宽度不足时（例如窗口太窄）
        if (zfbX < 0) {
            zfbX = 0; // 如果宽度不够，强制左对齐
            imageSize = screenWidth; // 图片宽度占满屏幕（可能变形）
        }

        // 渲染支付宝二维码
        guiGraphics.blit(
                ZFB_TEXTURE,
                zfbX, zfbY,             // 右下角坐标
                imageSize, imageSize,    // 目标宽高
                0, 0,                    // 纹理起始UV坐标
                256, 256,                // 纹理裁剪区域
                256, 256                 // 纹理实际尺寸
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false; // 打开关于页面时游戏不暂停
    }

    @Override
    public boolean keyPressed(int p_96552_, int p_96553_, int p_96554_) {
        if (p_96552_ == 256 && this.shouldCloseOnEsc()) {
            Minecraft.getInstance().setScreen(new Screen_Home());
            return true;
        }
        return  false;
    }

    @Override
    protected void initPosition() {
        startX = Math.max((this.width / 2) - 300, 60);
        startY = Math.max((this.height - 450) / 4, 55);
    }
}

package com.mo.economy_system.screen;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.economy_system.Packet_BalanceRequest;
import com.mo.economy_system.screen.components.AnimatedButton;
import com.mo.economy_system.screen.components.TextAnimation;
import com.mo.economy_system.screen.economy_system.deliver_box.Screen_DeliveryBox;
import com.mo.economy_system.screen.economy_system.market.Screen_Market;
import com.mo.economy_system.screen.economy_system.shop.Screen_Shop;
import com.mo.economy_system.screen.newUI.EconomyMainScreen;
import com.mo.economy_system.screen.newUI.Screen_Test;
import com.mo.economy_system.screen.newUI.a1111_Screen;
import com.mo.economy_system.screen.territory_system.Screen_Territory;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Screen_Home 类用于创建和管理主界面屏幕，包含多个按钮以导航到不同的子界面，并显示玩家余额和其他相关信息。
 */
public class Screen_Home extends EconomySystem_Screen {

    private TextAnimation balanceAnimation;

    /**
     * 用于存储玩家余额，默认值为 -1 表示未获取。
     */
    private int balance = -1;

    /**
     * 存储玩家账户列表，键为玩家名称，值为余额。
     */
    private List<Map.Entry<String, Integer>> accounts;

    /**
     * 构造函数，初始化主界面标题。
     */
    public Screen_Home() {
        super(Component.translatable(Util_MessageKeys.HOME_TITLE_KEY));
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_BalanceRequest());
    }

    /**
     * 初始化屏幕组件，包括添加按钮和请求玩家余额。
     */
    @Override
    protected void init() {
        initPosition();

        // 添加商店按钮
        this.addRenderableWidget(
                new AnimatedButton(
                        -100,
                        startY,
                        startX,
                        startY,
                        100, 20,
                        Component.translatable(Util_MessageKeys.HOME_SHOP_BUTTON_KEY),
                        1000,
                        button -> {
                            // 请求服务器的商店数据并打开 ShopScreen
                            this.minecraft.setScreen(new Screen_Shop());
                        })
        );

        // 添加市场按钮
        this.addRenderableWidget(
                new AnimatedButton(
                        -100,
                        startY + 30,
                        startX,
                        startY + 30,
                        100, 20,
                        Component.translatable(Util_MessageKeys.HOME_MARKET_BUTTON_KEY),
                        1000,
                        button -> {
                            // 请求服务器的市场数据并打开 MarketScreen
                            this.minecraft.setScreen(new Screen_Market());
                        })
        );

        // 添加物资箱按钮
        this.addRenderableWidget(
                new AnimatedButton(
                        -100,                   // 初始X（屏幕左侧外）
                        startY + 60,            // 初始Y
                        startX,                 // 目标X
                        startY + 60,            // 目标Y
                        100, 20,                // 宽度和高度
                        Component.translatable(Util_MessageKeys.HOME_DELIVERY_BOX_BUTTON_KEY),
                        1000,                   // 动画持续时间 1秒
                        button -> {
                            // 请求服务器的市场数据并打开 DeliveryBoxScreen
                            this.minecraft.setScreen(new Screen_DeliveryBox());
                        })
        );

        // 添加领地按钮
        this.addRenderableWidget(
                new AnimatedButton(
                        -100,                   // 初始X
                        startY + 90,            // 初始Y
                        startX,                 // 目标X
                        startY + 90,            // 目标Y
                        100, 20,                // 宽度和高度
                        Component.translatable(Util_MessageKeys.HOME_TERRITORY_BUTTON_KEY),
                        1000,                   // 动画持续时间
                        button -> {
                            // 请求服务器的领地数据并打开 TerritoryScreen
                            this.minecraft.setScreen(new Screen_Territory());
                        })
        );

        // 添加关于按钮
        this.addRenderableWidget(
                new AnimatedButton(
                        -100,                   // 初始X
                        startY + 120,           // 初始Y
                        startX,                 // 目标X
                        startY + 120,           // 目标Y
                        100, 20,                // 宽度和高度
                        Component.translatable(Util_MessageKeys.HOME_ABOUT_BUTTON_KEY),
                        1000,                   // 动画持续时间
                        button -> {
                            // 打开 AboutScreen
                            this.minecraft.setScreen(new Screen_About());
                        })
        );

        // 添加关于按钮
        this.addRenderableWidget(
                new AnimatedButton(
                        -100,                   // 初始X
                        startY + 150,           // 初始Y
                        startX,                 // 目标X
                        startY + 150,           // 目标Y
                        100, 20,                // 宽度和高度
                        Component.literal("测试界面"),
                        1000,                   // 动画持续时间
                        button -> {
                            // 打开 AboutScreen
                            //this.minecraft.setScreen(new Screen_Test());
                            // this.minecraft.setScreen(new a1111_Screen());
                            this.minecraft.setScreen(new EconomyMainScreen());
                        })
        );

        // 初始化渲染缓存（在所有按钮添加后调用）
        initializeRenderCache();
    }

    @Override
    protected void initializeRenderCache() {
        renderCache.clear();

        Component balanceText;
        if (balance == -1) {
            // 获取本地化的 "Fetching balance..." 文本
            balanceText = Component.translatable(Util_MessageKeys.HOME_FETCHING_BALANCE_TEXT_KEY);
        } else {
            // 获取本地化的 "Your balance: %s coins" 文本，并替换占位符
            balanceText = Component.translatable(Util_MessageKeys.HOME_BALANCE_TEXT_KEY, balance);
        }
        // 计算文本居中位置
        int textWidth = this.font.width(balanceText);
        int xPosition = startX + textWidth / 2;

        balanceAnimation = new TextAnimation(
                -200,
                startY - 20,
                xPosition,
                startY - 20,
                0f,
                1f,
                1000
        );

        renderCache.add((guiGraphics) -> {
            // 渲染标题（带渐入和左滑效果）
            renderAnimatedText(
                    guiGraphics,
                    Component.translatable(Util_MessageKeys.HOME_BALANCE_TEXT_KEY, balance).withStyle(ChatFormatting.GOLD),
                    balanceAnimation
            );
        });

        // 新增步骤1：计算所有需要渲染的文本（前10条 + 自己）
        List<Component> allEntries = new ArrayList<>();

        int baseX = 0;
        // 添加前10条账户数据
        if (accounts != null) {
            int count = 0;
            for (Map.Entry<String, Integer> entry : accounts) {
                if (count >= 10) break;
                allEntries.add(createAccountComponent(entry.getKey(), entry.getValue(), count + 1));
                count++;
            }

            // 添加自己的条目（无论是否在前10）
            int selfIndex = getIndexOfPlayer(accounts, this.minecraft.player.getName().getString()) + 1;
            if (selfIndex != 0) { // 确保自己存在于列表中
                allEntries.add(Component.literal("[" + selfIndex + "] 你 拥有 " + balance + " 枚梦鱼币"));
            }

            // 新增步骤2：计算最大文本宽度
            int maxWidth = allEntries.stream()
                    .mapToInt(component -> this.font.width(component))
                    .max()
                    .orElse(100); // 默认值防止空列表

            // 新增步骤3：计算基准X坐标（右对齐位置）
            baseX = maxWidth;
        }



        // 渲染玩家账户列表
        int y = this.startY - 20;
        int index = -1;
        if (accounts != null) {
            int i = 1;
            for (Map.Entry<String, Integer> entry : accounts) {
                if (i > 10) {
                    break;
                }
                TextAnimation richList;

                richList = new TextAnimation(
                        this.width + 200,
                        y,
                        this.width - startX - baseX,
                        y,
                        0f,
                        1f,
                        1000
                );

                String playerName = entry.getKey();
                Integer playerBalance = entry.getValue();

                // 拼接文本 "玩家名称: 余额"
                Component account = Component.literal("[" + i + "] " + playerName + " 拥有 " + playerBalance + " 枚梦鱼币");

                renderCache.add((guiGraphics) -> {
                    // 渲染标题（带渐入和左滑效果）
                    renderAnimatedText(
                            guiGraphics,
                            account,
                            richList
                    );
                });

                // 增加 y 坐标，确保下一行文本显示在下方
                y += this.font.lineHeight + 2;  // 增加行高和一些间距
                i++;
            }
            index = getIndexOfPlayer(accounts, this.minecraft.player.getName().getString()) + 1;
        }

        if (index != -1) {
            TextAnimation myself;

            Component account = Component.literal("[" + index + "] 你 拥有 " + balance + " 枚梦鱼币");
            // 将富豪榜文本的 y 坐标设置为最后一个账户条目下方
            int leaderboardY = y + 10;  // 在最后一行账户文本之后加一点间隔

            myself = new TextAnimation(
                    this.width + 200,
                    leaderboardY,
                    this.width - startX - baseX,
                    leaderboardY,
                    0f,
                    1f,
                    1000
            );

            renderCache.add((guiGraphics) -> {
                // 渲染标题（带渐入和左滑效果）
                renderAnimatedText(
                        guiGraphics,
                        account,
                        myself
                );
            });
        }
    }

    /**
     * 渲染屏幕内容，包括背景、按钮、玩家余额和账户列表。
     *
     * @param guiGraphics GUI 图形对象
     * @param mouseX      鼠标 X 坐标
     * @param mouseY      鼠标 Y 坐标
     * @param partialTicks 部分刻数
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 绘制背景
        this.renderBackground(guiGraphics);

        // 执行渲染缓存中的任务
        for (RunnableWithGraphics task : renderCache) {
            task.run(guiGraphics);
        }

        // 渲染父类和按钮
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    /**
     * 更新玩家余额和账户列表的方法，供数据包调用。
     *
     * @param balance 玩家余额
     * @param accounts 玩家账户列表
     */
    public void updateBalance(int balance, List<Map.Entry<String, Integer>> accounts) {
        this.balance = balance;
        this.accounts = accounts;
        initializeRenderCache();
    }

    /**
     * 获取指定玩家名称在账户列表中的索引。
     *
     * @param accounts 账户列表
     * @param targetName 目标玩家名称
     * @return 索引，如果未找到则返回 -1
     */
    public static int getIndexOfPlayer(List<Map.Entry<String, Integer>> accounts, String targetName) {
        for (int i = 0; i < accounts.size(); i++) {
            Map.Entry<String, Integer> entry = accounts.get(i);
            if (entry.getKey().equals(targetName)) {
                return i;  // 返回索引
            }
        }
        return -1;  // 如果没有找到，返回 -1
    }

    @Override
    protected void initPosition(){
        TOP_MARGIN = this.height - 100;
        thingsPerPage = Math.max(1, TOP_MARGIN / THING_SPACING);

        startX = Math.max((this.width / 2) - 300, 60);
        startY = Math.max((this.height) / 4, 40);
    }

    // 辅助方法：生成带排名的条目文本
    private Component createAccountComponent(String name, int balance, int rank) {
        return Component.literal("[" + rank + "] " + name + " 拥有 " + balance + " 枚梦鱼币");
    }


    @Override
    protected void renderAnimatedText(GuiGraphics guiGraphics, Component text, TextAnimation animation) {
        // 计算当前属性
        int x = animation.getCurrentX();
        int y = animation.getCurrentY();
        float alpha = animation.getCurrentAlpha();

        // 设置透明度（ARGB格式：0xAARRGGBB）
        int color = 0xFFFFFF | ((int) (alpha * 255) << 24);

        // 文字居中绘制
        int textWidth = minecraft.font.width(text);
        guiGraphics.drawString(
                minecraft.font,
                text,
                x, // 居中计算
                y,
                color,
                true // 启用阴影
        );
    }
}

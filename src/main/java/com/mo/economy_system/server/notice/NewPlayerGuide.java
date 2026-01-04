package com.mo.economy_system.server.notice;

import com.mo.economy_system.server.serverui.tips.TipPushHelper;
import net.minecraft.server.level.ServerPlayer;

/**
 * 新手教程（无调度器 + 无复杂方法 + 逐条显示 + 每条10秒 + 不堆叠）
 */
public class NewPlayerGuide {
    // 固定10秒时长（显示时间） & 10秒延迟（推送间隔，解决堆叠）
    private static final long SECONDS = 20000;

    public static void sendNewPlayerGuide(ServerPlayer player) {
        if (player == null) return;

        TipPushHelper.sendTipToPlayer(player, "§6欢迎来到§bDreaming§dfish§6服务器\n按下u键可以打开服务器菜单", (int) SECONDS);

        //开启简单子线程，实现延迟推送
        new Thread(() -> {
            try {
                Thread.sleep(SECONDS);
                if (player.isAlive() && !player.isRemoved()) {
                    TipPushHelper.sendTipToPlayer(player, "\"§bDreaming§dfish\"§6栏会显示您当前的游戏档案\n即您在服务器的等级，游玩时间等更多信息", (int) SECONDS);
                }

                Thread.sleep(SECONDS);
                if (player.isAlive() && !player.isRemoved()) {
                    TipPushHelper.sendTipToPlayer(player, "§b\"故事\"§6是所有玩家需要共同推进的故事进度\n故事进度推进，服务器剧情得以发展", (int) SECONDS);
                }

                Thread.sleep(SECONDS);
                if (player.isAlive() && !player.isRemoved()) {
                    TipPushHelper.sendTipToPlayer(player, "§6原版血量ui和饥饿ui已被移除\n右侧的小人越鲜艳，代表你的血量越多\n左侧的绿色条是您的体力值，米色的是您的饥饿值", (int) SECONDS);
                }

                Thread.sleep(SECONDS);
                if (player.isAlive() && !player.isRemoved()) {
                    TipPushHelper.sendTipToPlayer(player, "§6升级等级可以提高您的血量，体力等属性上限", (int) SECONDS);
                }

                Thread.sleep(SECONDS);
                if (player.isAlive() && !player.isRemoved()) {
                    TipPushHelper.sendTipToPlayer(player, "§6RANK和称号可以通过服务器特殊活动获取", (int) SECONDS);
                }

                Thread.sleep(SECONDS);
                if (player.isAlive() && !player.isRemoved()) {
                    TipPushHelper.sendTipToPlayer(player, "§6加油生存下去吧！萌新鱼友", (int) SECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start(); // 启动子线程，执行延迟推送
    }
}
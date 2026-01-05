package com.mo.economy_system.server.notice;

import com.mo.economy_system.server.serverui.tips.TipPushHelper;
import net.minecraft.server.level.ServerPlayer;

/**
 * 新手教程（无调度器 + 无复杂方法 + 逐条显示 + 每条10秒 + 不堆叠）
 */
public class NewPlayerGuide {
    // 固定10秒时长（显示时间） & 10秒延迟（推送间隔，解决堆叠）
    private static final long SECONDS = 15000;

    public static void sendNewPlayerGuide(ServerPlayer player) {
        if (player == null) return;

        TipPushHelper.sendTipToPlayer(player, "§6欢迎来到§bDreaming§dfish§6服务器\n按下u键可以打开服务器菜单，I键可以打开商店", (int) SECONDS);

        //开启简单子线程，实现延迟推送
        new Thread(() -> {
            try {
                Thread.sleep(SECONDS);
                if (player.isAlive() && !player.isRemoved()) {
                    TipPushHelper.sendTipToPlayer(player, "\"§bDreaming§dfish\"§6栏会显示您当前的游戏档案\n即您在服务器的§a等级，游玩时间§6等更多信息", (int) SECONDS);
                }

                Thread.sleep(SECONDS);
                if (player.isAlive() && !player.isRemoved()) {
                    TipPushHelper.sendTipToPlayer(player, "§b\"故事\"§6是§c所有玩家§6需要共同推进的故事进度\n故事进度推进，服务器剧情得以发展", (int) SECONDS);
                }

                Thread.sleep(SECONDS);
                if (player.isAlive() && !player.isRemoved()) {
                    TipPushHelper.sendTipToPlayer(player, "§6原版血量ui和饥饿ui已被移除\n下侧的§a小人§6越鲜艳，代表你的§c血量§6越多\n§a绿色条§6是您的§a体力值，§e米色§6的是您的§e饥饿值，§d紫色§6是您的§d勇气值", (int) SECONDS);
                }

                Thread.sleep(SECONDS);
                if (player.isAlive() && !player.isRemoved()) {
                    TipPushHelper.sendTipToPlayer(player, "§6疾跑，跳跃会§c消耗§6您的体力，在夜晚您的§d勇气值§6会逐渐降低\n当您停下疾跑后，需要§a休息一段时间§6才能恢复体力并重新奔跑\n当您附近有§a4名及以上的玩家或者处于白天时，§6您的勇气值会逐渐恢复\n短时间§a击杀多个野怪§6可以快速提高您的勇气值", (int) SECONDS);
                }

                Thread.sleep(SECONDS);
                if (player.isAlive() && !player.isRemoved()) {
                    TipPushHelper.sendTipToPlayer(player, "§6勇气值过低会使您手脚不便，当然，勇气值高您会获得增益", (int) SECONDS);
                }

                Thread.sleep(SECONDS);
                if (player.isAlive() && !player.isRemoved()) {
                    TipPushHelper.sendTipToPlayer(player, "§6升级等级可以提高您的§a血量，体力§6等属性上限", (int) SECONDS);
                }

                Thread.sleep(SECONDS);
                if (player.isAlive() && !player.isRemoved()) {
                    TipPushHelper.sendTipToPlayer(player, "§6RANK和称号可以通过服务器§a特殊活动§6获取", (int) SECONDS);
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
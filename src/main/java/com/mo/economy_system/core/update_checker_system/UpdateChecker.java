package com.mo.economy_system.core.update_checker_system;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.platform.EconomyServices;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 更新检查器类，用于检查模组是否有新版本可用
 */
public class UpdateChecker {
    // GitHub API 地址，用于获取最新版本信息
    private static final String GITHUB_API_URL = "https://api.github.com/repos/QingMo-A/EconoeySystem/releases/latest";
    // private static final String CURRENT_VERSION = "1.0.0"; // 当前模组版本

    /**
     * 检查是否有新版本可用，并通知玩家
     * 此方法在新线程中执行，以避免阻塞主线程
     *
     * @param player 服务器玩家，用于接收更新通知
     */
    public static void checkForUpdates(ServerPlayer player) {
        new Thread(() -> {
            try {
                // 获取当前版本号
                String currentVersion = "v" + EconomyServices.platform().modVersion(EconomySystem.MODID);

                // 创建连接
                HttpURLConnection connection = (HttpURLConnection) new URL(GITHUB_API_URL).openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Minecraft-Mod"); // 设置 User-Agent 避免 GitHub 拒绝请求

                // 检查响应码
                if (connection.getResponseCode() != 200) {
                    player.sendSystemMessage(Component.literal("§b[§6EconomySystem§b] §c无法检查更新，请稍后重试！"));
                    return;
                }

                // 解析 JSON 响应
                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                String latestVersion = json.get("tag_name").getAsString(); // 获取最新版本号
                String downloadUrl = json.get("html_url").getAsString(); // 获取版本下载地址

                // 比较版本号
                if (!currentVersion.equals(latestVersion)) {
                    Component copyDownloadURL = Component.literal("[下载链接]")
                            .withStyle(style -> style
                                    .withColor(0x55FF55)
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, downloadUrl))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§e点击复制!"))));

                    player.sendSystemMessage(Component.literal("§b[§6EconomySystem§b] §a发现新版本：§r" + latestVersion)
                            .append(" ")
                            .append(copyDownloadURL)
                    );
                } else {
                    player.sendSystemMessage(Component.literal("§b[§6EconomySystem§b] §a当前已是最新版本！"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                player.sendSystemMessage(Component.literal("§b[§6EconomySystem§b] §c检查更新时出错！"));
            }
        }).start();
    }
}

package com.mo.economy_system.network.packets.check_system;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraftforge.network.NetworkEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class Packet_CheckResultResponse {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final String playerName;
    private final String playerUUID;
    private final String senderName;
    private final String senderUUID;
    private final String actionType;
    private final String result;

    public Packet_CheckResultResponse(String playerName, String playerUUID, String senderName, String senderUUID, String actionType, String result) {
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.senderName = senderName;
        this.senderUUID = senderUUID;
        this.actionType = actionType;
        this.result = result;
    }

    public static void encode(Packet_CheckResultResponse msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.playerName);
        buf.writeUtf(msg.playerUUID);
        buf.writeUtf(msg.senderName);
        buf.writeUtf(msg.senderUUID);
        buf.writeUtf(msg.actionType);
        buf.writeUtf(msg.result);
    }

    public static Packet_CheckResultResponse decode(FriendlyByteBuf buf) {
        return new Packet_CheckResultResponse(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf());
    }

    public static void handle(Packet_CheckResultResponse msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            executor.execute(() -> {
                // ----------------------------------------
                // 1. 在客户端提示
                // ----------------------------------------
                Minecraft mc = Minecraft.getInstance();
                LocalPlayer localPlayer = mc.player;
                if (localPlayer != null) {
                    localPlayer.sendSystemMessage(Component.literal(
                            "收到来自玩家 " + msg.playerName + " 的检查结果！"
                    ));
                }

                // ----------------------------------------
                // 2. 将远程JSON结果保存到本地文件 (可选)
                // ----------------------------------------
                File gameDir = mc.gameDirectory;
                File destFile = new File(gameDir, msg.playerName + "_check_result.json");
                try (FileWriter writer = new FileWriter(destFile, StandardCharsets.UTF_8)) {
                    writer.write(msg.result);
                    if (localPlayer != null) {
                        localPlayer.sendSystemMessage(Component.literal(
                                "已保存 " + msg.playerName + " 的检查结果到: " + destFile.getAbsolutePath()
                        ));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                /*File gameDir = mc.gameDirectory;
                File destFile = new File(gameDir, msg.playerName + "_check_result.json");

                try (FileWriter writer = new FileWriter(destFile, StandardCharsets.UTF_8)) {
                    writer.write(msg.result);

                    if (localPlayer != null) {
                        // 创建消息组件
                        Component filePathMessage = Component.literal("已保存 " + msg.playerName + " 的检查结果到: ");

                        // 创建可点击的文件路径组件
                        Component clickableFilePath = Component.literal(destFile.getAbsolutePath())
                                .withStyle(Style.EMPTY
                                        .withColor(0x55FF55) // 绿色
                                        .withUnderlined(true)
                                        .withHoverEvent(new HoverEvent(
                                                HoverEvent.Action.SHOW_TEXT,
                                                Component.literal("点击打开文件")
                                        ))
                                        .withClickEvent(new ClickEvent(
                                                ClickEvent.Action.RUN_COMMAND,
                                                "/economy_system:open_file \"" + destFile.getAbsolutePath().replace("\\", "\\\\") + "\""
                                        ))
                                );

                        // 添加打开文件按钮
                        Component openFileButton = Component.literal(" [打开文件]")
                                .withStyle(Style.EMPTY
                                        .withColor(0xAAAAFF) // 浅蓝色
                                        .withHoverEvent(new HoverEvent(
                                                HoverEvent.Action.SHOW_TEXT,
                                                Component.literal("点击用系统默认程序打开文件")
                                        ))
                                        .withClickEvent(new ClickEvent(
                                                ClickEvent.Action.RUN_COMMAND,
                                                "/economy_system:open_file_system \"" + destFile.getAbsolutePath().replace("\\", "\\\\") + "\""
                                        ))
                                );

                        // 发送组合消息
                        localPlayer.sendSystemMessage(Component.literal(
                                "已保存 " + msg.playerName + " 的检查结果到: " + destFile.getAbsolutePath()
                        ).append(clickableFilePath).append(openFileButton));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (localPlayer != null) {
                        localPlayer.sendSystemMessage(Component.literal("§c保存文件失败: " + e.getMessage()));
                    }
                }*/

                // ----------------------------------------
                // 3. 解析远程JSON，获取远程文件映射
                // ----------------------------------------
                // 远程JSON示例：{
                //   "PlayerName": "xxx",
                //   "PlayerUUID": "xxx",
                //   "mods": {"A.jar": "...", "B.jar": "..."}
                // }
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                // 使用通用类型解析 JSON -> Map<String, Object>
                Map<String, Object> remoteData = gson.fromJson(msg.result, new TypeToken<Map<String, Object>>(){}.getType());

                // 取出远程 actionType 对应的 Map (如 "mods","shaderpacks","resourcepacks")
                // 因为 remoteData.get(msg.actionType) 应该是一个 Map<String,String> 的结构
                @SuppressWarnings("unchecked")
                Map<String, String> remoteFileHashes = (Map<String, String>) remoteData.getOrDefault(msg.actionType, new LinkedHashMap<>());

                // ----------------------------------------
                // 4. 获取本地文件映射
                // ----------------------------------------
                File targetFolder;
                switch (msg.actionType.toLowerCase()) {
                    case "mods":
                        targetFolder = new File(gameDir, "mods");
                        break;
                    case "shaderpacks":
                        targetFolder = new File(gameDir, "shaderpacks");
                        break;
                    case "resourcepacks":
                        targetFolder = new File(gameDir, "resourcepacks");
                        break;
                    default:
                        targetFolder = new File(gameDir, "");
                        break;
                }

                Map<String, String> localFileHashes = new LinkedHashMap<>();
                if (targetFolder.exists() && targetFolder.isDirectory()) {
                    File[] fileList = targetFolder.listFiles();
                    if (fileList != null) {
                        for (File file : fileList) {
                            if (file.isFile()) {
                                try {
                                    String hash = computeSHA256(file.toPath());
                                    localFileHashes.put(file.getName(), hash);
                                } catch (IOException | NoSuchAlgorithmException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }

                // 比较远程与本地
                boolean hasDifference = false;

                // 先把所有文件名收集进一个集合
                Set<String> allFiles = new HashSet<>();
                allFiles.addAll(remoteFileHashes.keySet());
                allFiles.addAll(localFileHashes.keySet());

                for (String fileName : allFiles) {
                    String remoteHash = remoteFileHashes.get(fileName);
                    String localHash = localFileHashes.get(fileName);

                    if (remoteHash == null && localHash != null) {
                        // 本地存在, 远程没有
                        hasDifference = true;
                        sendDiffMessage(localPlayer, "[+]", fileName, msg);
                    } else if (remoteHash != null && localHash == null) {
                        // 远程存在, 本地没有
                        hasDifference = true;
                        sendDiffMessage(localPlayer, "[-]", fileName, msg);
                    } else if (!Objects.equals(remoteHash, localHash)) {
                        // 都存在, 但哈希不同
                        hasDifference = true;
                        sendDiffMessage(localPlayer, "[!]", fileName, msg);
                    }
                }

                if (!hasDifference) {
                    localPlayer.sendSystemMessage(Component.literal("与 " + msg.playerName + " 的文件对比：无差异"));
                }
            });
        });
        contextSupplier.get().setPacketHandled(true);
    }

    private static void sendDiffMessage(LocalPlayer localPlayer, String diffType, String fileName, Packet_CheckResultResponse msg) {
        // 拼出命令: /get <playerName> <fileName> <actionType>
        String command = "/get " + msg.playerName + " \"" + fileName + "\" " + msg.actionType;

        // 创建可点击的 [获取] 按钮
        Component getButton = Component.literal("[获取]")
                .withStyle(style -> style
                        .withColor(0x55FF55)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击获取该文件")))
                );

        // 最终组合消息: [!] fileName [获取]
        Component diffLine = Component.literal(diffType + " " + fileName + " ")
                .append(getButton);

        // 发送到聊天
        localPlayer.sendSystemMessage(diffLine);
    }

    private static String computeSHA256(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(path);
        byte[] hashBytes = digest.digest(fileBytes);
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}

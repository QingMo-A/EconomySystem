package com.mo.economy_system.network.packets.check_system;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import com.mo.economy_system.compat.network.NetworkEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class Packet_Get {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final String playerName;
    private final String playerUUID;
    private final String senderName;
    private final String senderUUID;
    private final String actionType;
    private final String fileName;

    public Packet_Get(String playerName, String playerUUID, String senderName, String senderUUID, String actionType, String fileName) {
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.senderName = senderName;
        this.senderUUID = senderUUID;
        this.actionType = actionType;
        this.fileName = fileName;
    }

    public static void encode(Packet_Get msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.playerName);
        buf.writeUtf(msg.playerUUID);
        buf.writeUtf(msg.senderName);
        buf.writeUtf(msg.senderUUID);
        buf.writeUtf(msg.actionType);
        buf.writeUtf(msg.fileName);
    }

    public static Packet_Get decode(FriendlyByteBuf buf) {
        return new Packet_Get(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf());
    }

    public static void handle(Packet_Get msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            executor.execute(() -> {
                Minecraft mc = Minecraft.getInstance();
                File gameDir = mc.gameDirectory;
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

                // 构造一个 file 对象
                File matchedFile = new File(targetFolder, msg.fileName);
                if (!matchedFile.exists() || !matchedFile.isFile()) {
                    EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_GetResultRequest(msg.playerName, msg.playerUUID, msg.senderName, msg.senderUUID, msg.actionType, msg.fileName, "Not Found"));
                    // 你可以考虑发送一个错误提示包回服务器
                    return;
                }

                // 读取文件并进行 Base64 编码
                String base64Content;
                try {
                    byte[] fileBytes = Files.readAllBytes(matchedFile.toPath());
                    base64Content = Base64.getEncoder().encodeToString(fileBytes);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                int chunkSize = 30000;
                String uuid = UUID.randomUUID().toString();

                if (base64Content.length() <= 30000) {
                    EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_GetResultRequest(msg.playerName, msg.playerUUID, msg.senderName, msg.senderUUID, msg.actionType, msg.fileName, base64Content));
                } else {
                    int totalChunks = (base64Content.length() + chunkSize - 1) / chunkSize;
                    for (int i = 0; i < totalChunks; i++) {
                        int start = i * chunkSize;
                        int end = Math.min(start + chunkSize, base64Content.length());
                        String chunkData = base64Content.substring(start, end);

                        // 发送 ChunkPacket
                        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_Chunk(msg.playerName, msg.playerUUID, msg.senderName, msg.senderUUID, msg.actionType, msg.fileName, uuid, i, totalChunks, chunkData));
                    }
                }
            });
        });
        contextSupplier.get().setPacketHandled(true);
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

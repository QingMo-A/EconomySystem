package com.mo.economy_system.network.packets.check_system;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Packet_Check implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_Check> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "check_system/packet_check"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_Check> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_Check.encode(packet, buf), Packet_Check::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final String playerName;
    private final String playerUUID;
    private final String senderName;
    private final String senderUUID;
    private final String actionType;

    public Packet_Check(String playerName, String playerUUID, String senderName, String senderUUID, String actionType) {
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.senderName = senderName;
        this.senderUUID = senderUUID;
        this.actionType = actionType;
    }

    public static void encode(Packet_Check msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.playerName);
        buf.writeUtf(msg.playerUUID);
        buf.writeUtf(msg.senderName);
        buf.writeUtf(msg.senderUUID);
        buf.writeUtf(msg.actionType);
    }

    public static Packet_Check decode(FriendlyByteBuf buf) {
        return new Packet_Check(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf());
    }

    public static void handle(Packet_Check msg, IPayloadContext context) {
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

                Map<String, Object> jsonData = new LinkedHashMap<>();
                jsonData.put("PlayerName", msg.playerName);
                jsonData.put("PlayerUUID", msg.playerUUID);

                Map<String, String> fileHashes = new LinkedHashMap<>();
                // 把文件哈希值也用 LinkedHashMap（如果你想按文件名遍历顺序来保持顺序）
                if (targetFolder.exists() && targetFolder.isDirectory()) {
                    File[] fileList = targetFolder.listFiles();
                    if (fileList != null) {
                        for (File file : fileList) {
                            if (file.isFile()) {
                                try {
                                    String hash = computeSHA256(file.toPath());
                                    fileHashes.put(file.getName(), hash);
                                } catch (IOException | NoSuchAlgorithmException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
                jsonData.put(msg.actionType, fileHashes); // 例如 "mods"

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String jsonContent = gson.toJson(jsonData);

                EconomySystem_NetworkManager.sendToServer(new Packet_CheckResultRequest(msg.playerName, msg.playerUUID, msg.senderName, msg.senderUUID, msg.actionType, jsonContent));

            });
        });
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

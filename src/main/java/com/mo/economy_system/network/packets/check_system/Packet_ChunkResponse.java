package com.mo.economy_system.network.packets.check_system;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ChunkPacket 用于分块传输大文件或大字符串。
 * 其中包含：
 * - fileId: 用于标识同一个文件/传输的唯一 ID
 * - chunkIndex: 当前是第几块
 * - totalChunks: 总块数
 * - chunkData: 当前块的 Base64 文本或其他字符串数据
 */
public class Packet_ChunkResponse implements net.minecraft.network.protocol.common.custom.CustomPacketPayload, EconomyNetworkMessage {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_ChunkResponse> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "check_system/packet_chunk_response"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_ChunkResponse> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_ChunkResponse.encode(packet, buf), Packet_ChunkResponse::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    /**
     * 用于缓存所有正在接收的分块。
     * key = fileId (String)
     * value = 一个临时对象，用来存储所有 chunkData，直到接收完成。
     */
    public static final Map<String, ChunkAccumulator> ACCUMULATOR_MAP = new ConcurrentHashMap<>();

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final String playerName;
    private final String playerUUID;
    private final String senderName;
    private final String senderUUID;
    private final String actionType;
    private final String fileName;

    private final String fileId;
    private final int chunkIndex;
    private final int totalChunks;
    private final String chunkData;

    public Packet_ChunkResponse(String playerName, String playerUUID, String senderName, String senderUUID, String actionType, String fileName, String fileId, int chunkIndex, int totalChunks, String chunkData) {
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.senderName = senderName;
        this.senderUUID = senderUUID;
        this.actionType = actionType;
        this.fileName = fileName;
        this.fileId = fileId;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.chunkData = chunkData;
    }

    // 编码
    public static void encode(Packet_ChunkResponse msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.playerName);
        buf.writeUtf(msg.playerUUID);
        buf.writeUtf(msg.senderName);
        buf.writeUtf(msg.senderUUID);
        buf.writeUtf(msg.actionType);
        buf.writeUtf(msg.fileName);
        buf.writeUtf(msg.fileId);
        buf.writeInt(msg.chunkIndex);
        buf.writeInt(msg.totalChunks);
        buf.writeUtf(msg.chunkData);
    }

    // 解码
    public static Packet_ChunkResponse decode(FriendlyByteBuf buf) {
        String playerName = buf.readUtf();
        String playerUUID = buf.readUtf();
        String senderName = buf.readUtf();
        String senderUUID = buf.readUtf();
        String actionType = buf.readUtf();
        String fileName = buf.readUtf();
        String fileId = buf.readUtf();
        int idx = buf.readInt();
        int total = buf.readInt();
        String data = buf.readUtf();
        return new Packet_ChunkResponse(playerName, playerUUID, senderName, senderUUID, actionType, fileName, fileId, idx, total, data);
    }

    // 处理
    public static void handle(Packet_ChunkResponse msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer localPlayer = mc.player;

            // 1. 找到/创建一个 ChunkAccumulator
            ChunkAccumulator accumulator = ACCUMULATOR_MAP.computeIfAbsent(
                    msg.fileId,
                    key -> new ChunkAccumulator(msg.totalChunks)
            );

            // 2. 存储当前 chunkData
            accumulator.addChunk(msg.chunkIndex, msg.chunkData);

            // 3. 检查是否全部接收完
            if (accumulator.isComplete()) {
                // 3a. 拼接所有分块
                String fullData = accumulator.buildFullData();

                // 否则 base64 不为 "NotFound"，说明文件存在
                if (localPlayer != null) {
                    localPlayer.sendSystemMessage(Component.literal("收到来自玩家 " + msg.playerName + " 的文件！"));
                }

                // 在本地将 base64 解码为二进制文件
                try {
                    byte[] fileBytes = Base64.getDecoder().decode(fullData);

                    // 你可以自行决定存储的文件名和扩展名。
                    // 比如说在同目录下，把 actionType 作为文件夹或前缀
                    File gameDir = mc.gameDirectory;
                    File outputFile = new File(gameDir, msg.fileName);

                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        fos.write(fileBytes);
                    }

                    if (localPlayer != null) {
                        localPlayer.sendSystemMessage(Component.literal("已保存文件为 " + outputFile.getAbsolutePath()));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }


                // 在此处理 fullData (例如 Base64 解码，写入文件)
                // ...
                // 处理完成后，你可以清理缓存
                ACCUMULATOR_MAP.remove(msg.fileId);
            }
        });
    }

    /**
     * ChunkAccumulator 用于暂存多个分块。
     */
    public static class ChunkAccumulator {
        private final String[] chunks;
        private final int totalChunks;
        private int receivedCount = 0;

        public ChunkAccumulator(int totalChunks) {
            this.totalChunks = totalChunks;
            this.chunks = new String[totalChunks];
        }

        public void addChunk(int idx, String data) {
            // 注意判断 idx 是否越界
            if (idx >= 0 && idx < totalChunks) {
                // 如果该分块尚未收到
                if (chunks[idx] == null) {
                    chunks[idx] = data;
                    receivedCount++;
                }
            }
        }

        public boolean isComplete() {
            return receivedCount >= totalChunks;
        }

        public String buildFullData() {
            // 将所有分块按顺序拼接
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < totalChunks; i++) {
                if (chunks[i] != null) {
                    sb.append(chunks[i]);
                }
            }
            return sb.toString();
        }
    }
}

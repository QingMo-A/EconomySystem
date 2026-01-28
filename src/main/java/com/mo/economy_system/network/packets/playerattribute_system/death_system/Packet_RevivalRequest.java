package com.mo.economy_system.network.packets.playerattribute_system.death_system;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesData;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesDataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraftforge.network.NetworkEvent;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 复活请求数据包
 * 客户端发送到服务端，请求复活指定玩家
 */
public class Packet_RevivalRequest {

    private final String playerName;

    public Packet_RevivalRequest(String playerName) {
        this.playerName = playerName;
    }

    /**
     * 编码
     */
    public static void encode(Packet_RevivalRequest packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.playerName);
    }

    /**
     * 解码
     */
    public static Packet_RevivalRequest decode(FriendlyByteBuf buf) {
        String playerName = buf.readUtf();
        return new Packet_RevivalRequest(playerName);
    }

    /**
     * 处理（服务端）
     */
    public static void handle(Packet_RevivalRequest packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) return;

            String targetName = packet.playerName;
            UserBanList banList = sender.server.getPlayerList().getBans();

            // 读取 banned-players.json 文件找到玩家的 UUID
            Path banFile = sender.server.getServerDirectory().toPath().resolve("banned-players.json");
            JsonObject foundEntry = null;

            try (FileReader reader = new FileReader(banFile.toFile())) {
                JsonArray bans = new Gson().fromJson(reader, JsonArray.class);
                if (bans != null) {
                    for (JsonElement element : bans) {
                        JsonObject ban = element.getAsJsonObject();
                        if (ban.has("name")) {
                            String bannedName = ban.get("name").getAsString();
                            if (bannedName.equalsIgnoreCase(targetName)) {
                                foundEntry = ban;
                                break;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                EconomySystem.LOGGER.error("读取封禁文件失败", e);
                sender.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c读取封禁数据失败！"));
                return;
            }

            // 检查是否找到封禁条目
            if (foundEntry == null) {
                sender.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c玩家 " + targetName + " 未被封禁！"));
                EconomySystem.LOGGER.info("玩家 {} 尝试复活未被 Ban 的玩家 {}",
                        sender.getScoreboardName(), targetName);
                return;
            }

            // 检查是否是死亡系统封禁的
            String source = foundEntry.has("source") ? foundEntry.get("source").getAsString() : "";
            if (!"DeathSystem".equals(source)) {
                // 不是死亡系统封禁的，无权解封
                sender.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§c该玩家不是因复活点耗尽被封禁，无法用复活护符复活！"
                ));
                EconomySystem.LOGGER.warn("玩家 {} 尝试用复活护符复活非死亡封禁的玩家 {}",
                        sender.getScoreboardName(), targetName);
                return;
            }

            // 获取 UUID 并创建 GameProfile
            try {
                String uuidStr = foundEntry.get("uuid").getAsString();
                UUID targetUuid = UUID.fromString(uuidStr);
                com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(targetUuid, targetName);

                // 使用 banList.remove() 同时更新内存和文件
                banList.remove(profile);

                // 传递感染状态 =====
                // 获取使用者的感染状态
                PlayerAttributesData senderData = PlayerAttributesDataManager.getPlayerAttributesData(sender.getUUID());
                boolean senderIsInfected = false;

                if (senderData != null) {
                    senderIsInfected = senderData.isInfected();
                    senderData.setRespawnPoint(senderData.getRespawnPoint() / 2);

                    // 获取被复活玩家的属性数据
                    PlayerAttributesData targetData = PlayerAttributesDataManager.getPlayerAttributesData(targetUuid);
                    if (targetData != null) {
                        // 设置被复活玩家的感染状态与使用者相同
                        targetData.setInfected(senderIsInfected);

                        // 如果是感染者，设置感染值为100,复活点数=100，勇气=50，体力恢复满
                        if (senderIsInfected) {
                            targetData.setCurrentInfection(100);
                            targetData.setRespawnPoint(100);
                            targetData.setCurrentCourage(50);
                            // 获取最大体力并恢复满
                            int maxStrength = targetData.getMaxStrength();
                            targetData.setCurrentStrength(maxStrength);
                        } else {
                            //如果是幸存者：感染值=0，复活点数=100，勇气=50，体力恢复满
                            targetData.setCurrentInfection(0);
                            targetData.setRespawnPoint(100);
                            targetData.setCurrentCourage(50);
                            // 获取最大体力并恢复满
                            int maxStrength = targetData.getMaxStrength();
                            targetData.setCurrentStrength(maxStrength);
                        }

                        // 保存数据
                        PlayerAttributesDataManager.saveSinglePlayerData(targetUuid, targetData);
                        PlayerAttributesDataManager.saveSinglePlayerData(sender.getUUID(), senderData);
                        EconomySystem.LOGGER.info("玩家 {} 的感染状态已设置为: {}（由 {} 传递）",
                                targetName, senderIsInfected ? "感染者" : "幸存者", sender.getScoreboardName());
                    }
                }

                // 发送消息
                String infectionStatus;
                if (senderIsInfected) {
                    infectionStatus = " §c(由于您是感染者，因此这名被复活的玩家也是感染者)";
                } else {
                    infectionStatus = " §a(由于您是幸存者，因此这名被复活的玩家的身份也为幸存者)";
                }

                sender.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§a您复活了玩家 " + targetName + "！" + infectionStatus + "。§c您丢失了一半的复活点数"
                ));

                EconomySystem.LOGGER.info("玩家 {} 使用复活护符复活了玩家 {}",
                        sender.getScoreboardName(), targetName);
            } catch (Exception e) {
                EconomySystem.LOGGER.error("解封玩家失败", e);
                sender.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c解封失败！"));
            }
        });
        context.setPacketHandled(true);
    }
}

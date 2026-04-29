package com.mo.economy_system.network.packets.territory_system;

import com.mo.economy_system.item.EconomySystem_Items;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class Packet_TeleportToTerritory implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_TeleportToTerritory> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "territory_system/packet_teleport_to_territory"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_TeleportToTerritory> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_TeleportToTerritory.encode(packet, buf), Packet_TeleportToTerritory::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private final UUID territoryID;

    public Packet_TeleportToTerritory(UUID territoryID) {
        this.territoryID = territoryID;
    }

    public static void encode(Packet_TeleportToTerritory msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.territoryID);
    }

    public static Packet_TeleportToTerritory decode(FriendlyByteBuf buf) {
        return new Packet_TeleportToTerritory(buf.readUUID());
    }

    public static void handle(Packet_TeleportToTerritory msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player == null) return;

            var territory = TerritoryManager.getTerritoryByID(msg.territoryID);
            if (territory == null) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.TELEPORT_TARGET_NOT_FOUND));
                return;
            }

            if (!territory.isOwner(player.getUUID()) && !territory.hasPermission(player.getUUID())) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.TELEPORT_NO_PERMISSION));
                return;
            }

            BlockPos backPoint = territory.getBackpoint();
            if (backPoint == null) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.TELEPORT_NO_BACKPOINT));
                return;
            }

            ResourceKey<Level> dimension = territory.getDimension(); // 获取目标领地所在的维度
            ServerLevel targetLevel = player.server.getLevel(dimension); // 根据维度获取目标服务器层级
            if (targetLevel == null) { // 如果目标服务器层级不存在
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.TELEPORT_DIMENSION_NOT_FOUND)); // 发送维度未找到的消息给玩家
                return; // 结束方法执行
            }

            // 检查玩家是否持有回忆药水
            var inventory = player.getInventory(); // 获取玩家的物品栏
            ItemStack potionStack = null; // 初始化药水堆栈为null
            for (ItemStack itemStack : inventory.items) { // 遍历玩家物品栏中的所有物品
                if (itemStack.getItem() == EconomySystem_Items.RECALL_POTION.get()) { // 如果找到回忆药水
                    potionStack = itemStack; // 将找到的药水堆栈赋值给potionStack
                    break; // 退出循环
                }
            }

            // 强制加载目标区块
            if (!targetLevel.isLoaded(backPoint)) { // 如果目标区块未加载
                targetLevel.getChunkSource().addRegionTicket( // 添加强制加载票，确保区块加载
                        net.minecraft.server.level.TicketType.POST_TELEPORT, // 票类型为传送后
                        new net.minecraft.world.level.ChunkPos(backPoint), // 目标区块位置
                        1, // 优先级
                        player.getId() // 玩家ID
                );
            }

            if (potionStack != null) {
                // 执行传送
                try {
                    player.teleportTo(
                            targetLevel,
                            backPoint.getX() + 0.5,
                            backPoint.getY() + 1,
                            backPoint.getZ() + 0.5,
                            player.getYRot(),
                            player.getXRot()
                    );

                    // 传送粒子效果
                    targetLevel.sendParticles(
                            ParticleTypes.PORTAL,
                            backPoint.getX() + 0.5, backPoint.getY() + 1, backPoint.getZ() + 0.5,
                            50, 1, 1, 1, 0.1
                    );

                    // 消耗物品
                    potionStack.shrink(1);

                    // 确保目标区块刷新
                    targetLevel.getChunkSource().updateChunkForced(
                            new net.minecraft.world.level.ChunkPos(backPoint),
                            true
                    );

                    player.sendSystemMessage(Component.translatable(Util_MessageKeys.TELEPORT_SUCCESS, territory.getName()));
                } catch (Exception e) {
                    player.sendSystemMessage(Component.translatable(Util_MessageKeys.TELEPORT_FAILED));
                    e.printStackTrace();
                }
                // 播放音效
                targetLevel.playSound(null, backPoint, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
            } else {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.TELEPORT_NO_POTION));
            }
        });
    }

}

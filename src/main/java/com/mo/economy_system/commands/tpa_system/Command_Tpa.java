package com.mo.economy_system.commands.tpa_system;

import com.mo.economy_system.item.EconomySystem_Items;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.utils.Util_MessageKeys;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber
public class Command_Tpa {

    private static final Map<UUID, TpaRequest> pendingRequests = new HashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpa")
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer sender = context.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(context, "target");

                            if (sender.getUUID().equals(target.getUUID())) {
                                context.getSource().sendFailure(Component.translatable(Util_MessageKeys.TPA_SELF_ERROR));
                                return 0;
                            }

                            if (!hasWormholePotion(sender)) {
                                context.getSource().sendFailure(Component.translatable(Util_MessageKeys.TPA_NO_POTION));
                                return 0;
                            }

                            sendTpaRequest(sender, target);
                            return 1;
                        })));

        dispatcher.register(Commands.literal("tpaccept")
                .executes(context -> {
                    ServerPlayer target = context.getSource().getPlayerOrException();
                    UUID senderUUID = getPendingRequestSender(target);

                    if (senderUUID == null) {
                        context.getSource().sendFailure(Component.translatable(Util_MessageKeys.TPA_NO_REQUEST));
                        return 0;
                    }

                    ServerPlayer sender = target.getServer().getPlayerList().getPlayer(senderUUID);
                    if (sender == null) {
                        context.getSource().sendFailure(Component.translatable(Util_MessageKeys.TPA_SENDER_OFFLINE));
                        return 0;
                    }

                    if (!hasWormholePotion(sender)) {
                        context.getSource().sendFailure(Component.translatable(Util_MessageKeys.TPA_SENDER_NO_POTION, sender.getName().getString()));
                        removePendingRequest(target.getUUID());
                        return 0;
                    }

                    // Consume the Wormhole Potion from the sender's inventory
                    consumeWormholePotion(sender);

                    // Perform the teleport with dimension handling
                    teleportPlayerToTarget(sender, target);

                    // Notify players
                    sender.sendSystemMessage(Component.translatable(Util_MessageKeys.TPA_TELEPORTED, target.getName().getString()));
                    target.sendSystemMessage(Component.translatable(Util_MessageKeys.TPA_ACCEPTED, sender.getName().getString()));

                    removePendingRequest(target.getUUID());
                    return 1;
                }));

        dispatcher.register(Commands.literal("tpdeny")
                .executes(context -> {
                    ServerPlayer target = context.getSource().getPlayerOrException();
                    UUID senderUUID = getPendingRequestSender(target);

                    if (senderUUID == null) {
                        context.getSource().sendFailure(Component.translatable(Util_MessageKeys.TPA_NO_REQUEST));
                        return 0;
                    }

                    ServerPlayer sender = target.getServer().getPlayerList().getPlayer(senderUUID);
                    if (sender != null) {
                        sender.sendSystemMessage(Component.translatable(Util_MessageKeys.TPA_DENIED, target.getName().getString()));
                    }

                    target.sendSystemMessage(Component.translatable(Util_MessageKeys.TPA_DENY));
                    removePendingRequest(target.getUUID());
                    return 1;
                }));
    }

    private static void sendTpaRequest(ServerPlayer sender, ServerPlayer target) {
        // 检查是否已有待处理请求
        if (pendingRequests.containsKey(target.getUUID())) {
            sender.sendSystemMessage(Component.literal(target.getName().getString() + " already has a pending TPA request."));
            return;
        }

        // 将请求添加到 Map
        pendingRequests.put(target.getUUID(), new TpaRequest(sender.getUUID(), System.currentTimeMillis()));

        // 通知发送者
        sender.sendSystemMessage(Component.translatable(Util_MessageKeys.TPA_REQUEST_SENT, target.getName().getString()));

        // 构建[同意]按钮
        Component acceptButton = Component.literal("[同意]")
                .withStyle(style -> style
                        .withColor(0x55FF55) // 绿色
                        .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/tpaccept"))
                        .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.translatable(Util_MessageKeys.TPA_ACCEPT))));

        // 构建[拒绝]按钮
        Component denyButton = Component.literal("[拒绝]")
                .withStyle(style -> style
                        .withColor(0xFF5555) // 红色
                        .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/tpdeny"))
                        .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.translatable(Util_MessageKeys.TPA_DENY))));

        // 向目标玩家发送消息
        target.sendSystemMessage(Component.translatable(Util_MessageKeys.TPA_REQUEST_SENT, sender.getName().getString())
                .append(" ")
                .append(acceptButton)
                .append(" ")
                .append(denyButton));
    }


    private static void teleportPlayerToTarget(ServerPlayer sender, ServerPlayer target) {
        ServerLevel targetLevel = target.serverLevel(); // 获取目标玩家所在的维度
        ServerLevel senderLevel = sender.serverLevel(); // 获取发送玩家所在的维度
        BlockPos targetBlockPos = target.getOnPos();

        // 强制加载目标区块
        if (!targetLevel.isLoaded(targetBlockPos)) { // 如果目标区块未加载
            targetLevel.getChunkSource().addRegionTicket( // 添加强制加载票，确保区块加载
                    net.minecraft.server.level.TicketType.POST_TELEPORT, // 票类型为传送后
                    new net.minecraft.world.level.ChunkPos(targetBlockPos), // 目标区块位置
                    1, // 优先级
                    sender.getId() // 玩家ID
            );
        }

        // 传送粒子效果
        targetLevel.sendParticles(
                ParticleTypes.PORTAL,
                targetBlockPos.getX() + 0.5, targetBlockPos.getY() + 1, targetBlockPos.getZ() + 0.5,
                50, 1, 1, 1, 0.1
        );

        // 执行传送
        try {
            sender.teleportTo(
                    targetLevel,
                    targetBlockPos.getX() + 0.5,
                    targetBlockPos.getY() + 1,
                    targetBlockPos.getZ() + 0.5,
                    sender.getYRot(),
                    sender.getXRot()
            );

            // 确保目标区块刷新
            targetLevel.getChunkSource().updateChunkForced(
                    new net.minecraft.world.level.ChunkPos(targetBlockPos),
                    true
            );

        } catch (Exception e) {
            e.printStackTrace();
        }

        // 播放音效
        targetLevel.playSound(null, targetBlockPos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }


    private static UUID getPendingRequestSender(ServerPlayer target) {
        TpaRequest request = pendingRequests.get(target.getUUID());
        return request != null ? request.senderUUID : null;
    }

    private static void removePendingRequest(UUID targetUUID) {
        pendingRequests.remove(targetUUID);
    }

    private static boolean hasWormholePotion(ServerPlayer player) {
        return player.getInventory().items.stream()
                .anyMatch(stack -> stack.getItem() == EconomySystem_Items.WORMHOLE_POTION.get());
    }

    private static void consumeWormholePotion(ServerPlayer player) {
        player.getInventory().items.stream()
                .filter(stack -> stack.getItem() == EconomySystem_Items.WORMHOLE_POTION.get())
                .findFirst()
                .ifPresent(stack -> stack.shrink(1));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (true) {
            long currentTime = System.currentTimeMillis();
            pendingRequests.entrySet().removeIf(entry -> {
                TpaRequest request = entry.getValue();
                if (currentTime - request.timestamp > 60000) { // 超时 60 秒
                    ServerPlayer target = getPlayerByUUID(request.targetUUID);
                    ServerPlayer sender = getPlayerByUUID(request.senderUUID);

                    if (target != null) {
                        target.sendSystemMessage(Component.translatable(Util_MessageKeys.TPA_TIMEOUT_TARGET, sender != null ? sender.getName().getString() : "unknown"));
                    }

                    if (sender != null) {
                        sender.sendSystemMessage(Component.translatable(Util_MessageKeys.TPA_TIMEOUT_SENDER, target != null ? target.getName().getString() : "unknown"));
                    }

                    return true; // 从 Map 中移除请求
                }
                return false;
            });
        }
    }

    private static ServerPlayer getPlayerByUUID(UUID uuid) {
        MinecraftServer server = EconomyServices.platform().currentServer();
        return server == null ? null : server.getPlayerList().getPlayer(uuid);
    }


    private static class TpaRequest {
        private final UUID senderUUID;
        private UUID targetUUID;
        private final long timestamp;

        public TpaRequest(UUID senderUUID, long timestamp) {
            this.senderUUID = senderUUID;
            this.timestamp = timestamp;
        }
    }
}

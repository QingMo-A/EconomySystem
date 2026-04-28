package com.mo.economy_system.commands.world_wrap_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.world_wrap_system.WorldWrapEntityMirrorManager;
import com.mo.economy_system.core.world_wrap_system.WorldWrapConfig;
import com.mo.economy_system.core.world_wrap_system.WorldWrapManager;
import com.mo.economy_system.core.world_wrap_system.WorldWrapTransformer;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = EconomySystem.MODID)
public class Command_WorldWrap {
    private static final String COMMAND_WORLD_WRAP = "worldwrap";
    private static final String COMMAND_STATUS = "status";
    private static final String COMMAND_RELOAD = "reload";
    private static final String COMMAND_ENABLE = "enable";
    private static final String COMMAND_DISABLE = "disable";
    private static final String COMMAND_PREVIEW = "preview";
    private static final String COMMAND_ENTITY = "entity";
    private static final String COMMAND_DEBUG = "debug";
    private static final String MSG_STATUS_FORMAT = "世界环绕: %s | 边界预览: %s | 实体镜像: %s | 区块对齐: %s | 预览半径: %d | 维度: %s | X: %.1f ~ %.1f | Z: %.1f ~ %.1f | 冷却: %d ticks";
    private static final String MSG_ENTITY_DEBUG_FORMAT = "实体镜像调试: active=%s | mirrorChunks=%d | scanned=%d | accepted=%d | candidates=%d | manualSpawn=%d | manualTeleport=%d | removed=%d";
    private static final String MSG_ENABLED = "已启用世界环绕";
    private static final String MSG_DISABLED = "已禁用世界环绕";
    private static final String MSG_PREVIEW_ENABLED = "已启用边界预览。边界外会显示对侧真实区块的镜像。";
    private static final String MSG_PREVIEW_DISABLED = "已关闭边界预览。世界环绕只保留服务端边界传送。";
    private static final String MSG_ENTITY_ENABLED = "已启用实体镜像。边界对侧实体会显示在玩家视野附近。";
    private static final String MSG_ENTITY_DISABLED = "已关闭实体镜像。边界对侧实体不再额外渲染。";
    private static final String MSG_RELOADED = "世界环绕配置已重载";
    private static final String STATUS_ENABLED = "启用";
    private static final String STATUS_DISABLED = "关闭";

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal(COMMAND_WORLD_WRAP)
                .then(Commands.literal(COMMAND_STATUS)
                        .executes(context -> status(context.getSource())))
                .then(Commands.literal(COMMAND_RELOAD)
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> reload(context.getSource())))
                .then(Commands.literal(COMMAND_ENABLE)
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> enable(context.getSource())))
                .then(Commands.literal(COMMAND_DISABLE)
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> disable(context.getSource())))
                .then(Commands.literal(COMMAND_PREVIEW)
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal(COMMAND_ENABLE)
                                .executes(context -> setPreview(context.getSource(), true)))
                        .then(Commands.literal(COMMAND_DISABLE)
                                .executes(context -> setPreview(context.getSource(), false))))
                .then(Commands.literal(COMMAND_ENTITY)
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal(COMMAND_ENABLE)
                                .executes(context -> setEntityMirror(context.getSource(), true)))
                        .then(Commands.literal(COMMAND_DISABLE)
                                .executes(context -> setEntityMirror(context.getSource(), false)))
                        .then(Commands.literal(COMMAND_DEBUG)
                                .executes(context -> entityDebug(context.getSource())))));
    }

    private static int status(CommandSourceStack source) {
        WorldWrapConfig.WorldWrapConfigData config = WorldWrapConfig.getConfig();
        String enabledText = config.isEnabled() ? STATUS_ENABLED : STATUS_DISABLED;
        String mirrorEnabledText = config.isClientChunkMirrorEnabled() ? STATUS_ENABLED : STATUS_DISABLED;
        String entityMirrorEnabledText = config.isEntityMirrorEnabled() ? STATUS_ENABLED : STATUS_DISABLED;
        String alignedText = new WorldWrapTransformer(config).isChunkAligned() ? STATUS_ENABLED : STATUS_DISABLED;
        source.sendSuccess(() -> Component.literal(String.format(
                MSG_STATUS_FORMAT,
                enabledText,
                mirrorEnabledText,
                entityMirrorEnabledText,
                alignedText,
                config.getClientChunkMirrorRadius(),
                config.getDimension(),
                config.getMinX(),
                config.getMaxX(),
                config.getMinZ(),
                config.getMaxZ(),
                config.getCooldownTicks()
        )), false);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        WorldWrapConfig.load();
        if (source.getServer() != null) {
            WorldWrapManager.syncConfigToAll(source.getServer());
        }
        source.sendSuccess(() -> Component.literal(MSG_RELOADED), true);
        return 1;
    }

    private static int enable(CommandSourceStack source) {
        WorldWrapConfig.setEnabled(true);
        if (source.getServer() != null) {
            WorldWrapManager.syncConfigToAll(source.getServer());
        }
        source.sendSuccess(() -> Component.literal(MSG_ENABLED), true);
        return 1;
    }

    private static int disable(CommandSourceStack source) {
        WorldWrapConfig.setEnabled(false);
        if (source.getServer() != null) {
            WorldWrapManager.syncConfigToAll(source.getServer());
        }
        source.sendSuccess(() -> Component.literal(MSG_DISABLED), true);
        return 1;
    }

    private static int setPreview(CommandSourceStack source, boolean enabled) {
        WorldWrapConfig.setClientChunkMirrorEnabled(enabled);
        if (source.getServer() != null) {
            WorldWrapManager.syncConfigToAll(source.getServer());
        }
        source.sendSuccess(() -> Component.literal(enabled ? MSG_PREVIEW_ENABLED : MSG_PREVIEW_DISABLED), true);
        return 1;
    }

    private static int setEntityMirror(CommandSourceStack source, boolean enabled) {
        WorldWrapConfig.setEntityMirrorEnabled(enabled);
        if (source.getServer() != null) {
            WorldWrapManager.syncConfigToAll(source.getServer());
        }
        source.sendSuccess(() -> Component.literal(enabled ? MSG_ENTITY_ENABLED : MSG_ENTITY_DISABLED), true);
        return 1;
    }

    private static int entityDebug(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        WorldWrapEntityMirrorManager.EntityMirrorDebugData debugData = WorldWrapEntityMirrorManager.getDebugData(player);
        source.sendSuccess(() -> Component.literal(String.format(
                MSG_ENTITY_DEBUG_FORMAT,
                debugData.active() ? STATUS_ENABLED : STATUS_DISABLED,
                debugData.mirrorChunkCount(),
                debugData.scannedEntityCount(),
                debugData.acceptedEntityCount(),
                debugData.mirroredEntityCount(),
                debugData.sentSpawnCount(),
                debugData.sentTeleportCount(),
                debugData.staleRemovedCount()
        )), false);
        return 1;
    }
}

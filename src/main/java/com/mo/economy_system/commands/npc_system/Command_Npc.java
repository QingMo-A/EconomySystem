package com.mo.economy_system.commands.npc_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.npc_system.NpcData;
import com.mo.economy_system.core.npc_system.NpcManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = EconomySystem.MODID)
public class Command_Npc {
    private static final String COMMAND_NPC = "npc";
    private static final String COMMAND_OPEN = "open";
    private static final String COMMAND_BIND = "bind";
    private static final String COMMAND_UNBIND = "unbind";
    private static final String COMMAND_RELOAD = "reload";
    private static final String COMMAND_LIST = "list";
    private static final String ARG_NPC_ID = "npcId";
    private static final String ARG_TARGET = "target";
    private static final String ARG_ENTITY = "entity";
    private static final String MSG_NPC_NOT_FOUND = "NPC不存在: ";
    private static final String MSG_NPC_OPENED = "已打开NPC对话: ";
    private static final String MSG_NPC_BOUND = "已绑定NPC到实体: ";
    private static final String MSG_NPC_UNBOUND = "已移除实体NPC绑定";
    private static final String MSG_NPC_RELOADED = "NPC配置已重载";
    private static final String MSG_NPC_LIST_EMPTY = "当前没有NPC配置";

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal(COMMAND_NPC)
                .then(Commands.literal(COMMAND_OPEN)
                        .then(Commands.argument(ARG_NPC_ID, IntegerArgumentType.integer(1))
                                .executes(context -> openSelf(context.getSource(), IntegerArgumentType.getInteger(context, ARG_NPC_ID)))
                                .then(Commands.argument(ARG_TARGET, EntityArgument.player())
                                        .requires(source -> source.hasPermission(2))
                                        .executes(context -> openTarget(
                                                EntityArgument.getPlayer(context, ARG_TARGET),
                                                IntegerArgumentType.getInteger(context, ARG_NPC_ID),
                                                context.getSource()
                                        )))))
                .then(Commands.literal(COMMAND_BIND)
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument(ARG_NPC_ID, IntegerArgumentType.integer(1))
                                .then(Commands.argument(ARG_ENTITY, EntityArgument.entity())
                                        .executes(context -> bindEntity(
                                                EntityArgument.getEntity(context, ARG_ENTITY),
                                                IntegerArgumentType.getInteger(context, ARG_NPC_ID),
                                                context.getSource()
                                        )))))
                .then(Commands.literal(COMMAND_UNBIND)
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument(ARG_ENTITY, EntityArgument.entity())
                                .executes(context -> unbindEntity(EntityArgument.getEntity(context, ARG_ENTITY), context.getSource()))))
                .then(Commands.literal(COMMAND_RELOAD)
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> reload(context.getSource())))
                .then(Commands.literal(COMMAND_LIST)
                        .executes(context -> list(context.getSource()))));
    }

    private static int openSelf(CommandSourceStack source, int npcId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return openTarget(source.getPlayerOrException(), npcId, source);
    }

    private static int openTarget(ServerPlayer player, int npcId, CommandSourceStack source) {
        if (!NpcManager.openNpcDialogue(player, npcId)) {
            source.sendFailure(Component.literal(MSG_NPC_NOT_FOUND + npcId));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(MSG_NPC_OPENED + npcId), false);
        return 1;
    }

    private static int bindEntity(Entity entity, int npcId, CommandSourceStack source) {
        if (NpcManager.getNpc(npcId).isEmpty()) {
            source.sendFailure(Component.literal(MSG_NPC_NOT_FOUND + npcId));
            return 0;
        }
        entity.getPersistentData().putInt(NpcManager.ENTITY_NPC_ID_TAG, npcId);
        source.sendSuccess(() -> Component.literal(MSG_NPC_BOUND + npcId), false);
        return 1;
    }

    private static int unbindEntity(Entity entity, CommandSourceStack source) {
        entity.getPersistentData().remove(NpcManager.ENTITY_NPC_ID_TAG);
        source.sendSuccess(() -> Component.literal(MSG_NPC_UNBOUND), false);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        NpcManager.load();
        source.sendSuccess(() -> Component.literal(MSG_NPC_RELOADED), true);
        return 1;
    }

    private static int list(CommandSourceStack source) {
        if (NpcManager.getAllNpcs().isEmpty()) {
            source.sendSuccess(() -> Component.literal(MSG_NPC_LIST_EMPTY), false);
            return 1;
        }
        for (NpcData npc : NpcManager.getAllNpcs()) {
            source.sendSuccess(() -> Component.literal("[" + npc.getNpcId() + "] " + npc.getNpcName() + " - " + npc.getNpcProfession()), false);
        }
        return 1;
    }
}

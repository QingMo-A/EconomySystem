//package com.mo.economy_system.core.auth_system;
//
//import com.mojang.brigadier.CommandDispatcher;
//import com.mojang.brigadier.arguments.StringArgumentType;
//import net.minecraft.commands.CommandSourceStack;
//import net.minecraft.commands.Commands;
//import net.minecraft.core.BlockPos;
//import net.minecraft.network.chat.Component;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.server.level.ServerPlayer;
//
//
//// DEPRECATED: Old login system
//// public class Command_Login {
//    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
//        dispatcher.register(Commands.literal("login")
//                .then(Commands.argument("password", StringArgumentType.string())
//                        .executes(context -> {
//                            ServerPlayer player = context.getSource().getPlayerOrException();
//                            String password = StringArgumentType.getString(context, "password");
//
//                            AuthSavedData data = AuthSavedData.getInstance(player.serverLevel());
//
//                            if (!data.isRegistered(player.getUUID())) {
//                                player.sendSystemMessage(Component.literal("§c你还没有注册！请使用 /register <密码> <确认密码> 注册。"));
//                                return 0;
//                            }
//
//                            if (data.isLoggedIn(player.getUUID())) {
//                                player.sendSystemMessage(Component.literal("§a你已经登录了！"));
//                                return 0;
//                            }
//
//                            if (data.login(player.getUUID(), password)) {
//                                player.sendSystemMessage(Component.literal("§a登录成功！欢迎回来！"));
//                                teleportToGround(player);
//                                return 1;
//                            } else {
//                                player.sendSystemMessage(Component.literal("§c密码错误！"));
//                                return 0;
//                            }
//                        })));
//    }
//
//    private static void teleportToGround(ServerPlayer player) {
//        ServerLevel level = player.serverLevel();
//        BlockPos pos = player.blockPosition();
//
//        int safeY = findSafeY(level, pos.getX(), pos.getZ());
//
//        if (safeY > 0) {
//            player.teleportTo(pos.getX() + 0.5, safeY, pos.getZ() + 0.5);
//        }
//    }
//
//    private static int findSafeY(ServerLevel level, int x, int z) {
//        for (int y = level.getMaxBuildHeight(); y >= level.getMinBuildHeight(); y--) {
//            BlockPos pos = new BlockPos(x, y, z);
//            if (!level.getBlockState(pos).isAir() && !level.getBlockState(pos.above()).isAir()) {
//                return y + 1;
//            }
//        }
//        return level.getSeaLevel() + 1;
//    }
//}

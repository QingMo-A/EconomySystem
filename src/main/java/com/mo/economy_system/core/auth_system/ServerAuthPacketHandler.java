//package com.mo.economy_system.core.auth_system;
//
//import com.mo.economy_system.network.EconomySystem_NetworkManager;
//import com.mo.economy_system.network.packets.auth_system.Packet_LoginResponse;
//import com.mo.economy_system.network.packets.auth_system.Packet_RegisterResponse;
//import net.minecraft.core.BlockPos;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.server.level.ServerPlayer;
//
//
//// DEPRECATED: Old login system
//// public class ServerAuthPacketHandler {
//
//    public static void handleLoginRequest(ServerPlayer player, String password) {
//        ServerLevel level = player.serverLevel();
//        AuthSavedData data = AuthSavedData.getInstance(level);
//
//        if (!data.isRegistered(player.getUUID())) {
//            sendLoginResponse(player, false, "你还没有注册！请先注册。");
//            return;
//        }
//
//        if (data.isLoggedIn(player.getUUID())) {
//            sendLoginResponse(player, false, "你已经登录了！");
//            return;
//        }
//
//        if (data.loginImmediate(player.getUUID(), password, level)) {
//            sendLoginResponse(player, true, "登录成功！");
//            // 传送到地面
//            teleportToGround(player);
//        } else {
//            sendLoginResponse(player, false, "密码错误！");
//        }
//    }
//
//    public static void handleRegisterRequest(ServerPlayer player, String password) {
//        ServerLevel level = player.serverLevel();
//        AuthSavedData data = AuthSavedData.getInstance(level);
//
//        if (data.isRegistered(player.getUUID())) {
//            sendRegisterResponse(player, false, "你已经注册过了！请登录。");
//            return;
//        }
//
//        if (password.length() < 4) {
//            sendRegisterResponse(player, false, "密码长度至少需要4个字符！");
//            return;
//        }
//
//        if (data.registerImmediate(player.getUUID(), password, level)) {
//            sendRegisterResponse(player, true, "注册成功！请登录。");
//        } else {
//            sendRegisterResponse(player, false, "注册失败！");
//        }
//    }
//
//    private static void sendLoginResponse(ServerPlayer player, boolean success, String message) {
//        EconomySystem_NetworkManager.INSTANCE.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new Packet_LoginResponse(success, message));
//    }
//
//    private static void sendRegisterResponse(ServerPlayer player, boolean success, String message) {
//        EconomySystem_NetworkManager.INSTANCE.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new Packet_RegisterResponse(success, message));
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

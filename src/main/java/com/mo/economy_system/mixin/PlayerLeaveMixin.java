//package com.mo.economy_system.mixin;
//
//import com.mo.economy_system.EconomySystem;
//import com.mo.economy_system.server.rank.Rank;
//import com.mo.economy_system.server.rank.capability.RankCapabilityProvider;
//import net.minecraft.network.chat.Component;
//import net.minecraft.server.level.ServerPlayer;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.level.Level;
//import net.minecraft.core.BlockPos;
//import com.mojang.authlib.GameProfile;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//@Mixin(ServerPlayer.class)
//public abstract class PlayerLeaveMixin extends Player {
//
//    public PlayerLeaveMixin(Level level, BlockPos pos, float yaw, GameProfile profile) {
//        super(level, pos, yaw, profile);
//    }
//
//    @Inject(method = "disconnect", at = @At("HEAD"))
//    private void onPlayerDisconnect(CallbackInfo ci) {
//        ServerPlayer player = (ServerPlayer) (Object) this;
//
//        try {
//            // 先覆盖原版离服消息
//            player.server.getPlayerList().broadcastSystemMessage(Component.empty(), false);
//
//            // 再发自定义消息
//            Rank rank = RankCapabilityProvider.getPlayerRank(player);
//            String rawMsg = com.mo.economy_system.server.ChangeJoinMessage.getLeaveMessageByRank(rank);
//            String formattedMsg = rawMsg.replace("%player%", player.getName().getString());
//
//            player.server.getPlayerList().broadcastSystemMessage(Component.literal(formattedMsg), false);
//        } catch (Exception e) {
//            EconomySystem.LOGGER.error("Error sending custom leave message for " + player.getName().getString(), e);
//        }
//    }
//
//    @Override
//    public boolean isSpectator() { return false; }
//
//    @Override
//    public boolean isCreative() { return false; }
//}
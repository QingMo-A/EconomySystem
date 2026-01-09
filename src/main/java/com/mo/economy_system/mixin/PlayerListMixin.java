package com.mo.economy_system.mixin;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.auth_system.AuthSavedData;
import com.mo.economy_system.core.auth_system.network.ServerAuthHandler;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin类：拦截PlayerList
 * 在玩家真正进入世界之前要求登录
 */
@Mixin(PlayerList.class)
public class PlayerListMixin {

    /**
     * 在玩家加入世界后检查认证状态
     * 注意：不要在这里强制logout，否则会覆盖快速登录的效果
     */
    @Inject(
        method = "placeNewPlayer",
        at = @At("TAIL")
    )
    private void economySystem$afterPlayerPlace(Connection connection, ServerPlayer player, CallbackInfo ci) {
        EconomySystem.LOGGER.info("PlayerListMixin.placeNewPlayer 被调用，玩家: " + player.getName().getString());

        // 检查玩家是否需要认证
        AuthSavedData authData = AuthSavedData.getInstance(player.serverLevel());
        boolean isLoggedIn = authData.isLoggedIn(player.getUUID());
        boolean isRegistered = authData.isRegistered(player.getUUID());

        EconomySystem.LOGGER.info("玩家登录状态 - 已登录: " + isLoggedIn + ", 已注册: " + isRegistered);

        // 只有在玩家未登录时才发送认证挑战
        // 不要强制logout，否则会覆盖快速登录的效果
        if (!isLoggedIn) {
            EconomySystem.LOGGER.info("玩家未登录，准备发送认证挑战");
            ServerAuthHandler.sendAuthChallenge(player);
        } else {
            EconomySystem.LOGGER.info("玩家已登录（可能是快速登录），跳过认证挑战");
        }
    }
}

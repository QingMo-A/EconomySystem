package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.market.*;
import com.mo.economy_system.common.network.CreateDemandOrderMessage;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import com.mo.economy_system.platform.EconomyServices;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public final class NeoForge1211CreateDemandOrderHandler {
    private NeoForge1211CreateDemandOrderHandler() {}

    public static void handle(CreateDemandOrderMessage message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            EconomySavedData accounts = EconomySavedData.getInstance(player.serverLevel());
            MarketSavedData market = MarketSavedData.getInstance(player.serverLevel());
            CreateDemandOrderResult result = CreateDemandOrderService.execute(message,
                    new CreateDemandOrderService.Context(new NeoForge1211DemandItemResolver(
                            EconomyServices.platform().itemStacks(), player.registryAccess()), new AccountAdapter(accounts, player),
                            new RepositoryAdapter(market), player.getUUID(), player.getName().getString(),
                            UUID::randomUUID, System::currentTimeMillis, reporter(player)));
            player.sendSystemMessage(messageFor(result));
            if (CreateDemandOrderFeedback.internalFailure(result))
                EconomySystem.LOGGER.error("Demand order creation failed player={} result={}", player.getUUID(), result);
        });
    }

    static Component messageFor(CreateDemandOrderResult result) {
        return Component.translatable(CreateDemandOrderFeedback.messageKey(result));
    }

    private static CreateDemandOrderService.FailureReporter reporter(ServerPlayer player) {
        return (tradeId, stage, result, cause, refunded) -> EconomySystem.LOGGER.error(
                "Demand order transaction failed player={} order={} stage={} result={} refunded={}",
                player.getUUID(), tradeId, stage, result, refunded, cause);
    }

    private record AccountAdapter(EconomySavedData data, ServerPlayer player) implements CreateDemandOrderService.Account {
        public boolean canDebit(int amount) { return data.hasEnoughBalance(player.getUUID(), amount); }
        public boolean debit(int amount) { return data.minBalance(player.getUUID(), amount, "市场", "创建求购单冻结金额"); }
        public boolean credit(int amount) { return data.addBalance(player.getUUID(), amount, "市场", "创建求购单失败退款"); }
    }
    private record RepositoryAdapter(MarketSavedData data) implements CreateDemandOrderService.Repository {
        public boolean isFull() { return data.isFull(); }
        public boolean add(MarketOrder order) { return data.addOrder(order); }
    }
}

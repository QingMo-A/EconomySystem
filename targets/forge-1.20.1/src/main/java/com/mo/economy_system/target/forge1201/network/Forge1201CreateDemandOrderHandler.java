package com.mo.economy_system.target.forge1201.network;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.market.*;
import com.mo.economy_system.common.network.CreateDemandOrderMessage;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import com.mo.economy_system.platform.EconomyServices;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.function.Supplier;

final class Forge1201CreateDemandOrderHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private Forge1201CreateDemandOrderHandler() {}

    static void handle(CreateDemandOrderMessage message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context network = supplier.get();
        ServerPlayer player = network.getSender();
        if (player != null) {
            EconomySavedData accounts = EconomySavedData.getInstance(player.serverLevel());
            MarketSavedData market = MarketSavedData.getInstance(player.serverLevel());
            CreateDemandOrderResult result = CreateDemandOrderService.execute(message,
                    new CreateDemandOrderService.Context(new Forge1201DemandItemResolver(
                            EconomyServices.platform().itemStacks(), player.serverLevel().registryAccess()), new AccountAdapter(accounts, player),
                            new RepositoryAdapter(market), player.getUUID(), player.getName().getString(),
                            UUID::randomUUID, System::currentTimeMillis, reporter(player)));
            player.sendSystemMessage(messageFor(result));
            if (result == CreateDemandOrderResult.SUCCESS) Forge1201MarketInvalidation.broadcast(player);
            if (CreateDemandOrderFeedback.internalFailure(result))
                LOGGER.error("Demand order creation failed player={} result={}", player.getUUID(), result);
        }
        network.setPacketHandled(true);
    }

    static Component messageFor(CreateDemandOrderResult result) {
        return Component.translatable(CreateDemandOrderFeedback.messageKey(result));
    }

    private static CreateDemandOrderService.FailureReporter reporter(ServerPlayer player) {
        return (tradeId, stage, result, cause, refunded) -> LOGGER.error(
                "Demand order transaction failed player={} order={} stage={} result={} refunded={}",
                player.getUUID(), tradeId, stage, result, refunded, cause);
    }

    private record AccountAdapter(EconomySavedData data, ServerPlayer player) implements CreateDemandOrderService.Account {
        public boolean canDebit(int amount) { return data.hasEnoughBalance(player.getUUID(), amount); }
        public BalanceMutationResult debitExact(int amount) { return data.debitExact(player.getUUID(), amount, "市场", "创建求购单冻结金额"); }
        public BalanceMutationResult creditExact(int amount) { return data.creditExact(player.getUUID(), amount, "市场", "创建求购单失败退款"); }
    }
    private record RepositoryAdapter(MarketSavedData data) implements CreateDemandOrderService.Repository {
        public boolean isFull() { return data.isFull(); }
        public boolean add(MarketOrder order) { return data.addOrder(order); }
    }
}

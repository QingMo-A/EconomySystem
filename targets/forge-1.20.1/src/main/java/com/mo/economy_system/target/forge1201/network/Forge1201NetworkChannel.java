package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.EconomyConstants;
import com.mo.economy_system.common.network.AccountBalance;
import com.mo.economy_system.common.network.BalanceLogRequestMessage;
import com.mo.economy_system.common.network.BalanceLogResponseMessage;
import com.mo.economy_system.common.network.BalanceRequestMessage;
import com.mo.economy_system.common.network.BalanceResponseMessage;
import com.mo.economy_system.common.network.EconomyMessages;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.common.network.ServerPlayerListRequestMessage;
import com.mo.economy_system.common.network.ServerPlayerListResponseMessage;
import com.mo.economy_system.common.network.ShopDataRequestMessage;
import com.mo.economy_system.common.network.ShopDataResponseMessage;
import com.mo.economy_system.common.network.ShopBuyItemMessage;
import com.mo.economy_system.common.network.ShopItemSnapshot;
import com.mo.economy_system.common.network.TransferMessage;
import com.mo.economy_system.core.economy_system.BalanceLogEntry;
import com.mo.economy_system.protocol.EconomyProtocol;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;

/** Forge 1.20.1 wire adapter for common protocol messages ported so far. */
public final class Forge1201NetworkChannel {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EconomyConstants.MOD_ID, "bridge"),
            () -> EconomyProtocol.VERSION,
            EconomyProtocol.VERSION::equals,
            EconomyProtocol.VERSION::equals
    );

    private static volatile boolean registered;

    private Forge1201NetworkChannel() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }

        CHANNEL.messageBuilder(
                        BalanceRequestMessage.class,
                        EconomyMessages.BALANCE_REQUEST.discriminator(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(Forge1201NetworkChannel::encodeBalanceRequest)
                .decoder(Forge1201NetworkChannel::decodeBalanceRequest)
                .consumerMainThread(Forge1201BalanceHandlers::handleRequest)
                .add();

        CHANNEL.messageBuilder(
                        BalanceResponseMessage.class,
                        EconomyMessages.BALANCE_RESPONSE.discriminator(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(Forge1201NetworkChannel::encodeBalanceResponse)
                .decoder(Forge1201NetworkChannel::decodeBalanceResponse)
                .consumerMainThread(Forge1201BalanceHandlers::handleResponse)
                .add();

        CHANNEL.messageBuilder(
                        BalanceLogRequestMessage.class,
                        EconomyMessages.BALANCE_LOG_REQUEST.discriminator(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(Forge1201NetworkChannel::encodeBalanceLogRequest)
                .decoder(Forge1201NetworkChannel::decodeBalanceLogRequest)
                .consumerMainThread(Forge1201BalanceLogHandlers::handleRequest)
                .add();

        CHANNEL.messageBuilder(
                        BalanceLogResponseMessage.class,
                        EconomyMessages.BALANCE_LOG_RESPONSE.discriminator(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(Forge1201NetworkChannel::encodeBalanceLogResponse)
                .decoder(Forge1201NetworkChannel::decodeBalanceLogResponse)
                .consumerMainThread(Forge1201BalanceLogHandlers::handleResponse)
                .add();

        CHANNEL.messageBuilder(
                        TransferMessage.class,
                        EconomyMessages.TRANSFER.discriminator(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(Forge1201NetworkChannel::encodeTransfer)
                .decoder(Forge1201NetworkChannel::decodeTransfer)
                .consumerMainThread(Forge1201TransferHandler::handle)
                .add();

        CHANNEL.messageBuilder(
                        ShopDataRequestMessage.class,
                        EconomyMessages.SHOP_DATA_REQUEST.discriminator(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(Forge1201NetworkChannel::encodeShopDataRequest)
                .decoder(Forge1201NetworkChannel::decodeShopDataRequest)
                .consumerMainThread(Forge1201ShopDataHandlers::handleRequest)
                .add();

        CHANNEL.messageBuilder(
                        ShopDataResponseMessage.class,
                        EconomyMessages.SHOP_DATA_RESPONSE.discriminator(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(Forge1201NetworkChannel::encodeShopDataResponse)
                .decoder(Forge1201NetworkChannel::decodeShopDataResponse)
                .consumerMainThread(Forge1201ShopDataHandlers::handleResponse)
                .add();

        CHANNEL.messageBuilder(
                        ShopBuyItemMessage.class,
                        EconomyMessages.SHOP_BUY_ITEM.discriminator(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(Forge1201NetworkChannel::encodeShopBuyItem)
                .decoder(Forge1201NetworkChannel::decodeShopBuyItem)
                .consumerMainThread(Forge1201ShopPurchaseHandler::handle)
                .add();

        CHANNEL.messageBuilder(
                        ServerPlayerListRequestMessage.class,
                        EconomyMessages.SERVER_PLAYER_LIST_REQUEST.discriminator(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .encoder(Forge1201NetworkChannel::encodePlayerListRequest)
                .decoder(Forge1201NetworkChannel::decodePlayerListRequest)
                .consumerMainThread(Forge1201PlayerListHandlers::handleRequest)
                .add();

        CHANNEL.messageBuilder(
                        ServerPlayerListResponseMessage.class,
                        EconomyMessages.SERVER_PLAYER_LIST_RESPONSE.discriminator(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(Forge1201NetworkChannel::encodePlayerListResponse)
                .decoder(Forge1201NetworkChannel::decodePlayerListResponse)
                .consumerMainThread(Forge1201PlayerListHandlers::handleResponse)
                .add();

        registered = true;
    }

    static void sendToServer(BalanceRequestMessage message) {
        requireRegistered();
        CHANNEL.sendToServer(message);
    }

    static void sendToServer(BalanceLogRequestMessage message) {
        requireRegistered();
        CHANNEL.sendToServer(message);
    }

    static void sendToServer(TransferMessage message) {
        requireRegistered();
        CHANNEL.sendToServer(message);
    }

    static void sendToServer(ShopDataRequestMessage message) {
        requireRegistered();
        CHANNEL.sendToServer(message);
    }

    static void sendToServer(ShopBuyItemMessage message) {
        requireRegistered();
        CHANNEL.sendToServer(message);
    }

    static void sendToServer(ServerPlayerListRequestMessage message) {
        requireRegistered();
        CHANNEL.sendToServer(message);
    }

    static void sendToPlayer(ServerPlayer player, BalanceResponseMessage message) {
        requireRegistered();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    static void sendToPlayer(ServerPlayer player, BalanceLogResponseMessage message) {
        requireRegistered();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    static void sendToPlayer(ServerPlayer player, ShopDataResponseMessage message) {
        requireRegistered();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    static void sendToPlayer(ServerPlayer player, ServerPlayerListResponseMessage message) {
        requireRegistered();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    private static void encodeBalanceRequest(BalanceRequestMessage message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.includeAccountList());
    }

    private static BalanceRequestMessage decodeBalanceRequest(FriendlyByteBuf buffer) {
        return new BalanceRequestMessage(buffer.readBoolean());
    }

    private static void encodeBalanceResponse(BalanceResponseMessage message, FriendlyByteBuf buffer) {
        if (message.accounts().size() > EconomyNetworkLimits.MAX_ACCOUNT_ENTRIES) {
            throw new IllegalArgumentException(
                    "Balance response has too many accounts: " + message.accounts().size()
            );
        }

        buffer.writeInt(message.balance());
        buffer.writeInt(message.accounts().size());
        for (AccountBalance account : message.accounts()) {
            buffer.writeUtf(account.playerName(), EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH);
            buffer.writeInt(account.balance());
        }
    }

    private static BalanceResponseMessage decodeBalanceResponse(FriendlyByteBuf buffer) {
        int balance = buffer.readInt();
        int size = buffer.readInt();
        if (size < 0 || size > EconomyNetworkLimits.MAX_ACCOUNT_ENTRIES) {
            throw new DecoderException("Invalid balance account count: " + size);
        }

        List<AccountBalance> accounts = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            accounts.add(new AccountBalance(
                    buffer.readUtf(EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH),
                    buffer.readInt()
            ));
        }
        return new BalanceResponseMessage(balance, accounts);
    }

    private static void encodeBalanceLogRequest(
            BalanceLogRequestMessage message,
            FriendlyByteBuf buffer
    ) {
        if (!isValidBalanceLogRequestMetadata(message.offset(), message.limit())) {
            throw new IllegalArgumentException(
                    "Invalid balance-log request page: offset=" + message.offset()
                            + ", limit=" + message.limit()
            );
        }
        buffer.writeUtf(
                message.category(),
                EconomyNetworkLimits.MAX_BALANCE_LOG_CATEGORY_LENGTH
        );
        buffer.writeInt(message.offset());
        buffer.writeInt(message.limit());
    }

    private static BalanceLogRequestMessage decodeBalanceLogRequest(FriendlyByteBuf buffer) {
        String category = buffer.readUtf(EconomyNetworkLimits.MAX_BALANCE_LOG_CATEGORY_LENGTH);
        int offset = buffer.readInt();
        int limit = buffer.readInt();
        if (!isValidBalanceLogRequestMetadata(offset, limit)) {
            throw new DecoderException(
                    "Invalid balance-log request page: offset=" + offset
                            + ", limit=" + limit
            );
        }
        return new BalanceLogRequestMessage(category, offset, limit);
    }

    private static void encodeBalanceLogResponse(
            BalanceLogResponseMessage message,
            FriendlyByteBuf buffer
    ) {
        int size = message.logs().size();
        if (!isValidBalanceLogResponseMetadata(
                message.offset(),
                message.limit(),
                message.total(),
                size
        )) {
            throw new IllegalArgumentException(
                    "Invalid balance-log response page: offset=" + message.offset()
                            + ", limit=" + message.limit()
                            + ", total=" + message.total()
                            + ", size=" + size
            );
        }

        buffer.writeUtf(
                message.category(),
                EconomyNetworkLimits.MAX_BALANCE_LOG_CATEGORY_LENGTH
        );
        buffer.writeInt(message.offset());
        buffer.writeInt(message.limit());
        buffer.writeInt(message.total());
        buffer.writeInt(size);
        for (BalanceLogEntry log : message.logs()) {
            buffer.writeLong(log.timeMillis());
            buffer.writeUtf(
                    log.category() == null ? "系统" : log.category(),
                    EconomyNetworkLimits.MAX_BALANCE_LOG_CATEGORY_LENGTH
            );
            buffer.writeUtf(
                    log.reason() == null ? "" : log.reason(),
                    EconomyNetworkLimits.MAX_BALANCE_LOG_REASON_LENGTH
            );
            buffer.writeInt(log.delta());
            buffer.writeInt(log.beforeBalance());
            buffer.writeInt(log.afterBalance());
        }
    }

    private static BalanceLogResponseMessage decodeBalanceLogResponse(FriendlyByteBuf buffer) {
        String category = buffer.readUtf(EconomyNetworkLimits.MAX_BALANCE_LOG_CATEGORY_LENGTH);
        int offset = buffer.readInt();
        int limit = buffer.readInt();
        int total = buffer.readInt();
        int size = buffer.readInt();
        if (!isValidBalanceLogResponseMetadata(offset, limit, total, size)) {
            throw new DecoderException(
                    "Invalid balance-log response page: offset=" + offset
                            + ", limit=" + limit
                            + ", total=" + total
                            + ", size=" + size
            );
        }

        List<BalanceLogEntry> logs = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            logs.add(new BalanceLogEntry(
                    buffer.readLong(),
                    buffer.readUtf(EconomyNetworkLimits.MAX_BALANCE_LOG_CATEGORY_LENGTH),
                    buffer.readUtf(EconomyNetworkLimits.MAX_BALANCE_LOG_REASON_LENGTH),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readInt()
            ));
        }
        return new BalanceLogResponseMessage(category, offset, limit, total, logs);
    }

    private static void encodeTransfer(TransferMessage message, FriendlyByteBuf buffer) {
        buffer.writeUUID(message.targetPlayerId());
        buffer.writeInt(message.amount());
    }

    private static TransferMessage decodeTransfer(FriendlyByteBuf buffer) {
        return new TransferMessage(buffer.readUUID(), buffer.readInt());
    }

    private static void encodeShopDataRequest(
            ShopDataRequestMessage message,
            FriendlyByteBuf buffer
    ) {
        // Empty payload.
    }

    private static ShopDataRequestMessage decodeShopDataRequest(FriendlyByteBuf buffer) {
        return ShopDataRequestMessage.INSTANCE;
    }

    private static void encodeShopBuyItem(ShopBuyItemMessage message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.shopItemId(), EconomyNetworkLimits.MAX_SHOP_ITEM_ID_LENGTH);
        buffer.writeInt(message.quantity());
    }

    private static ShopBuyItemMessage decodeShopBuyItem(FriendlyByteBuf buffer) {
        return new ShopBuyItemMessage(
                buffer.readUtf(EconomyNetworkLimits.MAX_SHOP_ITEM_ID_LENGTH),
                buffer.readInt()
        );
    }

    private static void encodeShopDataResponse(
            ShopDataResponseMessage message,
            FriendlyByteBuf buffer
    ) {
        if (message.items().size() > EconomyNetworkLimits.MAX_SHOP_ENTRIES) {
            throw new IllegalArgumentException(
                    "Shop response has too many entries: " + message.items().size()
            );
        }
        buffer.writeInt(message.items().size());
        for (ShopItemSnapshot item : message.items()) {
            encodeShopItem(item, buffer);
        }
    }

    private static ShopDataResponseMessage decodeShopDataResponse(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        if (size < 0 || size > EconomyNetworkLimits.MAX_SHOP_ENTRIES) {
            throw new DecoderException("Invalid shop entry count: " + size);
        }
        List<ShopItemSnapshot> items = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            items.add(decodeShopItem(buffer));
        }
        return new ShopDataResponseMessage(items);
    }

    private static void encodeShopItem(ShopItemSnapshot item, FriendlyByteBuf buffer) {
        buffer.writeUtf(item.shopItemId(), EconomyNetworkLimits.MAX_SHOP_ITEM_ID_LENGTH);
        buffer.writeUtf(item.itemId(), EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH);
        buffer.writeInt(item.basePrice());
        buffer.writeInt(item.currentPrice());
        buffer.writeInt(item.lastPrice());
        buffer.writeUtf(item.description(), EconomyNetworkLimits.MAX_SHOP_DESCRIPTION_LENGTH);
        buffer.writeDouble(item.fluctuationFactor());
        buffer.writeUtf(item.nbt(), EconomyNetworkLimits.MAX_ITEM_DATA_LENGTH);
        buffer.writeUtf(item.itemData(), EconomyNetworkLimits.MAX_ITEM_DATA_LENGTH);
        buffer.writeInt(item.recentDemand());
        buffer.writeInt(item.virtualStock());
        buffer.writeInt(item.maxVirtualStock());
    }

    private static ShopItemSnapshot decodeShopItem(FriendlyByteBuf buffer) {
        return new ShopItemSnapshot(
                buffer.readUtf(EconomyNetworkLimits.MAX_SHOP_ITEM_ID_LENGTH),
                buffer.readUtf(EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readUtf(EconomyNetworkLimits.MAX_SHOP_DESCRIPTION_LENGTH),
                buffer.readDouble(),
                buffer.readUtf(EconomyNetworkLimits.MAX_ITEM_DATA_LENGTH),
                buffer.readUtf(EconomyNetworkLimits.MAX_ITEM_DATA_LENGTH),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt()
        );
    }

    private static void encodePlayerListRequest(
            ServerPlayerListRequestMessage message,
            FriendlyByteBuf buffer
    ) {
        // Empty payload.
    }

    private static ServerPlayerListRequestMessage decodePlayerListRequest(FriendlyByteBuf buffer) {
        return ServerPlayerListRequestMessage.INSTANCE;
    }

    private static void encodePlayerListResponse(
            ServerPlayerListResponseMessage message,
            FriendlyByteBuf buffer
    ) {
        if (message.players().size() > EconomyNetworkLimits.MAX_PLAYER_LIST_ENTRIES) {
            throw new IllegalArgumentException(
                    "Player list response has too many entries: " + message.players().size()
            );
        }

        buffer.writeInt(message.players().size());
        for (PlayerSummary player : message.players()) {
            buffer.writeUUID(player.playerId());
            buffer.writeUtf(player.playerName(), EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH);
        }
    }

    private static ServerPlayerListResponseMessage decodePlayerListResponse(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        if (size < 0 || size > EconomyNetworkLimits.MAX_PLAYER_LIST_ENTRIES) {
            throw new DecoderException("Invalid player list entry count: " + size);
        }

        List<PlayerSummary> players = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            players.add(new PlayerSummary(
                    buffer.readUUID(),
                    buffer.readUtf(EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH)
            ));
        }
        return new ServerPlayerListResponseMessage(players);
    }

    private static boolean isValidBalanceLogRequestMetadata(int offset, int limit) {
        return offset >= 0
                && limit >= 1
                && limit <= EconomyNetworkLimits.MAX_BALANCE_LOG_ENTRIES;
    }

    private static boolean isValidBalanceLogResponseMetadata(
            int offset,
            int limit,
            int total,
            int size
    ) {
        if (!isValidBalanceLogRequestMetadata(offset, limit)
                || total < 0
                || size < 0
                || size > EconomyNetworkLimits.MAX_BALANCE_LOG_ENTRIES
                || size > limit
                || size > total) {
            return false;
        }
        return size == 0 || (long) offset + size <= total;
    }

    private static void requireRegistered() {
        if (!registered) {
            throw new IllegalStateException("Forge 1.20.1 network channel is not registered");
        }
    }
}

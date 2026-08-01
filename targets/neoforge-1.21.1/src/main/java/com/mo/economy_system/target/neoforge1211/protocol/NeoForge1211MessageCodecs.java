package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.network.AccountBalance;
import com.mo.economy_system.common.network.BalanceLogRequestMessage;
import com.mo.economy_system.common.network.BalanceLogResponseMessage;
import com.mo.economy_system.common.network.BalanceRequestMessage;
import com.mo.economy_system.common.network.BalanceResponseMessage;
import com.mo.economy_system.common.network.EconomyMessages;
import com.mo.economy_system.common.network.CreateSalesOrderMessage;
import com.mo.economy_system.common.network.CreateDemandOrderMessage;
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
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import com.mo.economy_system.protocol.EconomyMessageType;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Codecs for common messages migrated to the NeoForge 1.21.1 target. */
public final class NeoForge1211MessageCodecs {
    private static final Map<EconomyMessageType<?>, NeoForge1211MessageCodec<?>> CODECS =
            createCodecs();

    private NeoForge1211MessageCodecs() {
    }

    public static boolean supports(EconomyMessageType<?> messageType) {
        return CODECS.containsKey(messageType);
    }

    @SuppressWarnings("unchecked")
    public static <T extends EconomyNetworkMessage> NeoForge1211MessageCodec<T> codec(
            EconomyMessageType<T> messageType
    ) {
        NeoForge1211MessageCodec<?> codec = CODECS.get(messageType);
        if (codec == null) {
            throw new IllegalArgumentException(
                    "No NeoForge 1.21.1 common-message codec for " + messageType.id()
            );
        }
        return (NeoForge1211MessageCodec<T>) codec;
    }

    private static Map<EconomyMessageType<?>, NeoForge1211MessageCodec<?>> createCodecs() {
        Map<EconomyMessageType<?>, NeoForge1211MessageCodec<?>> codecs = new HashMap<>();
        register(codecs, EconomyMessages.BALANCE_REQUEST, new NeoForge1211MessageCodec<>() {
            @Override
            public void encode(BalanceRequestMessage message, RegistryFriendlyByteBuf buffer) {
                buffer.writeBoolean(message.includeAccountList());
            }

            @Override
            public BalanceRequestMessage decode(RegistryFriendlyByteBuf buffer) {
                return new BalanceRequestMessage(buffer.readBoolean());
            }
        });
        register(codecs, EconomyMessages.BALANCE_RESPONSE, new NeoForge1211MessageCodec<>() {
            @Override
            public void encode(BalanceResponseMessage message, RegistryFriendlyByteBuf buffer) {
                if (message.accounts().size() > EconomyNetworkLimits.MAX_ACCOUNT_ENTRIES) {
                    throw new IllegalArgumentException(
                            "Balance response has too many accounts: " + message.accounts().size()
                    );
                }
                // Preserve the NeoForge 1.21.1 wire order exactly.
                buffer.writeInt(message.balance());
                buffer.writeInt(message.accounts().size());
                for (AccountBalance account : message.accounts()) {
                    buffer.writeUtf(
                            account.playerName(),
                            EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH
                    );
                    buffer.writeInt(account.balance());
                }
            }

            @Override
            public BalanceResponseMessage decode(RegistryFriendlyByteBuf buffer) {
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
        });
        register(codecs, EconomyMessages.BALANCE_LOG_REQUEST, new NeoForge1211MessageCodec<>() {
            @Override
            public void encode(BalanceLogRequestMessage message, RegistryFriendlyByteBuf buffer) {
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

            @Override
            public BalanceLogRequestMessage decode(RegistryFriendlyByteBuf buffer) {
                String category = buffer.readUtf(
                        EconomyNetworkLimits.MAX_BALANCE_LOG_CATEGORY_LENGTH
                );
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
        });
        register(codecs, EconomyMessages.BALANCE_LOG_RESPONSE, new NeoForge1211MessageCodec<>() {
            @Override
            public void encode(BalanceLogResponseMessage message, RegistryFriendlyByteBuf buffer) {
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

            @Override
            public BalanceLogResponseMessage decode(RegistryFriendlyByteBuf buffer) {
                String category = buffer.readUtf(
                        EconomyNetworkLimits.MAX_BALANCE_LOG_CATEGORY_LENGTH
                );
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
        });
        register(codecs, EconomyMessages.TRANSFER, new NeoForge1211MessageCodec<>() {
            @Override
            public void encode(TransferMessage message, RegistryFriendlyByteBuf buffer) {
                buffer.writeUUID(message.targetPlayerId());
                buffer.writeInt(message.amount());
            }

            @Override
            public TransferMessage decode(RegistryFriendlyByteBuf buffer) {
                return new TransferMessage(buffer.readUUID(), buffer.readInt());
            }
        });
        register(codecs, EconomyMessages.SHOP_DATA_REQUEST, new NeoForge1211MessageCodec<>() {
            @Override
            public void encode(ShopDataRequestMessage message, RegistryFriendlyByteBuf buffer) {
                // Empty payload.
            }

            @Override
            public ShopDataRequestMessage decode(RegistryFriendlyByteBuf buffer) {
                return ShopDataRequestMessage.INSTANCE;
            }
        });
        register(codecs, EconomyMessages.SHOP_DATA_RESPONSE, new NeoForge1211MessageCodec<>() {
            @Override
            public void encode(ShopDataResponseMessage message, RegistryFriendlyByteBuf buffer) {
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

            @Override
            public ShopDataResponseMessage decode(RegistryFriendlyByteBuf buffer) {
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
        });
        register(codecs, EconomyMessages.SHOP_BUY_ITEM, new NeoForge1211MessageCodec<>() {
            @Override
            public void encode(ShopBuyItemMessage message, RegistryFriendlyByteBuf buffer) {
                buffer.writeUtf(
                        message.shopItemId(),
                        EconomyNetworkLimits.MAX_SHOP_ITEM_ID_LENGTH
                );
                buffer.writeInt(message.quantity());
            }

            @Override
            public ShopBuyItemMessage decode(RegistryFriendlyByteBuf buffer) {
                return new ShopBuyItemMessage(
                        buffer.readUtf(EconomyNetworkLimits.MAX_SHOP_ITEM_ID_LENGTH),
                        buffer.readInt()
                );
            }
        });
        register(codecs, EconomyMessages.CREATE_SALES_ORDER, new NeoForge1211MessageCodec<>() {
            @Override public void encode(CreateSalesOrderMessage message, RegistryFriendlyByteBuf buffer) {
                buffer.writeInt(message.slot()); buffer.writeInt(message.quantity()); buffer.writeInt(message.totalPrice());
            }
            @Override public CreateSalesOrderMessage decode(RegistryFriendlyByteBuf buffer) {
                int slot = buffer.readInt(); int quantity = buffer.readInt(); int totalPrice = buffer.readInt();
                if (slot < 0 || quantity <= 0 || totalPrice <= 0) throw new DecoderException("Invalid create sales order request");
                return new CreateSalesOrderMessage(slot, quantity, totalPrice);
            }
        });
        register(codecs, EconomyMessages.CREATE_DEMAND_ORDER, new NeoForge1211MessageCodec<>() {
            @Override public void encode(CreateDemandOrderMessage message, RegistryFriendlyByteBuf buffer) {
                validateDemand(message.itemId(), message.quantity(), message.totalPrice());
                buffer.writeUtf(message.itemId(), EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH);
                buffer.writeInt(message.quantity()); buffer.writeInt(message.totalPrice());
            }
            @Override public CreateDemandOrderMessage decode(RegistryFriendlyByteBuf buffer) {
                String itemId = buffer.readUtf(EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH);
                int quantity = buffer.readInt(); int totalPrice = buffer.readInt();
                validateDemand(itemId, quantity, totalPrice);
                return new CreateDemandOrderMessage(itemId, quantity, totalPrice);
            }
        });
        register(codecs, EconomyMessages.MARKET_DATA_REQUEST, NeoForge1211MarketDataCodec.REQUEST);
        register(codecs, EconomyMessages.MARKET_DATA_RESPONSE, NeoForge1211MarketDataCodec.RESPONSE);
        register(codecs, EconomyMessages.SERVER_PLAYER_LIST_REQUEST, new NeoForge1211MessageCodec<>() {
            @Override
            public void encode(
                    ServerPlayerListRequestMessage message,
                    RegistryFriendlyByteBuf buffer
            ) {
                // Empty payload.
            }

            @Override
            public ServerPlayerListRequestMessage decode(RegistryFriendlyByteBuf buffer) {
                return ServerPlayerListRequestMessage.INSTANCE;
            }
        });
        register(codecs, EconomyMessages.SERVER_PLAYER_LIST_RESPONSE, new NeoForge1211MessageCodec<>() {
            @Override
            public void encode(
                    ServerPlayerListResponseMessage message,
                    RegistryFriendlyByteBuf buffer
            ) {
                if (message.players().size() > EconomyNetworkLimits.MAX_PLAYER_LIST_ENTRIES) {
                    throw new IllegalArgumentException(
                            "Player list response has too many entries: " + message.players().size()
                    );
                }
                buffer.writeInt(message.players().size());
                for (PlayerSummary player : message.players()) {
                    buffer.writeUUID(player.playerId());
                    buffer.writeUtf(
                            player.playerName(),
                            EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH
                    );
                }
            }

            @Override
            public ServerPlayerListResponseMessage decode(RegistryFriendlyByteBuf buffer) {
                int size = buffer.readInt();
                if (size < 0 || size > EconomyNetworkLimits.MAX_PLAYER_LIST_ENTRIES) {
                    throw new DecoderException("Invalid player list size: " + size);
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
        });
        return Map.copyOf(codecs);
    }

    private static void encodeShopItem(ShopItemSnapshot item, RegistryFriendlyByteBuf buffer) {
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

    private static ShopItemSnapshot decodeShopItem(RegistryFriendlyByteBuf buffer) {
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

    private static boolean isValidBalanceLogRequestMetadata(int offset, int limit) {
        return offset >= 0
                && limit >= 1
                && limit <= EconomyNetworkLimits.MAX_BALANCE_LOG_ENTRIES;
    }

    private static void validateDemand(String itemId, int quantity, int totalPrice) {
        if (itemId == null || itemId.isBlank()
                || itemId.length() > EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH
                || quantity <= 0 || totalPrice <= 0)
            throw new DecoderException("Invalid create demand order request");
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

    private static <T extends EconomyNetworkMessage> void register(
            Map<EconomyMessageType<?>, NeoForge1211MessageCodec<?>> codecs,
            EconomyMessageType<T> messageType,
            NeoForge1211MessageCodec<T> codec
    ) {
        if (codecs.put(messageType, codec) != null) {
            throw new IllegalStateException("Duplicate NeoForge codec for " + messageType.id());
        }
    }
}

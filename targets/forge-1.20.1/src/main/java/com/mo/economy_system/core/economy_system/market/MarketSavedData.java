package com.mo.economy_system.core.economy_system.market;

import com.mo.economy_system.common.market.MarketLedger;
import com.mo.economy_system.common.market.MarketOrder;
import com.mo.economy_system.common.market.MarketOrderCodec;
import com.mo.economy_system.common.market.MarketOrderType;
import com.mo.economy_system.common.market.DemandDeliveryTransitionResult;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.platform.item.ItemStackSnapshotResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Forge 1.20.1 persistence shell. Unsupported legacy native data aborts loading instead of being dropped. */
public final class MarketSavedData extends SavedData {
    private static final String DATA_NAME = "market_data";
    private final MarketLedger ledger = new MarketLedger(this::setDirty);
    private ServerLevel level;
    private List<CompoundTag> pendingLegacy = List.of();

    public boolean isFull() { return ledger.isFull(); }
    public boolean addOrder(MarketOrder order) { return ledger.add(order); }
    public List<MarketOrder> getOrders() { return ledger.orders(); }
    public MarketOrder getOrder(java.util.UUID id) { return ledger.find(id); }
    public DemandDeliveryTransitionResult markDemandDelivered(java.util.UUID id) { return ledger.markDemandDelivered(id); }

    @Override public CompoundTag save(CompoundTag tag) {
        if (!pendingLegacy.isEmpty()) throw new IllegalStateException("unresolved legacy market data cannot be overwritten");
        ListTag list = new ListTag();
        for (MarketOrder order : ledger.orders()) list.add(MarketOrderCodec.encode(order));
        tag.put("marketItems", list);
        return tag;
    }

    public static MarketSavedData load(CompoundTag tag) {
        MarketSavedData data = new MarketSavedData();
        List<MarketOrder> orders = new ArrayList<>();
        List<CompoundTag> legacy = new ArrayList<>();
        if (tag.contains("marketItems", Tag.TAG_LIST)) {
            ListTag list = tag.getList("marketItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag order = list.getCompound(i);
                ItemStackSnapshotResult<MarketOrder> current = MarketOrderCodec.decodeCurrent(order);
                if (current.isSuccess()) orders.add(current.orElseThrow());
                else if (order.getCompound("itemStack").contains("schemaVersion"))
                    throw new IllegalArgumentException(current.error().orElseThrow() + ": " + current.detail());
                else legacy.add(order.copy());
            }
        }
        data.ledger.restore(orders);
        data.pendingLegacy = List.copyOf(legacy);
        return data;
    }

    public static MarketSavedData getInstance(ServerLevel requested) {
        ServerLevel overworld = requested.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) throw new IllegalStateException("Overworld is not loaded");
        MarketSavedData data = overworld.getDataStorage().computeIfAbsent(MarketSavedData::load, MarketSavedData::new, DATA_NAME);
        data.level = overworld;
        if (!data.pendingLegacy.isEmpty()) {
            List<MarketOrder> merged = new ArrayList<>(data.ledger.orders());
            for (CompoundTag legacy : data.pendingLegacy) merged.add(data.decodeLegacy(legacy));
            data.ledger.restore(merged);
            data.pendingLegacy = List.of();
        }
        return data;
    }

    private MarketOrder decodeLegacy(CompoundTag tag) {
        String typeId = tag.getString("type");
        MarketOrderType type = MarketOrderType.fromPersistentId(typeId);
        ItemStack nativeStack = ItemStack.of(tag.getCompound("itemStack"));
        int quantity = tag.contains("listedCount", Tag.TAG_INT) ? tag.getInt("listedCount") : nativeStack.getCount();
        nativeStack.setCount(1);
        ItemStackSnapshot snapshot = EconomyServices.platform().itemStacks()
                .captureSnapshot(nativeStack, level.registryAccess()).orElseThrow();
        long listing = tag.getLong("listingTime");
        long expiration = tag.contains("expirationTime", Tag.TAG_LONG) ? tag.getLong("expirationTime")
                : listing + MarketOrder.EXPIRATION_DURATION_MILLIS;
        return new MarketOrder(type, tag.getUUID("tradeID"), snapshot, quantity, tag.getInt("basePrice"),
                tag.getString("sellerName"), tag.getUUID("sellerID"), listing, expiration,
                type == MarketOrderType.DEMAND && tag.getBoolean("delivered"));
    }
}

package com.mo.economy_system.core.economy_system.market;

import com.mo.economy_system.common.market.MarketLedger;
import com.mo.economy_system.common.market.MarketOrder;
import com.mo.economy_system.common.market.MarketOrderCodec;
import com.mo.economy_system.common.market.MarketOrderType;
import com.mo.economy_system.common.market.DemandDeliveryTransitionResult;
import com.mo.economy_system.common.market.DemandOrderRemovalResult;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.platform.item.ItemStackSnapshotResult;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** NeoForge SavedData shell around the common market ledger. */
public class MarketSavedData extends SavedData {
    private static final String DATA_NAME = "market_data";
    private final MarketLedger ledger = new MarketLedger(this::setDirty);
    private HolderLookup.Provider registries;

    public List<MarketOrder> getOrders() { return ledger.orders(); }
    public com.mo.economy_system.common.market.MarketLedgerView getView() { return ledger.view(); }
    public MarketOrder getOrder(java.util.UUID id) { return ledger.find(id); }
    public boolean isFull() { return ledger.isFull(); }
    public boolean addOrder(MarketOrder order) { return ledger.add(order); }
    public boolean removeOrder(java.util.UUID id) { return ledger.remove(id); }
    public DemandDeliveryTransitionResult markDemandDelivered(java.util.UUID id) { return ledger.markDemandDelivered(id); }
    public DemandOrderRemovalResult removeUndeliveredDemand(java.util.UUID id) { return ledger.removeUndeliveredDemand(id); }
    public com.mo.economy_system.common.market.SalesOrderRemovalResult removeSalesForPurchase(java.util.UUID id) { return ledger.removeSalesForPurchase(id); }

    public List<MarketItem> getMarketItems() {
        List<MarketItem> result = new ArrayList<>();
        for (MarketOrder order : ledger.orders()) result.add(toLegacy(order));
        return result;
    }

    public void addMarketItem(MarketItem item) { if (!ledger.add(fromLegacy(item))) throw new IllegalStateException("market rejected order"); }
    public void removeMarketItem(MarketItem item) { ledger.remove(item.getTradeID()); }
    public void clearMarketItems() { ledger.replaceAll(List.of()); }
    public void replaceMarketItems(List<MarketItem> items) { ledger.replaceAll(items.stream().map(this::fromLegacy).toList()); }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (MarketOrder order : ledger.orders()) list.add(MarketOrderCodec.encode(order));
        tag.put("marketItems", list);
        tag.putLong("marketRevision", ledger.revision());
        return tag;
    }

    public static MarketSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        MarketSavedData data = new MarketSavedData();
        data.registries = provider;
        List<MarketOrder> restored = new ArrayList<>();
        if (tag.contains("marketItems", Tag.TAG_LIST)) {
            ListTag list = tag.getList("marketItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) restored.add(data.decodeOrder(list.getCompound(i)));
        }
        long revision=tag.contains("marketRevision",Tag.TAG_LONG)?tag.getLong("marketRevision"):0L;
        data.ledger.loadFromPersistence(restored,revision);
        return data;
    }

    public static MarketSavedData getInstance(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) throw new IllegalStateException("Overworld is not loaded");
        MarketSavedData data = overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(MarketSavedData::new, MarketSavedData::load), DATA_NAME);
        data.registries = overworld.registryAccess();
        MarketManager.bind(data);
        return data;
    }

    private MarketOrder decodeOrder(CompoundTag tag) {
        ItemStackSnapshotResult<MarketOrder> current = MarketOrderCodec.decodeCurrent(tag);
        if (current.isSuccess()) return current.orElseThrow();
        if (tag.getCompound("itemStack").contains("schemaVersion")) {
            throw new IllegalArgumentException(current.error().orElseThrow() + ": " + current.detail());
        }
        // Old 1.21 saves contain a full native stack. Conversion is strict through the stage-A bridge.
        MarketItem legacy = MarketItem.fromNBT(tag, registries);
        return fromLegacy(legacy);
    }

    private MarketOrder fromLegacy(MarketItem item) {
        ItemStack stack = item.getItemStack();
        int quantity = stack.getCount();
        stack.setCount(1);
        ItemStackSnapshot snapshot = EconomyServices.platform().itemStacks().captureSnapshot(stack, registries).orElseThrow();
        return new MarketOrder(item instanceof DemandOrder ? MarketOrderType.DEMAND : MarketOrderType.SALES,
                item.getTradeID(), snapshot, quantity, item.getBasePrice(), item.getSellerName(), item.getSellerID(),
                item.getListingTime(), item.getExpirationTime(), item instanceof DemandOrder demand && demand.isDelivered());
    }

    private MarketItem toLegacy(MarketOrder order) {
        ItemStack stack = EconomyServices.platform().itemStacks().restoreSnapshot(order.item(), registries).orElseThrow();
        stack.setCount(order.quantity());
        if (order.type() == MarketOrderType.DEMAND) return new DemandOrder(order.tradeId(), order.item().itemId(), stack,
                order.totalPrice(), order.sellerName(), order.sellerId(), order.listingTime(), order.expirationTime(), order.delivered());
        return new SalesOrder(order.tradeId(), order.item().itemId(), stack, order.totalPrice(), order.sellerName(),
                order.sellerId(), order.listingTime(), order.expirationTime());
    }
}

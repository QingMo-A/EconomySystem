package com.mo.economy_system.network;

import com.mo.economy_system.common.network.*;
import com.mo.economy_system.platform.network.WireBuffer;
import java.util.ArrayList;
import java.util.List;

/** Shared bounded wire format for the recycling station. */
public final class RecycleWireCodec {
  private RecycleWireCodec() {}
  public static void encodeDataRequest(RecycleDataRequestMessage m, WireBuffer b) { b.writeLong(m.requestId()); }
  public static RecycleDataRequestMessage decodeDataRequest(WireBuffer b) { return new RecycleDataRequestMessage(b.readLong()); }
  public static void encodeSubmit(RecycleSubmitMessage m, WireBuffer b) { b.writeLong(m.requestId()); b.writeUuid(m.submissionId()); b.writeUtf(m.itemId(), EconomyNetworkLimits.MAX_RECYCLE_ITEM_ID_LENGTH); b.writeInt(m.amount()); }
  public static RecycleSubmitMessage decodeSubmit(WireBuffer b) { return new RecycleSubmitMessage(b.readLong(), b.readUuid(), b.readUtf(EconomyNetworkLimits.MAX_RECYCLE_ITEM_ID_LENGTH), b.readInt()); }
  public static void encodeResponse(RecycleDataResponseMessage m, WireBuffer b) {
    b.writeInt(m.kind().ordinal()); b.writeLong(m.requestId()); b.writeLong(m.serverNowMillis()); b.writeLong(m.cycleEndsAt()); b.writeUtf(m.errorKey(), EconomyNetworkLimits.MAX_RECYCLE_TEXT_LENGTH); b.writeInt(m.offers().size());
    for (RecycleOfferSnapshot o : m.offers()) { b.writeUtf(o.itemId(), EconomyNetworkLimits.MAX_RECYCLE_ITEM_ID_LENGTH); b.writeInt(o.baseUnitPrice()); b.writeInt(o.highUnitPrice()); b.writeInt(o.highQuotaRemaining()); b.writeInt(o.ownedCount()); b.writeInt(o.maxStackSize()); b.writeBoolean(o.fallbackToBaseWhenHighQuotaExhausted()); }
  }
  public static RecycleDataResponseMessage decodeResponse(WireBuffer b) {
    RecycleDataResponseKind kind = enumValue(b.readInt(), RecycleDataResponseKind.values(), "recycle response kind"); long id=b.readLong(), now=b.readLong(), end=b.readLong(); String error=b.readUtf(EconomyNetworkLimits.MAX_RECYCLE_TEXT_LENGTH); int size=b.readInt(); if(size<0||size>EconomyNetworkLimits.MAX_RECYCLE_OFFERS)throw new IllegalArgumentException("invalid recycle offer count: "+size);
    List<RecycleOfferSnapshot> offers=new ArrayList<>(size); for(int i=0;i<size;i++) offers.add(new RecycleOfferSnapshot(b.readUtf(EconomyNetworkLimits.MAX_RECYCLE_ITEM_ID_LENGTH),b.readInt(),b.readInt(),b.readInt(),b.readInt(),b.readInt(),b.readBoolean())); return new RecycleDataResponseMessage(kind,id,now,end,offers,error);
  }
  public static void encodeAction(RecycleActionResponseMessage m, WireBuffer b) { b.writeLong(m.requestId()); b.writeInt(m.status().ordinal()); b.writeInt(m.acceptedAmount()); b.writeInt(m.payout()); b.writeInt(m.highQuotaRemaining()); b.writeUtf(m.message(), EconomyNetworkLimits.MAX_RECYCLE_TEXT_LENGTH); }
  public static RecycleActionResponseMessage decodeAction(WireBuffer b) { return new RecycleActionResponseMessage(b.readLong(), enumValue(b.readInt(), RecycleActionStatus.values(), "recycle action status"), b.readInt(), b.readInt(), b.readInt(), b.readUtf(EconomyNetworkLimits.MAX_RECYCLE_TEXT_LENGTH)); }
  private static <T> T enumValue(int n,T[] values,String what){if(n<0||n>=values.length)throw new IllegalArgumentException("invalid "+what);return values[n];}
}

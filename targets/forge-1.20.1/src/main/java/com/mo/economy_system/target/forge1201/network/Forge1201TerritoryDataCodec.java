package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.network.TerritoryDataRequestMessage;
import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.network.TerritoryDataWireCodec;
import net.minecraft.network.FriendlyByteBuf;

/** Forge buffer binding; the shared codec is the only wire-field implementation. */
final class Forge1201TerritoryDataCodec {
  private Forge1201TerritoryDataCodec() {}

  public static void encodeRequest(TerritoryDataRequestMessage message, FriendlyByteBuf buffer) {
    TerritoryDataWireCodec.encodeRequest(message, buffer);
  }

  public static TerritoryDataRequestMessage decodeRequest(FriendlyByteBuf buffer) {
    return TerritoryDataWireCodec.decodeRequest(buffer);
  }

  public static void encodeResponse(TerritoryDataResponseMessage message, FriendlyByteBuf buffer) {
    TerritoryDataWireCodec.encodeResponse(message, buffer);
  }

  public static TerritoryDataResponseMessage decodeResponse(FriendlyByteBuf buffer) {
    return TerritoryDataWireCodec.decodeResponse(buffer);
  }
}

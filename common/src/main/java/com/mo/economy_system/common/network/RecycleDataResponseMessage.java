package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.List;
import java.util.Objects;

public record RecycleDataResponseMessage(RecycleDataResponseKind kind, long requestId,
                                         long serverNowMillis, long cycleEndsAt,
                                         List<RecycleOfferSnapshot> offers, String errorKey)
    implements EconomyNetworkMessage {
  public RecycleDataResponseMessage {
    Objects.requireNonNull(kind, "kind");
    if (requestId < 0 || serverNowMillis < 0 || cycleEndsAt < serverNowMillis) throw new IllegalArgumentException("invalid recycle metadata");
    offers = List.copyOf(Objects.requireNonNull(offers, "offers"));
    if (offers.size() > EconomyNetworkLimits.MAX_RECYCLE_OFFERS) throw new IllegalArgumentException("too many recycle offers");
    errorKey = Objects.requireNonNullElse(errorKey, "");
    if (errorKey.length() > EconomyNetworkLimits.MAX_RECYCLE_TEXT_LENGTH) throw new IllegalArgumentException("recycle error too long");
    if (kind == RecycleDataResponseKind.ERROR && errorKey.isBlank()) throw new IllegalArgumentException("error response requires key");
  }

  public static RecycleDataResponseMessage data(long requestId, long now, long cycleEndsAt,
                                                 List<RecycleOfferSnapshot> offers) {
    return new RecycleDataResponseMessage(RecycleDataResponseKind.DATA, requestId, now, cycleEndsAt, offers, "");
  }
  public static RecycleDataResponseMessage error(long requestId, long now, String key) {
    return new RecycleDataResponseMessage(RecycleDataResponseKind.ERROR, requestId, now, now, List.of(), key);
  }
}

package com.mo.economy_system.common.network;

public enum DeliveryBoxResponseKind {
  DATA("data"),
  ERROR("error");

  private final String id;

  DeliveryBoxResponseKind(String id) {
    this.id = id;
  }

  public String id() {
    return id;
  }

  public static DeliveryBoxResponseKind fromId(String id) {
    for (DeliveryBoxResponseKind value : values()) if (value.id.equals(id)) return value;
    throw new IllegalArgumentException("unknown delivery response kind: " + id);
  }
}

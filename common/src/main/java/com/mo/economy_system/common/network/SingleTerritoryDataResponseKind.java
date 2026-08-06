package com.mo.economy_system.common.network;

public enum SingleTerritoryDataResponseKind {
  DATA("data"),
  NOT_FOUND("not_found"),
  UNAUTHORIZED("unauthorized"),
  ERROR("error");

  private final String id;

  SingleTerritoryDataResponseKind(String id) {
    this.id = id;
  }

  public String id() {
    return id;
  }

  public static SingleTerritoryDataResponseKind fromId(String id) {
    for (SingleTerritoryDataResponseKind value : values()) if (value.id.equals(id)) return value;
    throw new IllegalArgumentException("unknown single territory response kind: " + id);
  }
}

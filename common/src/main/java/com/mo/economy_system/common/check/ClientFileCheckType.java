package com.mo.economy_system.common.check;

import java.util.Arrays;

public enum ClientFileCheckType {
  MODS("mods"),
  SHADERPACKS("shaderpacks"),
  RESOURCEPACKS("resourcepacks");

  private final String id;

  ClientFileCheckType(String id) {
    this.id = id;
  }

  public String id() {
    return id;
  }

  public static ClientFileCheckType fromId(String id) {
    if (id == null) throw new IllegalArgumentException("check type");
    return Arrays.stream(values())
        .filter(value -> value.id.equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("unknown check type"));
  }
}

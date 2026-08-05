package com.mo.economy_system.common.check;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.util.Objects;

public final class ClientFileCheckValidation {
  private ClientFileCheckValidation() {}

  public static String playerName(String value) {
    Objects.requireNonNull(value, "player name");
    if (value.isEmpty()
        || !value.equals(value.trim())
        || value.length() > EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH)
      throw new IllegalArgumentException("player name");
    return value;
  }

  public static String fileName(String value) {
    Objects.requireNonNull(value, "file name");
    if (value.isEmpty()
        || !value.equals(value.trim())
        || value.length() > EconomyNetworkLimits.MAX_CHECK_FILE_NAME_LENGTH
        || value.contains("/")
        || value.contains("\\")
        || value.contains("\0")
        || value.contains("..")
        || value.contains(":")) throw new IllegalArgumentException("file name");
    return value;
  }

  public static String errorCode(String value) {
    Objects.requireNonNull(value, "error code");
    if (value.isEmpty() || value.length() > 64 || !value.matches("[A-Z0-9_]+"))
      throw new IllegalArgumentException("error code");
    return value;
  }

  public static String resultJson(String value) {
    Objects.requireNonNull(value, "result JSON");
    if (value.length() > EconomyNetworkLimits.MAX_CHECK_RESULT_JSON_LENGTH)
      throw new IllegalArgumentException("result JSON too long");
    return value;
  }
}

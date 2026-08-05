package com.mo.economy_system.common.transfer;

import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.check.ClientFileCheckValidation;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class CheckedFileTransferValidation {
  private CheckedFileTransferValidation() {}

  public static String fileName(String value) {
    String checked = ClientFileCheckValidation.fileName(value);
    if (checked.length() > EconomyNetworkLimits.MAX_TRANSFER_FILE_NAME_CHARS
        || checked.chars().anyMatch(Character::isISOControl)
        || checked.equals(".") || checked.equals("..")) throw new IllegalArgumentException("file name");
    return checked;
  }

  public static String sha256(String value) {
    Objects.requireNonNull(value, "sha256");
    if (!value.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("sha256");
    return value;
  }

  public static String canonicalUuid(UUID value) {
    return Objects.requireNonNull(value, "uuid").toString().toLowerCase(Locale.ROOT);
  }

  public static UUID canonicalUuid(String value) {
    Objects.requireNonNull(value, "uuid");
    UUID parsed = UUID.fromString(value);
    if (!parsed.toString().equals(value)) throw new IllegalArgumentException("canonical uuid");
    return parsed;
  }

  public static int totalChunks(long bytes, int rawChunkBytes) {
    if (bytes < 0 || bytes > EconomyNetworkLimits.MAX_TRANSFER_FILE_BYTES
        || rawChunkBytes != EconomyNetworkLimits.TRANSFER_RAW_CHUNK_BYTES)
      throw new IllegalArgumentException("transfer bounds");
    return bytes == 0 ? 0 : Math.toIntExact((bytes + rawChunkBytes - 1) / rawChunkBytes);
  }

  public static ClientFileCheckType type(ClientFileCheckType type) {
    return Objects.requireNonNull(type, "check type");
  }
}

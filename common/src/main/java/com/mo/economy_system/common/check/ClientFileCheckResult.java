package com.mo.economy_system.common.check;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record ClientFileCheckResult(
    int schemaVersion,
    ClientFileCheckStatus status,
    ClientFileCheckType checkType,
    List<ClientFileCheckEntry> files,
    List<ClientFileCheckSkippedEntry> skipped,
    String errorCode) {
  public static final int SCHEMA_VERSION = 1;

  public ClientFileCheckResult {
    if (schemaVersion != SCHEMA_VERSION) throw new IllegalArgumentException("schema version");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(checkType, "checkType");
    files = List.copyOf(Objects.requireNonNull(files, "files"));
    skipped = List.copyOf(Objects.requireNonNull(skipped, "skipped"));
    if (files.size() > EconomyNetworkLimits.MAX_CHECK_FILES
        || skipped.size() > EconomyNetworkLimits.MAX_CHECK_SKIPPED_FILES)
      throw new IllegalArgumentException("result entries");
    HashSet<String> names = new HashSet<>();
    for (ClientFileCheckEntry entry : files)
      if (!names.add(entry.fileName())) throw new IllegalArgumentException("duplicate file name");
    for (ClientFileCheckSkippedEntry entry : skipped)
      if (!names.add(entry.fileName())) throw new IllegalArgumentException("duplicate result name");
    if (status == ClientFileCheckStatus.DECLINED && (!files.isEmpty() || !skipped.isEmpty()))
      throw new IllegalArgumentException("declined entries");
    if (status == ClientFileCheckStatus.FAILED && !files.isEmpty())
      throw new IllegalArgumentException("failed files");
    if (status == ClientFileCheckStatus.FAILED || status == ClientFileCheckStatus.TRUNCATED) {
      errorCode = ClientFileCheckValidation.errorCode(errorCode);
    } else if (errorCode != null) {
      throw new IllegalArgumentException("unexpected error code");
    }
  }

  public static ClientFileCheckResult declined(ClientFileCheckType type) {
    return new ClientFileCheckResult(
        SCHEMA_VERSION, ClientFileCheckStatus.DECLINED, type, List.of(), List.of(), null);
  }

  public static ClientFileCheckResult failed(ClientFileCheckType type, String code) {
    return new ClientFileCheckResult(
        SCHEMA_VERSION, ClientFileCheckStatus.FAILED, type, List.of(), List.of(), code);
  }
}

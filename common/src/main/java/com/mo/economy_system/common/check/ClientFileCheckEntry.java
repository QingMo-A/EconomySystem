package com.mo.economy_system.common.check;

import java.util.Objects;

public record ClientFileCheckEntry(String fileName, long size, String sha256) {
  public ClientFileCheckEntry {
    fileName = ClientFileCheckValidation.fileName(fileName);
    if (size < 0) throw new IllegalArgumentException("size");
    Objects.requireNonNull(sha256, "sha256");
    if (!sha256.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("sha256");
  }
}

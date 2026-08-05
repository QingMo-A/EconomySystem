package com.mo.economy_system.common.check;

public record ClientFileCheckSkippedEntry(String fileName, String reason) {
  public ClientFileCheckSkippedEntry {
    fileName = ClientFileCheckValidation.fileName(fileName);
    reason = ClientFileCheckValidation.errorCode(reason);
  }
}

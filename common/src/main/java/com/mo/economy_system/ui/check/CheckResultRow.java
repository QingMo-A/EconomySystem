package com.mo.economy_system.ui.check;

import java.util.Objects;

/** One semantic result row. reasonKey is a translation key, never target-local text. */
public record CheckResultRow(String fileName, String reasonKey, boolean skipped) {
  public CheckResultRow {
    fileName = Objects.requireNonNullElse(fileName, "");
    reasonKey = Objects.requireNonNullElse(reasonKey, "screen.check_result.unknown");
  }
}

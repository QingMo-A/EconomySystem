package com.mo.economy_system.ui.balance;

import com.mo.economy_system.core.economy_system.BalanceLogEntry;
import java.util.Objects;

public record BalanceLogRow(BalanceLogEntry entry) {
  public BalanceLogRow { Objects.requireNonNull(entry, "entry"); }
}

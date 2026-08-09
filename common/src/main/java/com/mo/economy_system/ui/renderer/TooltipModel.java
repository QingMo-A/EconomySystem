package com.mo.economy_system.ui.renderer;

import java.util.List;
import java.util.Objects;

public record TooltipModel(List<TooltipLine> lines) {
  public TooltipModel {
    lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
    if (lines.isEmpty()) throw new IllegalArgumentException("tooltip cannot be empty");
  }
}

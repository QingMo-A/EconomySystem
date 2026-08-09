package com.mo.economy_system.ui.renderer;

import java.util.List;
import java.util.Objects;

/** Loader-neutral tooltip line; item names are resolved by the target renderer. */
public sealed interface TooltipLine
    permits TooltipLine.Literal, TooltipLine.Translated, TooltipLine.Item {
  record Literal(String text) implements TooltipLine {
    public Literal {
      Objects.requireNonNull(text, "text");
    }
  }

  record Translated(String key, List<String> arguments) implements TooltipLine {
    public Translated {
      if (key == null || key.isBlank()) throw new IllegalArgumentException("tooltip key");
      arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
    }
  }

  /** The resolved item display name is prepended to {@code arguments}. */
  record Item(String key, String itemId, List<String> arguments) implements TooltipLine {
    public Item {
      if (key == null || key.isBlank()) throw new IllegalArgumentException("tooltip key");
      if (itemId == null || itemId.isBlank()) throw new IllegalArgumentException("itemId");
      arguments = List.copyOf(Objects.requireNonNull(arguments, "arguments"));
    }
  }
}

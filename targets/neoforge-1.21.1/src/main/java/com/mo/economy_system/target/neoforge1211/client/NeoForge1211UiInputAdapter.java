package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.ui.theme.EconomyUiTheme;
import com.mo.economy_system.ui.theme.UiInputStyle;
import net.minecraft.client.gui.components.EditBox;

/** Applies the common input semantic policy to NeoForge's native EditBox. */
public final class NeoForge1211UiInputAdapter {
  private NeoForge1211UiInputAdapter() {}

  public static void apply(EditBox box) {
    apply(box, EconomyUiTheme.INPUT_STYLE);
  }

  public static void apply(EditBox box, UiInputStyle style) {
    if (box == null || style == null) throw new IllegalArgumentException("input style");
    box.setBordered(!style.hideNativeBorder());
    box.setTextColor(style.textColor());
    box.setTextColorUneditable(style.disabledTextColor());
  }
}

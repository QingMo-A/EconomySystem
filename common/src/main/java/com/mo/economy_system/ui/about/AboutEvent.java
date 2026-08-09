package com.mo.economy_system.ui.about;

import java.util.Objects;

public sealed interface AboutEvent permits AboutEvent.Initialize, AboutEvent.ActionClicked {
  record Initialize() implements AboutEvent {}
  record ActionClicked(AboutAction action) implements AboutEvent {
    public ActionClicked { Objects.requireNonNull(action, "action"); }
  }
}

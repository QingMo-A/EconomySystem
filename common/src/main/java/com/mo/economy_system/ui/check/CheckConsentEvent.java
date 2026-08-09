package com.mo.economy_system.ui.check;

/** Input translated by a target Screen into the common consent state machine. */
public sealed interface CheckConsentEvent permits CheckConsentEvent.ActionClicked {
  record ActionClicked(CheckConsentAction action) implements CheckConsentEvent {}
}

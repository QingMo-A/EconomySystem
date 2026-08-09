package com.mo.economy_system.ui.transfer;

import com.mo.economy_system.ui.core.ScreenState;
import java.util.Objects;
import java.util.Set;

/** Immutable rendering model for an incoming artifact decision or terminal transfer message. */
public record TransferResultState(
    boolean terminal,
    String sourceName,
    String checkTypeId,
    String fileName,
    long byteSize,
    String sha256,
    String artifactStateKey,
    String terminalStatusKey,
    String terminalErrorKey,
    String actionErrorKey,
    ScreenState screenState,
    Set<TransferResultAction> actions) {
  public TransferResultState {
    sourceName = Objects.requireNonNullElse(sourceName, "");
    checkTypeId = Objects.requireNonNullElse(checkTypeId, "");
    fileName = Objects.requireNonNullElse(fileName, "");
    if (byteSize < 0) throw new IllegalArgumentException("byte size");
    sha256 = Objects.requireNonNullElse(sha256, "");
    artifactStateKey = Objects.requireNonNullElse(artifactStateKey, "");
    terminalStatusKey = Objects.requireNonNullElse(terminalStatusKey, "");
    terminalErrorKey = Objects.requireNonNullElse(terminalErrorKey, "");
    actionErrorKey = Objects.requireNonNullElse(actionErrorKey, "");
    screenState = Objects.requireNonNull(screenState, "screenState");
    actions = Set.copyOf(Objects.requireNonNull(actions, "actions"));
    if (terminal && (terminalStatusKey.isBlank() || terminalErrorKey.isBlank())) {
      throw new IllegalArgumentException("terminal content");
    }
    if (!terminal && (sourceName.isBlank() || checkTypeId.isBlank() || fileName.isBlank())) {
      throw new IllegalArgumentException("artifact content");
    }
  }

  public static TransferResultState artifact(
      String sourceName,
      String checkTypeId,
      String fileName,
      long byteSize,
      String sha256,
      String artifactStateKey) {
    return new TransferResultState(
        false,
        sourceName,
        checkTypeId,
        fileName,
        byteSize,
        sha256,
        artifactStateKey,
        "",
        "",
        "",
        ScreenState.READY,
        Set.of(TransferResultAction.SAVE, TransferResultAction.DISCARD));
  }

  public static TransferResultState terminal(String statusKey, String errorKey) {
    return new TransferResultState(
        true,
        "",
        "",
        "",
        0,
        "",
        "",
        statusKey,
        errorKey,
        "",
        ScreenState.READY,
        Set.of(TransferResultAction.CLOSE));
  }

  public boolean can(TransferResultAction action) {
    return actions.contains(action);
  }

  TransferResultState withActionError(String errorKey) {
    return new TransferResultState(
        terminal,
        sourceName,
        checkTypeId,
        fileName,
        byteSize,
        sha256,
        artifactStateKey,
        terminalStatusKey,
        terminalErrorKey,
        errorKey,
        ScreenState.ERROR,
        actions);
  }

  TransferResultState finished() {
    return new TransferResultState(
        terminal,
        sourceName,
        checkTypeId,
        fileName,
        byteSize,
        sha256,
        artifactStateKey,
        terminalStatusKey,
        terminalErrorKey,
        "",
        ScreenState.IDLE,
        Set.of());
  }

  TransferResultState withArtifactStateKey(String stateKey) {
    if (terminal || stateKey == null || stateKey.isBlank() || artifactStateKey.equals(stateKey)) {
      return this;
    }
    return new TransferResultState(
        false,
        sourceName,
        checkTypeId,
        fileName,
        byteSize,
        sha256,
        stateKey,
        "",
        "",
        actionErrorKey,
        screenState,
        actions);
  }
}

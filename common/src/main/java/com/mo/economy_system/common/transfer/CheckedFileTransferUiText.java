package com.mo.economy_system.common.transfer;

/** Stable translation-key mapping shared by both checked-file transfer adapters. */
public final class CheckedFileTransferUiText {
  private CheckedFileTransferUiText() {}

  /** Unknown or provider-supplied text fails closed to one bounded generic message. */
  public static String errorKey(String errorCode) {
    if (errorCode == null) return "message.transfer.invalid_server_response";
    return switch (errorCode) {
      case "ARTIFACT_PENDING" -> "message.transfer.artifact_pending";
      case "TEMP_DIRECTORY_PROVIDER_UNSAFE", "DIRECTORY_PROVIDER_UNSAFE", "DIRECTORY_CHANGED" ->
          "message.transfer.temp_directory_provider_unsafe";
      case "STALE_SESSION" -> "message.transfer.stale_session";
      case "DECLINED" -> "message.transfer.declined";
      case "NOT_FOUND" -> "message.transfer.not_found";
      case "STALE_CHECK", "FILE_NOT_IN_CHECK_RESULT" -> "message.transfer.stale_check";
      case "FILE_CHANGED_RECHECK_REQUIRED" ->
          "message.transfer.file_changed_recheck_required";
      case "TEMP_STORAGE_LIMIT" -> "message.transfer.temp_storage_limit";
      case "TRANSFER_EXPIRED", "REQUEST_EXPIRED" -> "message.transfer.expired";
      case "SAVE_NAME_EXHAUSTED" -> "message.transfer.save_name_exhausted";
      case "SAVE_PARENT_UNSAFE" -> "message.transfer.save_parent_unsafe";
      case "SOURCE_MISSING", "NOT_REGULAR_FILE", "SYMLINK" ->
          "message.transfer.source_missing";
      case "MOVE_FAILED" -> "message.transfer.move_failed";
      case "NOT_PENDING" -> "message.transfer.not_pending";
      case "SNAPSHOT_FAILED" -> "message.transfer.snapshot_failed";
      case "SNAPSHOT_TIMEOUT" -> "message.transfer.snapshot_timeout";
      case "SNAPSHOT_CANCELLED" -> "message.transfer.snapshot_cancelled";
      case "TRANSFER_INTERRUPTED" -> "message.transfer.interrupted";
      case "CONSENT_BUSY" -> "message.transfer.consent_busy";
      case "FILE_TOO_LARGE" -> "message.transfer.file_too_large";
      case "INVALID_SERVER_RESPONSE", "INVALID_METADATA", "INVALID_CHUNK", "SIZE_MISMATCH",
          "HASH_MISMATCH", "WRITE_FAILED" -> "message.transfer.invalid_server_response";
      default -> "message.transfer.invalid_server_response";
    };
  }

  public static String terminalStatusKey(CheckedFileTransferControlStatus status) {
    if (status == null) return "message.transfer.status.failed";
    return switch (status) {
      case DECLINED -> "message.transfer.status.declined";
      case NOT_FOUND -> "message.transfer.status.not_found";
      case FAILED -> "message.transfer.status.failed";
      case READY, COMPLETE -> "message.transfer.status.failed";
    };
  }

  public static String artifactStateKey(CheckedFileTransferReceivedArtifact.State state) {
    if (state == null) return "message.transfer.state.pending";
    return switch (state) {
      case PENDING_DECISION -> "message.transfer.state.pending";
      case SAVED -> "message.transfer.state.saved";
      case DISCARDED -> "message.transfer.state.discarded";
    };
  }

  public static String saveErrorKey(CheckedFileTransferSaveService.ResultCode code) {
    if (code == null) return "message.transfer.move_failed";
    return switch (code) {
      case SAVE_NAME_EXHAUSTED -> "message.transfer.save_name_exhausted";
      case SAVE_PARENT_UNSAFE -> "message.transfer.save_parent_unsafe";
      case SOURCE_MISSING -> "message.transfer.source_missing";
      case MOVE_FAILED -> "message.transfer.move_failed";
      case NOT_PENDING -> "message.transfer.not_pending";
      case INVALID_FILE_NAME -> "message.transfer.invalid_file";
      case SAVED -> "message.transfer.not_pending";
    };
  }

  public static String discardErrorKey(CheckedFileTransferReceivedArtifact.DiscardResult code) {
    if (code == null) return "message.transfer.delete_failed";
    return switch (code) {
      case DELETE_FAILED -> "message.transfer.delete_failed";
      case NOT_PENDING, DISCARDED -> "message.transfer.not_pending";
    };
  }
}

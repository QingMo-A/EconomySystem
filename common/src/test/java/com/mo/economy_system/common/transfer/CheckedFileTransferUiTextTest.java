package com.mo.economy_system.common.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CheckedFileTransferUiTextTest {
  @Test
  void requiredTerminalCodesHaveStableBoundedTranslations() {
    assertEquals("message.transfer.artifact_pending",
        CheckedFileTransferUiText.errorKey("ARTIFACT_PENDING"));
    assertEquals("message.transfer.temp_directory_provider_unsafe",
        CheckedFileTransferUiText.errorKey("TEMP_DIRECTORY_PROVIDER_UNSAFE"));
    assertEquals("message.transfer.stale_session",
        CheckedFileTransferUiText.errorKey("STALE_SESSION"));
    assertEquals("message.transfer.declined", CheckedFileTransferUiText.errorKey("DECLINED"));
    assertEquals("message.transfer.not_found", CheckedFileTransferUiText.errorKey("NOT_FOUND"));
    assertEquals("message.transfer.stale_check", CheckedFileTransferUiText.errorKey("STALE_CHECK"));
    assertEquals("message.transfer.file_changed_recheck_required",
        CheckedFileTransferUiText.errorKey("FILE_CHANGED_RECHECK_REQUIRED"));
    assertEquals("message.transfer.temp_storage_limit",
        CheckedFileTransferUiText.errorKey("TEMP_STORAGE_LIMIT"));
    assertEquals("message.transfer.expired",
        CheckedFileTransferUiText.errorKey("TRANSFER_EXPIRED"));
    assertEquals("message.transfer.save_name_exhausted",
        CheckedFileTransferUiText.errorKey("SAVE_NAME_EXHAUSTED"));
    assertEquals("message.transfer.save_parent_unsafe",
        CheckedFileTransferUiText.errorKey("SAVE_PARENT_UNSAFE"));
    assertEquals("message.transfer.source_missing",
        CheckedFileTransferUiText.errorKey("SOURCE_MISSING"));
    assertEquals("message.transfer.move_failed",
        CheckedFileTransferUiText.errorKey("MOVE_FAILED"));
    assertEquals("message.transfer.invalid_server_response",
        CheckedFileTransferUiText.errorKey("INVALID_SERVER_RESPONSE"));
  }

  @Test
  void unknownProviderTextFailsClosed() {
    assertEquals("message.transfer.invalid_server_response",
        CheckedFileTransferUiText.errorKey("C:\\secret\\provider-message"));
    assertEquals("message.transfer.invalid_server_response", CheckedFileTransferUiText.errorKey(null));
  }
}

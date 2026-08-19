package com.mo.economy_system.common.mail;

/**
 * Defines who is allowed to create mailbox messages.
 * Actual item transfer continues through DeliveryBoxClaimService.
 */
public enum MailAttachmentPolicy {
  NONE,
  EXISTING_DELIVERY_ENTRY,
  PLAYER_GIFT,
  SYSTEM_REWARD
}

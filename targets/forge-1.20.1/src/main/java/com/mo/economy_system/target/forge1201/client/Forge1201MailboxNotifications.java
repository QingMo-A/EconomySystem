package com.mo.economy_system.target.forge1201.client;

import com.mo.economy_system.common.network.MailboxNotificationMessage;
import com.mo.economy_system.ui.delivery.MailboxDisplayKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/** Client-only vanilla-style feedback for newly delivered mailbox entries. */
public final class Forge1201MailboxNotifications {
  private Forge1201MailboxNotifications() {}

  public static void show(MailboxNotificationMessage message) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft == null) return;
    Component sender = message.type() == com.mo.economy_system.common.mail.MailType.PLAYER
        && !message.senderName().isBlank()
        ? Component.literal(message.senderName())
        : Component.translatable(MailboxDisplayKeys.senderKey(message.type()));
    Component subject = message.subject().isBlank()
        ? Component.translatable(MailboxDisplayKeys.subjectKey(message.type()))
        : Component.literal(message.subject());
    Component body = Component.translatable("message.mailbox.toast.body", sender, subject);
    SystemToast.add(minecraft.getToasts(), SystemToast.SystemToastIds.PERIODIC_NOTIFICATION,
        Component.translatable("message.mailbox.toast.title"), body);
    minecraft.getSoundManager().play(
        SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
  }
}

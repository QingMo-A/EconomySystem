package com.mo.economy_system.target.neoforge1211.commission;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Source-level contract for NeoForge commission event identity and administrator refresh wiring. */
class NeoForge1211CommissionContractTest {
  @Test
  void deathHookPassesTheKilledEntityIdentityToTheCommissionRuntime() throws Exception {
    String runtime = read(repositoryRoot().resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/commission/NeoForge1211CommissionRuntime.java"));
    String deathHook = read(repositoryRoot().resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/events/EconomySystem_EventHandler.java"));

    assertTrue(runtime.contains("CommissionEventIds.entityKill(player.getUUID(), killedEntityId)"));
    assertTrue(runtime.contains("player.getUUID(), c.commissionId(), submissionId, 1"));
    assertTrue(runtime.contains("onKill(ServerPlayer player, LivingEntity entity, UUID killedEntityId)"));
    assertTrue(runtime.contains("onKill(ServerPlayer player, LivingEntity entity)"));
    assertTrue(deathHook.contains("onKill(player, mob, mob.getUUID())"));
  }

  @Test
  void administratorPlayerRefreshUsesTheForceRefreshPath() throws Exception {
    String runtime = read(repositoryRoot().resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/commission/NeoForge1211CommissionRuntime.java"));
    String commands = read(repositoryRoot().resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/commission/NeoForge1211CommissionCommands.java"));

    assertTrue(runtime.contains("service(player.server).forceRefresh"));
    assertTrue(commands.contains("var view = NeoForge1211CommissionRuntime.forceRefresh(target)"));
  }

  @Test
  void catalogLoaderValidatesMinecraftTargetRegistriesBeforeActivation() throws Exception {
    String runtime = read(repositoryRoot().resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/commission/NeoForge1211CommissionRuntime.java"));

    assertTrue(runtime.contains("validateCatalog(catalog, path)"));
    assertTrue(runtime.contains("template.type() != CommissionType.ITEM_DELIVERY"));
    assertTrue(runtime.contains("template.type() != CommissionType.ENTITY_KILL"));
    assertTrue(runtime.contains("BuiltInRegistries.ITEM.containsKey(id)"));
    assertTrue(runtime.contains("item == null || item == Items.AIR"));
    assertTrue(runtime.contains("BuiltInRegistries.ENTITY_TYPE.containsKey(id)"));
    assertTrue(runtime.contains("target=NeoForge 1.21.1"));
    assertTrue(runtime.contains("template="));
    assertTrue(runtime.contains("pool="));
    assertTrue(runtime.contains("targetId="));
  }

  @Test
  void rewardPersistenceRejectsIdAndIdempotencyKeyCollisions() throws Exception {
    String savedData = read(repositoryRoot().resolve(
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/commission/NeoForge1211CommissionSavedData.java"));

    assertTrue(savedData.contains("private final Map<String, UUID> rewardsByKey"));
    assertTrue(savedData.contains("reward id is already used by another idempotency key"));
    assertTrue(savedData.contains("reward idempotency key cannot change"));
    assertTrue(savedData.contains("idempotency key is already used by another reward"));
    assertTrue(savedData.contains("duplicate commission reward id"));
    assertTrue(savedData.contains("duplicate commission reward key"));
    assertTrue(savedData.contains("rewardsByKey.put(reward.idempotencyKey(), reward.rewardRecordId())"));
  }

  private static String read(Path path) throws Exception {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  private static Path repositoryRoot() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null && !Files.exists(current.resolve("settings.gradle"))) {
      current = current.getParent();
    }
    if (current == null) throw new IllegalStateException("repository root not found");
    return current;
  }
}

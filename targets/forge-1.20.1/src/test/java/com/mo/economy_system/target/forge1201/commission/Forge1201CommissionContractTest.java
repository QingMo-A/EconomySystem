package com.mo.economy_system.target.forge1201.commission;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Source-level contract for Forge commission event identity and administrator refresh wiring. */
class Forge1201CommissionContractTest {
  @Test
  void deathHookPassesTheKilledEntityIdentityToTheCommissionRuntime() throws Exception {
    String runtime = read(repositoryRoot().resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/commission/Forge1201CommissionRuntime.java"));
    String deathHook = read(repositoryRoot().resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/reward/Forge1201RewardEvents.java"));

    assertTrue(runtime.contains("CommissionEventIds.entityKill(player.getUUID(), killedEntityId)"));
    assertTrue(runtime.contains("player.getUUID(), commissionId, submissionId, 1, now()"));
    assertTrue(runtime.contains("handleEntityKill(ServerPlayer player, String entityId, UUID killedEntityId)"));
    assertTrue(runtime.contains("handleEntityKill(ServerPlayer player, String entityId)"));
    assertTrue(deathHook.contains(
        "handleEntityKill(player, entityId.toString(), mob.getUUID())"));
  }

  @Test
  void administratorPlayerRefreshUsesTheForceRefreshPath() throws Exception {
    String runtime = read(repositoryRoot().resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/commission/Forge1201CommissionRuntime.java"));
    String commands = read(repositoryRoot().resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/Forge1201EconomyCommands.java"));

    assertTrue(runtime.contains("service(player.serverLevel(), data).forceRefresh"));
    assertTrue(commands.contains("var view = Forge1201CommissionRuntime.forceRefresh(target)"));
  }

  @Test
  void catalogLoaderValidatesMinecraftTargetRegistriesBeforeActivation() throws Exception {
    String runtime = read(repositoryRoot().resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/commission/Forge1201CommissionRuntime.java"));

    assertTrue(runtime.contains("validateCatalog(catalog, path)"));
    assertTrue(runtime.contains("template.type() != CommissionType.ITEM_DELIVERY"));
    assertTrue(runtime.contains("template.type() != CommissionType.ENTITY_KILL"));
    assertTrue(runtime.contains("BuiltInRegistries.ITEM.containsKey(id)"));
    assertTrue(runtime.contains("item == null || item == Items.AIR"));
    assertTrue(runtime.contains("BuiltInRegistries.ENTITY_TYPE.containsKey(id)"));
    assertTrue(runtime.contains("target=Forge 1.20.1"));
    assertTrue(runtime.contains("template="));
    assertTrue(runtime.contains("pool="));
    assertTrue(runtime.contains("targetId="));
  }

  @Test
  void publicCommissionExpiryRunsFromServerTickWithoutOnlinePlayers() throws Exception {
    String runtime = read(repositoryRoot().resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/commission/Forge1201CommissionRuntime.java"));
    String events = read(repositoryRoot().resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/Forge1201ServerEvents.java"));

    assertTrue(runtime.contains("publicService(level, data(level)).expireDue(now())"));
    assertTrue(events.contains("Forge1201CommissionRuntime.expirePublic(event.getServer())"));
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

package com.mo.economy_system.common.territory;

import static com.mo.economy_system.common.territory.TerritoryManagementTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.SingleTerritoryDataRequestMessage;
import com.mo.economy_system.common.network.SingleTerritoryDataResponseKind;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SingleTerritoryQueryServiceTest {
  @Test
  void onlyCurrentOwnerReceivesFullSnapshot() {
    SingleTerritoryDataRequestMessage request =
        new SingleTerritoryDataRequestMessage(TERRITORY, 5);
    var response = SingleTerritoryQueryService.query(request, OWNER, id -> owned());
    assertEquals(SingleTerritoryDataResponseKind.DATA, response.kind());
    assertEquals(owned(), response.territory().orElseThrow());
    assertEquals(
        SingleTerritoryDataResponseKind.UNAUTHORIZED,
        SingleTerritoryQueryService.query(request, MEMBER, id -> owned()).kind());
  }

  @Test
  void missingAndRepositoryFailureAreDistinctBoundedResponses() {
    SingleTerritoryDataRequestMessage request =
        new SingleTerritoryDataRequestMessage(TERRITORY, 6);
    assertEquals(
        SingleTerritoryDataResponseKind.NOT_FOUND,
        SingleTerritoryQueryService.query(request, OWNER, id -> null).kind());
    assertEquals(
        SingleTerritoryDataResponseKind.ERROR,
        SingleTerritoryQueryService.query(
                request, OWNER, id -> { throw new IllegalStateException("storage"); })
            .kind());
  }
}

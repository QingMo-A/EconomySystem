package com.mo.economy_system.common.market;

import java.util.UUID;

public record DemandOrderDeliveryFailure(
    UUID tradeId,
    UUID supplierId,
    UUID requesterId,
    String stage,
    DemandOrderDeliveryResult result,
    MarketMutationState mutationState,
    DemandDeliveryTransitionStatus transitionStatus,
    Boolean inventoryRemovalRestored,
    Boolean inventoryRollbackSucceeded,
    Boolean paymentCreditSucceeded,
    Boolean paymentReversalSucceeded,
    RuntimeException primaryError,
    RuntimeException inventoryError,
    RuntimeException paymentError,
    RuntimeException repositoryError) {}

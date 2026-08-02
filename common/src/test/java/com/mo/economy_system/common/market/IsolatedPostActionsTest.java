package com.mo.economy_system.common.market;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class IsolatedPostActionsTest {
    @Test void actionAndFailureSinkFailuresDoNotStopLaterActions(){List<String> events=new ArrayList<>();IsolatedPostActions.runAll(List.of(new IsolatedPostActions.NamedAction("broadcast",()->{events.add("broadcast");throw new IllegalStateException();}),new IsolatedPostActions.NamedAction("feedback",()->events.add("feedback")),new IsolatedPostActions.NamedAction("notice",()->events.add("notice"))),(stage,error)->{events.add(stage+"-failed");throw new IllegalStateException();});assertEquals(List.of("broadcast","broadcast-failed","feedback","notice"),events);}
    @Test void mutationStatesDefineInvalidation(){assertFalse(MarketMutationState.UNCHANGED.requiresInvalidation());assertTrue(MarketMutationState.CHANGED.requiresInvalidation());assertTrue(MarketMutationState.UNKNOWN.requiresInvalidation());}
}

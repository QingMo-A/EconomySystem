package com.mo.economy_system.common.market;

import java.util.List;
import java.util.Objects;

public final class IsolatedPostActions {
    private IsolatedPostActions() {}

    public static void runAll(List<NamedAction> actions, FailureSink failureSink) {
        Objects.requireNonNull(actions, "actions");
        Objects.requireNonNull(failureSink, "failureSink");
        for (NamedAction namedAction : List.copyOf(actions)) {
            Objects.requireNonNull(namedAction, "namedAction");
            try {
                namedAction.action().run();
            } catch (RuntimeException exception) {
                try {
                    failureSink.failed(namedAction.stage(), exception);
                } catch (RuntimeException ignored) {
                    // Reporting must never prevent later actions.
                }
            }
        }
    }

    public record NamedAction(String stage, Runnable action) {
        public NamedAction {
            Objects.requireNonNull(stage, "stage");
            Objects.requireNonNull(action, "action");
            if (stage.isBlank()) throw new IllegalArgumentException("stage is blank");
        }
    }

    @FunctionalInterface
    public interface FailureSink {
        void failed(String stage, RuntimeException exception);
    }
}

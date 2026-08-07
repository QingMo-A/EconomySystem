package com.mo.economy_system.ui.core;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import org.junit.jupiter.api.Test;

class AbstractEconomyScreenControllerTest {
    @Test
    void navigationIsOneShotAndStateIsControlled() {
        Controller controller = new Controller();
        assertEquals("initial", controller.state());
        controller.goMarket();
        assertEquals(new UiNavigation.Route(EconomyUiRoute.MARKET), controller.pollNavigation().orElseThrow());
        assertTrue(controller.pollNavigation().isEmpty());
    }

    private static final class Controller extends AbstractEconomyScreenController<String, UiEvent> {
        private Controller() {
            super("initial");
        }

        private void goMarket() {
            navigate(new UiNavigation.Route(EconomyUiRoute.MARKET));
        }

        @Override
        public void handle(UiEvent event) {
        }
    }
}

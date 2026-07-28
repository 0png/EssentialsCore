package dev.zeropng.essentialscore.tpa;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TpaChatCommandTest {
    @Test
    void clickableButtonsUseThePluginOwnedEcCommandAndRequesterUuid() {
        UUID requester = UUID.fromString("7e426efa-6477-4bf0-a47b-ae927d21ae73");
        assertEquals("/ec tpaccept " + requester, TpaManager.internalCommand(true, requester));
        assertEquals("/ec tpdeny " + requester, TpaManager.internalCommand(false, requester));
    }
}

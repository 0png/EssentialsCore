package dev.zeropng.essentialscore.rank;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankDisplayTest {
    @Test
    void chatPlacesRankBeforePlayerName() {
        RankData rank = new RankData("vip", "VIP", "<aqua>[VIP]</aqua> ", "#55FFFF");
        Component formatted = RankDisplayListener.format(rank, Component.text("Steve"), Component.text("Hello"));
        assertEquals("[VIP] Steve: Hello", PlainTextComponentSerializer.plainText().serialize(formatted));
    }

    @Test
    void scoreboardTeamNamesAreStableShortAndRankSpecific() {
        String vip = NameTagManager.teamName("vip");
        assertEquals(vip, NameTagManager.teamName("vip"));
        assertNotEquals(vip, NameTagManager.teamName("member"));
        assertTrue(vip.startsWith("ec_"));
        assertTrue(vip.length() <= 16);
    }
}

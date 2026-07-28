package dev.zeropng.essentialscore.rank;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RankValidationTest {
    @Test
    void validatesIdsAndNames() {
        assertTrue(RankManager.validId("senior_admin-2"));
        assertFalse(RankManager.validId("Owner"));
        assertFalse(RankManager.validId("has space"));
        assertTrue(RankManager.validName("伺服器管理員"));
        assertFalse(RankManager.validName(""));
    }

    @Test
    void validatesMiniMessageAndColors() {
        assertTrue(RankManager.validPrefix("<gradient:red:gold>[VIP]</gradient> "));
        assertFalse(RankManager.validPrefix("<red>broken"));
        assertTrue(RankManager.validColor("dark_red"));
        assertTrue(RankManager.validColor("#12ABEF"));
        assertFalse(RankManager.validColor("ultraviolet"));
    }
}

package dev.zeropng.essentialscore.home;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HomeNameTest {
    @Test
    void normalizesWhitespaceCaseAndUnicode() {
        assertEquals("我的 家", HomeManager.displayName("  我的   家 "));
        assertEquals("spawn point", HomeManager.normalize(" Spawn   Point "));
    }

    @Test
    void acceptsConfiguredCharacters() {
        assertTrue(HomeManager.validCharacters("基地 2-A_區"));
        assertFalse(HomeManager.validCharacters("bad.name"));
        assertFalse(HomeManager.validCharacters("<red>home"));
        assertFalse(HomeManager.validCharacters("a".repeat(25)));
        assertFalse(HomeManager.validCharacters(""));
    }
}

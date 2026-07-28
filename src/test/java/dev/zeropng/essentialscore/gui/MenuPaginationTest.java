package dev.zeropng.essentialscore.gui;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MenuPaginationTest {
    @Test
    void clampsPagesAndSlicesAtFortyFive() {
        var values = IntStream.range(0, 100).boxed().toList();
        assertEquals(0, MenuManager.page(-3, values.size()));
        assertEquals(2, MenuManager.page(8, values.size()));
        assertEquals(45, MenuManager.pageSlice(values, 0).size());
        assertEquals(45, MenuManager.pageSlice(values, 1).size());
        assertEquals(10, MenuManager.pageSlice(values, 2).size());
    }
}

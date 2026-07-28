package dev.zeropng.essentialscore.trash;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrashHolderTest {
    @Test
    void canOnlyBeHandledOnceByTheManager() {
        TrashHolder holder = new TrashHolder(UUID.randomUUID());
        assertFalse(holder.handled());
        holder.markHandled();
        assertTrue(holder.handled());
    }
}

package dev.zeropng.essentialscore.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AtomicFilesTest {
    @TempDir Path directory;

    @Test
    void replacesContentAndLeavesNoTemporaryFile() throws Exception {
        Path target = directory.resolve("data.yml");
        AtomicFiles.writeString(target, "first");
        AtomicFiles.writeString(target, "second");
        assertEquals("second", Files.readString(target));
        assertFalse(Files.exists(directory.resolve("data.yml.tmp")));
    }
}

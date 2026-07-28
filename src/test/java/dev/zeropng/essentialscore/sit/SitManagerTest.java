package dev.zeropng.essentialscore.sit;

import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Slab;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SitManagerTest {
    @Test
    void acceptsOnlySingleSlabsAndUsesTheirActualSurfaceHeight() {
        assertTrue(SitManager.isSingleSlab(Slab.Type.BOTTOM));
        assertTrue(SitManager.isSingleSlab(Slab.Type.TOP));
        assertFalse(SitManager.isSingleSlab(Slab.Type.DOUBLE));
        assertEquals(0.5D, SitManager.slabSurfaceHeight(Slab.Type.BOTTOM));
        assertEquals(1.0D, SitManager.slabSurfaceHeight(Slab.Type.TOP));
        assertEquals(100.3D, SitManager.seatEntityY(100, 0.5D));
        assertEquals(100.8D, SitManager.seatEntityY(100, 1.0D));
    }

    @Test
    void convertsCardinalFacingToBukkitYaw() {
        assertEquals(0.0F, SitManager.yawFor(BlockFace.SOUTH));
        assertEquals(90.0F, SitManager.yawFor(BlockFace.WEST));
        assertEquals(180.0F, SitManager.yawFor(BlockFace.NORTH));
        assertEquals(-90.0F, SitManager.yawFor(BlockFace.EAST));
    }
}

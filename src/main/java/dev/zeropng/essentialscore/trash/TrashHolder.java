package dev.zeropng.essentialscore.trash;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

final class TrashHolder implements InventoryHolder {
    private final UUID ownerId;
    private Inventory inventory;
    private boolean handled;

    TrashHolder(UUID ownerId) {
        this.ownerId = ownerId;
    }

    UUID ownerId() {
        return ownerId;
    }

    boolean handled() {
        return handled;
    }

    void markHandled() {
        handled = true;
    }

    void attach(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) throw new IllegalStateException("Trash inventory has not been attached");
        return inventory;
    }
}

package dev.zeropng.essentialscore.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class MenuHolder implements InventoryHolder {
    private final MenuType type;
    private final int page;
    private final String context;
    private Inventory inventory;

    public MenuHolder(MenuType type) {
        this(type, 0, "");
    }

    public MenuHolder(MenuType type, int page, String context) {
        this.type = type;
        this.page = page;
        this.context = context;
    }

    public MenuType type() { return type; }
    public int page() { return page; }
    public String context() { return context; }

    public void attach(Inventory inventory) {
        if (this.inventory != null) throw new IllegalStateException("Menu inventory already attached");
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) throw new IllegalStateException("Menu inventory has not been attached yet");
        return inventory;
    }
}

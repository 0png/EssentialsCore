package dev.zeropng.essentialscore.trash;

import dev.zeropng.essentialscore.gui.Items;
import dev.zeropng.essentialscore.localization.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class TrashManager implements Listener {
    public static final int STORAGE_SIZE = 45;
    private static final int CANCEL_SLOT = 45;
    private static final int DELETE_SLOT = 49;

    private final JavaPlugin plugin;
    private final MessageService messages;

    public TrashManager(JavaPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void open(Player player) {
        TrashHolder holder = new TrashHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 54, messages.component("trash.title"));
        holder.attach(inventory);
        ItemStack filler = Items.item(Material.GRAY_STAINED_GLASS_PANE, messages.component("trash.controls"));
        for (int slot = STORAGE_SIZE; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
        inventory.setItem(CANCEL_SLOT, Items.item(Material.BARRIER, messages.component("trash.cancel"),
                messages.component("trash.cancel-lore")));
        inventory.setItem(DELETE_SLOT, Items.item(Material.LAVA_BUCKET, messages.component("trash.delete"),
                messages.component("trash.delete-lore")));
        inventory.setItem(53, Items.item(Material.PAPER, messages.component("trash.instructions"),
                messages.component("trash.instructions-lore")));
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof TrashHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)
                || !holder.ownerId().equals(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (event.getClick() == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            return;
        }
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0) return;
        if (rawSlot < STORAGE_SIZE) return;
        if (rawSlot >= event.getView().getTopInventory().getSize()) return;

        event.setCancelled(true);
        if (rawSlot == CANCEL_SLOT) {
            player.closeInventory();
        } else if (rawSlot == DELETE_SLOT) {
            discard(player, holder);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof TrashHolder)) return;
        if (event.getRawSlots().stream().anyMatch(slot -> slot >= STORAGE_SIZE
                && slot < event.getView().getTopInventory().getSize())) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof TrashHolder holder)
                || !(event.getPlayer() instanceof Player player)) return;
        returnContents(player, holder);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (event.getPlayer().getOpenInventory().getTopInventory().getHolder() instanceof TrashHolder holder) {
            returnContents(event.getPlayer(), holder);
        }
    }

    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof TrashHolder holder) {
                returnContents(player, holder);
                player.closeInventory();
            }
        }
    }

    private void discard(Player player, TrashHolder holder) {
        if (holder.handled()) return;
        int count = 0;
        Inventory inventory = holder.getInventory();
        for (int slot = 0; slot < STORAGE_SIZE; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) count += item.getAmount();
            inventory.setItem(slot, null);
        }
        holder.markHandled();
        player.closeInventory();
        messages.send(player, "trash.deleted", Map.of("count", count));
    }

    private void returnContents(Player player, TrashHolder holder) {
        if (holder.handled()) return;
        holder.markHandled();
        Inventory inventory = holder.getInventory();
        for (int slot = 0; slot < STORAGE_SIZE; slot++) {
            ItemStack item = inventory.getItem(slot);
            inventory.setItem(slot, null);
            if (item == null || item.getType().isAir()) continue;
            for (ItemStack leftover : player.getInventory().addItem(item).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
    }
}

package dev.zeropng.essentialscore.listener;

import dev.zeropng.essentialscore.config.PluginSettings;
import dev.zeropng.essentialscore.gui.MenuHolder;
import dev.zeropng.essentialscore.gui.MenuManager;
import dev.zeropng.essentialscore.gui.MenuType;
import dev.zeropng.essentialscore.home.HomeData;
import dev.zeropng.essentialscore.home.HomeManager;
import dev.zeropng.essentialscore.localization.MessageService;
import dev.zeropng.essentialscore.pet.PetManager;
import dev.zeropng.essentialscore.pet.PetRecord;
import dev.zeropng.essentialscore.tpa.TpaManager;
import dev.zeropng.essentialscore.tpa.TpaRequest;
import dev.zeropng.essentialscore.tpa.TpaType;
import dev.zeropng.essentialscore.trash.TrashManager;
import dev.zeropng.essentialscore.warp.WarpData;
import dev.zeropng.essentialscore.warp.WarpManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GuiListener implements Listener {
    private final JavaPlugin plugin;
    private final MenuManager menus;
    private final HomeManager homes;
    private final TpaManager tpa;
    private final PetManager pets;
    private final PluginSettings settings;
    private final MessageService messages;
    private final WarpManager warps;
    private final TrashManager trash;

    public GuiListener(JavaPlugin plugin, MenuManager menus, HomeManager homes, TpaManager tpa,
                       PetManager pets, PluginSettings settings, MessageService messages, WarpManager warps,
                       TrashManager trash) {
        this.plugin = plugin;
        this.menus = menus;
        this.homes = homes;
        this.tpa = tpa;
        this.pets = pets;
        this.settings = settings;
        this.messages = messages;
        this.warps = warps;
        this.trash = trash;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;
        int slot = event.getRawSlot();
        switch (holder.type()) {
            case MAIN -> main(player, slot);
            case TELEPORT -> teleport(player, slot);
            case HOME -> home(player, holder.page(), slot, event.isRightClick());
            case HOME_DELETE -> homeDelete(player, holder, slot);
            case PLAYER_SELECT_TPA -> playerSelect(player, holder.page(), slot, TpaType.TO_TARGET);
            case PLAYER_SELECT_TPAHERE -> playerSelect(player, holder.page(), slot, TpaType.HERE);
            case REQUESTS -> requests(player, holder.page(), slot, event.isRightClick());
            case RANK_INFO -> rankInfo(player, slot);
            case RANK_LIST -> rankList(player, holder.page(), slot);
            case RANK_EDIT -> rankEdit(player, holder, slot);
            case RANK_DELETE -> rankDelete(player, holder, slot);
            case RANK_PLAYERS -> rankPlayers(player, holder.page(), slot);
            case RANK_ASSIGN -> rankAssign(player, holder, slot);
            case PETS -> pet(player, holder.page(), slot);
            case WARP -> warp(player, holder.page(), slot);
            case HELP -> { if (slot == 22) later(() -> menus.openMain(player)); }
            case ADMIN -> admin(player, slot);
            case ADMIN_HOME -> adminHome(player, slot);
            case ADMIN_TPA -> adminTpa(player, slot);
            case ADMIN_PET -> adminPet(player, slot);
            case ADMIN_EXPERIMENTAL -> adminExperimental(player, slot);
            case ADMIN_WARP -> adminWarp(player, holder.page(), slot);
            case WARP_EDIT -> warpEdit(player, holder, slot);
            case WARP_DELETE -> warpDelete(player, holder, slot);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuHolder)) return;
        if (event.getRawSlots().stream().anyMatch(slot -> slot < top.getSize())) event.setCancelled(true);
    }

    private void main(Player player, int slot) {
        switch (slot) {
            case 10 -> later(() -> menus.openHomes(player, 0));
            case 11 -> later(() -> menus.openTeleport(player));
            case 12 -> later(() -> menus.openRequests(player, 0));
            case 14 -> later(() -> menus.openWarps(player, 0));
            case 15 -> later(() -> menus.openPets(player, 0));
            case 16 -> later(() -> menus.openRank(player));
            case 20 -> {
                player.closeInventory();
                later(() -> trash.open(player));
            }
            case 24 -> later(() -> menus.openHelp(player));
            default -> { }
        }
    }

    private void teleport(Player player, int slot) {
        switch (slot) {
            case 11 -> later(() -> menus.openPlayerSelect(player, TpaType.TO_TARGET, 0));
            case 15 -> later(() -> menus.openPlayerSelect(player, TpaType.HERE, 0));
            case 22 -> later(() -> menus.openMain(player));
            default -> { }
        }
    }

    private void home(Player player, int page, int slot, boolean rightClick) {
        List<HomeData> list = homes.homes(player.getUniqueId());
        int index = page * MenuManager.PAGE_SIZE + slot;
        if (slot < MenuManager.PAGE_SIZE && index < list.size()) {
            HomeData selected = list.get(index);
            if (rightClick) later(() -> menus.openHomeDelete(player, selected, page));
            else {
                player.closeInventory();
                homes.teleport(player, selected);
            }
            return;
        }
        switch (slot) {
            case 45 -> later(() -> menus.openHomes(player, page - 1));
            case 48 -> later(() -> menus.openMain(player));
            case 49 -> later(() -> menus.beginCreateHome(player));
            case 53 -> later(() -> menus.openHomes(player, page + 1));
            default -> { }
        }
    }

    private void homeDelete(Player player, MenuHolder holder, int slot) {
        String[] context = holder.context().split("\\|", 2);
        String homeKey = context[0];
        int page = context.length == 2 ? parseInt(context[1], holder.page()) : holder.page();
        if (slot == 11) {
            later(() -> menus.openHomes(player, page));
        } else if (slot == 15) {
            HomeData home = homes.find(player.getUniqueId(), homeKey);
            if (home == null) messages.send(player, "home.missing");
            else if (homes.delete(player.getUniqueId(), homeKey))
                messages.send(player, "home.deleted", Map.of("name", home.name()));
            later(() -> menus.openHomes(player, page));
        }
    }

    private void playerSelect(Player player, int page, int slot, TpaType type) {
        List<Player> list = menus.onlineTargets(player);
        int index = page * MenuManager.PAGE_SIZE + slot;
        if (slot < MenuManager.PAGE_SIZE && index < list.size()) {
            Player target = list.get(index);
            if (tpa.send(player, target, type)) player.closeInventory();
            return;
        }
        switch (slot) {
            case 45 -> later(() -> menus.openPlayerSelect(player, type, page - 1));
            case 48 -> later(() -> menus.openTeleport(player));
            case 53 -> later(() -> menus.openPlayerSelect(player, type, page + 1));
            default -> { }
        }
    }

    private void requests(Player player, int page, int slot, boolean rightClick) {
        List<TpaRequest> list = tpa.incoming(player.getUniqueId());
        int index = page * MenuManager.PAGE_SIZE + slot;
        if (slot < MenuManager.PAGE_SIZE && index < list.size()) {
            TpaRequest request = list.get(index);
            if (rightClick) tpa.deny(player, request.requesterName());
            else tpa.accept(player, request.requesterName());
            later(() -> menus.openRequests(player, page));
            return;
        }
        switch (slot) {
            case 45 -> later(() -> menus.openRequests(player, page - 1));
            case 48 -> later(() -> menus.openMain(player));
            case 53 -> later(() -> menus.openRequests(player, page + 1));
            default -> { }
        }
    }

    private void pet(Player player, int page, int slot) {
        List<PetRecord> list = pets.pets(player.getUniqueId());
        int index = page * MenuManager.PAGE_SIZE + slot;
        if (slot < MenuManager.PAGE_SIZE && index < list.size()) {
            player.closeInventory();
            pets.recall(player, list.get(index));
            return;
        }
        switch (slot) {
            case 45 -> later(() -> menus.openPets(player, page - 1));
            case 46 -> {
                player.closeInventory();
                pets.setAllSitting(player, true);
            }
            case 47 -> {
                player.closeInventory();
                pets.setAllSitting(player, false);
            }
            case 48 -> later(() -> menus.openMain(player));
            case 50 -> {
                player.closeInventory();
                pets.recallAll(player);
            }
            case 53 -> later(() -> menus.openPets(player, page + 1));
            default -> { }
        }
    }

    private void rankInfo(Player player, int slot) {
        if (slot == 20 && player.isOp()) later(() -> menus.openRankList(player, 0));
        else if (slot == 22) later(() -> menus.openMain(player));
    }

    private void rankList(Player player, int page, int slot) {
        if (!adminAllowed(player)) return;
        List<dev.zeropng.essentialscore.rank.RankData> list = menus.rankList();
        int index = page * MenuManager.PAGE_SIZE + slot;
        if (slot < MenuManager.PAGE_SIZE && index < list.size()) {
            later(() -> menus.openRankEdit(player, list.get(index).id(), page));
            return;
        }
        switch (slot) {
            case 45 -> later(() -> menus.openRankList(player, page - 1));
            case 48 -> later(() -> menus.openRank(player));
            case 49 -> later(() -> menus.beginCreateRank(player, page));
            case 50 -> later(() -> menus.openRankPlayers(player, 0));
            case 53 -> later(() -> menus.openRankList(player, page + 1));
            default -> { }
        }
    }

    private void rankEdit(Player player, MenuHolder holder, int slot) {
        if (!adminAllowed(player)) return;
        String id = holder.context();
        int page = holder.page();
        switch (slot) {
            case 10 -> later(() -> menus.beginEditRank(player, id, "name", page));
            case 12 -> later(() -> menus.beginEditRank(player, id, "prefix", page));
            case 14 -> later(() -> menus.beginEditRank(player, id, "color", page));
            case 16 -> later(() -> menus.setDefaultRank(player, id, page));
            case 20 -> later(() -> menus.openRankDelete(player, id, page));
            case 22 -> later(() -> menus.openRankList(player, page));
            default -> { }
        }
    }

    private void rankDelete(Player player, MenuHolder holder, int slot) {
        if (!adminAllowed(player)) return;
        if (slot == 11) later(() -> menus.openRankEdit(player, holder.context(), holder.page()));
        else if (slot == 15) later(() -> menus.deleteRank(player, holder.context(), holder.page()));
    }

    private void rankPlayers(Player player, int page, int slot) {
        if (!adminAllowed(player)) return;
        List<MenuManager.RankPlayer> list = menus.rankPlayers();
        int index = page * MenuManager.PAGE_SIZE + slot;
        if (slot < MenuManager.PAGE_SIZE && index < list.size()) {
            MenuManager.RankPlayer target = list.get(index);
            later(() -> menus.openRankAssign(player, target.id(), page, 0));
            return;
        }
        switch (slot) {
            case 45 -> later(() -> menus.openRankPlayers(player, page - 1));
            case 48 -> later(() -> menus.openRankList(player, 0));
            case 53 -> later(() -> menus.openRankPlayers(player, page + 1));
            default -> { }
        }
    }

    private void rankAssign(Player player, MenuHolder holder, int slot) {
        if (!adminAllowed(player)) return;
        String[] context = holder.context().split("\\|", 2);
        UUID targetId;
        try {
            targetId = UUID.fromString(context[0]);
        } catch (IllegalArgumentException exception) {
            messages.send(player, "error.no-player");
            later(() -> menus.openRankPlayers(player, 0));
            return;
        }
        int playerPage = context.length == 2 ? parseInt(context[1], 0) : 0;
        List<dev.zeropng.essentialscore.rank.RankData> list = menus.rankList();
        int index = holder.page() * MenuManager.PAGE_SIZE + slot;
        if (slot < MenuManager.PAGE_SIZE && index < list.size()) {
            menus.assignRank(player, targetId, list.get(index).id(), playerPage);
            return;
        }
        switch (slot) {
            case 45 -> later(() -> menus.openRankAssign(player, targetId, playerPage, holder.page() - 1));
            case 48 -> later(() -> menus.openRankPlayers(player, playerPage));
            case 53 -> later(() -> menus.openRankAssign(player, targetId, playerPage, holder.page() + 1));
            default -> { }
        }
    }

    private boolean adminAllowed(Player player) {
        if (player.isOp()) return true;
        player.closeInventory();
        messages.send(player, "error.op-only");
        return false;
    }

    private void warp(Player player, int page, int slot) {
        List<WarpData> list = warps.all();
        int index = page * MenuManager.PAGE_SIZE + slot;
        if (slot < MenuManager.PAGE_SIZE && index < list.size()) {
            player.closeInventory();
            warps.teleport(player, list.get(index));
            return;
        }
        switch (slot) {
            case 45 -> later(() -> menus.openWarps(player, page - 1));
            case 48 -> later(() -> menus.openMain(player));
            case 53 -> later(() -> menus.openWarps(player, page + 1));
            default -> { }
        }
    }

    private void admin(Player player, int slot) {
        if (!adminAllowed(player)) return;
        switch (slot) {
            case 10 -> later(() -> menus.openAdminHome(player));
            case 12 -> later(() -> menus.openAdminTpa(player));
            case 14 -> later(() -> menus.openAdminWarps(player, 0));
            case 16 -> later(() -> menus.openAdminPet(player));
            case 20 -> later(() -> menus.openAdminExperimental(player));
            case 24 -> later(() -> menus.openRankList(player, 0));
            case 22 -> later(() -> menus.openMain(player));
            default -> { }
        }
    }

    private void adminHome(Player player, int slot) {
        if (!adminAllowed(player)) return;
        switch (slot) {
            case 10 -> later(() -> menus.editNumber(player, "home.max-homes", 1, 100, () -> menus.openAdminHome(player)));
            case 13 -> later(() -> menus.editNumber(player, "home.cooldown-seconds", 0, 86400, () -> menus.openAdminHome(player)));
            case 16 -> later(() -> menus.editNumber(player, "home.teleport-delay-seconds", 0, 300, () -> menus.openAdminHome(player)));
            case 22 -> later(() -> menus.openAdmin(player));
            default -> { }
        }
    }

    private void adminTpa(Player player, int slot) {
        if (!adminAllowed(player)) return;
        switch (slot) {
            case 10 -> later(() -> menus.editNumber(player, "tpa.request-expiry-seconds", 5, 600, () -> menus.openAdminTpa(player)));
            case 13 -> later(() -> menus.editNumber(player, "tpa.send-cooldown-seconds", 0, 86400, () -> menus.openAdminTpa(player)));
            case 16 -> later(() -> menus.editNumber(player, "tpa.teleport-delay-seconds", 0, 300, () -> menus.openAdminTpa(player)));
            case 22 -> later(() -> menus.openAdmin(player));
            default -> { }
        }
    }

    private void adminPet(Player player, int slot) {
        if (!adminAllowed(player)) return;
        if (slot == 13) {
            settings.setBoolean("pet.protection-enabled", !settings.petProtection());
            messages.send(player, "admin.saved");
            later(() -> menus.openAdminPet(player));
        } else if (slot == 22) {
            later(() -> menus.openAdmin(player));
        }
    }

    private void adminExperimental(Player player, int slot) {
        if (!adminAllowed(player)) return;
        if (slot == 11) {
            settings.setBoolean("experimental.universal-lead-enabled", !settings.universalLead());
            messages.send(player, "admin.saved");
            later(() -> menus.openAdminExperimental(player));
        } else if (slot == 15) {
            settings.setBoolean("experimental.pet-affection-enabled", !settings.petAffection());
            messages.send(player, "admin.saved");
            later(() -> menus.openAdminExperimental(player));
        } else if (slot == 22) {
            later(() -> menus.openAdmin(player));
        }
    }

    private void adminWarp(Player player, int page, int slot) {
        if (!adminAllowed(player)) return;
        List<WarpData> list = warps.all();
        int index = page * MenuManager.PAGE_SIZE + slot;
        if (slot < MenuManager.PAGE_SIZE && index < list.size()) {
            WarpData selected = list.get(index);
            later(() -> menus.openWarpEdit(player, selected.id(), page));
            return;
        }
        switch (slot) {
            case 45 -> later(() -> menus.openAdminWarps(player, page - 1));
            case 46 -> later(() -> menus.editNumber(player, "warp.cooldown-seconds", 0, 86400,
                    () -> menus.openAdminWarps(player, page)));
            case 47 -> later(() -> menus.editNumber(player, "warp.teleport-delay-seconds", 0, 300,
                    () -> menus.openAdminWarps(player, page)));
            case 48 -> later(() -> menus.openAdmin(player));
            case 49 -> later(() -> menus.beginCreateWarp(player, page));
            case 53 -> later(() -> menus.openAdminWarps(player, page + 1));
            default -> { }
        }
    }

    private void warpEdit(Player player, MenuHolder holder, int slot) {
        if (!adminAllowed(player)) return;
        String id = holder.context();
        int page = holder.page();
        switch (slot) {
            case 10 -> later(() -> menus.beginRenameWarp(player, id, page));
            case 12 -> {
                WarpData warp = warps.get(id);
                if (warp == null) messages.send(player, "warp.not-found");
                else {
                    warps.setLocation(id, player.getLocation());
                    messages.send(player, "warp.location-updated", Map.of("warp", warp.displayName()));
                }
                later(() -> menus.openWarpEdit(player, id, page));
            }
            case 14 -> later(() -> menus.beginWarpIcon(player, id, page));
            case 16 -> later(() -> menus.openWarpDelete(player, id, page));
            case 22 -> later(() -> menus.openAdminWarps(player, page));
            default -> { }
        }
    }

    private void warpDelete(Player player, MenuHolder holder, int slot) {
        if (!adminAllowed(player)) return;
        String id = holder.context();
        int page = holder.page();
        if (slot == 11) {
            later(() -> menus.openWarpEdit(player, id, page));
        } else if (slot == 15) {
            WarpData warp = warps.get(id);
            if (warp == null) messages.send(player, "warp.not-found");
            else {
                warps.delete(id);
                messages.send(player, "warp.deleted", Map.of("warp", warp.displayName()));
            }
            later(() -> menus.openAdminWarps(player, page));
        }
    }

    private void later(Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return fallback; }
    }
}

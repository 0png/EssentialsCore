package dev.zeropng.essentialscore.gui;

import dev.zeropng.essentialscore.config.PluginSettings;
import dev.zeropng.essentialscore.home.HomeData;
import dev.zeropng.essentialscore.home.HomeManager;
import dev.zeropng.essentialscore.input.ChatInputManager;
import dev.zeropng.essentialscore.localization.MessageService;
import dev.zeropng.essentialscore.pet.PetManager;
import dev.zeropng.essentialscore.pet.PetRecord;
import dev.zeropng.essentialscore.rank.RankData;
import dev.zeropng.essentialscore.rank.RankManager;
import dev.zeropng.essentialscore.tpa.TpaManager;
import dev.zeropng.essentialscore.tpa.TpaRequest;
import dev.zeropng.essentialscore.tpa.TpaType;
import dev.zeropng.essentialscore.warp.WarpData;
import dev.zeropng.essentialscore.warp.WarpManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class MenuManager {
    public static final int PAGE_SIZE = 45;

    private final JavaPlugin plugin;
    private final MessageService messages;
    private final PluginSettings settings;
    private final HomeManager homes;
    private final TpaManager tpa;
    private final RankManager ranks;
    private final PetManager pets;
    private final ChatInputManager input;
    private final WarpManager warps;

    public MenuManager(JavaPlugin plugin, MessageService messages, PluginSettings settings,
                       HomeManager homes, TpaManager tpa, RankManager ranks, PetManager pets,
                       ChatInputManager input, WarpManager warps) {
        this.plugin = plugin;
        this.messages = messages;
        this.settings = settings;
        this.homes = homes;
        this.tpa = tpa;
        this.ranks = ranks;
        this.pets = pets;
        this.input = input;
        this.warps = warps;
    }

    public void openMain(Player player) {
        Inventory menu = createMenu(new MenuHolder(MenuType.MAIN), 27, "menu.main.title");
        menu.setItem(10, Items.item(Material.RED_BED, messages.component("menu.main.home")));
        menu.setItem(11, Items.item(Material.ENDER_PEARL, messages.component("menu.main.teleport")));
        menu.setItem(12, Items.item(Material.PAPER, messages.component("menu.main.requests")));
        menu.setItem(14, Items.item(Material.COMPASS, messages.component("menu.main.warp")));
        menu.setItem(15, Items.item(Material.BONE, messages.component("menu.main.pets")));
        menu.setItem(16, Items.item(Material.NAME_TAG, messages.component("menu.main.rank")));
        menu.setItem(20, Items.item(Material.LAVA_BUCKET, messages.component("menu.main.trash")));
        menu.setItem(24, Items.item(Material.KNOWLEDGE_BOOK, messages.component("menu.main.help")));
        player.openInventory(menu);
    }

    public void openHelp(Player player) {
        Inventory menu = createMenu(new MenuHolder(MenuType.HELP), 27, "menu.help.title");
        int slot = 0;
        menu.setItem(slot++, helpItem(Material.NETHER_STAR, "/ec", "help.ec"));
        menu.setItem(slot++, helpItem(Material.RED_BED, "/home · /sethome", "help.home"));
        menu.setItem(slot++, helpItem(Material.ENDER_PEARL, "/tpa · /tpahere", "help.tpa"));
        menu.setItem(slot++, helpItem(Material.PAPER, "/tpaccept · /tpdeny", "help.requests"));
        menu.setItem(slot++, helpItem(Material.COMPASS, "/warp [名稱]", "help.warp"));
        menu.setItem(slot++, helpItem(Material.BONE, "/pet", "help.pet"));
        menu.setItem(slot++, helpItem(Material.OAK_STAIRS, "/sit", "help.sit"));
        menu.setItem(slot++, helpItem(Material.RECOVERY_COMPASS, "/back", "help.back"));
        menu.setItem(slot++, helpItem(Material.LAVA_BUCKET, "/trash", "help.trash"));
        menu.setItem(slot++, helpItem(Material.NAME_TAG, "/rank", "help.rank"));
        if (player.isOp()) menu.setItem(17, helpItem(Material.COMMAND_BLOCK,
                "/ec admin · /warp admin", "help.admin"));
        menu.setItem(22, backItem());
        player.openInventory(menu);
    }

    public void openTeleport(Player player) {
        Inventory menu = createMenu(new MenuHolder(MenuType.TELEPORT), 27, "menu.teleport.title");
        menu.setItem(11, Items.item(Material.ENDER_PEARL, messages.component("menu.teleport.tpa")));
        menu.setItem(15, Items.item(Material.LEAD, messages.component("menu.teleport.tpahere")));
        menu.setItem(22, backItem());
        player.openInventory(menu);
    }

    public void openHomes(Player player, int requestedPage) {
        List<HomeData> list = homes.homes(player.getUniqueId());
        int page = page(requestedPage, list.size());
        Inventory menu = createMenu(new MenuHolder(MenuType.HOME, page, ""), 54, "menu.home.title");
        pageSliceIndexed(list, page).forEachIndexed((slot, home) -> menu.setItem(slot,
                Items.item(Material.RED_BED, Component.text(home.name()),
                        messages.component("menu.home.left-click"), messages.component("menu.home.right-click"),
                        Component.text(home.worldName() + "  " + home.x() + ", " + home.y() + ", " + home.z()))));
        if (list.isEmpty()) menu.setItem(22, Items.item(Material.GRAY_DYE, messages.component("menu.home.empty")));
        navigation(menu, page, list.size());
        menu.setItem(48, backItem());
        menu.setItem(49, Items.item(Material.LIME_DYE, messages.component("menu.home.add"),
                messages.component("menu.home.limit-status", Map.of("count", list.size(), "max", settings.maxHomes()))));
        player.openInventory(menu);
    }

    public void beginCreateHome(Player player) {
        if (homes.homes(player.getUniqueId()).size() >= settings.maxHomes()) {
            messages.send(player, "home.limit", Map.of("max", settings.maxHomes()));
            return;
        }
        input.start(player, "home.input-prompt", Map.of(), name -> {
            HomeManager.NameResult result = homes.validateName(player.getUniqueId(), name);
            if (result != HomeManager.NameResult.VALID) {
                String key = switch (result) {
                    case INVALID -> "home.invalid-name";
                    case RESERVED -> "home.reserved-name";
                    case DUPLICATE -> "home.duplicate";
                    default -> "home.invalid-name";
                };
                messages.send(player, key);
                return false;
            }
            if (homes.homes(player.getUniqueId()).size() >= settings.maxHomes()) {
                messages.send(player, "home.limit", Map.of("max", settings.maxHomes()));
                return false;
            }
            HomeData created = homes.create(player, name);
            messages.send(player, "home.created", Map.of("name", created.name()));
            openHomes(player, 0);
            return true;
        }, () -> openHomes(player, 0));
    }

    public void openHomeDelete(Player player, HomeData home, int returnPage) {
        String context = home.key() + "|" + returnPage;
        Inventory menu = createMenu(new MenuHolder(MenuType.HOME_DELETE, returnPage, context), 27,
                "menu.home.delete-title");
        menu.setItem(13, Items.item(Material.RED_BED, Component.text(home.name()),
                Component.text(home.worldName() + "  " + home.x() + ", " + home.y() + ", " + home.z())));
        menu.setItem(11, Items.item(Material.LIME_CONCRETE, messages.component("menu.home.delete-cancel")));
        menu.setItem(15, Items.item(Material.RED_CONCRETE, messages.component("menu.home.delete-confirm")));
        player.openInventory(menu);
    }

    public void openPlayerSelect(Player player, TpaType type, int requestedPage) {
        List<Player> list = onlineTargets(player);
        int page = page(requestedPage, list.size());
        MenuType menuType = type == TpaType.TO_TARGET ? MenuType.PLAYER_SELECT_TPA : MenuType.PLAYER_SELECT_TPAHERE;
        String title = type == TpaType.TO_TARGET ? "menu.players.title-tpa" : "menu.players.title-tpahere";
        Inventory menu = createMenu(new MenuHolder(menuType, page, ""), 54, title);
        pageSliceIndexed(list, page).forEachIndexed((slot, target) -> menu.setItem(slot, playerHead(target)));
        if (list.isEmpty()) menu.setItem(22, Items.item(Material.GRAY_DYE, messages.component("menu.players.empty")));
        navigation(menu, page, list.size());
        menu.setItem(48, backItem());
        player.openInventory(menu);
    }

    public void openRequests(Player player, int requestedPage) {
        List<TpaRequest> list = tpa.incoming(player.getUniqueId());
        int page = page(requestedPage, list.size());
        Inventory menu = createMenu(new MenuHolder(MenuType.REQUESTS, page, ""), 54, "menu.requests.title");
        pageSliceIndexed(list, page).forEachIndexed((slot, request) -> {
            String type = request.type() == TpaType.TO_TARGET ? "TPA" : "TPAHere";
            menu.setItem(slot, Items.item(Material.PLAYER_HEAD, Component.text(request.requesterName()),
                    Component.text(type + " · " + request.secondsRemaining(System.currentTimeMillis()) + "s"),
                    messages.component("menu.requests.accept"), messages.component("menu.requests.deny")));
        });
        if (list.isEmpty()) menu.setItem(22, Items.item(Material.GRAY_DYE, messages.component("menu.requests.empty")));
        navigation(menu, page, list.size());
        menu.setItem(48, backItem());
        player.openInventory(menu);
    }

    public void openRank(Player player) {
        RankData rank = ranks.assigned(player.getUniqueId());
        Inventory menu = createMenu(new MenuHolder(MenuType.RANK_INFO), 27, "menu.rank.title");
        menu.setItem(13, Items.item(Material.NAME_TAG, rank.displayComponent(), rank.prefixComponent()));
        menu.setItem(22, backItem());
        player.openInventory(menu);
    }

    public void openPets(Player player, int requestedPage) {
        List<PetRecord> list = pets.pets(player.getUniqueId());
        int page = page(requestedPage, list.size());
        Inventory menu = createMenu(new MenuHolder(MenuType.PETS, page, ""), 54, "menu.pet.title");
        pageSliceIndexed(list, page).forEachIndexed((slot, pet) -> {
            String display = pet.name() == null || pet.name().isBlank() ? pretty(pet.type().name()) : pet.name();
            Component status = messages.component(pets.isLoaded(pet) ? "pet.status-loaded" : "pet.status-stored");
            menu.setItem(slot, Items.item(pets.icon(pet), Component.text(display),
                    Component.text(pretty(pet.type().name()) + " · " + pet.worldName() + " · ").append(status),
                    messages.component("menu.pet.recall")));
        });
        if (list.isEmpty()) menu.setItem(22, Items.item(Material.GRAY_DYE, messages.component("menu.pet.empty")));
        navigation(menu, page, list.size());
        menu.setItem(46, Items.item(Material.LIME_DYE, messages.component("menu.pet.sit-all")));
        menu.setItem(47, Items.item(Material.LIGHT_BLUE_DYE, messages.component("menu.pet.stand-all")));
        menu.setItem(48, backItem());
        menu.setItem(50, Items.item(Material.ENDER_PEARL, messages.component("menu.pet.recall-all")));
        player.openInventory(menu);
    }

    public void openWarps(Player player, int requestedPage) {
        List<WarpData> list = warps.all();
        int page = page(requestedPage, list.size());
        Inventory menu = createMenu(new MenuHolder(MenuType.WARP, page, ""), 54, "menu.warp.title");
        pageSliceIndexed(list, page).forEachIndexed((slot, warp) -> menu.setItem(slot,
                Items.item(warp.icon(), Component.text(warp.displayName()),
                        messages.component("menu.warp.id", Map.of("id", warp.id())),
                        messages.component("menu.warp.teleport"))));
        if (list.isEmpty()) menu.setItem(22, Items.item(Material.GRAY_DYE, messages.component("menu.warp.empty")));
        navigation(menu, page, list.size());
        menu.setItem(48, backItem());
        player.openInventory(menu);
    }

    public void openAdmin(Player player) {
        Inventory menu = createMenu(new MenuHolder(MenuType.ADMIN), 27, "menu.admin.title");
        menu.setItem(10, Items.item(Material.RED_BED, messages.component("menu.admin.home"),
                messages.component("admin.home-summary", Map.of("max", settings.maxHomes()))));
        menu.setItem(12, Items.item(Material.ENDER_PEARL, messages.component("menu.admin.tpa")));
        menu.setItem(14, Items.item(Material.COMPASS, messages.component("menu.admin.warp")));
        menu.setItem(16, Items.item(Material.BONE, messages.component("menu.admin.pet")));
        menu.setItem(22, backItem());
        player.openInventory(menu);
    }

    public void openAdminHome(Player player) {
        Inventory menu = createMenu(new MenuHolder(MenuType.ADMIN_HOME), 27, "menu.admin.home-title");
        menu.setItem(10, settingItem(Material.CHEST, "admin.home-max", settings.maxHomes()));
        menu.setItem(13, settingItem(Material.CLOCK, "admin.home-cooldown", settings.homeCooldown()));
        menu.setItem(16, settingItem(Material.REPEATER, "admin.home-delay", settings.homeDelay()));
        menu.setItem(22, backItem());
        player.openInventory(menu);
    }

    public void openAdminTpa(Player player) {
        Inventory menu = createMenu(new MenuHolder(MenuType.ADMIN_TPA), 27, "menu.admin.tpa-title");
        menu.setItem(10, settingItem(Material.PAPER, "admin.tpa-expiry", settings.tpaExpiry()));
        menu.setItem(13, settingItem(Material.CLOCK, "admin.tpa-cooldown", settings.tpaCooldown()));
        menu.setItem(16, settingItem(Material.REPEATER, "admin.tpa-delay", settings.tpaDelay()));
        menu.setItem(22, backItem());
        player.openInventory(menu);
    }

    public void openAdminPet(Player player) {
        Inventory menu = createMenu(new MenuHolder(MenuType.ADMIN_PET), 27, "menu.admin.pet-title");
        menu.setItem(13, Items.item(settings.petProtection() ? Material.LIME_DYE : Material.GRAY_DYE,
                messages.component("admin.pet-protection"),
                messages.component(settings.petProtection() ? "admin.enabled" : "admin.disabled")));
        menu.setItem(22, backItem());
        player.openInventory(menu);
    }

    public void openAdminWarps(Player player, int requestedPage) {
        List<WarpData> list = warps.all();
        int page = page(requestedPage, list.size());
        Inventory menu = createMenu(new MenuHolder(MenuType.ADMIN_WARP, page, ""), 54,
                "menu.admin.warp-title");
        pageSliceIndexed(list, page).forEachIndexed((slot, warp) -> menu.setItem(slot,
                Items.item(warp.icon(), Component.text(warp.displayName()),
                        messages.component("menu.warp.id", Map.of("id", warp.id())),
                        messages.component("admin.warp.edit"))));
        if (list.isEmpty()) menu.setItem(22, Items.item(Material.GRAY_DYE, messages.component("menu.warp.empty")));
        navigation(menu, page, list.size());
        menu.setItem(46, settingItem(Material.CLOCK, "admin.warp-cooldown", settings.warpCooldown()));
        menu.setItem(47, settingItem(Material.REPEATER, "admin.warp-delay", settings.warpDelay()));
        menu.setItem(48, backItem());
        menu.setItem(49, Items.item(Material.LIME_DYE, messages.component("admin.warp.create"),
                messages.component("admin.click-edit")));
        player.openInventory(menu);
    }

    public void openWarpEdit(Player player, String id, int returnPage) {
        WarpData warp = warps.get(id);
        if (warp == null) {
            messages.send(player, "warp.not-found");
            openAdminWarps(player, returnPage);
            return;
        }
        Inventory menu = createMenu(new MenuHolder(MenuType.WARP_EDIT, returnPage, warp.id()), 27,
                "menu.admin.warp-edit-title");
        menu.setItem(10, Items.item(Material.NAME_TAG, messages.component("admin.warp.rename"),
                Component.text(warp.displayName())));
        menu.setItem(12, Items.item(Material.LODESTONE, messages.component("admin.warp.set-location"),
                Component.text(warp.worldName() + " · " + (int) warp.x() + ", " + (int) warp.y() + ", " + (int) warp.z())));
        menu.setItem(14, Items.item(warp.icon(), messages.component("admin.warp.set-icon"),
                Component.text(warp.icon().name())));
        menu.setItem(16, Items.item(Material.RED_CONCRETE, messages.component("admin.warp.delete")));
        menu.setItem(22, backItem());
        player.openInventory(menu);
    }

    public void openWarpDelete(Player player, String id, int returnPage) {
        WarpData warp = warps.get(id);
        if (warp == null) {
            messages.send(player, "warp.not-found");
            openAdminWarps(player, returnPage);
            return;
        }
        Inventory menu = createMenu(new MenuHolder(MenuType.WARP_DELETE, returnPage, warp.id()), 27,
                "menu.admin.warp-delete-title");
        menu.setItem(13, Items.item(warp.icon(), Component.text(warp.displayName())));
        menu.setItem(11, Items.item(Material.LIME_CONCRETE, messages.component("common.back")));
        menu.setItem(15, Items.item(Material.RED_CONCRETE, messages.component("admin.warp.delete-confirm")));
        player.openInventory(menu);
    }

    public void beginCreateWarp(Player player, int returnPage) {
        input.start(player, "warp.create-prompt", Map.of(), raw -> {
            String[] parts = raw.strip().split("\\s+", 2);
            if (parts.length < 2 || !WarpManager.validId(parts[0].toLowerCase())) {
                messages.send(player, "warp.invalid-create-input");
                return false;
            }
            String id = parts[0].toLowerCase();
            String name = parts[1].strip();
            if (!WarpManager.validDisplayName(name)) {
                messages.send(player, "warp.invalid-name");
                return false;
            }
            if (warps.get(id) != null) {
                messages.send(player, "warp.exists");
                return false;
            }
            warps.create(id, name, player.getLocation());
            messages.send(player, "warp.created", Map.of("warp", name));
            openAdminWarps(player, returnPage);
            return true;
        }, () -> openAdminWarps(player, returnPage));
    }

    public void beginRenameWarp(Player player, String id, int returnPage) {
        input.start(player, "warp.rename-prompt", Map.of(), raw -> {
            String name = raw.strip();
            if (!WarpManager.validDisplayName(name)) {
                messages.send(player, "warp.invalid-name");
                return false;
            }
            if (!warps.rename(id, name)) messages.send(player, "warp.not-found");
            else messages.send(player, "warp.renamed", Map.of("warp", name));
            openWarpEdit(player, id, returnPage);
            return true;
        }, () -> openWarpEdit(player, id, returnPage));
    }

    public void beginWarpIcon(Player player, String id, int returnPage) {
        input.start(player, "warp.icon-prompt", Map.of(), raw -> {
            Material icon = Material.matchMaterial(raw.strip());
            if (icon == null || !icon.isItem() || icon.isAir()) {
                messages.send(player, "warp.invalid-icon");
                return false;
            }
            if (!warps.setIcon(id, icon)) messages.send(player, "warp.not-found");
            else messages.send(player, "warp.icon-updated", Map.of("warp", id));
            openWarpEdit(player, id, returnPage);
            return true;
        }, () -> openWarpEdit(player, id, returnPage));
    }

    public void editNumber(Player player, String path, int minimum, int maximum, Runnable returnMenu) {
        input.start(player, "admin.input-prompt", Map.of("min", minimum, "max", maximum), raw -> {
            try {
                int value = Integer.parseInt(raw);
                if (value < minimum || value > maximum) throw new NumberFormatException();
                settings.setInt(path, value);
                messages.send(player, "admin.saved");
                returnMenu.run();
                return true;
            } catch (NumberFormatException exception) {
                messages.send(player, "error.invalid-number", Map.of("min", minimum, "max", maximum));
                return false;
            }
        }, returnMenu);
    }

    public List<Player> onlineTargets(Player viewer) {
        List<Player> result = new ArrayList<>();
        result.addAll(Bukkit.getOnlinePlayers());
        result.removeIf(player -> player.getUniqueId().equals(viewer.getUniqueId()));
        result.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private ItemStack playerHead(Player player) {
        ItemStack stack = Items.item(Material.PLAYER_HEAD, Component.text(player.getName()));
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        meta.setOwningPlayer(player);
        stack.setItemMeta(meta);
        return stack;
    }

    private Inventory createMenu(MenuHolder holder, int size, String titleKey) {
        Inventory inventory = Bukkit.createInventory(holder, size, messages.component(titleKey));
        holder.attach(inventory);
        return inventory;
    }

    private ItemStack settingItem(Material material, String key, int value) {
        return Items.item(material, messages.component(key),
                messages.component("admin.current-value", Map.of("value", value)), messages.component("admin.click-edit"));
    }

    private ItemStack backItem() {
        return Items.item(Material.ARROW, messages.component("common.back"));
    }

    private ItemStack helpItem(Material material, String command, String descriptionKey) {
        return Items.item(material, Component.text(command), messages.component(descriptionKey));
    }

    private void navigation(Inventory menu, int page, int size) {
        if (page > 0) menu.setItem(45, Items.item(Material.ARROW, messages.component("common.previous")));
        if ((page + 1) * PAGE_SIZE < size) menu.setItem(53, Items.item(Material.ARROW, messages.component("common.next")));
    }

    public static int page(int requested, int size) {
        int maximum = Math.max(0, (size - 1) / PAGE_SIZE);
        return Math.max(0, Math.min(maximum, requested));
    }

    public static <T> List<T> pageSlice(List<T> list, int page) {
        int from = Math.min(list.size(), page * PAGE_SIZE);
        return list.subList(from, Math.min(list.size(), from + PAGE_SIZE));
    }

    private static String pretty(String enumName) {
        String[] parts = enumName.toLowerCase().split("_");
        List<String> words = new ArrayList<>();
        for (String part : parts) words.add(Character.toUpperCase(part.charAt(0)) + part.substring(1));
        return String.join(" ", words);
    }

    @FunctionalInterface
    private interface IndexedConsumer<T> { void accept(int index, T value); }

    private static final class IndexedList<T> {
        private final List<T> values;
        private IndexedList(List<T> values) { this.values = values; }
        private void forEachIndexed(IndexedConsumer<T> consumer) {
            for (int i = 0; i < values.size(); i++) consumer.accept(i, values.get(i));
        }
    }

    private static <T> IndexedList<T> pageSliceIndexed(List<T> list, int page) {
        return new IndexedList<>(pageSlice(list, page));
    }
}

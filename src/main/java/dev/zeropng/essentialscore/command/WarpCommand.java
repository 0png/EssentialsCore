package dev.zeropng.essentialscore.command;

import dev.zeropng.essentialscore.gui.MenuManager;
import dev.zeropng.essentialscore.localization.MessageService;
import dev.zeropng.essentialscore.warp.WarpData;
import dev.zeropng.essentialscore.warp.WarpManager;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WarpCommand implements CommandExecutor, TabCompleter {
    private static final List<String> ADMIN_ACTIONS = List.of("admin", "create", "delete", "setlocation",
            "rename", "icon", "list");

    private final WarpManager warps;
    private final MenuManager menus;
    private final MessageService messages;

    public WarpCommand(WarpManager warps, MenuManager menus, MessageService messages) {
        this.warps = warps;
        this.menus = menus;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "error.players-only");
            return true;
        }
        if (args.length == 0) {
            menus.openWarps(player, 0);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        if (ADMIN_ACTIONS.contains(action)) {
            if (!player.isOp()) {
                messages.send(player, "error.op-only");
                return true;
            }
            handleAdmin(player, action, args);
            return true;
        }

        WarpData warp = warps.get(args[0]);
        if (warp == null) messages.send(player, "warp.not-found");
        else warps.teleport(player, warp);
        return true;
    }

    private void handleAdmin(Player player, String action, String[] args) {
        switch (action) {
            case "admin" -> menus.openAdminWarps(player, 0);
            case "create" -> {
                if (args.length < 3) { messages.send(player, "warp.usage"); return; }
                String id = args[1].toLowerCase(Locale.ROOT);
                String name = join(args, 2);
                if (!WarpManager.validId(id)) messages.send(player, "warp.invalid-id");
                else if (!WarpManager.validDisplayName(name)) messages.send(player, "warp.invalid-name");
                else if (warps.get(id) != null) messages.send(player, "warp.exists");
                else if (warps.create(id, name, player.getLocation()))
                    messages.send(player, "warp.created", Map.of("warp", name));
            }
            case "delete" -> {
                if (args.length < 2) {
                    messages.send(player, "warp.usage");
                    return;
                }
                if (args.length != 3 || !args[2].equalsIgnoreCase("confirm")) {
                    messages.send(player, "warp.delete-command-confirm", Map.of("id", args[1]));
                    return;
                }
                WarpData warp = warps.get(args[1]);
                if (warp == null) messages.send(player, "warp.not-found");
                else {
                    warps.delete(warp.id());
                    messages.send(player, "warp.deleted", Map.of("warp", warp.displayName()));
                }
            }
            case "setlocation" -> {
                if (args.length != 2) { messages.send(player, "warp.usage"); return; }
                WarpData warp = warps.get(args[1]);
                if (warp == null) messages.send(player, "warp.not-found");
                else {
                    warps.setLocation(warp.id(), player.getLocation());
                    messages.send(player, "warp.location-updated", Map.of("warp", warp.displayName()));
                }
            }
            case "rename" -> {
                if (args.length < 3) { messages.send(player, "warp.usage"); return; }
                WarpData warp = warps.get(args[1]);
                String name = join(args, 2);
                if (warp == null) messages.send(player, "warp.not-found");
                else if (!WarpManager.validDisplayName(name)) messages.send(player, "warp.invalid-name");
                else {
                    warps.rename(warp.id(), name);
                    messages.send(player, "warp.renamed", Map.of("warp", name));
                }
            }
            case "icon" -> {
                if (args.length != 3) { messages.send(player, "warp.usage"); return; }
                WarpData warp = warps.get(args[1]);
                Material icon = Material.matchMaterial(args[2]);
                if (warp == null) messages.send(player, "warp.not-found");
                else if (icon == null || !icon.isItem() || icon.isAir()) messages.send(player, "warp.invalid-icon");
                else {
                    warps.setIcon(warp.id(), icon);
                    messages.send(player, "warp.icon-updated", Map.of("warp", warp.displayName()));
                }
            }
            case "list" -> messages.send(player, "warp.list", Map.of("warps",
                    String.join(", ", warps.all().stream().map(WarpData::id).toList())));
            default -> messages.send(player, "warp.usage");
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> values = new java.util.ArrayList<>(warps.all().stream().map(WarpData::id).toList());
            if (sender.isOp()) values.addAll(ADMIN_ACTIONS);
            return filter(values, args[0]);
        }
        if (args.length == 2 && sender.isOp()
                && List.of("delete", "setlocation", "rename", "icon").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(warps.all().stream().map(WarpData::id).toList(), args[1]);
        }
        if (args.length == 3 && sender.isOp() && args[0].equalsIgnoreCase("delete")) return List.of("confirm");
        return List.of();
    }

    private static String join(String[] args, int from) {
        return String.join(" ", Arrays.copyOfRange(args, from, args.length)).trim();
    }

    private static List<String> filter(List<String> values, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).distinct().toList();
    }
}

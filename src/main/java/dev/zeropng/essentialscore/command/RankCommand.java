package dev.zeropng.essentialscore.command;

import dev.zeropng.essentialscore.localization.MessageService;
import dev.zeropng.essentialscore.gui.MenuManager;
import dev.zeropng.essentialscore.rank.RankData;
import dev.zeropng.essentialscore.rank.RankManager;
import dev.zeropng.essentialscore.rank.NameTagManager;
import dev.zeropng.essentialscore.storage.DataStore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class RankCommand implements CommandExecutor, TabCompleter {
    private static final List<String> ACTIONS = List.of("info", "list", "create", "edit", "set", "default", "delete");
    private final RankManager ranks;
    private final DataStore store;
    private final MessageService messages;
    private final NameTagManager nameTags;
    private final MenuManager menus;

    public RankCommand(RankManager ranks, DataStore store, MessageService messages, NameTagManager nameTags,
                       MenuManager menus) {
        this.ranks = ranks;
        this.store = store;
        this.messages = messages;
        this.nameTags = nameTags;
        this.menus = menus;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) messages.send(sender, "error.players-only");
            else menus.openRank(player);
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("info")) return info(sender, args);
        if (!manager(sender)) return true;
        switch (action) {
            case "list" -> messages.send(sender, "rank.list", Map.of("ranks",
                    String.join(", ", ranks.all().stream().map(RankData::id).toList())));
            case "create" -> create(sender, args);
            case "edit" -> edit(sender, args);
            case "set" -> assign(sender, args);
            case "default" -> setDefault(sender, args);
            case "delete" -> delete(sender, args);
            default -> messages.send(sender, "rank.usage");
        }
        return true;
    }

    private boolean info(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (sender instanceof Player player) menus.openRank(player);
            else messages.send(sender, "rank.usage");
            return true;
        }
        if (!manager(sender)) return true;
        UUID playerId = resolvePlayer(args[1]);
        if (playerId == null) messages.send(sender, "error.no-player");
        else show(sender, playerId);
        return true;
    }

    private void show(CommandSender sender, UUID playerId) {
        RankData rank = ranks.assigned(playerId);
        sender.sendMessage(messages.component("prefix").append(messages.component("rank.current",
                Map.of("rank", rank.displayName()))).append(Component.space()).append(rank.prefixComponent()));
    }

    private void create(CommandSender sender, String[] args) {
        if (args.length < 3) { messages.send(sender, "rank.usage"); return; }
        String id = args[1].toLowerCase(Locale.ROOT);
        String name = join(args, 2);
        if (!RankManager.validId(id)) messages.send(sender, "rank.invalid-id");
        else if (!RankManager.validName(name)) messages.send(sender, "rank.invalid-name");
        else if (ranks.get(id) != null) messages.send(sender, "rank.exists");
        else if (ranks.create(id, name)) {
            nameTags.refreshAll();
            messages.send(sender, "rank.created", Map.of("rank", id));
        } else messages.send(sender, "rank.save-failed");
    }

    private void edit(CommandSender sender, String[] args) {
        if (args.length < 4) { messages.send(sender, "rank.usage"); return; }
        RankData rank = ranks.get(args[1]);
        if (rank == null) { messages.send(sender, "rank.not-found"); return; }
        String field = args[2].toLowerCase(Locale.ROOT);
        String value = join(args, 3);
        boolean success;
        switch (field) {
            case "name" -> {
                if (!RankManager.validName(value)) { messages.send(sender, "rank.invalid-name"); return; }
                success = ranks.editName(rank.id(), value);
            }
            case "prefix" -> {
                if (!RankManager.validPrefix(value)) { messages.send(sender, "rank.invalid-prefix"); return; }
                success = ranks.editPrefix(rank.id(), value);
            }
            case "color" -> {
                if (!RankManager.validColor(value)) { messages.send(sender, "rank.invalid-color"); return; }
                success = ranks.editColor(rank.id(), value);
            }
            default -> { messages.send(sender, "rank.usage"); return; }
        }
        if (success) {
            nameTags.refreshAll();
            messages.send(sender, "rank.updated", Map.of("rank", rank.id()));
        }
    }

    private void assign(CommandSender sender, String[] args) {
        if (args.length != 3) { messages.send(sender, "rank.usage"); return; }
        UUID playerId = resolvePlayer(args[1]);
        RankData rank = ranks.get(args[2]);
        if (playerId == null) messages.send(sender, "error.no-player");
        else if (rank == null) messages.send(sender, "rank.not-found");
        else {
            ranks.assign(playerId, rank.id());
            nameTags.refreshAll();
            String name = store.knownPlayers().getOrDefault(playerId, args[1]);
            messages.send(sender, "rank.assigned", Map.of("rank", rank.id(), "player", name));
        }
    }

    private void setDefault(CommandSender sender, String[] args) {
        if (args.length != 2) { messages.send(sender, "rank.usage"); return; }
        if (ranks.setDefault(args[1])) {
            nameTags.refreshAll();
            messages.send(sender, "rank.default-set", Map.of("rank", args[1]));
        }
        else messages.send(sender, "rank.not-found");
    }

    private void delete(CommandSender sender, String[] args) {
        if (args.length < 2) { messages.send(sender, "rank.usage"); return; }
        RankData rank = ranks.get(args[1]);
        if (rank == null) { messages.send(sender, "rank.not-found"); return; }
        if (rank.id().equals(ranks.defaultRank().id())) { messages.send(sender, "rank.cannot-delete-default"); return; }
        if (args.length != 3 || !args[2].equalsIgnoreCase("confirm")) {
            messages.send(sender, "rank.delete-confirm", Map.of("rank", rank.id()));
            return;
        }
        int count = ranks.delete(rank.id());
        nameTags.refreshAll();
        messages.send(sender, "rank.deleted", Map.of("rank", rank.id(), "count", count));
    }

    private boolean manager(CommandSender sender) {
        if (!(sender instanceof Player player) || player.isOp()) return true;
        messages.send(sender, "error.op-only");
        return false;
    }

    private UUID resolvePlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            store.recordName(online.getUniqueId(), online.getName());
            return online.getUniqueId();
        }
        return store.findKnownPlayer(name);
    }

    private static String join(String[] args, int from) {
        return String.join(" ", Arrays.copyOfRange(args, from, args.length)).trim();
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return filter(ACTIONS, args[0]);
        if (args.length == 2 && List.of("edit", "default", "delete").contains(args[0].toLowerCase()))
            return filter(ranks.all().stream().map(RankData::id).toList(), args[1]);
        if (args.length == 2 && List.of("set", "info").contains(args[0].toLowerCase()))
            return filter(new ArrayList<>(store.knownPlayers().values()), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("set"))
            return filter(ranks.all().stream().map(RankData::id).toList(), args[2]);
        if (args.length == 3 && args[0].equalsIgnoreCase("edit"))
            return filter(List.of("name", "prefix", "color"), args[2]);
        if (args.length == 3 && args[0].equalsIgnoreCase("delete")) return filter(List.of("confirm"), args[2]);
        return List.of();
    }

    private static List<String> filter(List<String> values, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}

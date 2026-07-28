package dev.zeropng.essentialscore.command;

import dev.zeropng.essentialscore.gui.MenuManager;
import dev.zeropng.essentialscore.localization.MessageService;
import dev.zeropng.essentialscore.tpa.TpaManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public final class MainCommand implements CommandExecutor, TabCompleter {
    private final MenuManager menus;
    private final MessageService messages;
    private final TpaManager tpa;

    public MainCommand(MenuManager menus, MessageService messages, TpaManager tpa) {
        this.menus = menus;
        this.messages = messages;
        this.tpa = tpa;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "error.players-only");
            return true;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("tpaccept") || args[0].equalsIgnoreCase("tpdeny"))) {
            try {
                UUID requesterId = UUID.fromString(args[1]);
                if (args[0].equalsIgnoreCase("tpaccept")) tpa.accept(player, requesterId);
                else tpa.deny(player, requesterId);
            } catch (IllegalArgumentException exception) {
                messages.send(player, "error.no-request");
            }
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
            if (!player.isOp()) messages.send(player, "error.op-only");
            else menus.openAdmin(player);
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
            menus.openHelp(player);
            return true;
        }
        menus.openMain(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) return List.of();
        List<String> values = sender.isOp() ? List.of("help", "admin") : List.of("help");
        String prefix = args[0].toLowerCase();
        return values.stream().filter(value -> value.startsWith(prefix)).toList();
    }
}

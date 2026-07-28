package dev.zeropng.essentialscore.command;

import dev.zeropng.essentialscore.gui.MenuManager;
import dev.zeropng.essentialscore.localization.MessageService;
import dev.zeropng.essentialscore.tpa.TpaManager;
import dev.zeropng.essentialscore.tpa.TpaType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class TpaCommand implements CommandExecutor, TabCompleter {
    private final MenuManager menus;
    private final TpaManager tpa;
    private final MessageService messages;

    public TpaCommand(MenuManager menus, TpaManager tpa, MessageService messages) {
        this.menus = menus;
        this.tpa = tpa;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "error.players-only");
            return true;
        }
        String name = command.getName().toLowerCase();
        switch (name) {
            case "tpa" -> menus.openPlayerSelect(player, TpaType.TO_TARGET, 0);
            case "tpahere" -> menus.openPlayerSelect(player, TpaType.HERE, 0);
            case "tpaccept" -> tpa.accept(player, args.length == 0 ? null : args[0]);
            case "tpdeny" -> tpa.deny(player, args.length == 0 ? null : args[0]);
            default -> { }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player) || args.length != 1
                || !(command.getName().equalsIgnoreCase("tpaccept") || command.getName().equalsIgnoreCase("tpdeny"))) {
            return List.of();
        }
        String prefix = args[0].toLowerCase();
        return tpa.incoming(player.getUniqueId()).stream().map(request -> request.requesterName())
                .distinct().filter(name -> name.toLowerCase().startsWith(prefix)).toList();
    }
}

package dev.zeropng.essentialscore.command;

import dev.zeropng.essentialscore.gui.MenuManager;
import dev.zeropng.essentialscore.localization.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class PetCommand implements CommandExecutor {
    private final MenuManager menus;
    private final MessageService messages;

    public PetCommand(MenuManager menus, MessageService messages) {
        this.menus = menus;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) messages.send(sender, "error.players-only");
        else menus.openPets(player, 0);
        return true;
    }
}

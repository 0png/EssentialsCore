package dev.zeropng.essentialscore.command;

import dev.zeropng.essentialscore.localization.MessageService;
import dev.zeropng.essentialscore.trash.TrashManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class TrashCommand implements CommandExecutor {
    private final TrashManager trash;
    private final MessageService messages;

    public TrashCommand(TrashManager trash, MessageService messages) {
        this.trash = trash;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) messages.send(sender, "error.players-only");
        else trash.open(player);
        return true;
    }
}

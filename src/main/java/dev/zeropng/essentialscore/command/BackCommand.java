package dev.zeropng.essentialscore.command;

import dev.zeropng.essentialscore.back.DeathBackManager;
import dev.zeropng.essentialscore.localization.MessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class BackCommand implements CommandExecutor {
    private final DeathBackManager deathBack;
    private final MessageService messages;

    public BackCommand(DeathBackManager deathBack, MessageService messages) {
        this.deathBack = deathBack;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "error.players-only");
            return true;
        }
        deathBack.teleportBack(player);
        return true;
    }
}

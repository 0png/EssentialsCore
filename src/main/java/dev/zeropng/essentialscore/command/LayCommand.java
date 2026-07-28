package dev.zeropng.essentialscore.command;

import dev.zeropng.essentialscore.lay.LayManager;
import dev.zeropng.essentialscore.localization.MessageService;
import dev.zeropng.essentialscore.sit.SitManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class LayCommand implements CommandExecutor {
    private final LayManager lays;
    private final SitManager sits;
    private final MessageService messages;

    public LayCommand(LayManager lays, SitManager sits, MessageService messages) {
        this.lays = lays;
        this.sits = sits;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "error.players-only");
            return true;
        }
        if (!lays.isLaying(player)) sits.stand(player);
        lays.toggle(player);
        return true;
    }
}

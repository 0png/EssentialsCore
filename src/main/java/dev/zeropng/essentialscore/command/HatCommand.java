package dev.zeropng.essentialscore.command;

import dev.zeropng.essentialscore.localization.MessageService;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class HatCommand implements CommandExecutor {
    private final MessageService messages;

    public HatCommand(MessageService messages) {
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "error.players-only");
            return true;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            messages.send(player, "hat.empty-hand");
            return true;
        }
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet != null && helmet.getType() != Material.AIR) {
            messages.send(player, "hat.slot-occupied");
            return true;
        }
        player.getInventory().setHelmet(held.clone());
        player.getInventory().setItemInMainHand(null);
        messages.send(player, "hat.worn");
        return true;
    }
}

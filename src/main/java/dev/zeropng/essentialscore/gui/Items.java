package dev.zeropng.essentialscore.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public final class Items {
    private Items() {
    }

    public static ItemStack item(Material material, Component name, Component... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(name);
        if (lore.length > 0) meta.lore(List.copyOf(Arrays.asList(lore)));
        stack.setItemMeta(meta);
        return stack;
    }
}

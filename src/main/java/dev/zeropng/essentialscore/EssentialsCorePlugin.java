package dev.zeropng.essentialscore;

import dev.zeropng.essentialscore.back.DeathBackManager;
import dev.zeropng.essentialscore.command.BackCommand;
import dev.zeropng.essentialscore.command.TrashCommand;
import dev.zeropng.essentialscore.command.WarpCommand;
import dev.zeropng.essentialscore.command.HomeCommand;
import dev.zeropng.essentialscore.command.MainCommand;
import dev.zeropng.essentialscore.command.PetCommand;
import dev.zeropng.essentialscore.command.RankCommand;
import dev.zeropng.essentialscore.command.SitCommand;
import dev.zeropng.essentialscore.command.TpaCommand;
import dev.zeropng.essentialscore.config.PluginSettings;
import dev.zeropng.essentialscore.gui.MenuManager;
import dev.zeropng.essentialscore.home.HomeManager;
import dev.zeropng.essentialscore.input.ChatInputManager;
import dev.zeropng.essentialscore.listener.GuiListener;
import dev.zeropng.essentialscore.listener.PlayerListener;
import dev.zeropng.essentialscore.localization.MessageService;
import dev.zeropng.essentialscore.pet.PetManager;
import dev.zeropng.essentialscore.rank.EssentialsPlaceholderExpansion;
import dev.zeropng.essentialscore.rank.RankManager;
import dev.zeropng.essentialscore.rank.NameTagManager;
import dev.zeropng.essentialscore.rank.RankDisplayListener;
import dev.zeropng.essentialscore.storage.DataStore;
import dev.zeropng.essentialscore.sit.SitManager;
import dev.zeropng.essentialscore.teleport.TeleportCoordinator;
import dev.zeropng.essentialscore.trash.TrashManager;
import dev.zeropng.essentialscore.tpa.TpaManager;
import dev.zeropng.essentialscore.warp.WarpManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class EssentialsCorePlugin extends JavaPlugin {
    private DataStore store;
    private PluginSettings settings;
    private ChatInputManager input;
    private TeleportCoordinator teleports;
    private PetManager pets;
    private EssentialsPlaceholderExpansion placeholderExpansion;
    private NameTagManager nameTags;
    private SitManager sits;
    private TrashManager trash;

    @Override
    public void onEnable() {
        settings = new PluginSettings(this);
        MessageService messages = new MessageService(this, settings);
        store = new DataStore(this);
        teleports = new TeleportCoordinator(this, messages);
        DeathBackManager deathBack = new DeathBackManager(this, store, messages, teleports);
        input = new ChatInputManager(this, messages, settings);
        HomeManager homes = new HomeManager(store, settings, messages, teleports);
        TpaManager tpa = new TpaManager(this, messages, settings, teleports);
        RankManager ranks = new RankManager(this, store);
        WarpManager warps = new WarpManager(this, settings, messages, teleports);
        nameTags = new NameTagManager(ranks);
        pets = new PetManager(this, store, settings, messages);
        sits = new SitManager(this, messages);
        trash = new TrashManager(this, messages);
        MenuManager menus = new MenuManager(this, messages, settings, homes, tpa, ranks, pets, input, warps);

        getServer().getPluginManager().registerEvents(input, this);
        getServer().getPluginManager().registerEvents(teleports, this);
        getServer().getPluginManager().registerEvents(deathBack, this);
        getServer().getPluginManager().registerEvents(pets, this);
        getServer().getPluginManager().registerEvents(sits, this);
        getServer().getPluginManager().registerEvents(trash, this);
        getServer().getPluginManager().registerEvents(new RankDisplayListener(ranks), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this, menus, homes, tpa, pets, settings,
                messages, warps, trash), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this, store, nameTags, messages), this);

        MainCommand mainCommand = new MainCommand(menus, messages, tpa);
        bind("ec", mainCommand, mainCommand);
        HomeCommand homeCommand = new HomeCommand(menus, messages);
        bind("home", homeCommand, null);
        bind("sethome", homeCommand, null);
        TpaCommand tpaCommand = new TpaCommand(menus, tpa, messages);
        bind("tpa", tpaCommand, tpaCommand);
        bind("tpahere", tpaCommand, tpaCommand);
        bind("tpaccept", tpaCommand, tpaCommand);
        bind("tpdeny", tpaCommand, tpaCommand);
        bind("pet", new PetCommand(menus, messages), null);
        bind("sit", new SitCommand(sits, messages), null);
        bind("back", new BackCommand(deathBack, messages), null);
        bind("trash", new TrashCommand(trash, messages), null);
        WarpCommand warpCommand = new WarpCommand(warps, menus, messages);
        bind("warp", warpCommand, warpCommand);
        RankCommand rankCommand = new RankCommand(ranks, store, messages, nameTags);
        bind("rank", rankCommand, rankCommand);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholderExpansion = new EssentialsPlaceholderExpansion(ranks, getPluginMeta().getVersion());
            placeholderExpansion.register();
        }
        nameTags.refreshAll();
        getLogger().info("EssentialsCore enabled with language " + settings.language() + ".");
    }

    private void bind(String name, org.bukkit.command.CommandExecutor executor, TabCompleter completer) {
        PluginCommand command = getCommand(name);
        if (command == null) throw new IllegalStateException("Command missing from plugin.yml: " + name);
        command.setExecutor(executor);
        if (completer != null) command.setTabCompleter(completer);
    }

    @Override
    public void onDisable() {
        if (placeholderExpansion != null) placeholderExpansion.unregister();
        if (nameTags != null) nameTags.shutdown();
        if (input != null) input.cancelAll();
        if (teleports != null) teleports.cancelAll();
        if (sits != null) sits.shutdown();
        if (trash != null) trash.shutdown();
        if (pets != null) pets.flush();
        if (store != null) store.save();
        if (settings != null) settings.save();
    }
}

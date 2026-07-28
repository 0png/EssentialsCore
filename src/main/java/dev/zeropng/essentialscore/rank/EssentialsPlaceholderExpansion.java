package dev.zeropng.essentialscore.rank;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class EssentialsPlaceholderExpansion extends PlaceholderExpansion {
    private final RankManager ranks;
    private final String version;

    public EssentialsPlaceholderExpansion(RankManager ranks, String version) {
        this.ranks = ranks;
        this.version = version;
    }

    @Override public @NotNull String getIdentifier() { return "essentialscore"; }
    @Override public @NotNull String getAuthor() { return "0png"; }
    @Override public @NotNull String getVersion() { return version; }
    @Override public boolean persist() { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return null;
        return switch (params.toLowerCase()) {
            case "rank" -> ranks.placeholderRank(player.getUniqueId());
            case "rank_prefix" -> ranks.placeholderPrefix(player.getUniqueId());
            default -> null;
        };
    }
}

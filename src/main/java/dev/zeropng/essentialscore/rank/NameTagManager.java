package dev.zeropng.essentialscore.rank;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class NameTagManager {
    private static final String TEAM_PREFIX = "ec_";
    private final RankManager ranks;
    private final Scoreboard scoreboard;

    public NameTagManager(RankManager ranks) {
        this.ranks = ranks;
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        scoreboard.getTeams().stream().filter(team -> team.getName().startsWith(TEAM_PREFIX))
                .toList().forEach(Team::unregister);
    }

    public void refreshPlayer(Player player) {
        removePlayer(player);
        RankData rank = ranks.assigned(player.getUniqueId());
        String teamName = teamName(rank.id());
        Team team = scoreboard.getTeam(teamName);
        if (team == null) team = scoreboard.registerNewTeam(teamName);
        team.prefix(rank.prefixComponent());
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        team.addEntry(player.getName());
    }

    public void refreshAll() {
        Bukkit.getOnlinePlayers().forEach(this::refreshPlayer);
    }

    public void removePlayer(Player player) {
        for (Team team : scoreboard.getTeams()) {
            if (team.getName().startsWith(TEAM_PREFIX)) team.removeEntry(player.getName());
        }
    }

    public void shutdown() {
        scoreboard.getTeams().stream().filter(team -> team.getName().startsWith(TEAM_PREFIX))
                .toList().forEach(Team::unregister);
    }

    static String teamName(String rankId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rankId.getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder(TEAM_PREFIX);
            for (int i = 0; i < 6; i++) value.append(String.format("%02x", digest[i]));
            return value.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}

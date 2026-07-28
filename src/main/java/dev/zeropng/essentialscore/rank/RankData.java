package dev.zeropng.essentialscore.rank;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

public record RankData(String id, String displayName, String prefix, String color) {
    public TextColor textColor() {
        return RankManager.parseColor(color);
    }

    public Component displayComponent() {
        return Component.text(displayName, textColor());
    }

    public Component prefixComponent() {
        return MiniMessage.miniMessage().deserialize(prefix);
    }
}

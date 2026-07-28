package dev.zeropng.essentialscore.tpa;

import java.util.UUID;

public record TpaRequest(UUID requesterId, String requesterName, UUID recipientId,
                         TpaType type, long createdAt, long expiresAt) {
    public long secondsRemaining(long now) {
        return Math.max(0L, (expiresAt - now + 999L) / 1000L);
    }
}

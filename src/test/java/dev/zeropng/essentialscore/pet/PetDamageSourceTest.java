package dev.zeropng.essentialscore.pet;

import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.ThrownPotion;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PetDamageSourceTest {
    @Test
    void resolvesOnlyTheConfiguredAttributableSources() {
        UUID attackerId = UUID.randomUUID();
        Player player = proxy(Player.class, Map.of("getUniqueId", attackerId));
        AbstractArrow arrow = proxy(AbstractArrow.class, Map.of("getShooter", player));
        ThrownPotion potion = proxy(ThrownPotion.class, Map.of("getShooter", player));
        TNTPrimed tnt = proxy(TNTPrimed.class, Map.of("getSource", player));
        Tameable attackingPet = proxy(Tameable.class,
                Map.of("isTamed", true, "getOwnerUniqueId", attackerId));

        assertEquals(attackerId, PetManager.responsiblePlayer(player));
        assertEquals(attackerId, PetManager.responsiblePlayer(arrow));
        assertEquals(attackerId, PetManager.responsiblePlayer(potion));
        assertEquals(attackerId, PetManager.responsiblePlayer(tnt));
        assertEquals(attackerId, PetManager.responsiblePlayer(attackingPet));
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Map<String, Object> values) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (instance, method, args) -> {
            if (values.containsKey(method.getName())) return values.get(method.getName());
            Class<?> result = method.getReturnType();
            if (!result.isPrimitive()) return null;
            if (result == boolean.class) return false;
            if (result == char.class) return '\0';
            if (result == byte.class) return (byte) 0;
            if (result == short.class) return (short) 0;
            if (result == int.class) return 0;
            if (result == long.class) return 0L;
            if (result == float.class) return 0F;
            if (result == double.class) return 0D;
            return null;
        });
    }
}

package dev.zeropng.essentialscore.pet;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import dev.zeropng.essentialscore.config.PluginSettings;
import dev.zeropng.essentialscore.localization.MessageService;
import dev.zeropng.essentialscore.storage.DataStore;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PetManager implements Listener {
    private final JavaPlugin plugin;
    private final DataStore store;
    private final PluginSettings settings;
    private final MessageService messages;
    private final Map<UUID, Long> warningThrottle = new HashMap<>();
    private boolean dirty;

    public PetManager(JavaPlugin plugin, DataStore store, PluginSettings settings, MessageService messages) {
        this.plugin = plugin;
        this.store = store;
        this.settings = settings;
        this.messages = messages;
        Bukkit.getWorlds().forEach(world -> world.getEntities().forEach(this::trackIfPet));
        Bukkit.getScheduler().runTaskTimer(plugin, this::flush, 600L, 600L);
    }

    public List<PetRecord> pets(UUID ownerId) {
        ConfigurationSection pets = store.data().getConfigurationSection("pets");
        List<PetRecord> result = new ArrayList<>();
        if (pets == null) return result;
        for (String key : pets.getKeys(false)) {
            ConfigurationSection section = pets.getConfigurationSection(key);
            if (section == null) continue;
            PetRecord record = PetRecord.read(key, section);
            if (record != null && record.ownerId().equals(ownerId)) result.add(record);
        }
        result.sort(Comparator.comparing((PetRecord pet) -> Bukkit.getEntity(pet.entityId()) == null)
                .thenComparing(pet -> pet.name() == null ? pet.type().name() : pet.name(), String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public boolean isLoaded(PetRecord pet) {
        Entity entity = Bukkit.getEntity(pet.entityId());
        return entity != null && entity.isValid();
    }

    public Material icon(PetRecord pet) {
        Material material = Material.matchMaterial(pet.type().name() + "_SPAWN_EGG");
        return material == null ? Material.BONE : material;
    }

    public void recall(Player owner, PetRecord pet) {
        if (!pet.ownerId().equals(owner.getUniqueId())) {
            messages.send(owner, "pet.wrong-owner");
            return;
        }
        World world = pet.world();
        if (world == null) {
            messages.send(owner, "pet.world-missing");
            return;
        }
        messages.send(owner, "pet.loading");
        Entity loaded = Bukkit.getEntity(pet.entityId());
        if (loaded != null && loaded.isValid()) {
            finishRecall(owner, pet, loaded.getChunk(), null);
            return;
        }
        world.getChunkAtAsync(pet.chunkX(), pet.chunkZ(), true).whenComplete((chunk, throwable) ->
                Bukkit.getScheduler().runTask(plugin, () -> finishRecall(owner, pet, chunk, throwable)));
    }

    public void setAllSitting(Player owner, boolean sitting) {
        List<PetRecord> records = pets(owner.getUniqueId());
        if (records.isEmpty()) {
            messages.send(owner, "pet.no-pets");
            return;
        }
        messages.send(owner, "pet.bulk-loading");
        loadTrackedChunks(records).whenComplete((ignored, throwable) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (!owner.isOnline()) return;
            int count = 0;
            for (PetRecord record : records) {
                Tameable tameable = ownedTameable(owner, record);
                if (tameable instanceof Sittable sittable) {
                    sittable.setSitting(sitting);
                    count++;
                }
            }
            messages.send(owner, sitting ? "pet.bulk-sat" : "pet.bulk-stood", Map.of("count", count));
        }));
    }

    public void recallAll(Player owner) {
        List<PetRecord> records = pets(owner.getUniqueId());
        if (records.isEmpty()) {
            messages.send(owner, "pet.no-pets");
            return;
        }
        messages.send(owner, "pet.bulk-loading");
        loadTrackedChunks(records).whenComplete((ignored, throwable) -> Bukkit.getScheduler().runTask(plugin,
                () -> finishRecallAll(owner, records)));
    }

    private void finishRecallAll(Player owner, List<PetRecord> records) {
        if (!owner.isOnline()) return;
        Set<TargetBlock> reserved = new HashSet<>();
        List<PetTransfer> transfers = new ArrayList<>();
        for (PetRecord record : records) {
            Tameable tameable = ownedTameable(owner, record);
            if (tameable == null || !tameable.getPassengers().isEmpty() || tameable.getVehicle() != null) continue;
            if (tameable.isLeashed()) tameable.setLeashHolder(null);
            Location target = findSafeLocation(tameable, owner.getLocation(), reserved);
            if (target == null) continue;
            CompletableFuture<Boolean> future = tameable.teleportAsync(target)
                    .handle((success, error) -> error == null && Boolean.TRUE.equals(success));
            transfers.add(new PetTransfer(tameable, future));
        }
        if (transfers.isEmpty()) {
            messages.send(owner, "pet.bulk-recalled", Map.of("count", 0, "total", records.size()));
            return;
        }
        CompletableFuture.allOf(transfers.stream().map(PetTransfer::future)
                .toArray(CompletableFuture[]::new)).whenComplete((ignored, throwable) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!owner.isOnline()) return;
                    int success = 0;
                    for (PetTransfer transfer : transfers) {
                        if (transfer.future().getNow(false)) {
                            trackIfPet(transfer.entity());
                            success++;
                        }
                    }
                    messages.send(owner, "pet.bulk-recalled", Map.of("count", success, "total", records.size()));
                }));
    }

    private CompletableFuture<Void> loadTrackedChunks(List<PetRecord> records) {
        List<CompletableFuture<?>> loads = new ArrayList<>();
        for (PetRecord record : records) {
            Entity entity = Bukkit.getEntity(record.entityId());
            if (entity != null && entity.isValid()) continue;
            World world = record.world();
            if (world != null) {
                loads.add(world.getChunkAtAsync(record.chunkX(), record.chunkZ(), true)
                        .handle((chunk, error) -> null));
            }
        }
        return CompletableFuture.allOf(loads.toArray(CompletableFuture[]::new));
    }

    private Tameable ownedTameable(Player owner, PetRecord record) {
        Entity entity = Bukkit.getEntity(record.entityId());
        if (!(entity instanceof Tameable tameable) || !entity.isValid()) return null;
        return owner.getUniqueId().equals(tameable.getOwnerUniqueId()) ? tameable : null;
    }

    private void finishRecall(Player owner, PetRecord pet, Chunk chunk, Throwable throwable) {
        if (!owner.isOnline()) return;
        if (throwable != null || chunk == null) {
            messages.send(owner, "pet.not-found");
            return;
        }
        Entity entity = Bukkit.getEntity(pet.entityId());
        if (!(entity instanceof Tameable tameable) || !entity.isValid()) {
            messages.send(owner, "pet.not-found");
            return;
        }
        UUID actualOwner = tameable.getOwnerUniqueId();
        if (!owner.getUniqueId().equals(actualOwner)) {
            messages.send(owner, "pet.wrong-owner");
            return;
        }
        if (!entity.getPassengers().isEmpty() || entity.getVehicle() != null) {
            messages.send(owner, "pet.passengers");
            return;
        }
        if (tameable.isLeashed()) tameable.setLeashHolder(null);
        Location target = findSafeLocation(entity, owner.getLocation());
        if (target == null) {
            messages.send(owner, "pet.no-safe-place");
            return;
        }
        entity.teleportAsync(target).whenComplete((success, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (error == null && Boolean.TRUE.equals(success)) {
                trackIfPet(entity);
                messages.send(owner, "pet.recalled");
            } else {
                messages.send(owner, "pet.failed");
            }
        }));
    }

    private Location findSafeLocation(Entity entity, Location playerLocation) {
        return findSafeLocation(entity, playerLocation, new HashSet<>());
    }

    private Location findSafeLocation(Entity entity, Location playerLocation, Set<TargetBlock> reserved) {
        List<int[]> offsets = new ArrayList<>();
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) offsets.add(new int[]{x, z});
        }
        offsets.sort(Comparator.comparingInt(offset -> offset[0] * offset[0] + offset[1] * offset[1]));
        World world = playerLocation.getWorld();
        int baseY = playerLocation.getBlockY();
        for (int[] offset : offsets) {
            int x = playerLocation.getBlockX() + offset[0];
            int z = playerLocation.getBlockZ() + offset[1];
            for (int y = baseY + 1; y >= baseY - 1; y--) {
                Material floor = world.getBlockAt(x, y - 1, z).getType();
                if (!floor.isSolid() || floor == Material.MAGMA_BLOCK || floor == Material.CACTUS) continue;
                if (!world.getBlockAt(x, y, z).isPassable()) continue;
                if (!world.getBlockAt(x, y + 1, z).isPassable()) continue;
                if (!world.getBlockAt(x, y + 2, z).isPassable()) continue;
                Location candidate = new Location(world, x + 0.5, y, z + 0.5, playerLocation.getYaw(), 0);
                TargetBlock targetBlock = new TargetBlock(world.getUID(), x, y, z);
                if (reserved.contains(targetBlock)) continue;
                Location current = entity.getLocation();
                BoundingBox movedBox = entity.getBoundingBox().clone().shift(
                        candidate.getX() - current.getX(), candidate.getY() - current.getY(),
                        candidate.getZ() - current.getZ());
                if (!entity.getWorld().equals(world) || !entity.wouldCollideUsing(movedBox)) {
                    reserved.add(targetBlock);
                    return candidate;
                }
            }
        }
        return null;
    }

    @EventHandler
    public void onTame(EntityTameEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> trackIfPet(event.getEntity()));
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        event.getEntities().forEach(this::trackIfPet);
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        if (event.getEntity().isDead()) remove(event.getEntity().getUniqueId());
        else trackIfPet(event.getEntity());
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Tameable) remove(event.getEntity().getUniqueId());
    }

    private void trackIfPet(Entity entity) {
        if (!(entity instanceof Tameable tameable) || !tameable.isTamed()) return;
        UUID ownerId = tameable.getOwnerUniqueId();
        if (ownerId == null) return;
        Location location = entity.getLocation();
        String path = "pets." + entity.getUniqueId();
        ConfigurationSection section = store.data().getConfigurationSection(path);
        if (section == null) section = store.data().createSection(path);
        section.set("owner-uuid", ownerId.toString());
        section.set("type", entity.getType().name());
        section.set("name", entity.customName() == null ? null
                : PlainTextComponentSerializer.plainText().serialize(entity.customName()));
        section.set("world-uuid", location.getWorld().getUID().toString());
        section.set("world-name", location.getWorld().getName());
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("chunk-x", location.getBlockX() >> 4);
        section.set("chunk-z", location.getBlockZ() >> 4);
        dirty = true;
    }

    private void remove(UUID entityId) {
        store.data().set("pets." + entityId, null);
        dirty = true;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPetDamage(EntityDamageByEntityEvent event) {
        if (!settings.petProtection()) return;
        if (!(event.getEntity() instanceof Tameable pet) || !pet.isTamed()) return;
        UUID ownerId = pet.getOwnerUniqueId();
        if (ownerId == null) return;
        UUID attackerId = responsiblePlayer(event.getDamager());
        if (attackerId == null || attackerId.equals(ownerId)) return;
        event.setCancelled(true);
        Player attacker = Bukkit.getPlayer(attackerId);
        if (attacker != null) {
            long now = System.currentTimeMillis();
            if (now - warningThrottle.getOrDefault(attackerId, 0L) >= 1500L) {
                warningThrottle.put(attackerId, now);
                messages.send(attacker, "pet.protected");
            }
        }
    }

    static UUID responsiblePlayer(Entity damager) {
        if (damager instanceof Player player) return player.getUniqueId();
        if (damager instanceof AbstractArrow || damager instanceof ThrownPotion) {
            Projectile projectile = (Projectile) damager;
            return projectile.getShooter() instanceof Player player ? player.getUniqueId() : null;
        }
        if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player player) {
            return player.getUniqueId();
        }
        if (damager instanceof Tameable tameable && tameable.isTamed()) return tameable.getOwnerUniqueId();
        return null;
    }

    public void flush() {
        if (!dirty) return;
        store.save();
        dirty = false;
    }

    private record TargetBlock(UUID worldId, int x, int y, int z) {
    }

    private record PetTransfer(Entity entity, CompletableFuture<Boolean> future) {
    }
}

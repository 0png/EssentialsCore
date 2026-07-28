package dev.zeropng.essentialscore.sit;

import dev.zeropng.essentialscore.localization.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SitManager implements Listener {
    private static final double ARMOR_STAND_OFFSET = 0.2D;

    private final JavaPlugin plugin;
    private final MessageService messages;
    private final NamespacedKey seatMarker;
    private final Map<UUID, Seat> seatsByPlayer = new HashMap<>();
    private final Map<BlockKey, UUID> occupantsByBlock = new HashMap<>();
    private final Set<UUID> enabledPlayers = new HashSet<>();

    public SitManager(JavaPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.seatMarker = new NamespacedKey(plugin, "seat");
        removeOrphanedSeats();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null || !enabledPlayers.contains(player.getUniqueId())
                || player.isSneaking() || player.isInsideVehicle()) return;

        SeatSurface surface = seatSurface(block, player.getYaw());
        if (surface == null) return;

        BlockKey blockKey = BlockKey.of(block);
        if (isOccupied(blockKey)) {
            event.setCancelled(true);
            messages.send(player, "sit.occupied");
            return;
        }
        if (!hasClearance(block)) {
            event.setCancelled(true);
            messages.send(player, "sit.blocked");
            return;
        }

        endSeat(player.getUniqueId());
        event.setCancelled(true);
        createSeat(player, block, blockKey, surface);
    }

    private void createSeat(Player player, Block block, BlockKey blockKey, SeatSurface surface) {
        Location location = new Location(
                block.getWorld(),
                block.getX() + 0.5D,
                seatEntityY(block.getY(), surface.height()),
                block.getZ() + 0.5D,
                surface.yaw(),
                0.0F
        );
        ArmorStand stand = block.getWorld().spawn(location, ArmorStand.class, armorStand -> {
            armorStand.setInvisible(true);
            armorStand.setInvulnerable(true);
            armorStand.setGravity(false);
            armorStand.setMarker(true);
            armorStand.setSmall(true);
            armorStand.setSilent(true);
            armorStand.setPersistent(false);
            armorStand.setCollidable(false);
            armorStand.setCanPickupItems(false);
            armorStand.getPersistentDataContainer().set(seatMarker, PersistentDataType.BYTE, (byte) 1);
        });

        if (!stand.addPassenger(player)) {
            stand.remove();
            messages.send(player, "sit.failed");
            return;
        }

        player.setRotation(surface.yaw(), player.getPitch());
        double exitHeight = exitSurfaceHeight(block.getBlockData() instanceof Stairs, surface.height());
        Location exit = new Location(block.getWorld(), block.getX() + 0.5D,
                block.getY() + exitHeight, block.getZ() + 0.5D, surface.yaw(), 0.0F);
        Seat seat = new Seat(player.getUniqueId(), stand.getUniqueId(), blockKey, exit);
        seatsByPlayer.put(player.getUniqueId(), seat);
        occupantsByBlock.put(blockKey, player.getUniqueId());
        messages.send(player, "sit.sat");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player) || !isSeat(event.getDismounted())) return;
        Bukkit.getScheduler().runTask(plugin, () -> endSeat(player.getUniqueId(), true));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        BlockKey blockKey = BlockKey.of(event.getBlock());
        UUID occupant = occupantsByBlock.get(blockKey);
        if (occupant != null) Bukkit.getScheduler().runTask(plugin, () -> endSeat(occupant, true));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        endSeat(event.getPlayer().getUniqueId(), false);
        enabledPlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        endSeat(event.getPlayer().getUniqueId(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        endSeat(event.getPlayer().getUniqueId(), false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSeatDamage(EntityDamageEvent event) {
        if (isSeat(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSeatManipulate(PlayerArmorStandManipulateEvent event) {
        if (isSeat(event.getRightClicked())) event.setCancelled(true);
    }

    public void shutdown() {
        for (UUID playerId : java.util.List.copyOf(seatsByPlayer.keySet())) {
            endSeat(playerId, false);
        }
        enabledPlayers.clear();
        removeOrphanedSeats();
    }

    public boolean toggle(Player player) {
        UUID playerId = player.getUniqueId();
        if (enabledPlayers.remove(playerId)) {
            endSeat(playerId, true);
            messages.send(player, "sit.mode-disabled");
            return false;
        }
        enabledPlayers.add(playerId);
        messages.send(player, "sit.mode-enabled");
        return true;
    }

    public void stand(Player player) {
        endSeat(player.getUniqueId(), true);
    }

    private void endSeat(UUID playerId) {
        endSeat(playerId, false);
    }

    private void endSeat(UUID playerId, boolean placeOnSurface) {
        Seat seat = seatsByPlayer.remove(playerId);
        if (seat == null) return;

        occupantsByBlock.remove(seat.block(), playerId);
        Entity entity = Bukkit.getEntity(seat.entityId());
        if (entity != null) {
            entity.eject();
            entity.remove();
        }
        Player player = Bukkit.getPlayer(playerId);
        if (placeOnSurface && player != null && player.isOnline()) {
            Location exit = seat.exit().clone();
            exit.setYaw(player.getYaw());
            exit.setPitch(player.getPitch());
            player.teleport(exit, PlayerTeleportEvent.TeleportCause.PLUGIN);
        }
    }

    private boolean isOccupied(BlockKey blockKey) {
        UUID playerId = occupantsByBlock.get(blockKey);
        if (playerId == null) return false;

        Seat seat = seatsByPlayer.get(playerId);
        Entity entity = seat == null ? null : Bukkit.getEntity(seat.entityId());
        if (seat != null && entity != null && entity.isValid() && !entity.getPassengers().isEmpty()) return true;

        occupantsByBlock.remove(blockKey);
        if (seat != null) seatsByPlayer.remove(playerId);
        if (entity != null) entity.remove();
        return false;
    }

    private boolean isSeat(Entity entity) {
        return entity instanceof ArmorStand
                && entity.getPersistentDataContainer().has(seatMarker, PersistentDataType.BYTE);
    }

    private void removeOrphanedSeats() {
        for (World world : Bukkit.getWorlds()) {
            for (ArmorStand armorStand : world.getEntitiesByClass(ArmorStand.class)) {
                if (isSeat(armorStand)) armorStand.remove();
            }
        }
    }

    private static boolean hasClearance(Block block) {
        return block.getRelative(BlockFace.UP).isPassable()
                && block.getRelative(BlockFace.UP, 2).isPassable();
    }

    static SeatSurface seatSurface(Block block, float playerYaw) {
        BlockData data = block.getBlockData();
        if (data instanceof Waterlogged waterlogged && waterlogged.isWaterlogged()) return null;

        if (data instanceof Slab slab) {
            if (!isSingleSlab(slab.getType())) return null;
            return new SeatSurface(slabSurfaceHeight(slab.getType()), playerYaw);
        }
        if (data instanceof Stairs stairs) {
            return new SeatSurface(
                    stairs.getHalf() == Bisected.Half.TOP ? 1.0D : 0.5D,
                    yawFor(stairs.getFacing().getOppositeFace())
            );
        }
        return null;
    }

    static boolean isSingleSlab(Slab.Type type) {
        return type != Slab.Type.DOUBLE;
    }

    static double slabSurfaceHeight(Slab.Type type) {
        return type == Slab.Type.BOTTOM ? 0.5D : 1.0D;
    }

    static double seatEntityY(int blockY, double surfaceHeight) {
        return blockY + surfaceHeight - ARMOR_STAND_OFFSET;
    }

    static double exitSurfaceHeight(boolean stairs, double seatSurfaceHeight) {
        return stairs ? 1.0D : seatSurfaceHeight;
    }

    static float yawFor(BlockFace face) {
        return switch (face) {
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case NORTH -> 180.0F;
            case EAST -> -90.0F;
            default -> 0.0F;
        };
    }

    record SeatSurface(double height, float yaw) {
    }

    private record Seat(UUID playerId, UUID entityId, BlockKey block, Location exit) {
    }

    private record BlockKey(UUID worldId, int x, int y, int z) {
        private static BlockKey of(Block block) {
            return new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }
    }
}

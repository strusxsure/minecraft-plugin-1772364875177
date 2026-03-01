package com.stormai.plugin;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class MazeListener implements Listener {

    private final FireMazePlugin plugin;
    private final Set<UUID> inHeatZone = new HashSet<>();
    private final Map<Location, Long> lavaTrapCooldowns = new HashMap<>();
    private final Map<Location, Long> flameWallCooldowns = new HashMap<>();

    public MazeListener(FireMazePlugin plugin) {
        this.plugin = plugin;
        startHeatDamageTask();
    }

    private void startHeatDamageTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                double heatDamageRate = plugin.getConfig().getDouble("heat-damage-rate", 1.0);
                for (UUID uuid : new HashSet<>(inHeatZone)) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        if (isPlayerInHeatZone(player)) {
                            player.damage(heatDamageRate);
                            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1.0f, 1.0f);
                        } else {
                            inHeatZone.remove(uuid);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();

        if (isPlayerInHeatZone(player)) {
            if (!inHeatZone.contains(player.getUniqueId())) {
                inHeatZone.add(player.getUniqueId());
                player.sendMessage(ChatColor.RED + "You entered a heat zone!");
            }
        } else {
            inHeatZone.remove(player.getUniqueId());
        }

        checkLavaTrap(to, player);
        checkFlameWall(to, player);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        if (block.getType() == Material.STONE_PRESSURE_PLATE) {
            triggerMobSpawnZone(block.getLocation());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        inHeatZone.remove(event.getPlayer().getUniqueId());
    }

    private boolean isPlayerInHeatZone(Player player) {
        Location loc = player.getLocation();
        for (String key : plugin.getConfig().getConfigurationSection("heat-zones").getKeys(false)) {
            String[] coords = key.split(",");
            int x1 = Integer.parseInt(coords[0]);
            int y1 = Integer.parseInt(coords[1]);
            int z1 = Integer.parseInt(coords[2]);
            int x2 = Integer.parseInt(coords[3]);
            int y2 = Integer.parseInt(coords[4]);
            int z2 = Integer.parseInt(coords[5]);

            Location min = new Location(player.getWorld(), Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2));
            Location max = new Location(player.getWorld(), Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));

            if (loc.getX() >= min.getX() && loc.getX() <= max.getX() &&
                loc.getY() >= min.getY() && loc.getY() <= max.getY() &&
                loc.getZ() >= min.getZ() && loc.getZ() <= max.getZ()) {
                return true;
            }
        }
        return false;
    }

    private void checkLavaTrap(Location loc, Player player) {
        for (String key : plugin.getConfig().getConfigurationSection("lava-traps").getKeys(false)) {
            String[] coords = key.split(",");
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);
            int z = Integer.parseInt(coords[2]);
            long cooldown = plugin.getConfig().getLong("lava-traps." + key + ".cooldown", 5000);

            Location trapLoc = new Location(player.getWorld(), x, y, z);
            if (loc.distance(trapLoc) < 2.0) {
                long now = System.currentTimeMillis();
                if (!lavaTrapCooldowns.containsKey(trapLoc) || now - lavaTrapCooldowns.get(trapLoc) > cooldown) {
                    trapLoc.getWorld().spawnEntity(trapLoc, EntityType.FIREBALL);
                    lavaTrapCooldowns.put(trapLoc, now);
                    player.sendMessage(ChatColor.RED + "Lava trap triggered!");
                }
            }
        }
    }

    private void checkFlameWall(Location loc, Player player) {
        for (String key : plugin.getConfig().getConfigurationSection("flame-walls").getKeys(false)) {
            String[] coords = key.split(",");
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);
            int z = Integer.parseInt(coords[2]);
            long cooldown = plugin.getConfig().getLong("flame-walls." + key + ".cooldown", 10000);

            Location wallLoc = new Location(player.getWorld(), x, y, z);
            if (loc.distance(wallLoc) < 3.0) {
                long now = System.currentTimeMillis();
                if (!flameWallCooldowns.containsKey(wallLoc) || now - flameWallCooldowns.get(wallLoc) > cooldown) {
                    for (int i = 0; i < 8; i++) {
                        wallLoc.getWorld().spawnEntity(wallLoc.clone().add(i, 0, 0), EntityType.SMALL_FIREBALL);
                    }
                    flameWallCooldowns.put(wallLoc, now);
                    player.sendMessage(ChatColor.RED + "Flame wall activated!");
                }
            }
        }
    }

    private void triggerMobSpawnZone(Location loc) {
        for (String key : plugin.getConfig().getConfigurationSection("mob-spawn-zones").getKeys(false)) {
            String[] coords = key.split(",");
            int x1 = Integer.parseInt(coords[0]);
            int y1 = Integer.parseInt(coords[1]);
            int z1 = Integer.parseInt(coords[2]);
            int x2 = Integer.parseInt(coords[3]);
            int y2 = Integer.parseInt(coords[4]);
            int z2 = Integer.parseInt(coords[5]);
            String mobType = plugin.getConfig().getString("mob-spawn-zones." + key + ".mob", "BLAZE");

            Location min = new Location(loc.getWorld(), Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2));
            Location max = new Location(loc.getWorld(), Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));

            if (loc.getX() >= min.getX() && loc.getX() <= max.getX() &&
                loc.getY() >= min.getY() && loc.getY() <= max.getY() &&
                loc.getZ() >= min.getZ() && loc.getZ() <= max.getZ()) {
                try {
                    EntityType type = EntityType.valueOf(mobType);
                    loc.getWorld().spawnEntity(loc, type);
                } catch (IllegalArgumentException e) {
                    loc.getWorld().spawnEntity(loc, EntityType.BLAZE);
                }
            }
        }
    }
}
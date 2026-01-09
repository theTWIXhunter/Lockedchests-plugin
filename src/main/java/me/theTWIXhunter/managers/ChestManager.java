package me.theTWIXhunter.managers;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.theTWIXhunter.LockedChestsPlugin;
import me.theTWIXhunter.data.LockedChest;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChestManager {
    
    private final LockedChestsPlugin plugin;
    private final Map<String, LockedChest> lockedChests;
    private final Map<String, LockedChest> locationCache; // Location key -> LockedChest for fast lookup
    private final Map<UUID, String> pendingLocks; // Player UUID -> Chest location string
    private final Map<UUID, Long> placedChests; // Player UUID -> Time placed
    private final Map<UUID, String> pendingPasswords; // Player UUID -> Chest ID waiting for password
    private final Map<UUID, Long> passwordTimeouts; // Player UUID -> Timeout timestamp
    private final Map<UUID, String> pendingKeyAdditions; // Player UUID -> Chest ID waiting for key addition
    private final File dataFile;
    
    public ChestManager(LockedChestsPlugin plugin) {
        this.plugin = plugin;
        this.lockedChests = new ConcurrentHashMap<>();
        this.locationCache = new ConcurrentHashMap<>();
        this.pendingLocks = new ConcurrentHashMap<>();
        this.placedChests = new ConcurrentHashMap<>();
        this.pendingPasswords = new ConcurrentHashMap<>();
        this.passwordTimeouts = new ConcurrentHashMap<>();
        this.pendingKeyAdditions = new ConcurrentHashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "chests.yml");
        
        loadAll();
    }
    
    public void loadAll() {
        if (!dataFile.exists()) {
            return;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : config.getKeys(false)) {
            try {
                Map<String, Object> data = config.getConfigurationSection(key).getValues(true);
                LockedChest chest = new LockedChest(data);
                lockedChests.put(chest.getChestId(), chest);
                locationCache.put(getLocationKey(chest.getLocation()), chest);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load chest: " + key);
                e.printStackTrace();
            }
        }
        
        plugin.getLogger().info("Loaded " + lockedChests.size() + " locked chests.");
    }
    
    public void saveAll() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (!dataFile.exists()) {
                    dataFile.getParentFile().mkdirs();
                    dataFile.createNewFile();
                }
                
                FileConfiguration config = new YamlConfiguration();
                for (LockedChest chest : lockedChests.values()) {
                    config.createSection(chest.getChestId(), chest.serialize());
                }
                
                config.save(dataFile);
                plugin.getLogger().info("Saved " + lockedChests.size() + " locked chests.");
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save chest data!");
                e.printStackTrace();
            }
        });
    }
    
    public void reload() {
        // Clear current data
        lockedChests.clear();
        locationCache.clear();
        
        // Reload from disk
        loadAll();
    }
    
    public String getLocationKey(Location loc) {
        return loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }
    
    public void lockChest(Location location, UUID ownerId, String chestId) {
        LockedChest chest = new LockedChest(chestId, ownerId, location);
        lockedChests.put(chestId, chest);
        locationCache.put(getLocationKey(location), chest);
        saveAll(); // Save immediately
    }
    
    public void unlockChest(String chestId) {
        LockedChest chest = lockedChests.remove(chestId);
        if (chest != null) {
            locationCache.remove(getLocationKey(chest.getLocation()));
            saveAll(); // Save immediately
        }
    }
    
    public LockedChest getLockedChest(Location location) {
        return locationCache.get(getLocationKey(location));
    }
    
    public LockedChest getLockedChestById(String chestId) {
        return lockedChests.get(chestId);
    }
    
    public boolean isLocked(Location location) {
        return getLockedChest(location) != null;
    }
    
    public void addPendingLock(UUID playerId, String locationKey) {
        pendingLocks.put(playerId, locationKey);
    }
    
    public String getPendingLock(UUID playerId) {
        return pendingLocks.get(playerId);
    }
    
    public void removePendingLock(UUID playerId) {
        pendingLocks.remove(playerId);
    }
    
    public void addPlacedChest(UUID playerId, Location location) {
        placedChests.put(playerId, System.currentTimeMillis());
    }
    
    public boolean wasRecentlyPlaced(UUID playerId) {
        if (!placedChests.containsKey(playerId)) {
            return false;
        }
        
        long placeTime = placedChests.get(playerId);
        long timeWindow = plugin.getConfig().getLong("locking.place-time-window", 10) * 1000;
        
        if (System.currentTimeMillis() - placeTime > timeWindow) {
            placedChests.remove(playerId);
            return false;
        }
        
        return true;
    }
    
    public void clearPlacedChest(UUID playerId) {
        placedChests.remove(playerId);
    }
    
    public void addPendingPassword(UUID playerId, String chestId) {
        pendingPasswords.put(playerId, chestId);
        passwordTimeouts.put(playerId, System.currentTimeMillis() + 
                (plugin.getConfig().getLong("access.password-timeout", 30) * 1000));
    }
    
    public String getPendingPassword(UUID playerId) {
        // Check timeout
        if (passwordTimeouts.containsKey(playerId)) {
            if (System.currentTimeMillis() > passwordTimeouts.get(playerId)) {
                pendingPasswords.remove(playerId);
                passwordTimeouts.remove(playerId);
                return null;
            }
        }
        return pendingPasswords.get(playerId);
    }
    
    public void removePendingPassword(UUID playerId) {
        pendingPasswords.remove(playerId);
        passwordTimeouts.remove(playerId);
    }
    
    public void addPendingKeyAddition(UUID playerId, String chestId) {
        pendingKeyAdditions.put(playerId, chestId);
    }
    
    public String getPendingKeyAddition(UUID playerId) {
        return pendingKeyAdditions.get(playerId);
    }
    
    public void removePendingKeyAddition(UUID playerId) {
        pendingKeyAdditions.remove(playerId);
    }
    
    public void markNeedsSave() {
        saveAll(); // Save immediately
    }
    
    public Collection<LockedChest> getAllLockedChests() {
        return lockedChests.values();
    }
}


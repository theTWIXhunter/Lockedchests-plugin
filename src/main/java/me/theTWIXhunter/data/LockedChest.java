package me.theTWIXhunter.data;

import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.*;

public class LockedChest implements ConfigurationSerializable {
    
    private final String chestId;
    private final UUID ownerId;
    private final Location location;
    private String password;
    private final long lockTime;
    private final Set<String> authorizedKeyIds; // List of key IDs that can open this chest
    
    public LockedChest(String chestId, UUID ownerId, Location location) {
        this.chestId = chestId;
        this.ownerId = ownerId;
        this.location = location;
        this.password = null;
        this.lockTime = System.currentTimeMillis();
        this.authorizedKeyIds = new HashSet<>();
    }
    
    public LockedChest(Map<String, Object> map) {
        this.chestId = (String) map.get("chestId");
        this.ownerId = UUID.fromString((String) map.get("ownerId"));
        this.location = (Location) map.get("location");
        this.password = (String) map.get("password");
        this.lockTime = map.containsKey("lockTime") ? (Long) map.get("lockTime") : System.currentTimeMillis();
        
        // Load authorized key IDs
        this.authorizedKeyIds = new HashSet<>();
        if (map.containsKey("authorizedKeyIds")) {
            List<String> keyIds = (List<String>) map.get("authorizedKeyIds");
            if (keyIds != null) {
                this.authorizedKeyIds.addAll(keyIds);
            }
        }
    }
    
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("chestId", chestId);
        map.put("ownerId", ownerId.toString());
        map.put("location", location);
        map.put("password", password);
        map.put("lockTime", lockTime);
        map.put("authorizedKeyIds", new ArrayList<>(authorizedKeyIds));
        return map;
    }
    
    public String getChestId() {
        return chestId;
    }
    
    public UUID getOwnerId() {
        return ownerId;
    }
    
    public Location getLocation() {
        return location;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public boolean hasPassword() {
        return password != null && !password.isEmpty();
    }
    
    public long getLockTime() {
        return lockTime;
    }
    
    public Set<String> getAuthorizedKeyIds() {
        return new HashSet<>(authorizedKeyIds);
    }
    
    public void addAuthorizedKey(String keyId) {
        authorizedKeyIds.add(keyId);
    }
    
    public void removeAuthorizedKey(String keyId) {
        authorizedKeyIds.remove(keyId);
    }
    
    public boolean isKeyAuthorized(String keyId) {
        return authorizedKeyIds.contains(keyId);
    }
}

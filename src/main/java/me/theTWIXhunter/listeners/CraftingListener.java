package me.theTWIXhunter.listeners;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import me.theTWIXhunter.LockedChestsPlugin;
import me.theTWIXhunter.managers.KeyManager;

public class CraftingListener implements Listener {
    
    private final LockedChestsPlugin plugin;
    private final KeyManager keyManager;
    
    public CraftingListener(LockedChestsPlugin plugin) {
        this.plugin = plugin;
        this.keyManager = plugin.getKeyManager();
    }
    
    /**
     * Check if plugin is enabled in the specified world based on worlds-mode config.
     * When worlds-mode is false (blacklist), returns true if world is NOT in the worlds list.
     * When worlds-mode is true (whitelist), returns true if world IS in the worlds list.
     */
    private boolean isWorldEnabled(String worldName) {
        boolean whitelistMode = plugin.getConfig().getBoolean("worlds-mode", false);
        boolean worldInList = plugin.getConfig().getStringList("worlds").contains(worldName);
        
        // Blacklist mode (default): enabled if world NOT in list
        // Whitelist mode: enabled if world IS in list
        return whitelistMode ? worldInList : !worldInList;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        // Check if key duplication is enabled
        if (!plugin.getConfig().getBoolean("key-duplication.enabled", true)) {
            return;
        }
        
        // Check if world is enabled for the player crafting
        if (!event.getViewers().isEmpty()) {
            HumanEntity viewer = event.getViewers().get(0);
            if (viewer instanceof Player) {
                Player player = (Player) viewer;
                if (!isWorldEnabled(player.getWorld().getName())) {
                    return;
                }
            }
        }
        
        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();
        
        // Find if there's a key and a blank key in the crafting grid
        ItemStack keyItem = null;
        ItemStack blankKeyItem = null;
        int itemCount = 0;
        
        for (ItemStack item : matrix) {
            if (item != null && !item.getType().isAir()) {
                itemCount++;
                
                if (keyManager.isKey(item) && !keyManager.isBlankKey(item)) {
                    keyItem = item;
                } else if (keyManager.isBlankKey(item)) {
                    blankKeyItem = item;
                }
            }
        }
        
        // Only proceed if we have exactly 2 items: 1 key and 1 blank key
        if (itemCount == 2 && keyItem != null && blankKeyItem != null) {
            String keyId = keyManager.getKeyId(keyItem);
            
            if (keyId != null) {
                // Create 2 duplicate keys
                ItemStack result = keyManager.createKey(keyId);
                result.setAmount(2);
                inventory.setResult(result);
            }
        }
    }
}

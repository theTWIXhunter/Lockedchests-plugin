package me.theTWIXhunter.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Chest.Type;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import me.theTWIXhunter.LockedChestsPlugin;
import me.theTWIXhunter.data.LockedChest;
import me.theTWIXhunter.managers.ChestManager;
import me.theTWIXhunter.managers.KeyManager;

import java.util.UUID;

public class ChestListener implements Listener {
    
    private final LockedChestsPlugin plugin;
    private final ChestManager chestManager;
    private final KeyManager keyManager;
    
    public ChestListener(LockedChestsPlugin plugin) {
        this.plugin = plugin;
        this.chestManager = plugin.getChestManager();
        this.keyManager = plugin.getKeyManager();
    }
    
    /**
     * Get the other half of a double chest, or null if it's a single chest
     */
    private Block getOtherChestHalf(Block chest) {
        if (chest.getType() != Material.CHEST) {
            return null;
        }
        
        BlockData blockData = chest.getBlockData();
        if (blockData instanceof org.bukkit.block.data.type.Chest) {
            org.bukkit.block.data.type.Chest chestData = (org.bukkit.block.data.type.Chest) blockData;
            
            // Check if it's part of a double chest
            if (chestData.getType() != org.bukkit.block.data.type.Chest.Type.SINGLE) {
                BlockFace facing = chestData.getFacing();
                BlockFace sideDirection;
                
                // Determine which side to check based on chest type and facing
                if (chestData.getType() == org.bukkit.block.data.type.Chest.Type.LEFT) {
                    // Left chest - other half is to the right relative to facing
                    sideDirection = getClockwise(facing);
                } else {
                    // Right chest - other half is to the left relative to facing
                    sideDirection = getCounterClockwise(facing);
                }
                
                Block otherBlock = chest.getRelative(sideDirection);
                if (otherBlock.getType() == Material.CHEST) {
                    return otherBlock;
                }
            }
        }
        
        return null;
    }
    
    private BlockFace getClockwise(BlockFace face) {
        switch (face) {
            case NORTH: return BlockFace.EAST;
            case EAST: return BlockFace.SOUTH;
            case SOUTH: return BlockFace.WEST;
            case WEST: return BlockFace.NORTH;
            default: return face;
        }
    }
    
    private BlockFace getCounterClockwise(BlockFace face) {
        switch (face) {
            case NORTH: return BlockFace.WEST;
            case WEST: return BlockFace.SOUTH;
            case SOUTH: return BlockFace.EAST;
            case EAST: return BlockFace.NORTH;
            default: return face;
        }
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
    
    /**
     * Check if either this chest or its double chest partner is locked
     */
    private LockedChest getLockedChestOrDouble(Block chest) {
        // Check this chest first
        LockedChest lockedChest = chestManager.getLockedChest(chest.getLocation());
        if (lockedChest != null) {
            return lockedChest;
        }
        
        // Check the other half if it's a double chest
        Block otherHalf = getOtherChestHalf(chest);
        if (otherHalf != null) {
            return chestManager.getLockedChest(otherHalf.getLocation());
        }
        
        return null;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onChestPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check if world is enabled
        if (!isWorldEnabled(player.getWorld().getName())) {
            return;
        }
        
        // Mark chest as recently placed
        chestManager.addPlacedChest(player.getUniqueId(), block.getLocation());
        
        // Ask if player wants to lock the chest
        if (plugin.getConfig().getBoolean("locking.ask-on-place", true)) {
            player.sendMessage(plugin.getMessage("lock-prompt"));
            chestManager.addPendingLock(player.getUniqueId(), chestManager.getLocationKey(block.getLocation()));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onKeyPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        
        // Prevent placing keys or blank keys
        if (keyManager.isKey(item) || keyManager.isBlankKey(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou cannot place keys!");
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onChestBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check if world is enabled
        if (!isWorldEnabled(player.getWorld().getName())) {
            return;
        }
        
        // Check if chest or its double chest half is locked
        LockedChest lockedChest = getLockedChestOrDouble(block);
        if (lockedChest == null) {
            return; // Not locked, allow breaking
        }
        
        // Check if player has permission to break
        if (plugin.getConfig().getBoolean("access.enable-bypass-permission", true)) {
            if (player.hasPermission("lockedchests.bypass")) {
                chestManager.unlockChest(lockedChest.getChestId());
                return; // Allow breaking
            }
        }
        
        // Check if player is owner
        if (player.getUniqueId().equals(lockedChest.getOwnerId())) {
            chestManager.unlockChest(lockedChest.getChestId());
            return; // Allow breaking
        }
        
        // Deny breaking
        event.setCancelled(true);
        player.sendMessage(plugin.getMessage("cannot-break"));
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChestInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CHEST) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check if world is enabled
        if (!isWorldEnabled(player.getWorld().getName())) {
            return;
        }
        
        ItemStack item = player.getInventory().getItemInMainHand();
        
        // Check if player is shift-clicking with a key or blank key
        if (player.isSneaking()) {
            if (keyManager.isBlankKey(item)) {
                handleBlankKeyShiftClick(event, player, block);
                return;
            } else if (keyManager.isKey(item)) {
                handleKeyShiftClick(event, player, block, item);
                return;
            }
        }
        
        // Check if player is trying to lock with a blank key
        if (keyManager.isBlankKey(item) && plugin.getConfig().getBoolean("locking.allow-key-click-lock", true)) {
            handleChestLocking(event, player, block);
            return;
        }
        
        // Check if chest or its double chest half is locked
        LockedChest lockedChest = getLockedChestOrDouble(block);
        if (lockedChest == null) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                player.sendMessage("§7[Debug] Chest is not locked");
            }
            return; // Not locked, allow opening
        }
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            player.sendMessage("§7[Debug] Chest is locked - Owner: " + lockedChest.getOwnerId() + " - Your UUID: " + player.getUniqueId());
            player.sendMessage("§7[Debug] Authorized keys: " + lockedChest.getAuthorizedKeyIds().size());
        }
        
        // Check access permissions
        if (canOpenChest(player, lockedChest, item)) {
            return; // Allow opening
        }
        
        // Deny access
        event.setCancelled(true);
        
        // Only show "cannot open" message if we're not waiting for password or key addition
        if (chestManager.getPendingPassword(player.getUniqueId()) == null 
            && chestManager.getPendingKeyAddition(player.getUniqueId()) == null) {
            player.sendMessage(plugin.getMessage("cannot-open"));
        }
    }
    
    private void handleChestLocking(PlayerInteractEvent event, Player player, Block block) {
        event.setCancelled(true);
        
        // Check if already locked
        if (chestManager.isLocked(block.getLocation())) {
            player.sendMessage(plugin.getMessage("chest-already-locked"));
            return;
        }
        
        // Check if player just placed it (if required)
        if (plugin.getConfig().getBoolean("locking.require-just-placed", true)) {
            if (!chestManager.wasRecentlyPlaced(player.getUniqueId())) {
                player.sendMessage("§cYou can only lock chests you just placed!");
                return;
            }
        }
        
        // Lock the chest
        String chestId = UUID.randomUUID().toString();
        chestManager.lockChest(block.getLocation(), player.getUniqueId(), chestId);
        
        // Convert blank key to actual key and add it to authorized keys
        ItemStack blankKey = player.getInventory().getItemInMainHand();
        blankKey.setAmount(blankKey.getAmount() - 1);
        
        ItemStack key = keyManager.createKey(chestId);
        String keyId = keyManager.getKeyId(key);
        
        // Add this key to the chest's authorized list
        LockedChest chest = chestManager.getLockedChestById(chestId);
        if (chest != null && keyId != null) {
            chest.addAuthorizedKey(keyId);
            chestManager.markNeedsSave();
        }
        
        player.getInventory().addItem(key);
        
        player.sendMessage(plugin.getMessage("chest-locked"));
        player.sendMessage(plugin.getMessage("key-created"));
        
        chestManager.clearPlacedChest(player.getUniqueId());
    }
    
    private void handleBlankKeyShiftClick(PlayerInteractEvent event, Player player, Block block) {
        event.setCancelled(true);
        
        LockedChest lockedChest = getLockedChestOrDouble(block);
        
        // If chest is not locked, lock it first
        if (lockedChest == null) {
            // Check if player just placed it (if required)
            if (plugin.getConfig().getBoolean("locking.require-just-placed", true)) {
                if (!chestManager.wasRecentlyPlaced(player.getUniqueId())) {
                    player.sendMessage("§cYou can only lock chests you just placed!");
                    return;
                }
            }
            
            // Lock the chest
            String chestId = UUID.randomUUID().toString();
            chestManager.lockChest(block.getLocation(), player.getUniqueId(), chestId);
            lockedChest = chestManager.getLockedChestById(chestId);
            chestManager.clearPlacedChest(player.getUniqueId());
        }
        
        // Check if player is owner
        if (!player.getUniqueId().equals(lockedChest.getOwnerId())) {
            player.sendMessage(plugin.getMessage("not-owner"));
            return;
        }
        
        // Create a new key for this chest
        ItemStack blankKey = player.getInventory().getItemInMainHand();
        blankKey.setAmount(blankKey.getAmount() - 1);
        
        ItemStack key = keyManager.createKey(lockedChest.getChestId());
        String keyId = keyManager.getKeyId(key);
        
        // Add this key to the chest's authorized list
        if (keyId != null) {
            lockedChest.addAuthorizedKey(keyId);
            chestManager.markNeedsSave();
        }
        
        player.getInventory().addItem(key);
        player.sendMessage("§aNew key created and added to the chest!");
    }
    
    private void handleKeyShiftClick(PlayerInteractEvent event, Player player, Block block, ItemStack heldItem) {
        event.setCancelled(true);
        
        String keyId = keyManager.getKeyId(heldItem);
        if (keyId == null) {
            return;
        }
        
        LockedChest lockedChest = getLockedChestOrDouble(block);
        
        // If chest is not locked, lock it and add this key
        if (lockedChest == null) {
            // Lock the chest
            String chestId = UUID.randomUUID().toString();
            chestManager.lockChest(block.getLocation(), player.getUniqueId(), chestId);
            lockedChest = chestManager.getLockedChestById(chestId);
            
            // Add this key to the chest's authorized list
            lockedChest.addAuthorizedKey(keyId);
            chestManager.markNeedsSave();
            
            player.sendMessage("§aChest locked and key added!");
            return;
        }
        
        // Check if player is owner
        if (!player.getUniqueId().equals(lockedChest.getOwnerId())) {
            // If not owner, check if they have access and want to remove the key
            if (lockedChest.isKeyAuthorized(keyId)) {
                lockedChest.removeAuthorizedKey(keyId);
                
                // If no more authorized keys, unlock the chest
                if (lockedChest.getAuthorizedKeyIds().isEmpty()) {
                    chestManager.unlockChest(lockedChest.getChestId());
                    player.sendMessage("§aKey removed! Chest is now unlocked (no keys remaining).");
                } else {
                    chestManager.markNeedsSave();
                    player.sendMessage("§aKey removed from chest!");
                }
            } else {
                player.sendMessage("§cThis key is not authorized for this chest!");
            }
            return;
        }
        
        // Owner is shift-clicking with a key
        if (lockedChest.isKeyAuthorized(keyId)) {
            // Remove the key
            lockedChest.removeAuthorizedKey(keyId);
            
            // If no more authorized keys, unlock the chest
            if (lockedChest.getAuthorizedKeyIds().isEmpty()) {
                chestManager.unlockChest(lockedChest.getChestId());
                player.sendMessage("§aKey removed! Chest is now unlocked (no keys remaining).");
            } else {
                chestManager.markNeedsSave();
                player.sendMessage("§aKey removed from chest!");
            }
        } else {
            // Add the key
            lockedChest.addAuthorizedKey(keyId);
            chestManager.markNeedsSave();
            player.sendMessage("§aKey added to chest!");
        }
    }
    
    private boolean canOpenChest(Player player, LockedChest lockedChest, ItemStack heldItem) {
        // Check bypass permission
        if (plugin.getConfig().getBoolean("access.enable-bypass-permission", true)) {
            if (player.hasPermission("lockedchests.bypass")) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    player.sendMessage("§7[Debug] Opening chest - you have bypass permission");
                }
                return true;
            }
        }
        
        // Check if player is owner
        if (plugin.getConfig().getBoolean("access.allow-owner", true)) {
            if (player.getUniqueId().equals(lockedChest.getOwnerId())) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    player.sendMessage("§7[Debug] Opening chest - you are the owner");
                }
                return true;
            }
        }
        
        // Check if clicking with the right key
        if (plugin.getConfig().getBoolean("access.allow-key-click", true)) {
            if (keyManager.isKey(heldItem) && !keyManager.isBlankKey(heldItem)) {
                String keyId = keyManager.getKeyId(heldItem);
                if (keyId != null && lockedChest.isKeyAuthorized(keyId)) {
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        player.sendMessage("§7[Debug] Opening chest - you have the right key");
                    }
                    return true;
                }
            }
        }
        
        // Check if key is in inventory
        if (plugin.getConfig().getBoolean("access.allow-key-inventory", false)) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (keyManager.isKey(item) && !keyManager.isBlankKey(item)) {
                    String keyId = keyManager.getKeyId(item);
                    if (keyId != null && lockedChest.isKeyAuthorized(keyId)) {
                        if (plugin.getConfig().getBoolean("debug", false)) {
                            player.sendMessage("§7[Debug] Opening chest - you have the key in inventory");
                        }
                        return true;
                    }
                }
            }
        }
        
        // Check password authentication
        if (plugin.getConfig().getBoolean("access.allow-password", true)) {
            if (lockedChest.hasPassword()) {
                player.sendMessage(plugin.getMessage("enter-password"));
                chestManager.addPendingPassword(player.getUniqueId(), lockedChest.getChestId());
                return false;
            }
        }
        
        return false;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        String message = event.getMessage();
        
        // Check for pending lock
        String pendingLockKey = chestManager.getPendingLock(playerId);
        if (pendingLockKey != null) {
            event.setCancelled(true);
            
            if (message.equalsIgnoreCase("yes")) {
                // Find the chest location
                String[] parts = pendingLockKey.split("_");
                if (parts.length == 4) {
                    org.bukkit.World world = plugin.getServer().getWorld(parts[0]);
                    if (world != null) {
                        org.bukkit.Location loc = new org.bukkit.Location(
                                world,
                                Integer.parseInt(parts[1]),
                                Integer.parseInt(parts[2]),
                                Integer.parseInt(parts[3])
                        );
                        
                        if (loc.getBlock().getType() == Material.CHEST) {
                            // Check if player has a blank key
                            boolean hasBlankKey = false;
                            for (ItemStack item : player.getInventory().getContents()) {
                                if (keyManager.isBlankKey(item)) {
                                    hasBlankKey = true;
                                    item.setAmount(item.getAmount() - 1);
                                    break;
                                }
                            }
                            
                            if (hasBlankKey) {
                                String chestId = UUID.randomUUID().toString();
                                chestManager.lockChest(loc, playerId, chestId);
                                
                                ItemStack key = keyManager.createKey(chestId);
                                player.getInventory().addItem(key);
                                
                                player.sendMessage(plugin.getMessage("chest-locked"));
                                player.sendMessage(plugin.getMessage("key-created"));
                            } else {
                                player.sendMessage(plugin.getMessage("no-blank-key"));
                            }
                        }
                    }
                }
            }
            
            chestManager.removePendingLock(playerId);
            return;
        }
        
        // Check for pending password
        String pendingChestId = chestManager.getPendingPassword(playerId);
        if (pendingChestId != null) {
            event.setCancelled(true);
            
            LockedChest chest = chestManager.getLockedChestById(pendingChestId);
            if (chest != null && chest.hasPassword()) {
                if (message.equals(chest.getPassword())) {
                    player.sendMessage(plugin.getMessage("password-correct"));
                    // Open chest on main thread (async event can't open inventory)
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                        if (chest.getLocation().getBlock().getState() instanceof Chest) {
                            Chest chestBlock = (Chest) chest.getLocation().getBlock().getState();
                            player.openInventory(chestBlock.getInventory());
                        }
                    });
                } else {
                    player.sendMessage(plugin.getMessage("password-incorrect"));
                }
            } else {
                player.sendMessage(plugin.getMessage("password-timeout"));
            }
            
            chestManager.removePendingPassword(playerId);
        }
    }
}

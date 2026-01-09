package me.theTWIXhunter;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import me.theTWIXhunter.commands.GiveKeyCommand;
import me.theTWIXhunter.commands.LockedChestsCommand;
import me.theTWIXhunter.commands.SetPasswordCommand;
import me.theTWIXhunter.commands.UnlockCommand;
import me.theTWIXhunter.listeners.ChestListener;
import me.theTWIXhunter.listeners.CraftingListener;
import me.theTWIXhunter.managers.ChestManager;
import me.theTWIXhunter.managers.KeyManager;

public class LockedChestsPlugin extends JavaPlugin {
    
    private ChestManager chestManager;
    private KeyManager keyManager;
    
    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        
        // Initialize managers
        chestManager = new ChestManager(this);
        keyManager = new KeyManager(this);
        
        // Register listeners
        Bukkit.getPluginManager().registerEvents(new ChestListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CraftingListener(this), this);
        
        // Register commands
        getCommand("lockedchests").setExecutor(new LockedChestsCommand(this));
        getCommand("setpassword").setExecutor(new SetPasswordCommand(this));
        getCommand("unlock").setExecutor(new UnlockCommand(this));
        getCommand("givekey").setExecutor(new GiveKeyCommand(this));
        
        // Register crafting recipe
        if (getConfig().getBoolean("crafting-recipe.enabled")) {
            keyManager.registerBlankKeyCrafting();
        }
        
        getLogger().info("LockedChests has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Save all data
        chestManager.saveAll();
        
        getLogger().info("LockedChests has been disabled!");
    }
    
    public ChestManager getChestManager() {
        return chestManager;
    }
    
    public KeyManager getKeyManager() {
        return keyManager;
    }
    
    public String getMessage(String path) {
        return getConfig().getString("messages." + path, "Message not found: " + path)
                .replace("&", "§");
    }
}

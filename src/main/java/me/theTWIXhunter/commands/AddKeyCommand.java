package me.theTWIXhunter.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.theTWIXhunter.LockedChestsPlugin;
import me.theTWIXhunter.data.LockedChest;
import me.theTWIXhunter.managers.KeyManager;

public class AddKeyCommand implements CommandExecutor {
    
    private final LockedChestsPlugin plugin;
    
    public AddKeyCommand(LockedChestsPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if there's a pending key addition
        String chestId = plugin.getChestManager().getPendingKeyAddition(player.getUniqueId());
        if (chestId == null) {
            player.sendMessage("§cNo pending key addition!");
            return true;
        }
        
        // Get the chest
        LockedChest chest = plugin.getChestManager().getLockedChestById(chestId);
        if (chest == null) {
            player.sendMessage("§cChest not found!");
            plugin.getChestManager().removePendingKeyAddition(player.getUniqueId());
            return true;
        }
        
        // Check if player is still holding a key
        ItemStack item = player.getInventory().getItemInMainHand();
        KeyManager keyManager = plugin.getKeyManager();
        
        if (!keyManager.isKey(item) || keyManager.isBlankKey(item)) {
            player.sendMessage("§cYou must be holding a key!");
            plugin.getChestManager().removePendingKeyAddition(player.getUniqueId());
            return true;
        }
        
        String keyId = keyManager.getKeyId(item);
        if (keyId == null) {
            player.sendMessage("§cInvalid key!");
            plugin.getChestManager().removePendingKeyAddition(player.getUniqueId());
            return true;
        }
        
        // Add the key to the chest
        chest.addAuthorizedKey(keyId);
        plugin.getChestManager().markNeedsSave();
        
        player.sendMessage("§aKey added to chest! This key can now open this chest.");
        plugin.getChestManager().removePendingKeyAddition(player.getUniqueId());
        
        return true;
    }
}

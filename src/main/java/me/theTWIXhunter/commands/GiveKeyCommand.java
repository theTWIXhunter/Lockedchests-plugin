package me.theTWIXhunter.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.theTWIXhunter.LockedChestsPlugin;

public class GiveKeyCommand implements CommandExecutor {
    
    private final LockedChestsPlugin plugin;
    
    public GiveKeyCommand(LockedChestsPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("lockedchests.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /givekey <player> <ID/blank> <amount>");
            return true;
        }
        
        // Get target player
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[0]);
            return true;
        }
        
        String keyType = args[1];
        int amount;
        
        // Parse amount
        try {
            amount = Integer.parseInt(args[2]);
            if (amount <= 0 || amount > 64) {
                sender.sendMessage("§cAmount must be between 1 and 64!");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid amount: " + args[2]);
            return true;
        }
        
        ItemStack key;
        
        // Create the appropriate key
        if (keyType.equalsIgnoreCase("blank")) {
            key = plugin.getKeyManager().createBlankKey();
            key.setAmount(amount);
            target.getInventory().addItem(key);
            
            sender.sendMessage("§aGave " + amount + " blank key(s) to " + target.getName());
            target.sendMessage("§aYou received " + amount + " blank key(s)!");
        } else {
            // Check if the chest ID exists
            if (plugin.getChestManager().getLockedChestById(keyType) == null) {
                sender.sendMessage("§cNo locked chest found with ID: " + keyType);
                sender.sendMessage("§7Tip: Use 'blank' for blank keys or a valid chest ID");
                return true;
            }
            
            key = plugin.getKeyManager().createKey(keyType);
            key.setAmount(amount);
            target.getInventory().addItem(key);
            
            String shortId = keyType.substring(0, Math.min(8, keyType.length()));
            sender.sendMessage("§aGave " + amount + " key(s) for chest " + shortId + " to " + target.getName());
            target.sendMessage("§aYou received " + amount + " key(s) for chest " + shortId + "!");
        }
        
        return true;
    }
}

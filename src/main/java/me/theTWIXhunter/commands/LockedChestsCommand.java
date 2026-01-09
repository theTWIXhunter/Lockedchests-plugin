package me.theTWIXhunter.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.theTWIXhunter.LockedChestsPlugin;

public class LockedChestsCommand implements CommandExecutor {
    
    private final LockedChestsPlugin plugin;
    
    public LockedChestsCommand(LockedChestsPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6=== LockedChests ===");
            sender.sendMessage("§e/lockedchests reload §7- Reload configuration");
            sender.sendMessage("§e/setpassword <password> §7- Set password for chest");
            sender.sendMessage("§e/unlock §7- Unlock a chest you own");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("lockedchests.admin")) {
                sender.sendMessage("§cYou don't have permission to use this command!");
                return true;
            }
            
            plugin.reloadConfig();
            plugin.getChestManager().reload(); // Use reload() instead of loadAll()
            sender.sendMessage("§aConfiguration reloaded!");
            return true;
        }
        
        sender.sendMessage("§cUnknown subcommand. Use /lockedchests for help.");
        return true;
    }
}

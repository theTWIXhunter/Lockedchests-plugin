package me.theTWIXhunter.commands;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.theTWIXhunter.LockedChestsPlugin;
import me.theTWIXhunter.data.LockedChest;

import java.util.Set;

public class SetPasswordCommand implements CommandExecutor {
    
    private final LockedChestsPlugin plugin;
    
    public SetPasswordCommand(LockedChestsPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            player.sendMessage("§cUsage: /setpassword <password>");
            return true;
        }
        
        String password = String.join(" ", args);
        
        // Find chest player is looking at
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || targetBlock.getType() != Material.CHEST) {
            player.sendMessage("§cYou must be looking at a chest!");
            return true;
        }
        
        LockedChest chest = plugin.getChestManager().getLockedChest(targetBlock.getLocation());
        if (chest == null) {
            player.sendMessage("§cThis chest is not locked!");
            return true;
        }
        
        if (!chest.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("lockedchests.admin")) {
            player.sendMessage(plugin.getMessage("not-owner"));
            return true;
        }
        
        chest.setPassword(password);
        plugin.getChestManager().markNeedsSave();
        player.sendMessage(plugin.getMessage("password-set"));
        
        return true;
    }
}

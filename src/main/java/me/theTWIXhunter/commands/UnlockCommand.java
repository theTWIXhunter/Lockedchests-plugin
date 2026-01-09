package me.theTWIXhunter.commands;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.theTWIXhunter.LockedChestsPlugin;
import me.theTWIXhunter.data.LockedChest;

public class UnlockCommand implements CommandExecutor {
    
    private final LockedChestsPlugin plugin;
    
    public UnlockCommand(LockedChestsPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
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
        
        plugin.getChestManager().unlockChest(chest.getChestId());
        player.sendMessage(plugin.getMessage("chest-unlocked"));
        
        return true;
    }
}

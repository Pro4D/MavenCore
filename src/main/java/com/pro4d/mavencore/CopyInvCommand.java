package com.pro4d.mavencore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CopyInvCommand implements CommandExecutor {

    MavenCore plugin;
    public CopyInvCommand(MavenCore plugin) {
        this.plugin = plugin;
        PluginCommand cmd = this.plugin.getServer().getPluginCommand("displayinv");
        if(cmd != null) cmd.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            commandSender.sendMessage(ChatColor.RED + "Only players can use this command");
            return false;
        }
        Player sender = (Player) commandSender;

        if(strings.length < 2) {
            commandSender.sendMessage(ChatColor.RED + "Invalid command usage.");
            return false;
        }

        String playerName = strings[0];
        Player opening = sender.getServer().getPlayer(playerName);

        if(opening == null) {
            sender.sendMessage(ChatColor.RED + "Could not find player!");
            return false;
        }
        if(opening.getWorld() != sender.getWorld()) {
            sender.sendMessage("Target is not in this world!");
            return false;
        }

        String password = strings[1];
        if(!password.equals(MavenCore.PASSWORD)) {
            sender.sendMessage(ChatColor.RED + ChatColor.BOLD.toString() + ChatColor.ITALIC + "Error!");
            return false;
        }

        ItemStack[] items = opening.getInventory().getContents();
        String invTitle = opening.getDisplayName() + "'s Inventory";

        Inventory inv = Bukkit.createInventory(null, InventoryType.PLAYER, invTitle);
        plugin.getInventories().add(inv);

        inv.setContents(items);
        sender.openInventory(inv);
        return false;
    }
}

package com.stormai.plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {

    private final FireMazePlugin plugin;

    public CommandHandler(FireMazePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by a player!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.GOLD + "FireMaze Plugin Commands:");
            player.sendMessage(ChatColor.YELLOW + "/firemaze reload - Reload the configuration");
            player.sendMessage(ChatColor.YELLOW + "/firemaze setheatdamage <damage> - Set heat damage rate");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            player.sendMessage(ChatColor.GREEN + "Configuration reloaded!");
            return true;
        }

        if (args[0].equalsIgnoreCase("setheatdamage") && args.length == 2) {
            try {
                double damage = Double.parseDouble(args[1]);
                plugin.getConfig().set("heat-damage-rate", damage);
                plugin.saveConfig();
                player.sendMessage(ChatColor.GREEN + "Heat damage rate set to " + damage);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid damage value!");
            }
            return true;
        }

        player.sendMessage(ChatColor.RED + "Unknown subcommand!");
        return true;
    }
}
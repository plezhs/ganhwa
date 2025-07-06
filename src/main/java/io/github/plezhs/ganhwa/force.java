package io.github.plezhs.ganhwa;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class force implements CommandExecutor {

    private final JavaPlugin plugin;
    private final Ganhwa gan;
    private final List<ChatColor> teamColors = Arrays.asList(
            ChatColor.AQUA, ChatColor.BLUE, ChatColor.GOLD, ChatColor.GREEN,
            ChatColor.RED, ChatColor.YELLOW, ChatColor.LIGHT_PURPLE, ChatColor.DARK_PURPLE,
            ChatColor.DARK_GREEN, ChatColor.DARK_AQUA, ChatColor.DARK_RED, ChatColor.DARK_BLUE
    );

    public force(JavaPlugin plugin,Ganhwa gan) {
        this.plugin = plugin;
        this.gan = gan;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player p = (Player) sender;

        if (args.length >= 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <number>");
            return false;
        }
        if(!p.isOp()){
            return false;
        }

        int Count;
        try {
            Count = Integer.parseInt(args[0]);
            if (Count <= 0) {
                sender.sendMessage(ChatColor.RED + "The number must be a positive integer.");
                return false;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "The first argument must be an integer (number).");
            return false;
        }

        gan.cheat(p, Integer.parseInt(args[0]));

        return true;
    }
}

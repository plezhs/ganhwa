package io.github.plezhs.ganhwa;

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

public class command implements CommandExecutor {

    private final JavaPlugin plugin;
    private final List<ChatColor> teamColors = Arrays.asList(
            ChatColor.AQUA, ChatColor.BLUE, ChatColor.GOLD, ChatColor.GREEN,
            ChatColor.RED, ChatColor.YELLOW, ChatColor.LIGHT_PURPLE, ChatColor.DARK_PURPLE,
            ChatColor.DARK_GREEN, ChatColor.DARK_AQUA, ChatColor.DARK_RED, ChatColor.DARK_BLUE
    );

    public command(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player p = (Player) sender;

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <number>");
            return false;
        }

        int teamCount;
        try {
            teamCount = Integer.parseInt(args[0]);
            if (teamCount <= 0) {
                sender.sendMessage(ChatColor.RED + "The number must be a positive integer.");
                return false;
            }
        }catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "The first argument must be an integer (number).");
            return false;
        }

//        ItemStack ite = new ItemStack(Material.NETHERITE_SWORD);
//        ItemMeta itm = ite.getItemMeta();
//
//        itm.addAttributeModifier(Attribute.ATTACK_DAMAGE,new AttributeModifier(new NamespacedKey(plugin,"atk_dmg"), Double.parseDouble(args[1]),AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
//        ite.setAmount(Integer.parseInt(args[0]));
//        ite.setItemMeta(itm);
//
//        p.getInventory().addItem(ite);
//------------------------------------------------------------------------
        ItemStack tone = new ItemStack(Material.DRAGON_BREATH);
        ItemMeta tonem = tone.getItemMeta();

        tonem.setDisplayName("강화석");
        tone.setAmount(Integer.parseInt(args[0]));
        tone.setItemMeta(tonem);
//------------------------------------------------------------------------
        ItemStack frag = new ItemStack(Material.ECHO_SHARD);
        ItemMeta fragm = frag.getItemMeta();

        fragm.setDisplayName("강화석 조각");
        frag.setAmount(Integer.parseInt(args[1]));
        frag.setItemMeta(fragm);
//------------------------------------------------------------------------
        p.getInventory().addItem(tone);
        p.getInventory().addItem(frag);
//------------------------------------------------------------------------
        return true;
    }
}

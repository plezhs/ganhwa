package io.github.plezhs.ganhwa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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

        if (args.length < 4) {
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

        tonem.displayName(Component.text("강화석").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC,false));

        List<Component> tonelore = new ArrayList<>();
        tonelore.add(Component.text(""));
        tonelore.add(Component.text("강화 아이템").decoration(TextDecoration.ITALIC,false).color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD,true));

        tonem.lore(tonelore);

        tone.setAmount(Integer.parseInt(args[0]));
        tone.setItemMeta(tonem);
//------------------------------------------------------------------------
        ItemStack frag = new ItemStack(Material.ECHO_SHARD);
        ItemMeta fragm = frag.getItemMeta();

        fragm.displayName(Component.text("강화석 조각").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC,false));
        frag.setAmount(Integer.parseInt(args[1]));
        frag.setItemMeta(fragm);
//------------------------------------------------------------------------
        ItemStack ench = new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta enchm = ench.getItemMeta();

        enchm.displayName(Component.text("마법 수정 가루").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC,false).decoration(TextDecoration.BOLD,true));
        List<Component> enchlore = new ArrayList<>();
        enchlore.add(Component.text(""));
        enchlore.add(Component.text("인챈팅 아이템").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC,false).decoration(TextDecoration.BOLD,true));

        enchm.lore(enchlore);

        ench.setAmount(Integer.parseInt(args[2]));
        ench.setItemMeta(enchm);
//------------------------------------------------------------------------
        ItemStack recover_scroll = new ItemStack(Material.FLOW_BANNER_PATTERN);
        ItemMeta recovermeta = recover_scroll.getItemMeta();

        recovermeta.displayName(Component.text("아이템 복구 스크롤").decoration(TextDecoration.ITALIC,false));

        List<Component> recoverlore = new ArrayList<>();
        recoverlore.add(Component.text(""));
        recoverlore.add(Component.text("강화에서 가장 마지막으로 파괴된 아이템 한개를 복구합니다.").decoration(TextDecoration.ITALIC,false).color(NamedTextColor.LIGHT_PURPLE));
        recoverlore.add(Component.text("강화 아이템").decoration(TextDecoration.ITALIC,false).color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD,true));

        recovermeta.lore(recoverlore);

        recover_scroll.setAmount(Integer.parseInt(args[3]));
        recover_scroll.setItemMeta(recovermeta);
//------------------------------------------------------------------------
        p.getInventory().addItem(tone);
        p.getInventory().addItem(frag);
        p.getInventory().addItem(ench);
        p.getInventory().addItem(recover_scroll);
//------------------------------------------------------------------------
        return true;
    }
}

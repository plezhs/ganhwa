package io.github.plezhs.ganhwa;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;

import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class shop implements CommandExecutor {
    private final JavaPlugin plugin;
    private final List<ChatColor> teamColors = Arrays.asList(
            ChatColor.AQUA, ChatColor.BLUE, ChatColor.GOLD, ChatColor.GREEN,
            ChatColor.RED, ChatColor.YELLOW, ChatColor.LIGHT_PURPLE, ChatColor.DARK_PURPLE,
            ChatColor.DARK_GREEN, ChatColor.DARK_AQUA, ChatColor.DARK_RED, ChatColor.DARK_BLUE
    );

    public shop(JavaPlugin plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player player = (Player) sender;

        Inventory gui = Bukkit.createInventory(null, 9, Component.text("상점"));

        ItemStack advancedStar = new ItemStack(Material.DRAGON_BREATH);
        ItemMeta advancedMeta = advancedStar.getItemMeta();
        advancedMeta.displayName(Component.text("강화석", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        List<Component> adlore = new ArrayList<>();
        adlore.add(Component.text(""));
        adlore.add(Component.text("좌클릭 : 01개 구매" + ChatColor.GREEN + "  [ 가격 : 01레벨 ]", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        adlore.add(Component.text("우클릭 : 16개 구매" + ChatColor.GREEN + "  [ 가격 : 15레벨 ]", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        advancedMeta.lore(adlore);
        advancedStar.setItemMeta(advancedMeta);

        ItemStack normalStar = new ItemStack(Material.ECHO_SHARD);
        ItemMeta normalStarItemMetaMeta = normalStar.getItemMeta();
        normalStarItemMetaMeta.displayName(Component.text("강화석 조각", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        List<Component> nolore = new ArrayList<>();
        nolore.add(Component.text(""));
        nolore.add(Component.text("좌클릭 : 01개 구매" + ChatColor.GREEN + "  [ 가격 : 01레벨 ]", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        nolore.add(Component.text("우클릭 : 16개 구매" + ChatColor.GREEN + "  [ 가격 : 15레벨 ]", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        normalStarItemMetaMeta.lore(nolore);
        normalStar.setItemMeta(normalStarItemMetaMeta);

        //------------------------------------------------------------------------
        ItemStack ench = new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta enchm = ench.getItemMeta();

        enchm.displayName(Component.text("마법 수정 가루").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC,false).decoration(TextDecoration.BOLD,true));
        List<Component> enchlore = new ArrayList<>();
        enchlore.add(Component.text(""));
        enchlore.add(Component.text("좌클릭 : 01개 구매" + ChatColor.GREEN + "  [ 가격 : " + ChatColor.BLUE + "청금석" + ChatColor.GREEN + " 03개 ]").decoration(TextDecoration.ITALIC,false).color(NamedTextColor.GOLD));
        enchlore.add(Component.text("우클릭 : 16개 구매" + ChatColor.GREEN + "  [ 가격 : " + ChatColor.BLUE + "청금석" + ChatColor.GREEN + " 32개 ]").decoration(TextDecoration.ITALIC,false).color(NamedTextColor.GOLD));

        enchm.lore(enchlore);

        ench.addUnsafeEnchantment(Enchantment.POWER,32767);
        enchm.removeItemFlags(ItemFlag.HIDE_ENCHANTS);

        ench.setAmount(1);
        ench.setItemMeta(enchm);

        gui.setItem(2,advancedStar);
//        gui.setItem(4,ench);
        gui.setItem(6,ench);

        player.openInventory(gui);

        Dialog store = Dialog.create(b -> b.empty().base(DialogBase.builder(Component.text("Store")).build()).type());

        return true;
    }
}

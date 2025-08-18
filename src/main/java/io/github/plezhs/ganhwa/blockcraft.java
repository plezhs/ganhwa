package io.github.plezhs.ganhwa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class blockcraft implements Listener {

    private final JavaPlugin plugin;

    public blockcraft(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event){
        CraftingInventory inv = event.getInventory();

        for (ItemStack item : inv.getMatrix()){
            if (item != null && item.hasItemMeta()){
                ItemMeta meta = item.getItemMeta();
                if(meta.hasLore()){
                    List<String> lore = meta.getLore();
                    if (lore.contains(Component.text("인챈팅 아이템").color(NamedTextColor.GREEN))){
                        inv.setResult(new ItemStack(Material.AIR));
                    }
                }
            }
        }
    }

}

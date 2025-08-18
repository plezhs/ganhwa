package io.github.plezhs.ganhwa;

import com.google.common.collect.Multimap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryBuilder;
import io.papermc.paper.registry.data.dialog.DialogBase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.units.qual.C;
import org.checkerframework.checker.units.qual.N;


import javax.inject.Named;
import javax.naming.Name;
import java.util.Random;

import static org.apache.commons.lang3.StringUtils.capitalize;

public class Ganhwa extends JavaPlugin implements Listener {
    private final NamespacedKey enhancementLevelKey = new NamespacedKey(this, "enhancement_level");
    private final NamespacedKey enchantattempts = new NamespacedKey(this, "enchantattempts");
    private final Map<UUID, Inventory> openInventories = new ConcurrentHashMap<>();
    private final Map<UUID, ItemStack> originalItems = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> originalSlot = new ConcurrentHashMap<>();
    private final Map<UUID,ItemStack> ingitem = new ConcurrentHashMap<>();
    private final Map<UUID,ItemStack> destroyedItem = new ConcurrentHashMap<>();

    private final List<Double> normalSuccessChance = new ArrayList<>();
    private final List<Double> normalDestructionChance = new ArrayList<>();
    private final List<Double> advancedSuccessChance = new ArrayList<>();
    private final List<Double> advancedDestructionChance = new ArrayList<>();
    private static final int MAX_ENHANCEMENT_LEVEL = 10;
    private static final Random random = new Random();

    private static final Map<Enchantment, Integer> enchantments = new HashMap<>();

    private ItemStack reinforcestone = null;
    private ItemStack enchantingdust = null;
    private ItemStack recoverscroll = null;

//    private Dialog dia = Dialog.create();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        blockcraft bc = new blockcraft(this);
        getServer().getPluginManager().registerEvents(bc,this);

        initializeChances();
        startArmorAndJumpCheckTask();
        getLogger().info("Ganhwa plugin enabled!");
        getCommand("material").setExecutor(new command(this));
        getCommand("cheat").setExecutor(new force(this,this));
        getCommand("shop").setExecutor(new shop(this));

        //------------------------------------------------------------------------
        ItemStack recover_scroll = new ItemStack(Material.FLOW_BANNER_PATTERN);
        ItemMeta recovermeta = recover_scroll.getItemMeta();

        recovermeta.displayName(Component.text("아이템 복구 스크롤").decoration(TextDecoration.ITALIC,false));

        List<Component> recoverlore = new ArrayList<>();
        recoverlore.add(Component.text(""));
        recoverlore.add(Component.text("강화에서 가장 마지막으로 파괴된 아이템 한개를 복구합니다.").decoration(TextDecoration.ITALIC,false).color(NamedTextColor.LIGHT_PURPLE));
        recoverlore.add(Component.text("강화 아이템").decoration(TextDecoration.ITALIC,false).color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD,true));

        recovermeta.lore(recoverlore);
        recover_scroll.setItemMeta(recovermeta);

        //------------------------------------------------------------------------
        ItemStack ench = new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta enchm = ench.getItemMeta();

        enchm.displayName(Component.text("마법 수정 가루").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC,false).decoration(TextDecoration.BOLD,true));
        List<Component> enchlore = new ArrayList<>();
        enchlore.add(Component.text(""));
        enchlore.add(Component.text("인챈팅 아이템").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC,false).decoration(TextDecoration.BOLD,true));

        enchm.lore(enchlore);
        ench.setItemMeta(enchm);
        //------------------------------------------------------------------------

        ShapedRecipe recover = new ShapedRecipe(recover_scroll).shape(" # ","###", " # ").setIngredient('#',Material.DIAMOND);
        getServer().addRecipe(recover);

        ItemStack resultblock = ench;
        resultblock.setAmount(4);
        ShapelessRecipe blockglowstone = new ShapelessRecipe(resultblock).addIngredient(4,ench);
        getServer().addRecipe(blockglowstone);


    }



    @EventHandler
    public void onDeathmsg(PlayerDeathEvent event){
        Player victim = event.getPlayer();
        Player killer = event.getPlayer().getKiller();

        if(killer != null){
            if(victim.getScoreboard().getPlayerTeam(victim) != null){
                String deathmsg =killer.getScoreboard().getPlayerTeam(killer).getColor() + killer.getName() + ChatColor.WHITE + " ⚔ " + victim.getScoreboard().getPlayerTeam(victim).getColor() + victim.getName();
                event.setDeathMessage(deathmsg);
            }else{
                String deathmsg =killer.getName() + " ⚔ " + victim.getName();
                event.setDeathMessage(deathmsg);
            }
        }else if(killer == null){
            if(victim.getScoreboard().getPlayerTeam(victim) != null){
                String deathmsg =" ⚔ " + victim.getScoreboard().getPlayerTeam(victim).getColor() + victim.getName();
                event.setDeathMessage(deathmsg);
            }else{
                String deathmsg =" ⚔ " + victim.getName();
                event.setDeathMessage(deathmsg);
            }
        }
    }

    private void initializeChances() {
        // 일반 강화 확률 (0강 -> 1강 시도)
        // 성공확률, 파괴확률. 나머지는 하락.

        normalSuccessChance.addAll(Arrays.asList(0.90, 0.85, 0.80, 0.75, 0.70, 0.60, 0.50, 0.40, 0.20, 0.05));
        normalDestructionChance.addAll(Arrays.asList(0.0, 0.0, 0.0, 0.05, 0.10, 0.15, 0.20, 0.25, 0.30, 0.50));

        // 고급 강화 확률 (0강 -> 1강 시도)
        advancedSuccessChance.addAll(Arrays.asList(0.95, 0.90, 0.85, 0.80, 0.75, 0.70 , 0.60, 0.45 , 0.30, 0.10));
        advancedDestructionChance.addAll(Arrays.asList(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.05, 0.10, 0.15));

        //------------------------------------------------------------------------
        ItemStack recover_scroll = new ItemStack(Material.FLOW_BANNER_PATTERN);
        ItemMeta recovermeta = recover_scroll.getItemMeta();

        recovermeta.displayName(Component.text("아이템 복구 스크롤").decoration(TextDecoration.ITALIC,false));

        List<Component> recoverlore = new ArrayList<>();
        recoverlore.add(Component.text(""));
        recoverlore.add(Component.text("강화에서 가장 마지막으로 파괴된 아이템 한개를 복구합니다.").decoration(TextDecoration.ITALIC,false).color(NamedTextColor.LIGHT_PURPLE));
        recoverlore.add(Component.text("강화 아이템").decoration(TextDecoration.ITALIC,false).color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD,true));

        recovermeta.lore(recoverlore);
        recover_scroll.setItemMeta(recovermeta);

        //------------------------------------------------------------------------
        ItemStack ench = new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta enchm = ench.getItemMeta();

        enchm.displayName(Component.text("마법 수정 가루").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC,false).decoration(TextDecoration.BOLD,true));
        List<Component> enchlore = new ArrayList<>();
        enchlore.add(Component.text(""));
        enchlore.add(Component.text("인챈팅 아이템").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC,false).decoration(TextDecoration.BOLD,true));

        enchm.lore(enchlore);
        ench.setItemMeta(enchm);
        //------------------------------------------------------------------------
        ItemStack tone = new ItemStack(Material.DRAGON_BREATH);
        ItemMeta tonem = tone.getItemMeta();

        tonem.displayName(Component.text("강화석").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC,false));

        List<Component> tonelore = new ArrayList<>();
        tonelore.add(Component.text(""));
        tonelore.add(Component.text("강화 아이템").decoration(TextDecoration.ITALIC,false).color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD,true));

        tonem.lore(tonelore);
        tone.setItemMeta(tonem);

        recoverscroll = recover_scroll;
        enchantingdust = ench;
        reinforcestone = tone;

        //인챈트 목록
        enchantments.put(Enchantment.MENDING,1);
        enchantments.put(Enchantment.UNBREAKING,3);
        enchantments.put(Enchantment.BINDING_CURSE,1);
        enchantments.put(Enchantment.VANISHING_CURSE,1);
        enchantments.put(Enchantment.AQUA_AFFINITY,1);
        enchantments.put(Enchantment.BLAST_PROTECTION,4);
        enchantments.put(Enchantment.DEPTH_STRIDER,3);
        enchantments.put(Enchantment.FEATHER_FALLING,4);
        enchantments.put(Enchantment.FIRE_PROTECTION,4);
        enchantments.put(Enchantment.FROST_WALKER,2);
        enchantments.put(Enchantment.PROJECTILE_PROTECTION,4);
        enchantments.put(Enchantment.PROTECTION,4);
        enchantments.put(Enchantment.RESPIRATION,3);
        enchantments.put(Enchantment.SOUL_SPEED,3);
        enchantments.put(Enchantment.THORNS,3);
        enchantments.put(Enchantment.SWIFT_SNEAK,3);
        enchantments.put(Enchantment.BANE_OF_ARTHROPODS,5);
        enchantments.put(Enchantment.BREACH,4);
        enchantments.put(Enchantment.DENSITY,5);
        enchantments.put(Enchantment.EFFICIENCY,5);
        enchantments.put(Enchantment.FIRE_ASPECT,2);
        enchantments.put(Enchantment.LOOTING,3);
        enchantments.put(Enchantment.IMPALING,5);
        enchantments.put(Enchantment.KNOCKBACK,2);
        enchantments.put(Enchantment.SHARPNESS,5);
        enchantments.put(Enchantment.SMITE,5);
        enchantments.put(Enchantment.SWEEPING_EDGE,3);
        enchantments.put(Enchantment.WIND_BURST,3);
        enchantments.put(Enchantment.CHANNELING,1);
        enchantments.put(Enchantment.FLAME,1);
        enchantments.put(Enchantment.INFINITY,1);
        enchantments.put(Enchantment.LOYALTY,3);
        enchantments.put(Enchantment.RIPTIDE,3);
        enchantments.put(Enchantment.MULTISHOT,3);
        enchantments.put(Enchantment.PIERCING,4);
        enchantments.put(Enchantment.POWER,5);
        enchantments.put(Enchantment.PUNCH,2);
        enchantments.put(Enchantment.QUICK_CHARGE,6);
        enchantments.put(Enchantment.FORTUNE,3);
        enchantments.put(Enchantment.LUCK_OF_THE_SEA,3);
        enchantments.put(Enchantment.LURE,3);
        enchantments.put(Enchantment.SILK_TOUCH,1);
    }

    private boolean isReinforceable(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        Material type = item.getType();
        String typeName = type.name();
        return typeName.endsWith("_SWORD") ||
               typeName.endsWith("_HELMET") || typeName.endsWith("_CHESTPLATE") ||
               typeName.endsWith("_LEGGINGS") || typeName.endsWith("_BOOTS") || typeName.endsWith("BOW");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getHand() != EquipmentSlot.HAND || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.ANVIL) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            ItemStack itemInHand = player.getInventory().getItemInMainHand();

            ItemMeta meta = itemInHand.getItemMeta();
            int currentLevel = meta.getPersistentDataContainer().getOrDefault(enhancementLevelKey, PersistentDataType.INTEGER, 0);

            if (!isReinforceable(itemInHand)) {
                player.sendActionBar(Component.text("이 아이템은 강화가 불가능합니다.", NamedTextColor.RED));
                return;
            }
            openEnhancementUI(player, itemInHand,currentLevel);
        } else if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.ENCHANTING_TABLE && event.getPlayer().getInventory().getItemInMainHand() != null) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            ItemStack itemInHand = player.getInventory().getItemInHand();

            ItemMeta meta = itemInHand.getItemMeta();
            int currentattempts = meta.getPersistentDataContainer().getOrDefault(enchantattempts,PersistentDataType.INTEGER,0);
            openEnchantmentUI(player,itemInHand,currentattempts);

        } else if (event.getAction() == Action.RIGHT_CLICK_AIR && event.getPlayer().getInventory().getItemInMainHand().equals(recoverscroll)) {
            Player p = event.getPlayer();

            if (destroyedItem.get(p.getUniqueId()) != null){
                if (removeitem(p,recoverscroll,1)) {
                    p.getInventory().addItem(destroyedItem.get(p.getUniqueId()));
                    destroyedItem.remove(p.getUniqueId());
                }
            }else {
                p.sendMessage(Component.text("현재 복구할 강화 중 파괴된 아이템이 존재하지 않습니다.").color(NamedTextColor.RED));
            }

        }
    }

    private ItemStack netherstar(int lvl, ItemStack item){
//        if(lvl<10) {
//            if (isAd == 0) {
//                ItemStack advancedStar = new ItemStack(Material.NETHER_STAR);
//                ItemMeta advancedMeta = advancedStar.getItemMeta();
//                advancedMeta.displayName(Component.text("고급 강화", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
//                List<Component> adlore = new ArrayList<>();
//                int adfail = (int) (100 - advancedSuccessChance.get(lvl) * 100 - advancedDestructionChance.get(lvl) * 100);
//                adlore.add(Component.text(""));
//                adlore.add(Component.text(lvl + 1 + "강 시도", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
//                adlore.add(Component.text("성공 확률: " + advancedSuccessChance.get(lvl) * 100 + "%", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
//                adlore.add(Component.text("실패 확률: " + adfail + "%", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
//                adlore.add(Component.text("파괴 확률: " + advancedDestructionChance.get(lvl) * 100 + "%", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
//                advancedMeta.lore(adlore);
//                advancedStar.setItemMeta(advancedMeta);
//                return advancedStar;
//            } else if (isAd == 1) {
//                ItemStack normalStar = new ItemStack(Material.NETHER_STAR);
//                ItemMeta normalMeta = normalStar.getItemMeta();
//                normalMeta.displayName(Component.text("일반 강화", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
//                List<Component> nlore = new ArrayList<>();
//                int nfail = (int) (100 - (normalSuccessChance.get(lvl) * 100 + normalDestructionChance.get(lvl) * 100));
//                nlore.add(Component.text(""));
//                nlore.add(Component.text(lvl + 1 + "강 시도", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
//                nlore.add(Component.text("성공 확률: " + normalSuccessChance.get(lvl) * 100 + "%", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
//                nlore.add(Component.text("실패 확률: " + nfail + "%", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
//                nlore.add(Component.text("파괴 확률: " + normalDestructionChance.get(lvl) * 100 + "%", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
//                normalMeta.lore(nlore);
//                normalStar.setItemMeta(normalMeta);
//                return normalStar;
//            } else if (isAd == 3) {
//                ItemStack normalStar = new ItemStack(Material.NETHER_STAR);
//                ItemMeta normalMeta = normalStar.getItemMeta();
//                normalMeta.displayName(Component.text("펑퍼어퍼퍼어펑", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
//                List<Component> nlore = new ArrayList<>();
//                nlore.add(Component.text(""));
//                nlore.add(Component.text("펑퍼어퍼퍼어펑", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
//                normalMeta.lore(nlore);
//                normalStar.setItemMeta(normalMeta);
//                return normalStar;
//            } else {
//                ItemStack star = new ItemStack(Material.NETHER_STAR);
//                return star;
//            }
//        }else{
//            ItemStack normalStar = new ItemStack(Material.NETHER_STAR);
//            ItemMeta normalMeta = normalStar.getItemMeta();
//            normalMeta.displayName(Component.text("축하합니다.", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
//            List<Component> nlore = new ArrayList<>();
//            nlore.add(Component.text(""));
//            nlore.add(Component.text("10강에 도달하셨습니다.", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
//            normalMeta.lore(nlore);
//            normalStar.setItemMeta(normalMeta);
//            return normalStar;
//        }
        if (lvl<10){
            ItemStack normalStar = item;
            ItemMeta normalMeta = normalStar.getItemMeta();
//            normalMeta.displayName(Component.text("일반 강화", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            List<Component> nlore = new ArrayList<>();
            int nfail = (int) (100 - (advancedSuccessChance.get(lvl) * 100 + advancedDestructionChance.get(lvl) * 100));
            nlore.add(Component.text(""));
            nlore.add(Component.text(lvl + 1 + "강 시도", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            nlore.add(Component.text("성공 확률: " + advancedSuccessChance.get(lvl) * 100 + "%", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            nlore.add(Component.text("실패 확률: " + nfail + "%", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            nlore.add(Component.text("파괴 확률: " + advancedDestructionChance.get(lvl) * 100 + "%", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
            normalMeta.lore(nlore);
            normalStar.setItemMeta(normalMeta);
            return normalStar;
        }else {
            ItemStack normalStar = item;
            ItemMeta normalMeta = normalStar.getItemMeta();
//            normalMeta.displayName(Component.text("축하합니다.", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            List<Component> nlore = new ArrayList<>();
            nlore.add(Component.text(""));
            nlore.add(Component.text("축하합니다.").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC,false));
            nlore.add(Component.text("10강에 도달하셨습니다.", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            normalMeta.lore(nlore);
            normalStar.setItemMeta(normalMeta);
            return normalStar;
        }
    }

    private void openEnchantmentUI(Player player, ItemStack item, int attempts){
        Inventory gui = Bukkit.createInventory(null, 54, Component.text("인챈트"));
        gui.setItem(22,item.clone());

        player.openInventory(gui);
        openInventories.put(player.getUniqueId(),gui);
        originalItems.put(player.getUniqueId(),item);
        ingitem.put(player.getUniqueId(),item.clone());
        originalSlot.put(player.getUniqueId(),player.getInventory().getHeldItemSlot());
    }

    private void openEnhancementUI(Player player, ItemStack item,int currentlvl) {
        Inventory gui = Bukkit.createInventory(null, 54, Component.text("강화"));
        gui.setItem(22,netherstar(currentlvl,item.clone()));
//        강화 하나로 합체
//        gui.setItem(22, item.clone());
//        gui.setItem(21, netherstar(currentlvl,0));
//        gui.setItem(23, netherstar(currentlvl,1));

        player.openInventory(gui);
        openInventories.put(player.getUniqueId(), gui);
        originalItems.put(player.getUniqueId(), item);
        ingitem.put(player.getUniqueId(),item.clone());
        originalSlot.put(player.getUniqueId(), player.getInventory().getHeldItemSlot());
    }

    public void purchase(Player player, boolean isen, int count){
        //------------------------------------------------------------------------
        ItemStack tone = new ItemStack(Material.DRAGON_BREATH);
        ItemMeta tonem = tone.getItemMeta();

        tonem.displayName(Component.text("강화석").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC,false));

        List<Component> tonelore = new ArrayList<>();
        tonelore.add(Component.text(""));
        tonelore.add(Component.text("강화 아이템").decoration(TextDecoration.ITALIC,false).color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD,true));

        tonem.lore(tonelore);

        tone.setAmount(count);
        tone.setItemMeta(tonem);
        //------------------------------------------------------------------------
        ItemStack frag = new ItemStack(Material.ECHO_SHARD);
        ItemMeta fragm = frag.getItemMeta();

        fragm.displayName(Component.text("강화석 조각").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC,false));

        frag.setAmount(count);
        frag.setItemMeta(fragm);
        //------------------------------------------------------------------------
        ItemStack ench = new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta enchm = ench.getItemMeta();

        enchm.displayName(Component.text("마법 수정 가루").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC,false).decoration(TextDecoration.BOLD,true));
        List<Component> enchlore = new ArrayList<>();
        enchlore.add(Component.text(""));
        enchlore.add(Component.text("인챈팅 아이템").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC,false).decoration(TextDecoration.BOLD,true));

        enchm.lore(enchlore);

        ench.setAmount(count);
        ench.setItemMeta(enchm);
        //------------------------------------------------------------------------

        if (!isen){
            player.getInventory().addItem(tone);
        } else if (isen) {
            player.getInventory().addItem(ench);
        }

//        if(isAd){
//            ItemStack tone = new ItemStack(Material.DRAGON_BREATH);
//            ItemMeta tonem = tone.getItemMeta();
//
////            tonem.setDisplayName("강화석");
//            tonem.displayName(Component.text("강화석").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC,false));
//            tone.setAmount(count);
//            tone.setItemMeta(tonem);
//
//            player.getInventory().addItem(tone);
//        }else{
//            ItemStack frag = new ItemStack(Material.ECHO_SHARD);
//            ItemMeta fragm = frag.getItemMeta();
//
//            fragm.displayName(Component.text("강화석 조각").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC,false));
//            frag.setAmount(count);
//            frag.setItemMeta(fragm);
//
//            player.getInventory().addItem(frag);
//        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        UUID playerUuid = player.getUniqueId();

//        player.sendMessage(event.getView().getTitle() + " 강화");
//        player.sendMessage(event.getView().getTitle() + " 인챈트");
//        player.sendMessage(event.getView().getTitle() + " 상점");
//
//        getServer().getLogger().info(event.getView().getTitle());

//        if (openInventories.containsKey(playerUuid) && event.getInventory().equals(openInventories.get(playerUuid)) && event.getView().getTitle() == "강화") {
        if (event.getView().getTitle().contains("강화")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 22) { // 고급 강화
                processEnhancement(player, true, false);
            }
        } else if(event.getView().getTitle().contains("상점")){
            event.setCancelled(true);
            if(event.isLeftClick()){
                if(event.getRawSlot() == 2){
                    if(player.getLevel()>=1){
                        purchase(player,false,1);
                        player.setLevel(player.getLevel()-1);
                        player.playSound(player.getLocation(),Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1,1.7f);
                    }else{
                        player.sendMessage(ChatColor.RED + "경험치(이)가 부족합니다.");
                    }
                } else if (event.getRawSlot() == 6) {
                    if(removeitem(player,new ItemStack(Material.LAPIS_LAZULI),5)){
                        purchase(player,true,1);
                        player.playSound(player.getLocation(),Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1,1.7f);
                    }else{
                        player.sendMessage(ChatColor.RED + "청금석(이)가 부족합니다.");
                    }
                }
            } else if (event.isRightClick()) {
                if(event.getRawSlot() == 2){
                    if(player.getLevel()>=15){
                        purchase(player,false,16);
                        player.setLevel(player.getLevel()-15);
                        player.playSound(player.getLocation(),Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1,1.7f);
                    }else{
                        player.sendMessage(ChatColor.RED + "경험치(이)가 부족합니다.");
                    }
                } else if (event.getRawSlot() == 6) {
                    if(removeitem(player,new ItemStack(Material.LAPIS_LAZULI),64)){
                        purchase(player,true,16);
                        player.playSound(player.getLocation(),Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1,1.7f);
                    }else{
                        player.sendMessage(ChatColor.RED + "청금석(이)가 부족합니다.");
                    }
                }
            }
        } else if (event.getView().getTitle().contains("인챈트")) {
            event.setCancelled(true);



            int slot = event.getRawSlot();
            if(slot==22){
                processEnchantment(player);
            }
        } else if (event.getClickedInventory().getType().equals(InventoryType.GRINDSTONE) && event.getSlotType().equals(InventoryType.SlotType.RESULT)) {

            ItemStack item = event.getInventory().getItem(event.getRawSlot());
            ItemMeta meta = item.getItemMeta();

            meta.getPersistentDataContainer().set(enchantattempts,PersistentDataType.INTEGER,0);
            item.setItemMeta(meta);
        }
    }

    public void processEnchantment(Player player){

        //------------------------------------------------------------------------
        ItemStack tone = new ItemStack(Material.DRAGON_BREATH);
        ItemMeta tonem = tone.getItemMeta();

        tonem.displayName(Component.text("강화석").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC,false));

        tone.setAmount(1);
        tone.setItemMeta(tonem);
        //------------------------------------------------------------------------
        ItemStack frag = new ItemStack(Material.ECHO_SHARD);
        ItemMeta fragm = frag.getItemMeta();

        fragm.displayName(Component.text("강화석 조각").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC,false));

        frag.setAmount(1);
        frag.setItemMeta(fragm);
        //------------------------------------------------------------------------
        ItemStack ench = new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta enchm = ench.getItemMeta();

        enchm.displayName(Component.text("마법 수정 가루").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC,false).decoration(TextDecoration.BOLD,true));
        List<Component> enchlore = new ArrayList<>();
        enchlore.add(Component.text(""));
        enchlore.add(Component.text("인챈팅 아이템").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC,false).decoration(TextDecoration.BOLD,true));

        enchm.lore(enchlore);

        ench.addUnsafeEnchantment(Enchantment.POWER,32767);
        enchm.removeItemFlags(ItemFlag.HIDE_ENCHANTS);

        ench.setAmount(1);
        ench.setItemMeta(enchm);
        //------------------------------------------------------------------------

        Inventory gui = openInventories.get(player.getUniqueId());
        if (gui==null) return;

        ItemStack targetItem;
        targetItem = ingitem.get(player.getUniqueId());

        if (targetItem == null || targetItem.getType() == Material.AIR){
            player.sendMessage(Component.text("인챈트할 아이템이 없습니다.", NamedTextColor.RED));
            return;
        } else if (targetItem.getItemMeta().getDisplayName().contains("마법 수정 가루") || targetItem.getItemMeta().getDisplayName().contains("강화석")) {
            player.sendMessage(Component.text("이 아이템은 인챈트 할 수 없습니다.").color(NamedTextColor.RED).decoration(TextDecoration.BOLD,true));
            return;
        }

        ItemMeta meta = targetItem.getItemMeta();
        int attempts = meta.getPersistentDataContainer().getOrDefault(enchantattempts,PersistentDataType.INTEGER,0);

        if (!removeenchitemfrominv(player, Material.GLOWSTONE_DUST, ench, 1)) {
            player.sendMessage(Component.text(ench.getItemMeta().getDisplayName() + ChatColor.RED + "이(가) 부족합니다.", NamedTextColor.RED));
            return;
        }

        ItemStack newitem = applyEnchantment(targetItem,attempts+1,player,true);
        ingitem.remove(player.getUniqueId());
        ingitem.put(player.getUniqueId(),newitem);
        gui.setItem(22,newitem);

    }

    public void processEnhancement(Player player, boolean isAdvanced, boolean cheat) {
        Inventory gui = openInventories.get(player.getUniqueId());
        if (gui == null) return;

        ItemStack targetItem;
        targetItem = ingitem.get(player.getUniqueId()).clone();
        if (targetItem == null || targetItem.getType() == Material.AIR) {
            player.sendMessage(Component.text("강화할 아이템이 없습니다.", NamedTextColor.RED));
            return;
        }

        ItemMeta meta = targetItem.getItemMeta();
        int currentLevel = meta.getPersistentDataContainer().getOrDefault(enhancementLevelKey, PersistentDataType.INTEGER, 0);

        if (currentLevel >= MAX_ENHANCEMENT_LEVEL) {
            player.sendActionBar(Component.text("최대 강화 레벨에 도달했습니다.", NamedTextColor.GOLD));
            return;
        }

        ItemStack tone = new ItemStack(Material.DRAGON_BREATH);
        ItemMeta tonem = tone.getItemMeta();
        tonem.displayName(Component.text("강화석").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC,false));
        tone.setItemMeta(tonem);

        ItemStack frag = new ItemStack(Material.ECHO_SHARD);
        ItemMeta fragm = frag.getItemMeta();
        fragm.displayName(Component.text("강화석 조각").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC,false));
        frag.setItemMeta(fragm);

        Material requiredMaterial = isAdvanced ? Material.DRAGON_BREATH : Material.ECHO_SHARD;
        ItemStack requiredItemName = isAdvanced ? reinforcestone : frag;

        if (!removeItemFromInventory(player, requiredMaterial, requiredItemName,currentLevel)) {
            player.sendMessage(Component.text(requiredItemName.getItemMeta().getDisplayName() + ChatColor.RED + "이(가) 부족합니다.", NamedTextColor.RED));
            return;
        }

        List<Double> successChanceList = isAdvanced ? advancedSuccessChance : normalSuccessChance;
        List<Double> destructionChanceList = isAdvanced ? advancedDestructionChance : normalDestructionChance;

        double successChance = successChanceList.get(currentLevel);
        double destructionChance = destructionChanceList.get(currentLevel);
        double random = Math.random();
        if (random < successChance) {
            // 성공
            int newLevel = currentLevel + 1;
            player.sendActionBar(Component.text("강화에 성공했습니다! (+" + newLevel + ")", NamedTextColor.GREEN));
            player.playSound(player.getLocation(),Sound.BLOCK_AMETHYST_BLOCK_CHIME,100.0f,1.2f);
            ItemStack newStack = applyEnhancement(targetItem.clone(), newLevel, player);
            ingitem.remove(player.getUniqueId());
            ingitem.put(player.getUniqueId(),newStack);
            gui.setItem(22, netherstar(newLevel,newStack.clone()));
//            gui.setItem(21,netherstar(newLevel,0));
//            gui.setItem(23,netherstar(newLevel,1));
        } else if (random < successChance + destructionChance) {
            // 파괴
            destroyedItem.remove(player.getUniqueId());
            destroyedItem.put(player.getUniqueId(),ingitem.get(player.getUniqueId()).clone());
            ingitem.remove(player.getUniqueId());
            gui.setItem(22, null);
            player.sendActionBar(Component.text("아이템이 파괴되었습니다...", NamedTextColor.DARK_RED));
            player.playSound(player.getLocation(),Sound.ITEM_TOTEM_USE,0.5f,0);
//            gui.setItem(21,netherstar(0,3));
//            gui.setItem(23,netherstar(0,3));
        } else {
            // 실패 (하락)
            int newLevel = Math.max(0, currentLevel - 1);
            if (currentLevel > 0) {
                player.sendActionBar(Component.text("강화에 실패하여 등급이 하락했습니다. (+" + newLevel + ")", NamedTextColor.YELLOW));
                player.playSound(player.getLocation(),Sound.BLOCK_CHAIN_BREAK,1,0);
                ItemStack newStack = applyEnhancement(targetItem.clone(), newLevel,player);
                ingitem.remove(player.getUniqueId());
                ingitem.put(player.getUniqueId(),newStack);
                gui.setItem(22, netherstar(newLevel,newStack.clone()));
//                gui.setItem(21,netherstar(newLevel,0));
//                gui.setItem(23,netherstar(newLevel,1));
            } else {
                player.sendActionBar(Component.text("강화에 실패했습니다.", NamedTextColor.YELLOW));
                player.playSound(player.getLocation(),Sound.BLOCK_CHAIN_BREAK,1,0);
//                gui.setItem(21,netherstar(newLevel,0));
//                gui.setItem(23,netherstar(newLevel,1));
            }
        }
        player.updateInventory();
    }

    public void cheat(Player p, int lvl){
        ItemStack targetItem;
        targetItem = p.getItemInHand();
        if (!isReinforceable(targetItem)) {
            p.sendActionBar(Component.text("이 아이템은 강화가 불가능합니다.", NamedTextColor.RED));
        }else{
            ItemStack newStack = applyEnhancement(targetItem.clone(), lvl,p);
            targetItem.setItemMeta(newStack.getItemMeta());
        }
    }

    private ItemStack applyEnchantment(ItemStack item, int attempts, Player player,boolean allowUnsafeLevel){
        if (item == null || item.getType() == Material.AIR) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(enchantattempts,PersistentDataType.INTEGER,attempts);

        item.setItemMeta(meta);

        Map<Enchantment, Integer> cuenchantments = item.getEnchantments();

        // 맵의 모든 키(Enchantment)를 순회하며 인챈트를 제거합니다.
        for (Enchantment ench : cuenchantments.keySet()) {
            item.removeEnchantment(ench);
        }

        int count;

        if (Math.round(attempts/5) + 1 <=MAX_ENHANCEMENT_LEVEL){
            count = Math.round(attempts/5) + 1;
        }else{
            count = MAX_ENHANCEMENT_LEVEL;
        }

        List<Enchantment> availableEnchants = new ArrayList<>(enchantments.keySet());

        for (int i = 0; i < count; i++) {
            if (availableEnchants.isEmpty()) break;

            // 1. 무작위로 인챈트 선택
            Enchantment chosenEnchant = availableEnchants.get(random.nextInt(availableEnchants.size()));

            // 중복 인챈트를 피하고 싶다면 여기서 제거
            availableEnchants.remove(chosenEnchant);

            // 2. 무작위로 레벨 결정
            int maxLevel = enchantments.get(chosenEnchant);
            // 최소 레벨은 1, 최대 레벨은 정의된 maxLevel
            int level = random.nextInt(maxLevel) + 1;

            // 3. 아이템에 인챈트 부여
            if (allowUnsafeLevel) {
                // 안전하지 않은 인챈트 (Bukkit에서 허용하지 않는 조합도 가능)
                item.addUnsafeEnchantment(chosenEnchant, level);
            } else {
                // 안전한 인챈트 (Bukkit 규칙에 따름)
                // 이미 해당 인챈트가 더 높은 레벨로 부여되어 있으면 스킵
                if (item.getEnchantmentLevel(chosenEnchant) >= level) {
                    continue;
                }
                // 아이템에 부여 가능한 인챈트인지 확인
                if (chosenEnchant.canEnchantItem(item)) {
                    item.addEnchantment(chosenEnchant, level);
                }
            }
        }
        return item;
    }

    private ItemStack applyEnhancement(ItemStack item, int level, Player player) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(enhancementLevelKey, PersistentDataType.INTEGER, level);

        // 기존 강화 속성만 제거
        Multimap<Attribute, AttributeModifier> currentModifiers = meta.getAttributeModifiers();
        if (currentModifiers != null && !currentModifiers.isEmpty()) {
            List<Map.Entry<Attribute, AttributeModifier>> toRemove = new ArrayList<>();
            for (Map.Entry<Attribute, AttributeModifier> entry : currentModifiers.entries()) {
                if (entry.getValue().getName().startsWith("enhancement_")) {
                    toRemove.add(entry);
                }
            }
            for(Map.Entry<Attribute, AttributeModifier> entry : toRemove) {
                meta.removeAttributeModifier(entry.getKey(), entry.getValue());
            }
        }

        // 레벨 표시 로어 설정
//        List<Component> lore = new ArrayList<>();
//        lore.add(Component.text(""));
//        lore.add(Component.text("강화 레벨: +" + level, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
//        meta.lore(lore);

        String basicstar = "☆";
        String star = "★";
        String morestar = "✪";
        String itname = player.getName()+ "의 "+ capitalize(item.getType().name().replace("_"," ").toLowerCase()) + " ";

        if (level<10){
            String rename = itname.replace(star,"").replace(morestar,"").replace(basicstar,"") + star.repeat(level) + basicstar.repeat(10-level);
            meta.displayName(Component.text(rename).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC,false).decoration(TextDecoration.BOLD,true));
        } else if (level==10) {
            String rename = itname.replace(star,"").replace(morestar,"").replace(basicstar,"") + star.repeat(level) + basicstar.repeat(10-level);
            meta.displayName(Component.text(rename).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC,false).decoration(TextDecoration.BOLD,true));
        } else if (11>= level && level <= 20){
            String rename = itname.replace(star,"").replace(morestar,"").replace(basicstar,"") + morestar.repeat(level-10) + star.repeat(20-level);
            meta.displayName(Component.text(rename).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC,false).decoration(TextDecoration.BOLD,true));
        }else if (level>20){
            String rename = itname.replace(star,"").replace(morestar,"").replace(basicstar,"") + morestar.repeat(level);
            meta.displayName(Component.text(rename).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC,false).decoration(TextDecoration.BOLD,true));
        }

        // 아이템 종류에 따른 속성 부여
        Material type = item.getType();
        String typeName = type.name();

        if (level > 0) { // 0강일때는 속성을 부여 x
            if (typeName.endsWith("_SWORD")) {
                if (typeName.startsWith("WOODEN")){
                    meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(new NamespacedKey(this,"enhancement_attack"), 3 + 1.75*level, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HAND));
                }else if (typeName.startsWith("STONE")){
                    meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(new NamespacedKey(this,"enhancement_attack"), 4 + 1.75*level, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HAND));
                }else if (typeName.startsWith("IRON")){
                    meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(new NamespacedKey(this,"enhancement_attack"), 5 + 1.75*level, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HAND));
                }else if (typeName.startsWith("GOLDEN")){
                    meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(new NamespacedKey(this,"enhancement_attack"), 3 + 1.75*level, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HAND));
                }else if (typeName.startsWith("DIAMOND")){
                    meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(new NamespacedKey(this,"enhancement_attack"), 6 + 1.75*level, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HAND));
                }else if (typeName.startsWith("NETHERITE")){
                    meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(new NamespacedKey(this,"enhancement_attack"), 7 + 1.75*level, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HAND));
                }

                if (level >= 10 && typeName.endsWith("_SWORD")) {
                    meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(new NamespacedKey(this,"enhancement_attackspeed"), 999, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HAND));
                } else if(level<10){
                    meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(new NamespacedKey(this,"enhancement_attackspeed"), -2.4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HAND));
                }
            } else if (typeName.endsWith("_HELMET")) {
                if (typeName.startsWith("LEATHER")){
                    meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_helmet"), 1 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD));
                } else if (typeName.startsWith("CHAIN") || typeName.startsWith("IRON") || typeName.startsWith("GOLDEN")){
                    meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_helmet"), 2 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD));
                } else if (typeName.startsWith("DIAMOND")){
                    meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_helmet"), 3 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD));
                    meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_helmet"), 2 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD));
                }else if (typeName.startsWith("NETHERITE")){
                    meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_helmet"), 3 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD));
                    meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_helmet"), 3 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD));
                    meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_helmet"),1 + 0.02*level,AttributeModifier.Operation.ADD_NUMBER,EquipmentSlotGroup.HEAD));
                }
            } else if (typeName.endsWith("_CHESTPLATE")) {
                if (typeName.startsWith("LEATHER")){
                    meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_chest"), 3 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST));
                    meta.addAttributeModifier(Attribute.MAX_HEALTH, new AttributeModifier(new NamespacedKey(this,"enhancement_health_chest"), level * 4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST));
                } else if (typeName.startsWith("CHAIN") || typeName.startsWith("GOLDEN")){
                    meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_chest"), 5 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST));
                    meta.addAttributeModifier(Attribute.MAX_HEALTH, new AttributeModifier(new NamespacedKey(this,"enhancement_health_chest"), level * 4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST));
                } else if (typeName.startsWith("IRON")) {
                    meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_chest"), 6 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST));
                    meta.addAttributeModifier(Attribute.MAX_HEALTH, new AttributeModifier(new NamespacedKey(this,"enhancement_health_chest"), level * 4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST));
                } else if (typeName.startsWith("DIAMOND")){
                    meta.addAttributeModifier(Attribute.MAX_HEALTH, new AttributeModifier(new NamespacedKey(this,"enhancement_health_chest"), level * 4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST));
                    meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_chest"), 8 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST));
                    meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_chest"), 2 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST));
                }else if (typeName.startsWith("NETHERITE")){
                    meta.addAttributeModifier(Attribute.MAX_HEALTH, new AttributeModifier(new NamespacedKey(this,"enhancement_health_chest"), level * 4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST));
                    meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_chest"), 8 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST));
                    meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_chest"), 3 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST));
                    meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_chest"),1 + 0.02*level,AttributeModifier.Operation.ADD_NUMBER,EquipmentSlotGroup.CHEST));
                }

            } else if (typeName.endsWith("_LEGGINGS")) {

                if (typeName.startsWith("LEATHER")){
                    meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, new AttributeModifier(new NamespacedKey(this,"enhancement_speed_leg"), level * 0.02, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.LEGS));
                    meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_leg"), 2 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS));
                } else if (typeName.startsWith("CHAIN")){
                    meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, new AttributeModifier(new NamespacedKey(this,"enhancement_speed_leg"), level * 0.02, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.LEGS));
                    meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_leg"), 4 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS));
                } else if (typeName.startsWith("GOLDEN")) {
                    meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, new AttributeModifier(new NamespacedKey(this,"enhancement_speed_leg"), level * 0.02, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.LEGS));
                    meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_leg"), 3 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS));
                } else if (typeName.startsWith("IRON")) {
                    meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, new AttributeModifier(new NamespacedKey(this,"enhancement_speed_leg"), level * 0.02, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.LEGS));
                    meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_leg"), 5 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS));
                } else if (typeName.startsWith("DIAMOND")){
                    meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, new AttributeModifier(new NamespacedKey(this,"enhancement_speed_leg"), level * 0.02, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.LEGS));
                    meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_leg"), 6 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS));
                    meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_leg"), 2 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS));
                }else if (typeName.startsWith("NETHERITE")){
                    meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, new AttributeModifier(new NamespacedKey(this,"enhancement_speed_leg"), level * 0.02, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.LEGS));
                    meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_leg"), 6 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS));
                    meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_leg"), 3 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS));
                    meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_leg"),1 + 0.02*level,AttributeModifier.Operation.ADD_NUMBER,EquipmentSlotGroup.LEGS));
                }

            } else if (typeName.endsWith("_BOOTS")) {

                if (typeName.startsWith("LEATHER")){
                    meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_boots"), 1 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET));
                } else if (typeName.startsWith("CHAIN")){
                    meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_boots"), 1 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET));
                } else if (typeName.startsWith("GOLDEN")) {
                    meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_boots"), 1 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET));
                } else if (typeName.startsWith("IRON")) {
                    meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_boots"), 2 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET));
                } else if (typeName.startsWith("DIAMOND")){
                    meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_boots"), 3 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET));
                    meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_boots"), 2 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET));
                }else if (typeName.startsWith("NETHERITE")){
                    meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_boots"), 3 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET));
                    meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_boots"), 3 + level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET));
                    meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_boots"),1 + 0.02*level,AttributeModifier.Operation.ADD_NUMBER,EquipmentSlotGroup.FEET));
                }
            }
        }

        item.setItemMeta(meta);
        return item;
    }


    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        if (openInventories.containsKey(playerUuid) && event.getInventory().equals(openInventories.get(playerUuid))) {
            Inventory gui = openInventories.get(playerUuid);
//            ItemStack resultItem = gui.getItem(22);
            ItemStack resultItem = ingitem.get(playerUuid);
            ItemStack initialItem = originalItems.get(playerUuid);
            int itemSlot = originalSlot.get(playerUuid);
            
            ItemStack currentItemInSlot = player.getInventory().getItem(itemSlot);

            if (initialItem != null && initialItem.isSimilar(currentItemInSlot)) {
                 if (resultItem != null && resultItem.getType() != Material.AIR) {
                    player.getInventory().setItem(itemSlot, resultItem);
                } else {
                    player.getInventory().setItem(itemSlot, new ItemStack(Material.AIR));
                }
            } else {
                 if (resultItem != null && resultItem.getType() != Material.AIR) {
                    player.getInventory().addItem(resultItem);
                }
            }

            openInventories.remove(playerUuid);
            originalItems.remove(playerUuid);
            originalSlot.remove(playerUuid);
            ingitem.remove(playerUuid);
        }
    }

    private boolean removeitem(Player player, ItemStack material,int amount){

        if (amount <= 0) {
            return true; // 제거할게 없으므로 성공으로 간주
        }

        int currentAmount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material.getType()) {
                if (item.getType().equals(material.getType())) {
                    currentAmount += item.getAmount();
                }
            }
        }

        if (currentAmount < amount) {
//            player.sendMessage("여기서 문제임1");
//            player.sendMessage(String.valueOf(currentAmount));
//            player.sendMessage(String.valueOf(amount));
            return false;
        }

        ItemStack itemToRemove = material;

        // removeItem 메소드는 인벤토리의 해당 아이템을 amountToRemove만큼 찾아 제거합니다.
        // 이 메소드가 자동으로 여러 슬롯을 처리해줍니다.
        itemToRemove.setAmount(amount); // 이 amount는 removeItem이 제거할 '총량'을 의미합니다.
        player.getInventory().removeItemAnySlot(itemToRemove);

        return true;

    }

    private boolean removeenchitemfrominv(Player player,Material material,ItemStack requirement, int amountToRemove){
        PlayerInventory inventory = player.getInventory();

        // 제거할 수량이 0 이하면 처리할 필요 없음
        if (amountToRemove <= 0) {
            return true; // 제거할게 없으므로 성공으로 간주
        }

        // 인벤토리에서 해당 아이템의 총 수량 확인
        int currentAmount = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == material) {
                ItemMeta meta = item.getItemMeta();
                String name = requirement.getItemMeta().getDisplayName();
                if (meta.hasDisplayName() && meta.getDisplayName().equals(name)) {
                    currentAmount += item.getAmount();
                }
            }
        }

        // 제거하려는 수량보다 현재 가지고 있는 수량이 적으면 실패
        if (currentAmount < amountToRemove) {
//            player.sendMessage("여기서 문제임1");
//            player.sendMessage(String.valueOf(currentAmount));
//            player.sendMessage(String.valueOf(amountToRemove));
            return false;
        }

        // 제거할 아이템을 나타내는 임시 ItemStack 생성
        // 이 ItemStack의 amount는 실제로 제거될 양과 관련 없음
        // 중요한 것은 Material과 DisplayName이 일치해야 한다는 것
        ItemStack itemToRemove = requirement;

        // removeItem 메소드는 인벤토리의 해당 아이템을 amountToRemove만큼 찾아 제거합니다.
        // 이 메소드가 자동으로 여러 슬롯을 처리해줍니다.
        itemToRemove.setAmount(amountToRemove); // 이 amount는 removeItem이 제거할 '총량'을 의미합니다.
        player.getInventory().removeItemAnySlot(itemToRemove);

        return true;
    }
    
    private boolean removeItemFromInventory(Player player, Material material, ItemStack requirement,int level) {

        PlayerInventory inventory = player.getInventory();
        int amountToRemove;

        // 제거할 총 수량 계산
        if (level == 0) {
            amountToRemove = 1;
        } else if (level > 0 && level <= 5) {
            amountToRemove = Math.toIntExact(Math.round(level * 1.32));
        } else if (level > 5 && level <= 9) {
            amountToRemove = Math.toIntExact(Math.round(level * 1.62));
        } else {
            // 예상치 못한 level 값 처리 (예: 유효하지 않은 level)
            return false;
        }

        // 제거할 수량이 0 이하면 처리할 필요 없음
        if (amountToRemove <= 0) {
            return false; // 제거할게 없으므로 성공으로 간주
        }

        // 인벤토리에서 해당 아이템의 총 수량 확인
        int currentAmount = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == material) {
                ItemMeta meta = item.getItemMeta();
                String name = requirement.getItemMeta().getDisplayName();
                if (meta.hasDisplayName() && meta.getDisplayName().equals(name)) {
                    currentAmount += item.getAmount();
                }
            }
        }

        // 제거하려는 수량보다 현재 가지고 있는 수량이 적으면 실패
        if (currentAmount < amountToRemove) {
//            player.sendMessage("여기서 문제임1");
//            player.sendMessage(String.valueOf(currentAmount));
//            player.sendMessage(String.valueOf(amountToRemove));
            return false;
        }

        // 제거할 아이템을 나타내는 임시 ItemStack 생성
        // 이 ItemStack의 amount는 실제로 제거될 양과 관련 없음
        // 중요한 것은 Material과 DisplayName이 일치해야 한다는 것
        ItemStack itemToRemove = requirement;
//        ItemMeta meta = itemToRemove.getItemMeta();
//        if (meta != null) {
//            itemToRemove.setItemMeta(meta);
//        } else {
//            // ItemMeta를 가져올 수 없는 경우 (거의 발생하지 않음)
//            return false;
//        }

        // removeItem 메소드는 인벤토리의 해당 아이템을 amountToRemove만큼 찾아 제거합니다.
        // 이 메소드가 자동으로 여러 슬롯을 처리해줍니다.
        itemToRemove.setAmount(amountToRemove); // 이 amount는 removeItem이 제거할 '총량'을 의미합니다.
//        inventory.removeItem(itemToRemove); // 이 한 줄로 여러 덩어리 아이템을 제거할 수 있습니다.
        player.getInventory().removeItemAnySlot(itemToRemove);

        return true;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        // 특정 아이템인지 검사
        ItemStack item = attacker.getInventory().getItemInMainHand();
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        int level = container.getOrDefault(enhancementLevelKey, PersistentDataType.INTEGER, 0);
        if (item.getType().name().endsWith("_SWORD") && level >= 10) {
            // 한 번만 무적시간 제거
            victim.setNoDamageTicks(0);
            victim.setMaximumNoDamageTicks(0);

            // 1틱 후 다시 원래 무적시간으로 되돌림
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (victim.isValid()) {
                    victim.setMaximumNoDamageTicks(10);
                }
            }, 1L); // 1틱 후 실행
        }
    }

    private void startArmorAndJumpCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    ItemStack helmet = player.getInventory().getHelmet();
                    if (helmet != null && helmet.hasItemMeta()) {
                        PersistentDataContainer container = helmet.getItemMeta().getPersistentDataContainer();
                        int level = container.getOrDefault(enhancementLevelKey, PersistentDataType.INTEGER, 0);
                        if (level >= 10) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 80, 2, true, false));
                        }
                    }

                    ItemStack boots = player.getInventory().getBoots();
                    if (boots != null && boots.hasItemMeta()) {
                        PersistentDataContainer container = boots.getItemMeta().getPersistentDataContainer();
                        int level = container.getOrDefault(enhancementLevelKey, PersistentDataType.INTEGER, 0);
                        if (level >= 10 && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                            player.setAllowFlight(true);
                        } else if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
                            player.setAllowFlight(false);
                        }
                    } else if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
                         player.setAllowFlight(false);
                    }
                }
            }
        }.runTaskTimer(this, 0L, 30L * 1);
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        ItemStack boots = player.getInventory().getBoots();
        if (boots != null && boots.hasItemMeta()) {
            PersistentDataContainer container = boots.getItemMeta().getPersistentDataContainer();
            int level = container.getOrDefault(enhancementLevelKey, PersistentDataType.INTEGER, 0);
            if (level >= 10) {
                event.setCancelled(true);
                player.setAllowFlight(false);
                player.setFlying(false);
                player.setVelocity(player.getLocation().getDirection().multiply(0.7).setY(0.6));
            }
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
            player.setAllowFlight(false);
        }
    }
}
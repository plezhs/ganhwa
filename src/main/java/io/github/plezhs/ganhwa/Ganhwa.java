package io.github.plezhs.ganhwa;

import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
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
import org.w3c.dom.Attr;

public class Ganhwa extends JavaPlugin implements Listener {
    private final NamespacedKey enhancementLevelKey = new NamespacedKey(this, "enhancement_level");
    private final Map<UUID, Inventory> openInventories = new ConcurrentHashMap<>();
    private final Map<UUID, ItemStack> originalItems = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> originalSlot = new ConcurrentHashMap<>();

    private final List<Double> normalSuccessChance = new ArrayList<>();
    private final List<Double> normalDestructionChance = new ArrayList<>();
    private final List<Double> advancedSuccessChance = new ArrayList<>();
    private final List<Double> advancedDestructionChance = new ArrayList<>();
    private static final int MAX_ENHANCEMENT_LEVEL = 20;


    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        initializeChances();
        startArmorAndJumpCheckTask();
        getLogger().info("Ganhwa plugin enabled!");
        getCommand("material").setExecutor(new command(this));
        getCommand("cheat").setExecutor(new force(this,this));
    }

    @EventHandler
    public void onDeathmsg(PlayerDeathEvent e){
        Player p = e.getPlayer();

        if(e.getDamageSource().getDamageType() == DamageType.PLAYER_ATTACK || e.getDamageSource().getDamageType() == DamageType.PLAYER_EXPLOSION){
            String deathmsg = e.getDamageSource().getCausingEntity().getName() + " ⚔ " + p.getName();
            e.setDeathMessage(deathmsg);
        }else{
            String deathmsg = " ⚔ " + p.getName();
            e.setDeathMessage(deathmsg);
        }
    }

    private void initializeChances() {
        // 일반 강화 확률 (0강 -> 1강 시도 ~ 19강 -> 20강 시도)
        // 성공확률, 파괴확률. 나머지는 하락.
        normalSuccessChance.addAll(Arrays.asList(0.95, 0.90, 0.85, 0.80, 0.70, 0.60, 0.30, 0.20, 0.10, 0.05, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 0.10, 0.05, 0.03));
        normalDestructionChance.addAll(Arrays.asList(0.00, 0.00, 0.00, 0.05, 0.05, 0.10, 0.10, 0.15, 0.15, 0.20, 0.25, 0.25, 0.25, 0.35, 0.35, 0.35, 0.40, 0.45, 0.50, 0.55));

        // 고급 강화 확률 (0강 -> 1강 시도 ~ 19강 -> 20강 시도)
        advancedSuccessChance.addAll(Arrays.asList(0.99, 0.95, 0.9, 0.85, 0.75, 0.7, 0.65, 0.55, 0.45, 0.35, 0.3, 0.25, 0.22, 0.20, 0.10, 0.07, 0.065, 0.06, 0.055, 0.045));
        advancedDestructionChance.addAll(Arrays.asList(0.00, 0.00, 0.00, 0.00, 0.05, 0.05, 0.10, 0.15, 0.20, 0.20, 0.20, 0.20, 0.25, 0.25, 0.25, 0.35, 0.35, 0.40, 0.40, 0.50));
    }

    private boolean isReinforceable(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        Material type = item.getType();
        String typeName = type.name();
        return typeName.endsWith("_SWORD") ||
               typeName.endsWith("_HELMET") || typeName.endsWith("_CHESTPLATE") ||
               typeName.endsWith("_LEGGINGS") || typeName.endsWith("_BOOTS");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.ANVIL) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            ItemStack itemInHand = player.getInventory().getItemInMainHand();

            if (!isReinforceable(itemInHand)) {
                player.sendActionBar(Component.text("이 아이템은 강화가 불가능합니다.", NamedTextColor.RED));
                return;
            }
            openEnhancementUI(player, itemInHand);
        }
    }

    private void openEnhancementUI(Player player, ItemStack item) {
        Inventory gui = Bukkit.createInventory(null, 54, Component.text("강화"));
        ItemStack glassPane = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassPane.getItemMeta();
        glassMeta.displayName(Component.text(" "));
        glassPane.setItemMeta(glassMeta);

        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, glassPane);
        }

        gui.setItem(22, item.clone());

        ItemStack advancedStar = new ItemStack(Material.NETHER_STAR);
        ItemMeta advancedMeta = advancedStar.getItemMeta();
        advancedMeta.displayName(Component.text("고급 강화", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
        advancedStar.setItemMeta(advancedMeta);
        gui.setItem(21, advancedStar);

        ItemStack normalStar = new ItemStack(Material.NETHER_STAR);
        ItemMeta normalMeta = normalStar.getItemMeta();
        normalMeta.displayName(Component.text("일반 강화", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        normalStar.setItemMeta(normalMeta);
        gui.setItem(23, normalStar);

        player.openInventory(gui);
        openInventories.put(player.getUniqueId(), gui);
        originalItems.put(player.getUniqueId(), item);
        originalSlot.put(player.getUniqueId(), player.getInventory().getHeldItemSlot());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        UUID playerUuid = player.getUniqueId();

        if (openInventories.containsKey(playerUuid) && event.getInventory().equals(openInventories.get(playerUuid))) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 21) { // 고급 강화
                processEnhancement(player, true, false);
            } else if (slot == 23) { // 일반 강화
                processEnhancement(player, false, false);
            }
        }
    }

    public void processEnhancement(Player player, boolean isAdvanced, boolean cheat) {
        Inventory gui = openInventories.get(player.getUniqueId());
        if (gui == null) return;

        ItemStack targetItem;
        targetItem = gui.getItem(22);
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

        Material requiredMaterial = isAdvanced ? Material.DRAGON_BREATH : Material.ECHO_SHARD;
        String requiredItemName = isAdvanced ? "강화석" : "강화석 조각";

        if (!removeItemFromInventory(player, requiredMaterial, requiredItemName)) {
            player.sendMessage(Component.text(requiredItemName + "이(가) 부족합니다.", NamedTextColor.RED));
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
            ItemStack newStack = applyEnhancement(targetItem.clone(), newLevel);
            gui.setItem(22, newStack);
        } else if (random < successChance + destructionChance) {
            // 파괴
            gui.setItem(22, null);
            player.sendActionBar(Component.text("아이템이 파괴되었습니다...", NamedTextColor.DARK_RED));
        } else {
            // 실패 (하락)
            int newLevel = Math.max(0, currentLevel - 1);
            if (currentLevel > 0) {
                player.sendActionBar(Component.text("강화에 실패하여 등급이 하락했습니다. (+" + newLevel + ")", NamedTextColor.YELLOW));
                ItemStack newStack = applyEnhancement(targetItem.clone(), newLevel);
                gui.setItem(22, newStack);
            } else {
                player.sendActionBar(Component.text("강화에 실패했습니다.", NamedTextColor.YELLOW));
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
            ItemStack newStack = applyEnhancement(targetItem.clone(), lvl);
            targetItem.setItemMeta(newStack.getItemMeta());
        }
    }

    private ItemStack applyEnhancement(ItemStack item, int level) {
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
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("강화 레벨: +" + level, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        // 아이템 종류에 따른 속성 부여
        Material type = item.getType();
        String typeName = type.name();

        if (level > 0) { // 0강일때는 속성을 부여 x
            if (typeName.endsWith("_SWORD")) {
                meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(new NamespacedKey(this,"enhancement_attack"), level, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HAND));
                if (level >= 15 && typeName.endsWith("_SWORD")) {
                    meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(new NamespacedKey(this,"enhancement_attackspeed"), 100, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HAND));
                }
            } else if (typeName.endsWith("_HELMET")) {
                meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_helmet"), level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD));
                meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(new NamespacedKey(this,"enhancement_toughness_helmet"), level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD));
            } else if (typeName.endsWith("_CHESTPLATE")) {
                meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_chest"), level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST));
                meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(new NamespacedKey(this,"enhancement_toughness_chest"), level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST));
                meta.addAttributeModifier(Attribute.MAX_HEALTH, new AttributeModifier(new NamespacedKey(this,"enhancement_health_chest"), level * 2, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST));
            } else if (typeName.endsWith("_LEGGINGS")) {
                meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_leg"), level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS));
                meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(new NamespacedKey(this,"enhancement_toughness_leg"), level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS));
                meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, new AttributeModifier(new NamespacedKey(this,"enhancement_speed_leg"), level * 0.02, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.LEGS));
            } else if (typeName.endsWith("_BOOTS")) {
                meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey(this,"enhancement_armor_boots"),level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET));
                meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(new NamespacedKey(this,"enhancement_toughness_boots"), level* 1.5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET));
            }
        }

//        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }


    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        if (openInventories.containsKey(playerUuid) && event.getInventory().equals(openInventories.get(playerUuid))) {
            Inventory gui = openInventories.get(playerUuid);
            ItemStack resultItem = gui.getItem(22);
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
        }
    }
    
    private boolean removeItemFromInventory(Player player, Material material, String name) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                if (item.getItemMeta().getDisplayName().equals(name)) {
                    item.setAmount(item.getAmount() - 1);
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        // 특정 아이템인지 검사
        ItemStack item = attacker.getInventory().getItemInMainHand();
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        int level = container.getOrDefault(enhancementLevelKey, PersistentDataType.INTEGER, 0);
        if (item.getType().name().endsWith("_SWORD") && level >= 15) {
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
                    ItemStack chestplate = player.getInventory().getChestplate();
                    if (chestplate != null && chestplate.hasItemMeta()) {
                        PersistentDataContainer container = chestplate.getItemMeta().getPersistentDataContainer();
                        int level = container.getOrDefault(enhancementLevelKey, PersistentDataType.INTEGER, 0);
                        if (level >= 15) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 2, true, false));
                        }
                    }

                    ItemStack boots = player.getInventory().getBoots();
                    if (boots != null && boots.hasItemMeta()) {
                        PersistentDataContainer container = boots.getItemMeta().getPersistentDataContainer();
                        int level = container.getOrDefault(enhancementLevelKey, PersistentDataType.INTEGER, 0);
                        if (level >= 15 && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                            player.setAllowFlight(true);
                        } else if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
                            player.setAllowFlight(false);
                        }
                    } else if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
                         player.setAllowFlight(false);
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L * 1);
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
            if (level >= 15) {
                event.setCancelled(true);
                player.setAllowFlight(false);
                player.setFlying(false);
                player.setVelocity(player.getLocation().getDirection().multiply(0.5).setY(0.5));
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
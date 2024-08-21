package me.nashplugz.tycoond;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TycoonDroppers extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private Economy economy;
    private static final String DROPPER_DATA_PREFIX = ChatColor.DARK_GRAY + "Dropper Data: ";
    private final Map<UUID, Long> lastSellTime = new ConcurrentHashMap<>();
    private static final long SELL_COOLDOWN = 1000; // 1 second cooldown

    private static TycoonDroppers instance;

    @Override
    public void onEnable() {
        instance = this;
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("dropper") != null) {
            getCommand("dropper").setExecutor(this);
            getCommand("dropper").setTabCompleter(this);
        } else {
            getLogger().warning("Failed to register 'dropper' command. Is it defined in plugin.yml?");
        }

        setupEconomy();
    }

    public static TycoonDroppers getInstance() {
        if (instance == null) {
            instance = new TycoonDroppers();
        }
        return instance;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("dropper") && args.length == 6) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be run by a player.");
                return true;
            }

            Player player = (Player) sender;
            Material blockMaterial = Material.getMaterial(args[1].toUpperCase());
            Material itemMaterial = Material.getMaterial(args[2].toUpperCase());
            int interval;
            int amount;
            double itemValue;

            try {
                interval = Integer.parseInt(args[3]);
                amount = Integer.parseInt(args[4]);
                itemValue = Double.parseDouble(args[5]);
            } catch (NumberFormatException e) {
                player.sendMessage("Invalid number format.");
                return true;
            }

            if (blockMaterial == null || itemMaterial == null) {
                player.sendMessage("Invalid block or item material.");
                return true;
            }

            ItemStack dropperItem = createDropperItem(blockMaterial, itemMaterial, interval, amount, itemValue);
            player.getInventory().addItem(dropperItem);
            player.sendMessage(ChatColor.GREEN + "Custom dropper created and added to your inventory.");
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("dropper")) {
            if (args.length == 2 || args.length == 3) {
                for (Material material : Material.values()) {
                    if (material.isBlock() && args.length == 2) {
                        completions.add(material.name().toLowerCase());
                    } else if (material.isItem() && args.length == 3) {
                        completions.add(material.name().toLowerCase());
                    }
                }
            }
        }
        return completions;
    }

    private ItemStack createDropperItem(Material blockMaterial, Material itemMaterial, int interval, int amount, double itemValue) {
        ItemStack dropperItem = new ItemStack(blockMaterial);
        ItemMeta meta = dropperItem.getItemMeta();

        String itemName = Dropper.getCustomItemName(itemMaterial);
        meta.setDisplayName(ChatColor.GOLD + itemName + " (" + ChatColor.GREEN + "$" + String.format("%.2f", itemValue) + ChatColor.GOLD + ")");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Drops: " + ChatColor.YELLOW + itemMaterial.name().toLowerCase().replace("_", " "));
        lore.add(ChatColor.GRAY + "Interval: " + ChatColor.YELLOW + interval + " seconds");
        lore.add(ChatColor.GRAY + "Amount: " + ChatColor.YELLOW + amount);
        lore.add(ChatColor.GRAY + "Item Value: " + ChatColor.GREEN + "$" + String.format("%.2f", itemValue));

        // Add hidden dropper data
        String data = itemMaterial.name() + ";" + interval + ";" + amount + ";" + itemValue;
        lore.add(DROPPER_DATA_PREFIX + data);

        meta.setLore(lore);
        dropperItem.setItemMeta(meta);
        return dropperItem;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        ItemStack item = event.getItemInHand();
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            for (String line : lore) {
                if (line.startsWith(DROPPER_DATA_PREFIX)) {
                    String data = line.substring(DROPPER_DATA_PREFIX.length());
                    String[] parts = data.split(";");
                    if (parts.length == 4) {
                        Material itemMaterial = Material.getMaterial(parts[0]);
                        int interval = Integer.parseInt(parts[1]);
                        int amount = Integer.parseInt(parts[2]);
                        double itemValue = Double.parseDouble(parts[3]);

                        Location location = block.getLocation();
                        UUID uuid = UUID.randomUUID();

                        Dropper dropper = new Dropper(location, itemMaterial, interval, amount, itemValue);
                        DropperManager.addDropper(uuid, dropper);

                        updateFloatingText(dropper);

                        Bukkit.getScheduler().runTaskTimer(this, () -> {
                            Dropper d = DropperManager.getDropper(uuid);
                            if (d != null) {
                                d.drop();
                                updateFloatingText(d);
                            }
                        }, 0L, interval * 20L);
                        break;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockDispense(BlockDispenseEvent event) {
        Block block = event.getBlock();
        Dropper dropper = DropperManager.getDropper(block.getLocation());
        if (dropper != null) {
            event.setCancelled(true);
            dropper.drop();
            updateFloatingText(dropper);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        Dropper dropper = DropperManager.getDropper(block.getLocation());
        if (dropper != null) {
            DropperManager.removeDropper(block.getLocation());
            removeFloatingText(dropper.getLocation());
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Block block = player.getLocation().getBlock();

        if (block.getType() == Material.LIGHT_WEIGHTED_PRESSURE_PLATE) {
            long currentTime = System.currentTimeMillis();
            long lastSell = lastSellTime.getOrDefault(player.getUniqueId(), 0L);
            if (currentTime - lastSell >= SELL_COOLDOWN) {
                sellItems(player);
                lastSellTime.put(player.getUniqueId(), currentTime);
            }
        }
    }

    private void sellItems(Player player) {
        double totalValue = 0;
        Map<String, Integer> soldItems = new HashMap<>();
        List<ItemStack> itemsToRemove = new ArrayList<>();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.getPersistentDataContainer().has(new NamespacedKey(this, "item_value"), PersistentDataType.DOUBLE)) {
                    double value = meta.getPersistentDataContainer().get(new NamespacedKey(this, "item_value"), PersistentDataType.DOUBLE);
                    totalValue += value * item.getAmount();

                    String itemName = meta.getDisplayName();
                    soldItems.put(itemName, soldItems.getOrDefault(itemName, 0) + item.getAmount());

                    itemsToRemove.add(item);
                }
            }
        }

        if (totalValue > 0) {
            economy.depositPlayer(player, totalValue);

            player.sendMessage(ChatColor.GREEN + "You sold the following items:");
            for (Map.Entry<String, Integer> entry : soldItems.entrySet()) {
                player.sendMessage(ChatColor.YELLOW + "- " + entry.getValue() + "x " + entry.getKey());
            }
            player.sendMessage(ChatColor.GREEN + "Total value: $" + String.format("%.2f", totalValue));

            for (ItemStack item : itemsToRemove) {
                player.getInventory().remove(item);
            }
            player.updateInventory();
        }
    }

    private void updateFloatingText(Dropper dropper) {
        Location textLocation = dropper.getLocation().clone().add(0.5, 0.5, 0.5);

        removeFloatingText(dropper.getLocation());

        ArmorStand hologram = (ArmorStand) textLocation.getWorld().spawnEntity(textLocation, EntityType.ARMOR_STAND);
        hologram.setGravity(false);
        hologram.setCanPickupItems(false);
        hologram.setCustomNameVisible(true);
        hologram.setVisible(false);
        hologram.setSmall(true);

        String itemName = Dropper.getCustomItemName(dropper.getItemMaterial());
        String text = ChatColor.GOLD + itemName + " (" + ChatColor.GREEN + "$" + String.format("%.2f", dropper.getItemValue()) + ChatColor.GOLD + ")";

        hologram.setCustomName(text);
    }

    private void removeFloatingText(Location location) {
        Location textLocation = location.clone().add(0.5, 0.5, 0.5);
        for (Entity entity : textLocation.getWorld().getNearbyEntities(textLocation, 1, 1, 1)) {
            if (entity instanceof ArmorStand && entity.isCustomNameVisible()) {
                entity.remove();
            }
        }
    }

    public Economy getEconomy() {
        return economy;
    }
}

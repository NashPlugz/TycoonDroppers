package me.nashplugz.tycoond;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class Dropper {
    private final Location location;
    private final Material itemMaterial;
    private final int interval;
    private final int amount;
    private final double itemValue;
    private int totalDropped;

    public static final List<Material> INGOTS = Arrays.asList(
            Material.IRON_INGOT, Material.GOLD_INGOT, Material.COPPER_INGOT,
            Material.NETHERITE_INGOT, Material.BRICK
    );

    public static final List<Material> GEMS = Arrays.asList(
            Material.DIAMOND, Material.EMERALD, Material.AMETHYST_SHARD,
            Material.QUARTZ, Material.LAPIS_LAZULI
    );

    public Dropper(Location location, Material itemMaterial, int interval, int amount, double itemValue) {
        this.location = location;
        this.itemMaterial = itemMaterial;
        this.interval = interval;
        this.amount = amount;
        this.itemValue = itemValue;
        this.totalDropped = 0;
    }

    public void drop() {
        Location dropLocation = location.clone().add(0.5, -1, 0.5);
        for (int i = 0; i < amount; i++) {
            ItemStack item = createCustomItem(itemMaterial, itemValue);
            dropLocation.getWorld().dropItem(dropLocation, item);
            totalDropped++;
        }
    }

    private ItemStack createCustomItem(Material material, double value) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String itemName = getCustomItemName(material);
        meta.setDisplayName(ChatColor.YELLOW + itemName);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Value: " + ChatColor.GREEN + "$" + String.format("%.2f", value));
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(new NamespacedKey(TycoonDroppers.getPlugin(TycoonDroppers.class), "item_value"), PersistentDataType.DOUBLE, value);
        item.setItemMeta(meta);
        return item;
    }

    public static String getCustomItemName(Material material) {
        if (material == Material.SUNFLOWER) {
            return "Coin";
        }

        String baseName = material.name().replace("_", " ");
        String[] words = baseName.split(" ");
        String mainName = capitalizeFirstLetter(words[0]);

        if (INGOTS.contains(material)) {
            return mainName + " Bux";
        } else if (GEMS.contains(material)) {
            return mainName + " Shard";
        } else {
            return capitalizeFirstLetter(baseName) + " Token";
        }
    }

    private static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    public Location getLocation() {
        return location;
    }

    public Material getItemMaterial() {
        return itemMaterial;
    }

    public int getInterval() {
        return interval;
    }

    public int getAmount() {
        return amount;
    }

    public double getItemValue() {
        return itemValue;
    }

    public int getTotalDropped() {
        return totalDropped;
    }
}

package me.nashplugz.tycoond;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DropperItem extends ItemStack implements ConfigurationSerializable {
    private Material dropMaterial;
    private int interval;
    private int amount;
    private double itemValue;

    public DropperItem(Material blockMaterial, Material dropMaterial, int interval, int amount, double itemValue) {
        super(blockMaterial);
        this.dropMaterial = dropMaterial;
        this.interval = interval;
        this.amount = amount;
        this.itemValue = itemValue;
        updateMeta();
    }

    private void updateMeta() {
        ItemMeta meta = getItemMeta();
        if (meta != null) {
            String itemName = Dropper.getCustomItemName(dropMaterial);
            meta.setDisplayName(ChatColor.GOLD + itemName + " (" + ChatColor.GREEN + "$" + String.format("%.2f", itemValue) + ChatColor.GOLD + ")");

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Drops: " + ChatColor.YELLOW + dropMaterial.name().toLowerCase().replace("_", " "));
            lore.add(ChatColor.GRAY + "Interval: " + ChatColor.YELLOW + interval + " seconds");
            lore.add(ChatColor.GRAY + "Amount: " + ChatColor.YELLOW + amount);
            lore.add(ChatColor.GRAY + "Item Value: " + ChatColor.GREEN + "$" + String.format("%.2f", itemValue));
            meta.setLore(lore);
            setItemMeta(meta);
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> result = new HashMap<>();
        result.put("block-material", getType().name());
        result.put("drop-material", dropMaterial.name());
        result.put("interval", interval);
        result.put("amount", amount);
        result.put("item-value", itemValue);
        return result;
    }

    public static DropperItem deserialize(Map<String, Object> args) {
        Material blockMaterial = Material.valueOf((String) args.get("block-material"));
        Material dropMaterial = Material.valueOf((String) args.get("drop-material"));
        int interval = (int) args.get("interval");
        int amount = (int) args.get("amount");
        double itemValue = (double) args.get("item-value");
        return new DropperItem(blockMaterial, dropMaterial, interval, amount, itemValue);
    }

    public Material getDropMaterial() {
        return dropMaterial;
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
}
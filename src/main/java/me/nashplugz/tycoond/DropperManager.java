package me.nashplugz.tycoond;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DropperManager {
    private static final Map<UUID, Dropper> droppers = new HashMap<>();

    public static void addDropper(UUID uuid, Dropper dropper) {
        droppers.put(uuid, dropper);
    }

    public static Dropper getDropper(UUID uuid) {
        return droppers.get(uuid);
    }

    public static Dropper getDropper(Location location) {
        for (Dropper dropper : droppers.values()) {
            if (dropper.getLocation().equals(location)) {
                return dropper;
            }
        }
        return null;
    }

    public static void removeDropper(Location location) {
        droppers.values().removeIf(dropper -> dropper.getLocation().equals(location));
    }

    public static void clear() {
        droppers.clear();
    }
}
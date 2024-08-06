package me.nashplugz.tycoond;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Plot {
    private final int x;
    private final int z;
    private UUID owner;
    private Set<UUID> trustedPlayers;

    public Plot(int x, int z) {
        this.x = x;
        this.z = z;
        this.trustedPlayers = new HashSet<>();
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public boolean isOwner(UUID playerUUID) {
        return owner != null && owner.equals(playerUUID);
    }

    public boolean isTrusted(UUID playerUUID) {
        return trustedPlayers.contains(playerUUID);
    }

    public void addTrustedPlayer(UUID playerUUID) {
        trustedPlayers.add(playerUUID);
    }

    public void removeTrustedPlayer(UUID playerUUID) {
        trustedPlayers.remove(playerUUID);
    }

    public boolean canEdit(UUID playerUUID) {
        return isOwner(playerUUID) || isTrusted(playerUUID);
    }
}

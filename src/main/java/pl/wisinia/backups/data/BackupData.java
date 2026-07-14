package pl.wisinia.backups.data;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class BackupData {

    private final UUID playerUUID;
    private final String playerName;
    private final ItemStack[] inventory;
    private final ItemStack[] armor;
    private final ItemStack offHand;
    private final int deathNumber;

    public BackupData(UUID playerUUID, String playerName,
                      ItemStack[] inventory, ItemStack[] armor,
                      ItemStack offHand, int deathNumber) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.inventory = inventory;
        this.armor = armor;
        this.offHand = offHand;
        this.deathNumber = deathNumber;
    }

    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public ItemStack[] getInventory() { return inventory; }
    public ItemStack[] getArmor() { return armor; }
    public ItemStack getOffHand() { return offHand; }
    public int getDeathNumber() { return deathNumber; }
}

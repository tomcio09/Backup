package pl.wisinia.backups.data;

import org.bukkit.inventory.ItemStack;

import java.time.LocalDateTime;
import java.util.UUID;

public class DeathRecord {

    public enum DeathType {
        SUICIDE,    // Samobójstwo
        KILL,       // Zabójstwo
        LOGOUT      // Wylogowanie się podczas walki
    }

    private final UUID playerUUID;
    private final String playerName;
    private final DeathType deathType;
    private final LocalDateTime deathTime;
    private final UUID killerUUID;
    private final String killerName;

    // Sloty ekwipunku
    private final ItemStack[] inventory;   // 36 slotów (0-35) główny ekwipunek
    private final ItemStack[] armor;       // 4 sloty zbroi (0=boots,1=legs,2=chest,3=helmet)
    private final ItemStack offHand;       // offhand

    private BackupStatus backupStatus;
    private final int deathNumber;

    public enum BackupStatus {
        PENDING,    // Oczekujący
        RECEIVED,   // Odebrany
        NONE        // Brak (jeszcze nie nadany)
    }

    public DeathRecord(UUID playerUUID, String playerName, DeathType deathType,
                       LocalDateTime deathTime, UUID killerUUID, String killerName,
                       ItemStack[] inventory, ItemStack[] armor, ItemStack offHand,
                       int deathNumber) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.deathType = deathType;
        this.deathTime = deathTime;
        this.killerUUID = killerUUID;
        this.killerName = killerName;
        this.inventory = inventory;
        this.armor = armor;
        this.offHand = offHand;
        this.backupStatus = BackupStatus.NONE;
        this.deathNumber = deathNumber;
    }

    // Gettery
    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public DeathType getDeathType() { return deathType; }
    public LocalDateTime getDeathTime() { return deathTime; }
    public UUID getKillerUUID() { return killerUUID; }
    public String getKillerName() { return killerName; }
    public ItemStack[] getInventory() { return inventory; }
    public ItemStack[] getArmor() { return armor; }
    public ItemStack getOffHand() { return offHand; }
    public BackupStatus getBackupStatus() { return backupStatus; }
    public int getDeathNumber() { return deathNumber; }

    public void setBackupStatus(BackupStatus status) {
        this.backupStatus = status;
    }
}

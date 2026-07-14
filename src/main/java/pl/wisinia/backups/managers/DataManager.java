package pl.wisinia.backups.managers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import pl.wisinia.backups.WisiniaBackups;
import pl.wisinia.backups.data.BackupData;
import pl.wisinia.backups.data.DeathRecord;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DataManager {

    private final WisiniaBackups plugin;
    private final File dataFolder;

    // UUID gracza -> lista śmierci
    private final Map<UUID, List<DeathRecord>> deathRecords = new HashMap<>();
    // UUID gracza -> aktywny backup do odebrania
    private final Map<UUID, BackupData> pendingBackups = new HashMap<>();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DataManager(WisiniaBackups plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public void load() {
        loadDeathRecords();
        loadPendingBackups();
    }

    public void save() {
        saveDeathRecords();
        savePendingBackups();
    }

    // ============== DEATH RECORDS ==============

    public void addDeathRecord(DeathRecord record) {
        deathRecords.computeIfAbsent(record.getPlayerUUID(), k -> new ArrayList<>()).add(record);
        saveDeathRecords();
    }

    public List<DeathRecord> getDeathRecords(UUID uuid) {
        return deathRecords.getOrDefault(uuid, new ArrayList<>());
    }

    public int getNextDeathNumber(UUID uuid) {
        return getDeathRecords(uuid).size() + 1;
    }

    private void saveDeathRecords() {
        File file = new File(dataFolder, "deaths.yml");
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, List<DeathRecord>> entry : deathRecords.entrySet()) {
            String uuidStr = entry.getKey().toString();
            List<DeathRecord> records = entry.getValue();

            for (int i = 0; i < records.size(); i++) {
                DeathRecord record = records.get(i);
                String path = uuidStr + "." + i;

                config.set(path + ".playerName", record.getPlayerName());
                config.set(path + ".deathType", record.getDeathType().name());
                config.set(path + ".deathTime", record.getDeathTime().format(FORMATTER));
                config.set(path + ".killerUUID", record.getKillerUUID() != null ? record.getKillerUUID().toString() : null);
                config.set(path + ".killerName", record.getKillerName());
                config.set(path + ".backupStatus", record.getBackupStatus().name());
                config.set(path + ".deathNumber", record.getDeathNumber());

                // Zapisz inventory (36 slotów)
                ItemStack[] inv = record.getInventory();
                for (int s = 0; s < inv.length; s++) {
                    if (inv[s] != null) {
                        config.set(path + ".inventory." + s, inv[s]);
                    }
                }

                // Zapisz zbroję (4 sloty)
                ItemStack[] armor = record.getArmor();
                for (int s = 0; s < armor.length; s++) {
                    if (armor[s] != null) {
                        config.set(path + ".armor." + s, armor[s]);
                    }
                }

                // Zapisz offhand
                if (record.getOffHand() != null) {
                    config.set(path + ".offhand", record.getOffHand());
                }
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Błąd podczas zapisywania śmierci: " + e.getMessage());
        }
    }

    private void loadDeathRecords() {
        File file = new File(dataFolder, "deaths.yml");
        if (!file.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String uuidStr : config.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }

            List<DeathRecord> records = new ArrayList<>();
            Set<String> indices = config.getConfigurationSection(uuidStr).getKeys(false);

            List<Integer> sortedIndices = new ArrayList<>();
            for (String idx : indices) {
                try {
                    sortedIndices.add(Integer.parseInt(idx));
                } catch (NumberFormatException ignored) {}
            }
            Collections.sort(sortedIndices);

            for (int i : sortedIndices) {
                String path = uuidStr + "." + i;

                String playerName = config.getString(path + ".playerName", "Unknown");
                String deathTypeStr = config.getString(path + ".deathType", "SUICIDE");
                String deathTimeStr = config.getString(path + ".deathTime");
                String killerUUIDStr = config.getString(path + ".killerUUID");
                String killerName = config.getString(path + ".killerName");
                String backupStatusStr = config.getString(path + ".backupStatus", "NONE");
                int deathNumber = config.getInt(path + ".deathNumber", i + 1);

                DeathRecord.DeathType deathType;
                try {
                    deathType = DeathRecord.DeathType.valueOf(deathTypeStr);
                } catch (IllegalArgumentException e) {
                    deathType = DeathRecord.DeathType.SUICIDE;
                }

                LocalDateTime deathTime;
                try {
                    deathTime = LocalDateTime.parse(deathTimeStr, FORMATTER);
                } catch (Exception e) {
                    deathTime = LocalDateTime.now();
                }

                UUID killerUUID = null;
                if (killerUUIDStr != null) {
                    try {
                        killerUUID = UUID.fromString(killerUUIDStr);
                    } catch (IllegalArgumentException ignored) {}
                }

                // Wczytaj inventory
                ItemStack[] inv = new ItemStack[36];
                if (config.isConfigurationSection(path + ".inventory")) {
                    for (String slotStr : config.getConfigurationSection(path + ".inventory").getKeys(false)) {
                        try {
                            int slot = Integer.parseInt(slotStr);
                            ItemStack item = config.getItemStack(path + ".inventory." + slotStr);
                            if (slot >= 0 && slot < 36) {
                                inv[slot] = item;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }

                // Wczytaj zbroję
                ItemStack[] armor = new ItemStack[4];
                if (config.isConfigurationSection(path + ".armor")) {
                    for (String slotStr : config.getConfigurationSection(path + ".armor").getKeys(false)) {
                        try {
                            int slot = Integer.parseInt(slotStr);
                            ItemStack item = config.getItemStack(path + ".armor." + slotStr);
                            if (slot >= 0 && slot < 4) {
                                armor[slot] = item;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }

                // Wczytaj offhand
                ItemStack offHand = config.getItemStack(path + ".offhand");

                DeathRecord record = new DeathRecord(uuid, playerName, deathType, deathTime,
                        killerUUID, killerName, inv, armor, offHand, deathNumber);

                DeathRecord.BackupStatus backupStatus;
                try {
                    backupStatus = DeathRecord.BackupStatus.valueOf(backupStatusStr);
                } catch (IllegalArgumentException e) {
                    backupStatus = DeathRecord.BackupStatus.NONE;
                }
                record.setBackupStatus(backupStatus);
                records.add(record);
            }

            if (!records.isEmpty()) {
                deathRecords.put(uuid, records);
            }
        }
    }

    // ============== PENDING BACKUPS ==============

    public boolean hasPendingBackup(UUID uuid) {
        return pendingBackups.containsKey(uuid);
    }

    public BackupData getPendingBackup(UUID uuid) {
        return pendingBackups.get(uuid);
    }

    public void setPendingBackup(UUID uuid, BackupData data) {
        pendingBackups.put(uuid, data);
        savePendingBackups();
    }

    public void removePendingBackup(UUID uuid) {
        pendingBackups.remove(uuid);
        savePendingBackups();
    }

    private void savePendingBackups() {
        File file = new File(dataFolder, "pending_backups.yml");
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, BackupData> entry : pendingBackups.entrySet()) {
            String uuidStr = entry.getKey().toString();
            BackupData data = entry.getValue();

            config.set(uuidStr + ".playerName", data.getPlayerName());
            config.set(uuidStr + ".deathNumber", data.getDeathNumber());

            ItemStack[] inv = data.getInventory();
            for (int s = 0; s < inv.length; s++) {
                if (inv[s] != null) {
                    config.set(uuidStr + ".inventory." + s, inv[s]);
                }
            }

            ItemStack[] armor = data.getArmor();
            for (int s = 0; s < armor.length; s++) {
                if (armor[s] != null) {
                    config.set(uuidStr + ".armor." + s, armor[s]);
                }
            }

            if (data.getOffHand() != null) {
                config.set(uuidStr + ".offhand", data.getOffHand());
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Błąd podczas zapisywania pending backupów: " + e.getMessage());
        }
    }

    private void loadPendingBackups() {
        File file = new File(dataFolder, "pending_backups.yml");
        if (!file.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String uuidStr : config.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }

            String playerName = config.getString(uuidStr + ".playerName", "Unknown");
            int deathNumber = config.getInt(uuidStr + ".deathNumber", 0);

            ItemStack[] inv = new ItemStack[36];
            if (config.isConfigurationSection(uuidStr + ".inventory")) {
                for (String slotStr : config.getConfigurationSection(uuidStr + ".inventory").getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(slotStr);
                        ItemStack item = config.getItemStack(uuidStr + ".inventory." + slotStr);
                        if (slot >= 0 && slot < 36) {
                            inv[slot] = item;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

            ItemStack[] armor = new ItemStack[4];
            if (config.isConfigurationSection(uuidStr + ".armor")) {
                for (String slotStr : config.getConfigurationSection(uuidStr + ".armor").getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(slotStr);
                        ItemStack item = config.getItemStack(uuidStr + ".armor." + slotStr);
                        if (slot >= 0 && slot < 4) {
                            armor[slot] = item;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

            ItemStack offHand = config.getItemStack(uuidStr + ".offhand");

            pendingBackups.put(uuid, new BackupData(uuid, playerName, inv, armor, offHand, deathNumber));
        }
    }

    public void updateDeathRecordStatus(UUID playerUUID, int deathIndex, DeathRecord.BackupStatus status) {
        List<DeathRecord> records = deathRecords.get(playerUUID);
        if (records != null && deathIndex >= 0 && deathIndex < records.size()) {
            records.get(deathIndex).setBackupStatus(status);
            saveDeathRecords();
        }
    }
}

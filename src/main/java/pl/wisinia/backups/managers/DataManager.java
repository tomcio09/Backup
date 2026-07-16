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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DataManager {

    private final WisiniaBackups plugin;
    private final File dataFolder;

    // Thread-safe kolekcje
    private final ConcurrentHashMap<UUID, List<DeathRecord>> deathRecords = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, BackupData> pendingBackups = new ConcurrentHashMap<>();

    // Cache nick -> UUID (unikamy Bukkit.getOfflinePlayers())
    private final ConcurrentHashMap<String, UUID> nameToUuidCache = new ConcurrentHashMap<>();

    // Lock dla operacji zapisu/odczytu plików
    private final ReentrantReadWriteLock deathLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock backupLock = new ReentrantReadWriteLock();

    // Flaga dirty - zapisujemy tylko gdy coś się zmieniło
    private volatile boolean deathsDirty = false;
    private volatile boolean backupsDirty = false;

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

    /**
     * Asynchroniczny zapis - wywoływany z timera
     */
    public void saveAsync() {
        if (deathsDirty) {
            deathsDirty = false;
            saveDeathRecordsToDisk();
        }
        if (backupsDirty) {
            backupsDirty = false;
            savePendingBackupsToDisk();
        }
    }

    /**
     * Synchroniczny zapis - wywoływany przy wyłączaniu serwera
     */
    public void saveSynchronous() {
        saveDeathRecordsToDisk();
        savePendingBackupsToDisk();
    }

    // ============== DEATH RECORDS ==============

    public void addDeathRecord(DeathRecord record) {
        deathRecords.computeIfAbsent(record.getPlayerUUID(), k ->
                Collections.synchronizedList(new ArrayList<>())).add(record);

        // Cache nick -> UUID
        cachePlayer(record.getPlayerName(), record.getPlayerUUID());

        deathsDirty = true;

        // Zapis asynchroniczny
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveDeathRecordsToDisk);
    }

    public List<DeathRecord> getDeathRecords(UUID uuid) {
        List<DeathRecord> records = deathRecords.get(uuid);
        if (records == null) return new ArrayList<>();
        synchronized (records) {
            return new ArrayList<>(records);
        }
    }

    public int getNextDeathNumber(UUID uuid) {
        return getDeathRecords(uuid).size() + 1;
    }

    public void updateDeathRecordStatus(UUID playerUUID, int deathIndex, DeathRecord.BackupStatus status) {
        List<DeathRecord> records = deathRecords.get(playerUUID);
        if (records != null) {
            synchronized (records) {
                if (deathIndex >= 0 && deathIndex < records.size()) {
                    records.get(deathIndex).setBackupStatus(status);
                    deathsDirty = true;
                }
            }
        }
    }

    private void saveDeathRecordsToDisk() {
        deathLock.writeLock().lock();
        try {
            File file = new File(dataFolder, "deaths.yml");
            FileConfiguration config = new YamlConfiguration();

            for (Map.Entry<UUID, List<DeathRecord>> entry : deathRecords.entrySet()) {
                String uuidStr = entry.getKey().toString();
                List<DeathRecord> records = entry.getValue();

                synchronized (records) {
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

                        ItemStack[] inv = record.getInventory();
                        for (int s = 0; s < inv.length; s++) {
                            if (inv[s] != null) {
                                config.set(path + ".inventory." + s, inv[s]);
                            }
                        }

                        ItemStack[] armor = record.getArmor();
                        for (int s = 0; s < armor.length; s++) {
                            if (armor[s] != null) {
                                config.set(path + ".armor." + s, armor[s]);
                            }
                        }

                        if (record.getOffHand() != null) {
                            config.set(path + ".offhand", record.getOffHand());
                        }
                    }
                }
            }

            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Błąd podczas zapisywania śmierci: " + e.getMessage());
        } finally {
            deathLock.writeLock().unlock();
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

            if (!config.isConfigurationSection(uuidStr)) continue;

            List<DeathRecord> records = Collections.synchronizedList(new ArrayList<>());
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

                // Cache nick
                cachePlayer(playerName, uuid);

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
        backupsDirty = true;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::savePendingBackupsToDisk);
    }

    public void removePendingBackup(UUID uuid) {
        pendingBackups.remove(uuid);
        backupsDirty = true;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::savePendingBackupsToDisk);
    }

    private void savePendingBackupsToDisk() {
        backupLock.writeLock().lock();
        try {
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

            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Błąd podczas zapisywania pending backupów: " + e.getMessage());
        } finally {
            backupLock.writeLock().unlock();
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

            // Cache nick
            cachePlayer(playerName, uuid);

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

    // ============== NAME -> UUID CACHE ==============

    public void cachePlayer(String name, UUID uuid) {
        if (name != null && uuid != null) {
            nameToUuidCache.put(name.toLowerCase(), uuid);
        }
    }

    /**
     * Szybkie wyszukiwanie UUID po nicku - bez iterowania po offline players
     */
    public UUID getUUIDByName(String name) {
        if (name == null) return null;

        // 1. Online gracz
        org.bukkit.entity.Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            cachePlayer(name, online.getUniqueId());
            return online.getUniqueId();
        }

        // 2. Cache
        UUID cached = nameToUuidCache.get(name.toLowerCase());
        if (cached != null) return cached;

        // 3. Bukkit offline player (pojedynczy lookup - szybki)
        @SuppressWarnings("deprecation")
        org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(name);
        if (op.hasPlayedBefore() || op.isOnline()) {
            cachePlayer(name, op.getUniqueId());
            return op.getUniqueId();
        }

        return null;
    }
}

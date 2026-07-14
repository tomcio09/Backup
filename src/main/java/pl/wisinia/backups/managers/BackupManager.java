package pl.wisinia.backups.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.wisinia.backups.WisiniaBackups;
import pl.wisinia.backups.data.BackupData;
import pl.wisinia.backups.data.DeathRecord;

import java.util.UUID;

public class BackupManager {

    private final WisiniaBackups plugin;

    public BackupManager(WisiniaBackups plugin) {
        this.plugin = plugin;
    }

    /**
     * Nadaje backup graczowi z danej śmierci
     * Zwraca false jeśli gracz ma już pending backup
     */
    public boolean giveBackup(UUID targetUUID, String targetName, int deathIndex) {
        DataManager dm = plugin.getDataManager();

        if (dm.hasPendingBackup(targetUUID)) {
            return false;
        }

        java.util.List<DeathRecord> records = dm.getDeathRecords(targetUUID);
        if (deathIndex < 0 || deathIndex >= records.size()) {
            return false;
        }

        DeathRecord record = records.get(deathIndex);

        BackupData backupData = new BackupData(
                targetUUID,
                targetName,
                cloneArray(record.getInventory()),
                cloneArray(record.getArmor()),
                record.getOffHand() != null ? record.getOffHand().clone() : null,
                record.getDeathNumber()
        );

        dm.setPendingBackup(targetUUID, backupData);
        dm.updateDeathRecordStatus(targetUUID, deathIndex, DeathRecord.BackupStatus.PENDING);

        // Powiadom gracza jeśli jest online
        Player player = Bukkit.getPlayer(targetUUID);
        if (player != null && player.isOnline()) {
            player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacyAmpersand().deserialize("&7Masz &abackupa &7do odebrania pod &f/odbierzbackup&7!"));
        }

        return true;
    }

    /**
     * Odbiera backup dla gracza - rozdaje itemy
     * Zwraca true jeśli sukces
     */
    public boolean receiveBackup(Player player) {
        DataManager dm = plugin.getDataManager();
        UUID uuid = player.getUniqueId();

        BackupData backup = dm.getPendingBackup(uuid);
        if (backup == null) return false;

        ItemStack[] inv = backup.getInventory();
        ItemStack[] armor = backup.getArmor();
        ItemStack offHand = backup.getOffHand();

        // Sprawdź czy eq jest puste
        boolean emptyInventory = isInventoryEmpty(player);

        if (emptyInventory) {
            // Daj itemy dokładnie na miejsce
            giveItemsExact(player, inv, armor, offHand);
        } else {
            // Daj itemy, jeśli slot zajęty -> kolejny wolny -> drop
            giveItemsWithFallback(player, inv, armor, offHand);
        }

        // Zaktualizuj status śmierci na RECEIVED
        updateDeathStatusToReceived(uuid, backup.getDeathNumber());

        dm.removePendingBackup(uuid);
        return true;
    }

    private void giveItemsExact(Player player, ItemStack[] inv, ItemStack[] armor, ItemStack offHand) {
        // Główny ekwipunek (sloty 0-35)
        for (int i = 0; i < inv.length; i++) {
            if (inv[i] != null) {
                player.getInventory().setItem(i, inv[i].clone());
            }
        }

        // Zbroja: 0=boots, 1=legs, 2=chest, 3=helmet
        if (armor[0] != null) player.getInventory().setBoots(armor[0].clone());
        if (armor[1] != null) player.getInventory().setLeggings(armor[1].clone());
        if (armor[2] != null) player.getInventory().setChestplate(armor[2].clone());
        if (armor[3] != null) player.getInventory().setHelmet(armor[3].clone());

        // Offhand
        if (offHand != null) {
            player.getInventory().setItemInOffHand(offHand.clone());
        }
    }

    private void giveItemsWithFallback(Player player, ItemStack[] inv, ItemStack[] armor, ItemStack offHand) {
        // Zbroja
        giveArmorWithFallback(player, armor, offHand);

        // Główny ekwipunek (sloty 0-35)
        for (int i = 0; i < inv.length; i++) {
            if (inv[i] == null) continue;
            ItemStack current = player.getInventory().getItem(i);
            if (current == null) {
                player.getInventory().setItem(i, inv[i].clone());
            } else {
                // Znajdź wolny slot
                int freeSlot = findFreeSlot(player);
                if (freeSlot != -1) {
                    player.getInventory().setItem(freeSlot, inv[i].clone());
                } else {
                    // Drop na ziemię
                    player.getWorld().dropItemNaturally(player.getLocation(), inv[i].clone());
                }
            }
        }
    }

    private void giveArmorWithFallback(Player player, ItemStack[] armor, ItemStack offHand) {
        // Boots (armor[0])
        if (armor[0] != null) {
            if (player.getInventory().getBoots() == null) {
                player.getInventory().setBoots(armor[0].clone());
            } else {
                int free = findFreeSlot(player);
                if (free != -1) player.getInventory().setItem(free, armor[0].clone());
                else player.getWorld().dropItemNaturally(player.getLocation(), armor[0].clone());
            }
        }

        // Leggings (armor[1])
        if (armor[1] != null) {
            if (player.getInventory().getLeggings() == null) {
                player.getInventory().setLeggings(armor[1].clone());
            } else {
                int free = findFreeSlot(player);
                if (free != -1) player.getInventory().setItem(free, armor[1].clone());
                else player.getWorld().dropItemNaturally(player.getLocation(), armor[1].clone());
            }
        }

        // Chestplate (armor[2])
        if (armor[2] != null) {
            if (player.getInventory().getChestplate() == null) {
                player.getInventory().setChestplate(armor[2].clone());
            } else {
                int free = findFreeSlot(player);
                if (free != -1) player.getInventory().setItem(free, armor[2].clone());
                else player.getWorld().dropItemNaturally(player.getLocation(), armor[2].clone());
            }
        }

        // Helmet (armor[3])
        if (armor[3] != null) {
            if (player.getInventory().getHelmet() == null) {
                player.getInventory().setHelmet(armor[3].clone());
            } else {
                int free = findFreeSlot(player);
                if (free != -1) player.getInventory().setItem(free, armor[3].clone());
                else player.getWorld().dropItemNaturally(player.getLocation(), armor[3].clone());
            }
        }

        // Offhand
        if (offHand != null) {
            ItemStack currentOffhand = player.getInventory().getItemInOffHand();
            if (currentOffhand.getType().isAir()) {
                player.getInventory().setItemInOffHand(offHand.clone());
            } else {
                int free = findFreeSlot(player);
                if (free != -1) player.getInventory().setItem(free, offHand.clone());
                else player.getWorld().dropItemNaturally(player.getLocation(), offHand.clone());
            }
        }
    }

    private int findFreeSlot(Player player) {
        for (int i = 0; i < 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType().isAir()) {
                return i;
            }
        }
        return -1;
    }

    private boolean isInventoryEmpty(Player player) {
        for (int i = 0; i < 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && !item.getType().isAir()) return false;
        }
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chest = player.getInventory().getChestplate();
        ItemStack legs = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();
        ItemStack offhand = player.getInventory().getItemInOffHand();

        if (helmet != null && !helmet.getType().isAir()) return false;
        if (chest != null && !chest.getType().isAir()) return false;
        if (legs != null && !legs.getType().isAir()) return false;
        if (boots != null && !boots.getType().isAir()) return false;
        if (offhand != null && !offhand.getType().isAir()) return false;

        return true;
    }

    private void updateDeathStatusToReceived(UUID uuid, int deathNumber) {
        java.util.List<DeathRecord> records = plugin.getDataManager().getDeathRecords(uuid);
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).getDeathNumber() == deathNumber) {
                plugin.getDataManager().updateDeathRecordStatus(uuid, i, DeathRecord.BackupStatus.RECEIVED);
                break;
            }
        }
    }

    private ItemStack[] cloneArray(ItemStack[] arr) {
        if (arr == null) return new ItemStack[0];
        ItemStack[] clone = new ItemStack[arr.length];
        for (int i = 0; i < arr.length; i++) {
            clone[i] = arr[i] != null ? arr[i].clone() : null;
        }
        return clone;
    }
}

package pl.wisinia.backups.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.wisinia.backups.WisiniaBackups;
import pl.wisinia.backups.data.BackupData;
import pl.wisinia.backups.data.DeathRecord;

import java.util.List;
import java.util.UUID;

public class BackupManager {

    private final WisiniaBackups plugin;

    public BackupManager(WisiniaBackups plugin) {
        this.plugin = plugin;
    }

    public boolean giveBackup(UUID targetUUID, String targetName, int deathIndex) {
        DataManager dm = plugin.getDataManager();

        if (dm.hasPendingBackup(targetUUID)) {
            return false;
        }

        List<DeathRecord> records = dm.getDeathRecords(targetUUID);
        if (deathIndex < 0 || deathIndex >= records.size()) {
            return false;
        }

        DeathRecord record = records.get(deathIndex);

        BackupData backupData = new BackupData(
                targetUUID, targetName,
                cloneArray(record.getInventory()),
                cloneArray(record.getArmor()),
                record.getOffHand() != null ? record.getOffHand().clone() : null,
                record.getDeathNumber()
        );

        dm.setPendingBackup(targetUUID, backupData);
        dm.updateDeathRecordStatus(targetUUID, deathIndex, DeathRecord.BackupStatus.PENDING);

        Player player = Bukkit.getPlayer(targetUUID);
        if (player != null && player.isOnline()) {
            player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacyAmpersand().deserialize("&7Masz &abackupa &7do odebrania pod &f/odbierzbackup&7!"));
        }

        return true;
    }

    public boolean receiveBackup(Player player) {
        DataManager dm = plugin.getDataManager();
        UUID uuid = player.getUniqueId();

        BackupData backup = dm.getPendingBackup(uuid);
        if (backup == null) return false;

        ItemStack[] inv = backup.getInventory();
        ItemStack[] armor = backup.getArmor();
        ItemStack offHand = backup.getOffHand();

        if (isInventoryEmpty(player)) {
            giveItemsExact(player, inv, armor, offHand);
        } else {
            giveItemsWithFallback(player, inv, armor, offHand);
        }

        updateDeathStatusToReceived(uuid, backup.getDeathNumber());
        dm.removePendingBackup(uuid);
        return true;
    }

    private void giveItemsExact(Player player, ItemStack[] inv, ItemStack[] armor, ItemStack offHand) {
        for (int i = 0; i < inv.length; i++) {
            if (inv[i] != null) {
                player.getInventory().setItem(i, inv[i].clone());
            }
        }

        if (armor[0] != null) player.getInventory().setBoots(armor[0].clone());
        if (armor[1] != null) player.getInventory().setLeggings(armor[1].clone());
        if (armor[2] != null) player.getInventory().setChestplate(armor[2].clone());
        if (armor[3] != null) player.getInventory().setHelmet(armor[3].clone());

        if (offHand != null) {
            player.getInventory().setItemInOffHand(offHand.clone());
        }
    }

    private void giveItemsWithFallback(Player player, ItemStack[] inv, ItemStack[] armor, ItemStack offHand) {
        giveArmorWithFallback(player, armor);

        if (offHand != null) {
            ItemStack currentOffhand = player.getInventory().getItemInOffHand();
            if (currentOffhand.getType().isAir()) {
                player.getInventory().setItemInOffHand(offHand.clone());
            } else {
                tryGiveOrDrop(player, offHand.clone());
            }
        }

        for (int i = 0; i < inv.length; i++) {
            if (inv[i] == null) continue;
            ItemStack current = player.getInventory().getItem(i);
            if (current == null || current.getType().isAir()) {
                player.getInventory().setItem(i, inv[i].clone());
            } else {
                tryGiveOrDrop(player, inv[i].clone());
            }
        }
    }

    private void giveArmorWithFallback(Player player, ItemStack[] armor) {
        if (armor[0] != null) {
            if (player.getInventory().getBoots() == null) {
                player.getInventory().setBoots(armor[0].clone());
            } else {
                tryGiveOrDrop(player, armor[0].clone());
            }
        }

        if (armor[1] != null) {
            if (player.getInventory().getLeggings() == null) {
                player.getInventory().setLeggings(armor[1].clone());
            } else {
                tryGiveOrDrop(player, armor[1].clone());
            }
        }

        if (armor[2] != null) {
            if (player.getInventory().getChestplate() == null) {
                player.getInventory().setChestplate(armor[2].clone());
            } else {
                tryGiveOrDrop(player, armor[2].clone());
            }
        }

        if (armor[3] != null) {
            if (player.getInventory().getHelmet() == null) {
                player.getInventory().setHelmet(armor[3].clone());
            } else {
                tryGiveOrDrop(player, armor[3].clone());
            }
        }
    }

    private void tryGiveOrDrop(Player player, ItemStack item) {
        int freeSlot = findFreeSlot(player);
        if (freeSlot != -1) {
            player.getInventory().setItem(freeSlot, item);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
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

        if (player.getInventory().getHelmet() != null) return false;
        if (player.getInventory().getChestplate() != null) return false;
        if (player.getInventory().getLeggings() != null) return false;
        if (player.getInventory().getBoots() != null) return false;

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (!offhand.getType().isAir()) return false;

        return true;
    }

    private void updateDeathStatusToReceived(UUID uuid, int deathNumber) {
        List<DeathRecord> records = plugin.getDataManager().getDeathRecords(uuid);
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

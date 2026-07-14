package pl.wisinia.backups.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.wisinia.backups.WisiniaBackups;
import pl.wisinia.backups.data.BackupData;
import pl.wisinia.backups.data.DeathRecord;

import java.util.ArrayList;
import java.util.List;

public class BackupReceiveGUI {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    /**
     * GUI 3 rzędy (27 slotów):
     * Środkowy slot całego GUI = slot 13 (barrel)
     * Slot 6 środkowego rzędu (rząd 2) = slot 6 + 9 = 15... 
     * Rząd 2 zaczyna się od 9, slot 6 rzędu 2 = 9+6 = 15
     * Slot 2 środkowego rzędu = 9+2 = 11
     */
    public static void open(WisiniaBackups plugin, Player player) {
        BackupData backup = plugin.getDataManager().getPendingBackup(player.getUniqueId());
        if (backup == null) {
            player.sendMessage(LEGACY.deserialize("&cNie masz żadnego backupa do odebrania!"));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, LEGACY.deserialize("&8Backup"));

        // Barrel na środku GUI (slot 13)
        ItemStack barrel = new ItemStack(Material.BARREL);
        ItemMeta barrelMeta = barrel.getItemMeta();
        barrelMeta.displayName(LEGACY.deserialize("&7Itemy gracza"));
        List<Component> barrelLore = new ArrayList<>();
        barrelLore.add(LEGACY.deserialize(" &8» &7Po kliknięciu zobaczysz"));
        barrelLore.add(LEGACY.deserialize(" &8» &fpreview &7backupa."));
        barrelLore.add(Component.empty());
        barrelLore.add(LEGACY.deserialize(" &8» &7Jeśli się on &cnie"));
        barrelLore.add(LEGACY.deserialize(" &8» &czgadza &7to skontaktuj"));
        barrelLore.add(LEGACY.deserialize(" &8» &7się z administracją oraz"));
        barrelLore.add(LEGACY.deserialize(" &8» &cnie odbieraj &7backupa!"));
        barrelMeta.lore(barrelLore);
        barrel.setItemMeta(barrelMeta);
        inv.setItem(13, barrel);

        // Lime Concrete - odbierz (slot 15 = rząd 2, pozycja 6)
        ItemStack limeConcrete = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta limeMeta = limeConcrete.getItemMeta();
        limeMeta.displayName(LEGACY.deserialize("&aOdbierz"));
        List<Component> limeLore = new ArrayList<>();
        limeLore.add(LEGACY.deserialize(" &8» &7Upewnij się że twój"));
        limeLore.add(LEGACY.deserialize(" &8» &7ekwipunek jest &apusty&7!"));
        limeLore.add(Component.empty());
        limeLore.add(LEGACY.deserialize(" &8» &7Jeśli posiadasz &apusty"));
        limeLore.add(LEGACY.deserialize(" &8» &fekwipunek &7to itemy wrócą"));
        limeLore.add(LEGACY.deserialize(" &8» &7dokładnie na swoje miejsce,"));
        limeLore.add(LEGACY.deserialize(" &8» &7Jeśli nie to &cwysypią się&7!"));
        limeMeta.lore(limeLore);
        limeConcrete.setItemMeta(limeMeta);
        inv.setItem(15, limeConcrete);

        // Red Concrete - anuluj (slot 11 = rząd 2, pozycja 2)
        ItemStack redConcrete = new ItemStack(Material.RED_CONCRETE);
        ItemMeta redMeta = redConcrete.getItemMeta();
        redMeta.displayName(LEGACY.deserialize("&cAnuluj"));
        List<Component> redLore = new ArrayList<>();
        redLore.add(LEGACY.deserialize(" &8» &7Możesz nie odbierać teraz"));
        redLore.add(LEGACY.deserialize(" &8» &7tego backupa, aby jeszcze"));
        redLore.add(LEGACY.deserialize(" &8» &7raz przejść do niego"));
        redLore.add(LEGACY.deserialize(" &8» &7wpisz &f/odbierzbackup"));
        redMeta.lore(redLore);
        redConcrete.setItemMeta(redMeta);
        inv.setItem(11, redConcrete);

        player.openInventory(inv);
    }

    public static void handleClick(WisiniaBackups plugin, Player player, int slot) {
        switch (slot) {
            case 13 -> {
                // Barrel - podgląd itemów
                openBackupPreview(plugin, player);
            }
            case 15 -> {
                // Lime Concrete - odbierz
                player.closeInventory();
                boolean success = plugin.getBackupManager().receiveBackup(player);
                if (success) {
                    player.sendMessage(LEGACY.deserialize("&aBackup został odebrany pomyślnie!"));
                } else {
                    player.sendMessage(LEGACY.deserialize("&cNie masz żadnego backupa do odebrania!"));
                }
            }
            case 11 -> {
                // Red Concrete - anuluj
                player.closeInventory();
            }
        }
    }

    private static void openBackupPreview(WisiniaBackups plugin, Player player) {
        BackupData backup = plugin.getDataManager().getPendingBackup(player.getUniqueId());
        if (backup == null) return;

        // Znajdź odpowiedni DeathRecord dla tego backupa
        List<DeathRecord> records = plugin.getDataManager().getDeathRecords(player.getUniqueId());
        DeathRecord targetRecord = null;
        for (DeathRecord record : records) {
            if (record.getDeathNumber() == backup.getDeathNumber()) {
                targetRecord = record;
                break;
            }
        }

        if (targetRecord != null) {
            DeathPreviewGUI.openForPlayer(plugin, player, targetRecord);
        } else {
            // Utwórz tymczasowy record z danych backupa
            DeathRecord tempRecord = new DeathRecord(
                    backup.getPlayerUUID(),
                    backup.getPlayerName(),
                    DeathRecord.DeathType.SUICIDE,
                    java.time.LocalDateTime.now(),
                    null, null,
                    backup.getInventory(),
                    backup.getArmor(),
                    backup.getOffHand(),
                    backup.getDeathNumber()
            );
            DeathPreviewGUI.openForPlayer(plugin, player, tempRecord);
        }
    }
}

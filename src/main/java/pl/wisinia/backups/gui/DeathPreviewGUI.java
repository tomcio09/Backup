package pl.wisinia.backups.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.wisinia.backups.WisiniaBackups;
import pl.wisinia.backups.data.DeathRecord;

import java.util.*;

public class DeathPreviewGUI {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private static final Map<UUID, DeathRecord> previewCache = new HashMap<>();
    private static final Map<UUID, Integer> previewIndexCache = new HashMap<>();
    private static final Map<UUID, String> previewTargetCache = new HashMap<>();

    public static void open(WisiniaBackups plugin, Player admin, DeathRecord record, int deathIndex, String targetName) {
        Inventory inv = Bukkit.createInventory(null, 54,
                text("&8Śmierć #" + record.getDeathNumber()));

        // Rząd 1: Zbroja + offhand
        setOrEmpty(inv, 0, record.getArmor()[3]); // Helmet
        setOrEmpty(inv, 1, record.getArmor()[2]); // Chestplate
        setOrEmpty(inv, 2, record.getArmor()[1]); // Leggings
        setOrEmpty(inv, 3, record.getArmor()[0]); // Boots
        setOrEmpty(inv, 5, record.getOffHand());  // Offhand

        // Rzędy 2-4: inventory sloty 9-35
        for (int i = 9; i <= 35; i++) {
            ItemStack item = record.getInventory()[i];
            if (item != null) {
                inv.setItem(i, item.clone());
            }
        }

        // Rząd 5: Hotbar (inventory sloty 0-8) -> GUI sloty 36-44
        for (int i = 0; i < 9; i++) {
            ItemStack item = record.getInventory()[i];
            if (item != null) {
                inv.setItem(36 + i, item.clone());
            }
        }

        // Barrier - powrót slot 49
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta barrierMeta = barrier.getItemMeta();
        barrierMeta.displayName(text("&cPowrót"));
        barrier.setItemMeta(barrierMeta);
        inv.setItem(49, barrier);

        // Lime Dye - nadaj backup slot 53
        ItemStack limeDye = new ItemStack(Material.LIME_DYE);
        ItemMeta limeMeta = limeDye.getItemMeta();
        limeMeta.displayName(text("&aNadaj backupa"));
        List<Component> limeLore = new ArrayList<>();
        limeLore.add(text(" &8» &7Kliknij aby nadać"));
        limeLore.add(text(" &8» &abackupa &7graczowi!"));
        limeMeta.lore(limeLore);
        limeDye.setItemMeta(limeMeta);
        inv.setItem(53, limeDye);

        previewCache.put(admin.getUniqueId(), record);
        previewIndexCache.put(admin.getUniqueId(), deathIndex);
        previewTargetCache.put(admin.getUniqueId(), targetName);

        admin.openInventory(inv);
    }

    private static void setOrEmpty(Inventory inv, int slot, ItemStack item) {
        if (item != null) {
            inv.setItem(slot, item.clone());
        }
    }

    public static void handleClick(WisiniaBackups plugin, Player admin, int slot, ItemStack clickedItem, String title) {
        DeathRecord record = previewCache.get(admin.getUniqueId());
        String targetName = previewTargetCache.get(admin.getUniqueId());
        int deathIndex = previewIndexCache.getOrDefault(admin.getUniqueId(), -1);

        if (record == null || targetName == null) return;

        // Powrót - slot 49
        if (slot == 49 && clickedItem.getType() == Material.BARRIER) {
            DeathListGUI.open(plugin, admin, targetName);
            return;
        }

        // Nadaj backup - slot 53
        if (slot == 53 && clickedItem.getType() == Material.LIME_DYE) {
            UUID targetUUID = DeathListGUI.getUUIDByName(targetName);
            if (targetUUID == null) {
                admin.sendMessage(LEGACY.deserialize("&cNie znaleziono gracza!"));
                return;
            }

            if (plugin.getDataManager().hasPendingBackup(targetUUID)) {
                admin.sendMessage(LEGACY.deserialize("&7Ten gracz ma już &abackupa &7do oderania!"));
                return;
            }

            boolean success = plugin.getBackupManager().giveBackup(targetUUID, targetName, deathIndex);
            if (success) {
                admin.sendMessage(LEGACY.deserialize("&7Backup został &anadany &7graczowi &f" + targetName + "&7!"));
                DeathListGUI.open(plugin, admin, targetName);
            } else {
                admin.sendMessage(LEGACY.deserialize("&7Ten gracz ma już &abackupa &7do oderania!"));
            }
            return;
        }

        // Kliknięcie w item - kopiuj do admina
        if (slot != 49 && slot != 53) {
            if (clickedItem != null && !clickedItem.getType().isAir()) {
                if (admin.getInventory().firstEmpty() == -1) {
                    admin.sendMessage(LEGACY.deserialize("&cNie masz miejsca w ekwipunku!"));
                    return;
                }
                admin.getInventory().addItem(clickedItem.clone());
            }
        }
    }

    public static void openForPlayer(WisiniaBackups plugin, Player player, DeathRecord record) {
        Inventory inv = Bukkit.createInventory(null, 54,
                text("&8Backup podgląd #" + record.getDeathNumber()));

        setOrEmpty(inv, 0, record.getArmor()[3]);
        setOrEmpty(inv, 1, record.getArmor()[2]);
        setOrEmpty(inv, 2, record.getArmor()[1]);
        setOrEmpty(inv, 3, record.getArmor()[0]);
        setOrEmpty(inv, 5, record.getOffHand());

        for (int i = 9; i <= 35; i++) {
            ItemStack item = record.getInventory()[i];
            if (item != null) {
                inv.setItem(i, item.clone());
            }
        }

        for (int i = 0; i < 9; i++) {
            ItemStack item = record.getInventory()[i];
            if (item != null) {
                inv.setItem(36 + i, item.clone());
            }
        }

        player.openInventory(inv);
    }

    /**
     * Tworzy komponent z wyłączoną kursywą.
     * Rodzic Component.empty() z ITALIC=false, dziecko dziedziczy.
     */
    private static Component text(String legacyText) {
        if (legacyText.isEmpty()) {
            return Component.empty().decoration(TextDecoration.ITALIC, false);
        }
        return Component.empty()
                .decoration(TextDecoration.ITALIC, false)
                .append(LEGACY.deserialize(legacyText));
    }
}

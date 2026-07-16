package pl.wisinia.backups.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.wisinia.backups.WisiniaBackups;
import pl.wisinia.backups.data.DeathRecord;

import java.time.LocalDateTime;
import java.util.*;

public class DeathListGUI {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private static final Map<UUID, String> adminTargetCache = new HashMap<>();
    private static final Map<UUID, Integer> deathIndexCache = new HashMap<>();

    public static void open(WisiniaBackups plugin, Player admin, String targetName) {
        UUID targetUUID = plugin.getDataManager().getUUIDByName(targetName);

        List<DeathRecord> records;
        if (targetUUID != null) {
            records = plugin.getDataManager().getDeathRecords(targetUUID);
        } else {
            records = new ArrayList<>();
        }

        Inventory inv = Bukkit.createInventory(null, 54,
                text("&8Śmierci " + targetName));

        int max = Math.min(records.size(), 54);
        for (int i = 0; i < max; i++) {
            inv.setItem(i, createDeathBanner(records.get(i)));
        }

        adminTargetCache.put(admin.getUniqueId(), targetName);
        admin.openInventory(inv);
    }

    private static ItemStack createDeathBanner(DeathRecord record) {
        String rodzajText;

        switch (record.getDeathType()) {
            case SUICIDE -> rodzajText = "&cSamobójstwo";
            case KILL -> rodzajText = "&5Zabójstwo";
            case LOGOUT -> rodzajText = "&4Wylogowanie się";
            default -> rodzajText = "&7Nieznany";
        }

        ItemStack banner = new ItemStack(Material.FLOWER_BANNER_PATTERN);
        ItemMeta meta = banner.getItemMeta();
        if (meta == null) return banner;

        String dataFormatted = formatDate(record.getDeathTime());

        meta.displayName(text(rodzajText + " &7" + dataFormatted));

        List<Component> lore = new ArrayList<>();
        lore.add(text(" &8» &fRodzaj: " + rodzajText));
        lore.add(text(" &8» &fData: &7" + dataFormatted));

        if ((record.getDeathType() == DeathRecord.DeathType.KILL ||
                record.getDeathType() == DeathRecord.DeathType.LOGOUT)
                && record.getKillerUUID() != null) {

            String killerDisplay = getKillerDisplay(record.getKillerUUID(), record.getKillerName());
            lore.add(text(" &8» &fZabójca: &c" + killerDisplay));
        }

        String statusText = switch (record.getBackupStatus()) {
            case RECEIVED -> "&aOdebrany";
            case PENDING -> "&6Oczekujący";
            case NONE -> "&cBrak";
        };

        lore.add(text(" &8» &fStatus: " + statusText));
        lore.add(text(""));
        lore.add(text(" &8» &7Naciśnij, aby przejść"));
        lore.add(text(" &8» &7do &cśmierci&7!"));

        meta.lore(lore);
        banner.setItemMeta(meta);
        return banner;
    }

    private static String getKillerDisplay(UUID killerUUID, String killerName) {
        if (killerName == null || killerName.isEmpty()) {
            killerName = "Nieznany";
        }

        try {
            OfflinePlayer op = Bukkit.getOfflinePlayer(killerUUID);
            if (op.isBanned()) {
                return "Banned-&f" + killerName;
            }
        } catch (Exception ignored) {}

        return killerName;
    }

    public static String formatDate(LocalDateTime time) {
        String[] polishMonths = {
                "stycznia", "lutego", "marca", "kwietnia", "maja", "czerwca",
                "lipca", "sierpnia", "września", "października", "listopada", "grudnia"
        };

        return String.format("%02d:%02d %d %s",
                time.getHour(), time.getMinute(),
                time.getDayOfMonth(),
                polishMonths[time.getMonthValue() - 1]);
    }

    public static void handleClick(WisiniaBackups plugin, Player admin, int slot, String title) {
        String targetName = adminTargetCache.get(admin.getUniqueId());
        if (targetName == null) {
            String rawTitle = title.replace("§8", "");
            if (rawTitle.startsWith("Śmierci ")) {
                targetName = rawTitle.substring("Śmierci ".length());
            } else {
                return;
            }
        }

        UUID targetUUID = plugin.getDataManager().getUUIDByName(targetName);
        if (targetUUID == null) return;

        List<DeathRecord> records = plugin.getDataManager().getDeathRecords(targetUUID);
        if (slot < 0 || slot >= records.size()) return;

        deathIndexCache.put(admin.getUniqueId(), slot);
        DeathPreviewGUI.open(plugin, admin, records.get(slot), slot, targetName);
    }

    public static String getTargetName(UUID adminUUID) {
        return adminTargetCache.get(adminUUID);
    }

    public static int getDeathIndex(UUID adminUUID) {
        return deathIndexCache.getOrDefault(adminUUID, -1);
    }

    /**
     * Tworzy komponent z wyłączoną kursywą
     */
    public static Component text(String legacyText) {
        if (legacyText == null || legacyText.isEmpty()) {
            return Component.empty().decoration(TextDecoration.ITALIC, false);
        }
        return Component.empty()
                .decoration(TextDecoration.ITALIC, false)
                .append(LEGACY.deserialize(legacyText));
    }
}

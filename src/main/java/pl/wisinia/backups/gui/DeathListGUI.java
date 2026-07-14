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
        List<DeathRecord> records = new ArrayList<>();

        UUID targetUUID = getUUIDByName(targetName);

        if (targetUUID != null) {
            records = plugin.getDataManager().getDeathRecords(targetUUID);
        }

        Inventory inv = Bukkit.createInventory(null, 54,
                text("&8Śmierci " + targetName));

        for (int i = 0; i < Math.min(records.size(), 54); i++) {
            DeathRecord record = records.get(i);
            ItemStack banner = createDeathBanner(record);
            inv.setItem(i, banner);
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

        int hour = time.getHour();
        int minute = time.getMinute();
        int day = time.getDayOfMonth();
        String month = polishMonths[time.getMonthValue() - 1];

        return String.format("%02d:%02d %d %s", hour, minute, day, month);
    }

    public static void handleClick(WisiniaBackups plugin, Player admin, int slot, String title) {
        String targetName = adminTargetCache.getOrDefault(admin.getUniqueId(), null);
        if (targetName == null) {
            String rawTitle = title.replace("§8", "");
            if (rawTitle.startsWith("Śmierci ")) {
                targetName = rawTitle.substring("Śmierci ".length());
            } else {
                return;
            }
        }

        UUID targetUUID = getUUIDByName(targetName);
        if (targetUUID == null) return;

        List<DeathRecord> records = plugin.getDataManager().getDeathRecords(targetUUID);
        if (slot < 0 || slot >= records.size()) return;

        DeathRecord record = records.get(slot);
        deathIndexCache.put(admin.getUniqueId(), slot);
        DeathPreviewGUI.open(plugin, admin, record, slot, targetName);
    }

    public static String getTargetName(UUID adminUUID) {
        return adminTargetCache.get(adminUUID);
    }

    public static int getDeathIndex(UUID adminUUID) {
        return deathIndexCache.getOrDefault(adminUUID, -1);
    }

    public static UUID getUUIDByName(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();

        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            if (op.getName() != null && name.equalsIgnoreCase(op.getName())) {
                return op.getUniqueId();
            }
        }
        return null;
    }

    /**
     * Tworzy komponent z wyłączoną kursywą.
     * Opakowuje w rodzica z ITALIC=false, dzieci dziedziczą.
     */
    public static Component text(String legacyText) {
        if (legacyText.isEmpty()) {
            return Component.empty().decoration(TextDecoration.ITALIC, false);
        }
        return Component.empty()
                .decoration(TextDecoration.ITALIC, false)
                .append(LEGACY.deserialize(legacyText));
    }
}

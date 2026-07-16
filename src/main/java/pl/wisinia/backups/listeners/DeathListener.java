package pl.wisinia.backups.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import pl.wisinia.backups.WisiniaBackups;
import pl.wisinia.backups.api.AntylogoutAPI;
import pl.wisinia.backups.data.DeathRecord;
import pl.wisinia.backups.managers.DataManager;

import java.time.LocalDateTime;
import java.util.UUID;

public class DeathListener implements Listener {

    private final WisiniaBackups plugin;

    public DeathListener(WisiniaBackups plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();
        DataManager dm = plugin.getDataManager();

        // Zapisz ekwipunek - szybkie klonowanie
        ItemStack[] inv = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            inv[i] = item != null ? item.clone() : null;
        }

        ItemStack[] armor = new ItemStack[4];
        ItemStack boots = player.getInventory().getBoots();
        ItemStack legs = player.getInventory().getLeggings();
        ItemStack chest = player.getInventory().getChestplate();
        ItemStack helmet = player.getInventory().getHelmet();
        armor[0] = boots != null ? boots.clone() : null;
        armor[1] = legs != null ? legs.clone() : null;
        armor[2] = chest != null ? chest.clone() : null;
        armor[3] = helmet != null ? helmet.clone() : null;

        ItemStack offHand = player.getInventory().getItemInOffHand();
        ItemStack offHandClone = (!offHand.getType().isAir()) ? offHand.clone() : null;

        // Ustal typ śmierci
        DeathRecord.DeathType deathType;
        UUID killerUUID = null;
        String killerName = null;

        // Sprawdź AntylogoutAPI
        AntylogoutAPI.DeathType antylogoutType = AntylogoutAPI.getDeathType(uuid);

        if (antylogoutType == AntylogoutAPI.DeathType.LOGOUT_DURING_COMBAT) {
            deathType = DeathRecord.DeathType.LOGOUT;
            killerUUID = AntylogoutAPI.getKillerOf(uuid);
            if (killerUUID != null) {
                killerName = getPlayerName(killerUUID);
            }
        } else if (antylogoutType == AntylogoutAPI.DeathType.COMBAT_DEATH) {
            deathType = DeathRecord.DeathType.KILL;
            killerUUID = AntylogoutAPI.getKillerOf(uuid);
            if (killerUUID != null) {
                killerName = getPlayerName(killerUUID);
            } else if (event.getEntity().getKiller() != null) {
                killerUUID = event.getEntity().getKiller().getUniqueId();
                killerName = event.getEntity().getKiller().getName();
            }
        } else {
            if (event.getEntity().getKiller() != null) {
                deathType = DeathRecord.DeathType.KILL;
                killerUUID = event.getEntity().getKiller().getUniqueId();
                killerName = event.getEntity().getKiller().getName();
            } else {
                deathType = DeathRecord.DeathType.SUICIDE;
            }
        }

        int deathNumber = dm.getNextDeathNumber(uuid);
        LocalDateTime now = LocalDateTime.now();

        // Cache nick gracza
        dm.cachePlayer(playerName, uuid);
        if (killerUUID != null && killerName != null) {
            dm.cachePlayer(killerName, killerUUID);
        }

        DeathRecord record = new DeathRecord(
                uuid, playerName, deathType, now,
                killerUUID, killerName,
                inv, armor, offHandClone, deathNumber
        );

        // Dodanie rekordu (zapis async w środku)
        dm.addDeathRecord(record);
    }

    private String getPlayerName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();

        // Szybki lookup bez iterowania
        org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);
    }
}

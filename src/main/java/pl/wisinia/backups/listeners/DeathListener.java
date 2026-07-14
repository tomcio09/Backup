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
        DataManager dm = plugin.getDataManager();

        // Zapisz ekwipunek PRZED śmiercią (itemy są dostępne w inventory podczas death event)
        ItemStack[] inv = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            inv[i] = item != null ? item.clone() : null;
        }

        ItemStack[] armor = new ItemStack[4];
        armor[0] = player.getInventory().getBoots() != null ? player.getInventory().getBoots().clone() : null;
        armor[1] = player.getInventory().getLeggings() != null ? player.getInventory().getLeggings().clone() : null;
        armor[2] = player.getInventory().getChestplate() != null ? player.getInventory().getChestplate().clone() : null;
        armor[3] = player.getInventory().getHelmet() != null ? player.getInventory().getHelmet().clone() : null;

        ItemStack offHand = player.getInventory().getItemInOffHand();
        ItemStack offHandClone = (offHand != null && !offHand.getType().isAir()) ? offHand.clone() : null;

        // Ustal typ śmierci
        DeathRecord.DeathType deathType;
        UUID killerUUID = null;
        String killerName = null;

        AntylogoutAPI.DeathType antylogoutType = AntylogoutAPI.getDeathType(uuid);

        if (antylogoutType == AntylogoutAPI.DeathType.LOGOUT_DURING_COMBAT) {
            deathType = DeathRecord.DeathType.LOGOUT;
            killerUUID = AntylogoutAPI.getKillerOf(uuid);
            if (killerUUID != null) {
                Player killer = Bukkit.getPlayer(killerUUID);
                killerName = killer != null ? killer.getName() : getOfflinePlayerName(killerUUID);
            }
        } else if (antylogoutType == AntylogoutAPI.DeathType.COMBAT_DEATH) {
            deathType = DeathRecord.DeathType.KILL;
            killerUUID = AntylogoutAPI.getKillerOf(uuid);
            if (killerUUID != null) {
                Player killer = Bukkit.getPlayer(killerUUID);
                killerName = killer != null ? killer.getName() : getOfflinePlayerName(killerUUID);
            } else if (event.getEntity().getKiller() != null) {
                killerUUID = event.getEntity().getKiller().getUniqueId();
                killerName = event.getEntity().getKiller().getName();
            }
        } else {
            // NORMAL - sprawdź czy był killer
            if (event.getEntity().getKiller() != null) {
                deathType = DeathRecord.DeathType.KILL;
                killerUUID = event.getEntity().getKiller().getUniqueId();
                killerName = event.getEntity().getKiller().getName();
            } else {
                deathType = DeathRecord.DeathType.SUICIDE;
            }
        }

        int deathNumber = dm.getNextDeathNumber(uuid);

        DeathRecord record = new DeathRecord(
                uuid,
                player.getName(),
                deathType,
                LocalDateTime.now(),
                killerUUID,
                killerName,
                inv,
                armor,
                offHandClone,
                deathNumber
        );

        dm.addDeathRecord(record);
    }

    private String getOfflinePlayerName(UUID uuid) {
        org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);
    }
}

package pl.wisinia.backups.listeners;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import pl.wisinia.backups.WisiniaBackups;

public class PlayerJoinListener implements Listener {

    private final WisiniaBackups plugin;

    public PlayerJoinListener(WisiniaBackups plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (plugin.getDataManager().hasPendingBackup(player.getUniqueId())) {
            // Opóźnij powiadomienie o 2 sekundy po zalogowaniu
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        // Subtitle jako action bar + title
                        player.sendTitle(
                                "",
                                LegacyComponentSerializer.legacyAmpersand()
                                        .deserialize("&7Masz &abackupa &7do odebrania pod &f/odbierzbackup&7!")
                                        .content(),
                                10, 80, 20
                        );

                        // Wyślij również na czat dla pewności
                        player.sendMessage(LegacyComponentSerializer.legacyAmpersand()
                                .deserialize("&7Masz &abackupa &7do odebrania pod &f/odbierzbackup&7!"));
                    }
                }
            }.runTaskLater(plugin, 40L);
        }
    }
}

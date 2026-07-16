package pl.wisinia.backups.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import pl.wisinia.backups.WisiniaBackups;

import java.time.Duration;

public class PlayerJoinListener implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final WisiniaBackups plugin;

    public PlayerJoinListener(WisiniaBackups plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Cache nick -> UUID
        plugin.getDataManager().cachePlayer(player.getName(), player.getUniqueId());

        if (plugin.getDataManager().hasPendingBackup(player.getUniqueId())) {
            // Opóźnij powiadomienie o 3 sekundy
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;

                Component subtitle = LEGACY.deserialize(
                        "&7Masz &abackupa &7do odebrania pod &f/odbierzbackup&7!"
                ).decoration(TextDecoration.ITALIC, false);

                Title.Times times = Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofSeconds(4),
                        Duration.ofMillis(1000)
                );

                player.showTitle(Title.title(Component.empty(), subtitle, times));
            }, 60L);
        }
    }
}

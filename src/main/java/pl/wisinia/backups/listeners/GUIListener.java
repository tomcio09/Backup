package pl.wisinia.backups.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import pl.wisinia.backups.WisiniaBackups;
import pl.wisinia.backups.gui.BackupReceiveGUI;
import pl.wisinia.backups.gui.DeathListGUI;
import pl.wisinia.backups.gui.DeathPreviewGUI;

public class GUIListener implements Listener {

    private final WisiniaBackups plugin;

    public GUIListener(WisiniaBackups plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory inv = event.getInventory();
        String title = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().serialize(event.getView().title());

        // Sprawdź czy to nasze GUI
        if (title.startsWith("§8Śmierci ")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            DeathListGUI.handleClick(plugin, player, event.getSlot(), title);
            return;
        }

        if (title.startsWith("§8Śmierć #")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            DeathPreviewGUI.handleClick(plugin, player, event.getSlot(), event.getCurrentItem(), title);
            return;
        }

        if (title.equals("§8Backup")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            BackupReceiveGUI.handleClick(plugin, player, event.getSlot());
            return;
        }

        if (title.startsWith("§8Backup podgląd")) {
            event.setCancelled(true);
        }
    }
}

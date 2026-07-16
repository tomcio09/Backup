package pl.wisinia.backups.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import pl.wisinia.backups.WisiniaBackups;
import pl.wisinia.backups.gui.BackupReceiveGUI;
import pl.wisinia.backups.gui.DeathListGUI;
import pl.wisinia.backups.gui.DeathPreviewGUI;

public class GUIListener implements Listener {

    private final WisiniaBackups plugin;

    public GUIListener(WisiniaBackups plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().serialize(event.getView().title());

        // Sprawdź czy to nasze GUI
        if (title.startsWith("§8Śmierci ")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;
            if (event.getClickedInventory() != event.getView().getTopInventory()) return;
            DeathListGUI.handleClick(plugin, player, event.getSlot(), title);
            return;
        }

        if (title.startsWith("§8Śmierć #")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;
            if (event.getClickedInventory() != event.getView().getTopInventory()) return;
            DeathPreviewGUI.handleClick(plugin, player, event.getSlot(), event.getCurrentItem(), title);
            return;
        }

        if (title.equals("§8Backup")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;
            if (event.getClickedInventory() != event.getView().getTopInventory()) return;
            BackupReceiveGUI.handleClick(plugin, player, event.getSlot());
            return;
        }

        if (title.startsWith("§8Backup podgląd")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().serialize(event.getView().title());

        if (title.startsWith("§8Śmierci ") ||
                title.startsWith("§8Śmierć #") ||
                title.equals("§8Backup") ||
                title.startsWith("§8Backup podgląd")) {
            event.setCancelled(true);
        }
    }
}

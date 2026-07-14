package pl.wisinia.backups.commands;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.wisinia.backups.WisiniaBackups;
import pl.wisinia.backups.gui.BackupReceiveGUI;

public class OdbierzBackupCommand implements CommandExecutor {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final WisiniaBackups plugin;

    public OdbierzBackupCommand(WisiniaBackups plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LEGACY.deserialize("&cTylko gracz może użyć tej komendy!"));
            return true;
        }

        if (!plugin.getDataManager().hasPendingBackup(player.getUniqueId())) {
            player.sendMessage(LEGACY.deserialize("&cNie masz żadnego backupa do odebrania!"));
            return true;
        }

        BackupReceiveGUI.open(plugin, player);
        return true;
    }
}

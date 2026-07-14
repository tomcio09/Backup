package pl.wisinia.backups.commands;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.wisinia.backups.WisiniaBackups;
import pl.wisinia.backups.gui.DeathListGUI;

public class BackupCommand implements CommandExecutor {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final WisiniaBackups plugin;

    public BackupCommand(WisiniaBackups plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player admin)) {
            sender.sendMessage(LEGACY.deserialize("&cTylko gracz może użyć tej komendy!"));
            return true;
        }

        if (!admin.hasPermission("backup.admin")) {
            admin.sendMessage(LEGACY.deserialize("&cNie masz uprawnień do tej komendy!"));
            return true;
        }

        if (args.length < 1) {
            admin.sendMessage(LEGACY.deserialize("&cUżycie: &f/backup <nick>"));
            return true;
        }

        String targetName = args[0];
        DeathListGUI.open(plugin, admin, targetName);
        return true;
    }
}

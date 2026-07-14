package pl.wisinia.backups;

import org.bukkit.plugin.java.JavaPlugin;
import pl.wisinia.backups.commands.BackupCommand;
import pl.wisinia.backups.commands.OdbierzBackupCommand;
import pl.wisinia.backups.listeners.DeathListener;
import pl.wisinia.backups.listeners.GUIListener;
import pl.wisinia.backups.listeners.PlayerJoinListener;
import pl.wisinia.backups.managers.BackupManager;
import pl.wisinia.backups.managers.DataManager;

public class WisiniaBackups extends JavaPlugin {

    private static WisiniaBackups instance;
    private DataManager dataManager;
    private BackupManager backupManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.dataManager = new DataManager(this);
        this.backupManager = new BackupManager(this);

        dataManager.load();

        getCommand("backup").setExecutor(new BackupCommand(this));
        getCommand("odbierzbackup").setExecutor(new OdbierzBackupCommand(this));

        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        boolean antylogoutAvailable = getServer().getPluginManager().getPlugin("AnacodeAntylogout") != null;
        getLogger().info("AnacodeAntylogout: " + (antylogoutAvailable ? "dostępny" : "niedostępny"));
        getLogger().info("WisiniaBackups został włączony!");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.save();
        }
        getLogger().info("WisiniaBackups został wyłączony!");
    }

    public static WisiniaBackups getInstance() {
        return instance;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public BackupManager getBackupManager() {
        return backupManager;
    }
}

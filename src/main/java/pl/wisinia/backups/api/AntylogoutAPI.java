package pl.wisinia.backups.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.wisinia.backups.WisiniaBackups;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Wrapper dla AnacodeAntylogout API
 * Używa refleksji aby uniknąć błędów gdy plugin nie jest dostępny
 */
public class AntylogoutAPI {

    public enum DeathType {
        LOGOUT_DURING_COMBAT,
        COMBAT_DEATH,
        NORMAL
    }

    private static boolean enabled = false;
    private static Class<?> apiClass = null;

    static {
        try {
            apiClass = Class.forName("pl.anacode.antylogout.api.AntylogoutAPI");
            enabled = true;
        } catch (ClassNotFoundException e) {
            WisiniaBackups.getInstance().getLogger().warning(
                    "AnacodeAntylogout nie jest dostępny - typy śmierci będą ograniczone."
            );
        }
    }

    public static boolean isEnabled() {
        return enabled && Bukkit.getPluginManager().getPlugin("AnacodeAntylogout") != null;
    }

    public static DeathType getDeathType(UUID uuid) {
        if (!isEnabled() || apiClass == null) return DeathType.NORMAL;
        try {
            Method method = apiClass.getMethod("getDeathType", UUID.class);
            Object result = method.invoke(null, uuid);
            if (result == null) return DeathType.NORMAL;
            String name = result.toString();
            return switch (name) {
                case "LOGOUT_DURING_COMBAT" -> DeathType.LOGOUT_DURING_COMBAT;
                case "COMBAT_DEATH" -> DeathType.COMBAT_DEATH;
                default -> DeathType.NORMAL;
            };
        } catch (Exception e) {
            return DeathType.NORMAL;
        }
    }

    public static UUID getKillerOf(UUID uuid) {
        if (!isEnabled() || apiClass == null) return null;
        try {
            Method method = apiClass.getMethod("getKillerOf", UUID.class);
            Object result = method.invoke(null, uuid);
            return (UUID) result;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isPlayerTagged(Player player) {
        if (!isEnabled() || apiClass == null) return false;
        try {
            Method method = apiClass.getMethod("isPlayerTagged", Player.class);
            Object result = method.invoke(null, player);
            return (boolean) result;
        } catch (Exception e) {
            return false;
        }
    }
}

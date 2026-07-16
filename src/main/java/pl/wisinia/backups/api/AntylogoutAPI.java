package pl.wisinia.backups.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Wrapper dla AnacodeAntylogout API
 * Używa refleksji - bezpieczny gdy plugin nie jest dostępny
 */
public class AntylogoutAPI {

    public enum DeathType {
        LOGOUT_DURING_COMBAT,
        COMBAT_DEATH,
        NORMAL
    }

    private static boolean available = false;
    private static Class<?> apiClass = null;
    private static Method getDeathTypeMethod = null;
    private static Method getKillerOfMethod = null;
    private static Method isPlayerTaggedMethod = null;

    // Cache metod refleksji - wywoływane raz
    static {
        try {
            if (Bukkit.getPluginManager().getPlugin("AnacodeAntylogout") != null) {
                apiClass = Class.forName("pl.anacode.antylogout.api.AntylogoutAPI");
                getDeathTypeMethod = apiClass.getMethod("getDeathType", UUID.class);
                getKillerOfMethod = apiClass.getMethod("getKillerOf", UUID.class);
                isPlayerTaggedMethod = apiClass.getMethod("isPlayerTagged", Player.class);
                available = true;
            }
        } catch (Exception e) {
            available = false;
        }
    }

    public static boolean isEnabled() {
        return available;
    }

    public static DeathType getDeathType(UUID uuid) {
        if (!available || getDeathTypeMethod == null) return DeathType.NORMAL;
        try {
            Object result = getDeathTypeMethod.invoke(null, uuid);
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
        if (!available || getKillerOfMethod == null) return null;
        try {
            return (UUID) getKillerOfMethod.invoke(null, uuid);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isPlayerTagged(Player player) {
        if (!available || isPlayerTaggedMethod == null) return false;
        try {
            return (boolean) isPlayerTaggedMethod.invoke(null, player);
        } catch (Exception e) {
            return false;
        }
    }
}

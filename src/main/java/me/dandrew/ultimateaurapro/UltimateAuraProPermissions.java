package me.dandrew.ultimateaurapro;

import me.dandrew.ultimateaurapro.config.InstalledAura;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

public class UltimateAuraProPermissions {

    // General
    private static final String BASE_NODE = "uap";
    private static final String ALMIGHTY_NODE = BASE_NODE + ".*";

    // Admin
    public static final String ADMIN_NODE = BASE_NODE + ".admin";

    // Auras
    public static final String AURA_NODE = BASE_NODE + ".aura";
    private static final String AURA_NODE_ALL = AURA_NODE + ".*";

    public static boolean check(CommandSender commandSender, InstalledAura installedAura) {
        return commandSender.hasPermission(AURA_NODE_ALL) || check(commandSender, installedAura.getPermission());
    }

    public static boolean check(CommandSender commandSender, String permission) {
        return commandSender.hasPermission(ALMIGHTY_NODE)
                || commandSender.hasPermission(ADMIN_NODE)
                || commandSender.hasPermission(permission)
                || commandSender instanceof ConsoleCommandSender;
    }

}

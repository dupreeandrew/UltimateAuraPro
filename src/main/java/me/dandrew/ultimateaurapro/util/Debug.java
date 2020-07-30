package me.dandrew.ultimateaurapro.util;

import me.dandrew.ultimateaurapro.UltimateAuraProPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class Debug {

    private static boolean enabled = true;

    public static void send(String msg) {


        if (!enabled) {
            return;
        }


        Bukkit.getScheduler().scheduleSyncDelayedTask(UltimateAuraProPlugin.plugin, () -> {
            Player player = Bukkit.getPlayer("iSayCr4pAlot");
            if (player != null) {
                player.sendMessage(ChatColor.GRAY + msg);
            }
        });





    }
}

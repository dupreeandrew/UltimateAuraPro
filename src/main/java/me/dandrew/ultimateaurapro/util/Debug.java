package me.dandrew.ultimateaurapro.util;

public class Debug {

    private static boolean enabled = true;

    public static void send(String msg) {

        if (!enabled) {
            return;
        }


        /*
        Bukkit.getScheduler().scheduleSyncDelayedTask(UltimateAuraProPlugin.plugin, () -> {
            Player player = Bukkit.getPlayer("iSayCr4pAlot");
            if (player != null) {
                player.sendMessage(ChatColor.GRAY + msg);
            }
        });
        */

    }
}

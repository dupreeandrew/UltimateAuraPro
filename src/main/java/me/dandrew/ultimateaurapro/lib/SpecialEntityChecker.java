package me.dandrew.ultimateaurapro.lib;

import me.dandrew.ultimateaurapro.UltimateAuraProPlugin;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Entity;

public class SpecialEntityChecker {

    private static boolean didCheckCitizenApiAlready = false;
    private static boolean citizenApiInstalled = false;

    public static boolean checkGeneralNPC(Entity entity) {
        return entity.hasMetadata("NPC");
    }

    public static boolean checkInvulnerableCitizenNPC(Entity entity) {

        if (!didCheckCitizenApiAlready) {
            didCheckCitizenApiAlready = true;
            citizenApiInstalled = UltimateAuraProPlugin.softDependIsInstalled("Citizens");
        }

        if (!citizenApiInstalled) {
            return false;
        }

        if (!CitizensAPI.getNPCRegistry().isNPC(entity)) {
            return false;
        }

        NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
        return npc.data().get(NPC.DEFAULT_PROTECTED_METADATA);

    }

    public static boolean checkShopkeeper(Entity entity) {
        return entity.hasMetadata("shopkeeper");
    }

    public static boolean checkShouldIgnore(Entity entity) {
        return entity.isInvulnerable() || checkInvulnerableCitizenNPC(entity) || checkShopkeeper(entity);
    }

}

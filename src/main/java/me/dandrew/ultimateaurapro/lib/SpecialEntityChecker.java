package me.dandrew.ultimateaurapro.lib;

import me.dandrew.ultimateaurapro.UltimateAuraProPlugin;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Entity;

public class SpecialEntityChecker {

    public static boolean checkGeneralNPC(Entity entity) {
        return entity.hasMetadata("NPC");
    }

    public static boolean checkInvulnerableCitizenNPC(Entity entity) {

        boolean apiIsAvailable = UltimateAuraProPlugin.softDependIsInstalled("Citizens");
        if (!apiIsAvailable) {
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

}

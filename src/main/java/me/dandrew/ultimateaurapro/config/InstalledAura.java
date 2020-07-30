package me.dandrew.ultimateaurapro.config;

import me.dandrew.ultimateaurapro.UltimateAuraProPermissions;
import me.dandrew.ultimateaurapro.auragiving.AuraInfo;

public class InstalledAura {

    private String name;
    private String description;
    private AuraInfo auraInfo;

    public InstalledAura(String name, String description, AuraInfo auraInfo) {
        this.name = name;
        this.description = description;
        this.auraInfo = auraInfo;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AuraInfo getAuraInfo() {
        return auraInfo;
    }

    public String getPermission() {
        return UltimateAuraProPermissions.AURA_NODE + "." + name;
    }

}

package me.dandrew.ultimateaurapro.auragiving;

import java.util.ArrayList;
import java.util.List;

/**
 * An immutable class representing information regarding an aura
 */
public class AuraInfo {

    private List<AppearanceUnit> appearanceUnits;
    private AuraEffect auraEffect;
    private boolean isEffectOnlyAura = false;

    private int id;

    private static int lastId;

    public AuraInfo(AuraEffect auraEffect) {
        this(new ArrayList<>(), auraEffect);
        this.isEffectOnlyAura = true;
    }

    public AuraInfo(List<AppearanceUnit> appearanceUnits, AuraEffect auraEffect) {
        this(appearanceUnits, auraEffect, ++lastId);
    }

    private AuraInfo(List<AppearanceUnit> appearanceUnits, AuraEffect auraEffect, int id) {
        this.appearanceUnits = appearanceUnits;
        this.auraEffect = auraEffect;
        this.id = id;
    }

    boolean isEffectOnlyAura() {
        return isEffectOnlyAura;
    }

    public List<AppearanceUnit> getAppearanceUnits() {
        return appearanceUnits;
    }

    AuraEffect getAuraEffect() {
        return auraEffect;
    }

    public int getId() {
        return id;
    }

}

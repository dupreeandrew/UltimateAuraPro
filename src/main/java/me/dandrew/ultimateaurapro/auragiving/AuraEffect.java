package me.dandrew.ultimateaurapro.auragiving;

import org.bukkit.potion.PotionEffect;

import java.util.Collections;
import java.util.List;

public class AuraEffect {

    private AuraTarget auraTarget;
    private double radius;
    private List<PotionEffect> potionEffectList;

    public AuraEffect(AuraTarget auraTarget, double radius, List<PotionEffect> potionEffectList) {
        this.auraTarget = auraTarget;
        this.radius = radius;
        this.potionEffectList = Collections.unmodifiableList(potionEffectList);
    }

    public AuraTarget getAuraTarget() {
        return auraTarget;
    }

    public double getRadius() {
        return radius;
    }

    public List<PotionEffect> getPotionEffectList() {
        return potionEffectList;
    }

}

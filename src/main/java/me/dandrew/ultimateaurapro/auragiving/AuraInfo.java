package me.dandrew.ultimateaurapro.auragiving;

import me.dandrew.ultimateaurapro.particlecreation.presets.ShapeCreator;

/**
 * An immutable class representing information regarding an aura
 */
public class AuraInfo {

    private ShapeCreator shapeCreator;
    private long millisecondsUntilRepeat;
    private RotationMethod rotationMethod;
    private boolean isGrowthAura;
    private AuraEffect auraEffect;
    private boolean isEffectOnlyAura = false;

    private int id;

    private static int lastId;

    public AuraInfo(AuraEffect auraEffect) {
        this(null, -1, null, false, auraEffect);
        this.isEffectOnlyAura = true;
    }

    public AuraInfo(ShapeCreator shapeCreator, double secondsUntilRepeat, RotationMethod rotationMethod, boolean isGrowthAura, AuraEffect auraEffect) {
        this(shapeCreator, secondsUntilRepeat, rotationMethod, isGrowthAura, auraEffect, ++lastId);
    }

    private AuraInfo(ShapeCreator shapeCreator, double secondsUntilRepeat, RotationMethod rotationMethod, boolean isGrowthAura, AuraEffect auraEffect, int id) {
        this.shapeCreator = shapeCreator;
        this.millisecondsUntilRepeat = Math.max((long) (secondsUntilRepeat * 1000.00), 5); // a fifth of a tick
        this.rotationMethod = rotationMethod;
        this.isGrowthAura = isGrowthAura;
        this.auraEffect = auraEffect;
        this.id = id;
    }

    boolean isEffectOnlyAura() {
        return isEffectOnlyAura;
    }

    ShapeCreator getShapeCreatorCopy() {
        return shapeCreator.getCopy();
    }

    long getMillisecondsUntilRepeat() {
        return millisecondsUntilRepeat;
    }

    RotationMethod getRotationMethod() {
        return rotationMethod;
    }

    boolean isGrowthAura() {
        return isGrowthAura;
    }

    AuraEffect getAuraEffect() {
        return auraEffect;
    }

    public int getId() {
        return id;
    }

}

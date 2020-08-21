package me.dandrew.ultimateaurapro.auragiving;

import me.dandrew.ultimateaurapro.particlecreation.presets.ShapeCreator;

public class AppearanceUnit {

    private ShapeCreator shapeCreator;
    private long millisecondsUntilRepeat;
    private RotationMethod rotationMethod;
    private boolean isGrowthAura;
    private int id;

    private static int lastId = 0;

    public AppearanceUnit(ShapeCreator shapeCreator, double secondsUntilRepeat, RotationMethod rotationMethod, boolean isGrowthAura) {
        this.shapeCreator = shapeCreator;
        this.millisecondsUntilRepeat = (long) (secondsUntilRepeat * 1000.0);
        this.rotationMethod = rotationMethod;
        this.isGrowthAura = isGrowthAura;
        this.id = ++lastId;
    }

    public ShapeCreator getShapeCreatorCopy() {
        return shapeCreator.getCopy();
    }

    public long getMillisecondsUntilRepeat() {
        return millisecondsUntilRepeat;
    }

    public RotationMethod getRotationMethod() {
        return rotationMethod;
    }

    public boolean isGrowthAura() {
        return isGrowthAura;
    }

    public int getId() {
        return id;
    }

}

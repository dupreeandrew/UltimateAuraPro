package me.dandrew.ultimateaurapro.particlecreation.presets;

public enum RegisteredShape {
    HELIX("helix"), FORCEFIELD("forcefield"), STAR("star"), CIRCLE("circle"), WHIRL("whirl");

    private String shapeName;

    RegisteredShape(String shapeName) {
        this.shapeName = shapeName;
    }

    public String getShapeName() {
        return shapeName;
    }
}

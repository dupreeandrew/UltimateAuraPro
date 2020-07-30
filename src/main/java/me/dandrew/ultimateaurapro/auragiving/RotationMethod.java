package me.dandrew.ultimateaurapro.auragiving;

public enum RotationMethod {

    NONE("none"), YAW("yaw"), SLOW("slow"), ALWAYS("always");

    private String name;

    RotationMethod(String name) {
        this.name = name;
    }

    public static RotationMethod fromName(String name) {
        for (RotationMethod rotationMethod : values()) {
            if (rotationMethod.name.equals(name)) {
                return rotationMethod;
            }
        }

        throw new IllegalArgumentException("Received a bad rotation method. Received: " + name + ". Check auras.yml");

    }

}

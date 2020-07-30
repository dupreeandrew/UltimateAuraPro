package me.dandrew.ultimateaurapro.auragiving;

public enum AuraTarget {

    NONE("none"), WAND("wand"), WAND_SELF("wand-self"), SELF("self"), ALL("all"),
    OTHERS("others"), HOSTILE("hostile"), NON_HOSTILE("non-hostile");

    private String name;

    AuraTarget(String name) {
        this.name = name;
    }

    public static AuraTarget fromName(String name) {

        for (AuraTarget AuraTarget : values()) {
            if (AuraTarget.name.equals(name)) {
                return AuraTarget;
            }
        }

        throw new IllegalArgumentException("Received a bad effects target. Received: " + name + ". Check auras.yml");

    }
}

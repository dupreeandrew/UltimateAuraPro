package me.dandrew.ultimateaurapro.config;

import me.dandrew.ultimateaurapro.auragiving.RotationMethod;
import org.bukkit.Color;

import java.util.Collections;
import java.util.Map;

public class AppearanceEntry {

    private Map<String, Object> rawObjectMap;
    private String type;
    private double radius;
    private int thickness;
    private double spacingBetweenParticles;
    private double secondsUntilRepeat;
    private RotationMethod rotationMethod;
    private Map<Color, Integer> colorFrequencyMap;
    private boolean isGrowthAura;
    private double growthSecondsBetweenParticles;
    private int growthNumParticlesAtATime;



    private AppearanceEntry(Map<String, Object> rawObjectMap, String type, double radius, int thickness, double spacingBetweenParticles, double secondsUntilRepeat,
                            RotationMethod rotationMethod, Map<Color, Integer> colorFrequencyMap,
                            boolean isGrowthAura, double growthSecondsBetweenParticles, int growthNumParticlesAtATime) {
        this.rawObjectMap = rawObjectMap;
        this.type = type;
        this.radius = radius;
        this.thickness = thickness;
        this.spacingBetweenParticles = spacingBetweenParticles;
        this.secondsUntilRepeat = secondsUntilRepeat;
        this.rotationMethod = rotationMethod;
        this.colorFrequencyMap = Collections.unmodifiableMap(colorFrequencyMap);
        this.isGrowthAura = isGrowthAura;
        this.growthSecondsBetweenParticles = growthSecondsBetweenParticles;
        this.growthNumParticlesAtATime = growthNumParticlesAtATime;
    }

    public String getType() {
        return type;
    }

    public double getRadius() {
        return radius;
    }

    public int getThickness() {
        return thickness;
    }

    public double getSpacingBetweenParticles() {
        return spacingBetweenParticles;
    }

    public double getSecondsUntilRepeat() {
        return secondsUntilRepeat;
    }

    public RotationMethod getRotationMethod() {
        return rotationMethod;
    }

    public Object getUniqueProperty(String property, Object defaultObject) {
        return rawObjectMap.getOrDefault(property, defaultObject);
    }

    public Number getUniqueProperty(String property, Number defaultNumber) {
        return (Number) rawObjectMap.getOrDefault(property, defaultNumber);
    }

    /**
     * @return immutable map representing color and its respective frequency
     */
    public Map<Color, Integer> getColorFrequencyMap() {
        return colorFrequencyMap;
    }

    public boolean isGrowthAura() {
        return isGrowthAura;
    }

    public double getGrowthSecondsBetweenParticles() {
        return growthSecondsBetweenParticles;
    }

    public int getGrowthNumParticlesAtATime() {
        return growthNumParticlesAtATime;
    }

    public static class Builder {

        private String type;
        private double radius;
        private int thickness;
        private double spacingBetweenParticles;
        private double secondsUntilRepeat;
        private RotationMethod rotationMethod;
        private Map<Color, Integer> colorFrequencyMap;
        private boolean isGrowthAura;
        private double growthSecondsBetweenParticles;
        private int growthNumParticlesAtATime;
        private Map<String, Object> rawObjectMap;

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public Builder setRadius(double radius) {
            this.radius = radius;
            return this;
        }

        public Builder setThickness(int thickness) {
            this.thickness = thickness;
            return this;
        }

        public Builder setSpacingBetweenParticles(double spacingBetweenParticles) {
            this.spacingBetweenParticles = spacingBetweenParticles;
            return this;
        }

        public Builder setSecondsUntilRepeat(double secondsUntilRepeat) {
            this.secondsUntilRepeat = secondsUntilRepeat;
            return this;
        }

        public Builder setRotationMethod(RotationMethod rotationMethod) {
            this.rotationMethod = rotationMethod;
            return this;
        }

        public Builder setColorFrequencyMap(Map<Color, Integer> colorFrequencyMap) {
            this.colorFrequencyMap = colorFrequencyMap;
            return this;
        }

        public Builder setIsGrowthAura(boolean isGrowthAura) {
            this.isGrowthAura = isGrowthAura;
            return this;
        }

        public Builder setGrowthSecondsBetweenParticles(double growthSecondsBetweenParticles) {
            this.growthSecondsBetweenParticles = growthSecondsBetweenParticles;
            return this;
        }

        public Builder setGrowthNumParticlesAtATime(int growthNumParticlesAtATime) {
            this.growthNumParticlesAtATime = growthNumParticlesAtATime;
            return this;
        }
        public Builder setRawObjectMap(Map<String, Object> rawObjectMap) {
            this.rawObjectMap = rawObjectMap;
            return this;
        }

        public AppearanceEntry build() {
            return new AppearanceEntry(rawObjectMap, type, radius, thickness, spacingBetweenParticles, secondsUntilRepeat, rotationMethod, colorFrequencyMap, isGrowthAura, growthSecondsBetweenParticles, growthNumParticlesAtATime);
        }

    }
}

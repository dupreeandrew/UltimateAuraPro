package me.dandrew.ultimateaurapro.config;

import me.dandrew.ultimateaurapro.UltimateAuraProPlugin;
import me.dandrew.ultimateaurapro.auragiving.AuraInfo;
import me.dandrew.ultimateaurapro.particlecreation.presets.Particools;
import me.dandrew.ultimateaurapro.particlecreation.presets.ShapeCreator;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public enum AuraConfig {

    INSTANCE;

    private File aurasFile = null;
    private FileConfiguration config;

    private List<InstalledAura> installedAuras = new ArrayList<>();

    public void loadBaseFile(File aurasFile) {

        if (this.aurasFile == null) {
            UltimateAuraProPlugin.addReloadListener(this::reload);
        }

        this.aurasFile = aurasFile;
        reload();

    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(aurasFile);
        reInitInstalledAuras();
    }

    private void reInitInstalledAuras() {

        installedAuras.clear();


        Map<String, Object> auraNameAndRawSettingsMap = config.getValues(false);
        for (Map.Entry<String, Object> entry : auraNameAndRawSettingsMap.entrySet()) {

            ConfigurationSection auraSection = (ConfigurationSection) entry.getValue();
            StandardAuraProperties auraProperties = StandardAuraProperties.read(auraSection);

            String description = auraProperties.getDescription();
            AuraInfo auraInfo = getAuraInfoFromProperties(auraSection, auraProperties);

            String auraName = entry.getKey();
            InstalledAura installedAura = new InstalledAura(auraName, description, auraInfo);
            installedAuras.add(installedAura);


        }

    }

    private AuraInfo getAuraInfoFromProperties(ConfigurationSection auraSection, StandardAuraProperties properties) {
        switch (properties.getType()) {
            case "none":
                return new AuraInfo(properties.getAuraEffect());
            case "helix":
                return getHelixAuraInfo(auraSection, properties);
            case "forcefield":
                return BasicAuraSection.FORCEFIELD.getAuraInfo(auraSection);
            case "star":
                return BasicAuraSection.STAR.getAuraInfo(auraSection);
            case "circle":
                return BasicAuraSection.CIRCLE.getAuraInfo(auraSection);
            case "whirl":
                return BasicAuraSection.WHIRL.getAuraInfo(auraSection);
            default:
                throw new IllegalArgumentException("READ!!: Unsupported type: " + properties.getType()
                        + ". Please check auras.yml for: " + auraSection.getName());
        }
    }

    private AuraInfo getHelixAuraInfo(ConfigurationSection auraSection, StandardAuraProperties properties) {
        double radius = properties.getRadius();
        double height = getAppearanceSection(auraSection).getDouble("height");
        double distanceBetweenLoops = getAppearanceSection(auraSection).getDouble("distance-between-loops");
        ShapeCreator helixCreator = getParticoolsFromAuraProperties(properties)
                .getHelixCreator(radius, height, distanceBetweenLoops);
        return getAuraInfo(helixCreator, properties);
    }

    private static ConfigurationSection getAppearanceSection(ConfigurationSection auraSection) {
        ConfigurationSection appearanceSection = auraSection.getConfigurationSection("appearance");

        if (appearanceSection == null) {
            throw new IllegalArgumentException("Appearance section NOT FOUND");
        }

        return appearanceSection;

    }

    /**
     * Retrieves all the Particools-specific properties from StandardAuraProperties,
     * and creates an object out of it.
     */
    private static Particools getParticoolsFromAuraProperties(StandardAuraProperties s) {

        Particools.Builder builder = new Particools.Builder();
        for (Map.Entry<Color, Integer> colorFreqeuencyEntry : s.getColorFrequencyMap().entrySet()) {
            Color color = colorFreqeuencyEntry.getKey();
            int frequency = colorFreqeuencyEntry.getValue();
            builder.setColorProbabilityWeight(color, frequency);
        }

        return builder.setParticleThickness(s.getThickness())
                .setSpacingBetweenParticles(s.getSpacingBetweenParticles())
                .setSecondsBetweenGrowthParticles(s.getGrowthSecondsBetweenParticles())
                .setGrowthParticlesPerTick(s.getGrowthNumParticlesAtATime())
                .build();

    }

    private static AuraInfo getAuraInfo(ShapeCreator shapeCreator, StandardAuraProperties s) {
        shapeCreator.addOffset(0, 0.05, 0);
        return new AuraInfo(shapeCreator, s.getSecondsUntilRepeat(), s.getRotationMethod(), s.isGrowthAura(), s.getAuraEffect());
    }

    public List<InstalledAura> getInstalledAuras() {
        return Collections.unmodifiableList(installedAuras);
    }

    @Nullable
    public InstalledAura getInstalledAura(String name) {
        for (InstalledAura installedAura : installedAuras) {
            if (installedAura.getName().equals(name)) {
                return installedAura;
            }
        }
        return null;
    }

    /**
     * Basically, these config sections ALL have the same fields
     */
    private enum BasicAuraSection {

        FORCEFIELD(0), STAR(1), CIRCLE(2), WHIRL(3);


        private int id;

        BasicAuraSection(int id) {
            this.id = id;
        }

        public AuraInfo getAuraInfo(ConfigurationSection shapeSection) {
            StandardAuraProperties standardAuraProperties = StandardAuraProperties.read(shapeSection);
            return getAuraInfo(standardAuraProperties);
        }

        private AuraInfo getAuraInfo(StandardAuraProperties standardAuraProperties) {
            Particools particools = getParticoolsFromAuraProperties(standardAuraProperties);
            double radius = standardAuraProperties.getRadius();
            ShapeCreator shapeCreator = getShapeCreator(particools, radius);
            return AuraConfig.getAuraInfo(shapeCreator, standardAuraProperties);
        }

        private ShapeCreator getShapeCreator(Particools particools, double radius) {
            switch (id) {
                case 0:
                    return particools.getSphereCreator(radius);
                case 1:
                    return particools.getStarCreator(radius);
                case 2:
                    return particools.getCircleCreator(radius);
                case 3:
                    return particools.getWhirlCreator(radius, 12);
            }
            throw new UnsupportedOperationException("Could not map enum id to a ultimateaurapro shape creator method.");
        }

    }


}

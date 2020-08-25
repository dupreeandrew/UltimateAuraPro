package me.dandrew.ultimateaurapro.config;

import me.dandrew.ultimateaurapro.UltimateAuraProPlugin;
import me.dandrew.ultimateaurapro.auragiving.AppearanceUnit;
import me.dandrew.ultimateaurapro.auragiving.AuraInfo;
import me.dandrew.ultimateaurapro.particlecreation.presets.Particools;
import me.dandrew.ultimateaurapro.particlecreation.presets.ShapeCreator;
import me.dandrew.ultimateaurapro.particlecreation.presets.shapes.*;
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

        List<AppearanceUnit> appearanceUnits = new ArrayList<>();

        for (AppearanceEntry entry : properties.getAppearanceEntries()) {
            ShapeCreator shapeCreator;
            switch (entry.getType()) {
                case "none":
                    return new AuraInfo(properties.getAuraEffect());
                case "helix":
                    shapeCreator = getHelixShapeCreator(entry);
                    break;
                case "forcefield":
                    shapeCreator = BasicAuraSection.FORCEFIELD.getShapeCreator(entry);
                    break;
                case "star":
                    shapeCreator = BasicAuraSection.STAR.getShapeCreator(entry);
                    break;
                case "circle":
                    shapeCreator = BasicAuraSection.CIRCLE.getShapeCreator(entry);
                    break;
                case "whirl":
                    shapeCreator = BasicAuraSection.WHIRL.getShapeCreator(entry);
                    break;
                default:
                    throw new IllegalArgumentException("READ!!: Unsupported type: '" + entry.getType()
                            + "'. Please check auras.yml for the aura: " + auraSection.getName());
            }

            AppearanceUnit appearanceUnit = new AppearanceUnit(shapeCreator, entry.getSecondsUntilRepeat(),
                    entry.getRotationMethod(), entry.isGrowthAura());

            appearanceUnits.add(appearanceUnit);
        }

        return new AuraInfo(appearanceUnits, properties.getAuraEffect());

    }

    private ShapeCreator getHelixShapeCreator(AppearanceEntry entry) {
        double radius = entry.getRadius();
        double height = entry.getUniqueProperty("height", 2.00).doubleValue();
        double distanceBetweenLoops = entry.getUniqueProperty("distance-between-loops", 1.00).doubleValue();
        return getParticoolsFromAppearanceEntry(entry)
                .getPreconfiguredShapeCreator(new ShapeHelix(height, distanceBetweenLoops), radius);
    }

    /**
     * Retrieves all the Particools-specific properties from StandardAuraProperties,
     * and creates an object out of it.
     */
    private static Particools getParticoolsFromAppearanceEntry(AppearanceEntry e) {

        Particools.Builder builder = new Particools.Builder();
        for (Map.Entry<Color, Integer> colorFreqeuencyEntry : e.getColorFrequencyMap().entrySet()) {
            Color color = colorFreqeuencyEntry.getKey();
            int frequency = colorFreqeuencyEntry.getValue();
            builder.setColorProbabilityWeight(color, frequency);
        }

        return builder.setParticleThickness(e.getThickness())
                .setSpacingBetweenParticles(e.getSpacingBetweenParticles())
                .setSecondsBetweenGrowthParticles(e.getGrowthSecondsBetweenParticles())
                .setGrowthParticlesPerTick(e.getGrowthNumParticlesAtATime())
                .build();

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

        public ShapeCreator getShapeCreator(AppearanceEntry entry) {
            Particools particools = getParticoolsFromAppearanceEntry(entry);
            double radius = entry.getRadius();
            return getShapeCreator(particools, radius);
        }

        private ShapeCreator getShapeCreator(Particools particools, double radius) {
            Shape shape;
            switch (id) {
                case 0:
                    shape = new ShapeSphere();
                    break;
                case 1:
                    shape = new ShapeStar();
                    break;
                case 2:
                    shape = new ShapeCircle();
                    break;
                case 3:
                    shape = new ShapeWhirl(5);
                    break;
                default:
                    throw new UnsupportedOperationException("Could not map enum id to a ultimateaurapro shape creator method.");
            }

            ShapeCreator shapeCreator = particools.getPreconfiguredShapeCreator(shape, radius);
            if (id == 0) {
                shapeCreator.addOffset(0, 0.75, 0);
            }

            return shapeCreator;


        }

    }


}

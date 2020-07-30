package me.dandrew.ultimateaurapro.config;

import me.dandrew.ultimateaurapro.auragiving.AuraEffect;
import me.dandrew.ultimateaurapro.auragiving.AuraTarget;
import me.dandrew.ultimateaurapro.auragiving.RotationMethod;
import me.dandrew.ultimateaurapro.util.Debug;
import me.dandrew.ultimateaurapro.util.TickConverter;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class StandardAuraProperties {


    private String type;
    private String description;
    private double radius;
    private int thickness;
    private double spacingBetweenParticles;
    private double secondsUntilRepeat;
    private transient RotationMethod rotationMethod;
    private Map<String, Object> rawColorHexMap;
    private transient Map<Color, Integer> colorFrequencyMap;
    private boolean isGrowthAura;
    private double growthSecondsBetweenParticles;
    private int growthNumParticlesAtATime;
    private AuraEffect auraEffect;


    private StandardAuraProperties(String type, String description, double radius, int thickness, double spacingBetweenParticles,
                                   double secondsUntilRepeat, RotationMethod rotationMethod, Map<String, Object> rawColorHexMap,
                                   boolean isGrowthAura, double growthSecondsBetweenParticles, int growthNumParticlesAtATime,
                                   AuraEffect auraEffect) {
        this.type = type;
        this.description = description;
        this.radius = radius;
        this.thickness = thickness;
        this.spacingBetweenParticles = spacingBetweenParticles;
        this.secondsUntilRepeat = secondsUntilRepeat;
        this.rotationMethod = rotationMethod;
        this.rawColorHexMap = rawColorHexMap;
        this.colorFrequencyMap = getConvertedRawColorMap(rawColorHexMap);
        this.isGrowthAura = isGrowthAura;
        this.growthSecondsBetweenParticles = growthSecondsBetweenParticles;
        this.growthNumParticlesAtATime = growthNumParticlesAtATime;
        this.auraEffect = auraEffect;
    }

    private static Map<Color, Integer> getConvertedRawColorMap(Map<String, Object> rawColorHexFrequencyMap) {
        Map<Color, Integer> colorFrequencyMap = new LinkedHashMap<>();
        for (Map.Entry<String, Object> colorHexFrequencyEntry : rawColorHexFrequencyMap.entrySet()) {
            String hex = colorHexFrequencyEntry.getKey();
            Color color = getColorFromHex(hex);
            int frequency = (Integer) colorHexFrequencyEntry.getValue();
            colorFrequencyMap.put(color, frequency);
        }
        return Collections.unmodifiableMap(colorFrequencyMap);
    }

    private static Color getColorFromHex(String hex) {
        if (hex.charAt(0) == '#') {
            hex = hex.substring(1);
        }
        int rgb = Integer.valueOf(hex, 16);
        return Color.fromRGB(rgb);
    }

    public static StandardAuraProperties read(ConfigurationSection rawAuraProperties) {

        String description = rawAuraProperties.getString("description", "?");
        AuraEffect auraEffect = getAuraEffect(rawAuraProperties);

        // Begin Appearance Section
        ConfigurationSection appearanceSection = rawAuraProperties.getConfigurationSection("appearance");
        if (appearanceSection == null) {
            return new StandardAuraProperties("none", description, 0, 0, 0, 0, RotationMethod.NONE, new HashMap<>(), false, -1, -1, auraEffect);
        }

        String type = appearanceSection.getString("type", "circle");
        if ("none".equals(type)) {
            return new StandardAuraProperties("none", description, 0, 0, 0, 0, RotationMethod.NONE, new HashMap<>(), false, -1, -1, auraEffect);
        }

        double radius = appearanceSection.getDouble("radius", 3.0);
        int thickness = appearanceSection.getInt("particle-thickness", 2);
        double spacingBetweenParticles = appearanceSection.getDouble("spacing-between-particles", 1.0);
        double secondsUntilRepeat = appearanceSection.getDouble("seconds-until-repeat", 2.0);
        String rotationMethodString = appearanceSection.getString("rotation-method", "none");
        RotationMethod rotationMethod = RotationMethod.fromName(rotationMethodString);


        Map<String, Object> rawColorHexMap;
        if (appearanceSection.isSet("colors")) {
            rawColorHexMap = appearanceSection.getConfigurationSection("colors").getValues(false);
        }
        else {
            rawColorHexMap = new HashMap<>();
            rawColorHexMap.put("#00F5FF", 1);
        }


        ConfigurationSection growthSection = appearanceSection.getConfigurationSection("growth-settings");
        boolean isNull = (growthSection == null);
        boolean isGrowthAura = !isNull && growthSection.getBoolean("enabled", false);
        double growthSecondsBetweenParticles = isNull ? 0.05 : growthSection.getDouble("seconds-between-particles", 0.05);
        int numParticlesAtATime = isNull ? 3 : growthSection.getInt("num-particles-at-a-time", 3);

        return new StandardAuraProperties(type, description, radius, thickness, spacingBetweenParticles, secondsUntilRepeat,
                rotationMethod, rawColorHexMap, isGrowthAura, growthSecondsBetweenParticles, numParticlesAtATime, auraEffect);


    }

    private static AuraEffect getAuraEffect(ConfigurationSection rawAuraProperties) {
        ConfigurationSection auraEffectsSection = rawAuraProperties.getConfigurationSection("aura-effects");

        if (auraEffectsSection == null) {
            Debug.send(1 + "");
            return new AuraEffect(AuraTarget.NONE, 0, new ArrayList<>());
        }

        String targetString = auraEffectsSection.getString("target", "none");
        if (targetString == null || targetString.equals("none")) {
            return new AuraEffect(AuraTarget.NONE, 0, new ArrayList<>());
        }


        AuraTarget auraTarget = AuraTarget.fromName(targetString);
        double radius = auraEffectsSection.getDouble("radius", 0.00);

        List<String> potionEffectStrings = auraEffectsSection.getStringList("list");
        List<PotionEffect> potionEffects = (auraEffectsSection.isSet("list"))
                ? getPotionEffects(rawAuraProperties.getName(), potionEffectStrings)
                : new ArrayList<>();

        return new AuraEffect(auraTarget, radius, potionEffects);

    }


    private static void throwConfigError(String auraName, String msg) {
        throw new IllegalArgumentException("Error for auras.yml, aura: " + auraName + ": " + msg);
    }

    private static List<PotionEffect> getPotionEffects(String auraName, List<String> potionEffectStrings) {
        List<PotionEffect> potionEffects = new ArrayList<>();
        for (String potionEffectString : potionEffectStrings) {
            String[] args = potionEffectString.split(":");

            if (args.length == 1) {
                throwConfigError(auraName, "Bad potion effect format.");
                return null;
            }

            PotionEffectType potionEffectType = PotionEffectType.getByName(args[0]);
            if (potionEffectType == null) {
                throwConfigError(auraName, "Potion Effect: " + args[0] + " not found.");
                return null;
            }

            int level;
            try {
                level = Integer.parseInt(args[1]);
            }
            catch (NumberFormatException ex) {
                throwConfigError(auraName, "Received an invalid level for effect: " + args[0]);
                return null;
            }

            PotionEffect potionEffect = new PotionEffect(potionEffectType, TickConverter.getTicksFromSeconds(5), level - 1);
            potionEffects.add(potionEffect);
        }
        return Collections.unmodifiableList(potionEffects);
    }





    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public double getRadius() {
        return radius;
    }

    int getThickness() {
        return thickness;
    }

    double getSpacingBetweenParticles() {
        return spacingBetweenParticles;
    }

    double getSecondsUntilRepeat() {
        return secondsUntilRepeat;
    }

    RotationMethod getRotationMethod() {
        return rotationMethod;
    }

    /**
     * @return immutable map representing color and its respective frequency
     */
    Map<Color, Integer> getColorFrequencyMap() {
        return colorFrequencyMap;
    }

    boolean isGrowthAura() {
        return isGrowthAura;
    }

    double getGrowthSecondsBetweenParticles() {
        return growthSecondsBetweenParticles;
    }

    int getGrowthNumParticlesAtATime() {
        return growthNumParticlesAtATime;
    }

    AuraEffect getAuraEffect() {
        return auraEffect;
    }


}

package me.dandrew.ultimateaurapro.config;

import me.dandrew.ultimateaurapro.auragiving.AuraEffect;
import me.dandrew.ultimateaurapro.auragiving.AuraTarget;
import me.dandrew.ultimateaurapro.auragiving.RotationMethod;
import me.dandrew.ultimateaurapro.util.TickConverter;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class StandardAuraProperties {

    private List<AppearanceEntry> appearanceEntries;
    private String description;
    private AuraEffect auraEffect;


    private StandardAuraProperties(String description, List<AppearanceEntry> appearanceEntries, AuraEffect auraEffect) {
        this.appearanceEntries = appearanceEntries;
        this.description = description;
        this.auraEffect = auraEffect;
    }


    static StandardAuraProperties read(ConfigurationSection rawAuraProperties) {

        String description = rawAuraProperties.getString("description", "?");
        AuraEffect auraEffect = getAuraEffect(rawAuraProperties);

        // Begin Appearance Section
        List<LinkedHashMap<String, Object>> appearanceSections = (List<LinkedHashMap<String, Object>>) rawAuraProperties.get("appearances");
        List<AppearanceEntry> appearanceEntries = new ArrayList<>();

        if (appearanceSections != null) {
            for (LinkedHashMap<String, Object> appearanceMap : appearanceSections) {
                AppearanceEntry appearanceEntry = getAppearanceEntry(appearanceMap);
                appearanceEntries.add(appearanceEntry);
            }
        }

        return new StandardAuraProperties(description, appearanceEntries, auraEffect);


    }

    @SuppressWarnings("unchecked")
    private static AppearanceEntry getAppearanceEntry(LinkedHashMap<String, Object> appearanceMap) {
        if (appearanceMap == null) {
            return null;
        }

        String type = (String) appearanceMap.getOrDefault("type", "circle");
        if ("none".equals(type)) {
            return null;
        }

        double radius = ((Number) appearanceMap.getOrDefault("radius", 3.0)).doubleValue();
        int thickness = ((Number) appearanceMap.getOrDefault("particle-thickness", 2)).intValue();
        double spacingBetweenParticles = ((Number) appearanceMap.getOrDefault("spacing-between-particles", 1.0)).doubleValue();
        double secondsUntilRepeat = ((Number) appearanceMap.getOrDefault("seconds-until-repeat", 2.0)).doubleValue();
        String rotationMethodString = (String) appearanceMap.getOrDefault("rotation-method", "none");
        RotationMethod rotationMethod = RotationMethod.fromName(rotationMethodString);



        LinkedHashMap<String, Object> rawColorHexMap = (LinkedHashMap<String, Object>) appearanceMap.getOrDefault("colors", new LinkedHashMap<>());
        if (rawColorHexMap.size() == 0) {
            rawColorHexMap.put("#00F5FF", 1);
        }
        Map<Color, Integer> colorMap = getConvertedRawColorMap(rawColorHexMap);


        LinkedHashMap<String, Object> growthMap = (LinkedHashMap<String, Object>) appearanceMap.get("growth-settings");
        boolean isNull = (growthMap == null);
        boolean isGrowthAura = !isNull && (boolean) growthMap.getOrDefault("enabled", false);
        double growthSecondsBetweenParticles = isNull ? 0.05 : ((Number) growthMap.getOrDefault("seconds-between-particles", 0.05)).doubleValue();
        int numParticlesAtATime = isNull ? 3 : ((Number) growthMap.getOrDefault("num-particles-at-a-time", 3)).intValue();

        return new AppearanceEntry.Builder()
                .setType(type)
                .setRadius(radius)
                .setThickness(thickness)
                .setSpacingBetweenParticles(spacingBetweenParticles)
                .setSecondsUntilRepeat(secondsUntilRepeat)
                .setRotationMethod(rotationMethod)
                .setColorFrequencyMap(colorMap)
                .setIsGrowthAura(isGrowthAura)
                .setGrowthSecondsBetweenParticles(growthSecondsBetweenParticles)
                .setGrowthNumParticlesAtATime(numParticlesAtATime)
                .setRawObjectMap(appearanceMap)
                .build();

    }

    private static AuraEffect getAuraEffect(ConfigurationSection rawAuraProperties) {
        ConfigurationSection auraEffectsSection = rawAuraProperties.getConfigurationSection("aura-effects");

        if (auraEffectsSection == null) {
            return new AuraEffect(AuraTarget.NONE, 0, new ArrayList<>(), new ArrayList<>());
        }

        String targetString = auraEffectsSection.getString("target", "none");
        if (targetString == null || targetString.equals("none")) {
            return new AuraEffect(AuraTarget.NONE, 0, new ArrayList<>(), new ArrayList<>());
        }


        AuraTarget auraTarget = AuraTarget.fromName(targetString);
        double radius = auraEffectsSection.getDouble("radius", 0.00);

        List<String> potionEffectStrings = auraEffectsSection.getStringList("list");
        List<PotionEffect> potionEffects = getPotionEffects(rawAuraProperties.getName(), potionEffectStrings);

        List<String> commandEffectStrings = auraEffectsSection.getStringList("cmd-list");
        List<AuraEffect.PlayerCommandEffect> commandEffects = getCommandEffects(rawAuraProperties.getName(), commandEffectStrings);

        return new AuraEffect(auraTarget, radius, potionEffects, commandEffects);

    }



    private static void throwConfigError(String auraName, String msg) {
        throw new IllegalArgumentException("Error for auras.yml, aura: " + auraName + ": " + msg);
    }

    private static List<PotionEffect> getPotionEffects(String auraName, List<String> potionEffectStrings) {
        List<PotionEffect> potionEffects = new ArrayList<>();
        for (String potionEffectString : potionEffectStrings) {
            String[] args = potionEffectString.split(":");

            if (args.length == 1) {
                throwConfigError(auraName, "Bad potion effect format. Use: <potion-effect-name>:<level>");
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

            double seconds = (potionEffectType.getName().equals("CONFUSION")) ? 5.0 : 3.5;
            PotionEffect potionEffect = new PotionEffect(potionEffectType, TickConverter.getTicksFromSeconds(seconds), level - 1);
            potionEffects.add(potionEffect);

        }

        return Collections.unmodifiableList(potionEffects);

    }

    private static List<AuraEffect.PlayerCommandEffect> getCommandEffects(String auraName, List<String> commandEffectStrings) {
        List<AuraEffect.PlayerCommandEffect> commandEffects = new ArrayList<>();
        for (String commandEffectString : commandEffectStrings) {

            if (commandEffectString.isEmpty()) {
                continue;
            }

            if (commandEffectString.equals("3s:/a-test-command %username%")) {
                continue;
            }


            String[] args = commandEffectString.split(":");

            String syntaxMessage = "Read!!! Invalid player command effect syntax. " +
                    "Example syntax: \"3s:/eco give %username% 100\". " +
                    "In this example, this command will run every 3 seconds. What you typed: " + commandEffects;
            if (args.length == 1) {
                throwConfigError(auraName, syntaxMessage);
                return null;
            }

            String secondsUntilRepeatString = args[0].replace("s", "");
            double secondsUntilRepeat;
            try {
                secondsUntilRepeat = Double.parseDouble(secondsUntilRepeatString);
            }
            catch (NumberFormatException ex) {
                throwConfigError(auraName, syntaxMessage);
                return null;
            }

            String command = args[1];
            if (command.charAt(0) == '/') {
                command = command.substring(1);
            }

            AuraEffect.PlayerCommandEffect effect = new AuraEffect.PlayerCommandEffect(command, secondsUntilRepeat);
            commandEffects.add(effect);

        }
        return commandEffects;
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

    public List<AppearanceEntry> getAppearanceEntries() {
        return appearanceEntries;
    }

    public String getDescription() {
        return description;
    }

    AuraEffect getAuraEffect() {
        return auraEffect;
    }


}

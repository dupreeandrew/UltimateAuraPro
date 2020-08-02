package me.dandrew.ultimateaurapro.config;

import me.dandrew.ultimateaurapro.UltimateAuraProPlugin;
import me.dandrew.ultimateaurapro.auragiving.AssignedAura;
import me.dandrew.ultimateaurapro.auragiving.AuraInfo;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum LocationAuraConfig {

    INSTANCE;

    private File locationAurasFile;
    private FileConfiguration config;

    private Map<String, AssignedAura> runningLocationAuras = new HashMap<>();

    public void loadBaseFile(File locationAurasFile) {

        if (this.locationAurasFile == null) {
            UltimateAuraProPlugin.addReloadListener(this::reload);
        }
        this.locationAurasFile = locationAurasFile;
        reload();

    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(locationAurasFile);
        reInitLocationAurasFromConfig();
    }

    private void reInitLocationAurasFromConfig() {

        for (AssignedAura runningLocationAura : runningLocationAuras.values()) {
            runningLocationAura.deactivate();
        }

        runningLocationAuras.clear();

        for (LocationAura locationAura : getLocationAuras()) {
            createRunningLocationAura(locationAura);
        }

    }

    private void createRunningLocationAura(LocationAura locationAura) {
        Location location = locationAura.location;
        AuraInfo auraInfo = locationAura.installedAura.getAuraInfo();
        AssignedAura runningLocationAura = AssignedAura.activate(location, auraInfo);
        runningLocationAuras.put(locationAura.id, runningLocationAura);
    }

    public List<LocationAura> getLocationAuras() {
        List<LocationAura> locationAuras = new ArrayList<>();
        Map<String, Object> locationLocationDetails = config.getValues(false);
        for (Map.Entry<String, Object> entry : locationLocationDetails.entrySet()) {
            LocationAura locationAura = getLocationAura(entry);
            locationAuras.add(locationAura);
        }
        return locationAuras;
    }

    private static LocationAura getLocationAura(Map.Entry<String, Object> entry) {

        String id = entry.getKey();
        ConfigurationSection properties = (ConfigurationSection) entry.getValue();

        String auraName = properties.getString("aura-name");
        if (auraName == null) {
            throwConfigError(id, "Missing an aura name");
        }

        InstalledAura installedAura = AuraConfig.INSTANCE.getInstalledAura(auraName);
        if (installedAura == null) {
            throwConfigError(id, "Aura name: " + auraName + " does not exist in auras.yml");
        }

        String worldString = properties.getString("world");

        if (worldString == null) {
            throwConfigError(id, "Missing a world");
        }

        World world = Bukkit.getWorld(worldString);
        if (world == null) {
            throwConfigError(id, "Received an invalid world");
            return null;
        }

        double x = properties.getDouble("x");
        double y = properties.getDouble("y");
        double z = properties.getDouble("z");


        Location locationLocation = new Location(world, x, y, z);
        return new LocationAura(id, installedAura, locationLocation);

    }

    private static void throwConfigError(String id, String msg) {
        throw new IllegalArgumentException("Error for location-auras.yml under '" + id + "': " + msg);
    }

    /**
     * Returns true on success.
     * Returns false if the aura name was not found or the id already exists.
     */
    public boolean addLocationAura(LocationAura locationAura) {

        if (AuraConfig.INSTANCE.getInstalledAura(locationAura.installedAura.getName()) == null) {
            return false;
        }

        if (config.isSet(locationAura.id)) {
            return false;
        }

        createRunningLocationAura(locationAura);
        writeLocationAuraToConfig(locationAura);

        return true;

    }

    private void writeLocationAuraToConfig(LocationAura locationAura) {
        ConfigurationSection section = config.createSection(locationAura.id);
        section.set("aura-name", locationAura.installedAura.getName());

        Location location = locationAura.location;
        section.set("world", location.getWorld().getName());
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());

        save();
    }

    private boolean save() {
        try {
            config.save(locationAurasFile);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteLocationAura(String id) {
        boolean auraExists = config.isSet(id);
        if (!auraExists) {
            return false;
        }

        AssignedAura assignedAura = runningLocationAuras.get(id);
        if (assignedAura != null) {
            assignedAura.deactivate();
        }

        config.set(id, null);

        return save();

    }

    public static class LocationAura {

        private String id;
        private InstalledAura installedAura;
        private Location location;

        public LocationAura(String id, InstalledAura installedAura, Location location) {
            this.id = id;
            this.installedAura = installedAura;
            this.location = location;
        }

        public String getId() {
            return id;
        }

        public InstalledAura getInstalledAura() {
            return installedAura;
        }

        public Location getLocation() {
            return location.clone();
        }

    }

}

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

public enum PermanentAuraConfig {

    INSTANCE;

    private File permanentAurasFile;
    private FileConfiguration config;

    private Map<String, AssignedAura> runningPermanentAuras = new HashMap<>();

    public void loadBaseFile(File permanentAurasFile) {

        if (this.permanentAurasFile == null) {
            UltimateAuraProPlugin.addReloadListener(this::reload);
        }
        this.permanentAurasFile = permanentAurasFile;
        reload();

    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(permanentAurasFile);
        reInitPermanentAurasFromConfig();
    }

    private void reInitPermanentAurasFromConfig() {

        for (AssignedAura runningPermanentAura : runningPermanentAuras.values()) {
            runningPermanentAura.deactivate();
        }

        runningPermanentAuras.clear();

        for (PermanentAura permanentAura : getPermanentAuras()) {
            createRunningPermanentAura(permanentAura);
        }

    }

    private void createRunningPermanentAura(PermanentAura permanentAura) {
        Location location = permanentAura.location;
        AuraInfo auraInfo = permanentAura.installedAura.getAuraInfo();
        AssignedAura runningPermanentAura = AssignedAura.activate(location, auraInfo);
        runningPermanentAuras.put(permanentAura.id, runningPermanentAura);
    }

    public List<PermanentAura> getPermanentAuras() {
        List<PermanentAura> permanentAuras = new ArrayList<>();
        Map<String, Object> permanentLocationDetails = config.getValues(false);
        for (Map.Entry<String, Object> entry : permanentLocationDetails.entrySet()) {
            PermanentAura permanentAura = getPermanentAura(entry);
            permanentAuras.add(permanentAura);
        }
        return permanentAuras;
    }

    private static PermanentAura getPermanentAura(Map.Entry<String, Object> entry) {

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


        Location permanentLocation = new Location(world, x, y, z);
        return new PermanentAura(id, installedAura, permanentLocation);

    }

    private static void throwConfigError(String id, String msg) {
        throw new IllegalArgumentException("Error for permanent-auras.yml under '" + id + "': " + msg);
    }

    /**
     * Returns true on success.
     * Returns false if the aura name was not found or the id already exists.
     */
    public boolean addPermanentAura(PermanentAura permanentAura) {

        if (AuraConfig.INSTANCE.getInstalledAura(permanentAura.installedAura.getName()) == null) {
            return false;
        }

        if (config.isSet(permanentAura.id)) {
            return false;
        }

        createRunningPermanentAura(permanentAura);
        writePermanentAuraToConfig(permanentAura);

        return true;

    }

    private void writePermanentAuraToConfig(PermanentAura permanentAura) {
        ConfigurationSection section = config.createSection(permanentAura.id);
        section.set("aura-name", permanentAura.installedAura.getName());

        Location location = permanentAura.location;
        section.set("world", location.getWorld().getName());
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());

        save();
    }

    private boolean save() {
        try {
            config.save(permanentAurasFile);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deletePermanentAura(String id) {
        boolean auraExists = config.isSet(id);
        if (!auraExists) {
            return false;
        }

        AssignedAura assignedAura = runningPermanentAuras.get(id);
        if (assignedAura != null) {
            assignedAura.deactivate();
        }

        config.set(id, null);

        return save();

    }

    public static class PermanentAura {

        private String id;
        private InstalledAura installedAura;
        private Location location;

        public PermanentAura(String id, InstalledAura installedAura, Location location) {
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

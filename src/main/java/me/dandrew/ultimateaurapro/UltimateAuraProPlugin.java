package me.dandrew.ultimateaurapro;

import me.dandrew.ultimateaurapro.auragiving.AssignedAura;
import me.dandrew.ultimateaurapro.auragiving.AuraTracker;
import me.dandrew.ultimateaurapro.auragiving.AuraWand;
import me.dandrew.ultimateaurapro.command.BaseCommand;
import me.dandrew.ultimateaurapro.config.AuraConfig;
import me.dandrew.ultimateaurapro.config.PermanentAuraConfig;
import me.dandrew.ultimateaurapro.particlecreation.presets.Particools;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class UltimateAuraProPlugin extends JavaPlugin {

    public static UltimateAuraProPlugin plugin;
    private static List<ReloadListener> reloadListeners = new ArrayList<>();
    public static final ScheduledThreadPoolExecutor executorService;

    static {
        int maxThreads = Math.max(Runtime.getRuntime().availableProcessors() / 2, 2);
        maxThreads = (maxThreads % 2 == 0) ? maxThreads : maxThreads + 1;
        executorService = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(maxThreads);
    }

    @Override
    public void onEnable() {
        super.onEnable();

        UltimateAuraProPlugin.plugin = this;
        initStartupApps();
        registerEvents();
    }


    private void initStartupApps() {
        File[] files = ensureFilesAreCreated();

        File aurasFile = files[0];
        AuraConfig.INSTANCE.loadBaseFile(aurasFile);

        File permanentAurasFile = files[3];
        PermanentAuraConfig.INSTANCE.loadBaseFile(permanentAurasFile);
    }

    public static File[] ensureFilesAreCreated() {
        File[] files = {
                new File(plugin.getDataFolder(), "auras.yml"),
                new File(plugin.getDataFolder(), "help.txt"),
                new File(plugin.getDataFolder(), "templates.yml"),
                new File(plugin.getDataFolder(), "permanent-auras.yml")
        };

        for (File file : files) {
            if (!file.exists()) {
                plugin.saveResource(file.getName(), false);
            }
        }
        return files;
    }

    public static void reloadSettings() {
        AssignedAura.stopEverything();
        ensureFilesAreCreated();
        for (ReloadListener reloadListener : reloadListeners) {
            reloadListener.onReload();
        }
    }



    @Override
    public void onDisable() {
        super.onDisable();
        executorService.shutdown();
    }

    private void registerEvents() {
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new AuraTracker(), this);
        pluginManager.registerEvents(new AuraWand(), this);
    }

    public Particools.Builder getParticoolsBuilder() {
        return new Particools.Builder();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {

        if (!cmd.getName().equals("aura")) {
            return super.onCommand(sender, cmd, label, args);
        }


        BaseCommand command = new BaseCommand(sender, args);
        command.execute();

        return true;

    }

    public static void addReloadListener(ReloadListener listener) {
        reloadListeners.add(listener);
    }

    public static boolean softDependIsInstalled(String pluginName) {
        return plugin.getServer().getPluginManager().getPlugin(pluginName) != null;
    }

    public interface ReloadListener {
        void onReload();
    }

}

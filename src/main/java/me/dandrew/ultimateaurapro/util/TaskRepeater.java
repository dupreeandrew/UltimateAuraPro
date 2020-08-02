package me.dandrew.ultimateaurapro.util;

import me.dandrew.ultimateaurapro.UltimateAuraProPlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public class TaskRepeater {

    public static BukkitTask run(Runnable runnable, long ticksInterval, int ticksDuration) {
        BukkitScheduler scheduler = Bukkit.getScheduler();
        BukkitTask task = scheduler.runTaskTimer(UltimateAuraProPlugin.plugin, runnable, 0L, ticksInterval);

        scheduler.scheduleSyncDelayedTask(UltimateAuraProPlugin.plugin, () -> {
            if (!task.isCancelled()) {
                task.cancel();
            }
        }, ticksDuration);

        return task;
    }

    public static BukkitTask runUntilCancel(SelfCancellingTask task, long tickInterval, int maxTicksDuration) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(UltimateAuraProPlugin.plugin, task, 1, tickInterval);
        configureCancelling(bukkitTask, task, maxTicksDuration);
        return bukkitTask;
    }

    public static BukkitTask runUntilCancel(SelfCancellingTask task, long tickDelay, long tickInterval, int maxTicksDuration) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(UltimateAuraProPlugin.plugin, task, tickDelay, tickInterval);
        configureCancelling(bukkitTask, task, maxTicksDuration);
        return bukkitTask;
    }

    private static void configureCancelling(BukkitTask bukkitTask, SelfCancellingTask selfCancellingTask, int maxTicksDuration) {
        selfCancellingTask.setMainBukkitTask(bukkitTask);
        Bukkit.getScheduler().scheduleSyncDelayedTask(UltimateAuraProPlugin.plugin, () -> {
            if (!bukkitTask.isCancelled()) {
                bukkitTask.cancel();
            }
        }, maxTicksDuration);
    }

    public static abstract class SelfCancellingTask implements Runnable {

        private BukkitTask task;

        public SelfCancellingTask() {
            // empty constructor
        }

        private void setMainBukkitTask(BukkitTask task) {
            this.task = task;
        }

        @Override
        public final void run() {
            if (onTick()) {
                task.cancel();
            }
        }

        /**
         * @return true if task should cancel. false if it should resume
         */
        public abstract boolean onTick();

    }
}

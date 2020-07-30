package me.dandrew.ultimateaurapro.auragiving;

import me.dandrew.ultimateaurapro.UltimateAuraProPlugin;
import me.dandrew.ultimateaurapro.particlecreation.GrowthListener;
import me.dandrew.ultimateaurapro.particlecreation.GrowthTask;
import me.dandrew.ultimateaurapro.particlecreation.presets.ShapeCreator;
import me.dandrew.ultimateaurapro.util.LocationUtil;
import me.dandrew.ultimateaurapro.util.ObjectContainer;
import me.dandrew.ultimateaurapro.util.TaskRepeater;
import me.dandrew.ultimateaurapro.util.TickConverter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AssignedAura {

    private @Nullable String playerName;
    private @Nullable Location permanentLocation;
    private AuraInfo auraInfo;
    private ScheduledFuture particleTask;
    private BukkitTask effectsTask;
    private static Set<GrowthTask> activeGrowthTasks = new HashSet<>();
    private static Set<AssignedAura> assignedAuras = new HashSet<>();

    private static Map<Integer, ShapeCreator> appropriateShapeCreatorMap
            = new HashMap<>(); // K: auraSettings.getId(), V: appropriate ShapeCreator
    private static List<ScheduledFuture> rotationClocks = new ArrayList<>();
    private static ScheduledExecutorService executorService = UltimateAuraProPlugin.executorService;

    private AssignedAura(String playerName, AuraInfo auraInfo, @Nullable ScheduledFuture particleTask, @Nullable BukkitTask effectsTask) {
        this.playerName = playerName;
        this.auraInfo = auraInfo;
        this.particleTask = particleTask;
        this.effectsTask = effectsTask;
        assignedAuras.add(this);
    }

    private AssignedAura(Location location, AuraInfo auraInfo, @Nullable ScheduledFuture particleTask, @Nullable BukkitTask effectsTask) {
        this.auraInfo = auraInfo;
        this.particleTask = particleTask;
        this.effectsTask = effectsTask;
        this.permanentLocation = location;
        assignedAuras.add(this);
    }

    static AssignedAura activate(String playerName, AuraInfo auraInfo) {
        BukkitTask effectsTask = getEffectsTask(playerName, auraInfo);
        if (auraInfo.isEffectOnlyAura()) {
            return new AssignedAura(playerName, auraInfo, null, effectsTask);
        }

        ScheduledFuture particlesTask = getParticlesTask(getLocationCallbackFromPlayerName(playerName), auraInfo);
        return new AssignedAura(playerName, auraInfo, particlesTask, effectsTask);
    }

    private static SynchronizedLocation getLocationCallbackFromPlayerName(String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null || !player.isValid()) {
            return null;
        }
        return player::getLocation;
    }

    public static AssignedAura activate(Location location, AuraInfo auraInfo) {
        BukkitTask effectsTask = getEffectsTask(location, auraInfo);
        if (auraInfo.isEffectOnlyAura()) {
            return new AssignedAura(location, auraInfo, null, effectsTask);
        }

        ScheduledFuture particlesTask = getParticlesTask(() -> location, auraInfo);
        return new AssignedAura(location, auraInfo, particlesTask, effectsTask);
    }

    private static @Nullable BukkitTask getEffectsTask(String playerName, AuraInfo auraInfo) {
        AuraEffect auraEffect = auraInfo.getAuraEffect();

        if (auraEffect.getAuraTarget() == AuraTarget.NONE) {
            return null;
        }

        return TaskRepeater.runUntilCancel(new TaskRepeater.SelfCancellingTask() {
            @Override
            public boolean onTick() {

                Player player = Bukkit.getPlayerExact(playerName);
                if (player == null) {
                    return true;
                }

                buffTargetEntities(player, auraEffect);
                return false;

            }
        }, TickConverter.getTicksFromSeconds(2), Integer.MAX_VALUE);
    }

    private static @Nullable BukkitTask getEffectsTask(Location location, AuraInfo auraInfo) {
        AuraEffect auraEffect = auraInfo.getAuraEffect();

        if (auraEffect.getAuraTarget() == AuraTarget.NONE) {
            return null;
        }

        return TaskRepeater.runUntilCancel(new TaskRepeater.SelfCancellingTask() {
            @Override
            public boolean onTick() {

                Collection<Entity> nearbyEntities = location.getWorld()
                        .getNearbyEntities(location, auraEffect.getRadius(), auraEffect.getRadius(), auraEffect.getRadius());

                boolean hostilesOnly = auraEffect.getAuraTarget() == AuraTarget.HOSTILE;

                Set<LivingEntity> candidateEntities = new HashSet<>();
                for (Entity entity : nearbyEntities) {
                    if (!(entity instanceof LivingEntity)) {
                        continue;
                    }

                    if (hostilesOnly && !(entity instanceof Monster)) {
                        continue;
                    }

                    if (auraEffect.getAuraTarget() == AuraTarget.NON_HOSTILE && entity instanceof Monster) {
                        continue;
                    }

                    if (entity.getLocation().distance(location) <= auraEffect.getRadius()) {
                        candidateEntities.add((LivingEntity) entity);
                    }
                }


                buffEntities(auraEffect, candidateEntities);

                return false;

            }
        }, TickConverter.getTicksFromSeconds(2), Integer.MAX_VALUE);
    }

    private static void buffTargetEntities(Player player, AuraEffect auraEffect) {
        Set<LivingEntity> targetEntities = getTargetEntities(player, auraEffect);
        buffEntities(auraEffect, targetEntities);
    }

    private static void buffEntities(AuraEffect auraEffect, Iterable<LivingEntity> livingEntities) {
        for (PotionEffect potionEffect : auraEffect.getPotionEffectList()) {
            for (LivingEntity livingEntity : livingEntities) {
                PotionEffect activePotionEffect = livingEntity.getPotionEffect(potionEffect.getType());
                boolean force = activePotionEffect != null && activePotionEffect.getAmplifier() <= potionEffect.getAmplifier();
                livingEntity.addPotionEffect(potionEffect, force);
            }
        }
    }

    private static Set<LivingEntity> getTargetEntities(Player player, AuraEffect auraEffect) {
        Set<LivingEntity> targetEntities = new HashSet<>();
        double targetRadius = auraEffect.getRadius();
        switch (auraEffect.getAuraTarget()) {
            case NONE:
                return targetEntities;
            case SELF:
                targetEntities.add(player);
                return targetEntities;
            case WAND_SELF:
                targetEntities.add(player);
            case WAND:
                Set<LivingEntity> buffableLivingEntities = AuraWand.getBuffableLivingEntities(player, targetRadius);
                targetEntities.addAll(buffableLivingEntities);
                return targetEntities;
            case ALL:
                targetEntities.add(player);
            case OTHERS:
            case HOSTILE:
                break;
            default:
                throw new UnsupportedOperationException("This aura target is not supported yet: " + auraEffect.getAuraTarget().name());
        }

        // At this point, we have ALL, OTHERS, or HOSTILE
        List<Entity> possibleEntities = player.getNearbyEntities(targetRadius, targetRadius, targetRadius);
        for (Entity possibleEntity : possibleEntities) {
            LivingEntity targetedEntity = checkTargetedEntity(player, auraEffect, possibleEntity);
            if (targetedEntity != null) {
                targetEntities.add(targetedEntity);
            }
        }

        return targetEntities;

    }

    private static @Nullable LivingEntity checkTargetedEntity(Player player, AuraEffect auraEffect, Entity possibleEntity) {

        if (!(possibleEntity instanceof LivingEntity)) {
            return null;
        }

        LivingEntity targetedEntity = (LivingEntity) possibleEntity;

        if (targetedEntity.isInvulnerable()) {
            return null;
        }

        // ensure a spherical radius rather than a cube.
        if (player.getLocation().distance(possibleEntity.getLocation()) > auraEffect.getRadius()) {
            return null;
        }

        AuraTarget auraTarget = auraEffect.getAuraTarget();
        switch (auraTarget) {
            case ALL:
            case OTHERS:
                return targetedEntity;
            case HOSTILE:
                return (targetedEntity instanceof Monster) ? targetedEntity : null;
            case NON_HOSTILE:
                return (targetedEntity instanceof Monster) ? null : targetedEntity;
            default:
                throw new IllegalArgumentException("Aura target: " + auraTarget.name() + " not supported.");

        }

    }

    private static ScheduledFuture getParticlesTask(SynchronizedLocation synchronizedLocation, AuraInfo auraInfo) {
        ShapeCreator shapeCreator = getAppropriateShapeCreator(auraInfo, auraInfo.getRotationMethod());

        return executorService.scheduleAtFixedRate(() -> {
            Location location = synchronizedLocation.getLocation();

            RotationMethod rotationMethod = auraInfo.getRotationMethod();
            double angDeg = LocationUtil.getDegreesFromYaw(location.getYaw());
            if (auraInfo.isGrowthAura()) {


                ObjectContainer<GrowthTask> containedGrowthTask = new ObjectContainer<>();
                GrowthListener growthListener = getGrowthListener(synchronizedLocation, containedGrowthTask);

                GrowthTask growthTask = null;
                switch (rotationMethod) {
                    case NONE:
                    case SLOW:
                        growthTask = shapeCreator.grow(growthListener, false);
                        break;
                    case YAW:
                        shapeCreator.growAtAngle(growthListener, angDeg, innerGrowthTask -> {
                            containedGrowthTask.setObject(innerGrowthTask);
                            activeGrowthTasks.add(innerGrowthTask);
                        });
                        break;
                    case ALWAYS:
                        growthTask = shapeCreator.grow(growthListener, true);
                        break;
                    default:
                        throw new UnsupportedOperationException("Rotation method: " + rotationMethod.name() + " not yet available");
                }

                if (growthTask != null) {
                    containedGrowthTask.setObject(growthTask);
                    activeGrowthTasks.add(growthTask);
                }

            }
            else {
                switch (rotationMethod) {
                    case NONE:
                    case SLOW:
                        shapeCreator.emit(location);
                        break;
                    case YAW:
                    case ALWAYS:
                        shapeCreator.emitAtAngle(location, angDeg);
                        break;
                }

            }
        }, 1, auraInfo.getMillisecondsUntilRepeat(), TimeUnit.MILLISECONDS);
    }

    /**
     * There should be more than one ShapeCreator because orientations are constantly changing.
     * Each player shouldn't get their own shape creator either because there could be a lot of vectors in memory.
     * A rotation method of SLOW has its rotation clock pre-initialized from #ensureAuraInfoIsInstalled();
     * A rotation method of ALWAYS results in a new ShapeCreator being generated.
     */
    private static ShapeCreator getAppropriateShapeCreator(AuraInfo auraInfo, RotationMethod rotationMethod) {

        if (rotationMethod == RotationMethod.ALWAYS) {
            return auraInfo.getShapeCreatorCopy();
        }

        ensureAuraInfoIsInstalled(auraInfo);
        return appropriateShapeCreatorMap.get(auraInfo.getId());

    }

    /**
     * Initializes a key/value pair for appropriateShapeCreatorMap();
     * Which also... allows for #getAppropriateShapeCreator() to work
     */
    private static void ensureAuraInfoIsInstalled(AuraInfo auraInfo) {

        if (auraInfo.isEffectOnlyAura()) {
            return;
        }

        if (appropriateShapeCreatorMap.containsKey(auraInfo.getId())) {
            return;
        }

        if (auraInfo.getRotationMethod() == RotationMethod.ALWAYS) {
            throw new IllegalStateException("Aura Data should not be installed since the rotation method is ALWAYS");
        }

        ShapeCreator shapeCreatorCopy = auraInfo.getShapeCreatorCopy();
        appropriateShapeCreatorMap.put(auraInfo.getId(), shapeCreatorCopy);

        if (auraInfo.getRotationMethod() == RotationMethod.SLOW) {
            ScheduledFuture rotationClock = executorService.scheduleAtFixedRate(() -> {
                shapeCreatorCopy.changeOrientation(60, 0);
            }, 2, auraInfo.getMillisecondsUntilRepeat(), TimeUnit.MILLISECONDS);

            rotationClocks.add(rotationClock);
        }

    }

    private static GrowthListener getGrowthListener(SynchronizedLocation synchronizedLocation, ObjectContainer<GrowthTask> containedGrowthTask) {
        return new GrowthListener() {

            @Override
            public Location getSynchronizedCentralLocation() {
                return synchronizedLocation.getLocation();
            }

            @Override
            public void onFinish() {
                GrowthTask growthTask = containedGrowthTask.getObject();
                activeGrowthTasks.remove(growthTask);
            }

        };
    }

    public void deactivate() {

        if (particleTask != null) {
            particleTask.cancel(false);
        }

        if (effectsTask != null) {
            effectsTask.cancel();
        }

        assignedAuras.remove(this);

    }

    AssignedAura getActivatedCopy() {
        return playerName != null ? activate(playerName, auraInfo) : activate(permanentLocation, auraInfo);
    }

    public static void stopEverything() {

        for (AssignedAura assignedAura : new HashSet<>(assignedAuras)) {
            assignedAuras.remove(assignedAura);
        }

        stopRotationClocks();
        deleteParticleCreatorCache();
        stopIncompletedGrowthTasks();


    }

    private static void stopRotationClocks() {
        for (ScheduledFuture slowRotationClock : rotationClocks) {
            slowRotationClock.cancel(false);
        }
        rotationClocks.clear();
    }

    /**
     * Deletes the particle creator (or ShapeCreator collection) cache.
     * This will not halt any existing AssignedAuras, but rather, frees up memory.
     */
    private static void deleteParticleCreatorCache() {
        appropriateShapeCreatorMap.clear();
    }

    static void stopIncompletedGrowthTasks() {
        for (GrowthTask activeGrowthTask : new HashSet<>(activeGrowthTasks)) {
            activeGrowthTask.cancel();
        }
        activeGrowthTasks.clear();
    }

    AuraInfo getAuraInfo() {
        return auraInfo;
    }

}

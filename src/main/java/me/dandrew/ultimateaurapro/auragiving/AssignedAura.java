package me.dandrew.ultimateaurapro.auragiving;

import me.dandrew.ultimateaurapro.UltimateAuraProPlugin;
import me.dandrew.ultimateaurapro.lib.SpecialEntityChecker;
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
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AssignedAura {

    private static Set<AssignedAura> assignedAuras = new HashSet<>();

    private @Nullable String playerName;
    private @Nullable Location permanentLocation;

    private AuraInfo auraInfo;
    private List<ScheduledFuture> particleTasks;
    private BukkitTask effectsTask;
    private Set<BukkitTask> playerCommandEffectTasks = new HashSet<>();

    private Set<GrowthTask> activeGrowthTasks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static Map<Integer, ShapeCreator> appropriateShapeCreatorMap
            = new HashMap<>(); // K: AppearanceUnit.getId(), V: appropriate ShapeCreator
    private static List<ScheduledFuture> rotationClocks = new ArrayList<>();
    private static ScheduledExecutorService executorService = UltimateAuraProPlugin.executorService;

    static AssignedAura activate(String playerName, AuraInfo auraInfo) {
        return new AssignedAura(playerName, auraInfo);
    }

    private AssignedAura(String playerName, AuraInfo auraInfo) {
        this.playerName = playerName;
        this.auraInfo = auraInfo;
        initAllEffects(playerName, auraInfo);

        if (!auraInfo.isEffectOnlyAura()) {
            this.particleTasks = getParticlesTasks(getLocationCallbackFromPlayerName(playerName), auraInfo);
        }

        assignedAuras.add(this);
    }

    public static AssignedAura activate(Location location, AuraInfo auraInfo) {
        return new AssignedAura(location, auraInfo);
    }

    private AssignedAura(Location location, AuraInfo auraInfo) {
        this.permanentLocation = location;
        this.auraInfo = auraInfo;
        initAllEffects(location, auraInfo);

        if (!auraInfo.isEffectOnlyAura()) {
            this.particleTasks = getParticlesTasks(() -> location, auraInfo);
        }

        assignedAuras.add(this);
    }


    private static SynchronizedLocation getLocationCallbackFromPlayerName(String playerName) {

        return () -> {

            Player player = Bukkit.getPlayerExact(playerName);
            if (player == null || !player.isValid()) {
                return null;
            }

            if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                return null;
            }

            return player.getLocation();

        };
    }



    private void initAllEffects(String playerName, AuraInfo auraInfo) {
        AuraEffect auraEffect = auraInfo.getAuraEffect();

        if (auraEffect.getAuraTarget() == AuraTarget.NONE) {
            return;
        }

        this.effectsTask = TaskRepeater.runUntilCancel(new TaskRepeater.SelfCancellingTask() {
            @Override
            public boolean onTick() {

                Player player = Bukkit.getPlayerExact(playerName);
                if (player == null) {
                    return true;
                }

                buffTargetEntities(player, auraEffect);
                return false;

            }
        }, TickConverter.getTicksFromSeconds(1.5), Integer.MAX_VALUE);

        initPlayerCommandEffects(() -> {
            Player player = Bukkit.getPlayerExact(playerName);
            return getTargetEntities(player, auraEffect);
        }, auraEffect);

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
            case NON_HOSTILE:
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

        if (SpecialEntityChecker.checkShouldIgnore(possibleEntity)) {
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

    private void initPlayerCommandEffects(CandidateEntities candidateEntities, AuraEffect auraEffect) {
        for (AuraEffect.PlayerCommandEffect playerCommandEffect : auraEffect.getCommandEffectList()) {
            BukkitTask task = TaskRepeater.runUntilCancel(new TaskRepeater.SelfCancellingTask() {
                @Override
                public boolean onTick() {

                    Collection<LivingEntity> livingEntities = candidateEntities.get();
                    if (livingEntities == null) {
                        return true;
                    }

                    Collection<Player> players = getPlayers(livingEntities);
                    runConsoleCommands(playerCommandEffect, players);

                    return false;
                }
            }, playerCommandEffect.getTicksUntilRepeat(), playerCommandEffect.getTicksUntilRepeat(), Integer.MAX_VALUE);
            playerCommandEffectTasks.add(task);
        }
    }

    private void initAllEffects(Location location, AuraInfo auraInfo) {
        AuraEffect auraEffect = auraInfo.getAuraEffect();

        if (auraEffect.getAuraTarget() == AuraTarget.NONE) {
            return;
        }

        this.effectsTask = TaskRepeater.runUntilCancel(new TaskRepeater.SelfCancellingTask() {
            @Override
            public boolean onTick() {
                Collection<LivingEntity> candidateEntities = getCandidateEntities(location, auraEffect);
                buffEntities(auraEffect, candidateEntities);
                return false;

            }
        }, TickConverter.getTicksFromSeconds(1.5), Integer.MAX_VALUE);

        initPlayerCommandEffects(() -> getCandidateEntities(location, auraEffect), auraEffect);

    }


    private Collection<LivingEntity> getCandidateEntities(Location location, AuraEffect auraEffect) {
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

            if (SpecialEntityChecker.checkShouldIgnore(entity)) {
                continue;
            }

            if (entity.getLocation().distance(location) <= auraEffect.getRadius()) {
                candidateEntities.add((LivingEntity) entity);
            }
        }

        return candidateEntities;

    }

    private Collection<Player> getPlayers(Collection<LivingEntity> livingEntities) {
        Set<Player> players = new HashSet<>();
        for (LivingEntity entity : livingEntities) {
            if (entity instanceof Player) {
                players.add((Player) entity);
            }
        }
        return players;
    }

    private void runConsoleCommands(AuraEffect.PlayerCommandEffect playerCommandEffect, Collection<Player> players) {
        for (Player player : players) {
            String cmd = playerCommandEffect.getRunnableConsoleCommand(player);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    private List<ScheduledFuture> getParticlesTasks(SynchronizedLocation synchronizedLocation, AuraInfo auraInfo) {
        List<ScheduledFuture> particleTasks = new ArrayList<>();
        for (AppearanceUnit appearanceUnit : auraInfo.getAppearanceUnits()) {
            ScheduledFuture particleTask = getParticleTask(synchronizedLocation, auraInfo, appearanceUnit);
            particleTasks.add(particleTask);
        }
        return particleTasks;
    }

    private ScheduledFuture getParticleTask(SynchronizedLocation synchronizedLocation, AuraInfo auraInfo, AppearanceUnit appearanceUnit) {
        ShapeCreator shapeCreator = getAppropriateShapeCreator(auraInfo, appearanceUnit);
        return executorService.scheduleAtFixedRate(() -> {

            Location location = synchronizedLocation.getLocation();

            if (location == null) {
                return;
            }

            RotationMethod rotationMethod = appearanceUnit.getRotationMethod();
            double angDeg = LocationUtil.getDegreesFromYaw(location.getYaw());
            if (appearanceUnit.isGrowthAura()) {


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
        }, 1, appearanceUnit.getMillisecondsUntilRepeat(), TimeUnit.MILLISECONDS);
    }

    /**
     * There should be more than one ShapeCreator because orientations are constantly changing.
     * Each player shouldn't get their own shape creator either because there could be a lot of vectors in memory.
     * A rotation method of SLOW has its rotation clock pre-initialized from #ensureAuraInfoIsInstalled();
     * A rotation method of ALWAYS results in a new ShapeCreator being generated.
     */
    private static ShapeCreator getAppropriateShapeCreator(AuraInfo auraInfo, AppearanceUnit appearanceUnit) {

        if (appearanceUnit.getRotationMethod() == RotationMethod.ALWAYS) {
            return appearanceUnit.getShapeCreatorCopy();
        }

        ensureAuraInfoIsInstalled(auraInfo, appearanceUnit);
        return appropriateShapeCreatorMap.get(appearanceUnit.getId());

    }

    /**
     * Initializes a key/value pair for appropriateShapeCreatorMap();
     * Which also... allows for #getAppropriateShapeCreator() to work
     */
    private static void ensureAuraInfoIsInstalled(AuraInfo auraInfo, AppearanceUnit appearanceUnit) {

        if (auraInfo.isEffectOnlyAura()) {
            return;
        }

        if (appropriateShapeCreatorMap.containsKey(appearanceUnit.getId())) {
            return;
        }

        if (appearanceUnit.getRotationMethod() == RotationMethod.ALWAYS) {
            // this should not throw.
            throw new IllegalStateException("Aura Data should not be installed since the rotation method is ALWAYS");
        }

        ShapeCreator shapeCreatorCopy = appearanceUnit.getShapeCreatorCopy();
        appropriateShapeCreatorMap.put(appearanceUnit.getId(), shapeCreatorCopy);

        if (appearanceUnit.getRotationMethod() == RotationMethod.SLOW) {
            ScheduledFuture rotationClock = executorService.scheduleAtFixedRate(() -> {
                shapeCreatorCopy.changeOrientation(60, 0);
            }, 2, appearanceUnit.getMillisecondsUntilRepeat(), TimeUnit.MILLISECONDS);

            rotationClocks.add(rotationClock);
        }

    }

    private GrowthListener getGrowthListener(SynchronizedLocation synchronizedLocation, ObjectContainer<GrowthTask> containedGrowthTask) {
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

        for (ScheduledFuture particleTask : particleTasks) {
            particleTask.cancel(true);
        }
        particleTasks.clear();

        if (effectsTask != null) {
            effectsTask.cancel();
        }

        for (GrowthTask activeGrowthTask : new HashSet<>(activeGrowthTasks)) {
            activeGrowthTask.cancel();
        }
        activeGrowthTasks.clear();

        for (BukkitTask playerCommandEffectTask : new HashSet<>(playerCommandEffectTasks)) {
            playerCommandEffectTask.cancel();
        }
        playerCommandEffectTasks.clear();


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
            slowRotationClock.cancel(true);
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

    private static void stopIncompletedGrowthTasks() {
        for (AssignedAura assignedAura : new HashSet<>(assignedAuras)) {
            for (GrowthTask activeGrowthTask : new HashSet<>(assignedAura.activeGrowthTasks)) {
                activeGrowthTask.cancel();
            }
            assignedAura.activeGrowthTasks.clear();
        }
    }

    AuraInfo getAuraInfo() {
        return auraInfo;
    }

    private interface CandidateEntities {
        Collection<LivingEntity> get();
    }

}

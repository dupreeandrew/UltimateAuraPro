package me.dandrew.ultimateaurapro.particlecreation;

import me.dandrew.ultimateaurapro.UltimateAuraProPlugin;
import me.dandrew.ultimateaurapro.util.LocationUtil;
import me.dandrew.ultimateaurapro.util.ObjectContainer;
import me.dandrew.ultimateaurapro.util.VectorRotator;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Immutable class that will emit particles following constructor settings.
 * Same as Emitter, except there are vector calculations being done outside the main thread.
 */
@SuppressWarnings("WeakerAccess")
public class AsyncEmitter {

    private List<Color> colors;
    private AtomicInteger nextColorIndex = new AtomicInteger(0);
    private int particleThickness;
    private int numGrowthIterationsAtATime;
    private int millisBetweenGrowthParticles;

    public static ScheduledExecutorService executorService = UltimateAuraProPlugin.executorService;
    private static ConcurrentHashMap<Float, double[]> cachedTrigValuesMap = new ConcurrentHashMap<Float, double[]>() {
        @Override
        public double[] put(@NotNull Float key, @NotNull double[] value) {

            // Debug.send("Added to trig cache: " + key + "(" + size() + ")");
            if (size() >= 1250) {
                clear();
            }

            return super.put(key, value);

        }


    }; // K: yaw. V: sin -> cos
    private static final int CACHED_SIN_INDEX = 0;
    private static final int CACHED_COS_INDEX = 1;

    public AsyncEmitter(List<Color> colors, int particleThickness) {
        this(colors, particleThickness, .05, 3);
    }

    public AsyncEmitter(List<Color> colors, int particleThickness, double secondsBetweenGrowthParticles, int numGrowthIterationsAtATime) {

        if (secondsBetweenGrowthParticles <= 0) {
            secondsBetweenGrowthParticles = .05;
        }

        this.colors = new ArrayList<>(colors);
        this.millisBetweenGrowthParticles = (int) (secondsBetweenGrowthParticles * 1000.00);
        this.particleThickness = particleThickness;
        this.numGrowthIterationsAtATime = numGrowthIterationsAtATime;

    }



    public void emitParticles(Location location, Iterable<Vector> particleOffsets, boolean clone) {
        Iterable<Vector> submittedParticleOffsets = getSubmittableParticleOffsets(particleOffsets, clone);
        executorService.submit(() -> {
            List<Location> particleLocations = new ArrayList<>();
            for (Vector particleOffset : submittedParticleOffsets) {
                Location particleLocation = location.clone().add(particleOffset);
                particleLocations.add(particleLocation);
            }
            emitParticlesOnCurrentThread(particleLocations);
        });

    }
    private Iterable<Vector> getSubmittableParticleOffsets(Iterable<Vector> particleOffsets, boolean clone) {
        return clone ? LocationUtil.cloneOffsets(particleOffsets) : particleOffsets;
    }

    public void emitParticles(Iterable<Location> locations) {
        executorService.submit(() -> emitParticlesOnCurrentThread(locations));
    }

    public void emitParticlesOnCurrentThread(Iterable<Location> locations) {
        for (Location location : locations) {
            emitParticleOnCurrentThread(location);
        }
    }

    public void emitParticleOnCurrentThread(Location location) {
        EmitterTools.emitParticle(location, getNextColor(), particleThickness);
    }

    private Color getNextColor() {
        int colorIndex = nextColorIndex.getAndSet((nextColorIndex.get() + 1) % colors.size() );
        return colors.get(colorIndex);
    }

    public GrowthTask growParticles(GrowthListener growthListener, List<Vector> particleOffsets,
                                    boolean trackYaw, boolean clone) {
        Iterable<Vector> submittableParticleOffsets = getSubmittableParticleOffsets(particleOffsets, clone);
        Iterator<Vector> iterator = submittableParticleOffsets.iterator();
        return startGrowthClock(growthListener, iterator, trackYaw);
    }

    private GrowthTask startGrowthClock(GrowthListener growthListener, Iterator iterator, boolean trackYaw) {
        ObjectContainer<ScheduledFuture> scheduledFutureContainer = new ObjectContainer<>();
        ScheduledFuture<?> scheduledFuture = executorService.scheduleAtFixedRate(() -> {
            if (!emitNextParticles(growthListener, iterator, trackYaw)) {
                scheduledFutureContainer.getObject().cancel(false);
                growthListener.onFinish();
            }
        }, 1, millisBetweenGrowthParticles, TimeUnit.MILLISECONDS);
        scheduledFutureContainer.setObject(scheduledFuture);
        return () -> scheduledFuture.cancel(false);
    }

    /**
     * Emits the next set of particles (wrt numGrowthIterationsAtATime) returned by the iterator
     * Calculations & particle emission are async
     * @return true if there are more particles to emit
     * @param iterator Object must be either a vector or collection of vectors.
     */
    private boolean emitNextParticles(GrowthListener growthListener, Iterator iterator, boolean adjustWithYaw) {
        // This should not be multithreaded to ensure the return value is accurate
        List<Vector> offsetBuffer = getBuffer(iterator);
        if (offsetBuffer.size() > 0 && offsetBuffer.size() < 5) {
            processOffsetBuffer(growthListener, offsetBuffer, adjustWithYaw);
            return true;
        }
        else if (offsetBuffer.size() >= 5) {
            executorService.submit(() -> processOffsetBuffer(growthListener, offsetBuffer, adjustWithYaw));
            return true;
        }
        else {
            return false;
        }

    }

    private List<Vector> getBuffer(Iterator iterator) {
        List<Vector> particleOffsetBuffer = new ArrayList<>();
        for (int i = 0; i < numGrowthIterationsAtATime; i++) {
            addParticleOffsetsFromNextIteration(iterator, particleOffsetBuffer);
            if (!iterator.hasNext()) {
                break;
            }
        }
        return particleOffsetBuffer;
    }

    private void addParticleOffsetsFromNextIteration(Iterator iterator, List<Vector> particleOffsets) {
        Object object = iterator.next();
        if (object instanceof Collection) {
            Collection<Vector> offsetCollection = (Collection<Vector>) object;
            particleOffsets.addAll(offsetCollection);
        }
        else {
            particleOffsets.add((Vector) object);
        }
    }

    private void processOffsetBuffer(GrowthListener growthListener, Iterable<Vector> particleOffsetBuffer, boolean adjustWithYaw) {
        Location centralLocation = growthListener.getSynchronizedCentralLocation();
        List<Location> particleLocations = new ArrayList<>();
        for (Vector particleOffset : particleOffsetBuffer) {
            Location particleLocation = getParticleLocation(centralLocation, particleOffset, adjustWithYaw);
            particleLocations.add(particleLocation);
        }
        emitParticlesOnCurrentThread(particleLocations);
    }

    private static Location getParticleLocation(Location centralLocation, Vector particleOffset, boolean adjustWithYaw) {
        Location particleLocation = centralLocation.clone();
        if (adjustWithYaw) {

            Vector modifiedParticleOffset = particleOffset.clone();

            float yaw = centralLocation.getYaw();
            double[] trigValues = getTrigValues(yaw);

            VectorRotator.rotateAroundY(modifiedParticleOffset, trigValues[CACHED_SIN_INDEX], trigValues[CACHED_COS_INDEX]);
            particleLocation.add(modifiedParticleOffset);

        }
        else {
            particleLocation.add(particleOffset);
        }
        return particleLocation;
    }

    private static double[] getTrigValues(float yaw) {
        double[] trigValues = cachedTrigValuesMap.get(yaw);
        if (trigValues == null) {
            double radsRotation = LocationUtil.getRadiansFromYaw(yaw);
            trigValues = new double[2];
            trigValues[CACHED_SIN_INDEX] = Math.sin(radsRotation);
            trigValues[CACHED_COS_INDEX] = Math.cos(radsRotation);
            cachedTrigValuesMap.put(yaw, trigValues);

            addExpirationToYawKey(yaw);
        }
        return trigValues;
    }

    private static void addExpirationToYawKey(float yaw) {
        executorService.schedule(() -> {
            cachedTrigValuesMap.remove(yaw);
        }, 3, TimeUnit.SECONDS);
    }

    public GrowthTask growParticleCollections(GrowthListener growthListener, List<Iterable<Vector>> setsOfOffsets,
                                        boolean trackYaw, boolean clone) {
        setsOfOffsets = clone ? LocationUtil.cloneSetsOfOffsets(setsOfOffsets) : setsOfOffsets;
        Iterator<Iterable<Vector>> iterator = setsOfOffsets.iterator();
        return startGrowthClock(growthListener, iterator, trackYaw);
    }


    private void emitSetsOfParticleOffsets(Location location, Queue<Collection<Vector>> setsOfParticleOffsets, boolean clone) {
        for (Collection<Vector> particleOffsets : setsOfParticleOffsets) {
            emitParticles(location, particleOffsets, clone);
        }
    }

    public static class Builder {

        private List<Color> colors = new ArrayList<>();
        private double secondsBetweenGrowthIterations = 0.05;
        private int numIterationsAtATime = 3;
        private int particleThickness = 1;

        public Builder addColor(Color color) {
            colors.add(color);
            return this;
        }

        public Builder addColor(Color color, int occurrence) {
            for (int i = 0; i < occurrence; i++){
                colors.add(color);
            }
            return this;
        }

        public Builder setSecondsBetweenGrowthIterations(double seconds) {
            this.secondsBetweenGrowthIterations = seconds;
            return this;
        }

        public Builder setNumGrowthIterationsAtATime(int numIterationsAtATime) {
            this.numIterationsAtATime = numIterationsAtATime;
            return this;
        }

        public Builder setParticleThickness(int particleThickness) {
            this.particleThickness = particleThickness;
            return this;
        }


        public AsyncEmitter build() {

            if (colors.size() == 0) {
                throw new IllegalStateException("At least one color is needed.");
            }

            if (particleThickness < 1) {
                throw new IllegalStateException("Particle thickness must be greater than or equal to 1");
            }

            if (secondsBetweenGrowthIterations < 0) {
                throw new IllegalStateException("Invalid seconds between growth particles. Must be greater than 0");
            }

            if (numIterationsAtATime < 1) {
                throw new IllegalStateException("Particles per tick must be greater than or equal to 1");
            }

            return new AsyncEmitter(colors, particleThickness, secondsBetweenGrowthIterations, numIterationsAtATime);

        }



    }

}

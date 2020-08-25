package me.dandrew.ultimateaurapro.particlecreation.presets;

import me.dandrew.ultimateaurapro.UltimateAuraProPlugin;
import me.dandrew.ultimateaurapro.particlecreation.AsyncEmitter;
import me.dandrew.ultimateaurapro.particlecreation.GrowthListener;
import me.dandrew.ultimateaurapro.particlecreation.GrowthTask;
import me.dandrew.ultimateaurapro.util.LocationUtil;
import me.dandrew.ultimateaurapro.util.VectorRotator;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;

/**
 * This class will emit/grow shapes based on given particle offsets and the order they were inserted in.
 */
public class ShapeCreator {

    private static ExecutorService executorService = UltimateAuraProPlugin.executorService;

    private AsyncEmitter emitter;
    // Accessing / modifying these below variables MUST be done through the executorservice
    private Queue<Vector> particleOffsets;
    private Queue<Iterable<Vector>> particleOffsetCollections;

    // These are copies of the current object. These should not be modified (but may be re-referenced), as they're copies.
    private ShapeCreator cachedCopy;

    private Phaser modPhaser = new Phaser(1);

    private ShapeCreator(AsyncEmitter emitter, Queue<Vector> particleOffsets, Queue<Iterable<Vector>> particleOffsetCollections) {
        this.emitter = emitter;
        this.particleOffsets = particleOffsets;
        this.particleOffsetCollections = particleOffsetCollections;
    }

    public static ShapeCreator fromQueuedOffsets(AsyncEmitter emitter, Queue<Vector> particleOffsets) {
        return new ShapeCreator(emitter, particleOffsets, null);
    }

    public static ShapeCreator fromQueuedOffsetCollections(AsyncEmitter emitter, Queue<Iterable<Vector>> queueOfOffsetCollections) {
        return new ShapeCreator(emitter, null, queueOfOffsetCollections);
    }

    private synchronized ShapeCreator tryGettingCachedCopy() {
        if (probablyEquals(cachedCopy)) {
            this.cachedCopy = getCopy();
            return this;
        }
        else {
            this.cachedCopy = getCopy();
            return cachedCopy;
        }

    }

    private synchronized boolean probablyEquals(ShapeCreator sc2) {
        if (sc2 == null) {
            return false;
        }

        Vector firstOffset = getFirstOffset();
        Vector cachedFirstOffset = sc2.getFirstOffset();

        return firstOffset.equals(cachedFirstOffset);

    }

    private synchronized Vector getFirstOffset() {
        if (particleOffsets != null) {
            return particleOffsets.peek();
        }
        else {
            return particleOffsetCollections.peek().iterator().next();
        }
    }

    public synchronized ShapeCreator getCopy() {
        modPhaser.arriveAndAwaitAdvance();
        Queue<Vector> copiedParticleOffsets = null;
        Queue<Iterable<Vector>> copiedOffsetCollections = null;
        if (particleOffsets != null) {
            copiedParticleOffsets = LocationUtil.cloneOffsets(particleOffsets);
        }
        else {
            copiedOffsetCollections = LocationUtil.cloneSetsOfOffsets(particleOffsetCollections);
        }

        return new ShapeCreator(emitter, copiedParticleOffsets, copiedOffsetCollections);
    }

    public synchronized void changeOrientation(double degreesRotation, double degreesTilt) {

        final double RADS_ROTATION = Math.toRadians(degreesRotation);
        final double ROTATION_SIN_VALUE = Math.sin(RADS_ROTATION);
        final double ROTATION_COS_VALUE = Math.cos(RADS_ROTATION);

        final double RADS_TILT = Math.toRadians(degreesTilt);
        final double TILT_SIN_VALUE = Math.sin(RADS_TILT);
        final double TILT_COS_VALUE = Math.cos(RADS_TILT);

        modifyAllParticleOffsets(particleOffset -> {
            if (Math.abs(RADS_ROTATION) > 0.00001) {
                // rotate around Y, with pre-calculated trig values
                VectorRotator.rotateAroundY(particleOffset, ROTATION_SIN_VALUE, ROTATION_COS_VALUE);
            }

            if (Math.abs(RADS_TILT) > 0.00001) {
                // rotate around Z, with pre-calculated trig values
                VectorRotator.rotateAroundZ(particleOffset, TILT_SIN_VALUE, TILT_COS_VALUE);
            }
        });

    }

    /**
     * Blocking method. Recommended to call this on another thread
     */
    private synchronized void modifyAllParticleOffsets(ParticleOffsetModifier modifier) {
        modPhaser.register();
        if (particleOffsets != null) {
            for (Vector particleOffset : particleOffsets) {
                modifier.onOffsetModify(particleOffset);
            }
        }
        else {
            for (Iterable<Vector> particleOffsetCollection : particleOffsetCollections) {
                for (Vector particleOffset : particleOffsetCollection) {
                    modifier.onOffsetModify(particleOffset);
                }
            }
        }
        modPhaser.arriveAndDeregister();
    }

    public synchronized void addOffset(double x, double y, double z) {
        Vector addedOffset = new Vector(x, y, z);
        modifyAllParticleOffsets(particleOffset -> particleOffset.add(addedOffset));
    }

    public void emitAtAngle(Location location, double degrees) {
        executorService.submit(() -> {
            ShapeCreator copy = getCopy();
            copy.changeOrientation(degrees, 0);
            copy.emit(location);
        });
    }

    public void emit(Location location) {
        executorService.submit(() ->
                tryGettingCachedCopy().emit(location, false));
    }

    private void emit(Location location, boolean clone) {
        if (particleOffsets != null) {
            emitter.emitParticles(location, particleOffsets, clone);
            return;
        }
        executorService.submit(() -> {
            for (Iterable<Vector> particleOffsetCollection : particleOffsetCollections) {
                emitter.emitParticles(location, particleOffsetCollection, clone);
            }
        });

    }

    public void growAtAngle(GrowthListener growthListener, double degrees, TaskCreatedListener listener) {
        executorService.submit(() -> {
            ShapeCreator copy = getCopy();
            copy.changeOrientation(degrees, 0);
            GrowthTask growthTask = copy.grow(growthListener, false);
            listener.onTaskCreated(growthTask);
        });
    }


    public GrowthTask grow(GrowthListener growthListener, boolean trackYaw) {
        ShapeCreator copy = tryGettingCachedCopy();
        if (copy.particleOffsets != null) {
            return emitter.growParticles(growthListener, copy.particleOffsets, trackYaw, false);
        }
        else {
            return emitter.growParticleCollections(growthListener, copy.particleOffsetCollections, trackYaw, false);
        }
    }

    public interface ParticleOffsetModifier {
        void onOffsetModify(Vector particleOffset);
    }

    public interface TaskCreatedListener {
        void onTaskCreated(GrowthTask growthTask);
    }

}

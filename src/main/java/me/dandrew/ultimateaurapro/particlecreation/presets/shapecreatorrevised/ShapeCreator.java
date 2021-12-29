package me.dandrew.ultimateaurapro.particlecreation.presets.shapecreatorrevised;

import me.dandrew.ultimateaurapro.UltimateAuraProPlugin;
import me.dandrew.ultimateaurapro.particlecreation.AsyncEmitter;
import me.dandrew.ultimateaurapro.particlecreation.GrowthListener;
import me.dandrew.ultimateaurapro.particlecreation.GrowthTask;
import me.dandrew.ultimateaurapro.util.VectorRotator;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;

/**
 * This class can safely allow you to concurrently modify/transform given offsets in a safer manner.
 * If the passed in offsets will never be modified, using AsyncEmitter directly may offer better performance.
 */
public abstract class ShapeCreator<T extends ShapeCreator> {

    private static ExecutorService executorService = UltimateAuraProPlugin.executorService;

    private AsyncEmitter emitter;
    // Accessing / modifying these below variables MUST be done through the executorservice

    // This is a possible copy of the current object. This should NOT be modified (but may be re-referenced), as they're copies.
    private T cachedCopy;

    private Phaser modPhaser = new Phaser(1);

    ShapeCreator(AsyncEmitter emitter) {
        this.emitter = emitter;
    }

    AsyncEmitter getEmitter() {
        return emitter;
    }

    public static ShapeCreator fromQueuedOffsets(AsyncEmitter emitter, List<Vector> particleOffsets) {
        return new SingleShapeCreator(emitter, particleOffsets);
    }

    public static ShapeCreator fromQueuedOffsetCollections(AsyncEmitter emitter, List<Iterable<Vector>> queueOfOffsetCollections) {
        return new MultiShapeCreator(emitter, queueOfOffsetCollections);
    }

    private synchronized ShapeCreator tryGettingCachedCopy() {

        if (validateCache(cachedCopy)) {
            return cachedCopy;
        }
        else {
            this.cachedCopy = getCopy();
            return cachedCopy;
        }

    }

    private boolean validateCache(T cache) {
        return cache != null && onValidateCache(cache);
    }

    abstract boolean onValidateCache(@NotNull T cache);

    public synchronized T getCopy() {
        modPhaser.arriveAndAwaitAdvance();
        return onCreateCopy();
    }

    abstract T onCreateCopy();

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
        onModifyAllParticleOffsets(modifier);
        cachedCopy = null;
        modPhaser.arriveAndDeregister();
    }

    abstract void onModifyAllParticleOffsets(ParticleOffsetModifier modifier);

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
                tryGettingCachedCopy().onEmit(location, false));
    }

    abstract void onEmit(Location location, boolean clone);

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
        return copy.onGrow(growthListener, trackYaw);
    }

    abstract GrowthTask onGrow(GrowthListener growthListener, boolean trackYaw);

    public interface ParticleOffsetModifier {
        void onOffsetModify(Vector particleOffset);
    }

    public interface TaskCreatedListener {
        void onTaskCreated(GrowthTask growthTask);
    }

}

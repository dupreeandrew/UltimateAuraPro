package me.dandrew.ultimateaurapro.particlecreation.presets.shapecreatorrevised;

import me.dandrew.ultimateaurapro.UltimateAuraProPlugin;
import me.dandrew.ultimateaurapro.particlecreation.AsyncEmitter;
import me.dandrew.ultimateaurapro.particlecreation.GrowthListener;
import me.dandrew.ultimateaurapro.particlecreation.GrowthTask;
import me.dandrew.ultimateaurapro.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MultiShapeCreator extends ShapeCreator<MultiShapeCreator> {

    private List<Iterable<Vector>> particleOffsetCollections;

    public MultiShapeCreator(AsyncEmitter emitter, List<Iterable<Vector>> particleOffsetCollections) {
        super(emitter);
        this.particleOffsetCollections = particleOffsetCollections;
    }

    @Override
    MultiShapeCreator onCreateCopy() {
        List<Iterable<Vector>> copy = LocationUtil.cloneSetsOfOffsets(particleOffsetCollections);
        return new MultiShapeCreator(getEmitter(), copy);
    }

    @Override
    boolean onValidateCache(@NotNull MultiShapeCreator cache) {

        int indexToCheck = particleOffsetCollections.size() / 2;

        Iterable<Vector> origin = particleOffsetCollections.get(indexToCheck);
        Iterable<Vector> cached = cache.particleOffsetCollections.get(indexToCheck);


        for (Vector v1 : origin) {
            for (Vector v2 : cached) {
                return v1.equals(v2);
            }
        }

        return false;

    }

    @Override
    void onModifyAllParticleOffsets(ParticleOffsetModifier modifier) {
        for (Iterable<Vector> particleOffsetCollection : particleOffsetCollections) {
            for (Vector particleOffset : particleOffsetCollection) {
                modifier.onOffsetModify(particleOffset);
            }
        }
    }

    @Override
    void onEmit(Location location, boolean clone) {
        UltimateAuraProPlugin.executorService.submit(() -> {
            for (Iterable<Vector> particleOffsetCollection : particleOffsetCollections) {
                getEmitter().emitParticles(location, particleOffsetCollection, clone);
            }
        });
    }

    @Override
    GrowthTask onGrow(GrowthListener growthListener, boolean trackYaw) {
        return getEmitter().growParticleCollections(growthListener, particleOffsetCollections, trackYaw, false);
    }
}

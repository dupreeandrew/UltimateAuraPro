package me.dandrew.ultimateaurapro.particlecreation.presets.shapecreatorrevised;

import me.dandrew.ultimateaurapro.particlecreation.AsyncEmitter;
import me.dandrew.ultimateaurapro.particlecreation.GrowthListener;
import me.dandrew.ultimateaurapro.particlecreation.GrowthTask;
import me.dandrew.ultimateaurapro.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SingleShapeCreator extends ShapeCreator<SingleShapeCreator> {

    private List<Vector> particleOffsets;

    public SingleShapeCreator(AsyncEmitter emitter, List<Vector> particleOffsets) {
        super(emitter);
        this.particleOffsets = particleOffsets;
    }

    @Override
    SingleShapeCreator onCreateCopy() {
        List<Vector> copiedParticleOffsets = LocationUtil.cloneOffsets(particleOffsets);
        return new SingleShapeCreator(getEmitter(), copiedParticleOffsets);
    }

    @Override
    boolean onValidateCache(@NotNull SingleShapeCreator cache) {
        int maxVectorsToCheck = Math.min(particleOffsets.size(), 3);
        for (int i = 0; i < maxVectorsToCheck; i++) {

            Vector presentOffset = particleOffsets.get(i);

            if (presentOffset.length() < 0.0001) {
                continue;
            }

            Vector copiedOffset = cache.particleOffsets.get(i);

            return presentOffset.equals(copiedOffset);

        }

        return false;

    }

    @Override
    void onModifyAllParticleOffsets(ParticleOffsetModifier modifier) {
        for (Vector particleOffset : particleOffsets) {
            modifier.onOffsetModify(particleOffset);
        }
    }

    @Override
    void onEmit(Location location, boolean clone) {
        getEmitter().emitParticles(location, particleOffsets, clone);
    }

    @Override
    GrowthTask onGrow(GrowthListener growthListener, boolean trackYaw) {
        return getEmitter().growParticles(growthListener, particleOffsets, trackYaw, false);
    }
}

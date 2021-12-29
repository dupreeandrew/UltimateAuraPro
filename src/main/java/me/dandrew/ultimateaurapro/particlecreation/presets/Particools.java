/*
 * Copyright (c) 2020. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package me.dandrew.ultimateaurapro.particlecreation.presets;

import me.dandrew.ultimateaurapro.particlecreation.AsyncEmitter;
import me.dandrew.ultimateaurapro.particlecreation.presets.shapecreatorrevised.ShapeCreator;
import me.dandrew.ultimateaurapro.particlecreation.presets.shapes.Shape;
import me.dandrew.ultimateaurapro.util.LocationUtil;
import me.dandrew.ultimateaurapro.util.TaskRepeater;
import me.dandrew.ultimateaurapro.util.TickConverter;
import org.bukkit.Location;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;

import java.util.LinkedList;
import java.util.List;

/**
 * This class is responsible for creating ShapeCreators representing common shapes
 */
public class Particools {

    private AsyncEmitter emitter;
    private double spacingBetweenParticles;

    public Particools(AsyncEmitter emitter, double spacingBetweenParticles) {
        this.emitter = emitter;
        this.spacingBetweenParticles = spacingBetweenParticles;
    }

    public void addTrackingParticles(Projectile projectile) {
        TaskRepeater.run(() -> {
            if (projectile.isValid()) {
                emitter.emitParticleOnCurrentThread(projectile.getLocation());
                Location secondParticleLocation = projectile.getLocation().clone().add(.25, 0, .25);
                emitter.emitParticleOnCurrentThread(secondParticleLocation);
            }
        }, 1, TickConverter.getTicksFromSeconds(5));
    }

    private static boolean isLessThanOrEqualTo(double a, double b) {
        return a <= b || Math.abs(a - b) < 0.0001;
    }

    /*
    public void growSideRingParticles(SynchronizedLocation callback, double radius) {
        AtomicDouble atomicPhi = new AtomicDouble(0);
        double deltaPhi = getDeltaThetaForCircle(radius, 2 * Math.PI, spacingBetweenParticles);
        TaskRepeater.runUntilCancel(new TaskRepeater.SelfCancellingTask() {
            @Override
            public boolean onTick() {
                Location location = callback.getSynchronizedCentralLocation();
                double particlePhi = atomicPhi.getAndAdd(deltaPhi);
                Vector particleOffset = getSideRingParticleOffset(location, radius, particlePhi);
                emitter.emitParticle(location.add(particleOffset));
                return false;
            }
        }, emitter.getMillisBetweenGrowthParticles(), TickConverter.getTicksFromSeconds(5));
    }
    */

    public ShapeCreator getPreconfiguredShapeCreator(Shape shape, double radius) {
        List<Vector> offsets = shape.getOffsets(radius, spacingBetweenParticles);
        return ShapeCreator.fromQueuedOffsets(emitter, offsets);
    }

    private Vector getSideRingParticleOffset(Location location, double radius, double particlePhi) {
        double entityTheta = LocationUtil.getRadiansFromYaw(location.getYaw());
        double particleTheta = entityTheta + (Math.PI / 2);
        return LocationUtil.getVectorFromSphericalCoordinates(radius, particleTheta, particlePhi);
    }

    public void emitSideRingParticles(Location location, double radius) {
        for (double phi = 0; phi <= ((7 * Math.PI / 8) + .01); phi += (Math.PI / 8)) { // .01 helps any precision errors
            Vector particleOffset = getSideRingParticleOffset(location, radius, phi);
            Location particleLocation = location.add(particleOffset);
            emitter.emitParticleOnCurrentThread(particleLocation);
        }
    }


    public ShapeCreator getTargettedLineCreator(Location from, Location to) {

        Vector lineVector = to.toVector().subtract(from.toVector());
        double distance = lineVector.length();
        int numChildVectors = (int) (distance / spacingBetweenParticles);

        double deltaX = lineVector.getX() / numChildVectors;
        double deltaY = lineVector.getY() / numChildVectors;
        double deltaZ = lineVector.getZ() / numChildVectors;

        List<Vector> particleOffsets = new LinkedList<>();
        for (int i = 0; i < numChildVectors; i++) {
            double multiplier = (i / numChildVectors);
            Vector offset = new Vector(deltaX * multiplier, deltaY * multiplier, deltaZ * multiplier);
            particleOffsets.add(offset);
        }

        return ShapeCreator.fromQueuedOffsets(emitter, particleOffsets);

    }




}

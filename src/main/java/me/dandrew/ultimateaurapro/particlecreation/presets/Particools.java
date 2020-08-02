/*
 * Copyright (c) 2020. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package me.dandrew.ultimateaurapro.particlecreation.presets;

import com.google.common.util.concurrent.AtomicDouble;
import me.dandrew.ultimateaurapro.particlecreation.AsyncEmitter;
import me.dandrew.ultimateaurapro.util.LocationUtil;
import me.dandrew.ultimateaurapro.util.TaskRepeater;
import me.dandrew.ultimateaurapro.util.TickConverter;
import me.dandrew.ultimateaurapro.util.math.DifferentiableFunction;
import me.dandrew.ultimateaurapro.util.math.NewtonRaphson;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * This class is responsible for creating ShapeCreators representing common shapes
 */
public class Particools {

    private AsyncEmitter emitter;
    private double spacingBetweenParticles;

    private Particools(AsyncEmitter emitter, double spacingBetweenParticles) {
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

    public ShapeCreator getHelixCreator(double radius, double height, double distanceBetweenLoops) {

        AtomicDouble atomicTheta = new AtomicDouble(0);
        Queue<Vector> queuedParticleOffsets = new LinkedList<>();
        double deltaTheta = getThetaIncrementationValue(radius, 2 * Math.PI,  spacingBetweenParticles);
        while (true) {


            double deltaX = radius * Math.cos(atomicTheta.get());
            double deltaY = distanceBetweenLoops * atomicTheta.get() / (2.00 * Math.PI); // every revolution, deltaY += distanceBetweenLoops
            double deltaZ = radius * Math.sin(atomicTheta.get());

            Vector particleOffset = new Vector(deltaX, deltaY, deltaZ);
            queuedParticleOffsets.add(particleOffset);

            if (deltaY >= height) {
                break;
            }

            atomicTheta.addAndGet(deltaTheta);
        }

        return ShapeCreator.fromQueuedOffsets(emitter, queuedParticleOffsets);

    }

    /**
     * Gets how much theta should be incremented if one was to divide a circle into multiple segments, each of
     * distanceBetweenEndPoints length. The last segment will end where the first segment began
     */
    private static double getThetaIncrementationValue(double radius, double totalRadians, double distanceBetweenEndPoints) {
        // s = r * theta
        double arcLength = radius * totalRadians;
        long piecesOfCircle = Math.round(arcLength / distanceBetweenEndPoints);
        return totalRadians / piecesOfCircle;
    }

    public ShapeCreator getSphereCreator(double radius) {
        Queue<Vector> particleOffsetsForSphere = getParticleOffsetsForSphere(radius);
        return ShapeCreator.fromQueuedOffsets(emitter, particleOffsetsForSphere);
    }

    private Queue<Vector> getParticleOffsetsForSphere(double radius) {
        Queue<Vector> offsets = new LinkedList<>();
        for (Map.Entry<Double, Double> yOffsetRadiusEntry : getHollowSphereCircleOffsetRadiusMap(radius).entrySet()) {
            double circleYOffset = yOffsetRadiusEntry.getKey();
            double circleRadius = yOffsetRadiusEntry.getValue();

            for (Vector xAndZOffset : getCircleParticleOffsets(circleRadius)) {
                Vector particleOffset = xAndZOffset.setY(circleYOffset);
                offsets.add(particleOffset);
            }

        }
        return offsets;
    }

    /**
     * Returns a map for circle layers creating a sphere.
     * K: Y Offset for the center of a circle
     * V: Radius of that circle
     */
    private Map<Double, Double> getHollowSphereCircleOffsetRadiusMap(double radius) {
        Map<Double, Double> circleLocationRadiusMap = new LinkedHashMap<>();
        double deltaPhi = getThetaIncrementationValue(radius, Math.PI, spacingBetweenParticles);
        for (double phi = 0; isLessThanOrEqualTo(phi, Math.PI); phi += deltaPhi) {
            Vector firstEndPointVector = LocationUtil.getVectorFromSphericalCoordinates(radius, 0, phi);
            double iteratedCircleRadius
                    = Math.sqrt(Math.pow(firstEndPointVector.getX(), 2) + Math.pow(firstEndPointVector.getZ(), 2));
            double deltaY = firstEndPointVector.getY();
            circleLocationRadiusMap.put(deltaY, iteratedCircleRadius);
        }
        return circleLocationRadiusMap;
    }

    public ShapeCreator getCircleCreator(double radius) {
        return ShapeCreator.fromQueuedOffsets(emitter, getCircleParticleOffsets(radius));
    }

    /**
     * Returns the offsets for a flat circle in terms of x & z, but not Y.
     */
    private Queue<Vector> getCircleParticleOffsets(double radius) {
        Queue<Vector> vectors = new LinkedList<>();
        double deltaTheta = getThetaIncrementationValue(radius, 2 * Math.PI,  spacingBetweenParticles);
        for (double theta = 0; isLessThanOrEqualTo(theta, 2 * Math.PI); theta += deltaTheta) {
            double deltaX = radius * Math.cos(theta);
            double deltaZ = radius * Math.sin(theta);
            Vector vector = new Vector(deltaX, 0, deltaZ);
            vectors.add(vector);
        }
        return vectors;
    }

    private static boolean isLessThanOrEqualTo(double a, double b) {
        return a <= b || Math.abs(a - b) < 0.0001;
    }

    /*
    public void growSideRingParticles(SynchronizedLocation callback, double radius) {
        AtomicDouble atomicPhi = new AtomicDouble(0);
        double deltaPhi = getThetaIncrementationValue(radius, 2 * Math.PI, spacingBetweenParticles);
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

    public ShapeCreator getStarCreator(double radius) {
        Queue<Vector> starOffsets = getStarOffsets(radius);
        return ShapeCreator.fromQueuedOffsets(emitter, starOffsets);
    }

    private Queue<Vector> getStarOffsets(double radius) {

        Queue<Vector> starOffsets = new LinkedList<>();
        double[] anglesOfAStar = new double[]{306, 90, 234, 18, 162}; // each point has a degree change of (360/5) = 72.
        for (int i = 0; i < anglesOfAStar.length; i++) {
            double degrees = anglesOfAStar[i];
            anglesOfAStar[i] = Math.toRadians(degrees);
        }

        for (int i = 0; i < anglesOfAStar.length; i++) {

            double theta = anglesOfAStar[i % anglesOfAStar.length];
            double xInitial = radius * Math.sin(theta); // dependent variable
            double zInitial = radius * Math.cos(theta); // independent variable

            Vector cornerOffset = new Vector(xInitial, 0, zInitial);
            starOffsets.add(cornerOffset);

            double nextTheta = anglesOfAStar[(i + 1) % anglesOfAStar.length];
            double xFinal = radius * Math.sin(nextTheta);
            double zFinal = radius * Math.cos(nextTheta);
            double slope = ( (xFinal - xInitial) / (zFinal - zInitial)); // delta X / deltaZ

            double deltaZ = zFinal - zInitial;

            double length = Math.sqrt( Math.pow(xFinal - xInitial, 2) + Math.pow(zFinal - zInitial, 2) );
            int zPieces = (int) (length / spacingBetweenParticles);
            double zIncrementor = deltaZ / zPieces;

            for (int zPiece = 0; zPiece < zPieces; zPiece++) {
                double zOffset = zIncrementor * zPiece;
                double xOffset = slope * zOffset; // slope * z (x = mz)
                Vector particleOffset = new Vector(xOffset, 0, zOffset);
                Vector relativeParticleOffset = cornerOffset.clone().add(particleOffset); // wrt corner
                starOffsets.add(relativeParticleOffset);
            }

        }

        return starOffsets;

    }

    public ShapeCreator getLineCreator(double radius) {
        Queue<Vector> lineOffsets = getLineOffsets(radius);
        return ShapeCreator.fromQueuedOffsets(emitter, lineOffsets);
    }

    private Queue<Vector> getLineOffsets(double radius) {
        Queue<Vector> lineOffsets = new LinkedList<>();
        double adjustedSpaceBetweenParticles = getAdjustedSpaceBetweenParticles(radius);
        double particleRadius = 0;

        int numParticles = (int) (radius / spacingBetweenParticles);
        for (int i = 0; i < numParticles; i++) {
            Vector particleOffset = new Vector(particleRadius, 0, 0);
            lineOffsets.add(particleOffset);
            particleRadius += adjustedSpaceBetweenParticles;
        }
        return lineOffsets;
    }

    /**
     * Returns an adjusted "spacingBetweenParticles" variable, so that the spacing is in increments
     * that, if repeatedly added together, will match perfectly with the provided arc length's distance.
     */
    private double getAdjustedSpaceBetweenParticles(double arcLength) {
        int numSegments = (int) (arcLength / spacingBetweenParticles);
        return arcLength / numSegments;
    }

    public ShapeCreator getWhirlCreator(double radius, int whirlPieces) {
        Queue<Vector> whirlOffsets = getWhirlOffsets(radius, whirlPieces);
        return ShapeCreator.fromQueuedOffsets(emitter, whirlOffsets);
    }

    private Queue<Vector> getWhirlOffsets(double radius, int whirlPieces) {

        // r = polarEqMultiplier * theta

        double polarEqMultiplier = 1; // basically determines the curvature of the whirl. lower = more curve.
        double finalTheta = radius / polarEqMultiplier; // this ensures that the radius is preserved at end of whirl piece

        double whirlPieceArcLength = calculateWhirlArcLengthFromZero(polarEqMultiplier, finalTheta);
        double distanceBetweenParticles = getAdjustedSpaceBetweenParticles(whirlPieceArcLength);




        Queue<Vector> particleOffsets = new LinkedList<>();
        particleOffsets.add(new Vector(0, 0 ,0));

        double currentTheta = getNextUpperBound(polarEqMultiplier, distanceBetweenParticles, 0, finalTheta);
        boolean lastIteration = false;
        while (!lastIteration) {

            if (finalTheta - currentTheta < 0.0001 || currentTheta > finalTheta) {
                currentTheta = finalTheta;
                lastIteration = true;
            }

            double currentRadius = polarEqMultiplier * currentTheta;

            double deltaX = currentRadius * Math.sin(currentTheta);
            double deltaZ = currentRadius * Math.cos(currentTheta);
            Vector offset = new Vector(deltaX, 0, deltaZ);

            for (int whirlPieceNumber = 0; whirlPieceNumber < whirlPieces; whirlPieceNumber++) {

                if (whirlPieceNumber == 0) {
                    particleOffsets.add(offset);
                }
                else {
                    double deltaThetaPerPieceNumber = Math.PI * 2.00 / whirlPieces * 1.00;
                    double thetaOffset = deltaThetaPerPieceNumber * whirlPieceNumber;
                    double rotatedDeltaX = currentRadius * Math.sin(currentTheta + thetaOffset);
                    double rotatedDeltaZ = currentRadius * Math.cos(currentTheta + thetaOffset);
                    Vector rotatedOffset = new Vector(rotatedDeltaX, 0, rotatedDeltaZ);
                    particleOffsets.add(rotatedOffset);
                }

            }

            currentTheta = getNextUpperBound(polarEqMultiplier, distanceBetweenParticles, currentTheta, finalTheta);

        }

        return particleOffsets;

    }

    private double calculateWhirlArcLengthFromZero(double polarEqMultiplier, double theta) {
        return (polarEqMultiplier / 2.00) * calculateWhirlArcLengthAntiderivativeInnerBracket(theta);
    }

    private double calculateWhirlArcLengthAntiderivativeInnerBracket(double theta) {
        double duplicateValue = Math.sqrt( (theta * theta) + 1.00 );
        return Math.log( Math.abs( duplicateValue + theta ) )
                + ( theta * duplicateValue );
    }

    private double getNextUpperBound(double polarEqMultiplier, double desiredArcLength, double lowerBound, double finalTheta) {

        /*
            Basically, we're solving for the root, or the upper bound.
            We use the arc length formula through integration for polar curves
            Next, the fundamental theorem of calculus is used, with the answer being set to desiredArcLength.
            Finally, the equation is rearranged to make it so the unknown upper bound is a root.
            I believe the root can only be solved using numerical methods.
            The Newton Raphson Method would be a good candidate since the
            resulting equations "sort of" looks like a line.
         */


        double firstTerm = 2.00 * desiredArcLength / polarEqMultiplier;
        double innerBracketAtLowerBound = calculateWhirlArcLengthAntiderivativeInnerBracket(lowerBound);
        double allTheKnowns = firstTerm + innerBracketAtLowerBound;
        DifferentiableFunction function = new DifferentiableFunction() {
            @Override
            public double getBase(double theta) {
                return calculateWhirlArcLengthAntiderivativeInnerBracket(theta) - allTheKnowns;
            }

            @Override
            public double getDerivative(double theta) {
                return 2 * Math.sqrt((theta * theta) + 1.00);
            }
        };

        return NewtonRaphson.solve(function, finalTheta + 0.01);

    }

    public static class Builder {

        private Map<Color, Number> colorFrequencyMap = new HashMap<>();
        private double secondsBetweenGrowthParticles = 0.05;
        private int particlesPerTick = 1;
        private int particleThickness = 1;
        private double spacingBetweenParticles = 1;

        public Builder setColorProbabilityWeight(Color color, int probabilityWeight) {
            colorFrequencyMap.put(color, probabilityWeight);
            return this;
        }

        public Builder setSecondsBetweenGrowthParticles(double seconds) {
            this.secondsBetweenGrowthParticles = seconds;
            return this;
        }

        public Builder setGrowthParticlesPerTick(int particlesPerTick) {
            this.particlesPerTick = particlesPerTick;
            return this;
        }

        public Builder setParticleThickness(int particleThickness) {
            this.particleThickness = particleThickness;
            return this;
        }

        public Builder setSpacingBetweenParticles(double spacingBetweenParticles) {
            this.spacingBetweenParticles = spacingBetweenParticles;
            return this;
        }

        public Particools build() {

            if (colorFrequencyMap.size() == 0) {
                throw new IllegalStateException("At least one color is needed.");
            }

            if (particleThickness < 1) {
                throw new IllegalStateException("Particle thickness must be greater than or equal to 1");
            }

            if (secondsBetweenGrowthParticles < 0) {
                throw new IllegalStateException("Invalid seconds between growth particles. Must be greater than 0");
            }

            if (spacingBetweenParticles <= 0) {
                throw new IllegalStateException("Spacing between particles should be greater than 0");
            }

            if (particlesPerTick < 1) {
                throw new IllegalStateException("Particles per tick must be greater than or equal to 1");
            }

            doSafetyCheck();


            List<Color> colors = getColorList(colorFrequencyMap);
            AsyncEmitter emitter = new AsyncEmitter(colors, particleThickness, secondsBetweenGrowthParticles, particlesPerTick);
            return new Particools(emitter, spacingBetweenParticles);

        }

        private void doSafetyCheck() {
            // Better safe than sorry. There should be no reason for anything smaller other than stress testing.
            spacingBetweenParticles = Math.max(spacingBetweenParticles, 0.01);
        }

        private List<Color> getColorList(Map<Color, Number> colorFrequencyMap) {
            List<Color> colors = new ArrayList<>();
            for (Map.Entry<Color, Number> colorNumberEntry : colorFrequencyMap.entrySet()) {
                Color color = colorNumberEntry.getKey();
                int colorRepeats = colorNumberEntry.getValue().intValue();
                for (int i = 0; i < colorRepeats; i++) {
                    colors.add(color);
                }
            }
            return colors;
        }

    }

    public ShapeCreator getTargettedLineCreator(Location from, Location to) {

        Vector lineVector = to.toVector().subtract(from.toVector());
        double distance = lineVector.length();
        int numChildVectors = (int) (distance / spacingBetweenParticles);

        double deltaX = lineVector.getX() / numChildVectors;
        double deltaY = lineVector.getY() / numChildVectors;
        double deltaZ = lineVector.getZ() / numChildVectors;

        Queue<Vector> particleOffsets = new LinkedList<>();
        for (int i = 0; i < numChildVectors; i++) {
            double multiplier = (i / numChildVectors);
            Vector offset = new Vector(deltaX * multiplier, deltaY * multiplier, deltaZ * multiplier);
            particleOffsets.add(offset);
        }

        return ShapeCreator.fromQueuedOffsets(emitter, particleOffsets);

    }




}

package me.dandrew.ultimateaurapro.particlecreation.presets.shapes;

import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.util.Vector;

import java.util.LinkedList;
import java.util.List;

public class ShapeHelix implements Shape {

    private double height;
    private double distanceBetweenLoops;

    public ShapeHelix(double height, double distanceBetweenLoops) {
        this.height = height;
        this.distanceBetweenLoops = distanceBetweenLoops;
    }

    @Override
    public List<Vector> getOffsets(double radius, double spacingBetweenParticles) {

        AtomicDouble atomicTheta = new AtomicDouble(0);
        List<Vector> queuedParticleOffsets = new LinkedList<>();
        double deltaTheta = MathHelp.getDeltaThetaForCircle(radius, 2 * Math.PI,  spacingBetweenParticles);
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

        return queuedParticleOffsets;

    }

}

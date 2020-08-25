package me.dandrew.ultimateaurapro.particlecreation.presets.shapes;

import org.bukkit.util.Vector;

import java.util.LinkedList;
import java.util.Queue;

public class ShapeCircle implements Shape {

    @Override
    public Queue<Vector> getOffsets(double radius, double spacingBetweenParticles) {
        Queue<Vector> vectors = new LinkedList<>();
        double deltaTheta = MathHelp.getDeltaThetaForCircle(radius, 2 * Math.PI,  spacingBetweenParticles);
        for (double theta = 0; MathHelp.isLessThanOrEqualTo(theta, 2 * Math.PI); theta += deltaTheta) {
            double deltaX = radius * Math.cos(theta);
            double deltaZ = radius * Math.sin(theta);
            Vector vector = new Vector(deltaX, 0, deltaZ);
            vectors.add(vector);
        }
        return vectors;
    }

}

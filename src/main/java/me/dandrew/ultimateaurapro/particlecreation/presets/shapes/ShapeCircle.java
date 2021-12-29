package me.dandrew.ultimateaurapro.particlecreation.presets.shapes;

import org.bukkit.util.Vector;

import java.util.LinkedList;
import java.util.List;

public class ShapeCircle implements Shape {

    @Override
    public List<Vector> getOffsets(double radius, double spacingBetweenParticles) {
        List<Vector> vectors = new LinkedList<>();
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

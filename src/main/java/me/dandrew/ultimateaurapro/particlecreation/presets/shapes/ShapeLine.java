package me.dandrew.ultimateaurapro.particlecreation.presets.shapes;

import org.bukkit.util.Vector;

import java.util.LinkedList;
import java.util.Queue;

public class ShapeLine implements Shape {

    @Override
    public Queue<Vector> getOffsets(double radius, double spacingBetweenParticles) {
        Queue<Vector> lineOffsets = new LinkedList<>();
        double adjustedSpaceBetweenParticles = MathHelp.getAdjustedSpaceBetweenParticles(radius, spacingBetweenParticles);
        double particleRadius = 0;

        int numParticles = (int) (radius / spacingBetweenParticles);
        for (int i = 0; i < numParticles; i++) {
            Vector particleOffset = new Vector(particleRadius, 0, 0);
            lineOffsets.add(particleOffset);
            particleRadius += adjustedSpaceBetweenParticles;
        }
        return lineOffsets;
    }
}

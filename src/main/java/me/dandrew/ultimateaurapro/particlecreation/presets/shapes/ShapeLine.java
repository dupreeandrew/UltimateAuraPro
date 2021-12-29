package me.dandrew.ultimateaurapro.particlecreation.presets.shapes;

import me.dandrew.ultimateaurapro.util.VectorRotator;
import org.bukkit.util.Vector;

import java.util.LinkedList;
import java.util.List;

public class ShapeLine implements Shape {

    private double angDeg;

    public ShapeLine() {
        this.angDeg = 0.00;
    }

    public ShapeLine(double angDeg) {
        this.angDeg = angDeg;
    }

    @Override
    public List<Vector> getOffsets(double radius, double spacingBetweenParticles) {
        List<Vector> lineOffsets = new LinkedList<>();
        double adjustedSpaceBetweenParticles = MathHelp.getAdjustedSpaceBetweenParticles(radius, spacingBetweenParticles);
        double particleRadius = 0;

        int numParticles = (int) (radius / spacingBetweenParticles);
        for (int i = 0; i < numParticles; i++) {
            Vector particleOffset = new Vector(particleRadius, 0, 0);
            lineOffsets.add(particleOffset);
            particleRadius += adjustedSpaceBetweenParticles;
        }

        checkForRotation(lineOffsets);

        return lineOffsets;
    }

    private void checkForRotation(List<Vector> lineOffsets) {
        if (angDeg != 0.00) {
            VectorRotator.changeOrientationOfOffsets(lineOffsets, angDeg, 0.00);
        }
    }
}

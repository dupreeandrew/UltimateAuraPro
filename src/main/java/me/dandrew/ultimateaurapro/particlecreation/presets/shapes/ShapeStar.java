package me.dandrew.ultimateaurapro.particlecreation.presets.shapes;

import org.bukkit.util.Vector;

import java.util.LinkedList;
import java.util.List;

public class ShapeStar implements Shape {

    @Override
    public List<Vector> getOffsets(double radius, double spacingBetweenParticles) {

        List<Vector> starOffsets = new LinkedList<>();
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
            double slope = ( (xFinal - xInitial) / (zFinal - zInitial) ); // delta X / deltaZ

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
}

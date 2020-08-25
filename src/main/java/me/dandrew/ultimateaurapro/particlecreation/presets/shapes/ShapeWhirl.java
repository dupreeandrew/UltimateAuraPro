package me.dandrew.ultimateaurapro.particlecreation.presets.shapes;

import me.dandrew.ultimateaurapro.util.math.DifferentiableFunction;
import me.dandrew.ultimateaurapro.util.math.NewtonRaphson;
import org.bukkit.util.Vector;

import java.util.LinkedList;
import java.util.Queue;

public class ShapeWhirl implements Shape {

    private int whirlPieces;

    public ShapeWhirl(int whirlPieces) {
        this.whirlPieces = whirlPieces;
    }

    @Override
    public Queue<Vector> getOffsets(double radius, double spacingBetweenParticles) {

        // r = polarEqMultiplier * theta

        double polarEqMultiplier = 1; // basically determines the curvature of the whirl. lower = more curve.
        double finalTheta = radius / polarEqMultiplier; // this ensures that the radius is preserved at end of whirl piece

        double whirlPieceArcLength = getArcLength(polarEqMultiplier, finalTheta);
        double distanceBetweenParticles = MathHelp.getAdjustedSpaceBetweenParticles(whirlPieceArcLength, whirlPieceArcLength);

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

    private double getArcLength(double polarEqMultiplier, double theta) {
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

        if (Math.abs(finalTheta - lowerBound) < 0.0001) {
            return finalTheta;
        }


        double arcLengthTerm = 2.00 * desiredArcLength / polarEqMultiplier;
        double innerBracketAtLowerBound = calculateWhirlArcLengthAntiderivativeInnerBracket(lowerBound);
        double allTheKnowns = arcLengthTerm + innerBracketAtLowerBound;
        DifferentiableFunction function = new DifferentiableFunction() {
            @Override
            public double getBase(double theta) {
                // all the knowns = (equation with only unknowns, theta)
                // we can find the unknown, theta, by setting the overall equation to zero and solving the root.
                return calculateWhirlArcLengthAntiderivativeInnerBracket(theta) - allTheKnowns;
            }

            @Override
            public double getDerivative(double theta) {
                return 2.00 * Math.sqrt((theta * theta) + 1.00);
            }
        };

        return NewtonRaphson.solve(function, finalTheta + 0.01);

    }
}

package me.dandrew.ultimateaurapro.particlecreation.presets.shapes;

import me.dandrew.ultimateaurapro.util.LocationUtil;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class ShapeSphere implements Shape {

    @Override
    public Queue<Vector> getOffsets(double radius, double spacingBetweenParticles) {
        Queue<Vector> offsets = new LinkedList<>();
        for (Map.Entry<Double, Double> yOffsetRadiusEntry : getHollowSphereCircleOffsetRadiusMap(radius, spacingBetweenParticles).entrySet()) {
            double circleYOffset = yOffsetRadiusEntry.getKey();
            double circleRadius = yOffsetRadiusEntry.getValue();

            for (Vector xAndZOffset : new ShapeCircle().getOffsets(circleRadius, spacingBetweenParticles)) {
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
    private Map<Double, Double> getHollowSphereCircleOffsetRadiusMap(double radius, double spacingBetweenParticles) {
        Map<Double, Double> circleLocationRadiusMap = new LinkedHashMap<>();
        double deltaPhi = MathHelp.getDeltaThetaForCircle(radius, Math.PI, spacingBetweenParticles);
        for (double phi = 0; MathHelp.isLessThanOrEqualTo(phi, Math.PI); phi += deltaPhi) {
            Vector firstEndPointVector = LocationUtil.getVectorFromSphericalCoordinates(radius, 0, phi);
            double iteratedCircleRadius
                    = Math.sqrt(Math.pow(firstEndPointVector.getX(), 2) + Math.pow(firstEndPointVector.getZ(), 2));
            double deltaY = firstEndPointVector.getY();
            circleLocationRadiusMap.put(deltaY, iteratedCircleRadius);
        }
        return circleLocationRadiusMap;
    }

}

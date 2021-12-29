package me.dandrew.ultimateaurapro.util;

import org.bukkit.util.Vector;

public class VectorRotator {

    public static void changeOrientationOfOffsets(Iterable<Vector> offsets, double degreesRotation, double degreesTilt) {

        final double RADS_ROTATION = Math.toRadians(degreesRotation);
        final double ROTATION_SIN_VALUE = Math.sin(RADS_ROTATION);
        final double ROTATION_COS_VALUE = Math.cos(RADS_ROTATION);

        final double RADS_TILT = Math.toRadians(degreesTilt);
        final double TILT_SIN_VALUE = Math.sin(RADS_TILT);
        final double TILT_COS_VALUE = Math.cos(RADS_TILT);

        for (Vector offset : offsets) {
            if (Math.abs(RADS_ROTATION) > 0.00001) {
                // rotate around Y, with pre-calculated trig values
                rotateAroundY(offset, ROTATION_SIN_VALUE, ROTATION_COS_VALUE);
            }

            if (Math.abs(RADS_TILT) > 0.00001) {
                // rotate around Z, with pre-calculated trig values
                rotateAroundZ(offset, TILT_SIN_VALUE, TILT_COS_VALUE);
            }
        }

    }

    /**
     * Rotates a vector around Y axis given pre-calculated trig values
     */
    public static void rotateAroundY(Vector vector, double sinVal, double cosVal) {
        double newX = cosVal * vector.getX() + sinVal * vector.getZ();
        double newZ = -sinVal * vector.getX() + cosVal * vector.getZ();
        vector.setX(newX).setZ(newZ);
    }

    /**
     * Rotates a vector around Z axis given pre-calculated trig values
     */
    public static void rotateAroundZ(Vector vector, double sinVal, double cosVal) {
        double newX = cosVal * vector.getX() - sinVal * vector.getY();
        double newY = sinVal * vector.getX() + cosVal * vector.getY();
        vector.setX(newX).setY(newY);
    }

}

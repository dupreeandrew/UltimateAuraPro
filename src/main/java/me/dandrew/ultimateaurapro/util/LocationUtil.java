package me.dandrew.ultimateaurapro.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class LocationUtil {
    public static double getXandZDistance(Location loc1, Location loc2) {
        double xParam = Math.pow(loc1.getX() - loc2.getX(), 2);
        double zParam = Math.pow(loc1.getZ() - loc2.getZ(), 2);
        return Math.sqrt(xParam + zParam);
    }

    /**
     * @param radius radius of vector
     * @param theta similar? to yaw
     * @param phi similar? to pitch
     */
    public static Vector getVectorFromSphericalCoordinates(double radius, double theta, double phi) {
        double deltaY = radius * Math.cos(phi);
        double xAndZMagnitude = radius * Math.sin(phi);

        theta += (Math.PI / 2); // this is necessary because minecraft coordinates are weird..
        double deltaX = xAndZMagnitude * Math.sin(theta);
        double deltaZ = xAndZMagnitude * Math.cos(theta);
        return new Vector(deltaX, deltaY, deltaZ);
    }

    public static double getRadiansFromYaw(double yaw) {
        double degrees = getDegreesFromYaw(yaw);
        return Math.toRadians(degrees);
    }

    public static double getDegreesFromYaw(double yaw) {
        // At south direction, yaw is 0. Rotating clockwise makes it positive.
        // Below will ensure the yaw is in actual degrees between [0,360], starting at the East.
        double counterClockwiseYaw = 360.00 - yaw;
        double positiveOrNegativeDegrees = counterClockwiseYaw - 90.00;
        return (positiveOrNegativeDegrees < 0) ? 360.00 + positiveOrNegativeDegrees : positiveOrNegativeDegrees % 360.00;
    }

    public static Queue<Vector> cloneOffsets(Iterable<Vector> offsets) {
        Queue<Vector> clonedQueue = new LinkedList<>();
        for (Vector offset : offsets) {
            Vector offsetClone = offset.clone();
            clonedQueue.add(offsetClone);
        }
        return clonedQueue;
    }

    public static Queue<Iterable<Vector>> cloneSetsOfOffsets(Queue<? extends Iterable<Vector>> setsOfOffsets) {
        Queue<Iterable<Vector>> setsOfOffsetsClone = new LinkedList<>();
        for (Iterable<Vector> setOfOffset : setsOfOffsets) {
            List<Vector> offsetList = new ArrayList<>();
            for (Vector offset : setOfOffset) {
                Vector offsetCopy = offset.clone();
                offsetList.add(offsetCopy);
            }
            setsOfOffsetsClone.add(offsetList);
        }
        return setsOfOffsetsClone;

    }

    @Nullable
    public static LivingEntity getEntityInLineOfSight(LivingEntity looker, double boxRadius) {

        List<Entity> nearbyEntities = looker.getNearbyEntities(boxRadius, boxRadius, boxRadius);
        Vector playerVector = looker.getEyeLocation().toVector();
        Vector playerDirection = looker.getEyeLocation().getDirection();

        LivingEntity candidateEntity = null;
        double candidateDistance = 999;

        for (Entity nearbyEntity : nearbyEntities) {

            if (!(nearbyEntity instanceof LivingEntity)) {
                continue;
            }

            double heightIncrementor = nearbyEntity.getHeight() / 2.00;
            Vector entityVector = nearbyEntity.getLocation().add(0, heightIncrementor, 0).toVector();
            Vector destVector = entityVector.subtract(playerVector);
            double angRad = destVector.angle(playerDirection);


            double distance = destVector.length();
            if (angRad > getAllowedDegrees(distance)) {
                continue;
            }

            if (distance < candidateDistance) {
                candidateEntity = (LivingEntity) nearbyEntity;
                candidateDistance = distance;
            }

        }

        return candidateEntity;

    }

    private static double getAllowedDegrees(double distance) {
        double oneDegreeInRads = Math.PI / 180.0;
        double maxRads = oneDegreeInRads * 80;
        double minRads = oneDegreeInRads * 15;

        // Basically, drop the allowed degrees by 35 for every unit block distance, but minimum allowed degrees is 15.
        return Math.max(maxRads - (35.0 * oneDegreeInRads * distance), minRads);

    }



}

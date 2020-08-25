package me.dandrew.ultimateaurapro.particlecreation.presets.shapes;

class MathHelp {

    /**
     * Gets how much theta should be incremented if one was to divide a circle into multiple segments, each of
     * distanceBetweenEndPoints length. The last segment will end where the first segment began
     */
    static double getDeltaThetaForCircle(double radius, double totalRadians, double distanceBetweenEndPoints) {
        // s = r * theta
        double arcLength = radius * totalRadians;
        long piecesOfCircle = Math.round(arcLength / distanceBetweenEndPoints);
        return totalRadians / piecesOfCircle;
    }

    static boolean isLessThanOrEqualTo(double a, double b) {
        return a <= b || Math.abs(a - b) < 0.0001;
    }

    /**
     * Returns an adjusted "spacingBetweenParticles" variable, so that the spacing is in increments
     * that, if repeatedly added together, will match perfectly with the provided arc length's distance.
     */
    static double getAdjustedSpaceBetweenParticles(double arcLength, double spacingBetweenParticles) {
        int numSegments = (int) (arcLength / spacingBetweenParticles);
        return arcLength / numSegments;
    }



}

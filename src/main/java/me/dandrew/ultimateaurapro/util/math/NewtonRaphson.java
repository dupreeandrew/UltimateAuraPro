package me.dandrew.ultimateaurapro.util.math;

public class NewtonRaphson {

    private static final double MIN_PRECISION = 0.00001;

    public static double solve(DifferentiableFunction function, double guess) {
        double deltaX = function.getBase(guess) / function.getDerivative(guess);
        while (Math.abs(deltaX) >= MIN_PRECISION)
        {
            deltaX = function.getBase(guess) / function.getDerivative(guess);

            // x(i+1) = x(i) - f(x) / f'(x)
            guess = guess - deltaX;
        }
        return guess;
    }

}

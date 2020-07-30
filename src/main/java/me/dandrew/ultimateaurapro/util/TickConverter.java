package me.dandrew.ultimateaurapro.util;

public class TickConverter {
    public static int getTicksFromSeconds(double seconds) {
        return (int) (20.0 * seconds);
    }
}

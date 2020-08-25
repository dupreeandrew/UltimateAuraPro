package me.dandrew.ultimateaurapro.particlecreation.presets.shapes;

import org.bukkit.util.Vector;

import java.util.Queue;

public interface Shape {
    Queue<Vector> getOffsets(double radius, double spacingBetweenParticles);
}

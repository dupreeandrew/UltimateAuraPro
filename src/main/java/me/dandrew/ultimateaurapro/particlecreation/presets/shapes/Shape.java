package me.dandrew.ultimateaurapro.particlecreation.presets.shapes;

import org.bukkit.util.Vector;

import java.util.List;

public interface Shape {
    List<Vector> getOffsets(double radius, double spacingBetweenParticles);
}

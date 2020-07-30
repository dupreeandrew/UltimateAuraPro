package me.dandrew.ultimateaurapro.particlecreation;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

public class EmitterTools {

    public static void emitParticle(Location location, Color color, int thickness) {
        location.getWorld().spawnParticle(Particle.REDSTONE, location, thickness, new Particle.DustOptions(color, 1));
    }

}

package me.dandrew.ultimateaurapro.particlecreation;

import org.bukkit.Location;

public interface GrowthListener {
    Location getSynchronizedCentralLocation();
    void onFinish();
}

package me.dandrew.ultimateaurapro.auragiving;

import me.dandrew.ultimateaurapro.util.TickConverter;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.Collections;
import java.util.List;

public class AuraEffect {

    private AuraTarget auraTarget;
    private double radius;
    private List<PotionEffect> potionEffectList;
    private List<PlayerCommandEffect> commandEffectList;

    public AuraEffect(AuraTarget auraTarget, double radius, List<PotionEffect> potionEffectList, List<PlayerCommandEffect> commandEffectList) {
        this.auraTarget = auraTarget;
        this.radius = radius;
        this.potionEffectList = Collections.unmodifiableList(potionEffectList);
        this.commandEffectList = Collections.unmodifiableList(commandEffectList);
    }

    public AuraTarget getAuraTarget() {
        return auraTarget;
    }

    public double getRadius() {
        return radius;
    }

    public List<PotionEffect> getPotionEffectList() {
        return potionEffectList;
    }

    public List<PlayerCommandEffect> getCommandEffectList() {
        return commandEffectList;
    }

    public static class PlayerCommandEffect {

        private String baseCommand;
        private long millisecondsUntilRepeat;

        public PlayerCommandEffect(String baseCommand, double secondsUntilRepeat) {
            this.baseCommand = baseCommand;
            this.millisecondsUntilRepeat = TickConverter.getTicksFromSeconds(secondsUntilRepeat);
        }

        public String getRunnableConsoleCommand(Player player) {
            return baseCommand.replace("%username%", player.getName());
        }

        public long getTicksUntilRepeat() {
            return millisecondsUntilRepeat;
        }
    }

}

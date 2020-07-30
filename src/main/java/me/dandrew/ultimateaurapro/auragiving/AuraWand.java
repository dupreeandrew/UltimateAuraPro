package me.dandrew.ultimateaurapro.auragiving;

import me.dandrew.ultimateaurapro.lib.SpecialEntityChecker;
import me.dandrew.ultimateaurapro.util.ItemDb;
import me.dandrew.ultimateaurapro.util.LocationUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class AuraWand implements Listener {

    private static Map<String, LinkedList<LivingEntity>> buffedLivingEntities = new HashMap<>();
    private static final int MAX_ALLOWED_BUFFED_PLAYERS = 5;
    private static final ItemStack auraWandCopy = ItemDb.getAuraWand();

    @EventHandler
    public void onUseAuraWand(PlayerInteractEvent event) {

        switch (event.getAction()) {
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                break;
            default:
                return;
        }

        if (event.getHand() != (EquipmentSlot.HAND)) {
            return;
        }

        Player player = event.getPlayer();

        if (!player.getInventory().getItemInMainHand().equals(auraWandCopy)) {
            return;
        }

        LivingEntity target = LocationUtil.getEntityInLineOfSight(player,12);
        if (target == null) {
            return;
        }

        if (checkSpecialEntity(player, target)) {
            return;
        }


        String targetName = target.getName();
        LinkedList<LivingEntity> buffedLivingEntities = AuraWand.buffedLivingEntities.computeIfAbsent(player.getName(), s -> new LinkedList<>());

        fixBadEntities(buffedLivingEntities);

        String msg;

        if (reallyContainsTarget(buffedLivingEntities, target)) {
            buffedLivingEntities.remove(target);
            int currentSize = buffedLivingEntities.size();
            msg = "&7You removed &e" + targetName + "&7 from your aura wand buffs. " + getSizeMsg(currentSize);
        }
        else {
            buffedLivingEntities.add(target);
            ensureMaxCapacityLimit(player, buffedLivingEntities);
            int currentSize = buffedLivingEntities.size();
            msg = "&7You added &e" + targetName + "&7 to your aura wand buffs. " + getSizeMsg(currentSize);
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));

        event.setCancelled(true);

    }

    private static boolean checkSpecialEntity(Player player, LivingEntity target) {


        if (SpecialEntityChecker.checkInvulnerableCitizenNPC(target)) {
            player.sendMessage(ChatColor.YELLOW + "That NPC is invulnerable.");
            return true;
        }

        if (SpecialEntityChecker.checkShopkeeper(target)) {
            player.sendMessage(ChatColor.YELLOW + "You can not use your Aura Wand on this target.");
            return true;
        }

        return false;

    }

    private static void fixBadEntities(LinkedList<LivingEntity> buffedLivingEntities) {

        if (buffedLivingEntities == null) {
            return;
        }

        // Remove non-valid entities
        buffedLivingEntities.removeIf(entity -> !entity.isValid());

        // Ensure players are OK
        for (int i = 0; i < buffedLivingEntities.size(); i++) {
            LivingEntity buffedLivingEntity = buffedLivingEntities.get(i);

            if (buffedLivingEntity instanceof Player) {
                LivingEntity ensuredFixedPlayer = Bukkit.getPlayerExact(buffedLivingEntity.getName());

                if (ensuredFixedPlayer != null) {
                    buffedLivingEntities.set(i, ensuredFixedPlayer);
                }

            }

        }

    }

    private boolean reallyContainsTarget(LinkedList<LivingEntity> buffedLivingEntities, LivingEntity target) {

        if (!(target instanceof Player)) {
            return buffedLivingEntities.contains(target);
        }

        for (LivingEntity buffedLivingEntity : buffedLivingEntities) {
            if (buffedLivingEntity.getName().equals(target.getName())) {
                return true;
            }
        }

        return false;

    }

    private static String getSizeMsg(int currentSize) {
        return "(" + currentSize + "/" + MAX_ALLOWED_BUFFED_PLAYERS + ")";
    }

    private void ensureMaxCapacityLimit(Player player, List<LivingEntity> buffedLivingEntities) {
        if (buffedLivingEntities.size() > MAX_ALLOWED_BUFFED_PLAYERS) {
            String removedName = buffedLivingEntities.get(0).getName();
            String msg = "&e" + removedName + "&7 was removed from your buffs to make space.";
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));

            buffedLivingEntities.remove(0);
        }
    }

    public static Set<LivingEntity> getBuffableLivingEntities(Player auraOwner, double radius) {
        LinkedList<LivingEntity> buffedEntities = buffedLivingEntities.get(auraOwner.getName());

        if (buffedEntities == null) {
            return new HashSet<>();
        }

        fixBadEntities(buffedEntities);

        Set<LivingEntity> nearbyBuffedEntities = new HashSet<>();
        Location auraOwnerLocation = auraOwner.getLocation();
        for (Entity entity : buffedEntities) {

            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            Location entityLocation = entity.getLocation();

            if (!entityLocation.getWorld().equals(auraOwnerLocation.getWorld())) {
                continue;
            }

            // Ensure a spherical radius rather than a cube.
            if (entity.getLocation().distance(auraOwner.getLocation()) > radius) {
                continue;
            }

            nearbyBuffedEntities.add((LivingEntity) entity);

        }

        return nearbyBuffedEntities;
    }

}

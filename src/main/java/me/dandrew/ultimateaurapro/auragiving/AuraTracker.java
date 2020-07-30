package me.dandrew.ultimateaurapro.auragiving;

import me.dandrew.ultimateaurapro.UltimateAuraProPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class AuraTracker implements Listener {

    private static Map<String, List<AssignedAura>> playerAssignedAuraListMap = new HashMap<>();
    private static Set<String> pausedPlayerNames = new HashSet<>();
    private static boolean reloadListenerWasAdded = false;

    public static void giveAura(String playerName, AuraInfo auraInfo) {
        ensureActiveReloadListener();
        removeAuraInfoForPlayer(playerName, auraInfo);

        AssignedAura assignedAura = AssignedAura.activate(playerName, auraInfo);
        List<AssignedAura> playerSettingsList = getPlayerAuraList(playerName);
        playerSettingsList.add(assignedAura);
    }

    private static void ensureActiveReloadListener() {
        if (reloadListenerWasAdded) {
            return;
        }
        UltimateAuraProPlugin.addReloadListener(AuraTracker::deRegisterPlayers);
        reloadListenerWasAdded = true;
    }

    private static void deRegisterPlayers() {

        for (String playerName : new HashSet<>(playerAssignedAuraListMap.keySet())) {
            removeAllAuraInfoForPlayer(playerName);
        }

    }

    private static void removeAllAuraInfoForPlayer(String playerName) {
        List<AssignedAura> playerAuraList = getPlayerAuraList(playerName);
        for (AssignedAura assignedAura : playerAuraList) {
            assignedAura.deactivate();
        }
        playerAuraList.clear();
        playerAssignedAuraListMap.remove(playerName);
    }

    private static List<AssignedAura> getPlayerAuraList(String playerName) {
        return playerAssignedAuraListMap.computeIfAbsent(playerName, s -> new ArrayList<>());
    }

    public static void removeAuraInfoForPlayer(String playerName, AuraInfo auraInfo) {

        List<AssignedAura> assignedAuraList = playerAssignedAuraListMap.get(playerName);

        if (assignedAuraList == null) {
            return;
        }

        for (AssignedAura assignedAura : assignedAuraList) {
            if (assignedAura.getAuraInfo().getId() == auraInfo.getId()) {
                assignedAura.deactivate();
                assignedAuraList.remove(assignedAura);
                break;
            }
        }

        if (assignedAuraList.size() == 0) {
            playerAssignedAuraListMap.remove(playerName);
        }

    }
    
    public static boolean hasAuraInfo(String playerName, AuraInfo auraInfo) {
        List<AssignedAura> assignedAuraList = playerAssignedAuraListMap.get(playerName);

        if (assignedAuraList == null) {
            return false;
        }

        for (AssignedAura assignedAura : assignedAuraList) {
            if (assignedAura.getAuraInfo().getId() == auraInfo.getId()) {
                return true;
            }
        }

        return false;

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String playerName = event.getPlayer().getName();

        if (!playerAssignedAuraListMap.containsKey(playerName)) {
            return;
        }

        pauseAllAuras(playerName);

    }

    public void pauseAllAuras(String playerName) {
        for (AssignedAura assignedAura : getPlayerAuraList(playerName)) {
            assignedAura.deactivate();
        }
        pausedPlayerNames.add(playerName);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String playerName = event.getPlayer().getName();
        resumeAllPausedAuras(playerName);
    }

    private void resumeAllPausedAuras(String playerName) {

        if (!pausedPlayerNames.contains(playerName)) {
            return;
        }

        List<AssignedAura> reactivatedAuraInfoList = new ArrayList<>();
        for (AssignedAura deactivatedAuraInfo : getPlayerAuraList(playerName)) {
            AssignedAura activatedAuraInfo = deactivatedAuraInfo.getActivatedCopy();
            reactivatedAuraInfoList.add(activatedAuraInfo);
        }

        playerAssignedAuraListMap.put(playerName, reactivatedAuraInfoList);

    }




}

package me.dandrew.ultimateaurapro.command;

import me.dandrew.ultimateaurapro.UltimateAuraProPermissions;
import me.dandrew.ultimateaurapro.auragiving.AuraInfo;
import me.dandrew.ultimateaurapro.auragiving.AuraTracker;
import me.dandrew.ultimateaurapro.config.AuraConfig;
import me.dandrew.ultimateaurapro.config.InstalledAura;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BaseCommand {

    private CommandSender cmdSender;
    private String[] args;

    public BaseCommand(CommandSender cmdSender, String[] args) {
        this.cmdSender = cmdSender;
        this.args = args;
    }


    public void execute() {

        if (args.length < 1) {
            sendHelp();
            return;
        }

        if (args[0].equals("admin")) {
            handleAdmin();
            return;
        }
        
        if (!(cmdSender instanceof Player)) {
            cmdSender.sendMessage(ChatColor.RED + "Please run this command as a player, or try /aura admin");
            return;
        }

        String auraName = args[0];
        InstalledAura installedAura = AuraConfig.INSTANCE.getInstalledAura(auraName);

        if (installedAura == null) {
            cmdSender.sendMessage(ChatColor.RED + "That aura does not exist");
            return;
        }

        if (!UltimateAuraProPermissions.check(cmdSender, installedAura)) {
            cmdSender.sendMessage(ChatColor.RED + "You do not have permission to use this aura");
            return;
        }



        String playerName = cmdSender.getName();
        AuraInfo auraInfo = installedAura.getAuraInfo();
        if (AuraTracker.hasAuraInfo(playerName, auraInfo)) {
            AuraTracker.removeAuraInfoForPlayer(playerName, auraInfo);
            sendHelp();
            cmdSender.sendMessage(ChatColor.GREEN + "Aura removed!");

        }
        else {
            AuraTracker.giveAura(playerName, auraInfo);
            sendHelp();
            cmdSender.sendMessage(ChatColor.GREEN + "Aura activated!");
        }

    }

    private void handleAdmin() {

        if (!cmdSender.hasPermission(UltimateAuraProPermissions.ADMIN_NODE)) {
            cmdSender.sendMessage(ChatColor.RED + "You can't use this command.");
            return;
        }

        // Ex: /ultimateaurapro admin reload
        AdminCommand adminCommand = new AdminCommand(cmdSender, args);
        adminCommand.execute();

    }


    private void sendHelp() {

        cmdSender.sendMessage(ChatColor.GOLD + "Auras:");
        List<InstalledAura> allowedAuras = new ArrayList<>();
        for (InstalledAura installedAura : AuraConfig.INSTANCE.getInstalledAuras()) {
            String auraPermission = installedAura.getPermission();
            if (cmdSender.hasPermission(auraPermission)) {
                allowedAuras.add(installedAura);
            }
        }

        if (allowedAuras.size() > 0) {
            for (InstalledAura allowedAura : allowedAuras) {
                sendHelpLine(allowedAura);
            }
        }
        else {
            cmdSender.sendMessage(ChatColor.YELLOW + "You do not have any auras yet.");
        }

    }

    private void sendHelpLine(InstalledAura installedAura) {
        String onOffPrefix = getOnOffPrefix(installedAura.getAuraInfo());
        String rawMsg = onOffPrefix + "&6/aura " + installedAura.getName() + " : &7" + installedAura.getDescription();
        String coloredLine = ChatColor.translateAlternateColorCodes('&', rawMsg);

        BaseComponent[] coloredLineComponents = TextComponent.fromLegacyText(coloredLine);

        BaseComponent[] baseComponents = new ComponentBuilder("")
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/aura " + installedAura.getName()))
                .append(coloredLineComponents)
                .create();
        cmdSender.spigot().sendMessage(baseComponents);


    }

    private String getOnOffPrefix(AuraInfo auraInfo) {
        if (AuraTracker.hasAuraInfo(cmdSender.getName(), auraInfo)) {
            return "&a[ON] ";
        }
        else {
            return "&c[OFF] ";
        }
    }

}

package me.dandrew.ultimateaurapro.command;

import me.dandrew.ultimateaurapro.UltimateAuraProPlugin;
import me.dandrew.ultimateaurapro.config.AuraConfig;
import me.dandrew.ultimateaurapro.config.InstalledAura;
import me.dandrew.ultimateaurapro.config.LocationAuraConfig;
import me.dandrew.ultimateaurapro.util.ItemDb;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class AdminCommand {

    private CommandSender cmdSender;
    private String[] args;

    AdminCommand(CommandSender cmdSender, String[] args) {
        this.cmdSender = cmdSender;
        this.args = args;
        this.args = Arrays.copyOfRange(args, 1, args.length);
    }

    void execute() {

        if (args.length == 0) {
            sendAdminHelp();
            return;
        }

        switch (args[0]) {
            default:
                sendAdminHelp();
                cmdSender.sendMessage(ChatColor.YELLOW + "That argument was not recognized.");
                break;
            case "reload":
                UltimateAuraProPlugin.reloadSettings();
                cmdSender.sendMessage(ChatColor.GREEN + "Plugin was reloaded!");
                break;
            case "ladd":
                handleAddArg();
                break;
            case "ldelete":
                handleDeleteArg();
                break;
            case "llist":
                handleListArg();
                break;
            case "wand":
                handleWand();
                break;
        }
    }

    private void sendAdminHelp() {
        String[] lines = {
                "&a",
                "&8 = &cUltimate&3Aura&aPro: &eAdmin Help &8= ",
                "&6/aura admin reload &7- reload all configuration files",
                "&6/aura admin ladd &e<id> <aura name>&7 - add a location aura at where you're standing",
                "&6/aura admin ldelete &e<id>&7 - delete a location aura given its id",
                "&6/aura admin llist&7 - list ids of all location auras, along with their location details",
                "&6/aura admin wand &e[player]&7 - get an aura wand for auras using 'wand' or 'wand-self'",
                "&a"
        };

        for (String line : lines) {
            String coloredLine = ChatColor.translateAlternateColorCodes('&', line);
            cmdSender.sendMessage(coloredLine);
        }

    }


    private void handleAddArg() {

        if (!(cmdSender instanceof Player)) {
            cmdSender.sendMessage(ChatColor.RED + "You must be a player to type this command.");
            return;
        }

        if (!checkArgsLength(3)) {
            return;
        }

        String id = args[1];
        String auraName = args[2];

        InstalledAura installedAura = AuraConfig.INSTANCE.getInstalledAura(auraName);
        if (installedAura == null) {
            cmdSender.sendMessage(ChatColor.YELLOW + "That aura was not found.");
            return;
        }

        LocationAuraConfig.LocationAura locationAura
                = new LocationAuraConfig.LocationAura(id, installedAura, ((Player)(cmdSender)).getLocation());

        boolean success = LocationAuraConfig.INSTANCE.addLocationAura(locationAura);
        if (success) {
            cmdSender.sendMessage(ChatColor.GREEN + "Aura was added!");
        }
        else {
            cmdSender.sendMessage(ChatColor.YELLOW + "That aura id is already taken.");
        }

    }

    private boolean checkArgsLength(int expected) {
        if (args.length != expected) {
            sendAdminHelp();
            cmdSender.sendMessage(ChatColor.YELLOW + "Please double check your parameters.");
            return false;
        }
        return true;
    }

    private void handleDeleteArg() {

        if (!checkArgsLength(2)) {
            return;
        }

        String id = args[1];

        boolean success = LocationAuraConfig.INSTANCE.deleteLocationAura(id);
        if (success) {
            cmdSender.sendMessage(ChatColor.GREEN + "Location Aura was deleted.");
        }
        else {
            cmdSender.sendMessage(ChatColor.RED + "That location aura was not found.");
        }

    }

    private void handleListArg() {
        List<String> messages = new ArrayList<>();
        messages.add("&3&lInstalled Location Auras:");

        for (LocationAuraConfig.LocationAura locationAura : LocationAuraConfig.INSTANCE.getLocationAuras()) {
            Location location = locationAura.getLocation();
            String xString = String.format("%.3f", location.getX());
            String yString = String.format("%.3f", location.getY());
            String zString = String.format("%.3f", location.getZ());

            String msg = "&6" + locationAura.getId() + " @ &a" + location.getWorld().getName()
                    + ".&6 x: &7" + xString + ", &6y: &7" + yString + ", &6z: &7" + zString;
            messages.add(msg);
        }

        for (String msg: messages) {
            String coloredMsg = ChatColor.translateAlternateColorCodes('&', msg);
            cmdSender.sendMessage(coloredMsg);
        }
    }

    private void handleWand() {
        Player receiver;
        if (args.length == 2) {
            receiver = Bukkit.getPlayer(args[1]);
            if (receiver == null) {
                cmdSender.sendMessage(ChatColor.RED + "That player is offline.");
                return;
            }
        }
        else {
            if (cmdSender instanceof Player) {
                receiver = (Player) cmdSender;
            }
            else {
                cmdSender.sendMessage(ChatColor.RED + "You can not give a wand to yourself under console. Try /aura admin wand <player>");
                return;
            }
        }

        boolean success = ItemDb.tryToGiveItem(receiver, ItemDb.getAuraWand());
        if (success) {
            if (cmdSender != receiver) {
                cmdSender.sendMessage(ChatColor.GOLD + "Gave an aura wand to " + ChatColor.YELLOW + receiver.getName() + ".");
            }
            receiver.sendMessage(ChatColor.GOLD + "Received an " + ChatColor.DARK_AQUA + "Aura Wand.");
        }
        else {
            if (cmdSender == receiver) {
                cmdSender.sendMessage(ChatColor.RED + "Not enough inventory space.");
            }
            else {
                cmdSender.sendMessage(ChatColor.YELLOW + receiver.getName() + ChatColor.RED + " can not receive " +
                        "an aura wand because of a full inventory.");
            }
        }

    }

}

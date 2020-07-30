package me.dandrew.ultimateaurapro.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ItemDb {

    public static ItemStack getAuraWand() {
        ItemStack auraWand = new ItemStack(Material.STICK, 1);
        ItemMeta itemMeta = auraWand.getItemMeta();
        itemMeta.setDisplayName(ChatColor.GOLD + "Aura Wand");

        List<String> loreLines = new ArrayList<>();
        loreLines.add(ChatColor.DARK_AQUA + "Use this wand to add others");
        loreLines.add(ChatColor.DARK_AQUA + "to some of your " + ChatColor.GOLD + "auras");
        itemMeta.setLore(loreLines);

        auraWand.setItemMeta(itemMeta);
        return auraWand;
    }

    public static boolean tryToGiveItem(Player player, ItemStack item) {
        HashMap<Integer, ItemStack> result = player.getInventory().addItem(item);


        return result.size() == 0;
    }


}

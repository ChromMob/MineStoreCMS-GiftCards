package org.Kloppie74.giftCards.internal;

import org.bukkit.ChatColor;

public class MSG {

    public static String chatColors(String message) {
        if (message == null) return "";

        String processed = message.replace("&", "§");

        processed = processed.replaceAll("&#[a-fA-F0-9]{6}", "");
        processed = processed.replaceAll("#[a-fA-F0-9]{6}", "");
        processed = processed.replaceAll("<gradient:[^>]+>.*?</gradient>", "");

        return ChatColor.translateAlternateColorCodes('§', processed);
    }
}
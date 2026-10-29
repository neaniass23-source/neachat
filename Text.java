package dev.anuncioschat;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utilidades de texto para mensajes simples: &a y &#RRGGBB. */
public final class Text {

    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private Text() {
    }

    public static String color(String input) {
        if (input == null) {
            return "";
        }
        Matcher matcher = HEX.matcher(input);
        StringBuilder sb = new StringBuilder();
        int last = 0;
        while (matcher.find()) {
            sb.append(input, last, matcher.start());
            sb.append(ChatColor.of("#" + matcher.group(1)));
            last = matcher.end();
        }
        sb.append(input.substring(last));
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }
}

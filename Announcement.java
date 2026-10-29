package dev.anuncioschat;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Un anuncio: lineas de texto y, opcionalmente, una imagen a la izquierda. */
public final class Announcement {

    private static final Method PAPI;

    static {
        Method m = null;
        try {
            Class<?> c = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            m = c.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
        } catch (Throwable ignored) {
            // PlaceholderAPI no esta instalado
        }
        PAPI = m;
    }

    public final String id;
    private final List<String> lines;
    private final List<String> imageRows;
    private final int imageColumns;
    private final int separation;
    private final int offset;
    private final boolean perPlayer;

    public Announcement(String id, List<String> lines, List<String> imageRows, int imageColumns,
                        int separation, int offset) {
        this.id = id;
        this.lines = lines;
        this.imageRows = imageRows;
        this.imageColumns = imageColumns;
        this.separation = Math.max(0, separation);
        this.offset = Math.max(0, offset);
        boolean needs = false;
        for (String line : lines) {
            if (line.indexOf('%') >= 0) {
                needs = true;
                break;
            }
        }
        this.perPlayer = needs;
    }

    /** true si el texto cambia segun el jugador (variables o PlaceholderAPI). */
    public boolean isPerPlayer() {
        return perPlayer;
    }

    /** Construye las lineas listas para enviar. player puede ser null si no hay variables. */
    public List<BaseComponent> build(Player player) {
        List<BaseComponent> out = new ArrayList<>();
        boolean hasImage = !imageRows.isEmpty();
        int rows = hasImage ? Math.max(imageRows.size(), lines.size() + offset) : lines.size();
        String blankImage = " ".repeat(imageColumns * 2);

        for (int r = 0; r < rows; r++) {
            String text;
            int textIndex = r - (hasImage ? offset : 0);
            text = textIndex >= 0 && textIndex < lines.size() ? lines.get(textIndex) : "";
            text = applyPlaceholders(player, text);

            if (hasImage) {
                if (text.regionMatches(true, 0, "<center>", 0, 8)) {
                    text = text.substring(8);
                }
                String img = r < imageRows.size() ? imageRows.get(r) : blankImage;
                String raw = img + "&r" + " ".repeat(separation) + text;
                out.add(ChatFormat.parse(raw).component);
            } else if (text.regionMatches(true, 0, "<center>", 0, 8)) {
                out.add(ChatFormat.center(ChatFormat.parse(text.substring(8))));
            } else {
                out.add(ChatFormat.parse(text).component);
            }
        }
        if (out.isEmpty()) {
            out.add(new TextComponent(""));
        }
        return out;
    }

    private static String applyPlaceholders(Player player, String text) {
        if (player == null || text.indexOf('%') < 0) {
            return text;
        }
        String result = text.replace("%player%", player.getName()).replace("%jugador%", player.getName());
        if (PAPI != null && result.indexOf('%') >= 0) {
            try {
                Object value = PAPI.invoke(null, player, result);
                if (value instanceof String) {
                    result = (String) value;
                }
            } catch (Throwable ignored) {
                // se deja el texto sin resolver
            }
        }
        return result;
    }
}

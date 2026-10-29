package dev.anuncioschat;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class AnunciosChat extends JavaPlugin {

    private AnnouncementManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        for (String image : new String[]{"discord.png", "almas.png"}) {
            if (!new File(getDataFolder(), "imagenes/" + image).exists()) {
                saveResource("imagenes/" + image, false);
            }
        }

        manager = new AnnouncementManager(this);
        manager.load();
        manager.start();

        PluginCommand command = getCommand("anuncios");
        if (command != null) {
            AnunciosCommand handler = new AnunciosCommand(this, manager);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }
        getLogger().info("AnunciosChat activado con " + manager.count() + " anuncios.");
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.stop();
        }
    }

    /** Mensaje de config.yml (seccion "mensajes") con prefijo, colores y variables aplicadas. */
    public String msg(String key, String... replacements) {
        String raw = getConfig().getString("mensajes." + key, "&cFalta el mensaje: " + key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace(replacements[i], replacements[i + 1]);
        }
        return Text.color(getConfig().getString("mensajes.prefijo", "") + raw);
    }
}

package dev.anuncioschat;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Carga los anuncios, los envia por turnos y guarda quien los tiene silenciados. */
public final class AnnouncementManager {

    private final AnunciosChat plugin;
    private final List<Announcement> announcements = new ArrayList<>();
    private final Set<UUID> muted = new HashSet<>();
    private final Set<String> excludedWorlds = new HashSet<>();
    private final Random random = new Random();
    private final File mutedFile;

    private BukkitTask task;
    private int nextIndex = 0;
    private int lastIndex = -1;
    private boolean randomMode;
    private int minPlayers;
    private String soundKey = "";

    public AnnouncementManager(AnunciosChat plugin) {
        this.plugin = plugin;
        this.mutedFile = new File(plugin.getDataFolder(), "silenciados.yml");
    }

    // ------------------------------------------------------------------
    // Carga
    // ------------------------------------------------------------------

    public void load() {
        announcements.clear();
        excludedWorlds.clear();
        FileConfiguration config = plugin.getConfig();

        randomMode = "ALEATORIO".equalsIgnoreCase(config.getString("modo", "SECUENCIAL"));
        minPlayers = Math.max(0, config.getInt("minimo-jugadores", 1));
        soundKey = config.getString("sonido", "");
        if (soundKey == null) {
            soundKey = "";
        }
        for (String w : config.getStringList("mundos-excluidos")) {
            excludedWorlds.add(w);
        }

        String character = config.getString("imagen-caracter", "\u2588");
        String transparent = config.getString("imagen-color-transparente", "#14161f");

        ConfigurationSection section = config.getConfigurationSection("anuncios");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection s = section.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                List<String> lines = new ArrayList<>(s.getStringList("lineas"));
                if (lines.isEmpty()) {
                    plugin.getLogger().warning("El anuncio '" + id + "' no tiene lineas y se ignora.");
                    continue;
                }

                List<String> imageRows = new ArrayList<>();
                int columns = 0;
                int separation = 2;
                int offset = 0;
                ConfigurationSection img = s.getConfigurationSection("imagen");
                if (img != null) {
                    String fileName = img.getString("archivo", "");
                    columns = Math.max(1, img.getInt("ancho", 8));
                    int rows = Math.max(1, img.getInt("alto", 8));
                    separation = img.getInt("separacion", 2);
                    offset = img.getInt("desplazamiento", 0);
                    File file = new File(new File(plugin.getDataFolder(), "imagenes"), fileName);
                    if (!file.isFile()) {
                        plugin.getLogger().warning("Anuncio '" + id + "': no existe la imagen " + file.getPath());
                    } else {
                        try {
                            imageRows = ImageRenderer.render(file, columns, rows, character, transparent);
                        } catch (IOException ex) {
                            plugin.getLogger().warning("Anuncio '" + id + "': no se pudo leer la imagen: " + ex.getMessage());
                        }
                    }
                }
                announcements.add(new Announcement(id, lines, imageRows, columns, separation, offset));
            }
        }

        loadMuted();
        nextIndex = 0;
        lastIndex = -1;
    }

    public int count() {
        return announcements.size();
    }

    public List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (Announcement a : announcements) {
            ids.add(a.id);
        }
        return ids;
    }

    public Announcement get(String id) {
        if (id == null) {
            return null;
        }
        for (Announcement a : announcements) {
            if (a.id.equalsIgnoreCase(id)) {
                return a;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Envio
    // ------------------------------------------------------------------

    public void start() {
        stop();
        long ticks = Math.max(5L, plugin.getConfig().getLong("intervalo-segundos", 180L)) * 20L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::broadcastNext, ticks, ticks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void broadcastNext() {
        if (announcements.isEmpty() || Bukkit.getOnlinePlayers().size() < minPlayers) {
            return;
        }
        int size = announcements.size();
        int index;
        if (randomMode) {
            index = random.nextInt(size);
            if (size > 1) {
                while (index == lastIndex) {
                    index = random.nextInt(size);
                }
            }
        } else {
            index = nextIndex % size;
            nextIndex++;
        }
        lastIndex = index;
        send(announcements.get(index), new ArrayList<Player>(Bukkit.getOnlinePlayers()));
    }

    /** Envia el anuncio a todos los conectados que puedan verlo. */
    public void broadcast(Announcement announcement) {
        send(announcement, new ArrayList<Player>(Bukkit.getOnlinePlayers()));
    }

    /** Envia el anuncio solo a un jugador, sin comprobar silencio ni mundos (para probar). */
    public void preview(Announcement announcement, Player player) {
        deliver(announcement, player, null);
    }

    private void send(Announcement announcement, Collection<Player> targets) {
        List<BaseComponent> shared = announcement.isPerPlayer() ? null : announcement.build(null);
        for (Player player : targets) {
            if (muted.contains(player.getUniqueId())) {
                continue;
            }
            if (excludedWorlds.contains(player.getWorld().getName())) {
                continue;
            }
            deliver(announcement, player, shared);
        }
    }

    private void deliver(Announcement announcement, Player player, List<BaseComponent> shared) {
        List<BaseComponent> lines = shared != null ? shared : announcement.build(player);
        for (BaseComponent line : lines) {
            player.spigot().sendMessage(line);
        }
        if (!soundKey.isEmpty()) {
            player.playSound(player.getLocation(), soundKey.toLowerCase(Locale.ROOT), 1.0f, 1.0f);
        }
    }

    // ------------------------------------------------------------------
    // Silenciar
    // ------------------------------------------------------------------

    /** Cambia el estado y devuelve true si ahora esta silenciado. */
    public boolean toggleMuted(UUID uuid) {
        boolean nowMuted;
        if (muted.contains(uuid)) {
            muted.remove(uuid);
            nowMuted = false;
        } else {
            muted.add(uuid);
            nowMuted = true;
        }
        saveMuted();
        return nowMuted;
    }

    private void loadMuted() {
        muted.clear();
        if (!mutedFile.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(mutedFile);
        for (String s : yaml.getStringList("silenciados")) {
            try {
                muted.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
                // entrada invalida
            }
        }
    }

    private void saveMuted() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<String> list = new ArrayList<>();
        for (UUID u : muted) {
            list.add(u.toString());
        }
        yaml.set("silenciados", list);
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(mutedFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo guardar silenciados.yml: " + ex.getMessage());
        }
    }
}

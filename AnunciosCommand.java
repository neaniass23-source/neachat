package dev.anuncioschat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AnunciosCommand implements CommandExecutor, TabCompleter {

    private final AnunciosChat plugin;
    private final AnnouncementManager manager;

    public AnunciosCommand(AnunciosChat plugin, AnnouncementManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.msg("uso"));
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "silenciar":
                return mute(sender);
            case "lista":
                return list(sender);
            case "recargar":
                return reload(sender);
            case "enviar":
                return send(sender, args, false);
            case "probar":
                return send(sender, args, true);
            default:
                sender.sendMessage(plugin.msg("uso"));
                return true;
        }
    }

    private boolean mute(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.msg("solo-jugadores"));
            return true;
        }
        if (!sender.hasPermission("anuncioschat.silenciar")) {
            sender.sendMessage(plugin.msg("sin-permiso"));
            return true;
        }
        boolean muted = manager.toggleMuted(((Player) sender).getUniqueId());
        sender.sendMessage(plugin.msg(muted ? "silenciados" : "activados"));
        return true;
    }

    private boolean list(CommandSender sender) {
        if (!sender.hasPermission("anuncioschat.admin")) {
            sender.sendMessage(plugin.msg("sin-permiso"));
            return true;
        }
        sender.sendMessage(plugin.msg("lista-titulo"));
        for (String id : manager.ids()) {
            sender.sendMessage(plugin.msg("lista-entrada", "%id%", id));
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("anuncioschat.admin")) {
            sender.sendMessage(plugin.msg("sin-permiso"));
            return true;
        }
        plugin.reloadConfig();
        manager.load();
        manager.start();
        sender.sendMessage(plugin.msg("recargado", "%cantidad%", String.valueOf(manager.count())));
        return true;
    }

    private boolean send(CommandSender sender, String[] args, boolean preview) {
        if (!sender.hasPermission("anuncioschat.admin")) {
            sender.sendMessage(plugin.msg("sin-permiso"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.msg("uso"));
            return true;
        }
        Announcement announcement = manager.get(args[1]);
        if (announcement == null) {
            sender.sendMessage(plugin.msg("no-existe", "%id%", args[1]));
            return true;
        }
        if (preview) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.msg("solo-jugadores"));
                return true;
            }
            manager.preview(announcement, (Player) sender);
            return true;
        }
        manager.broadcast(announcement);
        sender.sendMessage(plugin.msg("enviado", "%id%", announcement.id));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String typed = args[0].toLowerCase(Locale.ROOT);
            List<String> options = new ArrayList<>();
            options.add("silenciar");
            if (sender.hasPermission("anuncioschat.admin")) {
                options.add("enviar");
                options.add("probar");
                options.add("lista");
                options.add("recargar");
            }
            for (String o : options) {
                if (o.startsWith(typed)) {
                    out.add(o);
                }
            }
        } else if (args.length == 2 && sender.hasPermission("anuncioschat.admin")
                && (args[0].equalsIgnoreCase("enviar") || args[0].equalsIgnoreCase("probar"))) {
            String typed = args[1].toLowerCase(Locale.ROOT);
            for (String id : manager.ids()) {
                if (id.toLowerCase(Locale.ROOT).startsWith(typed)) {
                    out.add(id);
                }
            }
        }
        return out;
    }
}

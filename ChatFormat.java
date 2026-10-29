package dev.anuncioschat;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

/**
 * Convierte una linea con colores (&a, &#RRGGBB) y etiquetas
 * (&lt;click:...&gt;, &lt;hover:...&gt;) en un componente de chat.
 */
public final class ChatFormat {

    /** Componente listo para enviar y su ancho aproximado en pixeles. */
    public static final class Result {
        public final BaseComponent component;
        public final int width;

        Result(BaseComponent component, int width) {
            this.component = component;
            this.width = width;
        }
    }

    private static final class Builder {
        final TextComponent root = new TextComponent("");
        final StringBuilder buf = new StringBuilder();
        ChatColor color;
        boolean bold;
        boolean italic;
        boolean underlined;
        boolean strike;
        boolean obf;
        ClickEvent click;
        HoverEvent hover;
        int width;

        void flush() {
            if (buf.length() == 0) {
                return;
            }
            String text = buf.toString();
            buf.setLength(0);
            TextComponent t = new TextComponent(text);
            if (color != null) {
                t.setColor(color);
            }
            if (bold) {
                t.setBold(true);
            }
            if (italic) {
                t.setItalic(true);
            }
            if (underlined) {
                t.setUnderlined(true);
            }
            if (strike) {
                t.setStrikethrough(true);
            }
            if (obf) {
                t.setObfuscated(true);
            }
            if (click != null) {
                t.setClickEvent(click);
            }
            if (hover != null) {
                t.setHoverEvent(hover);
            }
            root.addExtra(t);
            for (int i = 0; i < text.length(); i++) {
                width += charWidth(text.charAt(i), bold);
            }
        }

        void resetFormats() {
            bold = false;
            italic = false;
            underlined = false;
            strike = false;
            obf = false;
        }
    }

    private ChatFormat() {
    }

    public static Result parse(String input) {
        Builder b = new Builder();
        String s = input == null ? "" : input.replace('\u00A7', '&');
        int n = s.length();
        int i = 0;
        while (i < n) {
            char c = s.charAt(i);

            if (c == '&' && i + 1 < n) {
                char code = s.charAt(i + 1);
                if (code == '#' && i + 8 <= n && isHex(s, i + 2, i + 8)) {
                    b.flush();
                    b.color = ChatColor.of("#" + s.substring(i + 2, i + 8));
                    b.resetFormats();
                    i += 8;
                    continue;
                }
                char lower = Character.toLowerCase(code);
                if ((lower >= '0' && lower <= '9') || (lower >= 'a' && lower <= 'f')) {
                    ChatColor legacy = ChatColor.getByChar(lower);
                    if (legacy != null) {
                        b.flush();
                        b.color = legacy;
                        b.resetFormats();
                        i += 2;
                        continue;
                    }
                }
                boolean handled = true;
                switch (lower) {
                    case 'l':
                        b.flush();
                        b.bold = true;
                        break;
                    case 'o':
                        b.flush();
                        b.italic = true;
                        break;
                    case 'n':
                        b.flush();
                        b.underlined = true;
                        break;
                    case 'm':
                        b.flush();
                        b.strike = true;
                        break;
                    case 'k':
                        b.flush();
                        b.obf = true;
                        break;
                    case 'r':
                        b.flush();
                        b.color = null;
                        b.resetFormats();
                        break;
                    default:
                        handled = false;
                        break;
                }
                if (handled) {
                    i += 2;
                    continue;
                }
            }

            if (c == '<') {
                int end = s.indexOf('>', i);
                if (end > i) {
                    String tag = s.substring(i + 1, end);
                    String lowerTag = tag.toLowerCase();
                    if (lowerTag.equals("/click")) {
                        b.flush();
                        b.click = null;
                        i = end + 1;
                        continue;
                    }
                    if (lowerTag.equals("/hover")) {
                        b.flush();
                        b.hover = null;
                        i = end + 1;
                        continue;
                    }
                    if (lowerTag.startsWith("click:")) {
                        ClickEvent event = makeClick(tag.substring(6));
                        if (event != null) {
                            b.flush();
                            b.click = event;
                            i = end + 1;
                            continue;
                        }
                    }
                    if (lowerTag.startsWith("hover:")) {
                        b.flush();
                        b.hover = makeHover(tag.substring(6));
                        i = end + 1;
                        continue;
                    }
                }
            }

            b.buf.append(c);
            i++;
        }
        b.flush();
        return new Result(b.root, b.width);
    }

    /** Antepone espacios para centrar el texto en el chat (aproximado). */
    public static BaseComponent center(Result result) {
        int spaces = Math.max(0, (154 - result.width / 2) / 4);
        TextComponent wrapper = new TextComponent("");
        if (spaces > 0) {
            wrapper.addExtra(new TextComponent(" ".repeat(spaces)));
        }
        wrapper.addExtra(result.component);
        return wrapper;
    }

    private static ClickEvent makeClick(String spec) {
        int idx = spec.indexOf(':');
        if (idx <= 0) {
            return null;
        }
        String action = spec.substring(0, idx).toLowerCase();
        String value = spec.substring(idx + 1);
        switch (action) {
            case "open_url":
                return new ClickEvent(ClickEvent.Action.OPEN_URL, value);
            case "run_command":
                return new ClickEvent(ClickEvent.Action.RUN_COMMAND, value);
            case "suggest_command":
                return new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, value);
            case "copy_to_clipboard":
                return new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, value);
            default:
                return null;
        }
    }

    private static HoverEvent makeHover(String text) {
        Result inner = parse(text.replace("\\n", "\n"));
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new BaseComponent[]{inner.component}));
    }

    private static boolean isHex(String s, int from, int to) {
        for (int k = from; k < to; k++) {
            if (Character.digit(s.charAt(k), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    /** Ancho en pixeles de un caracter de la fuente por defecto (con 1 px de separacion). */
    static int charWidth(char c, boolean bold) {
        int w;
        switch (c) {
            case 'I': case '*': case '[': case ']': case '"':
                w = 3;
                break;
            case 'f': case 'k': case 't': case '<': case '>': case '(': case ')': case '{': case '}':
                w = 4;
                break;
            case 'i': case 'l': case '!': case ':': case ';': case '\'': case '|': case '.': case ',':
                w = 1;
                break;
            case '`':
                w = 2;
                break;
            case '@':
                w = 6;
                break;
            case ' ':
                w = 3;
                break;
            default:
                if (c >= 0xD800 && c <= 0xDFFF) {
                    w = 4;
                } else if (c >= 0x2000) {
                    w = 8;
                } else {
                    w = 5;
                }
                break;
        }
        return w + 1 + (bold ? 1 : 0);
    }
}

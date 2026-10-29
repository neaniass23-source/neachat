package dev.anuncioschat;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Convierte una imagen PNG en filas de caracteres de colores para el chat. */
public final class ImageRenderer {

    private ImageRenderer() {
    }

    public static List<String> render(File file, int width, int height, String character, String transparentHex)
            throws IOException {
        BufferedImage source = ImageIO.read(file);
        if (source == null) {
            throw new IOException("Formato de imagen no reconocido: " + file.getName());
        }
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();

        String fallback = transparentHex.startsWith("#") ? transparentHex.substring(1) : transparentHex;
        if (fallback.length() != 6) {
            fallback = "14161f";
        }

        List<String> rows = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            StringBuilder sb = new StringBuilder();
            String last = null;
            for (int x = 0; x < width; x++) {
                int argb = scaled.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                String hex = alpha < 128 ? fallback.toLowerCase() : String.format("%06x", argb & 0xFFFFFF);
                if (!hex.equals(last)) {
                    sb.append("&#").append(hex);
                    last = hex;
                }
                sb.append(character);
            }
            rows.add(sb.toString());
        }
        return rows;
    }
}

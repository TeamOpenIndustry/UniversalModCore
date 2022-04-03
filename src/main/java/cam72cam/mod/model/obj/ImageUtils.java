package cam72cam.mod.model.obj;

import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageUtils {
    public static Pair<Integer, Integer> scaleSize(int width, int height, int maxSize) {
        double scale = maxSize / (double)Math.max(width, height);
        return Pair.of(
                (int) Math.floor(width * scale),
                (int) Math.floor(height * scale)
        );
    }

    public static BufferedImage scaleImage(BufferedImage image, int maxSize) {
        Pair<Integer, Integer> size = scaleSize(image.getWidth(), image.getHeight(), maxSize);
        int x = size.getLeft();
        int y = size.getRight();
        BufferedImage target = new BufferedImage(x, y, image.getType());
        Graphics2D g = target.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, x, y, 0, 0, image.getWidth(), image.getHeight(), null);
        return target;
    }


    public static int[] toRGBA(BufferedImage image) {
        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        for (int i = 0; i < pixels.length; i++) {
            int c_argb = pixels[i];
            int a = c_argb >> 24 & 255;
            int r = c_argb >> 16 & 255;
            int g = c_argb >> 8 & 255;
            int b = c_argb >> 0 & 255;
            pixels[i] = (r << 24) | (g << 16) | (b << 8) | a;

            //pixels[i] = (argb & 0xFFFFFF) << 8 | (argb >> 24);
        }
        return pixels;
    }
}

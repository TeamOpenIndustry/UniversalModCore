package cam72cam.mod.model.obj;

import cam72cam.mod.Config;
import cam72cam.mod.ModCore;
import cam72cam.mod.resource.Identifier;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static cam72cam.mod.model.obj.ImageUtils.scaleImage;

/* primer: https://codeincomplete.com/articles/bin-packing/ */
public class OBJTexturePacker {
    private int width = 0;
    private int height = 0;
    private int scaledWidth = 0;
    private int scaledHeight = 0;
    private Function<String, Identifier> paths;
    private Function<String, InputStream> lookup;

    public final Map<String, UVConverter> converters = new HashMap<>();
    public final Map<String, Supplier<BufferedImage>> textures = new HashMap<>();
    public final Map<String, Supplier<BufferedImage>> normals = new HashMap<>();
    public final Map<String, Supplier<BufferedImage>> speculars = new HashMap<>();

    class Node {
        Dimension size;
        List<Material> materials;
        Material texture;
        int width;
        int height;
        Node down;
        Node right;

        public Node(List<Material> materials) {
            this.materials = materials;
            this.width = this.height = 8;
            this.texture = null; // Textured images will be color only if the image fails to load.

            size = new Dimension(width, height);

            if (materials.get(0).hasTexture()) {
                try {
                    size = getImageDimension(lookup.apply(materials.get(0).texKd), materials.get(0).texKd);
                    this.width = materials.stream().mapToInt(x -> x.copiesU).max().getAsInt() * size.width;
                    this.height = materials.stream().mapToInt(x -> x.copiesV).max().getAsInt() * size.height;
                    this.texture = materials.get(0);
                } catch (Exception e) {
                    ModCore.catching(e, "Unable to load image %s", paths.apply(materials.get(0).texKd));
                }
            }
        }

        public Node(int width, int height) {
            this.width = width;
            this.height = height;
            materials = null;
        }

        private boolean canFit(Node node) {
            return materials == null && this.width >= node.width && this.height >= node.height;
        }

        public boolean addNode(Node node) {
            if (this.right != null) {
                if (this.right.canFit(node)) {
                    node.right = new Node(this.right.width - node.width, this.right.height);
                    node.down = new Node(node.width, this.right.height - node.height);
                    node.right = node.right.width == 0 ? null : node.right;
                    node.down = node.down.height == 0 ? null : node.down;
                    this.right = node;
                    return true;
                } else {
                    boolean recursed = this.right.addNode(node);
                    if (recursed) {
                        return true;
                    }
                }
            }
            if (this.down != null) {
                if (this.down.canFit(node)) {
                    node.right = new Node(this.down.width - node.width, node.height);
                    node.down = new Node(this.down.width, this.down.height - node.height);
                    node.right = node.right.width == 0 ? null : node.right;
                    node.down = node.down.height == 0 ? null : node.down;
                    this.down = node;
                    return true;
                } else {
                    boolean recursed = this.down.addNode(node);
                    if (recursed) {
                        return true;
                    }
                }
            }
            return false;
        }

        public int getFullWidth() {
            return width + (this.right != null ? this.right.getFullWidth() : 0);
        }
        public int getFullHeight() {
            return height + (this.down != null ? this.down.getFullHeight() : 0);
        }

        public Node getFurthestRight() {
            return right != null ? right.getFurthestRight() : this;
        }

        public Node getFurthestDown() {
            return down != null ? down.getFurthestDown() : this;
        }

        public void converters(int x, int y) {
            if (materials != null) {
                int copiesU = materials.stream().mapToInt(m -> m.copiesU).max().getAsInt();
                int copiesV = materials.stream().mapToInt(m -> m.copiesV).max().getAsInt();
                UVConverter converter = new UVConverter(
                        x, y,
                        size.width, size.height,
                        copiesU, copiesV,
                        OBJTexturePacker.this.width, OBJTexturePacker.this.height
                );
                for (Material material : materials) {
                    converters.put(material.name, converter);
                }
                if (right != null) {
                    right.converters(x + width, y);
                }
                if (down != null) {
                    down.converters(x, y + height);
                }
            }
        }

        public void draw(int x, int y, String variant, Graphics2D graphics, Function<Material, String> texlu) {
            if (materials == null) {
                graphics.setColor(Color.BLACK);
                graphics.fillRect(x, y, width, height);
                return;
            }

            BufferedImage image;
            if (texture != null) {
                String origPath = texlu.apply(texture);
                try {
                    String path = origPath;
                    if (variant != null && !variant.isEmpty()) {
                        String[] sp = path.split("/");
                        String fname = sp[sp.length - 1];
                        path = path.replaceAll(fname, variant + "/" + fname);
                    }
                    image = ImageIO.read(lookup.apply(path));
                } catch (Exception e) {
                    //Fallback
                    try {
                        image = ImageIO.read(lookup.apply(texture.texKd));
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
            } else {
                Material mat = materials.get(0);
                int r = (int) (Math.max(0, mat.KdR) * 255);
                int g = (int) (Math.max(0, mat.KdG) * 255);
                int b = (int) (Math.max(0, mat.KdB) * 255);
                int a = (int) (mat.KdA * 255);
                int cint = (a << 24) | (r << 16) | (g << 8) | b;
                image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
                for (int px = 0; px < 8; px++) {
                    for (int py = 0; py < 8; py++) {
                        image.setRGB(px, py, cint);
                    }
                }
            }

            int copiesU = materials.stream().mapToInt(m -> m.copiesU).max().getAsInt();
            int copiesV = materials.stream().mapToInt(m -> m.copiesV).max().getAsInt();

            for (int cU = 0; cU < copiesU; cU++) {
                for (int cV = 0; cV < copiesV; cV++) {
                    int offX = x + image.getWidth() * cU;
                    int offY = y + image.getHeight() * cV;
                    graphics.drawImage(image, null, offX, offY);
                }
            }
            if (right != null) {
                right.draw(x + width, y, variant, graphics, texlu);
            }
            if (down != null) {
                down.draw(x, y + height, variant, graphics, texlu);
            }
        }
    }

    public static class UVConverter {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final int copiesU;
        private final int copiesV;
        private final int sheetWidth;
        private final int sheetHeight;

        public UVConverter(int x, int y, int width, int height, int copiesU, int copiesV, int sheetWidth, int sheetHeight) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.copiesU = copiesU;
            this.copiesV = copiesV;
            this.sheetWidth = sheetWidth;
            this.sheetHeight = sheetHeight;
        }

        public float convertU(float u) {
            float originU = x / (float) sheetWidth;
            float offsetU = u * (float) this.width / sheetWidth;
            return originU + offsetU;
        }

        public float convertV(float v) {
            float originV = 1 - ((y+height*copiesV) / (float) sheetHeight);
            float offsetV = v * ((float) this.height / sheetHeight);
            return 1-(originV + offsetV);
        }
    }

    public OBJTexturePacker(Identifier ident, Function<String, Identifier> paths, Function<String, InputStream> lookup, Collection<Material> materials, Collection<String> variants) {
        if (materials.isEmpty()) {
            return;
        }

        this.paths = paths;
        this.lookup = lookup;

        List<Node> inputNodes = materials.stream()
                .filter(m -> m.used)
                .collect(Collectors.groupingBy(k -> k.texKd == null ? k.name : k.texKd)).values().stream()
                .map(Node::new)
                .sorted(Comparator.comparingInt(x -> -10000 * x.height + x.width))
                .collect(Collectors.toList());

        Node rootNode = inputNodes.remove(0);
        for (Node node : inputNodes) {
            if (!rootNode.addNode(node)) {
                boolean fitsRight = rootNode.getFullHeight() >= node.height;
                boolean fitsDown = rootNode.getFullWidth() >= node.width;
                boolean betterFitRight = rootNode.getFullWidth() + node.width < rootNode.getFullHeight() + node.height;
                if (fitsRight && (!fitsDown || betterFitRight)) {
                    // Expand right
                    rootNode.getFurthestRight().right = new Node(node.width, rootNode.getFullHeight());
                } else if (fitsDown) {
                    // Expand down
                    rootNode.getFurthestDown().down = new Node(rootNode.getFullWidth(), node.height);
                } else {
                    throw new RuntimeException("Impossible!!!!");
                }
                rootNode.addNode(node);
            }
        }

        this.width = rootNode.getFullWidth();
        this.height = rootNode.getFullHeight();
        if (needsScaling()) {
            Pair<Integer, Integer> size = ImageUtils.scaleSize(width, height, Config.getMaxTextureSize());
            this.scaledWidth = size.getLeft();
            this.scaledHeight = size.getRight();
        } else {
            this.scaledWidth = width;
            this.scaledHeight = height;
        }
        rootNode.converters(0, 0);

        for (String variant : variants) {
            textures.put(variant, () -> {
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                rootNode.draw(0, 0, variant, graphics, m -> m.texKd);
                if (needsScaling()) {
                    int originalWidth = image.getWidth();
                    int originalHeight = image.getHeight();
                    image = scaleImage(image, Config.getMaxTextureSize());
                    ModCore.warn("Scaling texture '%s' for %s from (%s x %s) to (%s x %s)", variant, ident, originalWidth, originalHeight, image.getWidth(), image.getHeight());
                }
                return image;
            });

            if (materials.stream().anyMatch(x -> x.texBump != null)) {
                normals.put(variant, () -> {
                    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D graphics = image.createGraphics();
                    rootNode.draw(0, 0, variant, graphics, m -> m.texBump);
                    if (needsScaling()) {
                        int originalWidth = image.getWidth();
                        int originalHeight = image.getHeight();
                        image = scaleImage(image, Config.getMaxTextureSize());
                        ModCore.warn("Scaling texture '%s' for %s from (%s x %s) to (%s x %s)", variant, ident, originalWidth, originalHeight, image.getWidth(), image.getHeight());
                    }
                    return image;
                });
            }

            if (materials.stream().anyMatch(x -> x.texNs != null)) {
                speculars.put(variant, () -> {
                    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D graphics = image.createGraphics();
                    rootNode.draw(0, 0, variant, graphics, m -> m.texNs);
                    if (needsScaling()) {
                        int originalWidth = image.getWidth();
                        int originalHeight = image.getHeight();
                        image = scaleImage(image, Config.getMaxTextureSize());
                        ModCore.warn("Scaling texture '%s' for %s from (%s x %s) to (%s x %s)", variant, ident, originalWidth, originalHeight, image.getWidth(), image.getHeight());
                    }
                    return image;
                });
            }
        }
    }

    private boolean needsScaling() {
        return width > Config.getMaxTextureSize() || height > Config.getMaxTextureSize();
    }

    public int getWidth() {
        return scaledWidth;
    }
    public int getHeight() {
        return scaledHeight;
    }

    private static Dimension getImageDimension(InputStream imgFile, String texKd) throws IOException {
        try(ImageInputStream in = ImageIO.createImageInputStream(imgFile)){
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(in);
                    return new Dimension(reader.getWidth(0), reader.getHeight(0));
                } finally {
                    reader.dispose();
                }
            }
        }
        throw new IOException("Unable to determine image file type!");
    }
}

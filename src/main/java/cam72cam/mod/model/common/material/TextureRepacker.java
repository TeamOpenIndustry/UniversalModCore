package cam72cam.mod.model.common.material;

import cam72cam.mod.Config;
import cam72cam.mod.ModCore;
import cam72cam.mod.model.common.mesh.IModelBuilder;
import cam72cam.mod.model.common.util.ImageUtils;
import cam72cam.mod.resource.Identifier;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/* primer: https://codeincomplete.com/articles/bin-packing/ */
public class TextureRepacker {
    //ARGB
    private static final int normalFallback;
    private static final int specularFallback;
    private static final int albedoFallback;

    private int width = 0;
    private int height = 0;
    private int scaledWidth = 0;
    private int scaledHeight = 0;
    private Function<String, Identifier> locationResolver;
    private Function<Identifier, InputStream> lookup;
    private Node rootNode;

    private boolean hasSpecular;
    private boolean hasNormal;

    public final Map<String, UVConverter> converters = new HashMap<>();
    public final Map<String, Supplier<BufferedImage>> textures = new HashMap<>();
    public final Map<String, Supplier<BufferedImage>> speculars = new HashMap<>();
    public final Map<String, Supplier<BufferedImage>> normals = new HashMap<>();

    static {
        normalFallback = 0xFF8080FF;
        specularFallback = 0xFF000000;
        albedoFallback = 0xFFFFFFFF;
    }

    private final Map<String, BufferedImage> imageCache = new HashMap<>();

    private BufferedImage getCachedImage(String origPath, String variant) {
        if (variant != null && !variant.trim().isEmpty()) {
            try {
                // Variants should only be read once
                String fileName = FilenameUtils.getName(origPath);
                String path = origPath.replace(fileName, variant + "/" + fileName);
                Identifier applied = locationResolver.apply(path);
                return ImageIO.read(lookup.apply(applied));
            } catch (Exception e) {
                //Fallback
                return getCachedImage(origPath, null);
            }
        }

        // Base image should be cached and re-used when applicable
        return imageCache.computeIfAbsent(origPath, path -> {
            try {
                Identifier loc = locationResolver.apply(origPath);
                if (!loc.canLoad()) {
                    return null;
                }
                return ImageIO.read(lookup.apply(loc));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    class Node {
        Dimension size;
        List<Material> materials;
        Material material;
        int width;
        int height;
        Node down;
        Node right;

        public Node(List<Material> materials) {
            this.materials = materials;
            Material first = materials.get(0);
            // size is one tile; width/height is the full tiled region.
            // Textured materials use the dimensions read by Material.populateSize; untextured
            // and missing-texture materials fall back to the 16x16 default.
            this.size = new Dimension(first.width, first.height);
            this.width = materials.stream().mapToInt(x -> x.copiesOnU).max().getAsInt() * size.width;
            this.height = materials.stream().mapToInt(x -> x.copiesOnV).max().getAsInt() * size.height;
            this.material = first.texAlbedo != null ? first : null;
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
                    return this.down.addNode(node);
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
                int copiesU = materials.stream().mapToInt(m -> m.copiesOnU).max().getAsInt();
                int copiesV = materials.stream().mapToInt(m -> m.copiesOnV).max().getAsInt();
                UVConverter converter = new UVConverter(x, y, size.width, size.height, copiesU, copiesV,
                                                        TextureRepacker.this.width, TextureRepacker.this.height);
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

        public void draw(int x, int y, Graphics2D graphics, String variant, Function<Material, String> texlu, int fallbackColor, boolean isFatal) {
            if (materials == null) {
                graphics.setColor(Color.BLACK);
                graphics.fillRect(x, y, width, height);
                return;
            }

            BufferedImage image = null;
            if (material != null) {
                String path = texlu.apply(material);
                if (path != null) {
                    image = getCachedImage(path, variant);
                    if (image == null && isFatal) {
                        // Only the albedo channel is fatal; missing specular/normal is silent.
                        throw new RuntimeException("Missing texture: " + path);
                    }
                }
            }
            if (image == null) {
                // Untextured, or a missing specular/normal channel: solid tile, drawn copiesU*copiesV times.
                image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
                for (int px = 0; px < size.width; px++) {
                    for (int py = 0; py < size.height; py++) {
                        image.setRGB(px, py, fallbackColor);
                    }
                }
            }

            int copiesU = materials.stream().mapToInt(m -> m.copiesOnU).max().getAsInt();
            int copiesV = materials.stream().mapToInt(m -> m.copiesOnV).max().getAsInt();
            for (int cU = 0; cU < copiesU; cU++) {
                for (int cV = 0; cV < copiesV; cV++) {
                    graphics.drawImage(image, x + image.getWidth() * cU, y + image.getHeight() * cV, null);
                }
            }
            if (right != null) {
                right.draw(x + width, y, graphics, variant, texlu, fallbackColor, isFatal);
            }
            if (down != null) {
                down.draw(x, y + height, graphics, variant, texlu, fallbackColor, isFatal);
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
            //flipV
            float originV = 1 - ((y + height * copiesV) / (float) sheetHeight);
            float offsetV = v * ((float) this.height / sheetHeight);
            return 1 - (originV + offsetV);
        }
    }

    public TextureRepacker(IModelBuilder builder, Collection<Material> materials, Collection<String> variants) {
        ImageIO.setUseCache(false);
        if (materials.isEmpty()) {
            return;
        }
        Identifier modelLoc = builder.getModelLoc();

        this.locationResolver = str -> builder.getModelLoc().getRelative(str);
        this.lookup = p -> {
            try {
                return builder.open(p);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        // Load texture dimensions before bin packing
        materials.forEach(Material::populateSize);

        List<Node> inputNodes = materials.stream()
                                         .collect(Collectors.groupingBy(k -> k.texAlbedo == null ? k.name : k.texAlbedo)).values().stream()
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
        this.rootNode = rootNode;

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

        this.hasSpecular = materials.stream().anyMatch(x -> x.texSpecular != null && modelLoc.getRelative(x.texSpecular).canLoad());
        this.hasNormal = materials.stream().anyMatch(x -> x.texNormal != null && modelLoc.getRelative(x.texNormal).canLoad());

        for (String variant : variants) {
            textures.put(variant, sheet(modelLoc, variant, m -> m.texAlbedo, "albedo", true));
            if (hasSpecular) {
                speculars.put(variant, sheet(modelLoc, variant, m -> m.texSpecular, "specular", false));
            }
            if (hasNormal) {
                normals.put(variant, sheet(modelLoc, variant, m -> m.texNormal, "normal", false));
            }
        }
    }

    private Supplier<BufferedImage> sheet(Identifier ident, String variant, Function<Material, String> texlu, String type, boolean isFatal) {
        int fallbackColor;
        switch (type) {
            case "normal":
                fallbackColor = normalFallback;
                break;
            case "specular":
                fallbackColor = specularFallback;
                break;
            case "albedo":
            default:
                fallbackColor = albedoFallback;
        }
        return () -> {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            rootNode.draw(0, 0, graphics, variant, texlu, fallbackColor, isFatal);
            if (needsScaling()) {
                int originalWidth = image.getWidth();
                int originalHeight = image.getHeight();
                image = ImageUtils.scaleImage(image, Config.getMaxTextureSize());
                ModCore.warn("Scaling texture for %s (variant %s, channel %s) from (%s x %s) to (%s x %s)", ident, variant, type, originalWidth, originalHeight, image.getWidth(), image.getHeight());
            }
            return image;
        };
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

    public boolean hasSpecular() {
        return hasSpecular;
    }

    public boolean hasNormal() {
        return hasNormal;
    }
}

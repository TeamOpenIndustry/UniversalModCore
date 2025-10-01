package rendertrack.vertex;

import cam72cam.mod.ModCore;

import java.util.HashMap;

/**
 * Default vertex formats and hashmap of formats for use by other mods.
 * @author Haxorouse
 */
public class VertexFormats {
    public static final VertexFormat BLIT_SCREEN = new VertexFormat(new VertexFormatAttribute[] {VertexAttributes.POSITON, VertexAttributes.UV, VertexAttributes.COLOR});
    public static final VertexFormat BLOCK = new VertexFormat(new VertexFormatAttribute[]{VertexAttributes.POSITON, VertexAttributes.COLOR, VertexAttributes.UV0, VertexAttributes.UV2, VertexAttributes.NORMAL, VertexAttributes.PADDING});
    public static final VertexFormat NEW_ENTITY = new VertexFormat(new VertexFormatAttribute[]{VertexAttributes.POSITON, VertexAttributes.COLOR, VertexAttributes.UV0, VertexAttributes.UV1, VertexAttributes.UV2, VertexAttributes.NORMAL, VertexAttributes.PADDING});
    public static final VertexFormat PARTICLE = new VertexFormat(new VertexFormatAttribute[]{VertexAttributes.POSITON, VertexAttributes.UV0, VertexAttributes.COLOR, VertexAttributes.UV2});
    public static final VertexFormat POSITION = new VertexFormat(new VertexFormatAttribute[]{VertexAttributes.POSITON});
    public static final VertexFormat POSITION_COLOR = new VertexFormat(new VertexFormatAttribute[]{VertexAttributes.POSITON, VertexAttributes.COLOR});
    public static final VertexFormat POSITION_COLOR_NORMAL = new VertexFormat(new VertexFormatAttribute[]{VertexAttributes.POSITON, VertexAttributes.COLOR, VertexAttributes.NORMAL, VertexAttributes.PADDING});
    public static final VertexFormat POSITION_COLOR_LIGHTMAP = new VertexFormat(new VertexFormatAttribute[]{VertexAttributes.POSITON, VertexAttributes.COLOR, VertexAttributes.UV2});
    public static final VertexFormat POSITION_TEX = new VertexFormat(new VertexFormatAttribute[]{VertexAttributes.POSITON, VertexAttributes.UV0});
    public static final VertexFormat POSITION_COLOR_TEX = new VertexFormat(new VertexFormatAttribute[]{VertexAttributes.POSITON, VertexAttributes.COLOR, VertexAttributes.UV0});
    public static final VertexFormat POSITION_TEX_COLOR = new VertexFormat(new VertexFormatAttribute[]{VertexAttributes.POSITON, VertexAttributes.UV0, VertexAttributes.COLOR});
    public static final VertexFormat POSITION_COLOR_TEX_LIGHTMAP = new VertexFormat(new VertexFormatAttribute[]{VertexAttributes.POSITON, VertexAttributes.COLOR, VertexAttributes.UV0, VertexAttributes.UV2});
    public static final VertexFormat POSITION_TEX_LIGHTMAP_COLOR = new VertexFormat(new VertexFormatAttribute[]{VertexAttributes.POSITON, VertexAttributes.UV0, VertexAttributes.UV2, VertexAttributes.COLOR});
    public static final VertexFormat POSITION_TEX_COLOR_NORMAL = new VertexFormat(new VertexFormatAttribute[]{VertexAttributes.POSITON, VertexAttributes.UV0, VertexAttributes.COLOR, VertexAttributes.NORMAL, VertexAttributes.PADDING});

    /**
     * Contains all instanced vertex formats available to loaded mods.
     */
    private static final HashMap<String, VertexFormat> FORMATS = new HashMap<>() {{
        put("Blit Screen", BLIT_SCREEN);
        put("Block", BLOCK);
        put("New Entity", NEW_ENTITY);
        put("Particle", PARTICLE);
        put("Position", POSITION);
        put("Position Color", POSITION_COLOR);
        put("Position Color Normal", POSITION_COLOR_NORMAL);
        put("Position Color Lightmap", POSITION_COLOR_LIGHTMAP);
        put("Position Texture", POSITION_TEX);
        put("Position Color Texture", POSITION_COLOR_TEX);
        put("Position Texture Color", POSITION_TEX_COLOR);
        put("Position Color Texture Lightmap", POSITION_COLOR_TEX_LIGHTMAP);
        put("Position Texture Lightmap Color", POSITION_TEX_LIGHTMAP_COLOR);
        put("Position Texture Color Normal", POSITION_TEX_COLOR_NORMAL);
    }};

    /**
     * FOR EXTERNAL MOD USE ONLY.<br>
     * Add a custom vertex format if your model format requires it.
     * @param name Name of the vertex format.
     * @param format Vertex format object.
     */
    public static void addFormat(String name, VertexFormat format) {
        if(FORMATS.containsKey(name)){
            ModCore.error("Mod Conflict!!! vertex format \"%s\" already exists, will not be added to list", name);
            return;
            //TODO may need to free format
        }
        FORMATS.put(name, format);
    }

    /**
     * Get a vertex format.
     * @param name Name of the format.
     * @return The format.
     */
    public static VertexFormat getFormat(String name) {
        return FORMATS.get(name);
    }
}

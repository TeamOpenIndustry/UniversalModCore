package rendertrack.vertex;

import cam72cam.mod.ModCore;
import java.util.HashMap;

/**
 * Attributes to be assembled into a vertex format.<br>
 * Contains both finals for default attributes and a hash map to allow custom attributes to be added by mods.
 * @author Haxorouse
 */
public class VertexAttributes {
    public static final VertexFormatAttribute POSITON = new VertexFormatAttribute(VertexFormatAttribute.DefaultLocation.POSITION.ordinal(), 3, VertexFormatAttribute.GlType.FLOAT, VertexFormatAttribute.IntHandling.CONVERT_FLOAT);
    public static final VertexFormatAttribute COLOR = new VertexFormatAttribute(VertexFormatAttribute.DefaultLocation.COLOR.ordinal(), 4, VertexFormatAttribute.GlType.UBYTE, VertexFormatAttribute.IntHandling.NORMALIZE);
    public static final VertexFormatAttribute UV0 = new VertexFormatAttribute(VertexFormatAttribute.DefaultLocation.UV.ordinal(), 2, VertexFormatAttribute.GlType.FLOAT, VertexFormatAttribute.IntHandling.CONVERT_FLOAT);
    public static final VertexFormatAttribute UV1 = new VertexFormatAttribute(VertexFormatAttribute.DefaultLocation.UV.ordinal(), 2, VertexFormatAttribute.GlType.SHORT, VertexFormatAttribute.IntHandling.PURE_INT);
    public static final VertexFormatAttribute UV2 = new VertexFormatAttribute(VertexFormatAttribute.DefaultLocation.UV.ordinal(), 2, VertexFormatAttribute.GlType.SHORT, VertexFormatAttribute.IntHandling.PURE_INT);
    public static final VertexFormatAttribute NORMAL = new VertexFormatAttribute(VertexFormatAttribute.DefaultLocation.NORMAL.ordinal(), 3, VertexFormatAttribute.GlType.BYTE, VertexFormatAttribute.IntHandling.NORMALIZE);
    public static final VertexFormatAttribute PADDING = new VertexFormatAttribute(VertexFormatAttribute.DefaultLocation.PADDING.ordinal(), 1, VertexFormatAttribute.GlType.BYTE, VertexFormatAttribute.IntHandling.CONVERT_FLOAT);
    public static final VertexFormatAttribute UV = UV0;

    /**
     * Contains all instanced vertex attributes available to loaded mods.
     */
    private static final HashMap<String, VertexFormatAttribute> ATTRIBUTES = new HashMap<>() {{
        put("Position", POSITON);
        put("Color", COLOR);
        put("UV", UV);
        put("UV0", UV0);
        put("UV1", UV1);
        put("UV2", UV2);
        put("Normal", NORMAL);
        put("Padding", PADDING);
    }};

    /**
     * FOR EXTERNAL MOD USE ONLY.<br>
     * Add a custom vertex attribute if your model format requires it.
     * @param name Name of the attribute, ie position, color, uv, etc.
     * @param attribute Attribute object.
     */
    public static void addAttribute(String name, VertexFormatAttribute attribute) {
        if (ATTRIBUTES.containsKey(name)){
            ModCore.error("Mod Conflict!!! vertex attribute \"%s\" already exists, will not be added to list", name);
            return;
            //TODO may need to free attribute
        }
        ATTRIBUTES.put(name, attribute);
    }

    /**
     * Get a vertex attribute when assembling a format.
     * @param name Name of the attribute.
     * @return The attribute.
     */
    public static VertexFormatAttribute getAttribute(String name) {
        return ATTRIBUTES.get(name);
    }
}

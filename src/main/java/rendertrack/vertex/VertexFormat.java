package rendertrack.vertex;

import org.lwjgl.opengl.GL32;

import java.util.LinkedList;

/**
 * Custom type to represent formats of storing vertex data in memory
 * <a href="https://www.khronos.org/opengl/wiki/Vertex_Formats">reference</a>
 * @author Haxorouse
 */
public class VertexFormat {
    private LinkedList<AttributeOffset> format = new LinkedList<>();

    /**
     * Create a new vertex format.
     * FOR EXTERNAL MODS: only create formats in the VertexFormats hashmap, do not store them in your mod.
     * @param attributes Vertex attributes that comprise this format, structured in order as an array.
     */
    public VertexFormat(VertexFormatAttribute[] attributes) {
        int offset = 0;
        for(VertexFormatAttribute attribute : attributes) {
            format.add(new AttributeOffset(attribute, offset));
            offset += attribute.stride;
        }
    }

    /**
     * Appies this format to the currently bound VAO.<br>
     * Only call from VAO.
     */
    protected void apply() {
        for(AttributeOffset attrib : format) {
            attrib.attribute.apply(attrib.offset);
        }
    }

    /**
     *
     */
    public class AttributeOffset {
        private final VertexFormatAttribute attribute;
        private final int offset;

        public AttributeOffset(VertexFormatAttribute attribute, int offset) {
            this.attribute = attribute;
            this.offset = offset;
        }

        public VertexFormatAttribute getAttribute() {
            return attribute;
        }

        public int getOffset() {
            return offset;
        }
    }
}

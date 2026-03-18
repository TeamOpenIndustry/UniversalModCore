package cam72cam.mod.model.common;

import cam72cam.mod.util.With;
import org.lwjgl.opengl.GL11;

import java.util.*;

public class VertexAttribute {

    public static final VertexAttribute POSITION = VertexAttribute.of(Elements.POSITION);
    public static final VertexAttribute POSITION_COLOR = VertexAttribute.of(Elements.POSITION, Elements.COLOR);
    public static final VertexAttribute POSITION_UV = VertexAttribute.of(Elements.POSITION, Elements.TEXTURE_UV);
    public static final VertexAttribute POSITION_NORMAL_UV = VertexAttribute.of(Elements.POSITION, Elements.NORMAL, Elements.TEXTURE_UV);
    public static final VertexAttribute POSITION_COLOR_UV = VertexAttribute.of(Elements.POSITION, Elements.COLOR, Elements.TEXTURE_UV);
    public static final VertexAttribute POSITION_COLOR_NORMAL_UV = VertexAttribute.of(Elements.POSITION, Elements.COLOR, Elements.NORMAL, Elements.TEXTURE_UV);

    private final Map<Elements, Integer> elements;
    private final int stride; //in bytes

    private VertexAttribute(List<Elements> elements) {
        this.elements = new LinkedHashMap<>();
        int offset = 0;
        for (Elements element : elements) {
            this.elements.put(element, offset);
            offset += element.size;
        }
        this.stride = elements.stream().mapToInt(e -> e.size).sum() * Float.BYTES;
    }

    public static VertexAttribute of(Elements... elements) {
        return new VertexAttribute(Arrays.asList(elements));
    }

    public Map<Elements, Integer> getElements() {
        return elements;
    }

    public int getStride() {
        return stride;
    }

    public With setup() {
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        boolean hasNormal = false;
        for (Map.Entry<Elements, Integer> e : elements.entrySet()) {
            switch (e.getKey())  {
                case POSITION:
                    GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                    GL11.glVertexPointer(Elements.POSITION.size, GL11.GL_FLOAT, stride, e.getValue() * Float.BYTES);
                    break;
                case COLOR:
                    GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
                    GL11.glVertexPointer(Elements.COLOR.size, GL11.GL_FLOAT, stride, e.getValue() * Float.BYTES);
                    break;
                case NORMAL:
                    GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
                    GL11.glVertexPointer(Elements.NORMAL.size, GL11.GL_FLOAT, stride, e.getValue() * Float.BYTES);
                    hasNormal = true;
                    break;
                case TEXTURE_UV:
                    GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                    GL11.glVertexPointer(Elements.TEXTURE_UV.size, GL11.GL_FLOAT, stride, e.getValue() * Float.BYTES);
                    break;
                default:
                    //TODO
            }
        }
        if (!hasNormal) {
            GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
        }
        return GL11::glPopClientAttrib;
    }

    public enum Elements {
        POSITION(3),
        COLOR(4),
        NORMAL(3),
        TEXTURE_UV(2); //Default texture

        public final int size;
        Elements(int size) {
            this.size = size;
        }
    }
}

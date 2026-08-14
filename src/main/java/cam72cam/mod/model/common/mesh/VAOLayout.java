package cam72cam.mod.model.common.mesh;

import cam72cam.mod.serialization.TagCompound;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VAOLayout {
    public static final VAOLayout POS_TEX_COLOR = new VAOLayout(Element.POS, Element.UV, Element.COLOR);
    public static final VAOLayout POS_TEX_COLOR_NORMAL = new VAOLayout(Element.POS, Element.UV, Element.COLOR, Element.NORMAL);

    private final List<Element> elements;
    private final List<Integer> offsets;
    private final int stride;

    public VAOLayout(Element... elements) {
        this.elements = new ArrayList<>();
        this.offsets = new ArrayList<>();
        int offsetBytes = 0;
        for (Element element : elements) {
            this.elements.add(element);
            this.offsets.add(offsetBytes);
            offsetBytes += element.size;
        }
        this.stride = offsetBytes;

        if (!has(Usage.POSITION)) {
            throw new RuntimeException("VAO doesn't have POSITION");
        }
    }


    /** Total size in bytes of a single vertex. */
    public int getStrideBytes() {
        return stride;
    }

    public int getStride() {
        return stride / Float.BYTES;
    }

    public List<Element> getElements() {
        return elements;
    }

    public boolean has(Usage usage) {
        for (Element element : elements) {
            if (element.usage == usage) {
                return true;
            }
        }
        return false;
    }

    /** Byte offset of the first element with the given usage, or -1 when absent. */
    public int getOffsetBytes(Usage usage) {
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i).usage == usage) {
                return offsets.get(i);
            }
        }
        return -1;
    }

    public int getOffset(Usage usage) {
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i).usage == usage) {
                return offsets.get(i) / Float.BYTES;
            }
        }
        return -1;
    }

    private int getGlType(Usage usage) {
        for (Element element : elements) {
            if (element.usage == usage) {
                return element.type.glType;
            }
        }
        return -1;
    }

    public void setup() {
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glVertexPointer(3, getGlType(Usage.POSITION), getStrideBytes(), getOffsetBytes(Usage.POSITION));

        if (has(Usage.UV)) {
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glTexCoordPointer(2, getGlType(Usage.UV), getStrideBytes(), getOffsetBytes(Usage.UV));
        } else {
            GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        }
        if (has(Usage.COLOR)) {
            GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
            GL11.glColorPointer(4, getGlType(Usage.COLOR), getStrideBytes(), getOffsetBytes(Usage.COLOR));
        } else {
            GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
        }
        if (has(Usage.NORMAL)) {
            GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
            GL11.glNormalPointer(getGlType(Usage.NORMAL), getStrideBytes(), getOffsetBytes(Usage.NORMAL));
        } else {
            GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
        }
    }

    public void restore() {
        //No need to do anything in 1.16-
    }

    public TagCompound serialize() {
        return new TagCompound().setList("elements",
                elements.stream()
                        .map(e -> new TagCompound()
                                              .setEnum("usage", e.usage)
                                              .setEnum("type", e.type)
                                              .setInteger("count", e.count))
                        .collect(Collectors.toList()),
                t -> t);
    }

    public static VAOLayout deserialize(TagCompound data) {
        List<Element> elements = data.getList("elements", t ->
                new Element(t.getEnum("usage", Usage.class), t.getEnum("type", Type.class), t.getInteger("count")
        ));
        return new VAOLayout(elements.toArray(new Element[0]));
    }

    public enum Type {
        UNSIGNED_BYTE(GL11.GL_UNSIGNED_BYTE, Byte.BYTES),
        BYTE(GL11.GL_BYTE, Byte.BYTES),
        UNSIGNED_SHORT(GL11.GL_UNSIGNED_SHORT, Short.BYTES),
        SHORT(GL11.GL_SHORT, Short.BYTES),
        UNSIGNED_INT(GL11.GL_UNSIGNED_INT, Integer.BYTES),
        INT(GL11.GL_INT, Integer.BYTES),
        FLOAT(GL11.GL_FLOAT, Float.BYTES),
        DOUBLE(GL11.GL_DOUBLE, Double.BYTES),
        ;

        public final int glType;
        public final int size;
        Type(int glType, int size) {
            this.glType = glType;
            this.size = size;
        }
    }

    public enum Usage {
        POSITION, UV, COLOR, NORMAL, PADDING
    }

    public static class Element {
        //Default elements in float
        public static final Element POS = new Element(Usage.POSITION, Type.FLOAT, 3);
        public static final Element UV = new Element(Usage.UV, Type.FLOAT, 2);
        public static final Element NORMAL = new Element(Usage.NORMAL, Type.FLOAT, 3);
        public static final Element COLOR = new Element(Usage.COLOR, Type.FLOAT, 4);

        public final Usage usage;
        public final Type type;
        public final int count;
        final int size;

        public Element(Usage usage, Type type, int count) {
            this.usage = usage;
            this.type = type;
            this.count = count;
            this.size = type.size * count;
        }
    }
}

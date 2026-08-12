package cam72cam.mod.model.common.mesh;

import java.util.ArrayList;
import java.util.List;

public class VAOLayout {
    public static final VAOLayout POS_TEX_COLOR = new VAOLayout(Element.POS, Element.UV, Element.COLOR);
    public static final VAOLayout POS_TEX_COLOR_NORMAL = new VAOLayout(Element.POS, Element.UV, Element.COLOR, Element.NORMAL);

    private final List<Element> elements;
    private final List<Integer> offsets;
    private final int stride;

    public VAOLayout(Element... elements) {
        this.elements = new ArrayList<>();
        this.offsets = new ArrayList<>();
        int offset = 0;
        for (Element element : elements) {
            this.elements.add(element);
            this.offsets.add(offset);
            offset += element.size;
        }
        this.stride = offset;
    }


    /** Total size in bytes of a single vertex. */
    public int getStride() {
        return stride;
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
    public int getOffset(Usage usage) {
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i).usage == usage) {
                return offsets.get(i);
            }
        }
        return -1;
    }

    public enum Type {
        UBYTE(1),
        BYTE(1),
        USHORT(2),
        SHORT(2),
        UINT(4),
        INT(4),
        FLOAT(4),
        DOUBLE(8),
        ;
        public final int size;
        Type(int size) {
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

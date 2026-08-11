package cam72cam.mod.model.common.mesh;

import java.util.ArrayList;
import java.util.List;

public class VAOLayout {
    public static final VAOLayout POS_UV_COLOR = new VAOLayout()
            .addElement(Usage.POSITION, Type.FLOAT, 3)
            .addElement(Usage.UV, Type.FLOAT, 2)
            .addElement(Usage.COLOR, Type.FLOAT, 4);

    public static final VAOLayout POS_UV_COLOR_NORMAL = new VAOLayout()
            .addElement(Usage.POSITION, Type.FLOAT, 3)
            .addElement(Usage.UV, Type.FLOAT, 2)
            .addElement(Usage.COLOR, Type.FLOAT, 4)
            .addElement(Usage.NORMAL, Type.FLOAT, 3);

    public enum Type {
        FLOAT(4), UBYTE(1), BYTE(1), USHORT(2), SHORT(2), UINT(4), INT(4);
        public final int size;
        Type(int size) {
            this.size = size;
        }
    }

    public enum Usage {
        POSITION, NORMAL, COLOR, UV, PADDING
    }

    public static class Element {
        public final Usage usage;
        public final Type type;
        public final int count;
        final int size;

        private Element(Usage usage, Type type, int count) {
            this.usage = usage;
            this.type = type;
            this.count = count;
            this.size = type.size * count;
        }
    }

    private final List<Element> elements = new ArrayList<>();
    private final List<Integer> offsets = new ArrayList<>();
    private int stride;

    public VAOLayout addElement(Usage usage, Type type, int count) {
        Element element = new Element(usage, type, count);
        elements.add(element);
        offsets.add(stride);
        stride += element.size;
        return this;
    }

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
}

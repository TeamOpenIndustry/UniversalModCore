package cam72cam.mod.gui_v2.control;

import cam72cam.mod.gui_v2.core.layout.ILayoutable;
import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;

/**
 * Abstraction of panels that could set children's relative positions statically, like AnchorPane.
 * <p>
 * Inheritors should either extend and expose child positions related methods or provide their own ones.
 */
public abstract class PositionedPanel<T extends PositionedPanel<T>> extends AbstractPanel<T> {
    private final Object2LongArrayMap<ILayoutable<?>> childrenPositions = new Object2LongArrayMap<>();

    protected PositionedPanel(int width, int height) {
        super(width, height);
    }

    protected void addChildren(AbstractWidget<?> child, int relX, int relY) {
        super.addChild(child);
        setChildPosition(child, relX, relY);
    }

    protected void setChildPosition(AbstractWidget<?> child, int relX, int relY) {
        if (!getChildren().contains(child)) {
            return;
        }
        childrenPositions.put(child, (long) relX << 32 | relY);
    }

    public int getChildRelX(AbstractWidget<?> child) {
        if (childrenPositions.containsKey(child)) {
            return (int) (childrenPositions.getLong(child) >> 32);
        }
        return -1;
    }

    public int getChildRelY(AbstractWidget<?> child) {
        if (childrenPositions.containsKey(child)) {
            return (int) childrenPositions.getLong(child);
        }
        return -1;
    }
}

package cam72cam.mod.gui_v2.control;

import cam72cam.mod.gui_v2.core.ILayoutable;
import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;

/**
 * Abstraction of panels that could set children's relative positions statically, like AnchorPane
 */
public abstract class PositionedPanel<T extends PositionedPanel<T>> extends AbstractPanel<T> {
    private final Object2LongArrayMap<ILayoutable<?>> childrenPositions = new Object2LongArrayMap<>();

    public PositionedPanel(int width, int height) {
        super(width, height);
    }

    public void addChildren(ILayoutable<?> child, int relX, int relY) {
        super.addChildren(child);
        setChildPosition(child, relX, relY);
    }

    protected void setChildPosition(ILayoutable<?> child, int relX, int relY) {
        if (!children.contains(child)) {
            return;
        }
        childrenPositions.put(child, (long) relX << 32 | relY);
    }

    protected int getChildRelX(ILayoutable<?> child) {
        if (childrenPositions.containsKey(child)) {
            return (int) (childrenPositions.getLong(child) >> 32);
        }
        return 0;
    }

    protected int getChildRelY(ILayoutable<?> child) {
        if (childrenPositions.containsKey(child)) {
            return (int) childrenPositions.getLong(child);
        }
        return 0;
    }
}

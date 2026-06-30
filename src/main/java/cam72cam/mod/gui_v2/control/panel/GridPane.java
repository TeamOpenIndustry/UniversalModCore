package cam72cam.mod.gui_v2.control.panel;

import cam72cam.mod.gui_v2.control.AbstractWidget;
import cam72cam.mod.gui_v2.control.PositionedPanel;
import cam72cam.mod.gui_v2.core.layout.ILayoutable;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.ArrayList;
import java.util.List;

public class GridPane extends PositionedPanel<GridPane> {
    private final Object2IntOpenHashMap<AbstractWidget<?>> childColRow;
    private int calculatedWidth, calculatedHeight;

    private final Int2IntOpenHashMap colWidths = new Int2IntOpenHashMap();
    private final Int2IntOpenHashMap rowHeights = new Int2IntOpenHashMap();
    private final List<Integer> sortedCols = new ArrayList<>();
    private final List<Integer> sortedRows = new ArrayList<>();
    private final Int2IntOpenHashMap colOffsets = new Int2IntOpenHashMap();
    private final Int2IntOpenHashMap rowOffsets = new Int2IntOpenHashMap();

    public GridPane(int width, int height) {
        super(width, height);
        this.childColRow = new Object2IntOpenHashMap<>();
        this.childColRow.defaultReturnValue(-1);
        this.calculatedWidth = width;
        this.calculatedHeight = height;

        this.colWidths.defaultReturnValue(0);
        this.rowHeights.defaultReturnValue(0);
    }

    public void addChild(AbstractWidget<?> child, int col, int row) {
        super.addChild(child);
        setChildColRow(child, col, row);
    }

    public void setChildColRow(AbstractWidget<?> child, int col, int row) {
        childColRow.put(child, (col << 16) | (row & 0xFFFF));
        requestLayout();
    }

    public int getChildColumn(AbstractWidget<?> child) {
        int packed = childColRow.getInt(child);
        return packed == -1 ? -1 : (packed >> 16);
    }

    public int getChildRow(AbstractWidget<?> child) {
        int packed = childColRow.getInt(child);
        return packed == -1 ? -1 : (packed & 0xFFFF);
    }

    @Override
    public int width() {
        return calculatedWidth;
    }

    @Override
    public int height() {
        return calculatedHeight;
    }

    @Override
    public void layout(int x, int y) {
        setX(x);
        setY(y);

        List<AbstractWidget<?>> children = getChildren();
        boolean hasVisible = false;

        colWidths.clear();
        rowHeights.clear();
        sortedCols.clear();
        sortedRows.clear();

        for (AbstractWidget<?> child : children) {
            if (!child.isVisible()) continue;
            int col = getChildColumn(child);
            int row = getChildRow(child);
            if (col < 0 || row < 0) continue;

            hasVisible = true;
            colWidths.put(col, Math.max(colWidths.get(col), child.width()));
            rowHeights.put(row, Math.max(rowHeights.get(row), child.height()));
        }

        if (!hasVisible) {
            calculatedWidth = 0;
            calculatedHeight = 0;
            super.setWidth(0);
            super.setHeight(0);
            return;
        }

        sortedCols.addAll(colWidths.keySet());
        sortedRows.addAll(rowHeights.keySet());
        sortedCols.sort(Integer::compareTo);
        sortedRows.sort(Integer::compareTo);

        colOffsets.clear();
        int colSum = 0;
        for (int col : sortedCols) {
            colOffsets.put(col, colSum);
            colSum += colWidths.get(col);
        }
        calculatedWidth = colSum;

        rowOffsets.clear();
        int rowSum = 0;
        for (int row : sortedRows) {
            rowOffsets.put(row, rowSum);
            rowSum += rowHeights.get(row);
        }
        calculatedHeight = rowSum;

        super.setWidth(calculatedWidth);
        super.setHeight(calculatedHeight);

        for (AbstractWidget<?> child : children) {
            if (!child.isVisible()) continue;

            int col = getChildColumn(child);
            int row = getChildRow(child);
            if (col < 0 || row < 0) continue;

            int childX = colOffsets.get(col);
            int childY = rowOffsets.get(row);

            setChildPosition(child, childX, childY);

            child.setX(x + childX);
            child.setY(y + childY);
            child.layout(x + childX, y + childY);
        }
    }
}
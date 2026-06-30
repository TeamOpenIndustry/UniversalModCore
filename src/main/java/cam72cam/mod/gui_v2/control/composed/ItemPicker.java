package cam72cam.mod.gui_v2.control.composed;

import cam72cam.mod.gui_v2.control.ComposedWidget;
import cam72cam.mod.gui_v2.control.panel.GridPane;
import cam72cam.mod.gui_v2.control.panel.ScrollPane;
import cam72cam.mod.gui_v2.control.widget.Button;
import cam72cam.mod.gui_v2.control.widget.TextField;
import cam72cam.mod.gui_v2.rendering.GuiRenderer;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.text.PlayerMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import java.util.*;

public class ItemPicker extends ComposedWidget<ItemPicker> {

    private static final int SEARCH_HEIGHT = 20;
    private static final int SCROLLBAR_WIDTH = 10; // 与 ScrollPane 垂直滚动条宽度一致
    private static final int ITEM_SIZE = GuiRenderer.ITEM_SIZE;

    private final int columns;
    private final int visibleRows;
    private final Consumer<ItemStack> onItemSelected;

    private final TextField searchField;
    private final ScrollPane scroll;
    private final GridPane grid;
    private final List<ItemStack> allItems = new ArrayList<>();

    /**
     * 创建一个固定列数的物品选择器
     * @param columns      每行显示的物品数量（决定面板宽度）
     * @param visibleRows  可见的行数（决定面板高度，超出部分可滚动）
     * @param onItemSelected 物品选中回调
     */
    public ItemPicker(int columns, int visibleRows, Consumer<ItemStack> onItemSelected) {
        super(columns * ITEM_SIZE + SCROLLBAR_WIDTH,
              SEARCH_HEIGHT + visibleRows * ITEM_SIZE);
        this.columns = columns;
        this.visibleRows = visibleRows;
        this.onItemSelected = onItemSelected;

        searchField = TextField.of(width(), SEARCH_HEIGHT)
                               .callback(this::onSearchTextChanged)
                               .validator(s -> true);

        grid = new GridPane(0, 0);

        scroll = ScrollPane.vertical(width(), visibleRows * ITEM_SIZE);
        scroll.addChild(grid);
        scroll.setBarVisible(true);

        addChildren(searchField, 0, 0);
        addChildren(scroll, 0, SEARCH_HEIGHT);
    }

    public void addItems(Collection<ItemStack> items) {
        allItems.addAll(items);
        applyFilter();
    }

    public void setItems(List<ItemStack> items) {
        allItems.clear();
        allItems.addAll(items);
        applyFilter();
    }

    public void clearItems() {
        allItems.clear();
        applyFilter();
    }

    // ===================== 内部逻辑 =====================

    private void onSearchTextChanged(String text) {
        applyFilter();
    }

    private void applyFilter() {
        String searchText = searchField.getText().toLowerCase();
        String[] parts = searchText.isEmpty() ? new String[0] : searchText.split(" ");
        List<ItemStack> filtered = allItems.stream()
                                           .filter(stack -> parts.length == 0 || Arrays.stream(parts).allMatch(
                                                   part -> stack.getDisplayName().toLowerCase().contains(part)))
                                           .collect(Collectors.toList());
        rebuildGrid(filtered);
    }

    private void rebuildGrid(List<ItemStack> items) {
        grid.clearChildren();

        int row = 0, col = 0;
        for (ItemStack stack : items) {
            final ItemStack selected = stack;
            Button btn = Button.of(GuiRenderer.ITEM_SIZE, GuiRenderer.ITEM_SIZE, PlayerMessage.direct(""))
                               .callback((hand, self) -> {
                                   if (onItemSelected != null) {
                                       onItemSelected.accept(selected);
                                   }
                               })
                               .item(stack);
            grid.addChild(btn, col, row);

            col++;
            if (col >= columns) {
                col = 0;
                row++;
            }
        }

        requestLayout(); // 触发重新布局
    }

    // 尺寸固定，禁止外部修改
    @Override public void setWidth(int width) { /* no-op */ }
    @Override public void setHeight(int height) { /* no-op */ }
}
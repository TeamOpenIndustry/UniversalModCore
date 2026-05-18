package cam72cam.mod.gui_v2.control.composed;

import cam72cam.mod.gui_v2.control.ComposedWidget;
import cam72cam.mod.gui_v2.control.panel.HBox;
import cam72cam.mod.gui_v2.control.panel.VBox;
import cam72cam.mod.gui_v2.rendering.GuiRenderer;
import cam72cam.mod.item.ItemStack;

import cam72cam.mod.gui_v2.control.panel.ScrollPane;
import cam72cam.mod.gui_v2.control.widget.Button;
import cam72cam.mod.gui_v2.control.widget.TextField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ItemPicker extends ComposedWidget<ItemPicker> {
    private static final int SEARCH_HEIGHT = 20;

    private final TextField searchField;
    private final ScrollPane scrollPane;
    //TODO GridPane
    private final VBox rowsContainer;

    private final List<ItemStack> allItems = new ArrayList<>();
    private final Consumer<ItemStack> onItemSelected;
    private List<ItemStack> filteredItems = new ArrayList<>();

    public ItemPicker(int width, int height, Consumer<ItemStack> onItemSelected) {
        super(width, height);
        rowsContainer = new VBox(0);
        scrollPane = new ScrollPane(width, height);
        scrollPane.addChildren(rowsContainer);
        addChildren(scrollPane, 0, 0);

        this.onItemSelected = onItemSelected;

        searchField = new TextField(width, SEARCH_HEIGHT, this::onSearchTextChanged);
        searchField.setValidator(s -> true);

        addChildren(searchField, 0, 0);
        addChildren(scrollPane, 0, SEARCH_HEIGHT);
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

    private void onSearchTextChanged(String text) {
        applyFilter();
    }

    private void applyFilter() {
        String searchText = searchField.getText().toLowerCase();
        String[] parts = searchText.isEmpty() ? new String[0] : searchText.split(" ");
        filteredItems = allItems.stream()
                                .filter(stack -> Arrays.stream(parts).allMatch(part -> stack.getDisplayName().toLowerCase().contains(part)))
                                .collect(Collectors.toList());
        rebuildGrid();
    }

    private void rebuildGrid() {
        rowsContainer.clearChildren();
        int cols = Math.max(1, (scrollPane.panelWidth()) / GuiRenderer.ITEM_SIZE);
        HBox currentRow = null;
        for (int i = 0; i < filteredItems.size(); i++) {
            if (i % cols == 0) {
                currentRow = new HBox(0);
                rowsContainer.addChildren(currentRow);
            }
            int finalI = i;
            currentRow.addChildren(Button.item(filteredItems.get(i), (btn) -> {
                if (onItemSelected != null) {
                    onItemSelected.accept(filteredItems.get(finalI));
                }
            }));
        }
        requestLayout();
    }

    @Override
    public void layout(int x, int y) {
        super.layout(x, y);
    }
}

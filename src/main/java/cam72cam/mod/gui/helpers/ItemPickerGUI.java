package cam72cam.mod.gui.helpers;

import cam72cam.mod.item.ItemStack;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.text.StringTextComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** GUI that presents a standard item chooser */
public class ItemPickerGUI {
    private final ItemPickerScreen screen;
    public ItemStack choosenItem;
    private NonNullList<ItemStack> items;
    private final Consumer<ItemStack> onExit;

    /** Construct with a list of all available items and an exit (chosen or null) handler */
    public ItemPickerGUI(List<ItemStack> items, Consumer<ItemStack> onExit) {
        this.items = NonNullList.create();
        this.items.addAll(items);
        this.onExit = onExit;
        this.screen = new ItemPickerScreen();
    }

    /** Update items displayed */
    public void setItems(List<ItemStack> items) {
        this.items = NonNullList.create();
        this.items.addAll(items);
        screen.init();
    }

    /** Has at least one option to choose */
    public boolean hasOptions() {
        return this.items.size() != 0;
    }

    /** Show this UI as the current screen */
    public void show() {
        if (this.items.size() == 1) {
            onExit.accept(this.items.get(0));
            return;
        }
        Minecraft.getInstance().setScreen(screen);
    }

    /** Internal screen that actually renders and chooses the items */
    private class ItemPickerScreen extends Screen {
        private Map<Widget, Vector3i> buttonCoordList = new HashMap<>();
        private GuiScrollBar scrollBar;

        protected ItemPickerScreen() {
            super(new StringTextComponent(""));
        }

        @Override
        public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            this.renderBackground(matrixStack);
            super.render(matrixStack, mouseX, mouseY, partialTicks);

            for (Widget button : this.buttons) {
                if (button instanceof GuiScrollBar) continue;
                if (scrollBar != null) {
                    button.y = buttonCoordList.get(button).getY() - (int) Math.floor(scrollBar.getValue() * 32);
                }
                if (((ItemButton) button).isMouseOver(mouseX, mouseY)) {
                    this.renderTooltip(matrixStack, ((ItemButton) button).stack.internal, mouseX, mouseY);
                }
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public void init() {
            if (width == 0 || height == 0) {
                return;
            }
            int startX = this.width / 16;
            int startY = this.height / 8;

            int stacksX = this.width * 7 / 8 / 32;
            int stacksY = this.height * 7 / 8 / 32;

            this.buttons.clear();
            this.buttonCoordList.clear();
            startX += Math.max(0, (stacksX - items.size()) / 2) * 32;
            int i;
            for (i = 0; i < items.size(); i++) {
                int col = i % stacksX;
                int row = i / stacksX;
                this.addButton(new ItemButton(items.get(i), startX + col * 32, startY + row * 32) {
                    @Override
                    public void onPress() {
                        choosenItem = stack;
                        onExit.accept(stack);
                    }
                });
                this.buttonCoordList.put(this.buttons.get(this.buttons.size()-1), new Vector3i(startX + col * 32, startY + row * 32, 0));
            }
            int rows = i / stacksX + 2;
            if (stacksY < rows) {
                this.scrollBar = new GuiScrollBar(i++, this.width - 30, 4, 20, this.height - 8, "", 0.0, rows - stacksY, 0.0, null);
                this.addButton(this.scrollBar);
            }
        }

        @Override
        public boolean keyPressed(int typedChar, int keyCode, int mod) {
            if (super.keyPressed(typedChar, keyCode, mod)) {
                onExit.accept(null);
                return true;
            }
            if (keyCode == 1) {
                onExit.accept(null);
                return true;
            }
            return false;
        }
    }
}

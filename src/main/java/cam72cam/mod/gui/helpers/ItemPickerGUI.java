package cam72cam.mod.gui.helpers;

import cam72cam.mod.item.ItemStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ItemPickerGUI {
    private final ItemPickerScreen screen;
    public ItemStack choosenItem;
    private List<ItemStack> items;
    private Consumer<ItemStack> onExit;

    public ItemPickerGUI(List<ItemStack> items, Consumer<ItemStack> onExit) {
        this.items = new ArrayList<>();
        this.items.addAll(items);
        this.onExit = onExit;
        this.screen = new ItemPickerScreen();
    }


    public void setItems(List<ItemStack> items) {
        this.items = new ArrayList<>();
        this.items.addAll(items);
        screen.init();
    }

    public boolean hasOptions() {
        return this.items.size() != 0;
    }

    public void show() {
        if (this.items.size() == 1) {
            onExit.accept(this.items.get(0));
            return;
        }
        net.minecraft.client.MinecraftClient.getInstance().openScreen(screen);
    }

    private class ItemPickerScreen extends Screen {
        private Map<AbstractButtonWidget, Vec3i> buttonCoordList = new HashMap<>();
        private GuiScrollBar scrollBar;

        protected ItemPickerScreen() {
            super(new LiteralText(""));
        }

        @Override
        public void render(int mouseX, int mouseY, float partialTicks) {
            this.renderBackground();
            super.render(mouseX, mouseY, partialTicks);

            for (AbstractButtonWidget button : this.buttons) {
                if (button instanceof GuiScrollBar) continue;
                if (scrollBar != null) {
                    button.y = buttonCoordList.get(button).getY() - (int) Math.floor(scrollBar.getValue() * 32);
                }
                if (((ItemButton) button).isMouseOver(mouseX, mouseY)) {
                    this.renderTooltip(((ItemButton) button).stack.internal, mouseX, mouseY);
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
                ItemButton btn = new ItemButton(i, items.get(i), startX + col * 32, startY + row * 32) {
                    @Override
                    public void onClick(double double_1, double double_2) {
                        choosenItem = stack;
                        onExit.accept(choosenItem);
                    }
                };
                this.buttons.add(btn);
                this.buttonCoordList.put(btn, new Vec3i(startX + col * 32, startY + row * 32, 0));
            }
            int rows = i / stacksX + 2;
            if (stacksY < rows) {
                this.scrollBar = new GuiScrollBar(this.width - 30, 4, 20, this.height - 8, "", 0.0, rows - stacksY, 0.0);
                this.buttons.add(this.scrollBar);
            }
        }

        @Override
        public boolean keyPressed(int typedChar, int keyCode, int mod) {
            if (keyCode == 1) {
                onExit.accept(null);
                return true;
            }
            return false;
        }
    }
}

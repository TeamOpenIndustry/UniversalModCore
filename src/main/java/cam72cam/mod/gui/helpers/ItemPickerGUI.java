package cam72cam.mod.gui.helpers;

import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.util.ArrayList;
import java.util.List;
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
        screen.initGui();
    }

    public boolean hasOptions() {
        return this.items.size() != 0;
    }

    public void show() {
        if (this.items.size() == 1) {
            onExit.accept(this.items.get(0));
            return;
        }
        Minecraft.getMinecraft().displayGuiScreen(screen);
    }

    private class ItemPickerScreen extends GuiScreen {
        private List<Vec3i> buttonCoordList = new ArrayList<>();
        private GuiScrollBar scrollBar;

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.drawDefaultBackground();
            super.drawScreen(mouseX, mouseY, partialTicks);

            for (GuiButton button : (List<GuiButton>)this.buttonList) {
                if (button instanceof GuiScrollBar) continue;
                if (scrollBar != null) {
                    button.yPosition = buttonCoordList.get(button.id).y - (int) Math.floor(scrollBar.getValue() * 32);
                }
                if (((ItemButton) button).isMouseOver(mouseX, mouseY)) {
                    if (((ItemButton) button).stack.internal != null) {
                        this.renderToolTip(((ItemButton) button).stack.internal, mouseX, mouseY);
                    } else {
                        List<String> text = new ArrayList<>();
                        text.add(((ItemButton) button).stack.getDisplayName());
                        this.drawHoveringText(text, mouseX, mouseY, fontRendererObj);
                    }
                }
            }
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

        @Override
        public void initGui() {
            if (width == 0 || height == 0) {
                return;
            }
            int startX = this.width / 16;
            int startY = this.height / 8;

            int stacksX = this.width * 7 / 8 / 32;
            int stacksY = this.height * 7 / 8 / 32;

            this.buttonList.clear();
            this.buttonCoordList.clear();
            startX += Math.max(0, (stacksX - items.size()) / 2) * 32;
            int i;
            for (i = 0; i < items.size(); i++) {
                int col = i % stacksX;
                int row = i / stacksX;
                this.buttonList.add(new ItemButton(i, items.get(i), startX + col * 32, startY + row * 32));
                this.buttonCoordList.add(new Vec3i(startX + col * 32, startY + row * 32, 0));
            }
            int rows = i / stacksX + 2;
            if (stacksY < rows) {
                this.scrollBar = new GuiScrollBar(i++, this.width - 30, 4, 20, this.height - 8, "", 0.0, rows - stacksY, 0.0, null);
                this.buttonList.add(this.scrollBar);
            }
        }

        @Override
        public void actionPerformed(GuiButton button) {
            for (GuiButton itemButton : (List<GuiButton>)this.buttonList) {
                if (itemButton == button && !(button instanceof GuiScrollBar)) {
                    choosenItem = ((ItemButton) button).stack;
                    onExit.accept(choosenItem);
                    break;
                }
            }
        }

        @Override
        protected void keyTyped(char typedChar, int keyCode) {
            if (keyCode == 1) {
                onExit.accept(null);
            }
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }
}

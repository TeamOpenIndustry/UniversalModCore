package cam72cam.mod.gui.helpers;

import cam72cam.mod.gui.screen.TextField;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** GUI that presents a standard item chooser */
public class ItemPickerGUI {
    private final ItemPickerScreen screen;
    public ItemStack choosenItem;
    private List<ItemStack> items;
    private Consumer<ItemStack> onExit;

    /** Construct with a list of all available items and an exit (chosen or null) handler */
    public ItemPickerGUI(List<ItemStack> items, Consumer<ItemStack> onExit) {
        this.items = new ArrayList<>();
        this.items.addAll(items);
        this.onExit = onExit;
        this.screen = new ItemPickerScreen();
    }

    /** Update items displayed */
    public void setItems(List<ItemStack> items) {
        this.items = new ArrayList<>();
        this.items.addAll(items);
        screen.initGui();
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
        Minecraft.getMinecraft().displayGuiScreen(screen);
    }

    /** Internal screen that actually renders and chooses the items */
    private class ItemPickerScreen extends GuiScreen {
        private GuiTextField search;
        private final List<Vec3i> buttonCoordList = new ArrayList<>();
        private GuiScrollBar scrollBar;

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            this.drawDefaultBackground();
            super.drawScreen(mouseX, mouseY, partialTicks);

            search.drawTextBox();

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
            int startY = Math.max(this.height / 8, 40);

            int stacksX = this.width * 7 / 8 / 32;
            int stacksY = this.height * 7 / 8 / 32;
            if (search == null) {
                this.search = new GuiTextField(Minecraft.getMinecraft().fontRendererObj, width / 2 - 100, 20, 200, 20);
            } else {
                this.search.xPosition = width / 2 - 100;
                this.search.height = 20;
            }
            this.search.setFocused(true);
            this.buttonList.clear();
            this.buttonCoordList.clear();
            String[] searchParts = this.search.getText().toLowerCase(Locale.ROOT).split(" ");
            List<ItemStack> filteredItems = ItemPickerGUI.this.items.stream()
                    .filter(stack -> Arrays.stream(searchParts).allMatch(searchText -> {
                        if (stack.internal == null) {
                            return false;
                        }
                        if (stack.getDisplayName().toLowerCase(Locale.ROOT).contains(searchText)) {
                            return true;
                        }
                        List<?> tt = stack.internal.getTooltip(Minecraft.getMinecraft().thePlayer, false);
                        if (tt == null) {
                            return false;
                        }
                        return tt.stream().map(x -> (String) x).anyMatch(tip -> tip.toLowerCase(Locale.ROOT).contains(searchText));
                    })).collect(Collectors.toList());
            startX += Math.max(0, (stacksX - filteredItems.size()) / 2) * 32;
            int i;
            for (i = 0; i < filteredItems.size(); i++) {
                int col = i % stacksX;
                int row = i / stacksX;
                this.buttonList.add(new ItemButton(i, filteredItems.get(i), startX + col * 32, startY + row * 32));
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
            if (search.textboxKeyTyped(typedChar, keyCode)) {
                initGui();
            }
        }

        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            search.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }
}

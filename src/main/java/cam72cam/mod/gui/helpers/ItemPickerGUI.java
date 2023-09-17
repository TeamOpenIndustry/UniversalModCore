package cam72cam.mod.gui.helpers;

import cam72cam.mod.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.TooltipFlag;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
        private EditBox search;
        private Map<AbstractWidget, Vec3i> buttonCoordList = new HashMap<>();
        private GuiScrollBar scrollBar;

        protected ItemPickerScreen() {
            super(new TextComponent(""));
        }

        @Override
        public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            this.renderBackground(matrixStack);
            super.render(matrixStack, mouseX, mouseY, partialTicks);

            search.render(matrixStack, mouseX, mouseY, partialTicks);

            for (Widget button : this.renderables) {
                if (button instanceof ItemButton) {
                    if (scrollBar != null) {
                        ((AbstractWidget) button).y = buttonCoordList.get(button).getY() - (int) Math.floor(scrollBar.getValue() * 32);
                    }
                    if (((ItemButton) button).isMouseOver(mouseX, mouseY)) {
                        this.renderTooltip(matrixStack, ((ItemButton) button).stack.internal, mouseX, mouseY);
                    }
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
            int startY = Math.max(this.height / 8, 40);

            int stacksX = this.width * 7 / 8 / 32;
            int stacksY = this.height * 7 / 8 / 32;

            this.clearWidgets();
            this.buttonCoordList.clear();

            if (search == null) {
                this.search = new EditBox(Minecraft.getInstance().font, width / 2 - 100, 20, 200, 20, new TextComponent(""));
            } else {
                this.search.x = width / 2 - 100;
                this.search.setHeight(20);
            }

            this.search.setFocus(true);
            this.addRenderableWidget(this.search);

            String[] searchParts = this.search.getValue().toLowerCase(Locale.ROOT).split(" ");
            List<ItemStack> filteredItems = ItemPickerGUI.this.items.stream()
                    .filter(stack -> Arrays.stream(searchParts).allMatch(searchText ->
                            stack.getDisplayName().toLowerCase(Locale.ROOT).contains(searchText) ||
                            stack.internal.getTooltipLines(null, TooltipFlag.Default.NORMAL).stream()
                                    .anyMatch(tip -> tip.getString().toLowerCase(Locale.ROOT).contains(searchText))
                    )).collect(Collectors.toList());
            startX += Math.max(0, (stacksX - filteredItems.size()) / 2) * 32;
            int i;
            for (i = 0; i < filteredItems.size(); i++) {
                int col = i % stacksX;
                int row = i / stacksX;
                this.addRenderableWidget(new ItemButton(filteredItems.get(i), startX + col * 32, startY + row * 32) {
                    @Override
                    public void onPress() {
                        choosenItem = stack;
                        onExit.accept(stack);
                    }
                });
                this.buttonCoordList.put((AbstractWidget) this.renderables.get(this.renderables.size()-1), new Vec3i(startX + col * 32, startY + row * 32, 0));
            }
            int rows = i / stacksX + 2;
            if (stacksY < rows) {
                this.scrollBar = new GuiScrollBar(i++, this.width - 30, 4, 20, this.height - 8, "", 0.0, rows - stacksY, 0.0, b -> {});
                this.addRenderableWidget(this.scrollBar);
            }
        }

        @Override
        public boolean charTyped(char p_charTyped_1_, int p_charTyped_2_) {
            String oldSearch = search.getValue();
            boolean value = super.charTyped(p_charTyped_1_, p_charTyped_2_);
            if (!Objects.equals(oldSearch, search.getValue())) {
                init();
            }
            return value;
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

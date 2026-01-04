package cam72cam.mod.gui.screen;

import cam72cam.mod.entity.Player;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.input.Keyboard;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class ScreenBuilder extends Screen implements IScreenBuilder {
    private final IScreen screen;
    private final Map<AbstractWidget, Button> buttonMap = new HashMap<>();
    private final Map<EditBox, TextField> textFieldMap = new HashMap<>();
    private final Supplier<Boolean> valid;
    private PoseStack stack;

    public ScreenBuilder(IScreen screen, Supplier<Boolean> valid) {
        super(Component.literal(""));
        this.screen = screen;
        this.valid = valid;
    }
    
    @Override
    public void tick() {
        super.tick();
        if (!valid.get()) {
            this.close();
        }
    }

    // IScreenBuilder

    @Override
    public void close() {
        this.minecraft.setScreen(null);
        if (this.minecraft.screen == null) {
            this.minecraft.setWindowActive(true);
        }
        screen.onClose();
    }
/*
    @Override
    public void onClose() {
        screen.onClose();
        super.onClose();
    }*/

    @Override
    public void addButton(Button btn) {
        super.addRenderableWidget(btn.internal());
        this.buttonMap.put(btn.internal(), btn);
    }

    @Override
    public void addTextField(TextField textField) {
        super.addRenderableWidget(textField.internal());
        this.textFieldMap.put(textField.internal(), textField);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void drawImage(Identifier tex, int x, int y, int width, int height) {
        GUIHelpers.texturedRect(tex, this.width / 2 + x, this.height / 4 + y, width, height);
    }

    @Override
    public void drawTank(int x, int y, int width, int height, Fluid fluid, float fluidPercent, boolean background, int color) {
        GUIHelpers.drawTankBlock(this.width / 2 + x, this.height / 4 + y, width, height, fluid, fluidPercent, background, color);
    }

    @Override
    public void drawCenteredString(String str, int x, int y, int color) {
        drawCenteredString(stack, this.font, str, this.width / 2 + x, this.height / 4 + y, color);
    }

    @Override
    public void show() {
        this.minecraft.setScreen(this);
    }

    // GuiScreen

    @Override
    public void init() {
        buttonMap.clear();
        textFieldMap.clear();
        screen.init(this);
    }

    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float partialTicks) {
        this.stack = stack;
        GUIHelpers.initDelayed();
        for (Button btn : buttonMap.values()) {
            btn.onUpdate();
        }

        screen.draw(this, new RenderState(stack).depth_test(true).stage(RenderContext.Stage.GUI));

        // draw buttons
        super.render(stack, mouseX, mouseY, partialTicks);
        Optional<Button> first = buttonMap.values().stream()
                                          .filter(Button::isHovering)
                                          .filter(button -> button.tooltips != null)
                                          .findFirst();
        first.ifPresent(button -> GUIHelpers.drawTooltipAtCursor(button.tooltips));

        GUIHelpers.runDelayed(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int typedChar, int keyCode, int mods) {
        if (typedChar == 256 && this.shouldCloseOnEsc()) {
            close();
            return true;
        }
        if (super.keyPressed(typedChar, keyCode, mods)) {
            return true;
        }

        if (this.textFieldMap.keySet().stream()
                             .noneMatch(txt -> txt.keyPressed(typedChar, keyCode, mods))) {
            screen.onKeyType(this, Keyboard.KeyCode.of(typedChar));
        }

        return true;
    }

    @Override
    public boolean charTyped(char p_charTyped_1_, int p_charTyped_2_) {
        return this.textFieldMap.keySet().stream()
                             .anyMatch(txt -> txt.charTyped(p_charTyped_1_, p_charTyped_2_));
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        Player.Hand hand = button == 0 ? Player.Hand.PRIMARY : Player.Hand.SECONDARY;

        if (this.buttonMap.keySet().stream().anyMatch(btn -> btn.mouseClicked(x, y, button))) {
            return true;
        }

        if (this.textFieldMap.keySet().stream().noneMatch(txt -> {
            if (txt.mouseClicked(x, y, button)) {
                txt.setFocused(true);
                return true;
            }
            return false;
        })) {
            screen.onMouseClick((int) x, (int) y, hand);
        }
        return true;
    }

    // Default overrides
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

package cam72cam.mod.gui.screen;

import cam72cam.mod.entity.Player;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.input.Keyboard;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.widget.ExtendedSlider;

import java.util.*;
import java.util.function.Supplier;

public class ScreenBuilder extends Screen implements IScreenBuilder {
    private final IScreen screen;
    private final Map<AbstractWidget, Button> buttonMap = new HashMap<>();
    private final List<TextField> textFields = new ArrayList<>();
    private TextField active = null;
    private final Supplier<Boolean> valid;
    private GuiGraphics graphics;
    private ExtendedSlider dragging;

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
        this.textFields.add(textField);
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
        graphics.drawCenteredString(this.font, str, this.width / 2 + x, this.height / 4 + y, color);
    }

    @Override
    public void show() {
        this.minecraft.setScreen(this);
    }

    // GuiScreen

    @Override
    public void init() {
        buttonMap.clear();
        textFields.clear();
        screen.init(this);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.graphics = graphics;
        GUIHelpers.graphics = graphics;// This is horrifying and needs to change
        GUIHelpers.initDelayed();
        for (Button btn : buttonMap.values()) {
            btn.onUpdate();
        }

        screen.draw(this, new RenderState(graphics.pose()).depth_test(true).stage(RenderContext.Stage.GUI));

        // draw buttons
        super.render(graphics, mouseX, mouseY, partialTicks);
        Optional<Button> first = buttonMap.values().stream()
                                          .filter(Button::isHovering)
                                          .filter(button -> button.tooltips != null)
                                          .findFirst();
        first.ifPresent(button -> GUIHelpers.drawTooltipAtCursor(button.tooltips));

        GUIHelpers.runDelayed(mouseX, mouseY);
    }

    @Override
    protected void renderBlurredBackground() {
        //Do nothing here, this doesn't exist below 1.20.5
        //TODO backport?
    }

    @Override
    public boolean keyPressed(int typedChar, int keyCode, int mods) {
        if (typedChar == 256 && this.shouldCloseOnEsc()) {
            close();
            return true;
        }
        //See cam72cam.mod.mixin.fix.screen_navigation.MixinScreen
        if (super.keyPressed(typedChar, keyCode, mods)) {
            return true;
        }

        if (this.active != null && !active.internal.keyPressed(typedChar, keyCode, mods)) {
            screen.onKeyType(this, Keyboard.KeyCode.of(keyCode));
        }

        return true;
    }

    @Override
    public boolean charTyped(char p_charTyped_1_, int p_charTyped_2_) {
        if (active != null) {
            return active.internal.charTyped(p_charTyped_1_, p_charTyped_2_);
        }
        return super.charTyped(p_charTyped_1_, p_charTyped_2_);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        Player.Hand hand = button == 0 ? Player.Hand.PRIMARY : Player.Hand.SECONDARY;

        for (AbstractWidget btn : this.buttonMap.keySet()) {
            if (btn.mouseClicked(x, y, button)) {
                if (btn instanceof ExtendedSlider slider) {
                    dragging = slider;
                }
                return true;
            }
        }

        for (TextField field : textFields) {
            if (field.isVisible() && field.internal.mouseClicked(x, y, hand == Player.Hand.PRIMARY ? 0 : 1)) {
                active = field;
                field.setFocused(true);
                return true;
            }
            field.setFocused(false);
        }

        screen.onMouseClick((int) x, (int) y, hand);
        return true;
    }

    @Override
    public boolean mouseDragged(double p_94699_, double p_94700_, int p_94701_, double p_94702_, double p_94703_) {
        if (dragging != null) {
            dragging.mouseDragged(p_94699_, p_94700_, p_94701_, p_94702_, p_94703_);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double p_94722_, double p_94723_, int p_94724_) {
        dragging = null;
        return false;
    }

    // Default overrides
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

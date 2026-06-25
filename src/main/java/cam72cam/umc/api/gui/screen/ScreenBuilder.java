package cam72cam.umc.api.gui.screen;

import cam72cam.umc.api.entity.Player;
import cam72cam.umc.api.fluid.Fluid;
import cam72cam.umc.api.gui.helpers.GUIHelpers;
import cam72cam.umc.api.render.opengl.RenderContext;
import cam72cam.umc.api.input.Keyboard;
import cam72cam.umc.api.render.opengl.RenderState;
import cam72cam.umc.api.resource.Identifier;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

public class ScreenBuilder extends GuiScreen implements IScreenBuilder {
    private final IScreen screen;
    private final Map<GuiButton, Button> buttonMap = new HashMap<>();
    private final List<TextField> textFields = new ArrayList<>();
    private TextField active = null;
    private final Supplier<Boolean> valid;

    public ScreenBuilder(IScreen screen, Supplier<Boolean> valid) {
        this.screen = screen;
        this.valid = valid;
    }
    
    @Override
    public void updateScreen() {
        super.updateScreen();
        if (!valid.get()) {
            this.close();
        }
    }

    // IScreenBuilder

    @Override
    public void close() {
        this.mc.displayGuiScreen(null);
        if (this.mc.currentScreen == null) {
            this.mc.setIngameFocus();
        }
        screen.onClose();
    }

    @Override
    public void addButton(Button btn) {
        super.buttonList.add(btn.button);
        this.buttonMap.put(btn.button, btn);
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
        super.drawCenteredString(this.fontRenderer, str, this.width / 2 + x, this.height / 4 + y, color);
    }

    @Override
    public void show() {
        this.mc.displayGuiScreen(this);
    }

    @Override
    public void addTextField(TextField textField) {
        this.textFields.add(textField);
    }

    // GuiScreen

    @Override
    public void initGui() {
        buttonMap.clear();
        textFields.clear();
        screen.init(this);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GUIHelpers.initDelayed();
        for (Button btn : buttonMap.values()) {
            btn.onUpdate();
        }


        screen.draw(this, new RenderState().stage(RenderContext.Stage.GUI));

        textFields.stream().map(t -> t.internal).forEach(GuiTextField::drawTextBox);

        // draw buttons
        super.drawScreen(mouseX, mouseY, partialTicks);
        Optional<Button> first = buttonMap.values().stream()
                                          .filter(Button::isHovering)
                                          .filter(button -> button.tooltips != null)
                                          .findFirst();
        first.ifPresent(button -> GUIHelpers.drawTooltipAtCursor(button.tooltips));

        GUIHelpers.runDelayed(mouseX, mouseY);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            close();
        }

        if (this.active != null && !active.internal.textboxKeyTyped(typedChar, keyCode)) {
            screen.onKeyType(this, Keyboard.KeyCode.of(keyCode));
        }
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // Copy pasta to support right / left button click
        Player.Hand hand = mouseButton == 0 ? Player.Hand.PRIMARY : Player.Hand.SECONDARY;

        for (GuiButton guibutton : this.buttonList) {
            if (guibutton.mousePressed(this.mc, mouseX, mouseY)) {
                this.selectedButton = guibutton;
                guibutton.playPressSound(this.mc.getSoundHandler());
                buttonMap.get(guibutton).onClick(hand);
            }
        }

        for (TextField field : textFields) {
            if (field.isVisible() && field.internal.mouseClicked(mouseX, mouseY, mouseButton)) {
                active = field;
                field.setFocused(true);
                return;
            }
            field.setFocused(false);
        }

        screen.onMouseClick(mouseX, mouseY, hand);
    }

    // Default overrides
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

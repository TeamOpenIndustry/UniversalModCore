package cam72cam.mod.gui;

import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.resource.Identifier;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.text.LiteralText;

import java.util.HashMap;
import java.util.Map;

class ScreenBuilder extends Screen implements IScreenBuilder {
    private final IScreen screen;
    private Map<AbstractButtonWidget, Button> buttonMap = new HashMap<>();

    public ScreenBuilder(IScreen screen) {
        super(new LiteralText(""));
        this.screen = screen;
    }

    // IScreenBuilder

    @Override
    public void onClose() {
        screen.onClose();
        super.onClose();
    }

    @Override
    public void close() {
        onClose();
    }

    @Override
    public void addButton(Button btn) {
        super.buttons.add(btn.internal());
        this.buttonMap.put(btn.internal(), btn);
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
        this.minecraft.getTextureManager().bindTexture(tex.internal);

        GUIHelpers.texturedRect(this.width / 2 + x, this.height / 4 + y, width, height);
    }

    @Override
    public void drawTank(double x, int y, double width, double height, Fluid fluid, float fluidPercent, boolean background, int color) {
        GUIHelpers.drawTankBlock(this.width / 2 + x, this.height / 2 + y, width, height, fluid, fluidPercent, background, color);
    }

    @Override
    public void drawCenteredString(String str, int x, int y, int color) {
        super.drawCenteredString(this.font, str, this.width / 2 + x, this.height / 4 + y, color);
    }

    @Override
    public void show() {
        this.minecraft.openScreen(this);
    }

    @Override
    public void addTextField(TextField textField) {
        addButton(textField);
    }

    // GuiScreen

    @Override
    public void init() {
        super.init();
        screen.init(this);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        for (Button btn : buttonMap.values()) {
            btn.onUpdate();
        }

        screen.draw(this);

        //textFields.forEach(TextFieldWidget::render);

        // draw buttons
        super.render(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean keyPressed(int typedChar, int keyCode, int mods) {
        if (super.keyPressed(typedChar, keyCode, mods)) {
            return true;
        }
        if (keyCode == 1) {
            close();
        }

        // Enter
        if (keyCode == 28 || keyCode == 156) {
            screen.onEnterKey(this);
            return true;
        }
        return false;
    }

    // Default overrides
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

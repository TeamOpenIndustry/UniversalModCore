package cam72cam.mod.gui.screen;

import cam72cam.mod.entity.Player;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.resource.Identifier;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@SideOnly(Side.CLIENT)
public class ScreenBuilder extends GuiScreen implements IScreenBuilder {
    private final IScreen screen;
    private final Map<GuiButton, Button> buttonMap = new HashMap<>();
    private final List<GuiTextField> textFields = new ArrayList<>();
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
        super.drawCenteredString(this.fontRendererObj, str, this.width / 2 + x, this.height / 4 + y, color);
    }

    @Override
    public void show() {
        this.mc.displayGuiScreen(this);
    }

    @Override
    public void addTextField(TextField textField) {
        this.textFields.add(textField.textfield);
    }

    // GuiScreen

    @Override
    public void initGui() {
        screen.init(this);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        for (Button btn : buttonMap.values()) {
            btn.onUpdate();
        }

        screen.draw(this);

        textFields.forEach(GuiTextField::drawTextBox);

        // draw buttons
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) {
            close();
        }

        // Enter
        if (keyCode == 28 || keyCode == 156) {
            screen.onEnterKey(this);
        }

        this.textFields.forEach(x -> x.textboxKeyTyped(typedChar, keyCode));
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // Copy pasta to support right / left button click

        for (int i = 0; i < this.buttonList.size(); ++i) {
            GuiButton guibutton = (GuiButton) super.buttonList.get(i);

            if (guibutton.mousePressed(this.mc, mouseX, mouseY)) {
                //TODO 1.11.2 will this break anything? this.selectedButton = guibutton;
                guibutton.playPressSound(this.mc.getSoundHandler());
                buttonMap.get(guibutton).onClick(mouseButton == 0 ? Player.Hand.PRIMARY : Player.Hand.SECONDARY);
            }
        }

        this.textFields.forEach(x -> x.mouseClicked(mouseX, mouseY, mouseButton));
    }

    // Default overrides
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

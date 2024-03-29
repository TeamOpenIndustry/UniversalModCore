package cam72cam.mod.gui.screen;

import cam72cam.mod.entity.Player;
import cam72cam.mod.fluid.Fluid;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.resource.Identifier;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ScreenBuilder extends Screen implements IScreenBuilder {
    private final IScreen screen;
    private Map<AbstractWidget, Button> buttonMap = new HashMap<>();
    private final Supplier<Boolean> valid;
    private GuiGraphics graphics;

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
        if (btn instanceof TextField) {
            this.setFocused(btn.internal());
        }
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

    @Override
    public void addTextField(TextField textField) {
        addButton(textField);
    }

    // GuiScreen

    @Override
    public void init() {
        buttonMap.clear();
        screen.init(this);
    }

    @Override
    public void render(GuiGraphics graphics, int p_281550_, int p_282878_, float p_282465_) {
        this.graphics = graphics;
        GUIHelpers.graphics = graphics;// This is horrifying and needs to change
        for (Button btn : buttonMap.values()) {
            btn.onUpdate();
        }

        screen.draw(this, new RenderState(graphics.pose()).depth_test(true));

        // draw buttons
        super.render(graphics, p_281550_, p_282878_, p_282465_);
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

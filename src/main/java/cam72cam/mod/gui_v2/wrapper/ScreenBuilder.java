package cam72cam.mod.gui_v2.wrapper;

import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui_v2.GUIUtils;
import cam72cam.mod.gui_v2.control.widget.Slider;
import cam72cam.mod.gui_v2.rendering.GUIRenderer;
import cam72cam.mod.gui_v2.control.AbstractPanel;
import cam72cam.mod.gui_v2.control.widget.Button;
import cam72cam.mod.gui_v2.control.panel.VerticalPanel;
import cam72cam.mod.text.PlayerMessage;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.function.BiConsumer;

public class ScreenBuilder extends GuiScreen {
    private final AbstractPanel root;

    public ScreenBuilder() {
        //Test
        this.root = new VerticalPanel(0, 0, GUIHelpers.getScreenWidth(), GUIHelpers.getScreenHeight());
        BiConsumer<Player.Hand, Button<?>> btnTest = (hand, btn) -> System.out.println(btn.hashCode());
        Button<?> button1 = new Button(150, 20, PlayerMessage.direct("clicker"), btnTest);
        Button<?> button2 = new Button(150, 20, PlayerMessage.direct("clicker2"), btnTest);
        Button<?> button3 = new Button(150, 20, PlayerMessage.direct("clicker3"), btnTest);
        Slider<?> slider1 = new Slider(150, 20, PlayerMessage.direct("slider"), 0, 1, 0, false, slider -> System.out.println(((Slider<?>)slider).getValue()));
        root.addChildren(button1, button2, button3, slider1);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GUIUtils.mouseX = mouseX;
        GUIUtils.mouseY = mouseY;
        GUIRenderer renderer = new GUIRenderer(this);
        root.renderBackground(renderer);
        root.render(renderer);
        root.renderForeground(renderer);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        //TODO Mouse key detection
        root.onClick(Player.Hand.PRIMARY, mouseX, mouseY);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long deltaTicks) {
        root.onDrag(Player.Hand.PRIMARY, mouseX, mouseY);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        root.onRelease(Player.Hand.PRIMARY, mouseX, mouseY);
    }

    protected void mouseScrolled(int mouseX, int mouseY, double delta) {
        root.onScroll(mouseX, mouseY, delta);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        //TODO Mixin?
        int i = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int j = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

        double scroll = Math.signum(org.lwjgl.input.Mouse.getEventDWheel());
        if (scroll != 0) {
            this.mouseScrolled(i, j, scroll);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

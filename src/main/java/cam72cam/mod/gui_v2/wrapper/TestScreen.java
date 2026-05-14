package cam72cam.mod.gui_v2.wrapper;

import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui_v2.GuiUtils;
import cam72cam.mod.gui_v2.control.panel.ScrollPane;
import cam72cam.mod.gui_v2.control.panel.SimplePane;
import cam72cam.mod.gui_v2.control.widget.Slider;
import cam72cam.mod.gui_v2.control.widget.Button;
import cam72cam.mod.gui_v2.core.ScissorStack;
import cam72cam.mod.gui_v2.rendering.GuiRenderer;
import cam72cam.mod.gui_v2.control.AbstractPanel;
import cam72cam.mod.gui_v2.control.panel.VBox;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.text.PlayerMessage;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.function.BiConsumer;

public class TestScreen extends GuiScreen {
    private final SimplePane root;

    public TestScreen() {
        GuiUtils.current = this;
        //Test
        VBox vBox = new VBox(5);
        BiConsumer<Player.Hand, Button> btnTest = (hand, btn) -> System.out.println(btn.hashCode());
        Button button1 = Button.vanilla(150, 20, PlayerMessage.direct("clicker"), btnTest);
        Button button2 = Button.vanilla(150, 20, PlayerMessage.direct("clicker2"), btnTest);
        Button button3 = Button.textured(150, 20, PlayerMessage.direct("clicker3"), btnTest, new Identifier("textures/blocks/bedrock.png"));
        Slider horizontal = new Slider(150, 20, PlayerMessage.direct("slider"), 0, 1, 0, false,
                                       slider -> System.out.println(slider.getValue()), true);
        Slider vertical = new Slider(20, 150, PlayerMessage.direct("slider"), 0, 1, 0, false,
                                       slider -> System.out.println(slider.getValue()), false);
        ScrollPane pane = new ScrollPane(300, 200);
        vBox.addChildren(button1, button2, button3, horizontal, vertical);
        pane.addChildren(vBox);
        SimplePane pane1 = new SimplePane(GuiUtils.getScreenWidth(), GuiUtils.getScreenHeight());
        //TODO rel position
        pane1.addChildren(pane);
        this.root = pane1;
    }

    public void layout() {
        this.root.layout(0, 0);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GuiUtils.mouseX = mouseX;
        GuiUtils.mouseY = mouseY;
        GuiRenderer renderer = new GuiRenderer(this);
        ScissorStack stack = new ScissorStack();
        root.renderPanel(renderer, stack);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
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
    public void onGuiClosed() {
        super.onGuiClosed();
        GuiUtils.current = null;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

package cam72cam.mod.gui_v2.core;

import cam72cam.mod.entity.Player;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.gui_v2.GuiUtils;
import cam72cam.mod.gui_v2.control.panel.AnchorPane;
import cam72cam.mod.gui_v2.rendering.GuiRenderer;
import cam72cam.mod.input.Keyboard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.io.IOException;

public class ScreenWrapper extends GuiScreen {
    static ScreenWrapper instance;

    final ClientScreen clientScreen;
    final AnchorPane root;
    final boolean pausesGame;

    static {
        ClientEvents.TICK.subscribe(() -> {
            if (instance != null) {
                instance.onTick();
            } else if (Minecraft.getMinecraft().currentScreen instanceof ScreenWrapper) {
                instance = ((ScreenWrapper) Minecraft.getMinecraft().currentScreen);
            }
        });
    }

    public ScreenWrapper(ClientScreen screen, boolean pausesGame) {
        this.root = AnchorPane.fullScreen();
        this.clientScreen = screen;
        this.pausesGame = pausesGame;
        screen.bootstrap(this);
        instance = this;
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

        int i = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int j = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

        double scroll = Math.signum(org.lwjgl.input.Mouse.getEventDWheel());
        if (scroll != 0) {
            this.mouseScrolled(i, j, scroll);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        Keyboard.KeyCode key = Keyboard.KeyCode.of(keyCode);
        boolean consumed = false;
        if (key != null) {
            consumed = root.onKeyPressed(key);
        }
        if (!consumed && GuiUtils.isPrintable(typedChar)) {
            root.onCharTyped(typedChar);
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        this.clientScreen.onClose();
        instance = null;
    }

    @Override
    public void onResize(Minecraft mcIn, int w, int h) {
        super.onResize(mcIn, w, h);
        this.root.setBound(0, 0, w, h);
        this.root.layout(0, 0);
    }

    private void onTick() {
        this.root.onTick();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return this.pausesGame;
    }

    public static ScreenWrapper getInstance() {
        return instance;
    }
}

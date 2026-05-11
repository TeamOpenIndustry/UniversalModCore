package cam72cam.mod.gui_v2.wrapper;

import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.helpers.GUIHelpers;
import cam72cam.mod.gui_v2.GUIUtils;
import cam72cam.mod.gui_v2.rendering.GUIRenderer;
import cam72cam.mod.gui_v2.control.AbstractPanel;
import cam72cam.mod.gui_v2.control.widget.Button;
import cam72cam.mod.gui_v2.control.panel.VerticalPanel;
import cam72cam.mod.text.PlayerMessage;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

public class ScreenBuilder extends GuiScreen {
    private final AbstractPanel root;

    public ScreenBuilder() {
        //Test
        this.root = new VerticalPanel(0, 0, GUIHelpers.getScreenWidth(), GUIHelpers.getScreenHeight());
        Button<?> button1 = new Button(150, 20, PlayerMessage.direct("clicker"), (hand, btn) -> System.out.println(btn.hashCode()));
        Button<?> button2 = new Button(150, 20, PlayerMessage.direct("clicker2"), (hand, btn) -> System.out.println(btn.hashCode()));
        Button<?> button3 = new Button(150, 20, PlayerMessage.direct("clicker3"), (hand, btn) -> System.out.println(btn.hashCode()));
        root.addChildren(button1, button2, button3);
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
        root.consumeClick(Player.Hand.PRIMARY, mouseX, mouseY);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

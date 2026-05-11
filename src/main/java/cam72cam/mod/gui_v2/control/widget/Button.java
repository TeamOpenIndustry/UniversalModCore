package cam72cam.mod.gui_v2.control.widget;

import cam72cam.mod.entity.Player;
import cam72cam.mod.gui_v2.rendering.GUIRenderer;
import cam72cam.mod.gui_v2.control.AbstractButton;
import cam72cam.mod.text.PlayerMessage;

import java.util.function.BiConsumer;

public class Button<T extends Button<T>> extends AbstractButton<Button<T>> {

    /** Custom width/height */
    public Button(int width, int height, PlayerMessage name, BiConsumer<Player.Hand, Button<T>> handler) {
        super(0, 0, width, height, name, handler);
        setEnabled(true);
    }

    @Override
    public void render(GUIRenderer renderer) {
        boolean isHovering = isHovering();
        int i = !isEnabled()
                ? 0
                : isHovering ? 2 : 1;
        renderer.drawVanillaButton(getX(), getY(), getWidth(), getHeight(), i);

        int j = 14737632;

        if (nameColor != 0) {
            j = nameColor;
        } else if (!this.enabled) {
            j = 10526880;
        } else if (isHovering) {
            j = 16777120;
        }

        renderer.drawCenteredString(this.name.internal.getFormattedText(), this.x + this.width / 2, this.y + (this.height - 8) / 2, j);
    }

    @Override
    public void renderBackground(GUIRenderer renderer) {

    }

    @Override
    public void renderForeground(GUIRenderer renderer) {

    }
}

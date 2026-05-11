package cam72cam.mod.gui_v2.control.widget;

import cam72cam.mod.gui_v2.control.AbstractSlider;
import cam72cam.mod.gui_v2.rendering.GUIRenderer;
import cam72cam.mod.text.PlayerMessage;
import net.minecraft.client.renderer.GlStateManager;

import java.util.function.Consumer;

public class Slider<T extends Slider<T>> extends AbstractSlider<T> {
    public Slider(PlayerMessage text, double min, double max, double start, boolean doublePrecision, Consumer<T> handler) {
        super(text, min, max, start, doublePrecision, handler);
    }

    public Slider(int width, int height, PlayerMessage text, double min, double max, double start, boolean doublePrecision, Consumer<T> handler) {
        super(width, height, text, min, max, start, doublePrecision, handler);
    }

    public Slider(int width, int height, PlayerMessage text, double min, double max, double start, boolean doublePrecision, Consumer<T> handler, boolean isHorizontal) {
        super(width, height, text, min, max, start, doublePrecision, handler, isHorizontal);
    }

    @Override
    public void render(GUIRenderer renderer) {
        renderer.drawVanillaButton(getX(), getY(), getWidth(), getHeight(), 0);

        double ratio = (value - min) / (max - min);
        ratio = Math.max(0.0, Math.min(1.0, ratio));

        if (isHorizontal) {
            int trackWidth = getWidth() - 8;
            int handleX = getX() + (int) (ratio * trackWidth);
            int handleY = getY();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            renderer.texturedRect(GUIRenderer.VANILLA_BUTTON, handleX, handleY, 0, 66, 4, 20);
            renderer.texturedRect(GUIRenderer.VANILLA_BUTTON, handleX + 4, handleY, 196, 66, 4, 20);


        } else {
            //TODO Vertical
//            int trackHeight = getHeight() - 8;
//            int handleX = getX();
//            int handleY = getY() + (int) ((1.0 - ratio) * trackHeight);
//
//            renderer.setColor(1.0F, 1.0F, 1.0F, 1.0F);
//            renderer.drawTexturedModalRect(handleX, handleY, 0, 66, 20, 4);
//            renderer.drawTexturedModalRect(handleX, handleY + 4, 196, 66, 20, 4);
        }


        int j = 14737632;

        if (nameColor != 0) {
            j = nameColor;
        } else if (isHovering()) {
            j = 16777120;
        }

        renderer.drawCenteredString(this.name.internal.getFormattedText(), this.x + this.width / 2, this.y + (this.height - 8) / 2, j);
    }
}

package cam72cam.mod.gui_v2.control;

import cam72cam.mod.entity.Player;
import cam72cam.mod.gui_v2.core.actions.IClickable;
import cam72cam.mod.gui_v2.core.actions.IDraggable;
import cam72cam.mod.gui_v2.rendering.GUIRenderer;
import cam72cam.mod.text.PlayerMessage;
import net.minecraft.util.math.MathHelper;

import java.util.function.Consumer;

public abstract class AbstractSlider<T extends AbstractSlider<T>>
        extends AbstractWidget
        implements IClickable, IDraggable {
    protected final boolean isHorizontal;

    protected double min;
    protected double max;
    protected double value;
    protected int displayPrecision;

    protected boolean isDragging;

    protected Consumer<T> handler;

    //TODO
    // 1.Int precision
    // 2.Prefix/Suffix text
    public AbstractSlider(PlayerMessage text, double min, double max, double start, boolean doublePrecision, Consumer<T> handler) {
        this(150, 20, text, min, max, start, doublePrecision, handler);
    }

    public AbstractSlider(int width, int height, PlayerMessage text, double min, double max, double start, boolean doublePrecision, Consumer<T> handler) {
        this(width, height, text, min, max, start, doublePrecision, handler, true);
    }

    public AbstractSlider(int width, int height, PlayerMessage text, double min, double max, double start, boolean doublePrecision, Consumer<T> handler, boolean isHorizontal) {
        this.setBound(0, 0, width, height);
        this.name = text;
        this.min = min;
        this.max = max;
        this.value = start;
        this.displayPrecision = doublePrecision ? 4 : 0;
        this.handler = handler;
        this.isHorizontal = isHorizontal;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void setSliderBound(double min, double max) {
        this.min = min;
        this.max = max;
        this.value = MathHelper.clamp(min, max, value);
    }

    @Override
    public boolean onClick(Player.Hand hand, int x, int y) {
        if (!isHovering()) {
            return false;
        }
        updateSlider(x, y);
        return true;
    }

    @Override
    public boolean onDrag(Player.Hand hand, int mouseX, int mouseY) {
        if (!isHovering()) {
            if (!isDragging) {
                return false;
            }
        }
        isDragging = true;
        updateSlider(mouseX, mouseY);
        return true;
    }

    protected void updateSlider(int mouseX, int mouseY) {
        double oldValue = value;
        double ratio;

        if (isHorizontal) {
            double relX = mouseX - getX() - 4; //Slider bar size
            ratio = relX / (getWidth() - 8);
        } else {
            double relY = getY() + getHeight() - mouseY - 4;
            ratio = relY / (getHeight() - 8);
        }

        ratio = Math.max(0.0, Math.min(1.0, ratio));

        double rawValue = min + ratio * (max - min);
        value = Math.max(min, Math.min(max, rawValue));

        if (value != oldValue && handler != null) {
            handler.accept((T) this);
        }
    }

    @Override
    public boolean onRelease(Player.Hand hand, int mouseX, int mouseY) {
        if (isDragging) {
            isDragging = false;
            if (handler != null) {
                handler.accept((T) this);
            }
            return true;
        }
        return false;
    }

    @Override
    public void renderBackground(GUIRenderer renderer) {

    }

    @Override
    public void renderForeground(GUIRenderer renderer) {

    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);
        isDragging = false;
    }
}

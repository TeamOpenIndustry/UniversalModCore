package cam72cam.mod.gui_v2.control.widget;

import cam72cam.mod.entity.Player;
import cam72cam.mod.gui_v2.GuiUtils;
import cam72cam.mod.gui_v2.control.AbstractWidget;
import cam72cam.mod.gui_v2.core.actions.IClickable;
import cam72cam.mod.gui_v2.core.actions.IDraggable;
import cam72cam.mod.gui_v2.core.actions.IFocusable;
import cam72cam.mod.gui_v2.core.actions.IUpdatable;
import cam72cam.mod.gui_v2.rendering.GuiRenderer;
import cam72cam.mod.text.PlayerMessage;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class Slider extends AbstractWidget<Slider>
        implements IClickable, IDraggable, IFocusable, IUpdatable {
    protected final boolean isHorizontal;

    protected int handleSize;

    private double min;
    private double max;
    private double value;
    private int displayPrecision;

    protected String formatted;

    protected boolean isDragging;

    @Nonnull
    protected Consumer<Slider> handler;

    public Slider(int width, int height, PlayerMessage text, double min, double max, double start, int displayPrecision, Consumer<Slider> handler, boolean isHorizontal) {
        this.setBound(0, 0, width, height);
        this.min = min;
        this.max = max;
        this.value = start;
        this.displayPrecision = displayPrecision;
        this.handler = handler;
        this.isHorizontal = isHorizontal;
        this.setName(text);

        this.handleSize = 8; //Default

        setVanillaFacade();
    }

    /* Semitic constructors */
    public static Slider horizontal(int width, int height, PlayerMessage text, double min, double max, double start, int displayPrecision, Consumer<Slider> handler) {
        return new Slider(width, height, text, min, max, start, displayPrecision, handler, true);
    }

    public static Slider vertical(int width, int height, PlayerMessage text, double min, double max, double start, int displayPrecision, Consumer<Slider> handler) {
        return new Slider(width, height, text, min, max, start, displayPrecision, handler, false);
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
        this.handler.accept(this);
    }

    public double getMinBound() {
        return min;
    }

    public double getMaxBound() {
        return max;
    }

    public void setSliderBound(double min, double max) {
        this.min = min;
        this.max = max;
        this.value = Math.max(min, Math.min(value, max));
        this.handler.accept(this);
    }

    public void setDisplayPrecision(int displayPrecision) {
        this.displayPrecision = displayPrecision;
        this.formatName();
    }

    public void setHandleSize(int handleSize) {
        if (handleSize > this.height()) {
            handleSize = this.height();
        }
        this.handleSize = handleSize;
    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);
    }

    @Override
    public void setName(PlayerMessage text) {
        super.setName(text);
        this.formatName();
    }

    @Override
    public boolean onClick(Player.Hand hand, int x, int y) {
        if (!isHovering()) {
            return false;
        }
        requestFocus(this);
        updateSlider(x, y);
        return true;
    }

    @Override
    public boolean onDrag(Player.Hand hand, int mouseX, int mouseY) {
        if (!isDragging && !isHovering()) {
            return false;
        }
        updateSlider(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean onRelease(Player.Hand hand, int mouseX, int mouseY) {
        if (isDragging) {
            freeFocus();
            setValue(value);
            return true;
        }
        return false;
    }

    @Override
    public boolean isFocusing() {
        return this.isDragging;
    }

    @Override
    public void onFocusGained() {
        this.isDragging = true;
    }

    @Override
    public void onFocusLost() {
        this.isDragging = false;
    }

    @Override
    public void postRender() {
        if (this.isDragging) {
            updateSlider(GuiUtils.getMouseX(), GuiUtils.getMouseY());
        }
    }

    protected void updateSlider(int mouseX, int mouseY) {
        double ratio;

        if (isHorizontal) {
            double relX = mouseX - x() - handleSize / 2.0;
            ratio = relX / (width() - handleSize);
        } else {
            double relY = mouseY - y() - handleSize / 2.0;
            ratio = relY / (height() - handleSize);
        }

        ratio = Math.max(0.0, Math.min(1.0, ratio));

        double rawValue = min + ratio * (max - min);
        setValue(Math.max(min, Math.min(max, rawValue)));
    }

    private void formatName() {
        String text = this.getName().internal.getFormattedText();
        formatted = text.replace("slidValue", "%."+displayPrecision+"f");
    }

    public void setVanillaFacade() {
        this.setBackgroundRenderFunc((gui, slid) -> {
            //Render track
            gui.drawVanillaButton(slid.x(), slid.y(), slid.width(), slid.height(), 0);

            double ratio = (slid.value - slid.min) / (slid.max - slid.min);
            ratio = Math.max(0.0, Math.min(1.0, ratio));

            //Render slider handle
            if (slid.isHorizontal) {
                int trackWidth = slid.width() - handleSize;
                int handleX = slid.x() + (int) (ratio * trackWidth);
                int handleY = slid.y();
                gui.drawVanillaButton(handleX, handleY, handleSize, slid.height(), 1);
            } else {
                int trackHeight = slid.height() - handleSize;
                int handleX = slid.x();
                int handleY = slid.y() + (int) (ratio * trackHeight);
                gui.drawVanillaButton(handleX, handleY, slid.width(), handleSize, 1);
            }
        });
        this.setRenderFunc((gui, slid) -> {
            int j = slid.getNameColor() != 0 ? slid.getNameColor() :
                      slid.isHovering() ? 0xFFFFA0 : 0xE0E0E0;

            gui.drawCenteredString(String.format(formatted, value),
                                   slid.x() + slid.width() / 2, slid.y() + (slid.height() - GuiRenderer.TEXT_HEIGHT) / 2, j);
        });
    }
}

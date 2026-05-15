package cam72cam.mod.gui_v2.control.widget;

import cam72cam.mod.entity.Player;
import cam72cam.mod.gui_v2.GuiUtils;
import cam72cam.mod.gui_v2.control.AbstractWidget;
import cam72cam.mod.gui_v2.core.actions.IClickable;
import cam72cam.mod.gui_v2.core.actions.IDraggable;
import cam72cam.mod.gui_v2.core.actions.IUpdatable;
import cam72cam.mod.text.PlayerMessage;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class Slider extends AbstractWidget<Slider>
        implements IClickable, IDraggable, IUpdatable {
    protected final boolean isHorizontal;

    protected int handleSize;

    protected double min;
    protected double max;
    protected double value;
    protected int displayPrecision;

    protected String formatted;

    protected boolean isDragging;

    @Nonnull
    protected Consumer<Slider> handler;

    //TODO
    // 1.Prefix/Suffix text
    public Slider(int width, int height, PlayerMessage text, double min, double max, double start, int displayPrecision, Consumer<Slider> handler) {
        this(width, height, text, min, max, start, displayPrecision, handler, true);
    }

    public Slider(int width, int height, PlayerMessage text, double min, double max, double start, int displayPrecision, Consumer<Slider> handler, boolean isHorizontal) {
        this.setBound(0, 0, width, height);
        this.setName(text);
        this.min = min;
        this.max = max;
        this.value = start;
        this.displayPrecision = displayPrecision;
        this.handler = handler;
        this.isHorizontal = isHorizontal;

        this.handleSize = 8; //Default

        vanillaFacade();
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

    public void setSliderBound(double min, double max) {
        this.min = min;
        this.max = max;
        this.value = MathHelper.clamp(min, max, value);
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
    public boolean onClick(Player.Hand hand, int x, int y) {
        if (!isHovering()) {
            return false;
        }
        updateSlider(x, y);
        return true;
    }

    @Override
    public boolean onDrag(Player.Hand hand, int mouseX, int mouseY) {
        if (!isDragging) {
            if (!isHovering()) {
                return false;
            }
        }
        requestDragging(this);
        this.isDragging = true;
        updateSlider(mouseX, mouseY);
        return true;
    }

    protected void updateSlider(int mouseX, int mouseY) {
        double ratio;

        if (isHorizontal) {
            double relX = mouseX - x() - handleSize / 2.0; //Slider bar size
            ratio = relX / (width() - handleSize);
        } else {
            double relY = mouseY - y() - handleSize / 2.0;
            ratio = relY / (height() - handleSize);
        }

        ratio = Math.max(0.0, Math.min(1.0, ratio));

        double rawValue = min + ratio * (max - min);
        setValue(Math.max(min, Math.min(max, rawValue)));
    }

    @Override
    public boolean onRelease(Player.Hand hand, int mouseX, int mouseY) {
        if (isDragging) {
            isDragging = false;
            freeDragging();
            setValue(value);
            return true;
        }
        return false;
    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);
    }

    @Override
    public void postRender() {
        if (this.isDragging) {
            updateSlider(GuiUtils.getMouseX(), GuiUtils.getMouseY());
        }
    }

    public void vanillaFacade() {
        this.setBackgroundRenderFunc((gui, slid) -> {
            //Render track
            gui.drawVanillaButton(slid.x(), slid.y(), slid.width(), slid.height(), 0);

            double ratio = (slid.value - slid.min) / (slid.max - slid.min);
            ratio = Math.max(0.0, Math.min(1.0, ratio));

            //Render slider bar
            if (slid.isHorizontal) {
                int trackWidth = slid.width() - handleSize;
                int handleX = slid.x() + (int) (ratio * trackWidth);
                int handleY = slid.y();
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                gui.drawVanillaButton(handleX, handleY, handleSize, slid.height(), 1);
            } else {
                int trackHeight = slid.height() - handleSize;
                int handleX = slid.x();
                int handleY = slid.y() + (int) (ratio * trackHeight);

                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                gui.drawVanillaButton(handleX, handleY, slid.width(), handleSize, 1);
            }
        });
        this.setRenderFunc((gui, slid) -> {
            int j = slid.getNameColor() != 0 ? slid.getNameColor() :
                      slid.isHovering() ? 0xFFFFA0 : 0xE0E0E0;

            gui.drawCenteredString(String.format(formatted, value),
                                   slid.x() + slid.width() / 2, slid.y() + (slid.height() - GuiUtils.TEXT_HEiGHT) / 2, j);
        });
    }

    @Override
    public void setName(PlayerMessage text) {
        super.setName(text);
        this.formatName();
    }

    private void formatName() {
        String text = this.getName().internal.getFormattedText();
        formatted = text.replace("slidValue", "%."+displayPrecision+"f");
    }
}

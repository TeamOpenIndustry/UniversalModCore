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

    protected double min;
    protected double max;
    protected double value;
    protected int displayPrecision;

    protected boolean isDragging;

    @Nonnull
    protected Consumer<Slider> handler;

    //TODO
    // 1.Int precision
    // 2.Prefix/Suffix text
    public Slider(int width, int height, PlayerMessage text, double min, double max, double start, boolean doublePrecision, Consumer<Slider> handler) {
        this(width, height, text, min, max, start, doublePrecision, handler, true);
    }

    public Slider(int width, int height, PlayerMessage text, double min, double max, double start, boolean doublePrecision, Consumer<Slider> handler, boolean isHorizontal) {
        this.setBound(0, 0, width, height);
        this.setName(text);
        this.min = min;
        this.max = max;
        this.value = start;
        this.displayPrecision = doublePrecision ? 4 : 0;
        this.handler = handler;
        this.isHorizontal = isHorizontal;

        this.vanillaFacade();
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
            double relX = mouseX - x() - 4; //Slider bar size
            ratio = relX / (width() - 8);
        } else {
            double relY = mouseY - y() - 4;
            ratio = relY / (height() - 8);
        }

        ratio = Math.max(0.0, Math.min(1.0, ratio));

        double rawValue = min + ratio * (max - min);
        setValue(Math.max(min, Math.min(max, rawValue)));
    }

    @Override
    public boolean onRelease(Player.Hand hand, int mouseX, int mouseY) {
        if (isDragging) {
            isDragging = false;
            setValue(value);
            return true;
        }
        return false;
    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);
        isDragging = false;
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
                int trackWidth = slid.width() - 8;
                int handleX = slid.x() + (int) (ratio * trackWidth);
                int handleY = slid.y();
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                gui.drawVanillaButton(handleX, handleY, 8, slid.height(), 1);
            } else {
                int trackHeight = slid.height() - 8;
                int handleX = slid.x();
                int handleY = slid.y() + (int) ((1.0 - ratio) * trackHeight);

                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                gui.drawVanillaButton(handleX, handleY, slid.width(), 8, 1);
            }
        });
        this.setRenderFunc((gui, slid) -> {
            int j = slid.getNameColor() != 0 ? slid.getNameColor() :
                      slid.isHovering() ? 0xFFFFA0 : 0xE0E0E0;

            gui.drawCenteredString(slid.getName().internal.getFormattedText(), slid.x() + slid.width() / 2, slid.y() + (slid.height() - 8) / 2, j);
        });
    }
}

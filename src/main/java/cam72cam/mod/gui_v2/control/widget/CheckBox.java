package cam72cam.mod.gui_v2.control.widget;

import cam72cam.mod.entity.Player;
import cam72cam.mod.gui_v2.GuiUtils;
import cam72cam.mod.gui_v2.control.AbstractWidget;
import cam72cam.mod.gui_v2.core.actions.IClickable;
import cam72cam.mod.text.PlayerMessage;

import java.util.function.Consumer;

public class CheckBox extends AbstractWidget<CheckBox>
        implements IClickable {
    protected static final int CHECK_BOX_SIZE = 11;

    protected boolean checked;
    private Consumer<CheckBox> callback;

    protected CheckBox(PlayerMessage name, boolean checked) {
        this.setName(name);
        this.checked = checked;

        int width = CHECK_BOX_SIZE + 2 + GuiUtils.getTextWidth(this.getName());
        setWHInternal(width, CHECK_BOX_SIZE);

        vanilla();
    }

    public static CheckBox of(PlayerMessage name) {
        return of(name, false);
    }

    public static CheckBox of(PlayerMessage name, boolean defaultValue) {
        return new CheckBox(name, defaultValue);
    }

    public boolean isChecked() {
        return checked;
    }
    public CheckBox setChecked(boolean newValue) {
        this.checked = newValue;
        if (callback != null) {
            callback.accept(this);
        }
        return this;
    }

    public CheckBox clicker(Consumer<CheckBox> callback) {
        this.callback = callback;
        return this;
    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);
    }

    @Override
    public boolean onClick(Player.Hand hand, int mouseX, int mouseY) {
        if (!isHovering()) {
            return false;
        }
        checked = !checked;
        if (callback != null) {
            callback.accept(this);
        }
        return true;
    }

    @Override
    public void setName(PlayerMessage text) {
        super.setName(text);
        int newWidth = CHECK_BOX_SIZE + 2 + GuiUtils.getTextWidth(this.getName());
        setBound(0, 0, newWidth, height());
    }

    @Override
    public void setWidth(int width) {
        //NO-OP
    }

    @Override
    public void setHeight(int height) {
        //NO-OP
    }

    protected void setWHInternal(int width, int height) {
        super.setWidth(width);
        super.setHeight(height);
    }

    /* Default facade */
    public CheckBox vanilla() {
        return this.setRenderer((gui, cb) -> {
            gui.drawVanillaButton(cb.x(), cb.y(), CHECK_BOX_SIZE, CHECK_BOX_SIZE, 0);

            if (cb.isChecked()) {
                //TODO Better appearance
                gui.drawCenteredString("x", cb.x() + CHECK_BOX_SIZE / 2 + 1, cb.y() + 1, 14737632);
            }
            int color = cb.getNameColor() != 0 ? cb.getNameColor() :
                        !cb.isEnabled() ? 0xA0A0A0 : 0xE0E0E0;
            gui.drawString(cb.getName().internal.getFormattedText(), cb.x() + CHECK_BOX_SIZE + 2, cb.y() + 2, color);
        });
    }
}

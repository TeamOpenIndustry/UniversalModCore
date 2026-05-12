package cam72cam.mod.gui_v2.control.widget;

import cam72cam.mod.entity.Player;
import cam72cam.mod.gui_v2.control.AbstractWidget;
import cam72cam.mod.gui_v2.core.actions.IClickable;
import cam72cam.mod.gui_v2.core.actions.ITooltipper;
import cam72cam.mod.text.PlayerMessage;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public class Button extends AbstractWidget<Button>
        implements IClickable, ITooltipper {
    /**
     * Handler consumer, called upon clicked
     * Hand -> PRIMARY is a left-click, SECONDARY is a right-click
     * Button -> Reference of self, as it may not be fully constructed
     */
    protected BiConsumer<Player.Hand, Button> handler;
    protected List<PlayerMessage> tooltip;

    /** Custom width/height */
    public Button(int width, int height, PlayerMessage text, BiConsumer<Player.Hand, Button> handler) {
        this.x = 0;
        this.y = 0;
        this.width = width;
        this.height = height;
        this.name = text;
        this.handler = handler;

        this.vanillaFacade();
    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);
    }

    @Override
    public boolean onClick(Player.Hand hand, int x, int y) {
        if (isHovering()) {
            this.handler.accept(hand, this);
            return true;
        }
        return false;
    }

    /**
     * Set current widget's tooltip
     */
    @Override
    public void setTooltip(List<PlayerMessage> text) {
        this.tooltip = text;
    }

    @Override
    public List<PlayerMessage> getTooltips() {
        return Collections.singletonList(this.getName());
    }

    /* Facades */
    public void vanillaFacade() {
        this.setBackgroundRenderFunc((gui, btn) -> {
            int state = !btn.isEnabled() ? 0
                    : btn.isHovering() ? 2 : 1;
            gui.drawVanillaButton(btn.x(), btn.y(), btn.width(), btn.height(), state);
        });
        this.setRenderFunc((gui, btn) -> {
            int color = btn.nameColor != 0 ? btn.nameColor :
                        !btn.isEnabled() ? 0xA0A0A0 :
                        btn.isHovering() ? 0xFFFFA0 : 0xE0E0E0;
            gui.drawCenteredString(btn.getName().internal.getFormattedText(), btn.x + btn.width / 2, btn.y + (btn.height - 8) / 2, color);
        });
    }
}

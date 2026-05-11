package cam72cam.mod.gui_v2.control;

import cam72cam.mod.entity.Player;
import cam72cam.mod.gui_v2.core.actions.IClickable;
import cam72cam.mod.gui_v2.core.actions.ITooltipper;
import cam72cam.mod.gui_v2.core.actions.IUpdatable;
import cam72cam.mod.text.PlayerMessage;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public abstract class AbstractButton<T extends AbstractButton<T>>
        extends AbstractWidget
        implements IClickable, IUpdatable, ITooltipper {
    /**
     * Handler consumer, called upon clicked
     * Hand -> PRIMARY is a left-click, SECONDARY is a right-click
     * AbstractButton -> Reference of self, as it may not be fully constructed
     */
    protected BiConsumer<Player.Hand, T> handler;

    protected List<PlayerMessage> tooltip;

    /** Custom width/height */
    public AbstractButton(int x, int y, int width, int height, PlayerMessage text, BiConsumer<Player.Hand, T> handler) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.name = text;
        this.handler = handler;
    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);
    }

    @Override
    public boolean onClick(Player.Hand hand, int x, int y) {
        if (isHovering()) {
            this.handler.accept(hand, (T) this);
            this.onStateChange();
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

    @Override
    public void onStateChange() {}

    /** Called every screen draw */
    @Override
    public void postRender() {}
}

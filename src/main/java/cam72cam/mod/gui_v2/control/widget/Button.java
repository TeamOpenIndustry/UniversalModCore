package cam72cam.mod.gui_v2.control.widget;

import cam72cam.mod.entity.Player;
import cam72cam.mod.gui_v2.control.AbstractWidget;
import cam72cam.mod.gui_v2.core.actions.IClickable;
import cam72cam.mod.gui_v2.core.actions.ITooltipProvider;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.text.PlayerMessage;

import java.util.List;
import java.util.function.BiConsumer;

public class Button extends AbstractWidget<Button>
        implements IClickable, ITooltipProvider {
    /*
     * Handler consumer, called upon clicked
     * Hand -> PRIMARY is a left-click, SECONDARY is a right-click
     * Button -> Reference of self, as it may not be fully constructed
     */
    protected BiConsumer<Player.Hand, Button> handler;
    protected List<PlayerMessage> tooltip;

    /* Constructor with no rendering */
    public Button(int width, int height, PlayerMessage text) {
        this.setName(text);
        this.setBound(0, 0, width, height);
        vanilla();
    }

    public static Button of(int width, int height, PlayerMessage text) {
        return new Button(width, height, text);
    }

    public Button callback(BiConsumer<Player.Hand, Button> handler) {
        this.handler = handler;
        return this;
    }

    /* Default facades */
    public Button vanilla() {
        return this.setRenderer((gui, btn) -> {
            int state = !btn.isEnabled() ? 0
                                         : btn.isHovering() ? 2 : 1;
            gui.drawVanillaButton(btn.x(), btn.y(), btn.width(), btn.height(), state);
            int color = btn.getNameColor() != 0 ? btn.getNameColor() :
                        !btn.isEnabled() ? 0xA0A0A0 :
                        btn.isHovering() ? 0xFFFFA0 : 0xE0E0E0;
            gui.drawCenteredString(btn.getName().internal.getFormattedText(), btn.x() + btn.width() / 2, btn.y() + (btn.height() - 8) / 2, color);
        });
    }

    public Button textured(Identifier tex) {
        return this.textured(tex, 0, 0, 1, 1);
    }

    public Button textured(Identifier tex, float startU, float startV, float endU, float endV) {
        return this.setRenderer((gui, btn) -> {
            gui.blitTexture(tex, btn.x(), btn.y(), btn.width(), btn.height(), startU, startV, endU, endV);
            int color = btn.getNameColor() != 0 ? btn.getNameColor() :
                        !btn.isEnabled() ? 0xA0A0A0 :
                        btn.isHovering() ? 0xFFFFA0 : 0xE0E0E0;
            gui.drawCenteredString(btn.getName().internal.getFormattedText(), btn.x() + btn.width() / 2, btn.y() + (btn.height() - 8) / 2, color);
        });
    }

    public Button item(ItemStack stack) {
        return this.setRenderer((gui, btn) -> {
            gui.drawItem(stack, btn.x(), btn.y());
        });
    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);
    }

    @Override
    public boolean onClick(Player.Hand hand, int mouseX, int mouseY) {
        if (isHovering()) {
            this.handler.accept(hand, this);
            return true;
        }
        return false;
    }

    @Override
    public List<PlayerMessage> getTooltips() {
        return this.tooltip;
    }

    @Override
    public void setTooltip(List<PlayerMessage> text) {
        this.tooltip = text;
    }
}

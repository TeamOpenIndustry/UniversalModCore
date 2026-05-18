package cam72cam.mod.gui_v2.control.widget;

import cam72cam.mod.entity.Player;
import cam72cam.mod.gui_v2.control.AbstractWidget;
import cam72cam.mod.gui_v2.core.actions.IClickable;
import cam72cam.mod.gui_v2.core.actions.ITooltipProvider;
import cam72cam.mod.gui_v2.rendering.GuiRenderer;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.render.opengl.RenderContext;
import cam72cam.mod.render.opengl.RenderState;
import cam72cam.mod.render.opengl.Texture;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.text.PlayerMessage;
import cam72cam.mod.util.With;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
    public Button(int width, int height, PlayerMessage text, BiConsumer<Player.Hand, Button> handler) {
        this.setName(text);
        this.setBound(0, 0, width, height);
        this.handler = handler;

        setVanillaFacade();
        this.setTooltip(Collections.singletonList(this.getName()));
    }

    /* Semitic constructors */
    public static Button vanilla(int width, int height, PlayerMessage text, BiConsumer<Player.Hand, Button> handler) {
        return new Button(width, height, text, handler);
    }

    /* Render full texture */
    public static Button textured(int width, int height, PlayerMessage text, BiConsumer<Player.Hand, Button> handler,
                                  Identifier tex) {
        return textured(width, height, text, handler, tex, 0, 0, 1, 1);
    }

    /* Render sprite texture */
    public static Button textured(int width, int height, PlayerMessage text, BiConsumer<Player.Hand, Button> handler,
                                  Identifier tex, float startU, float startV, float endU, float endV) {
        Button button = new Button(width, height, text, handler);
        button.setBackgroundRenderFunc((gui, btn) -> {
            try (With ctx = RenderContext.apply(new RenderState().texture(Texture.wrap(tex)))) {
                gui.drawTexturedUvRect(tex, btn.x(), btn.y(), btn.width(), btn.height(), startU, startV, endU, endV);
            }
        });
        button.setMainRenderFunc((gui, btn) -> {
            int color = btn.getNameColor() != 0 ? btn.getNameColor() :
                        !btn.isEnabled() ? 0xA0A0A0 :
                        btn.isHovering() ? 0xFFFFA0 : 0xE0E0E0;
            gui.drawCenteredString(btn.getName().internal.getFormattedText(), btn.x() + btn.width() / 2, btn.y() + (btn.height() - 8) / 2, color);
        });
        return button;
    }

    public static Button item(ItemStack stack, Consumer<Button> callback) {
        Button button = new Button(GuiRenderer.ITEM_SIZE, GuiRenderer.ITEM_SIZE, PlayerMessage.direct(""),
                                   ((hand, btn) -> callback.accept(btn)));
        button.setBackgroundRenderFunc((gui, btn) -> {
//            gui.drawRect(btn.x(), btn.y(), btn.width(), btn.height(), 0x00000000);
        });
        button.setMainRenderFunc((gui, btn) -> {
            gui.drawItem(stack, btn.x(), btn.y());
        });
        return button;
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

    @Override
    public List<PlayerMessage> getTooltips() {
        return this.tooltip;
    }

    @Override
    public void setTooltip(List<PlayerMessage> text) {
        this.tooltip = text;
    }

    /* Facades */
    public void setVanillaFacade() {
        this.setBackgroundRenderFunc((gui, btn) -> {
            int state = !btn.isEnabled() ? 0
                    : btn.isHovering() ? 2 : 1;
            gui.drawVanillaButton(btn.x(), btn.y(), btn.width(), btn.height(), state);
        });
        this.setMainRenderFunc((gui, btn) -> {
            int color = btn.getNameColor() != 0 ? btn.getNameColor() :
                        !btn.isEnabled() ? 0xA0A0A0 :
                        btn.isHovering() ? 0xFFFFA0 : 0xE0E0E0;
            gui.drawCenteredString(btn.getName().internal.getFormattedText(), btn.x() + btn.width() / 2, btn.y() + (btn.height() - 8) / 2, color);
        });
    }
}

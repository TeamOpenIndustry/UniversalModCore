package cam72cam.mod.gui_v2.control.widget;

import cam72cam.mod.gui_v2.GuiUtils;
import cam72cam.mod.gui_v2.control.AbstractWidget;
import cam72cam.mod.text.PlayerMessage;

//TODO
// 1.How to handle multiline?
// 2.Scaling
public class Label extends AbstractWidget<Label> {
    private String formatted;

    public Label(PlayerMessage text, int textColor) {
        this.setName(text);
        this.setNameColor(textColor);
        this.setWidth(GuiUtils.getTextWidth(text));
        this.setHeight(8); //Default height
        this.formatted = text.internal.getFormattedText();
        this.setRenderFunc((gui, label) -> gui.drawString(formatted, x(), y(), getNameColor()));
    }

    public static Label direct(String text) {
        return direct(text, 0xE0E0E0);
    }

    public static Label direct(String text, int nameColor) {
        return new Label(PlayerMessage.direct(text), nameColor);
    }

    public static Label trans(String text) {
        return trans(text, 0xE0E0E0);
    }

    public static Label trans(String text, int nameColor) {
        return new Label(PlayerMessage.translate(text), nameColor);
    }

    public static Label url(String text) {
        return url(text, 0xE0E0E0);
    }

    public static Label url(String text, int nameColor) {
        return new Label(PlayerMessage.url(text), nameColor);
    }

    @Override
    public void setName(PlayerMessage text) {
        super.setName(text);
        this.formatted = text.internal.getFormattedText();
    }

    @Override
    public void layout(int x, int y) {
        this.setX(x);
        this.setY(y);
    }
}

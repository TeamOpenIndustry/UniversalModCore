package cam72cam.mod.gui;

import cam72cam.mod.util.Hand;
import com.google.common.base.Predicate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;

public class TextField extends Button {
    public TextField(IScreenBuilder builder, int x, int y, int width, int height) {
        super(builder, create(builder, x, y, width, height));
        builder.addTextField(this);
    }

    static TextFieldWidget create(IScreenBuilder builder, int x, int y, int width, int height) {
        return new TextFieldWidget(MinecraftClient.getInstance().textRenderer, builder.getWidth() / 2 + x, builder.getHeight() / 4 + y, width, height, "");
    }

    TextFieldWidget internal() {
        return (TextFieldWidget) button;
    }

    public void setValidator(Predicate<String> filter) {
        internal().setTextPredicate(filter);
    }

    public void setFocused(boolean b) {
        internal().method_1876(b);
    }

    public String getText() {
        return internal().getText();
    }

    public void setText(String s) {
        internal().setText(s);
    }

    @Override
    public void onClick(Hand hand) {

    }
}

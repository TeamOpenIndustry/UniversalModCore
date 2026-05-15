package cam72cam.mod.gui_v2.test;

import cam72cam.mod.entity.Player;
import cam72cam.mod.gui_v2.control.panel.AnchorPane;
import cam72cam.mod.gui_v2.control.panel.ScrollPane;
import cam72cam.mod.gui_v2.control.panel.VBox;
import cam72cam.mod.gui_v2.control.widget.Button;
import cam72cam.mod.gui_v2.control.widget.Label;
import cam72cam.mod.gui_v2.control.widget.Slider;
import cam72cam.mod.gui_v2.core.ClientScreen;
import cam72cam.mod.gui_v2.core.layout.HorizontalAlign;
import cam72cam.mod.gui_v2.core.layout.VerticalAlign;
import cam72cam.mod.resource.Identifier;
import cam72cam.mod.text.PlayerMessage;

import java.util.function.BiConsumer;

public class TestScreen extends ClientScreen {
    @Override
    public void init(AnchorPane root) {
        //Test
        VBox vBox = new VBox(5, HorizontalAlign.MIDDLE);
        BiConsumer<Player.Hand, Button> btnTest = (hand, btn) -> System.out.println(btn.hashCode());
        Label lab = Label.direct("label1");
        Button button1 = Button.vanilla(150, 20, PlayerMessage.direct("clicker"), btnTest);
        Label lab2 = Label.direct("label2");
        Button button2 = Button.vanilla(150, 20, PlayerMessage.direct("clicker2"), btnTest);
        Button button3 = Button.textured(150, 20, PlayerMessage.direct("clicker3"), btnTest, new Identifier("textures/blocks/bedrock.png"));
        Slider horizontal = Slider.horizontal(150, 20, PlayerMessage.direct("slider"), 0, 1, 0, 0,
                                              slider -> System.out.println(slider.getValue()));
        Slider vertical = Slider.vertical(20, 150, PlayerMessage.direct("slider"), 0, 1, 0, 0,
                                          slider -> System.out.println(slider.getValue()));
        ScrollPane pane = new ScrollPane(160, 200);
        vBox.addChildren(lab, button1, button2, lab2, button3, horizontal, vertical);
        pane.addChildren(vBox);
        root.addChildren(pane, HorizontalAlign.MIDDLE, 0, VerticalAlign.MIDDLE, 0);
    }
}

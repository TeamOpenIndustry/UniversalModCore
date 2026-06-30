package cam72cam.mod.gui_v2.test;

import cam72cam.mod.entity.Player;
import cam72cam.mod.gui_v2.control.widget.CyclableButton;
import cam72cam.mod.gui_v2.control.composed.ItemPicker;
import cam72cam.mod.gui_v2.control.composed.NumberField;
import cam72cam.mod.gui_v2.control.panel.AnchorPane;
import cam72cam.mod.gui_v2.control.panel.ScrollPane;
import cam72cam.mod.gui_v2.control.panel.VBox;
import cam72cam.mod.gui_v2.control.widget.*;
import cam72cam.mod.gui_v2.core.ClientScreen;
import cam72cam.mod.gui_v2.core.layout.HorizontalAlign;
import cam72cam.mod.gui_v2.core.layout.VerticalAlign;
import cam72cam.mod.item.Fuzzy;
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
        Label lab2 = Label.direct("label2");
        Button button3 = Button.of(150, 20, PlayerMessage.direct("clicker3 sel"))
                                             .callback((hand, btn) -> System.out.println(btn.hashCode()))
                                             .textured(new Identifier("textures/blocks/bedrock.png"));
        NumberField numberField = new NumberField(150, 20, PlayerMessage.direct("Val: slidValue"), 0, 1, 0, true, d -> System.out.println("Val: " + d));
        NumberField numberField1 = new NumberField(150, 20, PlayerMessage.direct("Val: slidValue"), 10, 100, 0, false, d -> System.out.println("Val: " + d));
        Slider horizontal = Slider.horizontal(150, 20, PlayerMessage.direct("slider"))
                                  .bound( 0, 1, 0)
                                  .setDisplayPrecision(0)
                                  .callback(slider -> System.out.println(slider.getValue()));
        Slider vertical = Slider.vertical(20, 150, PlayerMessage.direct("slider"))
                                .bound( 0, 1, 0)
                                .setDisplayPrecision(0)
                                .callback(slider -> {
                                    System.out.println(slider.getValue());
                                    numberField1.setWidth((int) (50 + slider.getValue() * 100));
                                });

        ItemPicker picker = new ItemPicker(140, 200, itemStack -> System.out.println(itemStack.getDisplayName()));
        picker.addItems(Fuzzy.BRICK_BLOCK.enumerate());
        picker.addItems(Fuzzy.WOOL_BLOCK.enumerate());
        picker.addItems(Fuzzy.LOG_WOOD.enumerate());
        root.addChildren(picker, HorizontalAlign.LEFT, 0, VerticalAlign.MIDDLE, 0);
        CheckBox checkBox = CheckBox.of(PlayerMessage.direct("cb"))
                                    .setChecked(true)
                                    .clicker(cb -> System.out.println(cb.isChecked()));
        TextField textField = TextField.of(150, 20)
                                       .callback(txt -> System.out.println("Text: " + txt));
        ScrollPane pane = ScrollPane.vertical(160, 200);
        ScrollPane hori = ScrollPane.horizontal(100, 230);
        Button button1 = Button.of(150, 20, PlayerMessage.direct("clicker"))
                               .callback((hand, btn) -> {
                                   if (pane.isBarVisible()) {
                                       pane.hideScrollBar();
                                   } else {
                                       pane.showScrollBar();
                                   }
                               });
        Button button2 = Button.of(150, 20, PlayerMessage.direct("clicker2"))
                               .callback((hand, b) -> picker.setVisible(!picker.isVisible()));
        vBox.addChildren(lab, button1, button2, lab2, button3, horizontal, checkBox, textField, numberField, numberField1, vertical);

        CyclableButton<HorizontalAlign> hAlign = CyclableButton.of(button3, HorizontalAlign.class, HorizontalAlign.LEFT)
                                                               .valueChanged(e -> {
                                                                     root.setChildHorizontalAnchor(hori, e, 0);
                                                                 });
        CyclableButton<VerticalAlign> vAlign = CyclableButton.of(button3, VerticalAlign.class, VerticalAlign.TOP)
                                                             .valueChanged(e -> {
                                                                   root.setChildVerticalAnchor(hori, e, 0);
                                                               });
        vBox.addChildren(hAlign, vAlign);
        pane.addChild(vBox);
        hori.addChild(pane);
        root.addChildren(hori, HorizontalAlign.RIGHT, 0, VerticalAlign.BOTTOM, 0);
    }
}

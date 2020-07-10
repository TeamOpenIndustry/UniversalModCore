package cam72cam.mod.automate;

import cam72cam.mod.util.CollectionUtil;

import java.awt.*;
import java.util.List;

public class WaitTicks extends Action {
    public static final String TYPE = "WaitTicks";
    private String ticks;
    private int counter = 0;

    protected WaitTicks(String... args) {
        super(TYPE);
        this.ticks = args[0];
    }

    @Override
    public List<String> getParams() {
        return CollectionUtil.listOf(ticks);
    }

    @Override
    public boolean tick() {
        counter++;
        if (counter >= Integer.parseInt(ticks)) {
            counter = 0;
            return true;
        }
        return false;
    }

    @Override
    public void renderEditor(Container panel) {
        Label l = new Label("Wait");
        l.setVisible(true);
        panel.add(l);

        TextField tn = new TextField(ticks);
        tn.addTextListener(a -> ticks = tn.getText());
        tn.setVisible(true);
        panel.add(tn);

        Label l2 = new Label("ticks");
        l2.setVisible(true);
        panel.add(l2);
    }

    @Override
    public void renderSummary(Container panel) {
        Label l = new Label(String.format("Wait %s ticks", ticks));
        l.setVisible(true);
        panel.add(l);
    }

    public static List<Action> getPotential() {
        return CollectionUtil.listOf(new WaitTicks("20"));
    }
}

package cam72cam.mod.automate;

import cam72cam.mod.util.CollectionUtil;

import javax.swing.*;
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
        if (counter > Integer.parseInt(ticks)) {
            counter = 0;
        }
        if (counter == Integer.parseInt(ticks)) {
            counter = 0;
            return true;
        }
        counter++;
        return false;
    }

    @Override
    public void renderEditor(JComponent panel) {
        JLabel l = new JLabel("Wait");
        panel.add(l);

        JTextField tn = new JTextField(ticks);
        tn.getDocument().addDocumentListener((TextListener)() -> ticks = tn.getText());
        panel.add(tn);

        JLabel l2 = new JLabel("ticks");
        panel.add(l2);
    }

    @Override
    public void renderSummary(JComponent panel) {
        JLabel l = new JLabel(String.format("Wait %s ticks", ticks));
        panel.add(l);
    }

    public static List<Action> getPotential() {
        return CollectionUtil.listOf(new WaitTicks("20"));
    }
}

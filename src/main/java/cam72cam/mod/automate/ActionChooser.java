package cam72cam.mod.automate;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ActionChooser extends JToolBar {
    private final List<Runnable> refreshes = new ArrayList<>();

    public ActionChooser() {
        super("Available Actions");
        setSize(200,200);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        GridBagConstraints c = new GridBagConstraints();

        JTabbedPane chooser = new JTabbedPane();

        for (String type : Action.getTypes()) {
            JPanel list = new JPanel(new GridBagLayout());

            Runnable refresh = () -> {
                list.removeAll();

                List<Action> potential = Action.getPotential(type);
                for (int i = 0; i < potential.size(); i++) {
                    Action action = potential.get(i);

                    FlowLayout l = new FlowLayout();
                    l.setAlignment(FlowLayout.LEFT);
                    JPanel sub = new JPanel(l);
                    action.renderSummary(sub);
                    sub.revalidate();

                    JButton append = new JButton("Add");
                    append.addActionListener(e -> Automation.INSTANCE.UI.playbook.appendAction(action));

                    JButton insert = new JButton("Insert");
                    insert.addActionListener(e -> Automation.INSTANCE.UI.playbook.insertAction(action));


                    c.fill = GridBagConstraints.HORIZONTAL;
                    c.gridx = 0;
                    c.gridy = i;
                    c.gridwidth = 1;
                    list.add(sub, c);

                    c.fill = GridBagConstraints.HORIZONTAL;
                    c.gridx = 1;
                    c.gridy = i;
                    c.gridwidth = 1;
                    list.add(append, c);

                    c.fill = GridBagConstraints.HORIZONTAL;
                    c.gridx = 2;
                    c.gridy = i;
                    c.gridwidth = 1;
                    list.add(insert, c);
                }

            };

            refreshes.add(refresh);

            chooser.addTab(type, list);
        }
        refresh();

        add(chooser);
    }

    public void refresh() {
        refreshes.forEach(Runnable::run);
        this.revalidate();
        this.repaint();
    }
}

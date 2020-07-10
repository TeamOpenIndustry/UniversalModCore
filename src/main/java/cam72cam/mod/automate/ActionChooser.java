package cam72cam.mod.automate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class ActionChooser extends Dialog {
    public ActionChooser(Frame frame, boolean insert) {
        super(frame, true);
        setSize(200,200);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setTitle(insert ? "Choose action to be inserted" : "Choose action to be added");

        Choice chooser = new Choice();
        Action.getTypes().forEach(chooser::add);
        chooser.setVisible(true);

        add(chooser);

        Panel list = new Panel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        Runnable refresh = () -> {
            list.removeAll();

            List<Action> potential = Action.getPotential(chooser.getSelectedItem());
            for (int i = 0; i < potential.size(); i++) {
                Action action = potential.get(i);

                FlowLayout l = new FlowLayout();
                l.setAlignment(FlowLayout.LEFT);
                Panel sub = new Panel(l);
                action.renderSummary(sub);
                sub.setVisible(true);
                sub.revalidate();

                Button btn = new Button("Add");
                btn.setVisible(true);
                btn.addActionListener(e -> {
                    if (insert) {
                        Automation.INSTANCE.UI.playbook.insertAction(action);
                    } else {
                        Automation.INSTANCE.UI.playbook.appendAction(action);
                    }
                    this.dispose();
                });

                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridx = 0;
                c.gridy = i;
                c.gridwidth = 1;
                list.add(sub, c);

                c.fill = GridBagConstraints.HORIZONTAL;
                c.gridx = 1;
                c.gridy = i;
                c.gridwidth = 1;
                list.add(btn, c);
            }
            list.revalidate();
            this.revalidate();
        };

        refresh.run();
        chooser.addItemListener(l -> refresh.run());

        list.setVisible(true);
        add(list);

        /*
        chooser.addItemListener(e -> {
            for (Action action : potential) {
                action.renderEditor(this);
            }
        });*/

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent){
                dispose();
            }
        });
    }

}

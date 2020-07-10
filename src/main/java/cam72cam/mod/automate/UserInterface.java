package cam72cam.mod.automate;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;

public class UserInterface extends Frame {
    private final MenuItem savePlaybook;
    private final MenuItem savePlaybookAs;
    private final Menu runMenu;
    private final Menu addMenu;

    public Playbook playbook;

    public UserInterface() {
        setSize(500,300);
        setTitle("Playbook Manager");
        MenuBar mb = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem newPlaybook = new MenuItem("New Playbook");
        MenuItem openPlaybook = new MenuItem("Open Playbook");
        savePlaybook = new MenuItem("Save Playbook");
        savePlaybookAs = new MenuItem("Save Playbook As");

        newPlaybook.addActionListener(e -> {
            File file = openFileDialog(FileDialog.SAVE);
            if (file != null) {
                setupPlaybook(file);
            }
        });

        openPlaybook.addActionListener(e -> {
            File file = openFileDialog(FileDialog.LOAD);
            if (file != null) {
                setupPlaybook(file);
            }
        });

        savePlaybook.addActionListener(e -> {
            try {
                playbook.save();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        savePlaybook.setEnabled(false);

        savePlaybookAs.addActionListener(e -> {
            File file = openFileDialog(FileDialog.SAVE);
            if (file != null) {
                try {
                    playbook.saveAs(file);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        savePlaybookAs.setEnabled(false);

        fileMenu.add(newPlaybook);
        fileMenu.add(openPlaybook);
        fileMenu.add(savePlaybook);
        fileMenu.add(savePlaybookAs);
        mb.add(fileMenu);

        runMenu = new Menu("Run");
        MenuItem run = new MenuItem("Run Playbook");
        MenuItem step = new MenuItem("Step Playbook");
        MenuItem pause = new MenuItem("Pause Playbook");
        MenuItem stop = new MenuItem("Stop Playbook");
        MenuItem restart = new MenuItem("Restart Playbook");

        run.addActionListener(e -> this.playbook.runAll());
        step.addActionListener(e -> this.playbook.runStep());
        pause.addActionListener(e -> this.playbook.pause());
        stop.addActionListener(e -> this.playbook.stop());
        restart.addActionListener(e -> this.playbook.startover());

        runMenu.add(run);
        runMenu.add(step);
        runMenu.add(pause);
        runMenu.add(stop);
        runMenu.add(restart);

        runMenu.setEnabled(false);
        mb.add(runMenu);

        addMenu = new Menu("Actions");

        MenuItem append = new MenuItem("Append Item to Playbook");
        MenuItem insert = new MenuItem("Insert After Current Item");
        MenuItem remove = new MenuItem("Remove Current Item");

        append.addActionListener(e -> new ActionChooser(this, false).setVisible(true));
        insert.addActionListener(e -> new ActionChooser(this, true).setVisible(true));
        remove.addActionListener(e -> this.playbook.removeCurrentAction());

        addMenu.add(append);
        addMenu.add(insert);
        addMenu.add(remove);

        addMenu.setEnabled(false);
        mb.add(addMenu);

        setMenuBar(mb);
        setLayout(new FlowLayout());
        setVisible(true);
    }

    private void setupPlaybook(File file) {
        try {
            playbook = new Playbook(file);
            savePlaybook.setEnabled(true);
            savePlaybookAs.setEnabled(true);
            runMenu.setEnabled(true);
            addMenu.setEnabled(true);

            ScrollPane pane = new ScrollPane();
            pane.add(playbook);
            pane.setSize(this.getSize());
            pane.setVisible(true);
            this.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent componentEvent) {
                    pane.setSize(componentEvent.getComponent().getSize().width, componentEvent.getComponent().getSize().height - 20);
                    playbook.revalidate();
                    super.componentResized(componentEvent);
                }
            });
            add(pane);

            this.revalidate();
            this.repaint();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private File openFileDialog(int mode) {
        FileDialog fd = new FileDialog((Frame) null);
        fd.setFile("*.mcplay");
        fd.setMode(mode);
        fd.setVisible(true);
        return fd.getFile() != null ? new File(fd.getDirectory(), fd.getFile()) : null;
    }

    public void tick() {
        if (playbook != null) {
            playbook.tick();
        }
    }
}

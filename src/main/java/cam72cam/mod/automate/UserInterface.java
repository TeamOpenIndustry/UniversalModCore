package cam72cam.mod.automate;

import net.minecraftforge.fml.common.Loader;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class UserInterface extends JFrame {
    private final JMenuItem savePlaybook;
    private final JMenuItem savePlaybookAs;
    private final JMenu addMenu;
    private final JButton run;
    private final JButton step;
    private final JButton pause;
    private final JButton stop;
    private final JButton restart;
    private final ActionChooser ac;
    private final FileNode fn;
    private final DefaultTreeModel tm;

    public Playbook playbook;
    private JToolBar tb;

    public UserInterface() {
        setSize(500,300);
        setTitle("Playbook Manager");
        setLayout(new BorderLayout());
        JMenuBar mb = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem newPlaybook = new JMenuItem("New Playbook");
        JMenuItem openPlaybook = new JMenuItem("Open Playbook");
        savePlaybook = new JMenuItem("Save Playbook");
        savePlaybookAs = new JMenuItem("Save Playbook As");

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

        run = new JButton("Run");
        step = new JButton("Step");
        pause = new JButton("Pause");
        stop = new JButton("Stop");
        restart = new JButton("Restart");

        run.addActionListener(e -> this.playbook.runAll());
        step.addActionListener(e -> this.playbook.runStep());
        pause.addActionListener(e -> this.playbook.pause());
        stop.addActionListener(e -> this.playbook.stop());
        restart.addActionListener(e -> this.playbook.startover());

        JToolBar actions = new JToolBar("Actions");
        actions.add(run);
        actions.add(step);
        actions.add(pause);
        actions.add(stop);
        actions.add(restart);

        run.setEnabled(false);
        step.setEnabled(false);
        pause.setEnabled(false);
        stop.setEnabled(false);
        restart.setEnabled(false);
        this.add(actions, BorderLayout.PAGE_START);

        addMenu = new JMenu("Actions");

        //MenuItem append = new JMenuItem("Append Item to Playbook");
        //MenuItem insert = new JMenuItem("Insert After Current Item");
        JMenuItem remove = new JMenuItem("Remove Current Item");

        remove.addActionListener(e -> this.playbook.removeCurrentAction());

        //addMenu.add(append);
        //addMenu.add(insert);
        addMenu.add(remove);

        addMenu.setEnabled(false);
        mb.add(addMenu);

        ac = new ActionChooser();
        add(ac, BorderLayout.LINE_END);

        fn = new FileNode(Loader.instance().getConfigDir().getParentFile());
        tm = new DefaultTreeModel(fn);
        JTree ft = new JTree(tm);
        ft.addTreeSelectionListener(e -> {
            File path = ((FileNode) e.getPath().getLastPathComponent()).path;
            if (path.isFile()) {
                setupPlaybook(path);
            }
        });
        JToolBar ttb = new JToolBar("Tree");
        ttb.add(new JScrollPane(ft));
        add(ttb, BorderLayout.LINE_START);

        setJMenuBar(mb);
        setVisible(true);
    }

    private void setupPlaybook(File file) {
        try {
            if (tb != null) {
                remove(tb);
            }
            playbook = new Playbook(file);
            setTitle("Playbook Manager " + playbook.getName());
            savePlaybook.setEnabled(true);
            savePlaybookAs.setEnabled(true);
            run.setEnabled(true);
            step.setEnabled(true);
            pause.setEnabled(true);
            stop.setEnabled(true);
            restart.setEnabled(true);
            addMenu.setEnabled(true);

            this.tb = new JToolBar("Playbook");
            tb.add(playbook);
            add(tb, BorderLayout.CENTER);

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

    int tick = 0;
    public void tick() {
        tick ++;
        if (tick % 20 == 0) {
            tick = 0;
            ac.refresh();
            fn.refresh();
            tm.reload();
        }

        if (playbook != null) {
            playbook.tick();
        }
    }

    public static class FileNode extends DefaultMutableTreeNode {
        private final File path;

        public FileNode(File path) {
            super(path.getName());
            this.path = path;
            this.refresh();
        }

        public void refresh() {
            this.removeAllChildren();
            if (path.isDirectory()) {
                for (File child : Objects.requireNonNull(path.listFiles())) {
                    if (child.isDirectory() || child.getName().endsWith("mcplay")) {
                        FileNode cn = new FileNode(child);
                        if (child.isDirectory() && !cn.children().hasMoreElements()) {
                            continue;
                        }
                        this.add(cn);
                    }
                }
            }
        }
    }
}

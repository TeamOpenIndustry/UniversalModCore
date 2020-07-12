package cam72cam.mod.automate;

import net.minecraftforge.fml.common.Loader;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private final JTabbedPane playbooks;

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
                getSelectedPlaybook().save();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        savePlaybook.setEnabled(false);

        savePlaybookAs.addActionListener(e -> {
            File file = openFileDialog(FileDialog.SAVE);
            if (file != null) {
                try {
                    getSelectedPlaybook().saveAs(file);
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

        run.addActionListener(e -> this.getSelectedPlaybook().runAll());
        step.addActionListener(e -> this.getSelectedPlaybook().runStep());
        pause.addActionListener(e -> this.getSelectedPlaybook().pause());
        stop.addActionListener(e -> this.getSelectedPlaybook().stop());
        restart.addActionListener(e -> this.getSelectedPlaybook().startover());

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

        remove.addActionListener(e -> this.getSelectedPlaybook().removeCurrentAction());

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

        JToolBar pbtb = new JToolBar("Playbooks");
        playbooks = new JTabbedPane();
        playbooks.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (SwingUtilities.isRightMouseButton(mouseEvent)) {
                    int tabId = playbooks.getUI().tabForCoordinate(playbooks, mouseEvent.getX(), mouseEvent.getY());
                    if (tabId >= 0) {
                        JPopupMenu ctx = new JPopupMenu();
                        JMenuItem save = new JMenuItem("Save Playbook");
                        save.addActionListener(e -> {
                            Playbook playbook = (Playbook) playbooks.getComponent(tabId);
                            try {
                                playbook.save();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                                JOptionPane.showMessageDialog(UserInterface.this, ex.toString());
                            }
                        });
                        ctx.add(save);

                        JMenuItem saveAs = new JMenuItem("Save Playbook As");
                        saveAs.addActionListener(e -> {
                            Playbook playbook = (Playbook) playbooks.getComponent(tabId);
                            try {
                                File file = openFileDialog(FileDialog.SAVE);
                                if (file != null) {
                                    playbook.saveAs(file);
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                                JOptionPane.showMessageDialog(UserInterface.this, ex.toString());
                            }
                        });
                        ctx.add(saveAs);

                        JMenuItem close = new JMenuItem("Close Playbook");
                        close.addActionListener(e -> playbooks.remove(tabId));
                        ctx.add(close);

                        UserInterface.this.add(ctx);
                        ctx.show(playbooks, mouseEvent.getX(), mouseEvent.getY());
                    }
                }
            }
        });

        pbtb.add(playbooks);
        add(pbtb);


        setJMenuBar(mb);
        setVisible(true);
    }

    private void setupPlaybook(File file) {
        for (Component component : playbooks.getComponents()) {
            Playbook playbook = (Playbook) component;
            if (playbook.file.equals(file)) {
                return;
            }
        }
        try {
            Playbook selectedPlaybook = new Playbook(file);
            playbooks.add(selectedPlaybook);

            setTitle("Playbook Manager " + selectedPlaybook.getName());
            savePlaybook.setEnabled(true);
            savePlaybookAs.setEnabled(true);
            run.setEnabled(true);
            step.setEnabled(true);
            pause.setEnabled(true);
            stop.setEnabled(true);
            restart.setEnabled(true);
            addMenu.setEnabled(true);

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

        if (getSelectedPlaybook() != null) {
            getSelectedPlaybook().tick();
        }
    }

    public Playbook getSelectedPlaybook() {
        return (Playbook) playbooks.getSelectedComponent();
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

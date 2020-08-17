package cam72cam.mod.automate;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

public class Project extends JFrame {
    private final JMenu addMenu;
    private final JButton run;
    private final JButton step;
    private final JButton pause;
    private final JButton stop;
    private final JButton restart;
    private final ActionChooser ac;
    private final FileNode fn;
    private final JTabbedPane playbooks;
    private final JTree ft;
    final File dir;

    public Project(File dir) {
        this.dir = dir;

        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int width = gd.getDisplayMode().getWidth();
        int height = gd.getDisplayMode().getHeight();

        setSize(width/2, height);
        setTitle("Playbook Manager");
        setLayout(new BorderLayout());
        JMenuBar mb = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem openProject = new JMenuItem("Open Project");
        JMenuItem closeProject = new JMenuItem("Close Project");

        openProject.addActionListener(e -> {
            JFileChooser fd = new JFileChooser(dir);
            fd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fd.showDialog(Project.this, "Choose Project Directory") == JFileChooser.APPROVE_OPTION) {
                if (!fd.getSelectedFile().exists()) {
                    fd.getSelectedFile().mkdirs();
                }
                Automation.INSTANCE.openProject(fd.getSelectedFile());
            }
        });

        fileMenu.add(openProject);
        fileMenu.add(closeProject);
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

        DefaultTreeModel tm = new DefaultTreeModel(null);
        fn = new FileNode(tm, dir);
        tm.setRoot(fn);
        ft = new JTree(tm);
        ft.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                TreePath fpath = ft.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
                if (fpath != null) {
                    FileNode node = (FileNode) fpath.getLastPathComponent();
                    File path = node.path;
                    if (SwingUtilities.isRightMouseButton(mouseEvent)) {
                        if (path.isDirectory()) {
                            JPopupMenu ctx = new JPopupMenu();

                            JMenuItem newPlaybook = new JMenuItem("New Playbook");
                            newPlaybook.addActionListener(e -> {
                                String fname = JOptionPane.showInputDialog(Project.this, "New Playbook");
                                if (fname != null && !fname.isEmpty()) {
                                    if (!fname.endsWith(".mcplay")) {
                                        fname += ".mcplay";
                                    }
                                    File file = new File(path, fname);
                                    try {
                                        setupPlaybook(file).save();
                                    } catch (IOException ioException) {
                                        ioException.printStackTrace();
                                    }
                                }
                            });
                            ctx.add(newPlaybook);

                            JMenuItem newDirectory = new JMenuItem("New Directory");
                            newDirectory.addActionListener(e -> {
                                String dirname = JOptionPane.showInputDialog(Project.this, "New Directory");
                                if (dirname != null && !dirname.isEmpty()) {
                                    File newDir = new File(path, dirname);
                                    if (!newDir.exists()) {
                                        newDir.mkdirs();
                                    }
                                }
                            });
                            ctx.add(newDirectory);

                            if (!node.isRoot()) {
                                JMenuItem rmDirectory = new JMenuItem("Remove Directory");
                                rmDirectory.addActionListener(e -> {
                                    if (JOptionPane.showConfirmDialog(Project.this, "Are you sure you want to delete " + path + "?", "Remove Directory", JOptionPane.YES_NO_OPTION,
                                            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                                        path.delete();
                                    }
                                });
                                ctx.add(rmDirectory);
                            }

                            Project.this.add(ctx);
                            ctx.show(ft, mouseEvent.getX(), mouseEvent.getY());
                        }
                    }

                    if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
                        if (path.isFile()) {
                            setupPlaybook(path);
                        }
                    }
                }
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
                                JOptionPane.showMessageDialog(Project.this, ex.toString());
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
                                JOptionPane.showMessageDialog(Project.this, ex.toString());
                            }
                        });
                        ctx.add(saveAs);

                        JMenuItem close = new JMenuItem("Close Playbook");
                        close.addActionListener(e -> {
                            Playbook playbook = (Playbook) playbooks.getComponent(tabId);
                            if (playbook.isModified()) {
                                if (JOptionPane.showConfirmDialog(Project.this, "Are you sure you want to close " + playbook.file.getName() + "?", "Unsaved Playbook", JOptionPane.YES_NO_OPTION,
                                        JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
                                    return;
                                }
                            }
                            playbooks.remove(tabId);
                        });
                        ctx.add(close);

                        Project.this.add(ctx);
                        ctx.show(playbooks, mouseEvent.getX(), mouseEvent.getY());
                    }
                }
            }
        });

        pbtb.add(playbooks);
        add(pbtb);

        ac = new ActionChooser(this);
        add(ac, BorderLayout.LINE_END);

        //
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (Arrays.stream(playbooks.getComponents()).map(c -> (Playbook)c).anyMatch(Playbook::isModified)) {
                    if (JOptionPane.showConfirmDialog(Project.this, "Are you sure you want to close this project?  Not all playbooks have been saved", "Close Project?",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) {
                        return;
                    }
                }
                Automation.INSTANCE.closeProject(Project.this);
                Project.this.dispose();
            }
        });


        setJMenuBar(mb);
        setVisible(true);

        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }

    private Playbook setupPlaybook(File file) {
        for (Component component : playbooks.getComponents()) {
            Playbook playbook = (Playbook) component;
            if (playbook.file.equals(file)) {
                playbooks.setSelectedComponent(playbook);
                return playbook;
            }
        }
        try {
            Playbook selectedPlaybook = new Playbook(file);
            playbooks.add(selectedPlaybook);
            playbooks.setSelectedComponent(selectedPlaybook);

            setTitle("Playbook Manager " + selectedPlaybook.getName());
            run.setEnabled(true);
            step.setEnabled(true);
            pause.setEnabled(true);
            stop.setEnabled(true);
            restart.setEnabled(true);
            addMenu.setEnabled(true);

            this.revalidate();
            this.repaint();
            return selectedPlaybook;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return getSelectedPlaybook();
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
        }

        Component[] components = playbooks.getComponents();
        for (int i = 0; i < components.length; i++) {
            Playbook playbook = (Playbook) components[i];
            playbooks.setTitleAt(i, playbook.file.getName() + (playbook.isModified() ? " *" : ""));
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
        private final DefaultTreeModel tm;

        public FileNode(DefaultTreeModel tm, File path) {
            super(path.getName());
            this.tm = tm;
            this.path = path;
            this.refresh();
        }

        public void refresh() {
            Map<File, FileNode> children = new HashMap<>();

            for (int i = 0; i < this.getChildCount(); i++) {
                FileNode fn = ((FileNode)this.getChildAt(i));
                children.put(fn.path, fn);
            }

            if (path.isDirectory()) {
                File[] files = path.listFiles().clone();
                Arrays.sort(files);
                for (File child : files) {
                    if (child.isDirectory() || child.getName().endsWith("mcplay")) {
                        if (children.containsKey(child)) {
                            children.get(child).refresh();
                            children.remove(child);
                        } else {
                            FileNode nn = new FileNode(tm, child);
                            this.add(nn);
                            tm.reload(this);
                        }
                    }
                }
            }
            for (int i = 0; i < this.getChildCount(); i++) {
                if (children.containsValue((FileNode)this.getChildAt(i))) {
                    this.remove(i);
                    tm.reload(this);
                }
            }
        }
    }
}

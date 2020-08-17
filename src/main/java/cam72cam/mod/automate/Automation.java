package cam72cam.mod.automate;

import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Automation {
    static {
        Action.register(GuiClickButton.TYPE, GuiClickButton::new, GuiClickButton::getPotential);
        Action.register(GuiSetText.TYPE, GuiSetText::new, GuiSetText::getPotential);
        Action.register(KeyPress.TYPE, KeyPress::new, KeyPress::getPotential);
        Action.register(PlayerLook.TYPE, PlayerLook::new, PlayerLook::getPotential);
        Action.register(GuiSelectWorld.TYPE, GuiSelectWorld::new, GuiSelectWorld::getPotential);
        Action.register(WaitTicks.TYPE, WaitTicks::new, WaitTicks::getPotential);
        Action.register(GuiSelectCreativeTab.TYPE, GuiSelectCreativeTab::new, GuiSelectCreativeTab::getPotential);
        Action.register(GuiClickSlot.TYPE, GuiClickSlot::new, GuiClickSlot::getPotential);
        Action.register(ClickButton.TYPE, ClickButton::new, ClickButton::getPotential);
    }

    public static final Automation INSTANCE = new Automation();
    private final Map<File, Project> projects = new HashMap<>();

    private final File mcDir = Loader.instance().getConfigDir().getParentFile();
    private final File cfg = new File(mcDir, "automc.cfg");

    private Automation() {
        if (cfg.exists()) {
            try {
                for (String line : Files.readAllLines(cfg.toPath())) {
                    openProject(new File(line));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (projects.isEmpty()) {
            openProject(new File(mcDir, "playbooks"));
        }
    }

    public void tick() {
        // CME
        new ArrayList<>(projects.values()).forEach(Project::tick);
    }

    public void openProject(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                SwingHelper.alert("Unable to create directory: " + dir);
                return;
            }
        }
        if (!dir.isDirectory()) {
            SwingHelper.alert("Not a directory: " + dir);
            return;
        }
        if (projects.containsKey(dir)) {
            projects.get(dir).toFront();
            projects.get(dir).repaint();
            return;
        }

        projects.put(dir, new Project(dir));

        try {
            Files.write(cfg.toPath(), projects.keySet().stream().map(File::toString).collect(Collectors.joining(System.lineSeparator())).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            SwingHelper.alert(e.toString());
        }
    }

    public void closeProject(Project project) {
        projects.remove(project.dir);

        try {
            Files.write(cfg.toPath(), projects.keySet().stream().map(File::toString).collect(Collectors.joining(System.lineSeparator())).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            SwingHelper.alert(e.toString());
        }
    }
}

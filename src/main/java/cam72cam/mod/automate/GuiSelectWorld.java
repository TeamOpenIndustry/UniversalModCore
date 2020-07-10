package cam72cam.mod.automate;

import cam72cam.mod.util.CollectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiListWorldSelection;
import net.minecraft.client.gui.GuiListWorldSelectionEntry;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiWorldSelection;
import net.minecraft.world.storage.WorldSummary;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class GuiSelectWorld extends Action {
    public static final String TYPE = "GuiClickWorld";
    private String worldName;

    GuiSelectWorld(String... params) {
        super(TYPE);
        this.worldName = params[0];
    }

    @Override
    public List<String> getParams() {
        return CollectionUtil.listOf(worldName);
    }

    @Override
    public boolean tick() {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen instanceof GuiWorldSelection) {
            GuiWorldSelection selector = (GuiWorldSelection) screen;
            try {
                Field sl = GuiWorldSelection.class.getDeclaredField("selectionList");
                sl.setAccessible(true);
                GuiListWorldSelection selectionList = (GuiListWorldSelection) sl.get(selector);
                for(int i = 0; ; i++) {
                    GuiListWorldSelectionEntry world = selectionList.getListEntry(i);
                    Field ws = world.getClass().getDeclaredField("worldSummary");
                    ws.setAccessible(true);
                    WorldSummary worldSummary = (WorldSummary) ws.get(world);
                    if (worldSummary.getDisplayName().equals(worldName)) {
                        selectionList.selectWorld(i);
                        return true;
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            } catch (IndexOutOfBoundsException e) {
                // NOP END ITERATION;
            }
        }
        return false;
    }

    @Override
    public void renderEditor(Container panel) {
        Label l = new Label("Select World");
        l.setVisible(true);
        panel.add(l);

        TextField tn = new TextField(worldName);
        tn.addActionListener(a -> worldName = tn.getText());
        tn.setVisible(true);
        panel.add(tn);
    }

    @Override
    public void renderSummary(Container panel) {
        Label l = new Label("Select World '" + worldName + "'");
        l.setVisible(true);
        panel.add(l);
    }

    public static List<Action> getPotential() {
        List<Action> actions = new ArrayList<>();

        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen instanceof GuiWorldSelection) {
            GuiWorldSelection selector = (GuiWorldSelection) screen;
            try {
                Field sl = GuiWorldSelection.class.getDeclaredField("selectionList");
                sl.setAccessible(true);
                GuiListWorldSelection selectionList = (GuiListWorldSelection) sl.get(selector);
                for(int i = 0; ; i++) {
                    GuiListWorldSelectionEntry world = selectionList.getListEntry(i);
                    Field ws = world.getClass().getDeclaredField("worldSummary");
                    ws.setAccessible(true);
                    WorldSummary worldSummary = (WorldSummary) ws.get(world);
                    actions.add(new GuiSelectWorld(worldSummary.getDisplayName()));
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            } catch (IndexOutOfBoundsException e) {
                // NOP END ITERATION;
            }
        }
        return actions;
    }
}

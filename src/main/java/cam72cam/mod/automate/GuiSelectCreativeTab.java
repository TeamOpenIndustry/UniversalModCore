package cam72cam.mod.automate;

import cam72cam.mod.util.CollectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.creativetab.CreativeTabs;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class GuiSelectCreativeTab extends Action {
    public static final String TYPE = "GuiSelectCreativeTab";
    private String tabName;

    GuiSelectCreativeTab(String... params) {
        super(TYPE);
        this.tabName = params[0];
    }

    @Override
    public List<String> getParams() {
        return CollectionUtil.listOf(tabName);
    }

    @Override
    public boolean tick() {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen instanceof GuiContainerCreative) {
            GuiContainerCreative selector = (GuiContainerCreative) screen;
            try {
                for (CreativeTabs creativeTab : CreativeTabs.CREATIVE_TAB_ARRAY) {
                    if (creativeTab.getTranslationKey().contains(tabName)) {
                        Method setCurrentCreativeTab = GuiContainerCreative.class.getDeclaredMethod("setCurrentCreativeTab", CreativeTabs.class);
                        setCurrentCreativeTab.setAccessible(true);
                        setCurrentCreativeTab.invoke(selector, creativeTab);
                        return true;
                    }
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public void renderEditor(JComponent panel) {
        JLabel l = new JLabel("Select Creative Tab");
        l.setVisible(true);
        panel.add(l);

        JComboBox<String> tn = new JComboBox<>();
        for (CreativeTabs creativeTab : CreativeTabs.CREATIVE_TAB_ARRAY) {
            tn.addItem(creativeTab.getTranslationKey().replace("itemGroup.", ""));
        }
        tn.setSelectedItem(tabName);
        tn.addItemListener(a -> tabName = (String) tn.getSelectedItem());
        tn.setVisible(true);
        panel.add(tn);
    }

    @Override
    public void renderSummary(JComponent panel) {
        JLabel l = new JLabel("Select Creative Tab '" + tabName + "'");
        l.setVisible(true);
        panel.add(l);
    }

    public static List<Action> getPotential() {
        List<Action> actions = new ArrayList<>();
        for (CreativeTabs creativeTab : CreativeTabs.CREATIVE_TAB_ARRAY) {
            actions.add(new GuiSelectCreativeTab(creativeTab.getTranslationKey().replace("itemGroup.", "")));
        }
        return actions;
    }
}

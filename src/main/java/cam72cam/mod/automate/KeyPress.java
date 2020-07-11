package cam72cam.mod.automate;

import cam72cam.mod.input.Keyboard;
import cam72cam.mod.util.CollectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class KeyPress extends Action {
    public static final String TYPE = "KeyPress";
    private Keyboard.KeyCode key;
    private int ticks;
    private int counter;

    public KeyPress(String... options) {
        super(TYPE);
        this.key = Keyboard.KeyCode.valueOf(options[0]);
        this.ticks = Integer.parseInt(options[1]);
    }

    @Override
    public List<String> getParams() {
        return CollectionUtil.listOf(key.toString(), "" + ticks);
    }

    @Override
    public boolean tick() {
        KeyBinding.setKeyBindState(key.code, counter != ticks);
        KeyBinding.onTick(key.code);
        if (counter >= ticks) {
            // Hack...
            if (Minecraft.getMinecraft().currentScreen == null && key == Keyboard.KeyCode.ESCAPE) {
                Minecraft.getMinecraft().displayInGameMenu();
            } else if (Minecraft.getMinecraft().currentScreen != null) {
                if (key == Keyboard.KeyCode.ESCAPE) {
                    Minecraft.getMinecraft().displayGuiScreen((GuiScreen) null);
                    if (Minecraft.getMinecraft().currentScreen == null) {
                        Minecraft.getMinecraft().setIngameFocus();
                    }
                } else {
                    try {
                        Method keyTyped = GuiScreen.class.getDeclaredMethod("keyTyped", char.class, int.class);
                        keyTyped.setAccessible(true);
                        // TODO proper char
                        keyTyped.invoke(Minecraft.getMinecraft().currentScreen, this.key.toString().charAt(0), key.code);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
            counter = 0;
            return true;
        }
        counter++;
        return false;
    }

    @Override
    public void renderEditor(JComponent panel) {
        JComboBox<Keyboard.KeyCode> l = new JComboBox<>(Keyboard.KeyCode.values().clone());
        l.setSelectedItem(key);
        l.addItemListener(e -> key = (Keyboard.KeyCode) l.getSelectedItem());
        panel.add(l);

        JTextField tn = new JTextField(ticks + "");
        tn.getDocument().addDocumentListener((TextListener)() -> ticks = Integer.parseInt(tn.getText()));
        panel.add(tn);
    }

    @Override
    public void renderSummary(JComponent panel) {
        JLabel l = new JLabel(String.format("Press '%s' for '%s' ticks", key, ticks));
        panel.add(l);
    }

    public static List<Action> getPotential() {
        return CollectionUtil.listOf(new KeyPress("ESCAPE", "1"));
    }
}

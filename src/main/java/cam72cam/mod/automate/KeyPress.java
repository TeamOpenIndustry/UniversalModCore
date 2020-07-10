package cam72cam.mod.automate;

import cam72cam.mod.input.Keyboard;
import cam72cam.mod.util.CollectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class KeyPress extends Action {
    public static final String TYPE = "KeyPress";
    private String key;
    private int ticks;
    private int counter;

    public KeyPress(String... options) {
        super(TYPE);
        this.key = options[0];
        this.ticks = Integer.parseInt(options[1]);
    }

    @Override
    public List<String> getParams() {
        return CollectionUtil.listOf(key, "" + ticks);
    }

    @Override
    public boolean tick() {
        Keyboard.KeyCode enKey = Keyboard.KeyCode.valueOf(key);
        KeyBinding.setKeyBindState(enKey.code, counter != ticks);
        KeyBinding.onTick(enKey.code);
        if (counter >= ticks) {
            // Hack...
            if (Minecraft.getMinecraft().currentScreen == null && enKey == Keyboard.KeyCode.ESCAPE) {
                Minecraft.getMinecraft().displayInGameMenu();
            } else if (Minecraft.getMinecraft().currentScreen != null) {
                if (enKey == Keyboard.KeyCode.ESCAPE) {
                    Minecraft.getMinecraft().displayGuiScreen((GuiScreen) null);
                    if (Minecraft.getMinecraft().currentScreen == null) {
                        Minecraft.getMinecraft().setIngameFocus();
                    }
                } else {
                    try {
                        Method keyTyped = GuiScreen.class.getDeclaredMethod("keyTyped", char.class, int.class);
                        keyTyped.setAccessible(true);
                        // TODO proper char
                        keyTyped.invoke(Minecraft.getMinecraft().currentScreen, key.charAt(0), enKey.code);
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
    public void renderEditor(Container panel) {
        Choice l = new Choice();
        for (Keyboard.KeyCode value : Keyboard.KeyCode.values()) {
            l.add(value.toString());
        }
        l.select(key);
        l.addItemListener(e -> key = l.getSelectedItem());
        l.setVisible(true);
        panel.add(l);

        TextField tn = new TextField(ticks + "");
        tn.addTextListener(a -> ticks = Integer.parseInt(tn.getText()));
        tn.setVisible(true);
        panel.add(tn);
    }

    @Override
    public void renderSummary(Container panel) {
        Label l = new Label(String.format("Press '%s' for '%s' ticks", key, ticks));
        l.setVisible(true);
        panel.add(l);
    }

    public static List<Action> getPotential() {
        return CollectionUtil.listOf(new KeyPress("ESCAPE", "1"));
    }
}

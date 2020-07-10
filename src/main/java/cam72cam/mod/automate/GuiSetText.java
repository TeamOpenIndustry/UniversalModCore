package cam72cam.mod.automate;

import cam72cam.mod.util.CollectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class GuiSetText extends Action {
    public static final String TYPE = "GuiSetText";
    private String partialName;
    private String value;

    GuiSetText(String... params) {
        super(TYPE);
        this.partialName = params[0];
        this.value = params[1];
    }

    @Override
    public List<String> getParams() {
        return CollectionUtil.listOf(partialName, value);
    }

    @Override
    public boolean tick() {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen != null) {
            try {
                for (Field field : Minecraft.getMinecraft().currentScreen.getClass().getDeclaredFields()) {
                    if (field.getName().toLowerCase().contains(partialName.toLowerCase()) && GuiTextField.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        GuiTextField textField = (GuiTextField) field.get(screen);
                        //textField.setText(value);
                        textField.setText("");
                        textField.setFocused(true);
                        for (char c : value.toCharArray()) {
                            //textField.textboxKeyTyped(c, -1);
                            Method keyTyped = GuiScreen.class.getDeclaredMethod("keyTyped", char.class, int.class);
                            keyTyped.setAccessible(true);
                            keyTyped.invoke(screen, c, -1);
                        }
                        return true;
                    }
                }
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static List<Action> getPotential() {
        List<Action> actions = new ArrayList<>();
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen != null) {
            for (Field field : Minecraft.getMinecraft().currentScreen.getClass().getDeclaredFields()) {
                if (GuiTextField.class.isAssignableFrom(field.getType())) {
                    actions.add(new GuiSetText(field.getName(), "Some Text"));
                }
            }
        }
        return actions;
    }

    @Override
    public void renderEditor(Container panel) {
        Label l = new Label("Set text of ");
        l.setVisible(true);
        panel.add(l);

        TextField tn = new TextField(partialName);
        tn.addTextListener(a -> {
            partialName = tn.getText();
        });
        tn.setVisible(true);
        panel.add(tn);

        Label l2 = new Label(" to ");
        l2.setVisible(true);
        panel.add(l2);

        TextField tv = new TextField(value);
        tv.addTextListener(e -> {
            value = tv.getText();
        });
        panel.add(tv);
    }

    @Override
    public void renderSummary(Container panel) {
        Label l = new Label(String.format("Set text of %%%s%% to '%s'", partialName, value));
        l.setVisible(true);
        panel.add(l);
    }
}

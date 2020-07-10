package cam72cam.mod.automate;

import cam72cam.mod.util.CollectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import java.awt.*;

public class GuiClickButton extends Action {
    public static final String TYPE = "GuiClickButton";
    private String buttonText;

    GuiClickButton(String... params) {
        super(TYPE);
        this.buttonText = params[0];
    }

    @Override
    public List<String> getParams() {
        return CollectionUtil.listOf(buttonText);
    }

    @Override
    public boolean tick() {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen != null) {
            try {
                Field bl = GuiScreen.class.getDeclaredField("buttonList");
                bl.setAccessible(true);
                Field eb = GuiScreen.class.getDeclaredField("eventButton");
                eb.setAccessible(true);
                Method mouseClicked = GuiScreen.class.getDeclaredMethod("mouseClicked", int.class, int.class, int.class);
                mouseClicked.setAccessible(true);

                List<net.minecraft.client.gui.GuiButton> buttonList = (List<net.minecraft.client.gui.GuiButton>) bl.get(screen);
                if (buttonList != null) {
                    for (net.minecraft.client.gui.GuiButton guiButton : buttonList) {
                        if (guiButton.displayString.equals(this.buttonText)) {
                            eb.set(screen, 0);
                            mouseClicked.invoke(screen, guiButton.x, guiButton.y, 0);
                            return true;
                        }
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public void renderEditor(Container panel) {
        Label l = new Label("Click");
        l.setVisible(true);
        panel.add(l);

        TextField tn = new TextField(buttonText);
        tn.addTextListener(a -> buttonText = tn.getText());
        tn.setVisible(true);
        panel.add(tn);
    }

    @Override
    public void renderSummary(Container panel) {
        Label l = new Label("Click '" + buttonText + "'");
        l.setVisible(true);
        panel.add(l);
    }

    public static List<Action> getPotential() {
        List<Action> actions = new ArrayList<>();
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen != null) {
            try {
                Field bl = GuiScreen.class.getDeclaredField("buttonList");
                bl.setAccessible(true);

                List<net.minecraft.client.gui.GuiButton> buttonList = (List<net.minecraft.client.gui.GuiButton>) bl.get(screen);
                if (buttonList != null) {
                    for (net.minecraft.client.gui.GuiButton guiButton : buttonList) {
                        System.out.println(guiButton.id + " : " + guiButton.displayString);
                        actions.add(new GuiClickButton(guiButton.displayString));
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return actions;
    }
}

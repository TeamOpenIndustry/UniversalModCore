package cam72cam.mod.automate;

import cam72cam.mod.util.CollectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import java.awt.*;
import java.util.List;

public class ClickButton extends Action {
    public static final String TYPE = "ClickButton";
    private GuiClickSlot.ClickType clickType;
    private PressType pressType;

    enum PressType {
        PressAndRelease,
        Press,
        Release
    }

    protected ClickButton(String... params) {
        super(TYPE);
        pressType = PressType.valueOf(params[0]);
        clickType = GuiClickSlot.ClickType.valueOf(params[1]);
    }

    @Override
    public List<String> getParams() {
        return CollectionUtil.listOf(pressType.name(), clickType.name());
    }

    @Override
    public boolean tick() {
        KeyBinding key = null;
        switch (clickType) {
            case Left:
                key = Minecraft.getMinecraft().gameSettings.keyBindAttack;
                break;
            case Right:
                key = Minecraft.getMinecraft().gameSettings.keyBindUseItem;
                break;
            case Middle:
                key = Minecraft.getMinecraft().gameSettings.keyBindPickBlock;
                break;
        }
        switch (pressType) {
            case PressAndRelease:
                if (key.isPressed()) {
                    KeyBinding.setKeyBindState(key.getKeyCode(), false);
                    return false;
                } else {
                    KeyBinding.setKeyBindState(key.getKeyCode(), true);
                    return true;
                }
            case Press:
                KeyBinding.setKeyBindState(key.getKeyCode(), true);
                return true;
            case Release:
                KeyBinding.setKeyBindState(key.getKeyCode(), false);
                return true;
        }
        return false;
    }

    @Override
    public void renderEditor(Container panel) {
        Choice pt = new Choice();
        for (PressType value : PressType.values()) {
            pt.add(value.name());
        }
        pt.select(pressType.name());
        pt.addItemListener(a -> pressType = PressType.valueOf(pt.getSelectedItem()));
        pt.setVisible(true);
        panel.add(pt);

        Label l = new Label("Mouse");
        l.setVisible(true);
        panel.add(l);

        Choice ct = new Choice();
        for (GuiClickSlot.ClickType value : GuiClickSlot.ClickType.values()) {
            ct.add(value.name());
        }
        ct.select(clickType.name());
        ct.addItemListener(a -> clickType = GuiClickSlot.ClickType.valueOf(ct.getSelectedItem()));
        ct.setVisible(true);
        panel.add(ct);

    }

    @Override
    public void renderSummary(Container panel) {
        Label l = new Label(pressType.toString() + " Mouse " + clickType.toString());
        l.setVisible(true);
        panel.add(l);
    }

    public static List<Action> getPotential() {
        return CollectionUtil.listOf(new ClickButton("PressAndRelease", "Left"));
    }
}

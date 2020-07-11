package cam72cam.mod.automate;

import cam72cam.mod.util.CollectionUtil;
import net.minecraft.client.Minecraft;

import javax.swing.*;
import java.util.List;

public class PlayerLook extends Action {
    public static String TYPE = "PlayerLook";
    private String yaw;
    private String pitch;

    public PlayerLook(String... params) {
        super(TYPE);
        this.yaw = params[0];
        this.pitch = params[1];
    }

    @Override
    public List<String> getParams() {
        return CollectionUtil.listOf(yaw, pitch);
    }

    @Override
    public boolean tick() {
        if (Minecraft.getMinecraft().player != null) {
            Minecraft.getMinecraft().player.rotationYaw = Float.parseFloat(yaw);
            Minecraft.getMinecraft().player.rotationPitch = Float.parseFloat(pitch);
            return true;
        }
        return false;
    }

    @Override
    public void renderEditor(JComponent panel) {
        JLabel l = new JLabel("Player look yaw:");
        panel.add(l);

        JTextField tn = new JTextField(yaw);
        tn.getDocument().addDocumentListener((TextListener)() -> yaw = tn.getText());
        panel.add(tn);

        JLabel l2 = new JLabel("pitch:");
        panel.add(l2);

        JTextField tv = new JTextField(pitch);
        tn.getDocument().addDocumentListener((TextListener)() -> pitch = tv.getText());
        panel.add(tv);
    }

    @Override
    public void renderSummary(JComponent panel) {
        JLabel l = new JLabel(String.format("Player look yaw:%s pitch:%s", yaw, pitch));
        panel.add(l);
    }

    public static List<Action> getPotential() {
        return CollectionUtil.listOf(new PlayerLook("0", "0"));
    }
}

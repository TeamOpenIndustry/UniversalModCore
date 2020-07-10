package cam72cam.mod.automate;

import cam72cam.mod.util.CollectionUtil;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.lang.reflect.Field;
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
    public void renderEditor(Container panel) {
        Label l = new Label("Player look yaw:");
        l.setVisible(true);
        panel.add(l);

        TextField tn = new TextField(yaw);
        tn.addTextListener(a -> yaw = tn.getText());
        tn.setVisible(true);
        panel.add(tn);

        Label l2 = new Label("pitch:");
        l2.setVisible(true);
        panel.add(l2);

        TextField tv = new TextField(pitch);
        tv.addTextListener(e -> pitch = tv.getText());
        panel.add(tv);
    }

    @Override
    public void renderSummary(Container panel) {
        Label l = new Label(String.format("Player look yaw:%s pitch:%s", yaw, pitch));
        l.setVisible(true);
        panel.add(l);
    }

    public static List<Action> getPotential() {
        return CollectionUtil.listOf(new PlayerLook("0", "0"));
    }
}

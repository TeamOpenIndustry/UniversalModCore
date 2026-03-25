package cam72cam.mod.render;

import cam72cam.mod.MinecraftClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.EntityViewRenderEvent;

import java.util.ArrayList;
import java.util.List;

public class CameraUtils {
    private static final List<Controller> controllers = new ArrayList<>();

    public static Perspectives getPerspective() {
        if (!MinecraftClient.isReady()) {
            switch (Minecraft.getMinecraft().gameSettings.thirdPersonView) {
                case 0:
                    return Perspectives.FIRST_PERSON;
                case 1:
                    return Perspectives.THIRD_PERSON;
                case 2:
                    return Perspectives.THIRD_PERSON_INVERTED;
            }
        }

        return Perspectives.FIRST_PERSON;
    }

    public enum Perspectives {
        FIRST_PERSON,
        THIRD_PERSON,
        THIRD_PERSON_INVERTED,
    }

    public static Controller getController() {
        return new Controller();
    }

    public static float getFov() {
        return Minecraft.getMinecraft().gameSettings.fovSetting;
    }

    public static void applyTranslation(EntityViewRenderEvent.CameraSetup event) {
        if (getPerspective() == Perspectives.FIRST_PERSON) {
            return;
        }

        float x = 0, y = 0, z = 0;
        float yaw = 0, pitch = 0, roll = 0;

        float partialTicks = (float) event.getRenderPartialTicks();

        for (Controller controller : controllers) {
            x += controller.xOffset.getValue(partialTicks);
            y += controller.yOffset.getValue(partialTicks);
            z += controller.zOffset.getValue(partialTicks);
            yaw += controller.yawOffset.getValue(partialTicks);
            pitch += controller.pitchOffset.getValue(partialTicks);
            roll += controller.rollOffset.getValue(partialTicks);
        }

        GlStateManager.translate(x, y, z);
        event.setYaw(event.getYaw() + yaw);
        event.setPitch(event.getPitch() + pitch);
        event.setRoll(event.getRoll() + roll);
    }

    public static void applyFov(EntityViewRenderEvent.FOVModifier event) {
        float fov = 0;
        for (Controller controller : controllers) {
            fov += controller.fovOffset.getValue((float) event.getRenderPartialTicks());
        }
        event.setFOV(fov);
    }

    public static class Controller {
        SmoothFloat xOffset;
        SmoothFloat yOffset;
        SmoothFloat zOffset;

        SmoothFloat yawOffset;
        SmoothFloat pitchOffset;
        SmoothFloat rollOffset;

        SmoothFloat fovOffset;

        private Controller() {
            this.xOffset = new SmoothFloat();
            this.yOffset = new SmoothFloat();
            this.zOffset = new SmoothFloat();

            this.yawOffset = new SmoothFloat();
            this.pitchOffset = new SmoothFloat();
            this.rollOffset = new SmoothFloat();

            this.fovOffset = new SmoothFloat();

            controllers.add(this);
        }

        //+X is left, +Y is top, +Z is back
        public void setThirdPersonXOffset(float x, float expectedTicks) {
            this.xOffset.setNewValue(x, expectedTicks);
        }

        public void setThirdPersonYOffset(float y, float expectedTicks) {
            this.yOffset.setNewValue(y, expectedTicks);
        }

        public void setThirdPersonZOffset(float z, float expectedTicks) {
            this.zOffset.setNewValue(z, expectedTicks);
        }

        public float getThirdPersonZOffset() {
            return zOffset.getValue(0);
        }

        public void setYawOffset(float yaw, float expectedTicks) {
            yawOffset.setNewValue(yaw, expectedTicks);
        }

        public void setPitchOffset(float pitch, float expectedTicks) {
            pitchOffset.setNewValue(pitch, expectedTicks);
        }

        public void setRollOffset(float roll, float expectedTicks) {
            rollOffset.setNewValue(roll, expectedTicks);
        }

        public void setFOV(float fov, float expectedTicks) {
            fovOffset.setNewValue(fov, expectedTicks);
        }
    }
}

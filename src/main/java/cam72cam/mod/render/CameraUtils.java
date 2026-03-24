package cam72cam.mod.render;

import cam72cam.mod.MinecraftClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.EntityViewRenderEvent;

public class CameraUtils {
    private static float x;
    private static float y;
    private static float z;

    private static float fov;

    private static float yaw;
    private static float pitch;
    private static float roll;

    private static boolean enabled;

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

    public static void enable() {
        enabled = true;
    }

    public static void disable() {
        enabled = false;
    }

    //+X is left, +Y is top, +Z is back
    public static void setThirdPersonCameraOffset(float x, float y, float z) {
        CameraUtils.x = x;
        CameraUtils.y = y;
        CameraUtils.z = z;
    }

    public static void setYawOffset(float yaw) {
        CameraUtils.yaw = yaw;
    }

    public static void setPitchOffset(float pitch) {
        CameraUtils.pitch = pitch;
    }

    public static void setRollOffset(float roll) {
        CameraUtils.roll = roll;
    }

    public static void setFov(float fov) {
        CameraUtils.fov = fov;
    }

    public static void applyTranslation(EntityViewRenderEvent.CameraSetup event) {
        if (enabled) {
            GlStateManager.translate(x, y, z);
            event.setYaw(yaw);
            event.setPitch(pitch);
            event.setRoll(roll);
        }
    }

    public static void applyFov(EntityViewRenderEvent.FOVModifier event) {
        if (enabled) {
            event.setFOV(fov);
        }
    }
}

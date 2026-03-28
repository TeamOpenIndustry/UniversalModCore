package cam72cam.mod.render;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.math.Vec3d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.EntityViewRenderEvent;

import java.util.*;

public class CameraUtils {
    private static final List<Controller> controllers = new ArrayList<>();
    private static float cameraRoll;
    private static boolean cameraColliding;

    public static Perspective getPerspective() {
        if (MinecraftClient.isReady()) {
            switch (Minecraft.getMinecraft().gameSettings.thirdPersonView) {
                case 0:
                    return Perspective.FIRST_PERSON;
                case 1:
                    return Perspective.THIRD_PERSON;
                case 2:
                    return Perspective.THIRD_PERSON_INVERTED;
            }
        }

        return Perspective.FIRST_PERSON;
    }

    public static void setPerspective(Perspective perspective) {
        if (MinecraftClient.isReady()) {
            switch (perspective) {
                case FIRST_PERSON:
                    Minecraft.getMinecraft().gameSettings.thirdPersonView = 0;
                    return;
                case THIRD_PERSON:
                    Minecraft.getMinecraft().gameSettings.thirdPersonView = 1;
                    return;
                case THIRD_PERSON_INVERTED:
                    Minecraft.getMinecraft().gameSettings.thirdPersonView = 2;
            }
        }
    }

    public static float getFov() {
        return Minecraft.getMinecraft().gameSettings.fovSetting;
    }

    /** Get global position of the player's eyes (with partialTicks taken into account) */
    public static Vec3d getCameraPos(float partialTicks) {
        net.minecraft.entity.Entity playerRender = Minecraft.getMinecraft().getRenderViewEntity();
        double d0 = playerRender.lastTickPosX + (playerRender.posX - playerRender.lastTickPosX) * partialTicks;
        double d1 = playerRender.lastTickPosY + (playerRender.posY - playerRender.lastTickPosY) * partialTicks;
        double d2 = playerRender.lastTickPosZ + (playerRender.posZ - playerRender.lastTickPosZ) * partialTicks;
        return new Vec3d(d0, d1, d2);
    }

    public static float getCameraYaw(float partialTicks) {
        net.minecraft.entity.Entity playerRender = Minecraft.getMinecraft().getRenderViewEntity();
        return playerRender.prevRotationYaw + (playerRender.rotationYaw - playerRender.prevRotationYaw) * partialTicks;
    }

    public static float getCameraPitch(float partialTicks) {
        net.minecraft.entity.Entity playerRender = Minecraft.getMinecraft().getRenderViewEntity();
        return playerRender.prevRotationPitch + (playerRender.rotationPitch - playerRender.prevRotationPitch) * partialTicks;
    }

    public static float getCameraRoll() {
        return cameraRoll;
    }

    public static Controller newController(Perspective... activeIn) {
        return new Controller(Arrays.asList(activeIn));
    }

    public static void applyTranslation(EntityViewRenderEvent.CameraSetup event) {
        cameraRoll = event.getRoll();

        float x = 0, y = 0, z = 0;
        float yaw = 0, pitch = 0, roll = 0;

        float partialTicks = (float) event.getRenderPartialTicks();

        Perspective perspective = getPerspective();
        for (Controller controller : controllers) {
            if (!controller.perspectives.contains(perspective))
                continue;
            x += controller.xOffset.getValue(partialTicks);
            y += controller.yOffset.getValue(partialTicks);
            z += controller.zOffset.getValue(partialTicks);
            yaw += controller.yawOffset.getValue(partialTicks);
            pitch += controller.pitchOffset.getValue(partialTicks);
            roll += controller.rollOffset.getValue(partialTicks);
        }

        GlStateManager.translate(x, y, z);
        event.setYaw(yaw);
        event.setPitch(pitch);
        event.setRoll(roll);
    }

    public static void applyFov(EntityViewRenderEvent.FOVModifier event) {
        float fov = 0;
        for (Controller controller : controllers) {
            fov += controller.fovOffset.getValue((float) event.getRenderPartialTicks());
        }
        event.setFOV(fov);
    }

    public enum Perspective {
        FIRST_PERSON,
        THIRD_PERSON,
        THIRD_PERSON_INVERTED,
    }

    public static class Controller {
        //+X is left, +Y is top, +Z is back
        public final SmoothFloat xOffset;
        public final SmoothFloat yOffset;
        public final SmoothFloat zOffset;

        //Roll is the first to be applied, then pitch, then yaw
        public final SmoothFloat rollOffset;
        public final SmoothFloat pitchOffset;
        public final SmoothFloat yawOffset;

        public final SmoothFloat fovOffset;

        final List<Perspective> perspectives;

        private Controller(List<Perspective> perspectives) {
            this.xOffset = new SmoothFloat(0);
            this.yOffset = new SmoothFloat(0);
            this.zOffset = new SmoothFloat(0);

            this.yawOffset = new SmoothFloat(0);
            this.pitchOffset = new SmoothFloat(0);
            this.rollOffset = new SmoothFloat(0);

            this.fovOffset = new SmoothFloat(0);

            this.perspectives = perspectives;

            controllers.add(this);
        }
    }
}
